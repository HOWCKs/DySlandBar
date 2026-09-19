package com.howck.dmi.ui

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable

/**
 * Material You colors derived from the user's wallpaper.
 *
 * minSdk is 31 (Android 12+), so dynamic colors are always available —
 * no fallback palette is needed in this build.
 */
@Composable
fun dynamicColorSchemeFor(context: Context): ColorScheme {
    return if (isSystemInDarkTheme()) {
        dynamicDarkColorScheme(context)
    } else {
        dynamicLightColorScheme(context)
    }
}
