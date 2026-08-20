package app.tit.reader.novel.android.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import app.tit.reader.novel.android.util.OfflineExportHelper
import app.tit.shared.model.MangaReadingMode
import app.tit.shared.model.NovelThemeType
import app.tit.shared.model.ReaderSettings
import app.tit.shared.model.ReadingHistoryItem
import app.tit.shared.repository.AggregatorRepository
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    chapter: Chapter,
    type: ContentType,
    contentMeta: Content? = null,
    repository: AggregatorRepository,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentChapterUrl by remember { mutableStateOf(chapter.url) }
    var currentChapterTitle by remember { mutableStateOf(chapter.title) }
    var textContent by remember { mutableStateOf<ChapterContent.Text?>(null) }
    var imageContent by remember { mutableStateOf<ChapterContent.ImagePages?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var readerSettings by remember { mutableStateOf(repository.getReaderSettings()) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }

    val listState = rememberLazyListState()

    // Export Offline Launcher
    val exportMangaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/x-cbz")
    ) { uri ->
        if (uri != null && imageContent != null) {
            scope.launch {
                Toast.makeText(context, "Đang tải và đóng gói CBZ...", Toast.LENGTH_SHORT).show()
                val okHttp = OkHttpClient()
                val ok = OfflineExportHelper.exportMangaChapterToCbz(context, okHttp, imageContent!!.imageUrls, uri)
                if (ok) {
                    Toast.makeText(context, "Xuất file CBZ thành công!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Lỗi khi xuất file CBZ", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val exportNovelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null && textContent != null) {
            scope.launch {
                val ok = OfflineExportHelper.exportNovelChapterToTxt(context, textContent!!.title, textContent!!.text, uri)
                if (ok) {
                    Toast.makeText(context, "Xuất file TXT thành công!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Lỗi khi xuất file TXT", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun loadChapter(url: String) {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val resolvedSourceId = chapter.sourceId.ifEmpty { contentMeta?.sourceId ?: "" }
                kotlinx.coroutines.withTimeout(15_000L) {
                    if (type == ContentType.NOVEL) {
                        val res = repository.getNovelChapterContent(resolvedSourceId, url)
                        textContent = res
                        currentChapterTitle = res.title
                        currentChapterUrl = res.chapterUrl
                    } else {
                        val res = repository.getMangaChapterContent(resolvedSourceId, url)
                        imageContent = res
                        currentChapterTitle = res.title
                        currentChapterUrl = res.chapterUrl
                    }
                }

                // Ghi nhận lịch sử đọc
                if (contentMeta != null) {
                    repository.recordReadingHistory(
                        ReadingHistoryItem(
                            content = contentMeta,
                            chapterUrl = currentChapterUrl,
                            chapterTitle = currentChapterTitle,
                            readAt = System.currentTimeMillis()
                        )
                    )
                }

                if (type == ContentType.NOVEL) {
                    runCatching { listState.scrollToItem(0) }
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Lỗi tải nội dung chương (Timeout hoặc mạng gián đoạn)"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(chapter.url) {
        loadChapter(chapter.url)
    }

    val bgColor = when (readerSettings.novelTheme) {
        NovelThemeType.LIGHT -> Color(0xFFFAF6EE)
        NovelThemeType.SEPIA -> Color(0xFFEEE4CC)
        NovelThemeType.DARK -> Color(0xFF1E293B)
        NovelThemeType.AMOLED -> Color(0xFF000000)
    }

    val textColor = when (readerSettings.novelTheme) {
        NovelThemeType.LIGHT -> Color(0xFF1E1914)
        NovelThemeType.SEPIA -> Color(0xFF2C2218)
        NovelThemeType.DARK -> Color(0xFFF1F5F9)
        NovelThemeType.AMOLED -> Color(0xFFE2E8F0)
    }

    val fontFamily = when (readerSettings.novelFontFamily) {
        "SERIF" -> FontFamily.Serif
        "MONOSPACE" -> FontFamily.Monospace
        "CURSIVE" -> FontFamily.Cursive
        else -> FontFamily.Default
    }

    val prevUrl = textContent?.prevChapterUrl ?: imageContent?.prevChapterUrl
    val nextUrl = textContent?.nextChapterUrl ?: imageContent?.nextChapterUrl

    Scaffold(
        topBar = {
            if (showControls) {
                TopAppBar(
                    title = {
                        Text(
                            text = currentChapterTitle,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            color = if (type == ContentType.NOVEL) textColor else Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Quay lại",
                                tint = if (type == ContentType.NOVEL) textColor else Color.White
                            )
                        }
                    },
                    actions = {
                        // Nút Tải Offline (.cbz / .txt)
                        IconButton(onClick = {
                            val cleanTitle = currentChapterTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                            if (type == ContentType.MANGA) {
                                exportMangaLauncher.launch("$cleanTitle.cbz")
                            } else {
                                exportNovelLauncher.launch("$cleanTitle.txt")
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Xuất Offline",
                                tint = if (type == ContentType.NOVEL) textColor else Color.White
                            )
                        }

                        IconButton(onClick = { showSettingsSheet = true }) {
                            Icon(
                                imageVector = if (type == ContentType.NOVEL) Icons.Default.FormatSize else Icons.Default.Settings,
                                contentDescription = "Cài đặt đọc",
                                tint = if (type == ContentType.NOVEL) textColor else Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (type == ContentType.NOVEL) {
                            if (readerSettings.novelTheme == NovelThemeType.AMOLED) Color(0xFF111111) else Color(0xFFF3EFE6)
                        } else Color(0xCC000000)
                    )
                )
            }
        },
        bottomBar = {
            if (showControls && !isLoading && errorMessage == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (type == ContentType.NOVEL && readerSettings.novelTheme == NovelThemeType.AMOLED) Color(0xFF111111) else if (type == ContentType.NOVEL) Color(0xFFF3EFE6) else Color(0xCC000000))
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
                .pointerInput(readerSettings.tapZonesEnabled) {
                    if (readerSettings.tapZonesEnabled) {
                        detectTapGestures { offset ->
                            val screenWidth = size.width
                            val x = offset.x
                            when {
                                x < screenWidth * 0.3f -> {
                                    // 30% trái: Cuộn lên / trang trước
                                    scope.launch {
                                        if (type == ContentType.NOVEL) {
                                            listState.animateScrollToItem(maxOf(0, listState.firstVisibleItemIndex - 1))
                                        }
                                    }
                                }
                                x > screenWidth * 0.7f -> {
                                    // 30% phải: Cuộn xuống / trang sau
                                    scope.launch {
                                        if (type == ContentType.NOVEL) {
                                            listState.animateScrollToItem(listState.firstVisibleItemIndex + 1)
                                        }
                                    }
                                }
                                else -> {
                                    // 40% giữa: Bật tắt menu
                                    showControls = !showControls
                                }
                            }
                        }
                    } else {
                        detectTapGestures {
                            showControls = !showControls
                        }
                    }
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
                        .padding(horizontal = readerSettings.novelHorizontalPadding.dp)
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
                            LazyColumn(
                                state = listState,
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
                        MangaReadingMode.DUAL_PAGE_LTR, MangaReadingMode.DUAL_PAGE_RTL -> {
                            // --- DUAL PAGE MODE (Trang đôi) ---
                            val isRtl = readerSettings.mangaMode == MangaReadingMode.DUAL_PAGE_RTL
                            val pairs = remember(images) { images.chunked(2) }
                            val dualPagerState = rememberPagerState(pageCount = { pairs.size })

                            Box(modifier = Modifier.fillMaxSize()) {
                                HorizontalPager(
                                    state = dualPagerState,
                                    reverseLayout = isRtl,
                                    modifier = Modifier.fillMaxSize()
                                ) { pairIndex ->
                                    val currentPair = pairs[pairIndex]
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isRtl && currentPair.size == 2) {
                                            // Trang 2 bên trái, Trang 1 bên phải (chuẩn Manga Nhật)
                                            Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                                                AsyncImage(model = currentPair[1], contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                                            }
                                            Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                                                AsyncImage(model = currentPair[0], contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                                            }
                                        } else {
                                            currentPair.forEach { imgUrl ->
                                                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                                                    AsyncImage(model = imgUrl, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                                                }
                                            }
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 16.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x99000000))
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    val startPage = dualPagerState.currentPage * 2 + 1
                                    val endPage = minOf(images.size, startPage + 1)
                                    Text(
                                        text = if (startPage == endPage) "Trang $startPage / ${images.size}" else "Trang $startPage-$endPage / ${images.size}",
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

    if (showSettingsSheet) {
        ReaderSettingsSheet(
            contentType = type,
            currentSettings = readerSettings,
            onSettingsChanged = { updated ->
                readerSettings = updated
                repository.saveReaderSettings(updated)
            },
            onDismiss = { showSettingsSheet = false }
        )
    }
}