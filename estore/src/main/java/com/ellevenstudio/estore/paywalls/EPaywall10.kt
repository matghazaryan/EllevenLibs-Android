package com.ellevenstudio.estore.paywalls

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ellevenstudio.estore.EStore
import com.ellevenstudio.estore.EStoreProduct
import com.ellevenstudio.estore.EStoreProductType
import com.ellevenstudio.estore.EStoreTheme
import com.ellevenstudio.estore.paywalls.components.*

/**
 * Consumable coin grid: "Store" title, balance display card,
 * LazyVerticalGrid of 2 columns showing consumable products with individual buy buttons.
 */
@Composable
fun EPaywall10(
    activity: Activity,
    theme: EStoreTheme? = null,
    onDismiss: (() -> Unit)? = null
) {
    val data = remember { EPaywallData(theme) }
    val consumables = data.consumables

    val bgColor = data.theme.backgroundColor
    val textColor = data.theme.textColor
    val subColor = data.theme.secondaryTextColor
    val cardBg = data.theme.cardBackgroundColor

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = bgColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Store",
                    style = MaterialTheme.typography.headlineMedium,
                    color = textColor
                )
                if (onDismiss != null) {
                    EPaywallCloseButton(theme = data.theme, onClick = onDismiss)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Balance display card
            Surface(
                shape = RoundedCornerShape(data.theme.cornerRadius),
                color = data.theme.primaryColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Your Balance",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(Modifier.height(8.dp))
                    // Show total balance across all consumables
                    val totalBalance = consumables.sumOf { EStore.consumableBalance(it.id) }
                    Text(
                        "\uD83E\uDE99 $totalBalance",
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Grid of consumable products
            if (consumables.isEmpty()) {
                Text(
                    "No items available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = subColor,
                    textAlign = TextAlign.Center
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(consumables) { product ->
                        ConsumableCard(
                            product = product,
                            theme = data.theme,
                            cardBg = cardBg,
                            textColor = textColor,
                            subColor = subColor,
                            onBuy = {
                                EStore.purchase(activity, product.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsumableCard(
    product: EStoreProduct,
    theme: EStoreTheme,
    cardBg: Color,
    textColor: Color,
    subColor: Color,
    onBuy: () -> Unit
) {
    val amount = (product.type as? EStoreProductType.Consumable)?.amount ?: 0

    Surface(
        shape = RoundedCornerShape(theme.cornerRadius),
        color = cardBg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("\uD83E\uDE99", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                product.localizedTitle,
                style = MaterialTheme.typography.titleSmall,
                color = textColor,
                textAlign = TextAlign.Center
            )
            Text(
                "+$amount",
                style = MaterialTheme.typography.bodySmall,
                color = subColor
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onBuy,
                shape = RoundedCornerShape(theme.cornerRadius),
                colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(product.displayPrice, color = Color.White)
            }
        }
    }
}
