package com.example.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.BuildConfig
import com.example.ui.viewmodel.TgzViewModel
import com.squareup.moshi.Moshi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Manual Dependency Injection container providing networking, Moshi, Retrofit ApiService,
 * native Minecraft SLP TCP client, ServerRepository, and ViewModelFactory.
 */
object NetworkModule {

    private const val DEFAULT_BASE_URL = "https://discord.com/api/v10/"
    private const val DEFAULT_DISCORD_INVITE = "https://discord.gg/J5P89mxXbc"
    private const val DEFAULT_MINECRAFT_HOST = "node2.vyroxsmp.online"
    private const val DEFAULT_MINECRAFT_PORT = 10010

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

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(userAgentInterceptor)
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

    val viewModelFactory: ViewModelProvider.Factory by lazy {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(TgzViewModel::class.java)) {
                    return TgzViewModel(serverRepository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }

    private fun extractDiscordInviteCode(inviteUrl: String): String {
        val cleaned = inviteUrl.substringBefore("?").trimEnd('/')
        return cleaned.substringAfterLast('/').ifBlank { "J5P89mxXbc" }
    }
}
