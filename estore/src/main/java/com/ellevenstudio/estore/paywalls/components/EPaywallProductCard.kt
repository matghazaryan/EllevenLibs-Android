package com.ellevenstudio.estore.paywalls.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ellevenstudio.estore.EStoreProduct
import com.ellevenstudio.estore.EStoreTheme

@Composable
internal fun EPaywallProductCard(
    product: EStoreProduct,
    isSelected: Boolean,
    theme: EStoreTheme,
    onClick: () -> Unit
) {
    val bg = if (isSelected) theme.primaryColor else theme.cardBackgroundColor
    val textCol = if (isSelected) theme.buttonTextColor else theme.textColor
    val subCol = if (isSelected) theme.buttonTextColor.copy(alpha = 0.8f) else theme.secondaryTextColor

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(theme.cornerRadius),
        color = bg
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(product.localizedTitle, style = MaterialTheme.typography.titleSmall, color = textCol)
            product.subscriptionPeriod?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = subCol)
            }
            Text(product.localizedDescription, style = MaterialTheme.typography.bodySmall, color = subCol, maxLines = 2)
            Spacer(Modifier.height(8.dp))
            Text(
                product.displayPrice,
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected) theme.buttonTextColor else theme.primaryColor
            )
        }
    }
}
