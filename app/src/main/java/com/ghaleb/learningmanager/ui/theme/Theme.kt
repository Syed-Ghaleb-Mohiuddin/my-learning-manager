package com.ghaleb.learningmanager.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Typeface
import androidx.compose.ui.unit.sp
import com.ghaleb.learningmanager.R

// Colour values live in res/values(-night)/colors.xml because the widget and the mascot drawables need them too.
// This file maps them into Compose and owns the type scale.

@Immutable
data class AppColors(
    val bg: Color,
    val card: Color,
    val text: Color,
    val textMuted: Color,
    val session: Color,
    val freeze: Color,
    val gap: Color,
    val future: Color,
    val anger: Color,
    val ink: Color,
)

val LocalAppColors = staticCompositionLocalOf<AppColors> { error("Wrap content in LearningTheme") }

object AppTheme {
    val colors: AppColors @Composable get() = LocalAppColors.current
}

val Condensed = FontFamily(
    Typeface(android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD)),
)

private val AppTypography = Typography().let { t ->
    t.copy(
        displayLarge = t.displayLarge.copy(fontFamily = Condensed, fontSize = 76.sp, lineHeight = 76.sp),
        headlineLarge = t.headlineLarge.copy(fontFamily = Condensed),
        headlineMedium = t.headlineMedium.copy(fontFamily = Condensed),
        titleLarge = t.titleLarge.copy(fontFamily = Condensed, fontWeight = FontWeight.Bold),
    )
}

@Composable
fun LearningTheme(content: @Composable () -> Unit) {
    val c = AppColors(
        bg = colorResource(R.color.bg),
        card = colorResource(R.color.card),
        text = colorResource(R.color.text),
        textMuted = colorResource(R.color.text_muted),
        session = colorResource(R.color.session),
        freeze = colorResource(R.color.freeze),
        gap = colorResource(R.color.gap),
        future = colorResource(R.color.future),
        anger = colorResource(R.color.anger),
        ink = colorResource(R.color.mascot_ink),
    )
    val base = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    val scheme = base.copy(
        primary = c.session,
        onPrimary = c.ink,
        secondaryContainer = c.gap,
        onSecondaryContainer = c.text,
        background = c.bg,
        onBackground = c.text,
        surface = c.bg,
        onSurface = c.text,
        surfaceContainer = c.card,
        surfaceContainerHigh = c.card,
        onSurfaceVariant = c.textMuted,
        outline = c.textMuted,
        error = c.anger,
    )
    CompositionLocalProvider(LocalAppColors provides c) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
    }
}
