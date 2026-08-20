package app.tit.parsers.manga

import app.tit.content.core.LoaderContext
import app.tit.content.core.MangaParser
import app.tit.content.core.SourceCatalog
import app.tit.content.core.model.ContentType
import app.tit.content.core.model.SourceInfo
import app.tit.parsers.manga.site.vi.NetTruyenParser
import app.tit.parsers.manga.site.vi.TruyenQQParser

object MangaSourceRegistry : SourceCatalog<MangaParser> {
    override val type: ContentType = ContentType.MANGA

    private val sources: Map<String, (LoaderContext) -> MangaParser> = mapOf(
        "TRUYENQQ" to { ctx -> TruyenQQParser(ctx) },
        "NETTRUYEN" to { ctx -> NetTruyenParser(ctx) }
    )

    private val sourceInfoList: List<SourceInfo> = listOf(
        SourceInfo(
            id = "TRUYENQQ",
            name = "TruyệnQQ",
            lang = "vi",
            version = 2,
            type = ContentType.MANGA,
            domain = "https://truyenqqko.com"
        ),
        SourceInfo(
            id = "NETTRUYEN",
            name = "NetTruyen",
            lang = "vi",
            version = 1,
            type = ContentType.MANGA,
            domain = "https://nettruyena.com"
        )
    )

    override fun allSources(): List<SourceInfo> = sourceInfoList

    override fun createParser(id: String, ctx: LoaderContext): MangaParser {
        val factory = sources[id] ?: sources.values.first()
        return factory(ctx)
    }
}
