package com.d3ff96.twitchmodpanel.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TwitchPurple = Color(0xFF9147FF)
private val TwitchPurpleDark = Color(0xFF772CE8)
private val AccentTeal = Color(0xFF00F2EA)
private val LiveRed = Color(0xFFEB0400)
private val Bg = Color(0xFF0E0E10)
private val Surface = Color(0xFF18181B)
private val SurfaceVariant = Color(0xFF1F1F23)
private val OnSurface = Color(0xFFEFEFF1)
private val OnMuted = Color(0xFFADADB8)

private val DarkColors = darkColorScheme(
    primary = TwitchPurple,
    onPrimary = Color.White,
    primaryContainer = TwitchPurpleDark,
    secondary = AccentTeal,
    onSecondary = Color.Black,
    error = LiveRed,
    background = Bg,
    onBackground = OnSurface,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnMuted,
    outline = Color(0xFF3A3A3D),
)

private val LightColors = lightColorScheme(
    primary = TwitchPurple,
    onPrimary = Color.White,
    secondary = TwitchPurpleDark,
    background = Color(0xFFF7F7F8),
    onBackground = Color(0xFF0E0E10),
    surface = Color.White,
    onSurface = Color(0xFF0E0E10),
)

val ModLiveRed = LiveRed
val ModAccent = AccentTeal

@Composable
fun TwitchModPanelTheme(
    darkTheme: Boolean = true, // mod panels default to dark
    content: @Composable () -> Unit,
) {
    val scheme = if (darkTheme || isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(
        colorScheme = scheme,
        typography = androidx.compose.material3.Typography(),
        content = content,
    )
}
