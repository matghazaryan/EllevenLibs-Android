package com.ellevenstudio.estore.paywalls

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
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
 * Horizontal pager: HorizontalPager with product cards filling the page.
 * Each shows icon, title, description, large price. CTA and restore at bottom.
 */
@Composable
fun EPaywall5(
    activity: Activity,
    theme: EStoreTheme? = null,
    onDismiss: (() -> Unit)? = null
) {
    val data = remember { EPaywallData(theme) }
    var isLoading by remember { mutableStateOf(false) }
    val products = data.premiumProducts

    val bgColor = data.theme.backgroundColor
    val textColor = data.theme.textColor

    val pagerState = rememberPagerState(pageCount = { products.size })
    val currentProduct = products.getOrNull(pagerState.currentPage)

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
            if (onDismiss != null) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    EPaywallCloseButton(theme = data.theme, onClick = onDismiss)
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "Choose a Plan",
                style = MaterialTheme.typography.headlineMedium,
                color = textColor,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            // Pager
            if (products.isNotEmpty()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    pageSpacing = 16.dp
                ) { page ->
                    val product = products[page]
                    val cardBg = data.theme.cardBackgroundColor

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(data.theme.cornerRadius),
                        color = cardBg
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("\uD83D\uDCE6", style = MaterialTheme.typography.displayMedium)
                            Spacer(Modifier.height(24.dp))
                            Text(
                                product.localizedTitle,
                                style = MaterialTheme.typography.headlineSmall,
                                color = data.theme.textColor,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                product.localizedDescription,
                                style = MaterialTheme.typography.bodyMedium,
                                color = data.theme.secondaryTextColor,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(24.dp))
                            Text(
                                product.displayPrice,
                                style = MaterialTheme.typography.displaySmall,
                                color = data.theme.primaryColor
                            )
                            product.subscriptionPeriod?.let {
                                Text(
                                    "per $it",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = data.theme.secondaryTextColor
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Page indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(products.size) { index ->
                        val dotColor = if (index == pagerState.currentPage) data.theme.primaryColor else Color.Gray.copy(alpha = 0.3f)
                        Surface(
                            modifier = Modifier.size(8.dp),
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = dotColor
                        ) {}
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            EPaywallCTAButton(
                title = "Subscribe",
                theme = data.theme,
                isLoading = isLoading,
                onClick = {
                    val product = currentProduct ?: return@EPaywallCTAButton
                    isLoading = true
                    EStore.purchase(activity, product.id)
                    isLoading = false
                }
            )

            Spacer(Modifier.height(8.dp))
            EPaywallRestoreButton(theme = data.theme) { EStore.restore() }
        }
    }
}
