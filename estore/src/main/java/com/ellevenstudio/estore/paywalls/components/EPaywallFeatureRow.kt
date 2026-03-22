package com.ellevenstudio.estore.paywalls.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ellevenstudio.estore.EStoreTheme

@Composable
internal fun EPaywallFeatureRow(
    icon: String = "\u2713",
    title: String,
    subtitle: String? = null,
    theme: EStoreTheme
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(icon, color = theme.primaryColor, style = MaterialTheme.typography.titleMedium)
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = theme.textColor)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = theme.secondaryTextColor)
            }
        }
    }
}
