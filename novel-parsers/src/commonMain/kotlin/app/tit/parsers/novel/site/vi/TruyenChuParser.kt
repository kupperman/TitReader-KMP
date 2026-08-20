package app.tit.parsers.novel.site.vi

import app.tit.content.core.LoaderContext
import app.tit.content.core.NovelParser
import app.tit.content.core.annotation.NovelSourceParser
import app.tit.content.core.model.*

@NovelSourceParser("TRUYENCHU", "Truyện Chữ", "vi")
class TruyenChuParser(
    private val context: LoaderContext
) : NovelParser {

    override val id: String = "TRUYENCHU"
    override val name: String = "Truyện Chữ"
    override val domain: String get() = mirrors.first()

    private val mirrors: List<String> = listOf(
        "https://truyenchu.net",
        "https://truyenchu.vn"
    )

    override suspend fun getList(page: Int, filter: ContentFilter.NovelFilter): List<Content> {
        val path = when (filter.order) {
            SortOrder.HOT -> if (page == 1) "/tong-hop/?m_orderby=views" else "/tong-hop/page/$page/?m_orderby=views"
            SortOrder.COMPLETED -> if (page == 1) "/tong-hop/?m_orderby=alphabet" else "/tong-hop/page/$page/?m_orderby=alphabet"
            SortOrder.LATEST -> if (page == 1) "/tong-hop/" else "/tong-hop/page/$page/"
        }

        val (doc, base) = context.parseHtmlRace(mirrors, path)
        val list = mutableListOf<Content>()

        doc.select(".page-listing-item .page-item-detail, .c-tabs-item__content, .item-thumb").forEach { el ->
            val link = el.selectFirst(".post-title a, .item-thumb a, h3 a")
            if (link != null) {
                val title = link.attr("title").ifEmpty { link.text() }.trim()
                val rawHref = link.attr("href").trim()
                val novelUrl = if (rawHref.startsWith("http")) rawHref else "$base$rawHref"

                val cover = el.selectFirst("img")?.let { img ->
                    img.attr("data-src")
                        .ifEmpty { img.attr("data-original") }
                        .ifEmpty { img.attr("src") }
                }?.let { raw ->
                    when {
                        raw.startsWith("//") -> "https:$raw"
                        raw.startsWith("http") -> raw
                        raw.isNotEmpty() -> "$base$raw"
                        else -> null
                    }
                }

                val author = el.selectFirst(".author, .mg_author")?.text()?.trim()
                val latestChap = el.selectFirst(".chapter a, .chapter-item a")?.text()?.trim()

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
        val path = if (page == 1) "/?s=$query&post_type=wp-manga" else "/page/$page/?s=$query&post_type=wp-manga"
        val (doc, base) = context.parseHtmlRace(mirrors, path)
        val list = mutableListOf<Content>()

        doc.select(".c-tabs-item__content, .page-listing-item, .row.c-tabs-item__content").forEach { el ->
            val link = el.selectFirst(".post-title a, h3 a, .item-thumb a")
            if (link != null) {
                val title = link.attr("title").ifEmpty { link.text() }.trim()
                val rawHref = link.attr("href").trim()
                val novelUrl = if (rawHref.startsWith("http")) rawHref else "$base$rawHref"

                val cover = el.selectFirst("img")?.let { img ->
                    img.attr("data-src")
                        .ifEmpty { img.attr("data-original") }
                        .ifEmpty { img.attr("src") }
                }?.let { raw ->
                    when {
                        raw.startsWith("//") -> "https:$raw"
                        raw.startsWith("http") -> raw
                        raw.isNotEmpty() -> "$base$raw"
                        else -> null
                    }
                }

                val author = el.selectFirst(".author, .mg_author, .post-content_item .summary-content")?.text()?.trim()
                val latestChap = el.selectFirst(".chapter a, .chapter-item a")?.text()?.trim()

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
        val title = doc.selectFirst(".post-title h1, h1.entry-title, h1")?.text()?.trim() ?: "Không có tiêu đề"

        val cover = doc.selectFirst(".summary_image img, .tab-summary img")?.let { img ->
            img.attr("data-src").ifEmpty { img.attr("src") }
        }

        val author = doc.selectFirst(".author-content a, .author a")?.text()?.trim()
        val desc = doc.selectFirst(".description-summary, .summary__content")?.text()?.trim()
        val status = if (doc.selectFirst(".post-status")?.text()?.contains("Hoàn", ignoreCase = true) == true) {
            ContentStatus.COMPLETED
        } else {
            ContentStatus.ONGOING
        }

        val chapters = mutableListOf<Chapter>()
        var order = 1
        doc.select("ul.sub-chap a, li.wp-manga-chapter a, .listing-chapters_wrap a, a[href*='chuong-']").forEach { a ->
            val chTitle = a.text().trim()
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
        val title = doc.selectFirst(".breadcrumb li.active, .chapter-heading, h1")?.text()?.trim() ?: "Nội dung chương"

        val contentEl = doc.selectFirst(".reading-content, .text-left, .entry-content")
            ?: error("Không tìm thấy nội dung chương")

        // Dọn dẹp quảng cáo
        contentEl.select("script, style, .ads, .ad, div[class*='ads']").remove()

        val text = contentEl.wholeText().trim()

        val prevHref = doc.selectFirst("a.prev_page, a.btn-prev")?.attr("href")?.takeIf { it.isNotEmpty() }
        val nextHref = doc.selectFirst("a.next_page, a.btn-next")?.attr("href")?.takeIf { it.isNotEmpty() }

        return ChapterContent.Text(
            title = title,
            chapterUrl = chapterUrl,
            text = text,
            prevChapterUrl = prevHref,
            nextChapterUrl = nextHref,
            sourceId = id
        )
    }
}