package com.ellevenstudio.estore.paywalls

import android.app.Activity
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import kotlin.random.Random

/**
 * Floating Particles style paywall with dark rich background,
 * drifting particle dots, glowing product cards, and bouncy CTA button.
 */
@Composable
fun EPaywall12(
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

    val bgColor = Color(0xFF0D0D1A)
    val particleColor = data.theme.primaryColor

    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(10000, easing = LinearEasing)
        ),
        label = "particleTime"
    )

    // Rotating gradient for "Most Popular" badge
    val borderRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(3000, easing = LinearEasing)
        ),
        label = "borderRotation"
    )

    // Glow pulse for cards
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            tween(1500, easing = EaseInOut),
            RepeatMode.Reverse
        ),
        label = "glow"
    )

    // Particles
    val particles = remember {
        List(25) {
            Paywall12Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = Random.nextFloat() * 4f + 2f,
                alpha = Random.nextFloat() * 0.4f + 0.1f,
                speed = Random.nextFloat() * 0.3f + 0.1f
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // Particle canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            particles.forEach { p ->
                val y = (p.y - time * p.speed) % 1f
                val adjustedY = if (y < 0) y + 1f else y
                drawCircle(
                    color = particleColor.copy(alpha = p.alpha),
                    radius = p.size,
                    center = Offset(p.x * size.width, adjustedY * size.height)
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
                        theme = data.theme.copy(secondaryTextColor = Color.White.copy(alpha = 0.6f)),
                        onClick = onDismiss
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Text(
                "\u2728",
                style = MaterialTheme.typography.displayLarge,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            Text(
                "Elevate Your\nExperience",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp,
                    lineHeight = 36.sp
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Choose your plan and unlock everything",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // Product cards with glow
            products.forEachIndexed { index, product ->
                val isSelected = product.id == selectedId
                val isMostPopular = index == 0

                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val cardCorner = data.theme.cornerRadius

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isSelected) {
                                    Modifier.drawBehind {
                                        val glowBrush = Brush.radialGradient(
                                            colors = listOf(
                                                particleColor.copy(alpha = glowAlpha),
                                                Color.Transparent
                                            ),
                                            radius = size.maxDimension * 0.6f
                                        )
                                        drawRect(brush = glowBrush)
                                    }
                                } else Modifier
                            ),
                        shape = RoundedCornerShape(cardCorner),
                        color = if (isSelected) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.06f),
                        onClick = { selectedId = product.id }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        product.localizedTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    product.subscriptionPeriod?.let {
                                        Text(
                                            it,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                                Text(
                                    product.displayPrice,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = if (isSelected) particleColor else Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (isMostPopular) {
                                Spacer(Modifier.height(8.dp))
                                Paywall12PopularBadge(
                                    color = particleColor,
                                    rotation = borderRotation
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(24.dp))

            // Bouncy CTA
            var ctaPressed by remember { mutableStateOf(false) }
            val ctaScale by animateFloatAsState(
                targetValue = if (ctaPressed) 0.95f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "ctaBounce"
            )

            Button(
                onClick = {
                    ctaPressed = true
                    val id = selectedId ?: return@Button
                    isLoading = true
                    EStore.purchase(activity, id)
                    isLoading = false
                    ctaPressed = false
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .graphicsLayer {
                        scaleX = ctaScale
                        scaleY = ctaScale
                    },
                shape = RoundedCornerShape(data.theme.cornerRadius),
                colors = ButtonDefaults.buttonColors(containerColor = particleColor)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "Get Started",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            EPaywallRestoreButton(
                theme = data.theme.copy(secondaryTextColor = Color.White.copy(alpha = 0.4f))
            ) { EStore.restore() }

            Spacer(Modifier.height(16.dp))
        }
    }
}

private data class Paywall12Particle(
    val x: Float,
    val y: Float,
    val size: Float,
    val alpha: Float,
    val speed: Float
)

@Composable
private fun Paywall12PopularBadge(color: Color, rotation: Float) {
    Box(
        modifier = Modifier
            .drawBehind {
                val brush = Brush.sweepGradient(
                    colors = listOf(color, color.copy(alpha = 0.3f), color, color.copy(alpha = 0.3f), color)
                )
                rotate(rotation) {
                    drawRoundRect(
                        brush = brush,
                        style = Stroke(width = 1.5.dp.toPx()),
                        cornerRadius = CornerRadius(8.dp.toPx())
                    )
                }
            }
            .padding(1.5.dp)
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            "\u2B50 Most Popular",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}
