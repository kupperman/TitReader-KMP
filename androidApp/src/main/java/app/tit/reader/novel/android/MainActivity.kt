package app.tit.reader.novel.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tit.content.core.model.Chapter
import app.tit.content.core.model.Content
import app.tit.content.core.model.ContentType
import app.tit.reader.novel.android.data.AndroidSharedPreferencesDriver
import app.tit.reader.novel.android.ui.screens.*
import app.tit.reader.novel.android.ui.theme.AccentOrange
import app.tit.reader.novel.android.ui.theme.BgCream
import app.tit.reader.novel.android.ui.theme.MutedGray
import app.tit.reader.novel.android.ui.theme.TitReaderTheme
import app.tit.shared.repository.AggregatorRepository
import app.tit.shared.storage.LibraryStorage
import coil.Coil
import coil.ImageLoader
import okhttp3.OkHttpClient

sealed class Screen {
    object Home : Screen()
    object Library : Screen()
    object History : Screen()
    data class Search(val type: ContentType) : Screen()
    data class Details(val content: Content) : Screen()
    data class Reader(val chapter: Chapter, val type: ContentType, val contentMeta: Content? = null) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val host = original.url.host.lowercase()
                val referer = when {
                    host.contains("nettruyen") || host.contains("kptackpte") || host.contains("ccnnts") -> "https://nettruyenx.net/"
                    host.contains("truyenqq") || host.contains("hinhhinh") || host.contains("truyenvua") -> "https://truyenqqko.com/"
                    host.contains("foxtruyen") || host.contains("hinhgg") -> "https://foxtruyen2.com/"
                    host.contains("truyendich") -> "https://truyendich.vn/"
                    host.contains("truyenhoan") -> "https://truyenhoan.com/"
                    host.contains("truyenfull") -> "https://truyenfull.live/"
                    else -> "${original.url.scheme}://${original.url.host}/"
                }

                val request = original.newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                    .header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                    .header("Referer", referer)
                    .build()
                chain.proceed(request)
            }
            .build()

        val imageLoader = ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .crossfade(true)
            .build()
        Coil.setImageLoader(imageLoader)

        val storage = LibraryStorage(AndroidSharedPreferencesDriver(applicationContext))
        val repository = AggregatorRepository(storage = storage)

        setContent {
            TitReaderTheme {
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
                val backStack = remember { mutableStateListOf<Screen>() }

                fun navigateTo(screen: Screen) {
                    backStack.add(currentScreen)
                    currentScreen = screen
                }

                fun navigateBack() {
                    if (backStack.isNotEmpty()) {
                        currentScreen = backStack.removeAt(backStack.lastIndex)
                    } else {
                        currentScreen = Screen.Home
                    }
                }

                val isTopLevel = currentScreen is Screen.Home || currentScreen is Screen.Library || currentScreen is Screen.History

                Scaffold(
                    bottomBar = {
                        if (isTopLevel) {
                            NavigationBar(
                                containerColor = BgCream,
                                tonalElevation = 8.dp
                            ) {
                                val navItems = listOf(
                                    Triple(Screen.Home, "Khám Phá", Icons.Default.Explore),
                                    Triple(Screen.Library, "Tủ Sách", Icons.Default.Bookmarks),
                                    Triple(Screen.History, "Lịch Sử", Icons.Default.History)
                                )

                                navItems.forEach { (screenDest, label, icon) ->
                                    val isSelected = currentScreen.javaClass == screenDest.javaClass
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = {
                                            if (currentScreen.javaClass != screenDest.javaClass) {
                                                backStack.clear()
                                                currentScreen = screenDest
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = label,
                                                tint = if (isSelected) AccentOrange else MutedGray
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = label,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) AccentOrange else MutedGray
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            indicatorColor = Color(0xFFFFF0EB)
                                        )
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        when (val screen = currentScreen) {
                            is Screen.Home -> {
                                HomeScreen(
                                    onContentClick = { content -> navigateTo(Screen.Details(content)) },
                                    onSearchClick = { type -> navigateTo(Screen.Search(type)) }
                                )
                            }
                            is Screen.Library -> {
                                LibraryScreen(
                                    repository = repository,
                                    onContentClick = { content -> navigateTo(Screen.Details(content)) }
                                )
                            }
                            is Screen.History -> {
                                HistoryScreen(
                                    repository = repository,
                                    onContentClick = { content -> navigateTo(Screen.Details(content)) },
                                    onContinueReadClick = { content, chapter ->
                                        navigateTo(Screen.Reader(chapter, content.type, content))
                                    }
                                )
                            }
                            is Screen.Search -> {
                                SearchScreen(
                                    initialType = screen.type,
                                    onContentClick = { content -> navigateTo(Screen.Details(content)) },
                                    onBackClick = { navigateBack() }
                                )
                            }
                            is Screen.Details -> {
                                DetailsScreen(
                                    content = screen.content,
                                    repository = repository,
                                    onChapterClick = { chapter, type ->
                                        navigateTo(Screen.Reader(chapter, type, screen.content))
                                    },
                                    onBackClick = { navigateBack() }
                                )
                            }
                            is Screen.Reader -> {
                                ReaderScreen(
                                    chapter = screen.chapter,
                                    type = screen.type,
                                    contentMeta = screen.contentMeta,
                                    repository = repository,
                                    onBackClick = { navigateBack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}