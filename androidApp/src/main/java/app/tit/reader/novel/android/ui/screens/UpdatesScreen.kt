package app.tit.reader.novel.android.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tit.content.core.model.Content
import app.tit.reader.novel.android.ui.theme.*
import app.tit.shared.model.BookUpdateItem
import app.tit.shared.repository.AggregatorRepository
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatesScreen(
    repository: AggregatorRepository,
    onContentClick: (Content) -> Unit
) {
    val scope = rememberCoroutineScope()
    var updates by remember { mutableStateOf(repository.getBookUpdates()) }
    var isRefreshing by remember { mutableStateOf(false) }

    fun refreshUpdates() {
        scope.launch {
            isRefreshing = true
            try {
                repository.checkLibraryUpdates(System.currentTimeMillis())
                updates = repository.getBookUpdates()
            } finally {
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        updates = repository.getBookUpdates()
    }

    val rotation = remember { Animatable(0f) }
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            rotation.animateTo(
                targetValue = 360f * 10,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        } else {
            rotation.snapTo(0f)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Bảng tin",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = InkDark
                        )
                        if (updates.any { !it.isSeen }) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${updates.count { !it.isSeen }}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            repository.markUpdatesSeen()
                            updates = repository.getBookUpdates()
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Đã xem hết", tint = InkDark)
                    }
                    IconButton(
                        onClick = { if (!isRefreshing) refreshUpdates() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Quét chương mới",
                            tint = AccentOrange,
                            modifier = Modifier.rotate(rotation.value)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgCream)
            )
        },
        containerColor = BgCream
    ) { innerPadding ->
        if (updates.isEmpty() && !isRefreshing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        tint = MutedGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Chưa có thông báo chương mới",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = InkDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Thêm truyện vào Tủ Sách và nhấn nút Quét để tự động kiểm tra chương mới.",
                        fontSize = 13.sp,
                        color = MutedGray,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { refreshUpdates() },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Quét Tủ Sách ngay")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // 1. Horizontal Carousel: Cập nhật mới nhất
                item {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = "Cập nhật gần đây",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = InkDark,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(updates.take(10), key = { it.content.url }) { item ->
                                KotatsuUpdateCard(item = item, onClick = { onContentClick(item.content) })
                            }
                        }
                    }
                }

                // 2. Grouped Timeline List
                item {
                    Text(
                        text = "Danh sách chương mới",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = InkDark,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }

                items(updates, key = { "row_${it.content.url}" }) { item ->
                    KotatsuUpdateRow(
                        item = item,
                        onClick = { onContentClick(item.content) }
                    )
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
fun KotatsuUpdateCard(
    item: BookUpdateItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(105.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .width(105.dp)
                .height(145.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFE5DDD0))
        ) {
            if (!item.content.coverUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = item.content.coverUrl,
                    contentDescription = item.content.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = item.content.title.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = InkDark
                    )
                }
            }

            // Red Badge on Top Right: New chapter count
            if (item.newChaptersCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "+${item.newChaptersCount}",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Blue Circle Badge on Bottom: Read percentage %
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(5.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3B82F6))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${item.readPercentage}%",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.content.title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = InkDark,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun KotatsuUpdateRow(
    item: BookUpdateItem,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mini Cover
            Box(
                modifier = Modifier
                    .size(width = 48.dp, height = 64.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFE5DDD0))
            ) {
                if (!item.content.coverUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = item.content.coverUrl,
                        contentDescription = item.content.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.content.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = InkDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (item.newChaptersCount > 0) "${item.newChaptersCount} Chương mới" else "Chương mới nhất",
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (!item.latestChapterTitle.isNullOrEmpty()) {
                        Text(
                            text = " • ${item.latestChapterTitle}",
                            color = MutedGray,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Source tag
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(AccentOrange.copy(alpha = 0.12f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = item.content.sourceName,
                    fontSize = 10.sp,
                    color = AccentOrange,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}