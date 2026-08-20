package app.tit.parsers.novel

import app.tit.content.core.LoaderContext
import app.tit.content.core.NovelParser
import app.tit.content.core.SourceCatalog
import app.tit.content.core.model.ContentType
import app.tit.content.core.model.SourceInfo
import app.tit.parsers.novel.site.vi.TruyenDichParser
import app.tit.parsers.novel.site.vi.TruyenFullParser
import app.tit.parsers.novel.site.vi.TruyenHoanParser

object NovelSourceRegistry : SourceCatalog<NovelParser> {
    override val type: ContentType = ContentType.NOVEL

    private val sources: Map<String, (LoaderContext) -> NovelParser> = mapOf(
        "TRUYENFULL" to { ctx -> TruyenFullParser(ctx) },
        "TRUYENDICH" to { ctx -> TruyenDichParser(ctx) },
        "TRUYENHOAN" to { ctx -> TruyenHoanParser(ctx) }
    )

    private val sourceInfoList: List<SourceInfo> = listOf(
        SourceInfo(
            id = "TRUYENFULL",
            name = "Truyện Full",
            lang = "vi",
            version = 1,
            type = ContentType.NOVEL,
            domain = "https://truyenfull.live"
        ),
        SourceInfo(
            id = "TRUYENDICH",
            name = "Truyện Dịch",
            lang = "vi",
            version = 1,
            type = ContentType.NOVEL,
            domain = "https://truyendich.vn"
        ),
        SourceInfo(
            id = "TRUYENHOAN",
            name = "Truyện Hoàn",
            lang = "vi",
            version = 1,
            type = ContentType.NOVEL,
            domain = "https://truyenhoan.com"
        )
    )

    override fun allSources(): List<SourceInfo> = sourceInfoList

    override fun createParser(id: String, ctx: LoaderContext): NovelParser {
        val upperId = id.uppercase()
        val factory = sources[upperId]
            ?: if (id.contains("truyendich", ignoreCase = true)) sources["TRUYENDICH"]
            else if (id.contains("truyenhoan", ignoreCase = true)) sources["TRUYENHOAN"]
            else if (id.contains("truyenfull", ignoreCase = true)) sources["TRUYENFULL"]
            else sources.values.first()
        return factory!!(ctx)
    }
}
