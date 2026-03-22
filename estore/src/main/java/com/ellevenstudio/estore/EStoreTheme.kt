package com.ellevenstudio.estore

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Theme configuration for EStore paywalls.
 * Pass your app's colors and paywalls auto-build with your brand.
 *
 * Usage:
 *     // Minimal - just pass primary color
 *     EStoreTheme(primaryColor = Color(0xFF6750A4))
 *
 *     // Full customization
 *     EStoreTheme(
 *         primaryColor = Color(0xFF6750A4),
 *         accentColor = Color(0xFFFF9800),
 *         backgroundColor = Color.Black,
 *         textColor = Color.White
 *     )
 */
data class EStoreTheme(
    val primaryColor: Color = Color(0xFF2196F3),
    val accentColor: Color = Color(0xFFFF9800),
    val backgroundColor: Color = Color(0xFFFFFFFF),
    val textColor: Color = Color(0xFF1C1B1F),
    val secondaryTextColor: Color = Color(0xFF6B6B6B),
    val cardBackgroundColor: Color = Color(0xFFF2F2F2),
    val cornerRadius: Dp = 16.dp
)
