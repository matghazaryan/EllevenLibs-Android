package com.ellevenstudio.estore.paywalls

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ellevenstudio.estore.EStore
import com.ellevenstudio.estore.EStoreTheme
import com.ellevenstudio.estore.paywalls.components.*

/**
 * Bottom sheet compact: Handle bar at top, "Go Premium" title,
 * horizontal scrollable product chips, CTA, restore.
 */
@Composable
fun EPaywall9(
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
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Spacer(Modifier.weight(1f))

            // Bottom sheet card
            val cardBg = data.theme.cardBackgroundColor

            Surface(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = cardBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Handle bar
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(data.theme.secondaryTextColor.copy(alpha = 0.3f))
                    )

                    Spacer(Modifier.height(20.dp))

                    if (onDismiss != null) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            EPaywallCloseButton(theme = data.theme, onClick = onDismiss)
                        }
                    }

                    Text(
                        "Go Premium",
                        style = MaterialTheme.typography.headlineSmall,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Choose a plan to get started",
                        style = MaterialTheme.typography.bodyMedium,
                        color = data.theme.secondaryTextColor,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(20.dp))

                    // Horizontal scrollable product chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        products.forEach { product ->
                            val isSelected = product.id == selectedId
                            val chipBg = if (isSelected) data.theme.primaryColor else data.theme.cardBackgroundColor
                            val chipText = if (isSelected) data.theme.buttonTextColor else textColor

                            Surface(
                                modifier = Modifier.clickable { selectedId = product.id },
                                shape = RoundedCornerShape(data.theme.cornerRadius),
                                color = chipBg
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(product.localizedTitle, style = MaterialTheme.typography.labelLarge, color = chipText)
                                    Spacer(Modifier.height(4.dp))
                                    Text(product.displayPrice, style = MaterialTheme.typography.titleSmall, color = chipText)
                                    product.subscriptionPeriod?.let {
                                        Text(it, style = MaterialTheme.typography.bodySmall, color = chipText.copy(alpha = 0.7f))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    EPaywallCTAButton(
                        title = "Subscribe",
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
    }
}
