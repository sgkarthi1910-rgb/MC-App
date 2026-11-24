package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun RanksScreen(
    onBuyOnDiscord: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    StoreScreen(
        onBuyOnDiscord = onBuyOnDiscord,
        modifier = modifier
    )
}
