package com.ellevenstudio.estore.paywalls

import android.app.Activity
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ellevenstudio.estore.EStore
import com.ellevenstudio.estore.EStoreTheme
import com.ellevenstudio.estore.paywalls.components.*
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Dark Premium / Luxury style paywall with dark background,
 * accent sparkle particles, rotating diamond icon,
 * animated accent border cards, and typewriter effect headline.
 */
@Composable
fun EPaywall14(
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
    val accent = t.accentColor
    val accentDark = t.primaryColor
    val bgColor = t.backgroundColor
    val textColor = t.textColor
    val subColor = t.secondaryTextColor

    val infiniteTransition = rememberInfiniteTransition(label = "luxury")

    // Rotating diamond
    val diamondRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(6000, easing = LinearEasing)
        ),
        label = "diamond"
    )

    // Rotating accent border for cards
    val borderRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(3000, easing = LinearEasing)
        ),
        label = "border"
    )

    // Sparkle particles
    val sparkles = remember {
        List(20) {
            Paywall14Sparkle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = Random.nextFloat() * 3f + 1f,
                baseAlpha = Random.nextFloat() * 0.5f + 0.2f,
                speed = Random.nextFloat() * 0.5f + 0.5f
            )
        }
    }
    val sparkleTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(4000, easing = LinearEasing)
        ),
        label = "sparkleTime"
    )

    // Typewriter effect
    val fullText = "Unlock Premium"
    var displayedText by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        fullText.forEachIndexed { i, _ ->
            delay(50)
            displayedText = fullText.take(i + 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // Sparkle canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            sparkles.forEach { s ->
                val twinkle = ((kotlin.math.sin((sparkleTime * s.speed * Math.PI * 2).toDouble()) + 1.0) / 2.0).toFloat()
                drawCircle(
                    color = accent.copy(alpha = s.baseAlpha * twinkle),
                    radius = s.size * (0.5f + twinkle * 0.5f),
                    center = Offset(s.x * size.width, s.y * size.height)
                )
            }
        }

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
                        theme = t.copy(secondaryTextColor = subColor),
                        onClick = onDismiss
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Rotating diamond icon
            Text(
                "\uD83D\uDC8E",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 56.sp),
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    rotationY = diamondRotation
                    cameraDistance = 12f * density
                }
            )

            Spacer(Modifier.height(20.dp))

            // Typewriter headline
            Text(
                displayedText,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    letterSpacing = 1.sp
                ),
                color = accent,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "The ultimate luxury experience",
                style = MaterialTheme.typography.bodyLarge,
                color = subColor,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(36.dp))

            // Features
            data.features.forEach { feature ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(feature.icon, color = accent, style = MaterialTheme.typography.titleMedium)
                    Text(
                        feature.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor.copy(alpha = 0.8f)
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(24.dp))

            // Product cards with animated accent border
            products.forEach { product ->
                val isSelected = product.id == selectedId
                val cornerRad = t.cornerRadius

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isSelected) {
                                Modifier.drawBehind {
                                    val brush = Brush.sweepGradient(
                                        colors = listOf(accent, accentDark, accent.copy(alpha = 0.5f), accent),
                                        center = center
                                    )
                                    rotate(borderRotation) {
                                        drawRoundRect(
                                            brush = brush,
                                            style = Stroke(width = 2.dp.toPx()),
                                            cornerRadius = CornerRadius(cornerRad.toPx())
                                        )
                                    }
                                }
                            } else Modifier
                        ),
                    shape = RoundedCornerShape(cornerRad),
                    color = if (isSelected) t.cardBackgroundColor.copy(alpha = 0.08f) else t.cardBackgroundColor.copy(alpha = 0.04f),
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
                                    color = subColor
                                )
                            }
                            Text(
                                product.localizedDescription,
                                style = MaterialTheme.typography.bodySmall,
                                color = subColor,
                                maxLines = 2
                            )
                        }
                        Text(
                            product.displayPrice,
                            style = MaterialTheme.typography.titleLarge,
                            color = if (isSelected) accent else textColor.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(20.dp))

            // CTA
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
                    .height(56.dp),
                shape = RoundedCornerShape(t.cornerRadius),
                colors = ButtonDefaults.buttonColors(containerColor = accent)
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

            Spacer(Modifier.height(8.dp))

            EPaywallRestoreButton(
                theme = t.copy(secondaryTextColor = subColor)
            ) { EStore.restore() }

            Spacer(Modifier.height(16.dp))
        }
    }
}

private data class Paywall14Sparkle(
    val x: Float,
    val y: Float,
    val size: Float,
    val baseAlpha: Float,
    val speed: Float
)
