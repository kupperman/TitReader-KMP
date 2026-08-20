package app.tit.parsers.novel

import app.tit.content.core.LoaderContext
import app.tit.content.core.NovelParser
import app.tit.content.core.SourceCatalog
import app.tit.content.core.model.ContentType
import app.tit.content.core.model.SourceInfo
import app.tit.parsers.novel.site.vi.DTruyenParser
import app.tit.parsers.novel.site.vi.TangThuVienParser
import app.tit.parsers.novel.site.vi.TruyenFullParser

object NovelSourceRegistry : SourceCatalog<NovelParser> {
    override val type: ContentType = ContentType.NOVEL

    private val sources: Map<String, (LoaderContext) -> NovelParser> = mapOf(
        "TRUYENFULL" to { ctx -> TruyenFullParser(ctx) },
        "DTRUYEN" to { ctx -> DTruyenParser(ctx) },
        "TANGTHUVIEN" to { ctx -> TangThuVienParser(ctx) }
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
            id = "DTRUYEN",
            name = "DTruyen",
            lang = "vi",
            version = 1,
            type = ContentType.NOVEL,
            domain = "https://dtruyen.com.vn"
        ),
        SourceInfo(
            id = "TANGTHUVIEN",
            name = "TangThưViện",
            lang = "vi",
            version = 1,
            type = ContentType.NOVEL,
            domain = "https://truyen.tangthuvien.vn"
        )
    )

    override fun allSources(): List<SourceInfo> = sourceInfoList

    override fun createParser(id: String, ctx: LoaderContext): NovelParser {
        val factory = sources[id] ?: sources.values.first()
        return factory(ctx)
    }
}
