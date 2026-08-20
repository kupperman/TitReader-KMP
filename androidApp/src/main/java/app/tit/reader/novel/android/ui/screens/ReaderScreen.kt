package app.tit.reader.novel.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tit.content.core.model.Chapter
import app.tit.content.core.model.ChapterContent
import app.tit.content.core.model.ContentType
import app.tit.reader.novel.android.ui.theme.AccentOrange
import app.tit.shared.repository.AggregatorRepository
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

enum class ReaderTheme(val bg: Color, val text: Color, val nameLabel: String) {
    LIGHT(Color(0xFFFAF6EE), Color(0xFF1E1914), "Sáng"),
    SEPIA(Color(0xFFEEE4CC), Color(0xFF3B3326), "Vàng"),
    DARK(Color(0xFF141F2D), Color(0xFFE2DAD0), "Tối"),
    BLACK(Color(0xFF000000), Color(0xFFCCCCCC), "Đen")
}

@Composable
fun ReaderScreen(
    chapter: Chapter,
    type: ContentType,
    onBackClick: () -> Unit
) {
    val repository = remember { AggregatorRepository() }
    val scope = rememberCoroutineScope()

    var currentChapterUrl by remember { mutableStateOf(chapter.url) }
    var textContent by remember { mutableStateOf<ChapterContent.Text?>(null) }
    var imageContent by remember { mutableStateOf<ChapterContent.ImagePages?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var fontSize by remember { mutableFloatStateOf(18f) }
    var readerTheme by remember { mutableStateOf(ReaderTheme.LIGHT) }
    var showControls by remember { mutableStateOf(false) }

    fun loadChapter(url: String) {
        currentChapterUrl = url
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
                        textContent = fetched as ChapterContent.Text
                    } else {
                        imageContent = fetched as ChapterContent.ImagePages
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

    val currentThemeColor = if (type == ContentType.NOVEL) readerTheme.bg else Color.Black
    val currentTextColor = if (type == ContentType.NOVEL) readerTheme.text else Color.White

    Scaffold(
        topBar = {
            if (showControls) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(currentThemeColor)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = currentTextColor)
                    }
                    Text(
                        text = textContent?.title ?: imageContent?.title ?: chapter.title,
                        color = currentTextColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        bottomBar = {
            if (showControls && type == ContentType.NOVEL) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(readerTheme.bg)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Cỡ chữ: ${fontSize.toInt()}pt", color = readerTheme.text, fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { fontSize = (fontSize - 2).coerceAtLeast(12f) },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("A-")
                            }
                            Button(
                                onClick = { fontSize = (fontSize + 2).coerceAtMost(32f) },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("A+")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReaderTheme.entries.forEach { th ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(th.bg)
                                    .clickable { readerTheme = th }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = th.nameLabel,
                                    color = th.text,
                                    fontSize = 12.sp,
                                    fontWeight = if (readerTheme == th) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = currentThemeColor
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentOrange)
            }
        } else if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "⚠ $errorMessage", color = Color.Red)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { loadChapter(currentChapterUrl) }, colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)) {
                        Text("Thử lại")
                    }
                }
            }
        } else {
            if (type == ContentType.NOVEL) {
                val txt = textContent ?: return@Scaffold
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { showControls = !showControls }
                        .padding(innerPadding)
                        .padding(horizontal = 18.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = txt.title,
                            fontSize = (fontSize + 4).sp,
                            fontWeight = FontWeight.Bold,
                            color = readerTheme.text,
                            lineHeight = (fontSize + 10).sp,
                            modifier = Modifier.padding(bottom = 20.dp)
                        )
                    }

                    items(txt.paragraphs) { paragraph ->
                        Text(
                            text = paragraph,
                            fontSize = fontSize.sp,
                            color = readerTheme.text,
                            lineHeight = (fontSize * 1.55f).sp,
                            modifier = Modifier.padding(bottom = 14.dp)
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 36.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val prevUrl = txt.prevChapterUrl
                            if (!prevUrl.isNullOrEmpty()) {
                                Button(
                                    onClick = { loadChapter(prevUrl) },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
                                ) {
                                    Text("‹ Chương trước")
                                }
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }

                            val nextUrl = txt.nextChapterUrl
                            if (!nextUrl.isNullOrEmpty()) {
                                Button(
                                    onClick = { loadChapter(nextUrl) },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
                                ) {
                                    Text("Chương tiếp ›")
                                }
                            }
                        }
                    }
                }
            } else {
                // Manga Image Reader
                val img = imageContent ?: return@Scaffold
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { showControls = !showControls }
                        .padding(innerPadding)
                ) {
                    items(img.imageUrls) { imgUrl ->
                        AsyncImage(
                            model = imgUrl,
                            contentDescription = null,
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val prevUrl = img.prevChapterUrl
                            if (!prevUrl.isNullOrEmpty()) {
                                Button(
                                    onClick = { loadChapter(prevUrl) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                                ) {
                                    Text("‹ Chap trước")
                                }
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }

                            val nextUrl = img.nextChapterUrl
                            if (!nextUrl.isNullOrEmpty()) {
                                Button(
                                    onClick = { loadChapter(nextUrl) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                                ) {
                                    Text("Chap tiếp ›")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
