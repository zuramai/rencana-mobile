package com.zuramai.rencana.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zuramai.rencana.resources.Res
import com.zuramai.rencana.resources.nunito_bold
import com.zuramai.rencana.resources.nunito_extrabold
import com.zuramai.rencana.resources.nunito_regular
import com.zuramai.rencana.resources.nunito_semibold
import org.jetbrains.compose.resources.Font

/** Brand palette (matches the web design tokens). */
object RColors {
    val Primary = Color(0xFF14B8A6)
    val PrimaryDark = Color(0xFF0F766E)
    val Coral = Color(0xFFFB923C)
    val Sun = Color(0xFFFACC15)
    val Berry = Color(0xFFF472B6)
    val Background = Color(0xFFFFFBF5)
    val Surface = Color(0xFFFFFFFF)
    val Text = Color(0xFF1C1917)
    val Muted = Color(0xFF78716C)
    val Border = Color(0xFFF0E6DA)
    val Danger = Color(0xFFEF4444)
    val DangerSoft = Color(0xFFFEE2E2)
}

/** Corner radii used across components. */
object RShapes {
    val Field = RoundedCornerShape(14.dp)
    val Card = RoundedCornerShape(20.dp)
    val Pill = RoundedCornerShape(percent = 50)
    val Sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val Dialog = RoundedCornerShape(28.dp)
}

/** Type scale: Display 28/34 ExtraBold, Title 20/26 Bold, Body 16/24 Regular and SemiBold, Caption 13/18 SemiBold. */
class RTypography(val family: FontFamily) {
    val display = TextStyle(fontFamily = family, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 34.sp, color = RColors.Text)
    val title = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp, color = RColors.Text)
    val body = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, color = RColors.Text)
    val bodySemi = body.copy(fontWeight = FontWeight.SemiBold)
    val caption = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp, color = RColors.Muted)
}

/** Access to the app's typography: `RencanaType.title`, `RencanaType.body`, ... Valid inside [RencanaTheme]. */
object RencanaType {
    val current: RTypography
        @Composable get() = LocalRTypography.current
}

internal val LocalRTypography = androidx.compose.runtime.staticCompositionLocalOf<RTypography> {
    error("RencanaTheme missing")
}

/** App theme: Nunito, brand colours, pill/card shapes. Wrap the whole app in it. */
@Composable
fun RencanaTheme(content: @Composable () -> Unit) {
    val family = FontFamily(
        Font(Res.font.nunito_regular, FontWeight.Normal),
        Font(Res.font.nunito_semibold, FontWeight.SemiBold),
        Font(Res.font.nunito_bold, FontWeight.Bold),
        Font(Res.font.nunito_extrabold, FontWeight.ExtraBold)
    )
    val rType = remember(family) { RTypography(family) }
    val base = Typography()
    val typography = remember(family) {
        base.copy(
            displayLarge = base.displayLarge.copy(fontFamily = family),
            displayMedium = base.displayMedium.copy(fontFamily = family),
            displaySmall = base.displaySmall.copy(fontFamily = family),
            headlineLarge = rType.display,
            headlineMedium = base.headlineMedium.copy(fontFamily = family),
            headlineSmall = base.headlineSmall.copy(fontFamily = family),
            titleLarge = rType.title,
            titleMedium = rType.bodySemi,
            titleSmall = base.titleSmall.copy(fontFamily = family),
            bodyLarge = rType.body,
            bodyMedium = rType.body.copy(fontSize = 14.sp, lineHeight = 20.sp),
            bodySmall = rType.caption,
            labelLarge = rType.bodySemi,
            labelMedium = rType.caption,
            labelSmall = rType.caption.copy(fontSize = 11.sp, lineHeight = 16.sp)
        )
    }
    val colors = lightColorScheme(
        primary = RColors.Primary,
        onPrimary = Color.White,
        primaryContainer = RColors.Primary.copy(alpha = 0.15f),
        onPrimaryContainer = RColors.PrimaryDark,
        secondary = RColors.Coral,
        onSecondary = Color.White,
        tertiary = RColors.Berry,
        background = RColors.Background,
        onBackground = RColors.Text,
        surface = RColors.Surface,
        onSurface = RColors.Text,
        surfaceVariant = RColors.Background,
        onSurfaceVariant = RColors.Muted,
        surfaceContainerLowest = RColors.Surface,
        surfaceContainerLow = RColors.Surface,
        surfaceContainer = RColors.Surface,
        surfaceContainerHigh = RColors.Surface,
        surfaceContainerHighest = RColors.Surface,
        outline = RColors.Border,
        outlineVariant = RColors.Border,
        error = RColors.Danger,
        onError = Color.White
    )
    androidx.compose.runtime.CompositionLocalProvider(LocalRTypography provides rType) {
        MaterialTheme(
            colorScheme = colors,
            typography = typography,
            shapes = Shapes(
                extraSmall = RShapes.Field, small = RShapes.Field, medium = RShapes.Card,
                large = RShapes.Card, extraLarge = RShapes.Dialog
            ),
            content = content
        )
    }
}
