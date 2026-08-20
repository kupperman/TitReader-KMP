package app.tit.reader.novel.android.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
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
import app.tit.content.core.model.ContentDetails
import app.tit.content.core.model.ContentType
import app.tit.reader.novel.android.ui.theme.*
import app.tit.shared.repository.AggregatorRepository
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

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

    // UI state
    var isDescExpanded by remember { mutableStateOf(false) }
    var isAscending by remember { mutableStateOf(true) }
    var chapterSearchQuery by remember { mutableStateOf("") }

    fun loadDetails() {
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                val fetched = kotlinx.coroutines.withTimeoutOrNull(12_000L) {
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
                    fontSize = 17.sp,
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
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Text(text = "⚠ $errorMessage", color = Color.Red, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { loadDetails() },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                        shape = RoundedCornerShape(8.dp)
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
            val firstChapter = d.chapters.firstOrNull()

            // Sort & Filter chapters
            val displayChapters = remember(d.chapters, isAscending, chapterSearchQuery) {
                val list = if (isAscending) d.chapters else d.chapters.reversed()
                if (chapterSearchQuery.isBlank()) {
                    list
                } else {
                    list.filter { it.title.contains(chapterSearchQuery, ignoreCase = true) }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                // 1. Header Area (Cover + Info + Action Buttons)
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                // Cover Image
                                Box(
                                    modifier = Modifier
                                        .width(115.dp)
                                        .height(165.dp)
                                        .clip(RoundedCornerShape(10.dp))
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
                                            fontSize = 36.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1E3A5F)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                // Meta details
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = InkDark,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Source pill badge
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(themeColor.copy(alpha = 0.12f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = item.sourceName,
                                            fontSize = 11.sp,
                                            color = themeColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    if (!item.author.isNullOrEmpty()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Tác giả: ${item.author}",
                                            fontSize = 12.sp,
                                            color = MutedGray,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Tổng: ${d.chapters.size} chương",
                                        fontSize = 12.sp,
                                        color = MutedGray
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Action buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (firstChapter != null) {
                                    Button(
                                        onClick = { onChapterClick(firstChapter, item.type) },
                                        colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Đọc từ đầu", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }

                                if (continueChapter != null) {
                                    OutlinedButton(
                                        onClick = { onChapterClick(continueChapter, item.type) },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Đọc tiếp", color = themeColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Expandable Description Card
                if (!d.description.isNullOrEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { isDescExpanded = !isDescExpanded }
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .animateContentSize()
                            ) {
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
                                    lineHeight = 19.sp,
                                    maxLines = if (isDescExpanded) Int.MAX_VALUE else 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isDescExpanded) "▲ Thu gọn" else "▼ Xem thêm",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeColor
                                )
                            }
                        }
                    }
                }

                // 3. Chapter List Controls Header
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Danh sách chương (${d.chapters.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = InkDark
                            )

                            // Sort Order Toggle Button
                            IconButton(
                                onClick = { isAscending = !isAscending },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = "Đảo thứ tự",
                                    tint = themeColor
                                )
                            }
                        }

                        // Search chapter field
                        OutlinedTextField(
                            value = chapterSearchQuery,
                            onValueChange = { chapterSearchQuery = it },
                            placeholder = { Text("Tìm tên hoặc số chương...", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceWhite,
                                unfocusedContainerColor = SurfaceWhite,
                                focusedBorderColor = themeColor,
                                unfocusedBorderColor = Color(0xFFE8E0D5)
                            )
                        )
                    }
                }

                // 4. Chapter Items List
                itemsIndexed(displayChapters, key = { _, ch -> ch.url }) { index, chapter ->
                    val isRead = repository.isChapterRead(chapter.url)
                    val chapterNum = if (isAscending) index + 1 else d.chapters.size - index

                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isRead) Color(0xFFF1ECE4) else SurfaceWhite
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clickable { onChapterClick(chapter, item.type) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = String.format("%03d", chapterNum),
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
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (isRead) {
                                Spacer(modifier = Modifier.width(6.dp))
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

                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }
}