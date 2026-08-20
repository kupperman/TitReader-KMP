package app.tit.parsers.novel.site.vi

import app.tit.content.core.LoaderContext
import app.tit.content.core.NovelParser
import app.tit.content.core.annotation.NovelSourceParser
import app.tit.content.core.model.*
import com.fleeksoft.ksoup.Ksoup

@NovelSourceParser("TANGTHUVIEN", "TangThưViện", "vi")
class TangThuVienParser(
    private val context: LoaderContext
) : NovelParser {

    override val id: String = "TANGTHUVIEN"
    override val name: String = "TangThưViện"
    override val domain: String get() = mirrors.first()

    private val mirrors = listOf(
        "https://truyen.tangthuvien.vn",
        "https://tangthuvien.net",
        "https://truyen.tangthuvien.net",
        "https://truyen.tangthuvien.com"
    )

    override suspend fun getList(page: Int, filter: ContentFilter.NovelFilter): List<Content> {
        val order = when (filter.order) {
            SortOrder.HOT -> "view"
            SortOrder.COMPLETED -> "full"
            SortOrder.LATEST -> "new"
        }
        val path = "/tong-hop?ord=$order&page=$page"

        val (doc, base) = context.parseHtmlRace(mirrors, path)
        val list = mutableListOf<Content>()

        doc.select(".book-img-text li, .list-stories li, .book-list-wrap li").forEach { el ->
            val link = el.selectFirst("h4 a, .book-mid-info h4 a, a.name")
            if (link != null) {
                val title = link.text().trim()
                val rawHref = link.attr("href").trim()
                val novelUrl = if (rawHref.startsWith("http")) rawHref else "$base$rawHref"

                val cover = el.selectFirst("img")?.attr("src")
                    ?: el.selectFirst("img")?.attr("data-original")

                val author = el.selectFirst(".author .name, .author a, p.author")?.text()?.trim()
                val latestChap = el.selectFirst(".update a, p.update a")?.text()?.trim()

                if (title.isNotEmpty() && novelUrl.isNotEmpty()) {
                    list.add(
                        Content(
                            id = novelUrl,
                            title = title,
                            url = novelUrl,
                            coverUrl = cover,
                            author = author,
                            latestChapter = latestChap,
                            type = ContentType.NOVEL,
                            sourceId = id,
                            sourceName = name
                        )
                    )
                }
            }
        }
        return list
    }

    override suspend fun search(query: String, page: Int): List<Content> {
        val path = if (page == 1) "/ket-qua-tim-kiem?term=$query" else "/ket-qua-tim-kiem?term=$query&page=$page"
        val (doc, base) = context.parseHtmlRace(mirrors, path)
        val list = mutableListOf<Content>()

        doc.select(".book-img-text li, .search-list li, .book-list-wrap li").forEach { el ->
            val link = el.selectFirst("h4 a, .book-mid-info h4 a")
            if (link != null) {
                val title = link.text().trim()
                val rawHref = link.attr("href").trim()
                val novelUrl = if (rawHref.startsWith("http")) rawHref else "$base$rawHref"

                val cover = el.selectFirst("img")?.attr("src")
                    ?: el.selectFirst("img")?.attr("data-original")

                val author = el.selectFirst(".author .name, .author a")?.text()?.trim()

                if (title.isNotEmpty() && novelUrl.isNotEmpty()) {
                    list.add(
                        Content(
                            id = novelUrl,
                            title = title,
                            url = novelUrl,
                            coverUrl = cover,
                            author = author,
                            type = ContentType.NOVEL,
                            sourceId = id,
                            sourceName = name
                        )
                    )
                }
            }
        }
        return list
    }

    override suspend fun getDetails(novelUrl: String): ContentDetails {
        val doc = context.parseHtml(novelUrl)
        val title = doc.selectFirst(".book-info h1, h1")?.text()?.trim() ?: "Không có tiêu đề"
        
        val cover = doc.selectFirst(".book-img img, .book-information img")?.attr("src")

        val author = doc.selectFirst(".book-info .tag a, .book-info p:contains(Tác giả) a")?.text()?.trim()
        val desc = doc.selectFirst(".book-intro p, .intro-content")?.text()?.trim()
        val status = if (doc.selectFirst(".book-info .tag span")?.text()?.contains("Hoàn", ignoreCase = true) == true) {
            ContentStatus.COMPLETED
        } else {
            ContentStatus.ONGOING
        }

        val chapters = mutableListOf<Chapter>()
        var order = 1
        doc.select(".chapter-list a, #j-catalogWrap a, .content-nav-wrap a").forEach { a ->
            val chTitle = a.attr("title").ifEmpty { a.text() }.trim()
            val rawHref = a.attr("href").trim()
            val chUrl = if (rawHref.startsWith("http")) rawHref else "$domain$rawHref"

            if (chTitle.isNotEmpty() && chUrl.isNotEmpty() && chapters.none { it.url == chUrl }) {
                chapters.add(
                    Chapter(
                        id = chUrl,
                        title = chTitle,
                        url = chUrl,
                        order = order++,
                        sourceId = id
                    )
                )
            }
        }

        return ContentDetails(
            content = Content(
                id = novelUrl,
                title = title,
                url = novelUrl,
                coverUrl = cover,
                author = author,
                latestChapter = chapters.lastOrNull()?.title,
                type = ContentType.NOVEL,
                sourceId = id,
                sourceName = name
            ),
            description = desc,
            status = status,
            chapters = chapters
        )
    }

    override suspend fun getChapterContent(chapterUrl: String): ChapterContent.Text {
        val doc = context.parseHtml(chapterUrl)
        val title = doc.selectFirst("h2.chapter-title, .heading-title, h1")?.text()?.trim() ?: "Nội dung chương"

        val contentEl = doc.selectFirst(".box-chap, .chapter-c, .content-detail")
        contentEl?.select("script, ins, .ads, .ads-holder, a[href*='tangthuvien']")?.remove()

        val paragraphs = contentEl?.html()
            ?.split(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE))
            ?.map { Ksoup.parse(it).text().trim() }
            ?.filter { it.isNotEmpty() && !it.contains("tangthuvien", ignoreCase = true) }
            ?: emptyList()

        val prevHref = doc.selectFirst("a.btn-prev")?.attr("href")?.takeIf { it.isNotEmpty() }
        val nextHref = doc.selectFirst("a.btn-next")?.attr("href")?.takeIf { it.isNotEmpty() }

        return ChapterContent.Text(
            title = title,
            chapterUrl = chapterUrl,
            text = paragraphs.joinToString("\n\n"),
            paragraphs = paragraphs,
            prevChapterUrl = prevHref,
            nextChapterUrl = nextHref,
            sourceId = id
        )
    }
}
