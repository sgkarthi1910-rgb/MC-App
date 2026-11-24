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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.data.TgzCatalogData
import com.example.ui.components.ObsidianCard
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.OnEmeraldDark
import com.example.ui.theme.SilkscreenFontFamily
import com.example.ui.theme.TextPrimary

@Composable
fun StoreScreen(
    selectedCategory: StoreCategory = StoreCategory.ALL,
    onSelectCategory: (StoreCategory) -> Unit = {},
    onBuyOnDiscord: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val ranks = TgzCatalogData.ranks

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(ranks, key = { it.id }) { rank ->
                ShopRankCard(
                    rank = rank,
                    onBuyClick = { onBuyOnDiscord(rank.name) }
                )
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
