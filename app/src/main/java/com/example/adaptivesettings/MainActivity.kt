package com.example.adaptivesettings

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        logConfiguration("onCreate", resources.configuration)
        setContent {
            SettingsTheme {
                SettingsScreen(
                    modifier = Modifier.windowInsetsPadding(
                        WindowInsets.systemBars.union(WindowInsets.displayCutout)
                    ),
                )
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        logConfiguration("onConfigurationChanged", newConfig)
    }

    private fun logConfiguration(event: String, configuration: Configuration) {
        Log.d(
            "AdaptiveSettings",
            "$event activity=${System.identityHashCode(this)} " +
                "config=${configuration.screenWidthDp}x${configuration.screenHeightDp}dp",
        )
    }
}

@Composable
internal fun SettingsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkSettingsColors else LightSettingsColors,
        content = content,
    )
}

private val LightSettingsColors = lightColorScheme(
    primary = Color(0xFF2456A6),
    primaryContainer = Color(0xFFDDE8FF),
    onPrimaryContainer = Color(0xFF102A56),
    background = Color(0xFFF3F5FA),
    surface = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEDF0F7),
    outlineVariant = Color(0xFFD5DAE4),
)

private val DarkSettingsColors = darkColorScheme(
    primary = Color(0xFFADC7FF),
    primaryContainer = Color(0xFF233F6D),
    onPrimaryContainer = Color(0xFFE6EEFF),
    background = Color(0xFF11141B),
    surface = Color(0xFF181D26),
    surfaceContainerLow = Color(0xFF1D2330),
    outlineVariant = Color(0xFF3E485B),
)
