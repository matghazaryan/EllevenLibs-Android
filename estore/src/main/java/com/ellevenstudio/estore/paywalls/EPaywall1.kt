package com.ellevenstudio.estore.paywalls

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ellevenstudio.estore.EStore
import com.ellevenstudio.estore.EStoreTheme
import com.ellevenstudio.estore.paywalls.components.*

/**
 * Minimal paywall: Crown icon, "Go Premium" title, 3 feature rows,
 * vertical product cards, CTA button, restore, optional legal links.
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
    val context = LocalContext.current

    LaunchedEffect(products) {
        if (selectedId == null && products.isNotEmpty()) {
            selectedId = products.first().id
        }
    }

    val t = data.theme
    val fontFamily = t.fontFamily

    Box(modifier = Modifier.fillMaxSize()) {
        // Background: image if provided, otherwise solid color
        if (t.backgroundImageResId != null) {
            Image(
                painter = painterResource(id = t.backgroundImageResId),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = if (t.backgroundImageResId != null) {
                androidx.compose.ui.graphics.Color.Transparent
            } else {
                t.backgroundColor
            }
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
                        EPaywallCloseButton(theme = t, onClick = onDismiss)
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Crown icon
                Text(
                    "\uD83D\uDC51",
                    fontSize = 48.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))

                // Title
                Text(
                    "Go Premium",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamily,
                    color = t.textColor,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(32.dp))

                // Feature rows
                data.features.forEach { feature ->
                    EPaywallFeatureRow(icon = feature.icon, title = feature.title, subtitle = feature.subtitle, theme = t)
                    Spacer(Modifier.height(12.dp))
                }

                Spacer(Modifier.height(32.dp))

                // Product cards
                products.forEach { product ->
                    EPaywallProductCard(
                        product = product,
                        isSelected = product.id == selectedId,
                        theme = t,
                        onClick = { selectedId = product.id }
                    )
                    Spacer(Modifier.height(12.dp))
                }

                Spacer(Modifier.height(16.dp))

                // CTA
                EPaywallCTAButton(
                    title = "Subscribe Now",
                    theme = t,
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
                EPaywallRestoreButton(theme = t) { EStore.restore() }

                // Terms & Privacy
                if (t.hasLegalLinks) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        t.termsURL?.let { url ->
                            Text(
                                "Terms of Use",
                                fontSize = 12.sp,
                                fontFamily = fontFamily,
                                color = t.secondaryTextColor,
                                modifier = Modifier.clickable {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }
                            )
                        }
                        if (t.termsURL != null && t.privacyURL != null) {
                            Spacer(Modifier.width(16.dp))
                        }
                        t.privacyURL?.let { url ->
                            Text(
                                "Privacy Policy",
                                fontSize = 12.sp,
                                fontFamily = fontFamily,
                                color = t.secondaryTextColor,
                                modifier = Modifier.clickable {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
