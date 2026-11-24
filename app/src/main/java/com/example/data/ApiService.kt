package com.example.data

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Url

/**
 * Retrofit API contract for TGZ Lifesteal live Discord and Minecraft server integrations.
 *
 * Supports:
 * - Official Discord Invite API (with live member & presence counts, zero bot token needed)
 * - Official Discord Guild Widget JSON API (optional live activity enrichment)
 * - Official Discord Bot Guild API (optional when DISCORD_BOT_TOKEN & DISCORD_GUILD_ID are configured)
 * - Minecraft Java Server Status API (api.mcsrvstat.us v3 fallback)
 * - Optional custom backend/plugin endpoints (/api/server/dashboard, /api/discord/stats, /api/minecraft/players)
 */
interface ApiService {

    @GET("https://discord.com/api/v10/invites/{inviteCode}?with_counts=true&with_expiration=true")
    suspend fun getDiscordInviteStats(
        @Path("inviteCode") inviteCode: String
    ): DiscordInviteResponse

    @GET("https://discord.com/api/guilds/{guildId}/widget.json")
    suspend fun getDiscordWidget(
        @Path("guildId") guildId: String
    ): DiscordWidgetResponse

    @GET("https://discord.com/api/v10/guilds/{guildId}?with_counts=true")
    suspend fun getDiscordGuildWithBot(
        @Path("guildId") guildId: String,
        @Header("Authorization") botAuthorization: String
    ): DiscordGuildResponse

    @GET("https://api.mcsrvstat.us/3/{address}")
    suspend fun getMinecraftServerStatus(
        @Path(value = "address", encoded = true) address: String
    ): McSrvStatResponse

    @GET
    suspend fun getCustomDashboard(
        @Url dashboardUrl: String
    ): CustomDashboardResponse

    @GET
    suspend fun getCustomDiscordStats(
        @Url discordStatsUrl: String
    ): CustomDiscordStatsResponse

    @GET
    suspend fun getCustomMinecraftPlayers(
        @Url playersUrl: String
    ): CustomMinecraftPlayersResponse
}
