package com.example.data

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

data class CombinedServerSnapshot(
    val discordStats: DiscordServerStats?,
    val discordError: String?,
    val minecraftStatus: MinecraftServerStatus?,
    val minecraftError: String?
)

/**
 * Coordinates real-time fetching of Discord guild statistics and Minecraft Java SMP telemetry.
 * Never invents fake player counts, fake member totals, or hardcoded ONLINE statuses.
 */
class ServerRepository(
    private val apiService: ApiService,
    private val slpClient: MinecraftSlpClient,
    val config: AppServerConfig
) {

    suspend fun fetchLiveSnapshot(): CombinedServerSnapshot = coroutineScope {
        // Optional unified custom backend dashboard (GET /api/server/dashboard)
        if (config.customDashboardApiUrl.isNotBlank()) {
            val customResult = runCatching {
                apiService.getCustomDashboard(config.customDashboardApiUrl)
            }.getOrNull()

            if (customResult != null && (customResult.discord != null || customResult.minecraft != null)) {
                val mappedDiscord = customResult.discord?.let { d ->
                    DiscordServerStats(
                        guildId = d.guildId,
                        guildName = d.guildName ?: "TGZ Lifesteal Discord",
                        guildIconUrl = d.guildIconUrl,
                        guildDescription = null,
                        totalMembers = d.totalMembers,
                        onlineMembers = d.onlineMembers,
                        inviteUrl = config.discordInviteUrl,
                        dataSource = "TGZ Backend API"
                    )
                }
                val mappedMc = customResult.minecraft?.let { m ->
                    val players = m.players.orEmpty().mapNotNull { p ->
                        val name = p.username?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                        MinecraftPlayer(
                            username = name,
                            uuid = p.uuid,
                            avatarUrl = buildMinecraftAvatarUrl(name, p.uuid)
                        )
                    }
                    MinecraftServerStatus(
                        isOnline = m.online == true,
                        host = config.minecraftHost,
                        port = config.minecraftPort,
                        onlinePlayers = m.onlinePlayers ?: players.size,
                        maxPlayers = m.maxPlayers ?: 0,
                        version = m.version,
                        motd = m.motd,
                        latencyMs = m.latencyMs,
                        players = players,
                        isSampleListOnly = m.completePlayerList != true,
                        querySource = "TGZ Backend API"
                    )
                }
                if (mappedDiscord != null && mappedMc != null) {
                    return@coroutineScope CombinedServerSnapshot(
                        discordStats = mappedDiscord,
                        discordError = null,
                        minecraftStatus = mappedMc,
                        minecraftError = null
                    )
                }
            }
        }

        val discordDeferred = async { fetchDiscordStats() }
        val minecraftDeferred = async { fetchMinecraftStatus() }

        val (discordStats, discordError) = discordDeferred.await()
        val (minecraftStatus, minecraftError) = minecraftDeferred.await()

        CombinedServerSnapshot(
            discordStats = discordStats,
            discordError = discordError,
            minecraftStatus = minecraftStatus,
            minecraftError = minecraftError
        )
    }

    private suspend fun fetchDiscordStats(): Pair<DiscordServerStats?, String?> {
        // 1. Optional direct Discord Bot API if user configured DISCORD_BOT_TOKEN & DISCORD_GUILD_ID in Secrets
        if (config.discordBotToken.isNotBlank() && config.discordGuildId.isNotBlank()) {
            val botResult = runCatching {
                val guild = apiService.getDiscordGuildWithBot(
                    guildId = config.discordGuildId,
                    botAuthorization = "Bot ${config.discordBotToken}"
                )
                val widgetMembers = fetchWidgetMinecraftMembers(config.discordGuildId)
                DiscordServerStats(
                    guildId = guild.id ?: config.discordGuildId,
                    guildName = guild.name ?: "TGZ Lifesteal",
                    guildIconUrl = buildDiscordGuildIconUrl(guild.id ?: config.discordGuildId, guild.icon),
                    guildDescription = guild.description,
                    totalMembers = guild.approximateMemberCount,
                    onlineMembers = guild.approximatePresenceCount,
                    inviteUrl = config.discordInviteUrl,
                    dataSource = "Discord Bot API",
                    widgetPlayingMinecraft = widgetMembers
                )
            }
            if (botResult.isSuccess) {
                return botResult.getOrNull() to null
            }
        }

        // 2. Official Discord Public Invite API with live counts (no bot token exposed in APK)
        val inviteCode = config.discordInviteCode
        if (inviteCode.isBlank()) {
            return null to "Discord invite URL is not configured."
        }

        return runCatching {
            val invite = apiService.getDiscordInviteStats(inviteCode)
            val guildId = invite.guild?.id ?: config.discordGuildId.takeIf { it.isNotBlank() }
            val widgetMembers = if (!guildId.isNullOrBlank()) {
                fetchWidgetMinecraftMembers(guildId)
            } else {
                emptyList()
            }

            DiscordServerStats(
                guildId = guildId,
                guildName = invite.guild?.name?.takeIf { it.isNotBlank() } ?: "TGZ Lifesteal Discord",
                guildIconUrl = buildDiscordGuildIconUrl(guildId, invite.guild?.icon),
                guildDescription = invite.guild?.description?.takeIf { it.isNotBlank() },
                totalMembers = invite.approximateMemberCount,
                onlineMembers = invite.approximatePresenceCount,
                inviteUrl = config.discordInviteUrl,
                dataSource = "Discord Live Invite API",
                widgetPlayingMinecraft = widgetMembers
            )
        }.fold(
            onSuccess = { it to null },
            onFailure = { error ->
                null to "Unable to reach Discord API (${error.message ?: "network error"})."
            }
        )
    }

    private suspend fun fetchWidgetMinecraftMembers(guildId: String): List<DiscordActivityMember> {
        return runCatching {
            val widget = apiService.getDiscordWidget(guildId)
            widget.members.orEmpty()
                .filter { member ->
                    val gameName = member.game?.name.orEmpty()
                    gameName.contains("minecraft", ignoreCase = true) ||
                        gameName.contains("vyrox", ignoreCase = true) ||
                        gameName.contains("tgz", ignoreCase = true)
                }
                .mapNotNull { member ->
                    val name = member.username?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    DiscordActivityMember(
                        id = member.id ?: name,
                        username = name,
                        status = member.status ?: "online",
                        gameName = member.game?.name,
                        avatarUrl = member.avatarUrl
                    )
                }
        }.getOrDefault(emptyList())
    }

    private suspend fun fetchMinecraftStatus(): Pair<MinecraftServerStatus?, String?> {
        val host = config.minecraftHost
        val port = config.minecraftPort
        val fullAddress = config.fullServerAddress

        // Optional custom Minecraft plugin complete roster endpoint
        val customPluginPlayers: CustomMinecraftPlayersResponse? =
            if (config.minecraftPluginPlayersUrl.isNotBlank()) {
                runCatching {
                    apiService.getCustomMinecraftPlayers(config.minecraftPluginPlayersUrl)
                }.getOrNull()
            } else {
                null
            }

        // 1. Primary: Direct Java Server List Ping (TCP SLP) for real-time status & latency
        val slpAttempt = runCatching { slpClient.ping(host, port) }
        if (slpAttempt.isSuccess) {
            val slp = slpAttempt.getOrThrow()
            var players = slp.samplePlayers
            var isSampleOnly = true
            var querySource = "Direct Java TCP Ping"

            // Enrich with custom SMP plugin roster if configured
            if (customPluginPlayers?.players != null) {
                players = customPluginPlayers.players.mapNotNull { dto ->
                    val name = dto.username?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    MinecraftPlayer(name, dto.uuid, buildMinecraftAvatarUrl(name, dto.uuid))
                }
                isSampleOnly = customPluginPlayers.completeList != true
                querySource = "Direct TCP + SMP Plugin"
            } else if (slp.onlinePlayers > 0 && players.isEmpty()) {
                // If TCP SLP omitted the player sample, check mcsrvstat.us v3 for cached names
                val apiPlayers = runCatching {
                    apiService.getMinecraftServerStatus(fullAddress).players?.list.orEmpty()
                }.getOrDefault(emptyList())
                if (apiPlayers.isNotEmpty()) {
                    players = apiPlayers.mapNotNull { p ->
                        val name = p.name?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                        MinecraftPlayer(name, p.uuid, buildMinecraftAvatarUrl(name, p.uuid))
                    }
                }
            }

            return MinecraftServerStatus(
                isOnline = slp.online,
                host = host,
                port = port,
                onlinePlayers = slp.onlinePlayers,
                maxPlayers = slp.maxPlayers,
                version = slp.version,
                motd = slp.motd,
                latencyMs = slp.latencyMs,
                players = players,
                isSampleListOnly = isSampleOnly,
                querySource = querySource
            ) to null
        }

        // 2. Fallback: Official mcsrvstat.us v3 Java status endpoint
        return runCatching {
            val response = apiService.getMinecraftServerStatus(fullAddress)
            val isOnline = response.online == true
            val apiPlayers = response.players?.list.orEmpty().mapNotNull { p ->
                val name = p.name?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                MinecraftPlayer(
                    username = name,
                    uuid = p.uuid,
                    avatarUrl = buildMinecraftAvatarUrl(name, p.uuid)
                )
            }

            val finalPlayers = if (customPluginPlayers?.players != null && isOnline) {
                customPluginPlayers.players.mapNotNull { dto ->
                    val name = dto.username?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    MinecraftPlayer(name, dto.uuid, buildMinecraftAvatarUrl(name, dto.uuid))
                }
            } else {
                apiPlayers
            }

            val isSampleOnly = if (customPluginPlayers?.players != null) {
                customPluginPlayers.completeList != true
            } else {
                true
            }

            MinecraftServerStatus(
                isOnline = isOnline,
                host = host,
                port = port,
                onlinePlayers = if (isOnline) (response.players?.online ?: finalPlayers.size) else 0,
                maxPlayers = response.players?.max ?: 0,
                version = response.version?.takeIf { it.isNotBlank() },
                motd = response.motd?.clean
                    ?.joinToString(" ")
                    ?.replace(Regex("\\s+"), " ")
                    ?.trim()
                    ?.takeIf { it.isNotBlank() },
                latencyMs = null,
                players = if (isOnline) finalPlayers else emptyList(),
                isSampleListOnly = isSampleOnly,
                querySource = "mcsrvstat.us v3 API"
            )
        }.fold(
            onSuccess = { status -> status to null },
            onFailure = { err ->
                null to "Unable to reach Minecraft status query (${err.message ?: "connection timeout"})."
            }
        )
    }

    private fun buildDiscordGuildIconUrl(guildId: String?, iconHash: String?): String? {
        if (guildId.isNullOrBlank() || iconHash.isNullOrBlank()) return null
        val ext = if (iconHash.startsWith("a_")) "gif" else "png"
        return "https://cdn.discordapp.com/icons/$guildId/$iconHash.$ext?size=128"
    }
}
