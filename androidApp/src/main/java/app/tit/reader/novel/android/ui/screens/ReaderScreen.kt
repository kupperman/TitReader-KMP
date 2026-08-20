package app.tit.reader.novel.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tit.content.core.model.Chapter
import app.tit.content.core.model.ChapterContent
import app.tit.content.core.model.Content
import app.tit.content.core.model.ContentType
import app.tit.reader.novel.android.ui.components.ReaderSettingsSheet
import app.tit.reader.novel.android.ui.theme.AccentOrange
import app.tit.shared.model.MangaReadingMode
import app.tit.shared.model.NovelThemeType
import app.tit.shared.model.ReaderSettings
import app.tit.shared.model.ReadingHistoryItem
import app.tit.shared.repository.AggregatorRepository
import coil.compose.AsyncImage

@Composable
fun ReaderScreen(
    chapter: Chapter,
    type: ContentType,
    contentMeta: Content? = null,
    repository: AggregatorRepository,
    onBackClick: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var currentChapterUrl by remember { mutableStateOf(chapter.url) }
    var currentChapterTitle by remember { mutableStateOf(chapter.title) }
    var textContent by remember { mutableStateOf<ChapterContent.Text?>(null) }
    var imageContent by remember { mutableStateOf<ChapterContent.ImagePages?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var readerSettings by remember { mutableStateOf(repository.getReaderSettings()) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(false) }

    fun loadChapter(url: String, title: String = "") {
        currentChapterUrl = url
        if (title.isNotEmpty()) currentChapterTitle = title
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                val fetched = kotlinx.coroutines.withTimeoutOrNull(15_000L) {
                    if (type == ContentType.NOVEL) {
                        repository.getNovelChapterContent(chapter.sourceId, url)
                    } else {
                        repository.getMangaChapterContent(chapter.sourceId, url)
                    }
                }
                if (fetched != null) {
                    if (type == ContentType.NOVEL) {
                        val novelResult = fetched as ChapterContent.Text
                        textContent = novelResult
                        currentChapterTitle = novelResult.title
                    } else {
                        val mangaResult = fetched as ChapterContent.ImagePages
                        imageContent = mangaResult
                        currentChapterTitle = mangaResult.title
                    }

                    // Tự động lưu lịch sử đọc
                    if (contentMeta != null) {
                        repository.recordReadingHistory(
                            ReadingHistoryItem(
                                content = contentMeta,
                                chapterUrl = url,
                                chapterTitle = currentChapterTitle,
                                readAt = System.currentTimeMillis()
                            )
                        )
                    }
                } else {
                    errorMessage = "Quá thời gian tải chương (Timeout). Vui lòng kiểm tra kết nối mạng và thử lại."
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Lỗi tải nội dung"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(currentChapterUrl) {
        loadChapter(currentChapterUrl)
    }

    // Determine colors based on NovelThemeType
    val (bgColor, textColor) = when (readerSettings.novelTheme) {
        NovelThemeType.LIGHT -> Color(0xFFFAF6EE) to Color(0xFF1E1914)
        NovelThemeType.SEPIA -> Color(0xFFEEE4CC) to Color(0xFF3B3326)
        NovelThemeType.DARK -> Color(0xFF1E293B) to Color(0xFFE2E8F0)
        NovelThemeType.AMOLED -> Color(0xFF000000) to Color(0xFFD1D5DB)
    }

    val fontFamily = when (readerSettings.novelFontFamily) {
        "SERIF" -> FontFamily.Serif
        "MONOSPACE" -> FontFamily.Monospace
        "CURSIVE" -> FontFamily.Cursive
        else -> FontFamily.Default
    }

    val listState = rememberLazyListState()

    if (showSettingsSheet) {
        ReaderSettingsSheet(
            contentType = type,
            currentSettings = readerSettings,
            onSettingsChanged = { newSettings ->
                readerSettings = newSettings
                repository.saveReaderSettings(newSettings)
            },
            onDismiss = { showSettingsSheet = false }
        )
    }

    Scaffold(
        topBar = {
            if (showControls) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (readerSettings.novelTheme == NovelThemeType.AMOLED) Color(0xFF111111) else Color(0xFFF3EFE6))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = textColor)
                    }
                    Text(
                        text = currentChapterTitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Cài đặt đọc", tint = textColor)
                    }
                }
            }
        },
        bottomBar = {
            if (showControls) {
                val prevUrl = if (type == ContentType.NOVEL) textContent?.prevChapterUrl else imageContent?.prevChapterUrl
                val nextUrl = if (type == ContentType.NOVEL) textContent?.nextChapterUrl else imageContent?.nextChapterUrl

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (readerSettings.novelTheme == NovelThemeType.AMOLED) Color(0xFF111111) else Color(0xFFF3EFE6))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { prevUrl?.let { loadChapter(it) } },
                        enabled = !prevUrl.isNullOrEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = null)
                        Text("Chương trước")
                    }

                    Button(
                        onClick = { nextUrl?.let { loadChapter(it) } },
                        enabled = !nextUrl.isNullOrEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Chương sau")
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            }
        },
        containerColor = if (type == ContentType.NOVEL) bgColor else Color(0xFF121212)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    showControls = !showControls
                }
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentOrange)
                }
            } else if (errorMessage != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Text(text = "⚠ $errorMessage", color = Color.Red, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { loadChapter(currentChapterUrl) },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
                        ) {
                            Text("Thử lại")
                        }
                    }
                }
            } else if (type == ContentType.NOVEL && textContent != null) {
                // --- NOVEL TEXT VIEW ---
                val txt = textContent!!
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = txt.title,
                            fontSize = (readerSettings.novelFontSize + 4).sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            fontFamily = fontFamily
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    item {
                        Text(
                            text = txt.text,
                            fontSize = readerSettings.novelFontSize.sp,
                            color = textColor,
                            lineHeight = (readerSettings.novelFontSize * readerSettings.novelLineHeightMultiplier).sp,
                            fontFamily = fontFamily
                        )
                        Spacer(modifier = Modifier.height(36.dp))
                    }
                }
            } else if (type == ContentType.MANGA && imageContent != null) {
                // --- MANGA IMAGE VIEW ---
                val images = imageContent!!.imageUrls

                if (images.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Không có trang ảnh nào cho chapter này", color = Color.White)
                    }
                } else {
                    when (readerSettings.mangaMode) {
                        MangaReadingMode.WEBTOON -> {
                            // Cuộn dọc liên tục
                            LazyColumn(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(images) { imgUrl ->
                                    AsyncImage(
                                        model = imgUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.FillWidth,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                        MangaReadingMode.LTR, MangaReadingMode.RTL -> {
                            // Lật trang ngang Pager
                            val isRtl = readerSettings.mangaMode == MangaReadingMode.RTL
                            val pagerState = rememberPagerState(pageCount = { images.size })

                            Box(modifier = Modifier.fillMaxSize()) {
                                HorizontalPager(
                                    state = pagerState,
                                    reverseLayout = isRtl,
                                    modifier = Modifier.fillMaxSize()
                                ) { pageIndex ->
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = images[pageIndex],
                                            contentDescription = "Trang ${pageIndex + 1}",
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                // Page Badge Indicator
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 16.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x99000000))
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${pagerState.currentPage + 1} / ${images.size}",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}