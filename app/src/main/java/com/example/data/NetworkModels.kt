package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DiscordInviteResponse(
    @Json(name = "code") val code: String? = null,
    @Json(name = "guild") val guild: DiscordInviteGuild? = null,
    @Json(name = "approximate_member_count") val approximateMemberCount: Int? = null,
    @Json(name = "approximate_presence_count") val approximatePresenceCount: Int? = null
)

@JsonClass(generateAdapter = true)
data class DiscordInviteGuild(
    @Json(name = "id") val id: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "icon") val icon: String? = null,
    @Json(name = "banner") val banner: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "vanity_url_code") val vanityUrlCode: String? = null
)

@JsonClass(generateAdapter = true)
data class DiscordGuildResponse(
    @Json(name = "id") val id: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "icon") val icon: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "approximate_member_count") val approximateMemberCount: Int? = null,
    @Json(name = "approximate_presence_count") val approximatePresenceCount: Int? = null
)

@JsonClass(generateAdapter = true)
data class DiscordWidgetResponse(
    @Json(name = "id") val id: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "instant_invite") val instantInvite: String? = null,
    @Json(name = "presence_count") val presenceCount: Int? = null,
    @Json(name = "members") val members: List<DiscordWidgetMember>? = null
)

@JsonClass(generateAdapter = true)
data class DiscordWidgetMember(
    @Json(name = "id") val id: String? = null,
    @Json(name = "username") val username: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "game") val game: DiscordWidgetGame? = null
)

@JsonClass(generateAdapter = true)
data class DiscordWidgetGame(
    @Json(name = "name") val name: String? = null
)

@JsonClass(generateAdapter = true)
data class McSrvStatResponse(
    @Json(name = "online") val online: Boolean? = null,
    @Json(name = "ip") val ip: String? = null,
    @Json(name = "port") val port: Int? = null,
    @Json(name = "hostname") val hostname: String? = null,
    @Json(name = "version") val version: String? = null,
    @Json(name = "software") val software: String? = null,
    @Json(name = "motd") val motd: McSrvStatMotd? = null,
    @Json(name = "players") val players: McSrvStatPlayers? = null
)

@JsonClass(generateAdapter = true)
data class McSrvStatMotd(
    @Json(name = "clean") val clean: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class McSrvStatPlayers(
    @Json(name = "online") val online: Int? = null,
    @Json(name = "max") val max: Int? = null,
    @Json(name = "list") val list: List<McSrvStatPlayer>? = null
)

@JsonClass(generateAdapter = true)
data class McSrvStatPlayer(
    @Json(name = "name") val name: String? = null,
    @Json(name = "uuid") val uuid: String? = null
)

@JsonClass(generateAdapter = true)
data class CustomDiscordStatsResponse(
    @Json(name = "guildId") val guildId: String? = null,
    @Json(name = "guildName") val guildName: String? = null,
    @Json(name = "guildIconUrl") val guildIconUrl: String? = null,
    @Json(name = "totalMembers") val totalMembers: Int? = null,
    @Json(name = "onlineMembers") val onlineMembers: Int? = null
)

@JsonClass(generateAdapter = true)
data class CustomMinecraftPlayersResponse(
    @Json(name = "online") val online: Int? = null,
    @Json(name = "max") val max: Int? = null,
    @Json(name = "completeList") val completeList: Boolean? = null,
    @Json(name = "players") val players: List<CustomMinecraftPlayerDto>? = null
)

@JsonClass(generateAdapter = true)
data class CustomMinecraftPlayerDto(
    @Json(name = "username") val username: String? = null,
    @Json(name = "uuid") val uuid: String? = null
)

@JsonClass(generateAdapter = true)
data class CustomDashboardResponse(
    @Json(name = "discord") val discord: CustomDiscordStatsResponse? = null,
    @Json(name = "minecraft") val minecraft: CustomMinecraftStatusDto? = null
)

@JsonClass(generateAdapter = true)
data class CustomMinecraftStatusDto(
    @Json(name = "online") val online: Boolean? = null,
    @Json(name = "version") val version: String? = null,
    @Json(name = "motd") val motd: String? = null,
    @Json(name = "latencyMs") val latencyMs: Long? = null,
    @Json(name = "onlinePlayers") val onlinePlayers: Int? = null,
    @Json(name = "maxPlayers") val maxPlayers: Int? = null,
    @Json(name = "completePlayerList") val completePlayerList: Boolean? = null,
    @Json(name = "players") val players: List<CustomMinecraftPlayerDto>? = null
)
