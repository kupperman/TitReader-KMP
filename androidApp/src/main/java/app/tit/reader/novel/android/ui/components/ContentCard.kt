package app.tit.reader.novel.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.tit.content.core.model.Content
import app.tit.content.core.model.ContentType
import app.tit.reader.novel.android.ui.theme.*
import coil.compose.AsyncImage

private fun representativeLetter(title: String): String =
    title.trim().firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "?"

@Composable
fun ContentCard(content: Content, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val (bgColor, letterColor) = remember(content.title) {
        PlaceholderPalette[content.title.hashCode().mod(PlaceholderPalette.size)]
    }
    val chapterText = if (content.latestChapter.isNullOrBlank()) "Chưa có chương" else content.latestChapter ?: ""
    Column(
        modifier = modifier.clickable(onClick = onClick).semantics {
            contentDescription = "${content.title}, ${if (content.type == ContentType.NOVEL) "truyện chữ" else "truyện tranh"}, $chapterText"
        }
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(16.dp)).background(bgColor),
            contentAlignment = Alignment.Center,
        ) {
            Text(representativeLetter(content.title), style = PlaceholderLetterStyle, color = letterColor)
            if (!content.coverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = content.coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Box(
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)
                    .clip(RoundedCornerShape(10.dp)).background(PlaceholderPalette[1].first)
                    .padding(horizontal = 9.dp, vertical = 3.dp)
            ) {
                Text(if (content.type == ContentType.NOVEL) "CHỮ" else "TRANH", color = PlaceholderPalette[1].second,
                    style = TitReaderTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold))
            }
        }
        Text(content.title, style = TitReaderTypography.titleMedium, maxLines = 2,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
        Text(chapterText, style = TitReaderTypography.labelSmall, modifier = Modifier.padding(top = 2.dp))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF9F0)
@Composable private fun NovelCardPreview_CoAnh() = PreviewContentCard("Thập Niên 70: Mỹ Nhân Đến Tây Bắc", "Chương 200", true)
@Preview(showBackground = true, backgroundColor = 0xFFFFF9F0)
@Composable private fun NovelCardPreview_LoiAnh() = PreviewContentCard("Quỷ Vương Chờ Ta Một Trăm Năm", null, false)
@Preview(showBackground = true, backgroundColor = 0xFFFFF9F0)
@Composable private fun NovelCardPreview_TieuDeDai() = PreviewContentCard("[Dịch] 999 Lần Trọng Sinh Của Nữ Phụ Phản Diện Trong Truyện Ngôn Tình Thập Niên 80", "Chương 9999", true)
@Preview(showBackground = true, backgroundColor = 0xFFFFF9F0)
@Composable private fun NovelCardPreview_CJK() = PreviewContentCard("重生之神级学霸", "Chương 88", true)

@Composable private fun PreviewContentCard(title: String, chapter: String?, novel: Boolean) {
    ContentCard(Content("preview-$title", title, "", null, null, chapter, if (novel) ContentType.NOVEL else ContentType.MANGA, "PREVIEW", "Preview"), {}, Modifier.width(170.dp))
}
