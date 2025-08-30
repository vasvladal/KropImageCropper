package com.example.kropimagecropper

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Dimens {
    // Padding
    val smallPadding = 4.dp
    val mediumPadding = 8.dp
    val largePadding = 16.dp
    val extraLargePadding = 24.dp

    // Icon sizes
    val iconSmall = 16.dp
    val iconMedium = 24.dp
    val iconLarge = 32.dp

    // Card sizes
    val cardSmall = 100.dp
    val cardMedium = 120.dp
    val cardLarge = 140.dp

    // Button heights
    val buttonSmall = 40.dp
    val buttonMedium = 56.dp
    val buttonLarge = 64.dp

    // Grid and list items
    val gridItemHeight = 160.dp
    val gridItemMinWidth = 100.dp

    // Text sizes
    val textSmall = 12.sp
    val textMedium = 14.sp
    val textLarge = 16.sp

    // Other
    val dividerHeight = 40.dp
    val badgeSize = 20.dp
    val cornerRadiusSmall = 8.dp
    val cornerRadiusMedium = 12.dp
    val cornerRadiusLarge = 16.dp

    // Responsive dimensions
    @Composable
    fun responsiveIconSize() = if (LocalConfiguration.current.screenWidthDp < 360) 20.dp else 24.dp

    @Composable
    fun responsivePadding() = if (LocalConfiguration.current.screenWidthDp < 360) 12.dp else 16.dp

    @Composable
    fun responsiveTextSize() = if (LocalConfiguration.current.screenWidthDp < 360) 12.sp else 14.sp
}