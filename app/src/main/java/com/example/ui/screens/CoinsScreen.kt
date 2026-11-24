package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ObsidianCard
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.GoldGlow
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MinecraftGold
import com.example.ui.theme.NetherRed
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianBorderSubtle
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.SilkscreenFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.roundToInt

private const val MIN_COINS = 25
private const val MAX_COINS = 10_000
private const val PRICE_PER_COIN = 0.08

private val QUICK_SELECT_AMOUNTS = listOf(250, 500, 1_000, 2_500, 5_000, 10_000)

@Composable
fun CoinsScreen(
    onBuyCoins: (Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var sliderValue by remember { mutableFloatStateOf(MIN_COINS.toFloat()) }
    val selectedCoins = remember(sliderValue) {
        val raw = ((sliderValue / 25f).roundToInt() * 25)
        raw.coerceIn(MIN_COINS, MAX_COINS)
    }

    val totalPrice = remember(selectedCoins) {
        selectedCoins * PRICE_PER_COIN
    }

    val formattedPrice = remember(totalPrice) {
        if (totalPrice % 1.0 == 0.0) {
            String.format(Locale.US, "₹%,d", totalPrice.toLong())
        } else {
            String.format(Locale.US, "₹%,.2f", totalPrice)
        }
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
                .widthIn(max = 940.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Heading & Subtitle
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val heading = buildAnnotatedString {
                        append("Choose your ")
                        withStyle(SpanStyle(color = MinecraftGold)) {
                            append("coin reserve.")
                        }
                    }

                    Text(
                        text = heading,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = SilkscreenFontFamily,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Set your amount. See your price. Complete your purchase with our team.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = SpaceGroteskFontFamily
                        ),
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Main Purchase Panel
            item {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val isTwoColumn = maxWidth >= 720.dp

                    ObsidianCard(
                        borderColor = MinecraftGold.copy(alpha = 0.4f),
                        accentGlow = MinecraftGold,
                        contentPadding = PaddingValues(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isTwoColumn) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(28.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(modifier = Modifier.weight(1.1f)) {
                                    CoinSelectionSection(
                                        selectedCoins = selectedCoins,
                                        sliderValue = sliderValue,
                                        onSliderChange = { sliderValue = it },
                                        onQuickSelect = { sliderValue = it.toFloat() }
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(380.dp)
                                        .background(ObsidianBorderSubtle)
                                )

                                Box(modifier = Modifier.weight(0.9f)) {
                                    PriceSummarySection(
                                        selectedCoins = selectedCoins,
                                        formattedPrice = formattedPrice,
                                        onBuyClick = { onBuyCoins(selectedCoins, formattedPrice) }
                                    )
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                CoinSelectionSection(
                                    selectedCoins = selectedCoins,
                                    sliderValue = sliderValue,
                                    onSliderChange = { sliderValue = it },
                                    onQuickSelect = { sliderValue = it.toFloat() }
                                )

                                HorizontalDivider(color = ObsidianBorderSubtle)

                                PriceSummarySection(
                                    selectedCoins = selectedCoins,
                                    formattedPrice = formattedPrice,
                                    onBuyClick = { onBuyCoins(selectedCoins, formattedPrice) }
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Information Cards
            item {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val isWide = maxWidth >= 720.dp

                    if (isWide) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            InfoCard(
                                icon = Icons.Default.Sell,
                                title = "Clear pricing",
                                description = "₹0.08 per coin, every time.",
                                modifier = Modifier.weight(1f)
                            )
                            InfoCard(
                                icon = Icons.Default.AutoAwesome,
                                title = "Built for your run",
                                description = "Choose only what you need.",
                                modifier = Modifier.weight(1f)
                            )
                            InfoCard(
                                icon = Icons.Default.Forum,
                                title = "Discord tickets",
                                description = "Our team helps complete your purchase.",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            InfoCard(
                                icon = Icons.Default.Sell,
                                title = "Clear pricing",
                                description = "₹0.08 per coin, every time.",
                                modifier = Modifier.fillMaxWidth()
                            )
                            InfoCard(
                                icon = Icons.Default.AutoAwesome,
                                title = "Built for your run",
                                description = "Choose only what you need.",
                                modifier = Modifier.fillMaxWidth()
                            )
                            InfoCard(
                                icon = Icons.Default.Forum,
                                title = "Discord tickets",
                                description = "Our team helps complete your purchase.",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CoinSelectionSection(
    selectedCoins: Int,
    sliderValue: Float,
    onSliderChange: (Float) -> Unit,
    onQuickSelect: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "YOUR SELECTION",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            ),
            color = MinecraftGold
        )
        Text(
            text = "Build your reserve",
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.SemiBold
            ),
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Selected Coin Amount Display with Gold Coin Icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(ObsidianBlack.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                .border(BorderStroke(1.dp, ObsidianBorder), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(MinecraftGold.copy(alpha = 0.28f), Color.Transparent)
                        )
                    )
                    .border(BorderStroke(1.5.dp, MinecraftGold), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MonetizationOn,
                    contentDescription = "Coin",
                    tint = MinecraftGold,
                    modifier = Modifier.size(26.dp)
                )
            }

            Column {
                Text(
                    text = String.format(Locale.US, "%,d", selectedCoins),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MinecraftGold
                )
                Text(
                    text = "COINS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = SilkscreenFontFamily,
                        letterSpacing = 1.sp
                    ),
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Amount Slider
        Slider(
            value = sliderValue,
            onValueChange = onSliderChange,
            valueRange = MIN_COINS.toFloat()..MAX_COINS.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = MinecraftGold,
                activeTrackColor = MinecraftGold,
                inactiveTrackColor = ObsidianBorder
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("coins_slider")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "25",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = JetBrainsMonoFontFamily
                ),
                color = TextMuted
            )
            Text(
                text = "10,000",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = JetBrainsMonoFontFamily
                ),
                color = TextMuted
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // QUICK SELECT
        Text(
            text = "QUICK SELECT",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(10.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QUICK_SELECT_AMOUNTS.forEach { amount ->
                val isSelected = selectedCoins == amount
                QuickSelectChip(
                    label = String.format(Locale.US, "%,d", amount),
                    isSelected = isSelected,
                    onClick = { onQuickSelect(amount) }
                )
            }
        }
    }
}

@Composable
private fun QuickSelectChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MinecraftGold.copy(alpha = 0.22f) else DeepCharcoal,
        border = BorderStroke(
            1.dp,
            if (isSelected) MinecraftGold else ObsidianBorder
        ),
        modifier = Modifier.testTag("quick_select_$label")
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (isSelected) MinecraftGold else TextPrimary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun PriceSummarySection(
    selectedCoins: Int,
    formattedPrice: String,
    onBuyClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        label = "buy_scale"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "TOTAL PRICE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                ),
                color = MinecraftGold
            )
            Text(
                text = formattedPrice,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = MinecraftGold
            )
            Text(
                text = String.format(Locale.US, "%,d Coins selected", selectedCoins),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = SpaceGroteskFontFamily
                ),
                color = TextSecondary
            )
        }

        // Breakdown Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ObsidianBlack.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                .border(BorderStroke(1.dp, ObsidianBorder), RoundedCornerShape(12.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Exchange rate",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = "25 Coins = ₹2",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = TextPrimary
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Price per coin",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = "₹0.08 / coin",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MinecraftGold
                )
            }
        }

        // Red Buy Coins Button
        Button(
            onClick = onBuyClick,
            interactionSource = interactionSource,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NetherRed,
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 2.dp,
                pressedElevation = 4.dp
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .scale(scale)
                .testTag("buy_coins_button")
        ) {
            Text(
                text = "BUY COINS",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = SilkscreenFontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )
        }

        // Security / Shield Note
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = MinecraftGold,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Purchases are completed through Discord tickets.",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = SpaceGroteskFontFamily
                ),
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun InfoCard(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    ObsidianCard(
        borderColor = ObsidianBorder,
        accentGlow = MinecraftGold,
        contentPadding = PaddingValues(16.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MinecraftGold.copy(alpha = 0.14f))
                    .border(BorderStroke(1.dp, MinecraftGold.copy(alpha = 0.45f)), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MinecraftGold,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = TextPrimary
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}
