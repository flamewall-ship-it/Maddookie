package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.dp

val MaddookieShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

val LocalZeroBlurFastPath = compositionLocalOf { true }

@Composable
fun MaddookieTheme(
    darkTheme: Boolean = true,
    zeroBlurFastPath: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) MaddookieDarkColorScheme else MaddookieLightColorScheme

    CompositionLocalProvider(LocalZeroBlurFastPath provides zeroBlurFastPath) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MaddookieTypography,
            shapes = MaddookieShapes,
            content = content
        )
    }
}
