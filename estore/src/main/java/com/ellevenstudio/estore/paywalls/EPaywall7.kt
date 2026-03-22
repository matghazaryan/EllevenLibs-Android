package com.ellevenstudio.estore.paywalls

import android.app.Activity
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
 * Social proof: 5 star icons, "Loved by Thousands", 2 review quote cards,
 * product cards below, CTA, restore.
 */
@Composable
fun EPaywall7(
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
    val subColor = data.theme.secondaryTextColor
    val cardBg = data.theme.cardBackgroundColor

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

            // 5 stars
            Text(
                "\u2B50\u2B50\u2B50\u2B50\u2B50",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))

            Text(
                "Loved by Thousands",
                style = MaterialTheme.typography.headlineMedium,
                color = textColor,
                textAlign = TextAlign.Center
            )
            Text(
                "See what our users are saying",
                style = MaterialTheme.typography.bodyMedium,
                color = subColor,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            // Review card 1
            Surface(
                shape = RoundedCornerShape(data.theme.cornerRadius),
                color = cardBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("\u201CAbsolutely worth it! The premium features have completely changed how I use this app.\u201D",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("- Sarah K.", style = MaterialTheme.typography.labelMedium, color = subColor)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Review card 2
            Surface(
                shape = RoundedCornerShape(data.theme.cornerRadius),
                color = cardBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("\u201CBest purchase I\u2019ve made this year. No ads and the exclusive content is amazing!\u201D",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("- Michael R.", style = MaterialTheme.typography.labelMedium, color = subColor)
                }
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

            EPaywallCTAButton(
                title = "Join Thousands",
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
