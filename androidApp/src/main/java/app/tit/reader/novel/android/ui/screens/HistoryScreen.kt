package app.tit.reader.novel.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tit.content.core.model.Chapter
import app.tit.content.core.model.Content
import app.tit.content.core.model.ContentType
import app.tit.reader.novel.android.ui.theme.*
import app.tit.shared.model.ReadingHistoryItem
import app.tit.shared.repository.AggregatorRepository
import coil.compose.AsyncImage

@Composable
fun HistoryScreen(
    repository: AggregatorRepository,
    onContentClick: (Content) -> Unit,
    onContinueReadClick: (Content, Chapter) -> Unit
) {
    var historyItems by remember { mutableStateOf<List<ReadingHistoryItem>>(emptyList()) }
    var showConfirmClear by remember { mutableStateOf(false) }

    fun refreshHistory() {
        historyItems = repository.getReadingHistory()
    }

    LaunchedEffect(Unit) {
        refreshHistory()
    }

    if (showConfirmClear) {
        AlertDialog(
            onDismissRequest = { showConfirmClear = false },
            title = { Text("Xác nhận xóa lịch sử") },
            text = { Text("Bạn có chắc chắn muốn xóa toàn bộ lịch sử đọc truyện?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        repository.storage.clearHistory()
                        refreshHistory()
                        showConfirmClear = false
                    }
                ) {
                    Text("Xóa hết", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClear = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgCream)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = AccentOrange,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Lịch Sử Đọc",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.weight(1f))
                if (historyItems.isNotEmpty()) {
                    IconButton(onClick = { showConfirmClear = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Xóa lịch sử",
                            tint = MutedGray
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BgCream)
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            if (historyItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MutedGray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Chưa có lịch sử đọc",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Các chương truyện bạn đã đọc sẽ tự động xuất hiện ở đây",
                            fontSize = 13.sp,
                            color = MutedGray
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(historyItems, key = { it.content.url }) { item ->
                        HistoryCard(
                            item = item,
                            onContentClick = { onContentClick(item.content) },
                            onContinueClick = {
                                onContinueReadClick(
                                    item.content,
                                    Chapter(
                                        id = item.chapterUrl,
                                        title = item.chapterTitle,
                                        url = item.chapterUrl,
                                        sourceId = item.content.sourceId
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryCard(
    item: ReadingHistoryItem,
    onContentClick: () -> Unit,
    onContinueClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onContentClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover Image
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(84.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE8E0D5)),
                contentAlignment = Alignment.Center
            ) {
                if (!item.content.coverUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = item.content.coverUrl,
                        contentDescription = item.content.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = item.content.title.take(1).uppercase(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E3A5F)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.content.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Đang đọc: ${item.chapterTitle}",
                    fontSize = 13.sp,
                    color = AccentOrange,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val tagBg = if (item.content.type == ContentType.NOVEL) Color(0xFFEBF5FF) else Color(0xFFF3E8FF)
                    val tagColor = if (item.content.type == ContentType.NOVEL) Color(0xFF2563EB) else Color(0xFF7C3AED)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(tagBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (item.content.type == ContentType.NOVEL) "Truyện chữ" else "Truyện tranh",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = tagColor
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.content.sourceName,
                        fontSize = 11.sp,
                        color = MutedGray
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Continue reading button
            IconButton(
                onClick = { onContinueClick() },
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFFF0EB))
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Đọc tiếp",
                    tint = AccentOrange
                )
            }
        }
    }
}