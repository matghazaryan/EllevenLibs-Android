package com.ellevenstudio.egate

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ellevenstudio.estore.EStore
import com.ellevenstudio.estore.paywalls.EPaywall1

/**
 * Full-screen gate overlay shown when the play limit is reached.
 * Offers premium upgrade, watch ad, or dismiss options.
 *
 * Usage:
 *     // Default — uses EGate.config
 *     EGateOverlay(activity = activity)
 *
 *     // Custom config
 *     EGateOverlay(activity = activity, config = myConfig)
 *
 *     // Custom paywall handler
 *     EGateOverlay(
 *         activity = activity,
 *         onPremiumTapped = { showMyPaywall() },
 *         onDismiss = { ... }
 *     )
 */
@Composable
fun EGateOverlay(
    activity: Activity,
    config: EGateConfig = EGate.config,
    onPremiumTapped: (() -> Unit)? = null,
    onAdTapped: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null
) {
    val shouldShow by EGate.shouldShowGate.collectAsState()
    var showPaywall by remember { mutableStateOf(false) }

    if (shouldShow) {
        EGateContent(
            config = config,
            onPremiumTapped = {
                if (onPremiumTapped != null) {
                    onPremiumTapped()
                } else {
                    showPaywall = true
                }
            },
            onAdTapped = {
                if (onAdTapped != null) {
                    onAdTapped()
                } else {
                    EGate.showRewardedAd(activity)
                    EGate.dismiss()
                    onDismiss?.invoke()
                }
            },
            onDismiss = {
                EGate.dismiss()
                onDismiss?.invoke()
            }
        )
    }

    if (showPaywall) {
        EPaywall1(
            activity = activity,
            onDismiss = {
                showPaywall = false
                if (EStore.isPremium.value) {
                    EGate.onPremiumPurchased()
                    onDismiss?.invoke()
                }
            }
        )
    }
}

/**
 * Standalone gate card composable. Use this if you want to embed the gate UI
 * inside your own layout instead of using the full-screen overlay.
 */
@Composable
fun EGateContent(
    config: EGateConfig = EGate.config,
    onPremiumTapped: () -> Unit = {},
    onAdTapped: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    val t = config.theme
    val shape = RoundedCornerShape(t.cornerRadius)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(t.overlayColor)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { /* block taps through */ },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .shadow(20.dp, shape)
                .clip(shape)
                .background(t.cardBackgroundColor)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon
            Text(
                text = t.iconText,
                fontSize = 56.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = config.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = t.titleColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Message
            Text(
                text = config.message,
                fontSize = 15.sp,
                color = t.messageColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Premium button
            if (config.showPremiumButton) {
                Button(
                    onClick = onPremiumTapped,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(t.cornerRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = t.premiumButtonColor,
                        contentColor = t.premiumButtonTextColor
                    )
                ) {
                    Text(
                        text = "\uD83D\uDC51 ${config.premiumButtonText}",
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Ad button
            if (config.showAdButton) {
                Button(
                    onClick = onAdTapped,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(t.adButtonBorderWidth, t.adButtonBorderColor, RoundedCornerShape(t.cornerRadius)),
                    shape = RoundedCornerShape(t.cornerRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = t.adButtonColor,
                        contentColor = t.adButtonTextColor
                    )
                ) {
                    Text(
                        text = "\u25B6 ${config.adButtonText}",
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Dismiss button
            if (config.showDismissButton) {
                Text(
                    text = config.dismissButtonText,
                    fontSize = 14.sp,
                    color = t.dismissButtonColor,
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(8.dp)
                )
            }
        }
    }
}
