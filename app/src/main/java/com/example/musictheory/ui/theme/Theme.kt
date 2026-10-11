package com.example.musictheory.ui.theme
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
val LocalAppDarkTheme = staticCompositionLocalOf { false }
private val Light = lightColorScheme(primary = Color(0xFF4F46E5), onPrimary = Color.White,
    primaryContainer = Color(0xFFE3E1FF), onPrimaryContainer = Color(0xFF150A66),
    secondary = Color(0xFF0D9488), secondaryContainer = Color(0xFFCFF2ED),
    tertiary = Color(0xFFD97706), background = Color(0xFFF4F3FB), surface = Color.White,
    surfaceVariant = Color(0xFFE6E4F0), onSurfaceVariant = Color(0xFF5B5B72), outline = Color(0xFFC9C7DD))
private val Dark = darkColorScheme(primary = Color(0xFFB9B3FF), onPrimary = Color(0xFF241478),
    primaryContainer = Color(0xFF3D33B0), secondary = Color(0xFF5ADBCB), tertiary = Color(0xFFFFB95C),
    background = Color(0xFF131220), surface = Color(0xFF1C1B2A), surfaceVariant = Color(0xFF2B2A3C),
    onSurfaceVariant = Color(0xFFB9B7CC), outline = Color(0xFF4A4862))
@Composable
fun HarmoniaTheme(darkTheme: Boolean = isSystemInDarkTheme(), dynamicColor: Boolean = false,
                  content: @Composable () -> Unit) {
    val cs = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(LocalContext.current)
            else dynamicLightColorScheme(LocalContext.current)
        darkTheme -> Dark; else -> Light }
    CompositionLocalProvider(LocalAppDarkTheme provides darkTheme) {
        MaterialTheme(colorScheme = cs, content = content) }
}
