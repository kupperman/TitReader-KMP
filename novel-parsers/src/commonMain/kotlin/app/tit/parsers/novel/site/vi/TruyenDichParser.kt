package app.tit.parsers.novel.site.vi

import app.tit.content.core.LoaderContext
import app.tit.content.core.NovelParser
import app.tit.content.core.annotation.NovelSourceParser
import app.tit.content.core.model.*

@NovelSourceParser("TRUYENDICH", "Truyện Dịch", "vi")
class TruyenDichParser(
    private val context: LoaderContext
) : NovelParser {

    override val id: String = "TRUYENDICH"
    override val name: String = "Truyện Dịch"
    override val domain: String get() = mirrors.first()

    private val mirrors: List<String> = listOf(
        "https://truyendich.vn"
    )

    override suspend fun getList(page: Int, filter: ContentFilter.NovelFilter): List<Content> {
        val path = when (filter.order) {
            SortOrder.HOT -> if (page == 1) "/danh-sach/truyen-hot/" else "/danh-sach/truyen-hot/trang-$page/"
            SortOrder.COMPLETED -> if (page == 1) "/danh-sach/truyen-full/" else "/danh-sach/truyen-full/trang-$page/"
            SortOrder.LATEST -> if (page == 1) "/danh-sach/truyen-moi-cap-nhat/" else "/danh-sach/truyen-moi-cap-nhat/trang-$page/"
        }

        val (doc, base) = context.parseHtmlRace(mirrors, path)
        val list = mutableListOf<Content>()

        doc.select(".list-truyen .row, .list-story .row, .top-list-item, .item, .story-item").forEach { el ->
            val link = el.selectFirst("h3 a, .title a, a[title]")
            if (link != null) {
                val title = link.attr("title").ifEmpty { link.text() }.trim()
                val rawHref = link.attr("href").trim()
                val novelUrl = if (rawHref.startsWith("http")) rawHref else "$base$rawHref"

                val cover = el.selectFirst("img")?.let { img ->
                    img.attr("data-src").ifEmpty { img.attr("data-original") }.ifEmpty { img.attr("src") }
                }?.let { raw ->
                    when {
                        raw.startsWith("//") -> "https:$raw"
                        raw.startsWith("http") -> raw
                        raw.isNotEmpty() -> "$base$raw"
                        else -> null
                    }
                }

                val author = el.selectFirst(".author, .story-author")?.text()?.trim()
                val latestChap = el.selectFirst(".text-info, .chapter, .last-chapter")?.text()?.trim()

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
        val path = if (page == 1) "/tim-kiem?q=$query" else "/tim-kiem?q=$query&page=$page"
        val (doc, base) = context.parseHtmlRace(mirrors, path)
        val list = mutableListOf<Content>()

        doc.select(".list-truyen .row, .list-story .row, .top-list-item, .item, .story-item").forEach { el ->
            val link = el.selectFirst("h3 a, .title a, a[title]")
            if (link != null) {
                val title = link.attr("title").ifEmpty { link.text() }.trim()
                val rawHref = link.attr("href").trim()
                val novelUrl = if (rawHref.startsWith("http")) rawHref else "$base$rawHref"

                val cover = el.selectFirst("img")?.let { img ->
                    img.attr("data-src").ifEmpty { img.attr("data-original") }.ifEmpty { img.attr("src") }
                }?.let { raw ->
                    when {
                        raw.startsWith("//") -> "https:$raw"
                        raw.startsWith("http") -> raw
                        raw.isNotEmpty() -> "$base$raw"
                        else -> null
                    }
                }

                val author = el.selectFirst(".author, .story-author")?.text()?.trim()
                val latestChap = el.selectFirst(".text-info, .chapter")?.text()?.trim()

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
        val title = doc.selectFirst("h1.title, h1[itemprop='name'], h1")?.text()?.trim() ?: "Không có tiêu đề"

        val cover = doc.selectFirst("img[itemprop='image'], .book img, .books img, img[src*='story-thumb'], .thumb img, .desc-story img")?.let { img ->
            img.attr("data-src").ifEmpty { img.attr("data-original") }.ifEmpty { img.attr("src") }
        }?.let { raw ->
            when {
                raw.startsWith("//") -> "https:$raw"
                raw.startsWith("http") -> raw
                raw.isNotEmpty() -> "$domain$raw"
                else -> null
            }
        }

        val author = doc.selectFirst("a[itemprop='author'], .author a, .info a[href*='tac-gia']")?.text()?.trim()
        val desc = doc.selectFirst(".desc-text, .story-desc, .desc")?.text()?.trim()
        val status = if (doc.selectFirst(".info, .status")?.text()?.contains("Hoàn", ignoreCase = true) == true) {
            ContentStatus.COMPLETED
        } else {
            ContentStatus.ONGOING
        }

        val chapters = mutableListOf<Chapter>()
        var order = 1
        // Chỉ lấy các thẻ a trong block danh sách chương (bỏ qua widget top các chương mới nhất)
        val chapterLinks = doc.select("#list-chapter ul.list-chapter li a, .list-chapter li a")
        val linksToUse = if (chapterLinks.isNotEmpty()) chapterLinks else doc.select("a[href*='chuong-']")

        linksToUse.forEach { a ->
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
        val title = doc.selectFirst("a.chapter-title, .chapter-title, .chapter-c h2, h2, h1")?.attr("title")?.takeIf { it.isNotEmpty() }
            ?: doc.selectFirst("a.chapter-title, .chapter-title, .chapter-c h2, h2, h1")?.text()?.trim()
            ?: "Nội dung chương"

        val contentEl = doc.selectFirst("#chapter-c, .chapter-c, .chapter-content, #content")
            ?: error("Không tìm thấy nội dung chương")

        // Loại bỏ quảng cáo
        contentEl.select("script, style, .ads, .ad, div[class*='ads']").remove()

        val paragraphs = contentEl.select("p").map { it.text().trim() }.filter { it.isNotEmpty() }
        val text = if (paragraphs.isNotEmpty()) {
            paragraphs.joinToString("\n\n")
        } else {
            contentEl.html()
                .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
                .replace(Regex("<p[^>]*>", RegexOption.IGNORE_CASE), "\n\n")
                .replace(Regex("</p>", RegexOption.IGNORE_CASE), "")
                .replace(Regex("<[^>]+>"), "")
                .trim()
        }

        val prevHref = doc.selectFirst("a#prev_chap, a.prev-chap, a.btn-prev")?.attr("href")
            ?.takeIf { it.isNotEmpty() && it != "#" && !it.startsWith("javascript") }
            ?.let { if (it.startsWith("http")) it else "$domain$it" }

        val nextHref = doc.selectFirst("a#next_chap, a.next-chap, a.btn-next")?.attr("href")
            ?.takeIf { it.isNotEmpty() && it != "#" && !it.startsWith("javascript") }
            ?.let { if (it.startsWith("http")) it else "$domain$it" }

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