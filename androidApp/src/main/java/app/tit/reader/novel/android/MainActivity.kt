package app.tit.reader.novel.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import app.tit.content.core.model.Chapter
import app.tit.content.core.model.Content
import app.tit.content.core.model.ContentType
import app.tit.reader.novel.android.ui.screens.DetailsScreen
import app.tit.reader.novel.android.ui.screens.HomeScreen
import app.tit.reader.novel.android.ui.screens.ReaderScreen
import app.tit.reader.novel.android.ui.screens.SearchScreen
import app.tit.reader.novel.android.ui.theme.TitReaderTheme

sealed class Screen {
    object Home : Screen()
    data class Search(val type: ContentType) : Screen()
    data class Details(val content: Content) : Screen()
    data class Reader(val chapter: Chapter, val type: ContentType) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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

                when (val screen = currentScreen) {
                    is Screen.Home -> {
                        HomeScreen(
                            onContentClick = { content -> navigateTo(Screen.Details(content)) },
                            onSearchClick = { type -> navigateTo(Screen.Search(type)) }
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
                            onChapterClick = { chapter, type -> navigateTo(Screen.Reader(chapter, type)) },
                            onBackClick = { navigateBack() }
                        )
                    }
                    is Screen.Reader -> {
                        ReaderScreen(
                            chapter = screen.chapter,
                            type = screen.type,
                            onBackClick = { navigateBack() }
                        )
                    }
                }
            }
        }
    }
}
