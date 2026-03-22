package com.ellevenstudio.estore.paywalls

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ellevenstudio.estore.EStore
import com.ellevenstudio.estore.EStoreTheme
import com.ellevenstudio.estore.paywalls.components.*

/**
 * Feature list paywall: Star icon, "Premium Features" title, 6 feature rows,
 * horizontal ScrollRow of product chips below, CTA, restore.
 */
@Composable
fun EPaywall3(
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
            if (onDismiss != null) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    EPaywallCloseButton(theme = data.theme, onClick = onDismiss)
                }
            }

            Spacer(Modifier.height(16.dp))

            Text("\u2B50", style = MaterialTheme.typography.displayLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))

            Text(
                "Premium Features",
                style = MaterialTheme.typography.headlineMedium,
                color = textColor,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            // Feature rows
            data.features.forEach { feature ->
                EPaywallFeatureRow(icon = feature.icon, title = feature.title, subtitle = feature.subtitle, theme = data.theme)
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(24.dp))

            // Horizontal scroll row of product chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                products.forEach { product ->
                    val isSelected = product.id == selectedId
                    val chipBg = if (isSelected) data.theme.primaryColor else data.theme.cardBackgroundColor
                    val chipText = if (isSelected) Color.White else data.theme.textColor

                    Surface(
                        modifier = Modifier.clickable { selectedId = product.id },
                        shape = RoundedCornerShape(data.theme.cornerRadius),
                        color = chipBg
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(product.localizedTitle, style = MaterialTheme.typography.labelLarge, color = chipText)
                            Spacer(Modifier.height(4.dp))
                            Text(product.displayPrice, style = MaterialTheme.typography.titleSmall, color = chipText)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            EPaywallCTAButton(
                title = "Get Premium",
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
