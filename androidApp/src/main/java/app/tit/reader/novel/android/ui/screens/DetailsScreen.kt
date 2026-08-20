package app.tit.reader.novel.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tit.content.core.model.Chapter
import app.tit.content.core.model.Content
import app.tit.content.core.model.ContentDetails
import app.tit.content.core.model.ContentType
import app.tit.reader.novel.android.ui.theme.*
import app.tit.shared.repository.AggregatorRepository
import coil.compose.AsyncImage

@Composable
fun DetailsScreen(
    content: Content,
    repository: AggregatorRepository,
    onChapterClick: (Chapter, ContentType) -> Unit,
    onBackClick: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var details by remember { mutableStateOf<ContentDetails?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isFavorite by remember { mutableStateOf(repository.isBookFavorite(content.url)) }

    fun loadDetails() {
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                val fetched = kotlinx.coroutines.withTimeoutOrNull(10_000L) {
                    if (content.type == ContentType.NOVEL) {
                        repository.getNovelDetails(content.sourceId, content.url)
                    } else {
                        repository.getMangaDetails(content.sourceId, content.url)
                    }
                }
                if (fetched != null) {
                    details = fetched
                } else {
                    errorMessage = "Quá thời gian phản hồi (Timeout). Vui lòng thử lại sau."
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Lỗi tải thông tin"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(content.url) {
        loadDetails()
        isFavorite = repository.isBookFavorite(content.url)
    }

    val themeColor = if (content.type == ContentType.NOVEL) AccentOrange else Color(0xFF3B82F6)

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgCream)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = InkDark)
                }
                Text(
                    text = if (content.type == ContentType.NOVEL) "Chi tiết truyện chữ" else "Chi tiết truyện tranh",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkDark
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = {
                        isFavorite = repository.toggleBookFavorite(content)
                    }
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Yêu thích",
                        tint = if (isFavorite) Color(0xFFEF4444) else MutedGray
                    )
                }
            }
        },
        containerColor = BgCream
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = themeColor)
            }
        } else if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "⚠ $errorMessage", color = Color.Red, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { loadDetails() },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                    ) {
                        Text("Thử lại")
                    }
                }
            }
        } else {
            val d = details ?: return@Scaffold
            val item = d.content
            val bookInLib = repository.storage.getLibraryBook(content.url)
            val continueChapter = d.chapters.find { it.url == bookInLib?.lastReadChapterUrl }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .width(110.dp)
                                .height(154.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE8E0D5)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!item.coverUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = item.coverUrl,
                                    contentDescription = item.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    text = item.title.take(1).uppercase(),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E3A5F)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = InkDark
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Nguồn: ${item.sourceName}",
                                fontSize = 12.sp,
                                color = themeColor,
                                fontWeight = FontWeight.Bold
                            )

                            if (!item.author.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tác giả: ${item.author}",
                                    fontSize = 13.sp,
                                    color = MutedGray
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (continueChapter != null) {
                                    Button(
                                        onClick = { onChapterClick(continueChapter, item.type) },
                                        colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Đọc tiếp", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }

                                if (d.chapters.isNotEmpty()) {
                                    OutlinedButton(
                                        onClick = { onChapterClick(d.chapters.first(), item.type) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("Đọc từ đầu", color = themeColor, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                if (!d.description.isNullOrEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Giới thiệu",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = InkDark
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = d.description ?: "",
                                    fontSize = 13.sp,
                                    color = MutedGray,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Danh sách ${d.chapters.size} chương / chap",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = InkDark,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }

                itemsIndexed(d.chapters) { index, chapter ->
                    val isRead = repository.isChapterRead(chapter.url)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isRead) Color(0xFFF3EFE6) else SurfaceWhite)
                            .clickable { onChapterClick(chapter, item.type) }
                            .padding(horizontal = 14.dp, vertical = 11.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = String.format("%03d", index + 1),
                                color = if (isRead) MutedGray else themeColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(36.dp)
                            )
                            Text(
                                text = chapter.title,
                                color = if (isRead) MutedGray else InkDark,
                                fontSize = 13.sp,
                                fontWeight = if (isRead) FontWeight.Normal else FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            if (isRead) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Đã đọc",
                                    tint = MutedGray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}