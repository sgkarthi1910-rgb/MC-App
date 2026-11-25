package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ServerRank
import com.example.data.StoreCategory
import com.example.data.StorePack
import com.example.data.TgzCatalogData
import com.example.ui.components.ObsidianCard
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MinecraftGold
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.OnEmeraldDark
import com.example.ui.theme.SilkscreenFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun StoreScreen(
    selectedCategory: StoreCategory = StoreCategory.ALL,
    onSelectCategory: (StoreCategory) -> Unit = {},
    showServerRanks: Boolean = false,
    onBuyOnDiscord: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeCategory by remember { mutableStateOf(selectedCategory) }
    val ranks = TgzCatalogData.ranks
    val packs: List<StorePack> = TgzCatalogData.storePacks

    val filteredPacks = when (activeCategory) {
        StoreCategory.ALL -> packs
        else -> packs.filter { it.category == activeCategory }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Category Filter Row
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(StoreCategory.entries.toTypedArray()) { category ->
                        val isSelected = activeCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                activeCategory = category
                                onSelectCategory(category)
                            },
                            label = {
                                Text(
                                    text = category.label.uppercase(),
                                    fontFamily = SilkscreenFontFamily,
                                    fontSize = 11.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = DeepCharcoal,
                                labelColor = TextMuted,
                                selectedContainerColor = EmeraldGreen.copy(alpha = 0.2f),
                                selectedLabelColor = EmeraldGreen
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = ObsidianBorder,
                                selectedBorderColor = EmeraldGreen
                            )
                        )
                    }
                }
            }

            // Packs in Store (VIP Rank Packs: Titan, Wizard, King, Static+, Chaos)
            items(filteredPacks, key = { it.id }) { pack ->
                ShopPackCard(
                    pack = pack,
                    onBuyClick = { onBuyOnDiscord(pack.name) }
                )
            }

            // If showServerRanks is true (e.g. from RanksScreen), show server ranks
            if (showServerRanks) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ADDITIONAL SERVER RANKS",
                        fontFamily = SilkscreenFontFamily,
                        fontSize = 14.sp,
                        color = MinecraftGold,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                items(ranks, key = { it.id }) { rank ->
                    ShopRankCard(
                        rank = rank,
                        onBuyClick = { onBuyOnDiscord(rank.name) }
                    )
                }
            }
        }
    }
}

@Composable
fun ShopPackCard(
    pack: StorePack,
    onBuyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ObsidianCard(
        borderColor = pack.accentColor.copy(alpha = 0.5f),
        accentGlow = pack.accentColor,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = pack.accentColor, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = pack.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = SilkscreenFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = pack.accentColor
                    )
                }

                if (pack.badge != null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = pack.accentColor.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, pack.accentColor)
                    ) {
                        Text(
                            text = pack.badge,
                            fontFamily = SilkscreenFontFamily,
                            fontSize = 9.sp,
                            color = pack.accentColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Text(
                text = pack.description,
                fontSize = 12.sp,
                color = TextSecondary
            )

            // Contents checklist
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                pack.contents.forEach { line ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = pack.accentColor, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = line, fontSize = 11.sp, color = TextPrimary, fontFamily = JetBrainsMonoFontFamily)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = pack.priceDisplay,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 13.sp,
                    color = MinecraftGold,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = onBuyClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = pack.accentColor,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "GET ON DISCORD",
                        fontFamily = SilkscreenFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ShopRankCard(
    rank: ServerRank,
    onBuyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        label = "card_scale"
    )

    val buttonContentColor = when (rank.id) {
        "rank_titan", "rank_king", "rank_static_plus" -> Color(0xFF0F172A)
        else -> Color.White
    }

    ObsidianCard(
        borderColor = rank.accentColor.copy(alpha = 0.5f),
        accentGlow = rank.accentColor,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .testTag("shop_rank_card_${rank.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = rank.name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = SilkscreenFontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = rank.accentColor
                )
                Text(
                    text = if (rank.price.startsWith("₹")) rank.price else "₹${rank.price}",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = rank.accentColor
                )
            }

            Button(
                onClick = onBuyClick,
                interactionSource = interactionSource,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = rank.accentColor,
                    contentColor = buttonContentColor
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 2.dp
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                modifier = Modifier.testTag("buy_button_${rank.id}")
            ) {
                Text(
                    text = "BUY NOW",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = SilkscreenFontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                )
            }
        }
    }
}
