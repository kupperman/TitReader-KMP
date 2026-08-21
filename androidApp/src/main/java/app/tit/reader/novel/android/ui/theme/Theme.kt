package app.tit.reader.novel.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

val BgCream = Color(0xFFFAF6EE)
val SurfaceWhite = Color(0xFFFFFFFF)
val AccentOrange = Color(0xFFE08E0B)
val InkDark = Color(0xFF1F1A14)
val TextPrimary = InkDark
val MutedGray = Color(0xFF7A7063)
val BorderLight = Color(0xFFE8DFD1)

private val LightColorScheme = lightColorScheme(
    primary = AccentOrange,
    background = BgCream,
    surface = SurfaceWhite,
    onPrimary = Color.White,
    onBackground = InkDark,
    onSurface = InkDark
)

@Composable
fun TitReaderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}

val PlaceholderPalette = listOf(
    Pair(Color(0xFFE8DED0), Color(0xFF3B3025)),
    Pair(Color(0xFFC9F2ED), Color(0xFF176B63)),
    Pair(Color(0xFFF6DEC0), Color(0xFF8A4B08)),
    Pair(Color(0xFFDCE8F7), Color(0xFF1E4C7A)),
)

val TitReaderTypography = androidx.compose.material3.Typography(
    titleMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 19.sp,
        color = InkDark,
    ),
    labelSmall = androidx.compose.ui.text.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontSize = 11.sp,
        color = MutedGray,
    ),
)

val PlaceholderLetterStyle = androidx.compose.ui.text.TextStyle(
    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
    fontSize = 48.sp,
)
