package com.ellevenstudio.estore.paywalls

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import com.ellevenstudio.estore.EStoreProduct
import com.ellevenstudio.estore.EStoreTheme
import com.ellevenstudio.estore.paywalls.components.*

/**
 * Side-by-side comparison cards: "Choose Your Plan" title, products in a Row
 * with equal weight, last one gets "BEST VALUE" badge. Selected one highlighted.
 */
@Composable
fun EPaywall2(
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

            Text(
                "Choose Your Plan",
                style = MaterialTheme.typography.headlineMedium,
                color = textColor,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // Side-by-side cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                products.forEachIndexed { index, product ->
                    val isSelected = product.id == selectedId
                    val isLast = index == products.lastIndex
                    PlanCard(
                        product = product,
                        isSelected = isSelected,
                        showBadge = isLast,
                        theme = data.theme,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedId = product.id }
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            EPaywallCTAButton(
                title = "Continue",
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

@Composable
private fun PlanCard(
    product: EStoreProduct,
    isSelected: Boolean,
    showBadge: Boolean,
    theme: EStoreTheme,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = if (isSelected) theme.primaryColor else theme.cardBackgroundColor
    val textCol = if (isSelected) theme.buttonTextColor else theme.textColor
    val subCol = if (isSelected) theme.buttonTextColor.copy(alpha = 0.8f) else theme.secondaryTextColor

    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(theme.cornerRadius),
        color = bg,
        border = if (isSelected) BorderStroke(2.dp, theme.primaryColor) else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showBadge) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = theme.accentColor
                ) {
                    Text(
                        "BEST VALUE",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.buttonTextColor
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
            Text(product.localizedTitle, style = MaterialTheme.typography.titleSmall, color = textCol, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            product.subscriptionPeriod?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = subCol)
                Spacer(Modifier.height(4.dp))
            }
            Text(product.displayPrice, style = MaterialTheme.typography.titleMedium, color = if (isSelected) theme.buttonTextColor else theme.primaryColor)
        }
    }
}
