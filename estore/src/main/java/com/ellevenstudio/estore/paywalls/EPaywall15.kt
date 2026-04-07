package com.ellevenstudio.estore.paywalls

import android.app.Activity
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ellevenstudio.estore.EStore
import com.ellevenstudio.estore.EStoreTheme
import com.ellevenstudio.estore.paywalls.components.*
import kotlinx.coroutines.delay

/**
 * 3D Card / Interactive style paywall with 3D rotation on drag,
 * parallax depth layers, animated shine sweep on CTA,
 * countdown timer, and elastic bounce entrance for product cards.
 */
@Composable
fun EPaywall15(
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

    // 3D rotation with drag
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 100f),
        label = "rotX"
    )
    val animatedOffsetY by animateFloatAsState(
        targetValue = offsetY,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 100f),
        label = "rotY"
    )

    // Shine sweep for CTA
    val infiniteTransition = rememberInfiniteTransition(label = "shine")
    val shineOffset by infiniteTransition.animateFloat(
        initialValue = -200f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shineOffset"
    )

    // Countdown timer
    var timeRemaining by remember { mutableIntStateOf(23 * 3600 + 59 * 60 + 59) }
    LaunchedEffect(Unit) {
        while (timeRemaining > 0) {
            delay(1000)
            timeRemaining--
        }
    }
    val hours = timeRemaining / 3600
    val minutes = (timeRemaining % 3600) / 60
    val seconds = timeRemaining % 60

    // Elastic entrance for cards
    var cardsVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { cardsVisible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX = (offsetX + dragAmount.x).coerceIn(-200f, 200f)
                        offsetY = (offsetY + dragAmount.y).coerceIn(-200f, 200f)
                    },
                    onDragEnd = {
                        offsetX = 0f
                        offsetY = 0f
                    }
                )
            }
    ) {
        // Parallax background layer (deep)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = animatedOffsetX * 0.05f
                    translationY = animatedOffsetY * 0.05f
                }
        ) {
            // Subtle background circles for depth
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .offset(x = (-60).dp, y = 100.dp)
                    .background(
                        data.theme.primaryColor.copy(alpha = 0.05f),
                        RoundedCornerShape(50)
                    )
            )
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 40.dp, y = (-50).dp)
                    .background(
                        data.theme.accentColor.copy(alpha = 0.05f),
                        RoundedCornerShape(50)
                    )
            )
        }

        // Main content with 3D rotation
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationY = animatedOffsetX / 20f
                    rotationX = -animatedOffsetY / 20f
                    cameraDistance = 12f * density
                }
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Close button (parallax mid layer)
            if (onDismiss != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            translationX = animatedOffsetX * 0.1f
                            translationY = animatedOffsetY * 0.1f
                        }
                ) {
                    EPaywallCloseButton(
                        theme = data.theme.copy(secondaryTextColor = data.theme.secondaryTextColor),
                        onClick = onDismiss
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Countdown timer badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = data.theme.accentColor.copy(alpha = 0.15f),
                modifier = Modifier.graphicsLayer {
                    translationX = animatedOffsetX * 0.15f
                    translationY = animatedOffsetY * 0.15f
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "\u23F0",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Offer expires in ",
                        style = MaterialTheme.typography.bodySmall,
                        color = data.theme.accentColor
                    )
                    Text(
                        String.format("%02d:%02d:%02d", hours, minutes, seconds),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = data.theme.accentColor
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Title (parallax foreground)
            Text(
                "\uD83D\uDE80",
                style = MaterialTheme.typography.displayLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    translationX = animatedOffsetX * 0.2f
                    translationY = animatedOffsetY * 0.2f
                }
            )

            Spacer(Modifier.height(16.dp))

            Text(
                "Limited Time Offer",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp
                ),
                color = textColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    translationX = animatedOffsetX * 0.15f
                    translationY = animatedOffsetY * 0.15f
                }
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Upgrade now before the deal ends",
                style = MaterialTheme.typography.bodyLarge,
                color = subColor,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // Features
            data.features.forEach { feature ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(feature.icon, color = data.theme.primaryColor, style = MaterialTheme.typography.titleMedium)
                    Text(feature.title, style = MaterialTheme.typography.bodyMedium, color = textColor)
                }
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(24.dp))

            // Product cards with elastic bounce entrance
            products.forEachIndexed { index, product ->
                val isSelected = product.id == selectedId

                val cardScale by animateFloatAsState(
                    targetValue = if (cardsVisible) 1f else 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "cardEntrance_$index"
                )

                val selectionScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.02f else 1f,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
                    label = "selection_$index"
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = cardScale * selectionScale
                            scaleY = cardScale * selectionScale
                        },
                    shape = RoundedCornerShape(data.theme.cornerRadius),
                    color = if (isSelected) data.theme.primaryColor.copy(alpha = 0.15f) else data.theme.cardBackgroundColor.copy(alpha = 0.06f),
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

            Spacer(Modifier.height(20.dp))

            // CTA with shine sweep
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
                    .drawWithContent {
                        drawContent()
                        val shimmerBrush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                data.theme.buttonTextColor.copy(alpha = 0.3f),
                                Color.Transparent
                            ),
                            start = Offset(shineOffset, 0f),
                            end = Offset(shineOffset + 150f, size.height)
                        )
                        drawRect(brush = shimmerBrush)
                    },
                shape = RoundedCornerShape(data.theme.cornerRadius),
                colors = ButtonDefaults.buttonColors(containerColor = data.theme.primaryColor)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = data.theme.buttonTextColor,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "Claim This Offer",
                        color = data.theme.buttonTextColor,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            EPaywallRestoreButton(
                theme = data.theme.copy(secondaryTextColor = data.theme.secondaryTextColor)
            ) { EStore.restore() }

            Spacer(Modifier.height(16.dp))
        }
    }
}
