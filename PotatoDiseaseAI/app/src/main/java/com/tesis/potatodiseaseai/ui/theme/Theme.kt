package com.tesis.potatodiseaseai.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Immutable
data class CustomColors(
    val preventionAccent: Color,
    val preventionContainer: Color,
    val preventionText: Color,
    
    val chemicalControlAccent: Color,
    val chemicalControlContainer: Color,
    val chemicalControlText: Color,
    
    val biologicalControlAccent: Color,
    val biologicalControlContainer: Color,
    val biologicalControlText: Color,
    
    val diseaseAlertAccent: Color,
    val diseaseAlertContainer: Color,
    val diseaseAlertText: Color,
    
    val lowConfidenceAccent: Color,
    val lowConfidenceContainer: Color,
    val lowConfidenceText: Color,
    
    val textPrimary: Color,
    val textSecondary: Color
)

val LocalCustomColors = staticCompositionLocalOf {
    CustomColors(
        preventionAccent = Color.Unspecified,
        preventionContainer = Color.Unspecified,
        preventionText = Color.Unspecified,
        chemicalControlAccent = Color.Unspecified,
        chemicalControlContainer = Color.Unspecified,
        chemicalControlText = Color.Unspecified,
        biologicalControlAccent = Color.Unspecified,
        biologicalControlContainer = Color.Unspecified,
        biologicalControlText = Color.Unspecified,
        diseaseAlertAccent = Color.Unspecified,
        diseaseAlertContainer = Color.Unspecified,
        diseaseAlertText = Color.Unspecified,
        lowConfidenceAccent = Color.Unspecified,
        lowConfidenceContainer = Color.Unspecified,
        lowConfidenceText = Color.Unspecified,
        textPrimary = Color.Unspecified,
        textSecondary = Color.Unspecified
    )
}

val LightCustomColors = CustomColors(
    preventionAccent = PreventionAccentLight,
    preventionContainer = PreventionContainerLight,
    preventionText = PreventionTextLight,
    chemicalControlAccent = ChemicalControlAccentLight,
    chemicalControlContainer = ChemicalControlContainerLight,
    chemicalControlText = ChemicalControlTextLight,
    biologicalControlAccent = BiologicalControlAccentLight,
    biologicalControlContainer = BiologicalControlContainerLight,
    biologicalControlText = BiologicalControlTextLight,
    diseaseAlertAccent = DiseaseAlertAccentLight,
    diseaseAlertContainer = DiseaseAlertContainerLight,
    diseaseAlertText = DiseaseAlertTextLight,
    lowConfidenceAccent = LowConfidenceAccentLight,
    lowConfidenceContainer = LowConfidenceContainerLight,
    lowConfidenceText = LowConfidenceTextLight,
    textPrimary = TextPrimaryLight,
    textSecondary = TextSecondaryLight
)

val DarkCustomColors = CustomColors(
    preventionAccent = PreventionAccentDark,
    preventionContainer = PreventionContainerDark,
    preventionText = PreventionTextDark,
    chemicalControlAccent = ChemicalControlAccentDark,
    chemicalControlContainer = ChemicalControlContainerDark,
    chemicalControlText = ChemicalControlTextDark,
    biologicalControlAccent = BiologicalControlAccentDark,
    biologicalControlContainer = BiologicalControlContainerDark,
    biologicalControlText = BiologicalControlTextDark,
    diseaseAlertAccent = DiseaseAlertAccentDark,
    diseaseAlertContainer = DiseaseAlertContainerDark,
    diseaseAlertText = DiseaseAlertTextDark,
    lowConfidenceAccent = LowConfidenceAccentDark,
    lowConfidenceContainer = LowConfidenceContainerDark,
    lowConfidenceText = LowConfidenceTextDark,
    textPrimary = TextPrimaryDark,
    textSecondary = TextSecondaryDark
)

private val DarkColorScheme = darkColorScheme(
    primary = PreventionAccentDark,
    onPrimary = SurfaceDark,
    primaryContainer = PreventionContainerDark,
    onPrimaryContainer = PreventionTextDark,
    
    secondary = ChemicalControlAccentDark,
    onSecondary = SurfaceDark,
    secondaryContainer = ChemicalControlContainerDark,
    onSecondaryContainer = ChemicalControlTextDark,
    
    tertiary = BiologicalControlAccentDark,
    onTertiary = SurfaceDark,
    tertiaryContainer = BiologicalControlContainerDark,
    onTertiaryContainer = BiologicalControlTextDark,
    
    error = DiseaseAlertAccentDark,
    onError = SurfaceDark,
    errorContainer = DiseaseAlertContainerDark,
    onErrorContainer = DiseaseAlertTextDark,
    
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = TextSecondaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = PreventionAccentLight,
    onPrimary = SurfaceLight,
    primaryContainer = PreventionContainerLight,
    onPrimaryContainer = PreventionTextLight,
    
    secondary = ChemicalControlAccentLight,
    onSecondary = SurfaceLight,
    secondaryContainer = ChemicalControlContainerLight,
    onSecondaryContainer = ChemicalControlTextLight,
    
    tertiary = BiologicalControlAccentLight,
    onTertiary = SurfaceLight,
    tertiaryContainer = BiologicalControlContainerLight,
    onTertiaryContainer = BiologicalControlTextLight,
    
    error = DiseaseAlertAccentLight,
    onError = SurfaceLight,
    errorContainer = DiseaseAlertContainerLight,
    onErrorContainer = DiseaseAlertTextLight,
    
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceLight,
    onSurfaceVariant = TextSecondaryLight
)

@Composable
fun PotatoDiseaseAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Disable Dynamic color by default to enforce custom WCAG AA palette
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val customColors = if (darkTheme) DarkCustomColors else LightCustomColors

    CompositionLocalProvider(
        LocalCustomColors provides customColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

object AppTheme {
    val colors: CustomColors
        @Composable
        get() = LocalCustomColors.current
}