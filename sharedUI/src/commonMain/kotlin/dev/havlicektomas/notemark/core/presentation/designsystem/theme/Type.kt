package dev.havlicektomas.notemark.core.presentation.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import notemark.sharedui.generated.resources.Res
import notemark.sharedui.generated.resources.inter_regular
import notemark.sharedui.generated.resources.jetbrainsmono_regular

/**
 * The design system ships only the Regular weights of Inter and JetBrains
 * Mono, so heavier weights are synthesised by Compose. Real Medium/SemiBold/
 * Bold TTFs can be dropped in later and registered here without touching call
 * sites.
 */
@Composable
fun interFontFamily(): FontFamily = FontFamily(
    Font(Res.font.inter_regular, FontWeight.Normal),
    Font(Res.font.inter_regular, FontWeight.Medium),
    Font(Res.font.inter_regular, FontWeight.SemiBold),
    Font(Res.font.inter_regular, FontWeight.Bold),
)

@Composable
fun jetBrainsMonoFontFamily(): FontFamily =
    FontFamily(Font(Res.font.jetbrainsmono_regular, FontWeight.Normal))

@Composable
fun noteMarkTypography(): Typography {
    val inter = interFontFamily()
    return Typography(
        // Landing hero headline — "Your notes, everywhere you are."
        displaySmall = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.Bold,
            fontSize = 34.sp,
            lineHeight = 38.sp,
            letterSpacing = (-0.022).em,
        ),
        // "Welcome back" / note title
        headlineMedium = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            letterSpacing = (-0.018).em,
        ),
        // Top app bar titles — "Log in", "Create account"
        titleLarge = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.Medium,
            fontSize = 22.sp,
            lineHeight = 28.sp,
        ),
        titleMedium = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            lineHeight = 24.sp,
        ),
        bodyLarge = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.sp,
        ),
        // Button text
        labelLarge = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 20.sp,
        ),
        // Field helper text / captions
        bodySmall = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        ),
    )
}
