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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SportsKabaddi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.FeatureIconType
import com.example.data.ServerFeature
import com.example.data.TgzCatalogData
import com.example.ui.components.ObsidianCard
import com.example.ui.components.PvpArenaBannerCanvas
import com.example.ui.components.SectionHeading
import com.example.ui.components.VoxelHeartIcon
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NetherRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun FeaturesScreen(
    modifier: Modifier = Modifier
) {
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
                        .border(BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.35f)), shape)
                ) {
                    PvpArenaBannerCanvas(
                        accentColor = EmeraldGreen,
                        modifier = Modifier.matchParentSize()
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SectionHeading(
                            title = "SERVER FEATURES",
                            subtitle = "Engineered for survival adventures, clan collaboration, and long-term community empires.",
                            accentColor = EmeraldGreen
                        )
                    }
                }
            }

            items(TgzCatalogData.features, key = { it.id }) { feature ->
                FeatureDetailCard(feature = feature)
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun FeatureDetailCard(feature: ServerFeature) {
    ObsidianCard(
        borderColor = feature.accentColor.copy(alpha = 0.34f),
        accentGlow = feature.accentColor,
        modifier = Modifier.testTag("feature_card_${feature.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(feature.accentColor.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                    .border(1.dp, feature.accentColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (feature.iconType == FeatureIconType.LIFESTEAL) {
                    VoxelHeartIcon(tint = NetherRed, iconSize = 22.dp)
                } else {
                    Icon(
                        imageVector = featureIconVector(feature.iconType),
                        contentDescription = feature.title,
                        tint = feature.accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = feature.title.uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = feature.subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = feature.accentColor
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = feature.description,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            feature.highlights.forEach { bullet ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = feature.accentColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = bullet,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

private fun featureIconVector(type: FeatureIconType): ImageVector = when (type) {
    FeatureIconType.LIFESTEAL -> Icons.Default.Favorite
    FeatureIconType.PVP -> Icons.Default.SportsKabaddi
    FeatureIconType.LAND_CLAIM -> Icons.Default.Security
    FeatureIconType.SURVIVAL -> Icons.Default.Explore
    FeatureIconType.COMMUNITY -> Icons.Default.Groups
    FeatureIconType.ECONOMY -> Icons.Default.AccountBalance
}
