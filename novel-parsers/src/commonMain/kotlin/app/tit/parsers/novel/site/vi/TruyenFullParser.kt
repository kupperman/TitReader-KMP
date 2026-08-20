package app.tit.parsers.novel.site.vi

import app.tit.content.core.LoaderContext
import app.tit.content.core.NovelParser
import app.tit.content.core.annotation.NovelSourceParser
import app.tit.content.core.model.*
import com.fleeksoft.ksoup.Ksoup

@NovelSourceParser("TRUYENFULL", "Truyện Full", "vi")
class TruyenFullParser(
    private val context: LoaderContext
) : NovelParser {

    override val id: String = "TRUYENFULL"
    override val name: String = "Truyện Full"
    override val domain: String get() = mirrors.first()

    private val mirrors = listOf(
        "https://truyenfull.live",
        "https://truyenfull.vn",
        "https://truyenfull.vision",
        "https://truyenfull.io"
    )

    override suspend fun getList(page: Int, filter: ContentFilter.NovelFilter): List<Content> {
        val path = when (filter.order) {
            SortOrder.HOT -> if (page == 1) "/danh-sach/truyen-hot/" else "/danh-sach/truyen-hot/trang-$page/"
            SortOrder.COMPLETED -> if (page == 1) "/danh-sach/truyen-full/" else "/danh-sach/truyen-full/trang-$page/"
            SortOrder.LATEST -> if (page == 1) "/danh-sach/truyen-moi/" else "/danh-sach/truyen-moi/trang-$page/"
        }

        val (doc, base) = context.parseHtmlRace(mirrors, path)
        val list = mutableListOf<Content>()

        doc.select(".list-truyen .row, .truyen-list .row").forEach { el ->
            val titleEl = el.selectFirst("h3 a, .truyen-title a")
            if (titleEl != null) {
                val title = titleEl.text().trim()
                val rawHref = titleEl.attr("href").trim()
                val novelUrl = if (rawHref.startsWith("http")) rawHref else "$base$rawHref"

                val cover = el.selectFirst("[data-image]")?.attr("data-image")
                    ?: el.selectFirst("[data-desk-image]")?.attr("data-desk-image")
                    ?: el.selectFirst(".lazy-image")?.attr("data-image")
                    ?: el.selectFirst("img")?.attr("data-src")
                    ?: el.selectFirst("img")?.attr("src")

                val author = el.selectFirst(".author")?.text()?.trim()
                val latestChap = el.selectFirst(".text-info")?.text()?.trim()

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
        val path = if (page == 1) "/tim-kiem/?tukhoa=$query" else "/tim-kiem/?tukhoa=$query&page=$page"
        val (doc, base) = context.parseHtmlRace(mirrors, path)
        val list = mutableListOf<Content>()

        doc.select(".list-truyen .row, .truyen-list .row").forEach { el ->
            val titleEl = el.selectFirst("h3 a, .truyen-title a")
            if (titleEl != null) {
                val title = titleEl.text().trim()
                val rawHref = titleEl.attr("href").trim()
                val novelUrl = if (rawHref.startsWith("http")) rawHref else "$base$rawHref"

                val cover = el.selectFirst("[data-image]")?.attr("data-image")
                    ?: el.selectFirst("[data-desk-image]")?.attr("data-desk-image")
                    ?: el.selectFirst("img")?.attr("data-src")
                    ?: el.selectFirst("img")?.attr("src")

                val author = el.selectFirst(".author")?.text()?.trim()
                val latestChap = el.selectFirst(".text-info")?.text()?.trim()

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

    override suspend fun getDetails(novelUrl: String): ContentDetails {
        val doc = context.parseHtml(novelUrl)
        val title = doc.selectFirst("h1, h1.title, [itemprop='name']")?.text()?.trim() ?: "Không có tiêu đề"
        
        val cover = doc.selectFirst(".book img, .info-holder img")?.attr("src")
            ?: doc.selectFirst(".book [data-image]")?.attr("data-image")
            ?: doc.selectFirst(".book img")?.attr("data-src")

        val author = doc.selectFirst(".info a[itemprop='author']")?.text()?.trim()
        val desc = doc.selectFirst(".desc-text, .desc")?.text()?.trim()
        val statusText = doc.selectFirst(".info .text-success")?.text()?.trim().orEmpty()

        val status = if (statusText.contains("Hoàn", ignoreCase = true)) {
            ContentStatus.COMPLETED
        } else {
            ContentStatus.ONGOING
        }

        val chapters = mutableListOf<Chapter>()
        var order = 1
        doc.select(".list-chapter a, #list-chapter a, a[href*='chuong-']").forEach { a ->
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
        val title = doc.selectFirst(".chapter-title, h2.chapter-title, .title-chapter")?.text()?.trim() ?: "Nội dung chương"

        val contentEl = doc.selectFirst("#chapter-c, .chapter-c, #chapter-content")
        contentEl?.select("script, ins, .ads, .ads-holder, a[href*='truyenfull']")?.remove()

        val paragraphs = contentEl?.html()
            ?.split(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE))
            ?.map { Ksoup.parse(it).text().trim() }
            ?.filter { it.isNotEmpty() && !it.contains("quảng cáo", ignoreCase = true) }
            ?: emptyList()

        val prevHref = doc.selectFirst("#prev_chap, a.btn-prev")?.attr("href")?.takeIf { it.isNotEmpty() }
        val nextHref = doc.selectFirst("#next_chap, a.btn-next")?.attr("href")?.takeIf { it.isNotEmpty() }

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
