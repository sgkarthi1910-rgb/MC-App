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
    val price: String = "",
    val tag: String = "",
    val priceDisplay: String = "",
    val tagline: String = "",
    val accentColor: Color = EmeraldGreen,
    val isFeatured: Boolean = false,
    val perks: List<String> = emptyList()
)

enum class StoreCategory(val label: String) {
    ALL("All Ranks"),
    TITAN("Titan"),
    WIZARD("Wizard"),
    KING("King"),
    STATIC_PLUS("Static+"),
    CHAOS("Chaos")
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
            title = "Lifestyle Community",
            subtitle = "Survival, Friendship & Creative Journeys",
            description = "Build your empire, connect with fellow players, create teams, and explore survival adventures in the TGZ Lifestyle community.",
            highlights = listOf(
                "Custom player economy, building, and trade markets",
                "Team alliances, land claims, and communal projects",
                "Seasonal lifestyle community events & rewards"
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
            id = "rank_titan",
            name = "Titan",
            price = "49",
            accentColor = Color(0xFF9CA3AF)
        ),
        ServerRank(
            id = "rank_wizard",
            name = "Wizard",
            price = "59",
            accentColor = Color(0xFFA855F7)
        ),
        ServerRank(
            id = "rank_king",
            name = "King",
            price = "79",
            accentColor = MinecraftGold
        ),
        ServerRank(
            id = "rank_static_plus",
            name = "Static+",
            price = "89",
            accentColor = Color(0xFF38BDF8)
        ),
        ServerRank(
            id = "rank_chaos",
            name = "Chaos",
            price = "149",
            accentColor = NetherRed
        )
    )

    val storePacks: List<StorePack> = listOf(
        StorePack(
            id = "rank_titan",
            name = "Titan Rank Pack",
            category = StoreCategory.TITAN,
            priceDisplay = "₹49",
            badge = "TIER 1",
            description = "Starter VIP rank pack with essential survivor commands and claim protection.",
            contents = listOf(
                "Permanent [Titan] in-game & Discord prefix",
                "+2,500 Land Claim Protection blocks",
                "Access to /hat and /near commands",
                "Priority queue connection on server restart"
            ),
            accentColor = Color(0xFF9CA3AF)
        ),
        StorePack(
            id = "rank_wizard",
            name = "Wizard Rank Pack",
            category = StoreCategory.WIZARD,
            priceDisplay = "₹59",
            badge = "TIER 2",
            description = "Enchanted VIP rank pack featuring magical utility commands and portable workstations.",
            contents = listOf(
                "Permanent [Wizard] in-game & Discord prefix",
                "+5,000 Land Claim Protection blocks",
                "Access to /craft, /enderchest & /workbench anywhere",
                "Weekly Wizard potion & supply care package"
            ),
            accentColor = Color(0xFFA855F7)
        ),
        StorePack(
            id = "rank_king",
            name = "King Rank Pack",
            category = StoreCategory.KING,
            priceDisplay = "₹79",
            badge = "POPULAR",
            description = "Royal VIP rank pack with territory flight, enhanced vault storage, and mystery vouchers.",
            contents = listOf(
                "Permanent [King] in-game & Discord prefix",
                "+10,000 Land Claim Protection blocks",
                "Access to /fly inside owned claim territory",
                "2x Vault storage pages (/pv 1-2)",
                "1x Monthly King Bonus Key voucher"
            ),
            accentColor = MinecraftGold
        ),
        StorePack(
            id = "rank_static_plus",
            name = "Static+ Rank Pack",
            category = StoreCategory.STATIC_PLUS,
            priceDisplay = "₹89",
            badge = "ELITE",
            description = "High-voltage elite rank with death heart retention and supreme combat utility.",
            contents = listOf(
                "Permanent [Static+] in-game & Discord prefix",
                "+15,000 Land Claim Protection blocks",
                "1x Guaranteed Heart Retention on PvP death",
                "3x Vault storage pages (/pv 1-3)",
                "Access to /feed and /condense commands"
            ),
            accentColor = Color(0xFF38BDF8)
        ),
        StorePack(
            id = "rank_chaos",
            name = "Chaos Rank Pack",
            category = StoreCategory.CHAOS,
            priceDisplay = "₹149",
            badge = "SUPREME",
            description = "The ultimate apex rank pack in TGZ Lifestyle. Maximum claim limits, vault storage, and VIP status.",
            contents = listOf(
                "Permanent [Chaos] in-game & Discord prefix",
                "+25,000 Land Claim Protection blocks",
                "Priority queue access & lifestyle perks",
                "5x Vault storage pages (/pv 1-5)",
                "All VIP commands + priority Discord support",
                "Exclusive monthly Chaos Champion Mystery crate"
            ),
            accentColor = NetherRed
        )
    )

    val faqs: List<FaqItem> = listOf(
        FaqItem(
            id = "faq_join",
            category = "Getting Started",
            question = "How do I join the TGZ Lifestyle Minecraft server?",
            answer = "Open Minecraft Java Edition (Port 10010) or Bedrock Edition (Port 10011), add TGZ Lifestyle with address 'node2.vyroxsmp.online', then save and connect."
        ),
        FaqItem(
            id = "faq_lifesteal",
            category = "Gameplay",
            question = "How do Lifestyle community perks and claims work?",
            answer = "Enjoy peaceful and adventurous survival with safe land claim protection, player trading, collaborative teams, and community milestones."
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
