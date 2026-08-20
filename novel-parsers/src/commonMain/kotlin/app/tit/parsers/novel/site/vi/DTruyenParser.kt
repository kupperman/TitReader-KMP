package app.tit.parsers.novel.site.vi

import app.tit.content.core.LoaderContext
import app.tit.content.core.NovelParser
import app.tit.content.core.annotation.NovelSourceParser
import app.tit.content.core.model.*
import com.fleeksoft.ksoup.Ksoup

@NovelSourceParser("DTRUYEN", "DTruyen", "vi")
class DTruyenParser(
    private val context: LoaderContext
) : NovelParser {

    override val id: String = "DTRUYEN"
    override val name: String = "DTruyen"
    override val domain: String get() = mirrors.first()

    private val mirrors = listOf(
        "https://truyencom.com",
        "https://dtruyen.net",
        "https://dtruyen.com.vn",
        "https://dtruyen.com"
    )

    override suspend fun getList(page: Int, filter: ContentFilter.NovelFilter): List<Content> {
        val path = when (filter.order) {
            SortOrder.HOT -> if (page == 1) "/truyen-hot/" else "/truyen-hot/trang-$page/"
            SortOrder.COMPLETED -> if (page == 1) "/truyen-full/" else "/truyen-full/trang-$page/"
            SortOrder.LATEST -> if (page == 1) "/truyen-moi-cap-nhat/" else "/truyen-moi-cap-nhat/trang-$page/"
        }

        val (doc, base) = context.parseHtmlRace(mirrors, path)
        val list = mutableListOf<Content>()

        doc.select(".list-stories .story-grid, .list-stories li, .list-story .story-item").forEach { el ->
            val link = el.selectFirst("h3 a, .title a, a.story-title")
            if (link != null) {
                val title = link.attr("title").ifEmpty { link.text() }.trim()
                val rawHref = link.attr("href").trim()
                val novelUrl = if (rawHref.startsWith("http")) rawHref else "$base$rawHref"

                val cover = el.selectFirst("img")?.attr("data-src")
                    ?: el.selectFirst("img")?.attr("src")

                val author = el.selectFirst(".author, .story-author")?.text()?.trim()
                val latestChap = el.selectFirst(".chapter-title, .chapter")?.text()?.trim()

                if (title.isNotEmpty() && novelUrl.isNotEmpty() && !novelUrl.contains("/the-loai/")) {
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
        val path = if (page == 1) "/tim-kiem/?q=$query" else "/tim-kiem/?q=$query&page=$page"
        val (doc, base) = context.parseHtmlRace(mirrors, path)
        val list = mutableListOf<Content>()

        doc.select(".list-stories .story-grid, .list-stories li, .list-story .story-item").forEach { el ->
            val link = el.selectFirst("h3 a, .title a, a.story-title")
            if (link != null) {
                val title = link.attr("title").ifEmpty { link.text() }.trim()
                val rawHref = link.attr("href").trim()
                val novelUrl = if (rawHref.startsWith("http")) rawHref else "$base$rawHref"

                val cover = el.selectFirst("img")?.attr("data-src")
                    ?: el.selectFirst("img")?.attr("src")

                val author = el.selectFirst(".author, .story-author")?.text()?.trim()
                val latestChap = el.selectFirst(".chapter-title, .chapter")?.text()?.trim()

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
        val title = doc.selectFirst("h1.title, h1.story-title, h1")?.text()?.trim() ?: "Không có tiêu đề"
        
        val cover = doc.selectFirst(".thumb img, .story-info img, .cover img")?.attr("data-src")
            ?: doc.selectFirst(".thumb img, .story-info img, .cover img")?.attr("src")

        val author = doc.selectFirst(".author span, .story-info .author a")?.text()?.trim()
        val desc = doc.selectFirst(".description, .story-desc, .desc-text")?.text()?.trim()
        val status = if (doc.selectFirst(".status, .badge")?.text()?.contains("Hoàn", ignoreCase = true) == true) {
            ContentStatus.COMPLETED
        } else {
            ContentStatus.ONGOING
        }

        val chapters = mutableListOf<Chapter>()
        var order = 1
        doc.select("#chapters-list a, .list-chapters a, a[href*='/chuong-']").forEach { a ->
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
        val title = doc.selectFirst("h2.chapter-title, .chapter-title, h1")?.text()?.trim() ?: "Nội dung chương"

        val contentEl = doc.selectFirst("#chapter-content, .chapter-content, .story-content")
        contentEl?.select("script, ins, .ads, .ads-holder, a[href*='dtruyen']")?.remove()

        val paragraphs = contentEl?.html()
            ?.split(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE))
            ?.map { Ksoup.parse(it).text().trim() }
            ?.filter { it.isNotEmpty() && !it.contains("dtruyen", ignoreCase = true) }
            ?: emptyList()

        val prevHref = doc.selectFirst("#prev-chapter, a.prev-chapter")?.attr("href")?.takeIf { it.isNotEmpty() }
        val nextHref = doc.selectFirst("#next-chapter, a.next-chapter")?.attr("href")?.takeIf { it.isNotEmpty() }

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
