package com.ramonapps.meetingscribe.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Purple = Color(0xFF6750A4)
private val PurpleDark = Color(0xFF4F378B)
private val Teal = Color(0xFF03DAC5)

private val LightColors = lightColorScheme(
    primary = Purple,
    secondary = Teal,
    tertiary = PurpleDark
)

private val DarkColors = darkColorScheme(
    primary = Purple,
    secondary = Teal,
    tertiary = PurpleDark
)

@Composable
fun MeetingScribeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
