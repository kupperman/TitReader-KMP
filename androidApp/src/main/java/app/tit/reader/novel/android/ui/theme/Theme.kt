package app.tit.reader.novel.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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
