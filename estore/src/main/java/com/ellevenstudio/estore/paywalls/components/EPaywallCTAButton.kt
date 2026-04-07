package com.ellevenstudio.estore.paywalls.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ellevenstudio.estore.EStoreTheme

@Composable
internal fun EPaywallCTAButton(
    title: String,
    theme: EStoreTheme,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(theme.cornerRadius),
        colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = theme.buttonTextColor,
                strokeWidth = 2.dp
            )
        } else {
            Text(title, color = theme.buttonTextColor, style = MaterialTheme.typography.titleMedium)
        }
    }
}
