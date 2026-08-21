package app.tit.reader.novel.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import app.tit.content.core.model.ContentType
import app.tit.reader.novel.android.ui.theme.*

@Composable
fun TopFilterRow(selectedType: ContentType, onTypeSelect: (ContentType) -> Unit, onSearchClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SegmentButton("Truyện chữ", selectedType == ContentType.NOVEL, { onTypeSelect(ContentType.NOVEL) }, Modifier.weight(1f))
        SegmentButton("Truyện tranh", selectedType == ContentType.MANGA, { onTypeSelect(ContentType.MANGA) }, Modifier.weight(1f))
        Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceWhite).clickable(onClick = onSearchClick).semantics { contentDescription = "Tìm kiếm" }, contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = InkDark)
        }
    }
}

@Composable
private fun SegmentButton(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(14.dp)).background(if (selected) AccentOrange else SurfaceWhite).clickable(onClick = onClick).padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        Text(label, color = if (selected) Color.White else InkDark, style = TitReaderTypography.labelSmall)
    }
}