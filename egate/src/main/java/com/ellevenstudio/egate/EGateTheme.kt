package com.ellevenstudio.egate

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Theme configuration for EGate UI.
 *
 * Usage:
 *     // Minimal
 *     EGateTheme(premiumButtonColor = Color(0xFFFF9800))
 *
 *     // Full customization
 *     EGateTheme(
 *         cardBackgroundColor = Color(0xFF1E1E1E),
 *         titleColor = Color.White,
 *         messageColor = Color.Gray,
 *         premiumButtonColor = Color(0xFFFF9800),
 *         adButtonBorderColor = Color(0xFF2196F3),
 *         cornerRadius = 20.dp
 *     )
 */
data class EGateTheme(
    val cardBackgroundColor: Color = Color(0xFFFFFFFF),
    val titleColor: Color = Color(0xFF1C1B1F),
    val messageColor: Color = Color(0xFF6B6B6B),
    val premiumButtonColor: Color = Color(0xFFFF9800),
    val premiumButtonTextColor: Color = Color.White,
    val adButtonColor: Color = Color.Transparent,
    val adButtonTextColor: Color = Color(0xFF2196F3),
    val adButtonBorderColor: Color = Color(0xFF2196F3),
    val adButtonBorderWidth: Dp = 1.5.dp,
    val dismissButtonColor: Color = Color(0xFF6B6B6B),
    val cornerRadius: Dp = 16.dp,
    val iconText: String = "\uD83C\uDFAE",
    val overlayColor: Color = Color(0x80000000)
)
