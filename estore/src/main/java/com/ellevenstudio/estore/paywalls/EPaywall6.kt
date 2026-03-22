package com.ellevenstudio.estore.paywalls

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
 * Savings comparison: "Save More with Annual" title. Products as rows with
 * checkmark circle selection. Last product gets "SAVE" badge.
 */
@Composable
fun EPaywall6(
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
            selectedId = products.last().id
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

            Spacer(Modifier.height(24.dp))

            Text("\uD83D\uDCB0", style = MaterialTheme.typography.displayLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))

            Text(
                "Save More with Annual",
                style = MaterialTheme.typography.headlineMedium,
                color = textColor,
                textAlign = TextAlign.Center
            )
            Text(
                "Choose the plan that works for you",
                style = MaterialTheme.typography.bodyMedium,
                color = data.theme.secondaryTextColor,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // Product rows with checkmark circle
            products.forEachIndexed { index, product ->
                val isSelected = product.id == selectedId
                val isLast = index == products.lastIndex
                val cardBg = if (isSelected) data.theme.primaryColor.copy(alpha = 0.1f) else data.theme.cardBackgroundColor
                val textCol = data.theme.textColor
                val subCol = data.theme.secondaryTextColor

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedId = product.id },
                    shape = RoundedCornerShape(data.theme.cornerRadius),
                    color = cardBg,
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, data.theme.primaryColor) else null
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Checkmark circle
                        Surface(
                            modifier = Modifier.size(28.dp),
                            shape = CircleShape,
                            color = if (isSelected) data.theme.primaryColor else Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(
                                2.dp,
                                if (isSelected) data.theme.primaryColor else subCol
                            )
                        ) {
                            if (isSelected) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Text("\u2713", color = Color.White, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        Spacer(Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(product.localizedTitle, style = MaterialTheme.typography.titleSmall, color = textCol)
                                if (isLast) {
                                    Spacer(Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = data.theme.accentColor
                                    ) {
                                        Text(
                                            "SAVE",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                            product.subscriptionPeriod?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = subCol)
                            }
                        }

                        Text(product.displayPrice, style = MaterialTheme.typography.titleMedium, color = data.theme.primaryColor)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(24.dp))

            EPaywallCTAButton(
                title = "Start Saving",
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
