package app.tit.reader.novel.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
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
import app.tit.reader.novel.android.ui.components.ContentCard
import app.tit.reader.novel.android.ui.theme.*
import app.tit.shared.model.BookCategory
import app.tit.shared.model.LibraryBook
import app.tit.shared.repository.AggregatorRepository

@Composable
fun LibraryScreen(
    repository: AggregatorRepository,
    onContentClick: (Content) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(BookCategory.ALL) }
    var books by remember { mutableStateOf<List<LibraryBook>>(emptyList()) }

    fun refreshBooks() {
        books = repository.getLibraryBooks(selectedCategory.id)
    }

    LaunchedEffect(selectedCategory) {
        refreshBooks()
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgCream)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = AccentOrange,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tủ Sách Của Bạn",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${books.size} truyện",
                        fontSize = 13.sp,
                        color = MutedGray
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Category Chips
                ScrollableTabRow(
                    selectedTabIndex = BookCategory.values().indexOf(selectedCategory),
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    divider = {},
                    indicator = {}
                ) {
                    BookCategory.values().forEach { category ->
                        val isSelected = selectedCategory == category
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) AccentOrange else Color(0xFFE8E0D5))
                                .clickable { selectedCategory = category }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = category.displayName,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextPrimary
                            )
                        }
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
                .padding(horizontal = 12.dp)
        ) {
            if (books.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = null,
                            tint = MutedGray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Tủ sách hiện đang trống",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Khi xem truyện, nhấn vào biểu tượng Trái tim để lưu truyện vào tủ sách",
                            fontSize = 13.sp,
                            color = MutedGray,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(books, key = { it.content.url }) { book ->
                        ContentCard(
                            content = book.content,
                            onClick = { onContentClick(book.content) }
                        )
                    }
                }
            }
        }
    }
}