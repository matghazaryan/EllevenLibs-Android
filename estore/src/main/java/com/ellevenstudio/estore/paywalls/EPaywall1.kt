package com.ellevenstudio.estore.paywalls

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ellevenstudio.estore.EStore
import com.ellevenstudio.estore.EStoreTheme
import com.ellevenstudio.estore.paywalls.components.*

/**
 * Minimal paywall: Crown icon, "Go Premium" title, 3 feature rows,
 * vertical product cards, CTA button, restore.
 */
@Composable
fun EPaywall1(
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

    val bgColor = data.theme.backgroundColor
    val textColor = data.theme.textColor

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = bgColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Close button
            if (onDismiss != null) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    EPaywallCloseButton(theme = data.theme, onClick = onDismiss)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Crown icon
            Text("\uD83D\uDC51", style = MaterialTheme.typography.displayLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))

            // Title
            Text(
                "Go Premium",
                style = MaterialTheme.typography.headlineMedium,
                color = textColor,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // Feature rows
            data.features.forEach { feature ->
                EPaywallFeatureRow(icon = feature.icon, title = feature.title, subtitle = feature.subtitle, theme = data.theme)
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(32.dp))

            // Product cards
            products.forEach { product ->
                EPaywallProductCard(
                    product = product,
                    isSelected = product.id == selectedId,
                    theme = data.theme,
                    onClick = { selectedId = product.id }
                )
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(16.dp))

            // CTA
            EPaywallCTAButton(
                title = "Subscribe Now",
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

            // Restore
            EPaywallRestoreButton(theme = data.theme) { EStore.restore() }

            Spacer(Modifier.height(16.dp))
        }
    }
}
