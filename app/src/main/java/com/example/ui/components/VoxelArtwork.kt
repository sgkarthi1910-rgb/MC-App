package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.MinecraftGold
import com.example.ui.theme.NetherRed
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianElevated
import kotlin.math.PI
import kotlin.math.sin

private data class PixelEmber(
    val xFraction: Float,
    val yOffset: Float,
    val speed: Float,
    val sizeRatio: Float,
    val color: Color
)

private val HERO_EMBERS = listOf(
    PixelEmber(0.08f, 0.15f, 1.0f, 0.011f, EmeraldGreen),
    PixelEmber(0.17f, 0.72f, 0.7f, 0.009f, NetherRed),
    PixelEmber(0.26f, 0.40f, 1.2f, 0.012f, EmeraldGreen),
    PixelEmber(0.38f, 0.85f, 0.85f, 0.008f, MinecraftGold),
    PixelEmber(0.51f, 0.25f, 1.05f, 0.010f, NetherRed),
    PixelEmber(0.63f, 0.62f, 0.9f, 0.012f, EmeraldGreen),
    PixelEmber(0.75f, 0.18f, 1.15f, 0.009f, NetherRed),
    PixelEmber(0.84f, 0.78f, 0.75f, 0.011f, EmeraldGreen),
    PixelEmber(0.92f, 0.48f, 1.0f, 0.008f, MinecraftGold)
)

/**
 * Custom Minecraft Lifesteal atmospheric hero canvas:
 * Obsidian citadel monoliths, emerald energy fissures, Nether dusk haze,
 * gently floating voxel Lifesteal heart, and drifting pixel embers.
 */
@Composable
fun HeroLifestealCanvas(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hero_lifesteal_motion")
    val driftProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ember_drift"
    )
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "heart_pulse"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val pulse = (sin(pulsePhase) + 1f) * 0.5f

        // 1. Deep Obsidian + Subtle Emerald/Nether Atmospheric Sky
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0A1410),
                    Color(0xFF101916),
                    ObsidianBlack
                )
            )
        )

        // Subtle emerald glow behind the altar
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    EmeraldGreen.copy(alpha = 0.18f + 0.06f * pulse),
                    EmeraldGreen.copy(alpha = 0.04f),
                    Color.Transparent
                ),
                center = Offset(w * 0.76f, h * 0.44f),
                radius = w * 0.42f
            ),
            radius = w * 0.42f,
            center = Offset(w * 0.76f, h * 0.44f)
        )

        // Subtle Nether crimson aura on upper-left horizon
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    NetherRed.copy(alpha = 0.13f),
                    Color.Transparent
                ),
                center = Offset(w * 0.18f, h * 0.28f),
                radius = w * 0.36f
            ),
            radius = w * 0.36f,
            center = Offset(w * 0.18f, h * 0.28f)
        )

        // 2. Subtle Voxel Stone Grid Texture
        val gridStep = (w / 18f).coerceAtLeast(18f)
        var gx = 0f
        while (gx < w) {
            drawLine(
                color = Color.White.copy(alpha = 0.025f),
                start = Offset(gx, 0f),
                end = Offset(gx, h),
                strokeWidth = 1f
            )
            gx += gridStep
        }
        var gy = 0f
        while (gy < h) {
            drawLine(
                color = Color.White.copy(alpha = 0.025f),
                start = Offset(0f, gy),
                end = Offset(w, gy),
                strokeWidth = 1f
            )
            gy += gridStep
        }

        // 3. Obsidian Citadel Monoliths (Minecraft stepped silhouette)
        val pillarColor = Color(0xFF0C1210)
        val pillarHighlight = Color(0xFF1B2B24)

        fun drawObsidianPillar(leftFrac: Float, widthFrac: Float, topFrac: Float, veinColor: Color?) {
            val px = w * leftFrac
            val pw = w * widthFrac
            val py = h * topFrac
            val ph = h - py
            drawRect(color = pillarColor, topLeft = Offset(px, py), size = Size(pw, ph))
            // Top bevel highlight
            drawRect(
                color = pillarHighlight,
                topLeft = Offset(px, py),
                size = Size(pw, (h * 0.018f).coerceAtLeast(3f))
            )
            if (veinColor != null) {
                val blockSize = (pw * 0.22f).coerceAtLeast(4f)
                drawRect(
                    color = veinColor.copy(alpha = 0.45f + 0.2f * pulse),
                    topLeft = Offset(px + pw * 0.28f, py + ph * 0.22f),
                    size = Size(blockSize, blockSize)
                )
                drawRect(
                    color = veinColor.copy(alpha = 0.28f),
                    topLeft = Offset(px + pw * 0.52f, py + ph * 0.48f),
                    size = Size(blockSize * 0.8f, blockSize * 0.8f)
                )
            }
        }

        drawObsidianPillar(0.02f, 0.09f, 0.52f, NetherRed)
        drawObsidianPillar(0.11f, 0.07f, 0.68f, null)
        drawObsidianPillar(0.56f, 0.08f, 0.64f, EmeraldGreen)
        drawObsidianPillar(0.64f, 0.24f, 0.74f, EmeraldGreen) // Altar pedestal
        drawObsidianPillar(0.88f, 0.10f, 0.44f, EmeraldGreen)

        // 4. Floating Voxel Lifesteal Heart over the right-side Obsidian Altar
        val heartCenterX = w * 0.76f
        val heartCenterY = h * 0.43f + (sin(pulsePhase) * h * 0.022f)
        val unit = (minOf(w, h) * 0.036f).coerceIn(5f, 12f)

        // Heart glow halo
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    NetherRed.copy(alpha = 0.34f + 0.12f * pulse),
                    Color.Transparent
                ),
                center = Offset(heartCenterX, heartCenterY),
                radius = unit * 6.5f
            ),
            radius = unit * 6.5f,
            center = Offset(heartCenterX, heartCenterY)
        )

        // 9x8 pixel Minecraft heart matrix
        val heartRows = listOf(
            "011000110",
            "122101111",
            "123211111",
            "111111111",
            "011111110",
            "001111100",
            "000111000",
            "000010000"
        )
        val originX = heartCenterX - (9f * unit) / 2f
        val originY = heartCenterY - (8f * unit) / 2f
        heartRows.forEachIndexed { rowIdx, row ->
            row.forEachIndexed { colIdx, ch ->
                if (ch != '0') {
                    val cellColor = when (ch) {
                        '3' -> Color(0xFFFFD1D1)
                        '2' -> Color(0xFFFF6E6E)
                        else -> NetherRed
                    }
                    // Dark pixel outline shadow
                    drawRect(
                        color = Color(0xFF2B0808),
                        topLeft = Offset(originX + colIdx * unit - 1f, originY + rowIdx * unit - 1f),
                        size = Size(unit + 2f, unit + 2f)
                    )
                    drawRect(
                        color = cellColor,
                        topLeft = Offset(originX + colIdx * unit, originY + rowIdx * unit),
                        size = Size(unit - 0.5f, unit - 0.5f)
                    )
                }
            }
        }

        // Emerald Energy Ring Squares orbiting the altar
        val orbitOffset = (pulse - 0.5f) * unit * 1.4f
        drawRect(
            color = EmeraldGreen.copy(alpha = 0.8f),
            topLeft = Offset(heartCenterX - unit * 5.2f, heartCenterY + orbitOffset),
            size = Size(unit * 0.9f, unit * 0.9f)
        )
        drawRect(
            color = MinecraftGold.copy(alpha = 0.8f),
            topLeft = Offset(heartCenterX + unit * 4.5f, heartCenterY - orbitOffset),
            size = Size(unit * 0.85f, unit * 0.85f)
        )

        // 5. Drifting Subtle Pixel Embers
        HERO_EMBERS.forEach { ember ->
            val yFrac = ((ember.yOffset - driftProgress * ember.speed) % 1f + 1f) % 1f
            val alpha = sin(yFrac * PI).toFloat().coerceIn(0f, 1f) * 0.55f
            val pxSize = (w * ember.sizeRatio).coerceIn(3f, 8f)
            drawRect(
                color = ember.color.copy(alpha = alpha),
                topLeft = Offset(w * ember.xFraction, h * yFrac),
                size = Size(pxSize, pxSize)
            )
        }

        // 6. Bottom Vignette Seam into ObsidianBlack
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, ObsidianBlack.copy(alpha = 0.92f)),
                startY = h * 0.62f,
                endY = h
            )
        )
    }
}

/**
 * Atmospheric Minecraft PvP & Nether Banner for Features / Ranks / Store headers.
 */
@Composable
fun PvpArenaBannerCanvas(
    accentColor: Color = EmeraldGreen,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    DeepCharcoal,
                    ObsidianElevated,
                    Color(0xFF0A1410)
                )
            )
        )

        // Accent radial atmosphere
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accentColor.copy(alpha = 0.22f), Color.Transparent),
                center = Offset(w * 0.85f, h * 0.45f),
                radius = w * 0.45f
            ),
            radius = w * 0.45f,
            center = Offset(w * 0.85f, h * 0.45f)
        )

        // Stepped obsidian blocks along bottom edge
        val blockW = w / 12f
        val heights = listOf(0.22f, 0.34f, 0.18f, 0.28f, 0.14f, 0.20f, 0.15f, 0.30f, 0.42f, 0.26f, 0.38f, 0.24f)
        heights.forEachIndexed { idx, hf ->
            val bh = h * hf
            drawRect(
                color = ObsidianBlack.copy(alpha = 0.65f),
                topLeft = Offset(idx * blockW, h - bh),
                size = Size(blockW + 1f, bh)
            )
        }

        // Minimalist geometric voxel sword silhouette on right side
        val cx = w * 0.86f
        val cy = h * 0.50f
        val bladePath = Path().apply {
            moveTo(cx, cy - h * 0.34f)
            lineTo(cx + h * 0.08f, cy - h * 0.22f)
            lineTo(cx + h * 0.06f, cy + h * 0.16f)
            lineTo(cx - h * 0.06f, cy + h * 0.16f)
            lineTo(cx - h * 0.08f, cy - h * 0.22f)
            close()
        }
        drawPath(bladePath, color = accentColor.copy(alpha = 0.32f))
        // Crossguard
        drawRect(
            color = MinecraftGold.copy(alpha = 0.45f),
            topLeft = Offset(cx - h * 0.20f, cy + h * 0.14f),
            size = Size(h * 0.40f, h * 0.08f)
        )
        // Pommel
        drawRect(
            color = MinecraftGold.copy(alpha = 0.45f),
            topLeft = Offset(cx - h * 0.05f, cy + h * 0.22f),
            size = Size(h * 0.10f, h * 0.16f)
        )
    }
}

/**
 * Crisp Minecraft-style pixel heart icon drawn on a 7x6 voxel grid.
 */
@Composable
fun VoxelHeartIcon(
    tint: Color = NetherRed,
    iconSize: Dp = 18.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(iconSize)) {
        val unit = minOf(size.width / 7f, size.height / 6f)
        val ox = (size.width - unit * 7f) / 2f
        val oy = (size.height - unit * 6f) / 2f
        val rows = listOf(
            "0110110",
            "1211111",
            "1111111",
            "0111110",
            "0011100",
            "0001000"
        )
        rows.forEachIndexed { r, row ->
            row.forEachIndexed { c, ch ->
                if (ch != '0') {
                    drawRect(
                        color = if (ch == '2') Color.White.copy(alpha = 0.65f) else tint,
                        topLeft = Offset(ox + c * unit, oy + r * unit),
                        size = Size(unit, unit)
                    )
                }
            }
        }
    }
}
