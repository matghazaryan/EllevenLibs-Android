package com.ellevenstudio.estore.paywalls

import android.app.Activity
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ellevenstudio.estore.EStore
import com.ellevenstudio.estore.EStoreTheme
import com.ellevenstudio.estore.paywalls.components.*

/**
 * Full-screen hero gradient: primaryColor gradient top to backgroundColor bottom.
 * "Unlock Everything" white text. Bottom card with radio-style product selection.
 */
@Composable
fun EPaywall4(
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

    val bottomColor = data.theme.backgroundColor

    Box(modifier = Modifier.fillMaxSize()) {
        // Gradient background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(data.theme.primaryColor, bottomColor)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (onDismiss != null) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    EPaywallCloseButton(
                        theme = data.theme.copy(secondaryTextColor = Color.White),
                        onClick = onDismiss
                    )
                }
            }

            Spacer(Modifier.height(60.dp))

            Text(
                "\uD83D\uDD13",
                style = MaterialTheme.typography.displayLarge,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))

            Text(
                "Unlock Everything",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                "Get full access to all premium features",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(48.dp))

            // Bottom card with radio-style selection
            val cardBg = data.theme.cardBackgroundColor
            Surface(
                shape = RoundedCornerShape(data.theme.cornerRadius),
                color = cardBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    products.forEach { product ->
                        val isSelected = product.id == selectedId
                        val textCol = data.theme.textColor
                        val subCol = data.theme.secondaryTextColor

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedId = product.id }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Radio circle
                            Surface(
                                modifier = Modifier.size(24.dp),
                                shape = CircleShape,
                                color = if (isSelected) data.theme.primaryColor else Color.Transparent,
                                border = androidx.compose.foundation.BorderStroke(
                                    2.dp,
                                    if (isSelected) data.theme.primaryColor else subCol
                                )
                            ) {
                                if (isSelected) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Surface(
                                            modifier = Modifier.size(10.dp),
                                            shape = CircleShape,
                                            color = Color.White
                                        ) {}
                                    }
                                }
                            }

                            Spacer(Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(product.localizedTitle, style = MaterialTheme.typography.titleSmall, color = textCol)
                                product.subscriptionPeriod?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall, color = subCol)
                                }
                            }

                            Text(
                                product.displayPrice,
                                style = MaterialTheme.typography.titleMedium,
                                color = data.theme.primaryColor
                            )
                        }

                        if (product != products.last()) {
                            HorizontalDivider(modifier = Modifier.padding(start = 40.dp))
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    EPaywallCTAButton(
                        title = "Unlock Now",
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

                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        EPaywallRestoreButton(theme = data.theme) { EStore.restore() }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
