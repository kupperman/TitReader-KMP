package app.tit.reader.novel.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tit.content.core.model.Content
import app.tit.content.core.model.ContentFilter
import app.tit.content.core.model.ContentType
import app.tit.content.core.model.SortOrder
import app.tit.reader.novel.android.ui.components.ContentCard
import app.tit.reader.novel.android.ui.theme.*
import app.tit.shared.repository.AggregatorRepository
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onContentClick: (Content) -> Unit,
    onSearchClick: (ContentType) -> Unit
) {
    val repository = remember { AggregatorRepository() }
    val scope = rememberCoroutineScope()

    var activeTab by remember { mutableStateOf(ContentType.NOVEL) }

    val novelSources = remember { repository.sourceManager.getSources(ContentType.NOVEL) }
    val mangaSources = remember { repository.sourceManager.getSources(ContentType.MANGA) }

    var selectedNovelSourceId by remember { mutableStateOf(novelSources.firstOrNull()?.id ?: "TRUYENFULL") }
    var selectedMangaSourceId by remember { mutableStateOf(mangaSources.firstOrNull()?.id ?: "OTRUYEN") }

    var selectedOrder by remember { mutableStateOf(SortOrder.LATEST) }
    var page by remember { mutableIntStateOf(1) }

    var items by remember { mutableStateOf<List<Content>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadData(resetPage: Boolean = false) {
        if (resetPage) page = 1
        isLoading = true
        errorMessage = null

        scope.launch {
            try {
                val fetched = kotlinx.coroutines.withTimeoutOrNull(10_000L) {
                    if (activeTab == ContentType.NOVEL) {
                        repository.getNovelCatalog(
                            sourceId = selectedNovelSourceId,
                            page = page,
                            filter = ContentFilter.NovelFilter(order = selectedOrder)
                        )
                    } else {
                        repository.getMangaCatalog(
                            sourceId = selectedMangaSourceId,
                            page = page,
                            filter = ContentFilter.MangaFilter(order = selectedOrder)
                        )
                    }
                }
                if (fetched != null) {
                    items = fetched
                } else {
                    errorMessage = "Quá thời gian phản hồi (Timeout). Vui lòng thử lại hoặc chọn nguồn khác."
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Lỗi tải dữ liệu"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(activeTab, selectedNovelSourceId, selectedMangaSourceId, selectedOrder, page) {
        loadData()
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgCream)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tít Reader 🐱",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkDark
                    )

                    IconButton(
                        onClick = { onSearchClick(activeTab) },
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(SurfaceWhite)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Tìm kiếm",
                            tint = InkDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Primary 2-tab switcher: Truyện chữ vs Truyện tranh
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE8E0D5))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (activeTab == ContentType.NOVEL) AccentOrange else Color.Transparent)
                            .clickable {
                                activeTab = ContentType.NOVEL
                                page = 1
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📖 Truyện Chữ",
                            color = if (activeTab == ContentType.NOVEL) Color.White else InkDark,
                            fontWeight = if (activeTab == ContentType.NOVEL) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (activeTab == ContentType.MANGA) Color(0xFF3B82F6) else Color.Transparent)
                            .clickable {
                                activeTab = ContentType.MANGA
                                page = 1
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🎨 Truyện Tranh",
                            color = if (activeTab == ContentType.MANGA) Color.White else InkDark,
                            fontWeight = if (activeTab == ContentType.MANGA) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        },
        containerColor = BgCream
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Source selector chips (separated per type)
            val currentSources = if (activeTab == ContentType.NOVEL) novelSources else mangaSources
            val currentSelectedId = if (activeTab == ContentType.NOVEL) selectedNovelSourceId else selectedMangaSourceId

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 6.dp)
            ) {
                items(currentSources) { src ->
                    val isSelected = src.id == currentSelectedId
                    val activeColor = if (activeTab == ContentType.NOVEL) AccentOrange else Color(0xFF3B82F6)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) activeColor else SurfaceWhite)
                            .clickable {
                                if (activeTab == ContentType.NOVEL) {
                                    selectedNovelSourceId = src.id
                                } else {
                                    selectedMangaSourceId = src.id
                                }
                                page = 1
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = src.name,
                            color = if (isSelected) Color.White else InkDark,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // Filter order tabs
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 6.dp)
            ) {
                listOf(
                    SortOrder.LATEST to "Mới cập nhật",
                    SortOrder.HOT to "🔥 Hot / Top",
                    SortOrder.COMPLETED to "✓ Hoàn thành"
                ).forEach { (order, label) ->
                    val isSelected = selectedOrder == order
                    val activeColor = if (activeTab == ContentType.NOVEL) AccentOrange else Color(0xFF3B82F6)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFFF3E3CE) else SurfaceWhite)
                            .clickable {
                                selectedOrder = order
                                page = 1
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) activeColor else MutedGray,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = if (activeTab == ContentType.NOVEL) AccentOrange else Color(0xFF3B82F6))
                }
            } else if (errorMessage != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Text(text = "⚠ $errorMessage", color = Color(0xFFB91C1C), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { loadData() },
                            colors = ButtonDefaults.buttonColors(containerColor = if (activeTab == ContentType.NOVEL) AccentOrange else Color(0xFF3B82F6))
                        ) {
                            Text("Thử lại")
                        }
                    }
                }
            } else if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Text(text = "📭 Chưa có dữ liệu từ nguồn này", color = MutedGray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Bạn có thể thử chọn nguồn khác hoặc đổi thứ tự sắp xếp", color = MutedGray, fontSize = 12.sp)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items) { content ->
                        ContentCard(
                            content = content,
                            onClick = { onContentClick(content) }
                        )
                    }
                }
            }
        }
    }
}
