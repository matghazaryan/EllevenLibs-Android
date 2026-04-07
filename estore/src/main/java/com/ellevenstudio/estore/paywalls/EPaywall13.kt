package com.ellevenstudio.estore.paywalls

import android.app.Activity
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ellevenstudio.estore.EStore
import com.ellevenstudio.estore.EStoreTheme
import com.ellevenstudio.estore.paywalls.components.*

/**
 * Glassmorphism / Material You style paywall with floating blurred circles,
 * semi-transparent product cards, spring selection animation, and rotating crown icon.
 */
@Composable
fun EPaywall13(
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

    val infiniteTransition = rememberInfiniteTransition(label = "glass")

    // Floating blob offsets
    val blobOffset1 by infiniteTransition.animateFloat(
        initialValue = -50f,
        targetValue = 50f,
        animationSpec = infiniteRepeatable(
            tween(5000, easing = EaseInOut),
            RepeatMode.Reverse
        ),
        label = "blob1"
    )
    val blobOffset2 by infiniteTransition.animateFloat(
        initialValue = 30f,
        targetValue = -40f,
        animationSpec = infiniteRepeatable(
            tween(4000, easing = EaseInOut),
            RepeatMode.Reverse
        ),
        label = "blob2"
    )

    // Rotating crown
    val crownRotation by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            tween(2000, easing = EaseInOut),
            RepeatMode.Reverse
        ),
        label = "crown"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // Floating blurred circles
        Box(
            modifier = Modifier
                .size(220.dp)
                .offset(x = (-40).dp + blobOffset1.dp, y = 80.dp)
                .blur(60.dp)
                .background(data.theme.primaryColor.copy(alpha = 0.2f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(180.dp)
                .align(Alignment.TopEnd)
                .offset(x = 30.dp + blobOffset2.dp, y = 200.dp)
                .blur(60.dp)
                .background(data.theme.accentColor.copy(alpha = 0.2f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(140.dp)
                .align(Alignment.BottomStart)
                .offset(x = 60.dp + (-blobOffset1).dp, y = (-100).dp)
                .blur(50.dp)
                .background(data.theme.primaryColor.copy(alpha = 0.15f), CircleShape)
        )

        // Content
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

            // Rotating crown icon
            Text(
                "\uD83D\uDC51",
                style = MaterialTheme.typography.displayLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    rotationZ = crownRotation
                }
            )

            Spacer(Modifier.height(16.dp))

            Text(
                "Premium Access",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp
                ),
                color = textColor,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Everything you need, beautifully designed",
                style = MaterialTheme.typography.bodyLarge,
                color = subColor,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // Features in glass cards
            data.features.forEach { feature ->
                Paywall13GlassFeature(icon = feature.icon, text = feature.title, textColor = textColor, cardBgColor = data.theme.cardBackgroundColor)
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(24.dp))

            // Product cards with spring selection
            products.forEach { product ->
                val isSelected = product.id == selectedId
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.03f else 1f,
                    animationSpec = spring(
                        dampingRatio = 0.6f,
                        stiffness = 300f
                    ),
                    label = "cardScale_${product.id}"
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        },
                    shape = RoundedCornerShape(data.theme.cornerRadius),
                    color = if (isSelected) data.theme.primaryColor.copy(alpha = 0.15f) else data.theme.cardBackgroundColor,
                    border = if (isSelected) {
                        ButtonDefaults.outlinedButtonBorder(true)
                    } else null,
                    shadowElevation = if (isSelected) 8.dp else 2.dp,
                    onClick = { selectedId = product.id }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Selection indicator
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    if (isSelected) data.theme.primaryColor else data.theme.secondaryTextColor.copy(alpha = 0.2f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Text(
                                    "\u2713",
                                    color = data.theme.buttonTextColor,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                product.localizedTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = textColor,
                                fontWeight = FontWeight.SemiBold
                            )
                            product.subscriptionPeriod?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = subColor
                                )
                            }
                        }

                        Text(
                            product.displayPrice,
                            style = MaterialTheme.typography.titleLarge,
                            color = if (isSelected) data.theme.primaryColor else textColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(16.dp))

            // CTA
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

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Paywall13GlassFeature(icon: String, text: String, textColor: Color, cardBgColor: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = cardBgColor,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(icon, style = MaterialTheme.typography.titleLarge)
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
        }
    }
}
