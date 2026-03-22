package com.ellevenstudio.estore.paywalls.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.ellevenstudio.estore.EStoreTheme

@Composable
internal fun EPaywallRestoreButton(theme: EStoreTheme, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(
            "Restore Purchases",
            style = MaterialTheme.typography.bodySmall,
            color = theme.secondaryTextColor
        )
    }
}
