package com.ellevenstudio.estore.paywalls.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.ellevenstudio.estore.EStoreTheme

@Composable
internal fun EPaywallCloseButton(theme: EStoreTheme, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Text(
            "\u2715",
            style = MaterialTheme.typography.titleLarge,
            color = theme.secondaryTextColor
        )
    }
}
