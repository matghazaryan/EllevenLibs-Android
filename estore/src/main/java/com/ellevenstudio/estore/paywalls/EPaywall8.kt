package com.ellevenstudio.estore.paywalls

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ellevenstudio.estore.EStore
import com.ellevenstudio.estore.EStoreTheme
import com.ellevenstudio.estore.paywalls.components.*

/**
 * Gradient stacked chips: Subtle gradient bg, "Upgrade Today" with wand icon,
 * products as pill-shaped rows, CTA.
 */
@Composable
fun EPaywall8(
    activity: Activity,
    theme: EStoreTheme? = null,
    onDismiss: (() -> Unit)? = null
) {
    val data = remember { EPaywallData(theme) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val products = data.premiumProducts

    LaunchedEffect(products) {
        if (selectedId == null && products.isNotEmpty()) {
            selectedId = products.first().id
        }
    }

    val bgTop = data.theme.primaryColor.copy(alpha = 0.08f)
    val bgBottom = data.theme.backgroundColor
    val textColor = data.theme.textColor

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(bgTop, bgBottom)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (onDismiss != null) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    EPaywallCloseButton(theme = data.theme, onClick = onDismiss)
                }
            }

            Spacer(Modifier.height(32.dp))

            Text("\uD83E\uDE84", style = MaterialTheme.typography.displayLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))

            Text(
                "Upgrade Today",
                style = MaterialTheme.typography.headlineMedium,
                color = textColor,
                textAlign = TextAlign.Center
            )
            Text(
                "Unlock the full experience",
                style = MaterialTheme.typography.bodyMedium,
                color = data.theme.secondaryTextColor,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // Pill-shaped product rows
            products.forEach { product ->
                val isSelected = product.id == selectedId
                val pillBg = if (isSelected) data.theme.primaryColor else data.theme.cardBackgroundColor
                val pillText = if (isSelected) data.theme.buttonTextColor else textColor
                val pillSub = if (isSelected) data.theme.buttonTextColor.copy(alpha = 0.8f) else data.theme.secondaryTextColor

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedId = product.id },
                    shape = RoundedCornerShape(50),
                    color = pillBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(product.localizedTitle, style = MaterialTheme.typography.titleSmall, color = pillText)
                            product.subscriptionPeriod?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = pillSub)
                            }
                        }
                        Text(
                            product.displayPrice,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isSelected) data.theme.buttonTextColor else data.theme.primaryColor
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(24.dp))

            EPaywallCTAButton(
                title = "Upgrade Now",
                theme = data.theme,
                isLoading = isLoading,
                onClick = {
                    val id = selectedId ?: return@EPaywallCTAButton
                    isLoading = true
                    EStore.purchase(activity, id)
                    isLoading = false
                }
            )

            Spacer(Modifier.height(8.dp))
            EPaywallRestoreButton(theme = data.theme) { EStore.restore() }
        }
    }
}
