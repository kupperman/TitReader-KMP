package app.tit.shared.manager

import app.tit.content.core.LoaderContext
import app.tit.content.core.MangaParser
import app.tit.content.core.NovelParser
import app.tit.content.core.SourceCatalog
import app.tit.content.core.model.ContentType
import app.tit.content.core.model.SourceInfo
import app.tit.parsers.manga.MangaSourceRegistry
import app.tit.parsers.novel.NovelSourceRegistry

class SourceManager(
    val novelCatalog: SourceCatalog<NovelParser> = NovelSourceRegistry,
    val mangaCatalog: SourceCatalog<MangaParser> = MangaSourceRegistry,
    val context: LoaderContext = LoaderContext()
) {
    fun getSources(type: ContentType): List<SourceInfo> = when (type) {
        ContentType.NOVEL -> novelCatalog.allSources()
        ContentType.MANGA -> mangaCatalog.allSources()
    }

    fun getNovelParser(id: String): NovelParser = novelCatalog.createParser(id, context)

    fun getMangaParser(id: String): MangaParser = mangaCatalog.createParser(id, context)
}
