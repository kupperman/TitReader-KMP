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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tit.content.core.model.Content
import app.tit.content.core.model.ContentType
import app.tit.content.core.model.SourceInfo
import app.tit.reader.novel.android.ui.components.ContentCard
import app.tit.reader.novel.android.ui.theme.*
import app.tit.shared.model.SourceSearchResult
import app.tit.shared.model.SourceStatus
import app.tit.shared.repository.AggregatorRepository
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    initialType: ContentType,
    onContentClick: (Content) -> Unit,
    onBackClick: () -> Unit
) {
    val repository = remember { AggregatorRepository() }
    val scope = rememberCoroutineScope()

    var activeType by remember { mutableStateOf(initialType) }
    var searchQuery by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Content>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }
    var selectedSourceId by remember { mutableStateOf<String?>(null) } // null = All sources

    val sources = remember(activeType) { repository.sourceManager.getSources(activeType) }
    val sourceStatusMap = remember { mutableStateMapOf<String, SourceStatus>() }

    fun doSearch() {
        val q = searchQuery.trim()
        if (q.isEmpty()) return
        isSearching = true
        hasSearched = true
        results = emptyList()
        selectedSourceId = null
        sourceStatusMap.clear()
        sources.forEach { sourceStatusMap[it.id] = SourceStatus.LOADING }

        scope.launch {
            try {
                val flow = if (activeType == ContentType.NOVEL) {
                    repository.searchNovelsStreaming(q)
                } else {
                    repository.searchMangaStreaming(q)
                }

                flow.collect { event ->
                    when (event) {
                        is SourceSearchResult.Success -> {
                            sourceStatusMap[event.source.id] = SourceStatus.DONE
                            results = results + event.items
                        }
                        is SourceSearchResult.TimedOut -> {
                            sourceStatusMap[event.source.id] = SourceStatus.TIMED_OUT
                        }
                        is SourceSearchResult.Failed -> {
                            sourceStatusMap[event.source.id] = SourceStatus.FAILED
                        }
                        is SourceSearchResult.Skipped -> {
                            sourceStatusMap[event.source.id] = SourceStatus.SKIPPED
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore general errors, per-source status is captured
            } finally {
                isSearching = false
            }
        }
    }

    val displayResults = remember(results, selectedSourceId) {
        if (selectedSourceId == null) results else results.filter { it.sourceId == selectedSourceId }
    }

    val allFailed = hasSearched && !isSearching && sourceStatusMap.isNotEmpty() && sourceStatusMap.values.all {
        it == SourceStatus.TIMED_OUT || it == SourceStatus.FAILED || it == SourceStatus.SKIPPED
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgCream)
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = InkDark)
                    }

                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(if (activeType == ContentType.NOVEL) "Tìm truyện chữ đa nguồn..." else "Tìm truyện tranh đa nguồn...")
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { doSearch() }),
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = ""; results = emptyList(); hasSearched = false; sourceStatusMap.clear(); selectedSourceId = null }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Xóa")
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp)),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = SurfaceWhite,
                            unfocusedContainerColor = SurfaceWhite,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    val themeColor = if (activeType == ContentType.NOVEL) AccentOrange else Color(0xFF3B82F6)
                    IconButton(
                        onClick = { doSearch() },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(themeColor)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Tìm kiếm", tint = Color.White)
                    }
                }

                // Live status chips per source (No blocking, individual source status pills with ✓ / ⏱ / ⚠️ / ⏳)
                if (hasSearched) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(sources) { src ->
                            val status = sourceStatusMap[src.id] ?: SourceStatus.IDLE
                            val isSelected = selectedSourceId == src.id
                            SourceChip(
                                source = src,
                                status = status,
                                selected = isSelected,
                                activeType = activeType,
                                onClick = {
                                    selectedSourceId = if (isSelected) null else src.id
                                }
                            )
                        }
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
            if (results.isEmpty() && isSearching) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = if (activeType == ContentType.NOVEL) AccentOrange else Color(0xFF3B82F6))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Đang tìm kiếm song song đa nguồn (Streaming)...",
                            color = MutedGray,
                            fontSize = 13.sp
                        )
                    }
                }
            } else if (allFailed) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("⚠️ Không nguồn nào phản hồi", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = InkDark)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Tất cả các nguồn hiện đang timeout hoặc gặp sự cố mạng.",
                            fontSize = 13.sp,
                            color = MutedGray
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = { doSearch() },
                            colors = ButtonDefaults.buttonColors(containerColor = if (activeType == ContentType.NOVEL) AccentOrange else Color(0xFF3B82F6))
                        ) {
                            Text("Thử lại")
                        }
                    }
                }
            } else if (hasSearched && displayResults.isEmpty() && !isSearching) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (selectedSourceId != null) "Không có kết quả nào từ nguồn này" else "Không tìm thấy kết quả nào cho \"$searchQuery\"",
                        color = MutedGray,
                        fontSize = 14.sp
                    )
                }
            } else {
                if (displayResults.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedSourceId != null) "Kết quả nguồn đã chọn (${displayResults.size}):" else "Tìm thấy ${displayResults.size} kết quả:",
                            fontWeight = FontWeight.Bold,
                            color = InkDark,
                            fontSize = 14.sp
                        )
                        if (isSearching) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = if (activeType == ContentType.NOVEL) AccentOrange else Color(0xFF3B82F6)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Đang tải thêm...", color = MutedGray, fontSize = 11.sp)
                            }
                        }
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(displayResults) { item ->
                        ContentCard(
                            content = item,
                            onClick = { onContentClick(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SourceChip(
    source: SourceInfo,
    status: SourceStatus,
    selected: Boolean,
    activeType: ContentType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColor = if (activeType == ContentType.NOVEL) AccentOrange else Color(0xFF3B82F6)
    val backgroundColor = when {
        selected -> themeColor
        status == SourceStatus.DONE -> Color(0xFFE6F4EA)
        status == SourceStatus.TIMED_OUT -> Color(0xFFFEF3C7)
        status == SourceStatus.FAILED -> Color(0xFFFEE2E2)
        status == SourceStatus.SKIPPED -> Color(0xFFF3F4F6)
        else -> SurfaceWhite
    }

    val contentColor = when {
        selected -> Color.White
        status == SourceStatus.DONE -> Color(0xFF137333)
        status == SourceStatus.TIMED_OUT -> Color(0xFF92400E)
        status == SourceStatus.FAILED -> Color(0xFFB91C1C)
        status == SourceStatus.SKIPPED -> Color(0xFF6B7280)
        else -> InkDark
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick),
        color = backgroundColor,
        shape = RoundedCornerShape(50)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StatusIndicator(status = status, tint = contentColor)
            Text(
                text = source.name,
                color = contentColor,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun StatusIndicator(status: SourceStatus, tint: Color) {
    when (status) {
        SourceStatus.IDLE -> Unit
        SourceStatus.LOADING -> {
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                strokeWidth = 1.5.dp,
                color = tint
            )
        }
        SourceStatus.DONE -> Text("✓", color = tint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        SourceStatus.TIMED_OUT -> Text("⏱", color = Color(0xFFE57373), fontSize = 12.sp)
        SourceStatus.FAILED -> Text("⚠️", color = Color(0xFFE57373), fontSize = 12.sp)
        SourceStatus.SKIPPED -> Text("💤", color = Color(0xFF9CA3AF), fontSize = 12.sp)
    }
}
