package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.StoreCategory
import com.example.data.StorePack
import com.example.data.TgzCatalogData
import com.example.ui.components.DiscordCheckoutBanner
import com.example.ui.components.ObsidianCard
import com.example.ui.components.PvpArenaBannerCanvas
import com.example.ui.components.SectionHeading
import com.example.ui.components.VoxelHeartIcon
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.MinecraftGold
import com.example.ui.theme.NetherRed
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.OnEmeraldDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun StoreScreen(
    selectedCategory: StoreCategory,
    onSelectCategory: (StoreCategory) -> Unit,
    onBuyOnDiscord: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val visiblePacks = remember(selectedCategory) {
        if (selectedCategory == StoreCategory.ALL) {
            TgzCatalogData.storePacks
        } else {
            TgzCatalogData.storePacks.filter { it.category == selectedCategory }
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                val shape = RoundedCornerShape(16.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .border(BorderStroke(1.dp, NetherRed.copy(alpha = 0.4f)), shape)
                ) {
                    PvpArenaBannerCanvas(
                        accentColor = NetherRed,
                        modifier = Modifier.matchParentSize()
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        SectionHeading(
                            title = "TGZ STORE PACKS",
                            subtitle = "Browse Heart Packs, Crate Keys, and Revival Bundles — tap Buy to order directly on Discord.",
                            accentColor = NetherRed
                        )
                    }
                }
            }

            item {
                DiscordCheckoutBanner(
                    onOpenDiscord = { onBuyOnDiscord("Store Ticket") }
                )
            }

            // CATEGORY FILTER CHIPS
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(StoreCategory.entries) { category ->
                        val selected = category == selectedCategory
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectCategory(category) },
                            label = {
                                Text(
                                    text = category.label,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = DeepCharcoal,
                                labelColor = TextSecondary,
                                selectedContainerColor = EmeraldGreen.copy(alpha = 0.2f),
                                selectedLabelColor = EmeraldGreen
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = ObsidianBorder,
                                selectedBorderColor = EmeraldGreen
                            ),
                            modifier = Modifier.testTag("store_filter_${category.name.lowercase()}")
                        )
                    }
                }
            }

            items(visiblePacks, key = { it.id }) { pack ->
                StorePackCard(
                    pack = pack,
                    onBuyClick = { onBuyOnDiscord(pack.name) }
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun StorePackCard(
    pack: StorePack,
    onBuyClick: () -> Unit
) {
    ObsidianCard(
        borderColor = pack.accentColor.copy(alpha = 0.38f),
        accentGlow = pack.accentColor,
        modifier = Modifier.testTag("store_pack_${pack.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(pack.accentColor.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                        .border(1.dp, pack.accentColor.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    when (pack.category) {
                        StoreCategory.HEARTS -> VoxelHeartIcon(tint = NetherRed, iconSize = 20.dp)
                        StoreCategory.CRATES -> Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = pack.name,
                            tint = pack.accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        else -> Icon(
                            imageVector = Icons.Default.CardGiftcard,
                            contentDescription = pack.name,
                            tint = pack.accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = pack.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = pack.category.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = pack.accentColor
                    )
                }
            }

            if (!pack.badge.isNullOrBlank()) {
                Surface(
                    color = pack.accentColor.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, pack.accentColor.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = pack.badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = pack.accentColor,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = pack.description,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ObsidianBlack.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            pack.contents.forEach { item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = pack.accentColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = onBuyClick,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = pack.accentColor,
                contentColor = when (pack.accentColor) {
                    NetherRed -> Color.White
                    MinecraftGold -> Color(0xFF1F1604)
                    else -> OnEmeraldDark
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("buy_pack_${pack.id}")
        ) {
            Icon(
                imageVector = Icons.Default.Forum,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.action_buy_on_discord),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
