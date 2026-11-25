package com.example

import com.example.data.StoreCategory
import com.example.data.TgzCatalogData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TgzRewardsAndLeaderboardTest {

    @Test
    fun `test store packs contain ONLY the 5 VIP rank packs and no heart, crate or revival packs`() {
        val storePacks = TgzCatalogData.storePacks

        // Exactly 5 rank packs in store: Titan, Wizard, King, Static+, Chaos
        assertEquals(5, storePacks.size)

        val expectedRankPackIds = listOf(
            "rank_titan",
            "rank_wizard",
            "rank_king",
            "rank_static_plus",
            "rank_chaos"
        )
        assertEquals(expectedRankPackIds, storePacks.map { it.id })

        val titanPack = storePacks[0]
        assertEquals("Titan Rank Pack", titanPack.name)
        assertEquals(StoreCategory.TITAN, titanPack.category)
        assertEquals("TIER 1", titanPack.badge)
        assertEquals("₹49", titanPack.priceDisplay)

        val wizardPack = storePacks[1]
        assertEquals("Wizard Rank Pack", wizardPack.name)
        assertEquals(StoreCategory.WIZARD, wizardPack.category)
        assertEquals("TIER 2", wizardPack.badge)
        assertEquals("₹59", wizardPack.priceDisplay)

        val kingPack = storePacks[2]
        assertEquals("King Rank Pack", kingPack.name)
        assertEquals(StoreCategory.KING, kingPack.category)
        assertEquals("POPULAR", kingPack.badge)
        assertEquals("₹79", kingPack.priceDisplay)

        val staticPack = storePacks[3]
        assertEquals("Static+ Rank Pack", staticPack.name)
        assertEquals(StoreCategory.STATIC_PLUS, staticPack.category)
        assertEquals("ELITE", staticPack.badge)
        assertEquals("₹89", staticPack.priceDisplay)

        val chaosPack = storePacks[4]
        assertEquals("Chaos Rank Pack", chaosPack.name)
        assertEquals(StoreCategory.CHAOS, chaosPack.category)
        assertEquals("SUPREME", chaosPack.badge)
        assertEquals("₹149", chaosPack.priceDisplay)

        // Ensure heart packs, crate packs, and revival packs are completely removed from store
        assertFalse(storePacks.any { it.name.contains("Heart", ignoreCase = true) })
        assertFalse(storePacks.any { it.name.contains("Crate", ignoreCase = true) })
        assertFalse(storePacks.any { it.name.contains("Revival", ignoreCase = true) })
        assertFalse(storePacks.any { it.name.contains("Starter", ignoreCase = true) })
    }

    @Test
    fun `test server ranks remain preserved`() {
        // Server ranks must be preserved
        assertTrue(TgzCatalogData.ranks.isNotEmpty())
        assertEquals(5, TgzCatalogData.ranks.size)
        assertEquals(listOf("rank_titan", "rank_wizard", "rank_king", "rank_static_plus", "rank_chaos"), TgzCatalogData.ranks.map { it.id })
    }
}
