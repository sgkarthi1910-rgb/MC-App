package com.example.data

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.DiscordBlurple
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.MinecraftGold
import com.example.ui.theme.NetherRed

data class AppServerConfig(
    val discordInviteUrl: String,
    val discordInviteCode: String,
    val discordGuildId: String,
    val discordBotToken: String,
    val minecraftHost: String,
    val minecraftPort: Int,
    val customDashboardApiUrl: String,
    val minecraftPluginPlayersUrl: String
) {
    val fullServerAddress: String
        get() = "$minecraftHost:$minecraftPort"
}

data class DiscordActivityMember(
    val id: String,
    val username: String,
    val status: String,
    val gameName: String?,
    val avatarUrl: String?
)

data class DiscordServerStats(
    val guildId: String?,
    val guildName: String,
    val guildIconUrl: String?,
    val guildDescription: String?,
    val totalMembers: Int?,
    val onlineMembers: Int?,
    val inviteUrl: String,
    val dataSource: String,
    val widgetPlayingMinecraft: List<DiscordActivityMember> = emptyList()
)

data class MinecraftPlayer(
    val username: String,
    val uuid: String?,
    val avatarUrl: String
)

data class MinecraftServerStatus(
    val isOnline: Boolean,
    val host: String,
    val port: Int,
    val onlinePlayers: Int,
    val maxPlayers: Int,
    val version: String?,
    val motd: String?,
    val latencyMs: Long?,
    val players: List<MinecraftPlayer>,
    val isSampleListOnly: Boolean,
    val querySource: String
) {
    val fullAddress: String
        get() = "$host:$port"
}

data class DashboardUiState(
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val lastUpdatedFormatted: String? = null,
    val nextRefreshSeconds: Int = 120,
    val discordStats: DiscordServerStats? = null,
    val minecraftStatus: MinecraftServerStatus? = null,
    val discordError: String? = null,
    val minecraftError: String? = null,
    val serverAddress: String = "node2.vyroxsmp.online:10010",
    val discordInviteUrl: String = "https://discord.gg/J5P89mxXbc"
) {
    val hasAnyConnectionWarning: Boolean
        get() = discordError != null || minecraftError != null
}

enum class FeatureIconType {
    LIFESTEAL,
    PVP,
    LAND_CLAIM,
    SURVIVAL,
    COMMUNITY,
    ECONOMY
}

data class ServerFeature(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val highlights: List<String>,
    val accentColor: Color,
    val iconType: FeatureIconType
)

data class ServerRank(
    val id: String,
    val name: String,
    val tag: String,
    val priceDisplay: String,
    val tagline: String,
    val accentColor: Color,
    val isFeatured: Boolean,
    val perks: List<String>
)

enum class StoreCategory(val label: String) {
    ALL("All Packs"),
    HEARTS("Heart Packs"),
    CRATES("Crate Keys"),
    BUNDLES("Revival & Bundles")
}

data class StorePack(
    val id: String,
    val name: String,
    val category: StoreCategory,
    val priceDisplay: String,
    val badge: String?,
    val description: String,
    val contents: List<String>,
    val accentColor: Color
)

data class FaqItem(
    val id: String,
    val category: String,
    val question: String,
    val answer: String
)

object TgzCatalogData {
    val features: List<ServerFeature> = listOf(
        ServerFeature(
            id = "lifesteal",
            title = "Lifesteal Mechanics",
            subtitle = "Every Kill Claims a Heart",
            description = "Eliminate rival survivors in combat to permanently steal one of their hearts. Drop to zero hearts and face elimination until an ally crafts a Revival Beacon.",
            highlights = listOf(
                "Steal +1 permanent heart on every PvP elimination",
                "Withdraw physical hearts to trade, store, or gift",
                "Craft custom Heart Crystals & Revival Beacons"
            ),
            accentColor = NetherRed,
            iconType = FeatureIconType.LIFESTEAL
        ),
        ServerFeature(
            id = "pvp",
            title = "Competitive PvP Battles",
            subtitle = "Zero-Lag Crystal & Sword Combat",
            description = "Tuned combat mechanics with anti-combat-log enforcement, balanced netherite metas, and high-stakes King of the Hill arenas where clans clash for supremacy.",
            highlights = listOf(
                "Responsive tick-rate optimized PvP combat",
                "Scheduled KOTH & Supply Drop warzones",
                "Strict anti-cheat & fair duel arenas"
            ),
            accentColor = EmeraldGreen,
            iconType = FeatureIconType.PVP
        ),
        ServerFeature(
            id = "land_claim",
            title = "Land Claim & Empires",
            subtitle = "Fortify Your Base & Vaults",
            description = "Protect your stashes, farms, and clan strongholds using chunk-based land claims with granular teammate permissions and raid defense alerts.",
            highlights = listOf(
                "Claim chunks effortlessly with golden shovel or commands",
                "Manage trusted clan members & container access",
                "Expand claim capacity through playtime & ranks"
            ),
            accentColor = MinecraftGold,
            iconType = FeatureIconType.LAND_CLAIM
        ),
        ServerFeature(
            id = "survival",
            title = "Pure SMP Survival",
            subtitle = "Expanded World & Custom Enchants",
            description = "Explore a vibrant Java survival world packed with custom terrain exploration, Nether expeditions, player-built towns, and balanced progression.",
            highlights = listOf(
                "Generous world border with fresh resources",
                "Random teleport (/rtp) straight into untouched biomes",
                "Balanced progression with seasonal resets & events"
            ),
            accentColor = EmeraldGreen,
            iconType = FeatureIconType.SURVIVAL
        ),
        ServerFeature(
            id = "community",
            title = "Active TGZ Community",
            subtitle = "Clans, Alliances & Discord Events",
            description = "Join alliances, organize clan raids, participate in weekend tournaments, and connect directly with staff and fellow survivors inside the TGZ Discord.",
            highlights = listOf(
                "Active moderation & dedicated staff support",
                "Community polls shaping seasonal updates",
                "Weekly Discord giveaways & PvP tournaments"
            ),
            accentColor = DiscordBlurple,
            iconType = FeatureIconType.COMMUNITY
        ),
        ServerFeature(
            id = "economy",
            title = "Economy & Trading",
            subtitle = "Player Markets & Auction House",
            description = "Build wealth through player-driven auctions, safe trading menus, emerald shops, and rare heart markets without pay-to-win inflation.",
            highlights = listOf(
                "Global Auction House (/ah) for rare gear & hearts",
                "Scam-proof player-to-player /trade GUI",
                "Dynamic server shop & player warp markets"
            ),
            accentColor = MinecraftGold,
            iconType = FeatureIconType.ECONOMY
        )
    )

    val ranks: List<ServerRank> = listOf(
        ServerRank(
            id = "rank_vanguard",
            name = "VANGUARD",
            tag = "[VANGUARD]",
            priceDisplay = "Discord Ticket",
            tagline = "Essential survival edge for rising Lifesteal fighters.",
            accentColor = EmeraldGreen,
            isFeatured = false,
            perks = listOf(
                "Emerald [VANGUARD] chat & tab prefix",
                "3 /sethome slots + /craft portable bench",
                "+2,500 bonus Land Claim blocks",
                "Weekly Vanguard Supply Kit"
            )
        ),
        ServerRank(
            id = "rank_warlord",
            name = "WARLORD",
            tag = "[WARLORD]",
            priceDisplay = "Discord Ticket",
            tagline = "Built for aggressive clan raiders and PvP hunters.",
            accentColor = MinecraftGold,
            isFeatured = false,
            perks = listOf(
                "Gold [WARLORD] prefix & colored chat",
                "6 /sethome slots + /enderchest anywhere",
                "+6,000 bonus Land Claim blocks",
                "5 Auction House slots + Warlord PvP Kit"
            )
        ),
        ServerRank(
            id = "rank_overlord",
            name = "OVERLORD",
            tag = "[OVERLORD]",
            priceDisplay = "Discord Ticket",
            tagline = "Most popular tier for empire leaders and top contenders.",
            accentColor = NetherRed,
            isFeatured = true,
            perks = listOf(
                "Crimson [OVERLORD] gradient tag",
                "10 /sethome slots + /anvil & /enderchest",
                "+15,000 Land Claim blocks & /nick access",
                "8 Auction House slots + Priority Server Queue",
                "Weekly Overlord Kit + 2 Monthly Crate Keys"
            )
        ),
        ServerRank(
            id = "rank_immortal",
            name = "IMMORTAL",
            tag = "[IMMORTAL]",
            priceDisplay = "Discord Ticket",
            tagline = "Ultimate TGZ prestige with maximum claim & clan perks.",
            accentColor = MinecraftGold,
            isFeatured = false,
            perks = listOf(
                "Custom Gold/Emerald [IMMORTAL] crown tag",
                "20 /sethome slots + full utility suite",
                "+35,000 Land Claim blocks & /playerwarp",
                "15 Auction House slots + Exclusive Kill Effects",
                "Priority Discord Support + 5 Monthly Crate Keys"
            )
        )
    )

    val storePacks: List<StorePack> = listOf(
        StorePack(
            id = "pack_heart_3",
            name = "3x Lifesteal Hearts",
            category = StoreCategory.HEARTS,
            priceDisplay = "Order via Discord",
            badge = "POPULAR",
            description = "Replenish your vitality after intense clan wars or stock your vault with tradeable Heart Crystals.",
            contents = listOf(
                "3x Physical Lifesteal Heart items",
                "Instant in-game delivery after ticket approval",
                "Safe to store, consume, or trade"
            ),
            accentColor = NetherRed
        ),
        StorePack(
            id = "pack_heart_5",
            name = "5x Crimson Heart Bundle",
            category = StoreCategory.HEARTS,
            priceDisplay = "Order via Discord",
            badge = "BEST VALUE",
            description = "A full five-heart reinforcement pack designed for frontline PvP champions.",
            contents = listOf(
                "5x Physical Lifesteal Heart items",
                "1x Bonus Obsidian Crate Key",
                "Priority staff fulfillment on Discord"
            ),
            accentColor = NetherRed
        ),
        StorePack(
            id = "pack_crate_emerald",
            name = "5x Emerald Crate Keys",
            category = StoreCategory.CRATES,
            priceDisplay = "Order via Discord",
            badge = null,
            description = "Unlock rare enchanted tools, claim blocks, totems, and seasonal cosmetics at Spawn.",
            contents = listOf(
                "5x Emerald Crate Keys",
                "Chance for Enchanted Netherite gear",
                "Chance for Heart Fragments & Claim Scrolls"
            ),
            accentColor = EmeraldGreen
        ),
        StorePack(
            id = "pack_crate_nether",
            name = "3x Nether Mythic Keys",
            category = StoreCategory.CRATES,
            priceDisplay = "Order via Discord",
            badge = "MYTHIC",
            description = "High-tier Nether crates packed with endgame Lifesteal artifacts and custom weapons.",
            contents = listOf(
                "3x Nether Mythic Crate Keys",
                "Guaranteed Rare+ loot roll on every key",
                "Exclusive TGZ seasonal weapon skins"
            ),
            accentColor = MinecraftGold
        ),
        StorePack(
            id = "pack_revival",
            name = "Revival Beacon Pack",
            category = StoreCategory.BUNDLES,
            priceDisplay = "Order via Discord",
            badge = "CLAN SAVER",
            description = "Bring an eliminated teammate back into the TGZ Lifesteal realm immediately.",
            contents = listOf(
                "1x Official Revival Beacon",
                "2x Starter Recovery Hearts",
                "Fulfilled directly via TGZ Discord ticket"
            ),
            accentColor = EmeraldGreen
        ),
        StorePack(
            id = "pack_empire_starter",
            name = "Empire Starter Bundle",
            category = StoreCategory.BUNDLES,
            priceDisplay = "Order via Discord",
            badge = "STARTER",
            description = "Everything a new survivor needs to establish a fortified base and start hunting.",
            contents = listOf(
                "+5,000 Land Claim Blocks",
                "3x Emerald Crate Keys",
                "2x Lifesteal Hearts + Clan Banner Token"
            ),
            accentColor = MinecraftGold
        )
    )

    val faqs: List<FaqItem> = listOf(
        FaqItem(
            id = "faq_join",
            category = "Getting Started",
            question = "How do I join the TGZ Lifesteal Minecraft server?",
            answer = "Open Minecraft: Java Edition, click Multiplayer -> Add Server, enter 'TGZ Lifesteal' as the name and 'node2.vyroxsmp.online:10010' as the Server Address, then save and join."
        ),
        FaqItem(
            id = "faq_lifesteal",
            category = "Gameplay",
            question = "How do Lifesteal hearts and elimination work?",
            answer = "Every player starts with 10 hearts. Eliminating another player in PvP steals 1 of their hearts permanently. If you lose all your hearts, you are eliminated until a clanmate revives you using a Revival Beacon or staff event."
        ),
        FaqItem(
            id = "faq_buy",
            category = "Ranks & Store",
            question = "How do I purchase a Rank, Heart Pack, or Crate Keys?",
            answer = "Tap any 'Buy on Discord' button in the Ranks or Store tab to join our official TGZ Discord server (discord.gg/J5P89mxXbc) and open a Store Ticket. Our staff team verifies and delivers all packs directly in-game."
        ),
        FaqItem(
            id = "faq_players",
            category = "Live Status",
            question = "Why might the player list show fewer names than the total online count?",
            answer = "Minecraft's standard Java Server List Ping (SLP) protocol returns the exact live online count and capacity, but servers often cap the public player sample list (typically 12 names) or hide names for privacy. When an optional SMP plugin endpoint is connected, the complete roster appears automatically."
        ),
        FaqItem(
            id = "faq_refresh",
            category = "Live Status",
            question = "How often do Discord and Minecraft statistics refresh?",
            answer = "All live telemetry (Discord Total Members, Discord Online, Minecraft Players Online, and Server Status) automatically refreshes every 2 minutes (120 seconds). You can also tap the Refresh button at any time."
        ),
        FaqItem(
            id = "faq_claim",
            category = "Gameplay",
            question = "How do I protect my base from griefing?",
            answer = "Use the Land Claim system in-game to claim chunks around your base and vaults. You can earn extra claim blocks passively just by playing on the SMP or via rank perks."
        )
    )
}
