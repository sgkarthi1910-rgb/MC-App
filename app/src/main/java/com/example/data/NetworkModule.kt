package com.example.data

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.BuildConfig
import com.example.ui.viewmodel.TgzViewModel
import com.squareup.moshi.Moshi
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Manual Dependency Injection container providing networking, Moshi, Retrofit ApiService,
 * native Minecraft SLP TCP client, ServerRepository, SessionManager, BackendRepository,
 * and ViewModelFactory.
 */
object NetworkModule {

    private const val DEFAULT_BASE_URL = "https://discord.com/api/v10/"
    private const val DEFAULT_DISCORD_INVITE = "https://discord.gg/J5P89mxXbc"
    private const val DEFAULT_MINECRAFT_HOST = "node2.vyroxsmp.online"
    private const val DEFAULT_MINECRAFT_PORT = 10010
    private const val DEFAULT_BACKEND_URL = "https://mc-app-ten.vercel.app/"

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val appConfig: AppServerConfig by lazy {
        val inviteUrl = BuildConfig.DISCORD_INVITE_URL
            .trim()
            .ifBlank { DEFAULT_DISCORD_INVITE }
        val inviteCode = extractDiscordInviteCode(inviteUrl)
        val mcHost = BuildConfig.MINECRAFT_SERVER_HOST
            .trim()
            .ifBlank { DEFAULT_MINECRAFT_HOST }
        val mcPort = BuildConfig.MINECRAFT_SERVER_PORT
            .trim()
            .toIntOrNull()
            ?: DEFAULT_MINECRAFT_PORT

        AppServerConfig(
            discordInviteUrl = inviteUrl,
            discordInviteCode = inviteCode,
            discordGuildId = cleanOptionalConfig(BuildConfig.DISCORD_GUILD_ID),
            discordBotToken = cleanOptionalConfig(BuildConfig.DISCORD_BOT_TOKEN),
            minecraftHost = mcHost,
            minecraftPort = mcPort,
            customDashboardApiUrl = cleanOptionalConfig(BuildConfig.CUSTOM_DASHBOARD_API_URL),
            minecraftPluginPlayersUrl = cleanOptionalConfig(BuildConfig.MINECRAFT_PLUGIN_PLAYERS_URL)
        )
    }

    private fun cleanOptionalConfig(raw: String): String {
        val trimmed = raw.trim()
        return if (trimmed.isBlank() || trimmed.startsWith("OPTIONAL_")) "" else trimmed
    }

    private val userAgentInterceptor: Interceptor by lazy {
        Interceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "TGZLifestealAndroid/1.0 (https://discord.gg/J5P89mxXbc)")
                .header("Accept", "application/json")
                .build()
            chain.proceed(request)
        }
    }

    private val loggingInterceptor: HttpLoggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
            redactHeader("Authorization")
        }
    }

    private val backendHostInterceptor: Interceptor by lazy {
        Interceptor { chain ->
            var request = chain.request()
            val customUrl = appContext?.let { SessionManager(it).getBackendUrl() }?.trim()
            if (!customUrl.isNullOrBlank()) {
                val parsed = customUrl.toHttpUrlOrNull()
                if (parsed != null &&
                    !request.url.host.contains("discord") &&
                    !request.url.host.contains("mcsrvstat") &&
                    !request.url.host.contains("mc-heads")
                ) {
                    val newUrl = request.url.newBuilder()
                        .scheme(parsed.scheme)
                        .host(parsed.host)
                        .port(parsed.port)
                        .build()
                    request = request.newBuilder().url(newUrl).build()
                }
            }
            chain.proceed(request)
        }
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(userAgentInterceptor)
            .addInterceptor(backendHostInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    val moshi: Moshi by lazy {
        Moshi.Builder().build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(DEFAULT_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    val backendRetrofit: Retrofit by lazy {
        val baseUrl = appConfig.customDashboardApiUrl.ifBlank { DEFAULT_BACKEND_URL }
        val normalized = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        Retrofit.Builder()
            .baseUrl(normalized)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    val backendApiService: BackendApiService by lazy {
        backendRetrofit.create(BackendApiService::class.java)
    }

    val minecraftSlpClient: MinecraftSlpClient by lazy {
        MinecraftSlpClient(timeoutMs = 4500)
    }

    val serverRepository: ServerRepository by lazy {
        ServerRepository(
            apiService = apiService,
            slpClient = minecraftSlpClient,
            config = appConfig
        )
    }

    @Volatile
    private var sessionManagerInstance: SessionManager? = null

    fun getSessionManager(context: Context? = null): SessionManager? {
        val ctx = context?.applicationContext ?: appContext
        if (sessionManagerInstance == null && ctx != null) {
            synchronized(this) {
                if (sessionManagerInstance == null) {
                    sessionManagerInstance = SessionManager(ctx)
                }
            }
        }
        return sessionManagerInstance
    }

    fun getBackendRepository(context: Context? = null): TgzBackendRepository? {
        val sm = getSessionManager(context) ?: return null
        return TgzBackendRepository(backendApiService, sm)
    }

    fun getViewModelFactory(context: Context? = null): ViewModelProvider.Factory {
        val sm = getSessionManager(context)
        val backendRepo = if (sm != null) TgzBackendRepository(backendApiService, sm) else null

        return object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(TgzViewModel::class.java)) {
                    return TgzViewModel(serverRepository, backendRepo, sm) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }

    val viewModelFactory: ViewModelProvider.Factory by lazy {
        getViewModelFactory(null)
    }

    private fun extractDiscordInviteCode(inviteUrl: String): String {
        val cleaned = inviteUrl.substringBefore("?").trimEnd('/')
        return cleaned.substringAfterLast('/').ifBlank { "J5P89mxXbc" }
    }
}
