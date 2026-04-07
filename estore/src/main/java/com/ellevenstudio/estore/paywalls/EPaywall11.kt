package com.ellevenstudio.estore.paywalls

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
 * Netflix/Spotify style paywall with animated gradient background,
 * pulsating CTA button, and staggered fade-in entrance animations.
 */
@Composable
fun EPaywall11(
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

    val t = data.theme
    val textColor = t.textColor

    // Animated gradient
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(3000, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = "gradientOffset"
    )

    val color1 = t.primaryColor
    val color2 = t.accentColor
    val color3 = t.backgroundColor

    val animatedGradient = Brush.verticalGradient(
        colors = listOf(
            lerp(color1, color2, offset),
            lerp(color3, color1, offset),
            color3
        )
    )

    // Pulsing CTA
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            tween(1200, easing = EaseInOut),
            RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Staggered entrance
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(animatedGradient)
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
                    EPaywallCloseButton(
                        theme = t.copy(secondaryTextColor = textColor.copy(alpha = 0.7f)),
                        onClick = onDismiss
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Title with staggered entrance
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600, delayMillis = 0)) + slideInVertically(
                    tween(600, delayMillis = 0)
                ) { it / 2 }
            ) {
                Text(
                    "\uD83D\uDE80",
                    style = MaterialTheme.typography.displayLarge,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(16.dp))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600, delayMillis = 100)) + slideInVertically(
                    tween(600, delayMillis = 100)
                ) { it / 2 }
            ) {
                Text(
                    "Unlock Premium",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    ),
                    color = textColor,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(8.dp))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600, delayMillis = 200)) + slideInVertically(
                    tween(600, delayMillis = 200)
                ) { it / 2 }
            ) {
                Text(
                    "Get unlimited access to all features",
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(32.dp))

            // Features
            data.features.forEachIndexed { index, feature ->
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(600, delayMillis = 300 + index * 100)) + slideInVertically(
                        tween(600, delayMillis = 300 + index * 100)
                    ) { it / 2 }
                ) {
                    Paywall11FeatureItem(icon = feature.icon, text = feature.title, textColor = textColor, cardBgColor = t.cardBackgroundColor)
                }
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(24.dp))

            // Product cards with blurred overlay style
            products.forEachIndexed { index, product ->
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(600, delayMillis = 600 + index * 100)) + slideInVertically(
                        tween(600, delayMillis = 600 + index * 100)
                    ) { it / 2 }
                ) {
                    val isSelected = product.id == selectedId
                    val cardBg = if (isSelected) t.cardBackgroundColor.copy(alpha = 0.25f) else t.cardBackgroundColor.copy(alpha = 0.1f)

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(t.cornerRadius),
                        color = cardBg,
                        border = if (isSelected) ButtonDefaults.outlinedButtonBorder(true) else null,
                        onClick = { selectedId = product.id }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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
                                        color = textColor.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            Text(
                                product.displayPrice,
                                style = MaterialTheme.typography.titleLarge,
                                color = textColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(16.dp))

            // Pulsating CTA button
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600, delayMillis = 900)) + slideInVertically(
                    tween(600, delayMillis = 900)
                ) { it / 2 }
            ) {
                Button(
                    onClick = {
                        val id = selectedId ?: return@Button
                        isLoading = true
                        EStore.purchase(activity, id)
                        isLoading = false
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                        },
                    shape = RoundedCornerShape(t.cornerRadius),
                    colors = ButtonDefaults.buttonColors(containerColor = t.primaryColor)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = t.buttonTextColor,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "Subscribe Now",
                            color = t.buttonTextColor,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            EPaywallRestoreButton(
                theme = t.copy(secondaryTextColor = textColor.copy(alpha = 0.5f))
            ) { EStore.restore() }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Paywall11FeatureItem(icon: String, text: String, textColor: Color, cardBgColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBgColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
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

private fun lerp(start: Color, end: Color, fraction: Float): Color {
    return Color(
        red = start.red + (end.red - start.red) * fraction,
        green = start.green + (end.green - start.green) * fraction,
        blue = start.blue + (end.blue - start.blue) * fraction,
        alpha = start.alpha + (end.alpha - start.alpha) * fraction
    )
}
