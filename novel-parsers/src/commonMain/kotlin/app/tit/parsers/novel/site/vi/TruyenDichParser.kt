package app.tit.parsers.novel.site.vi

import app.tit.content.core.LoaderContext
import app.tit.content.core.NovelParser
import app.tit.content.core.annotation.NovelSourceParser
import app.tit.content.core.model.*
import io.ktor.http.encodeURLParameter

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
        val path = if (page == 1) "/tim-kiem?q=${query.encodeURLParameter()}" else "/tim-kiem?q=${query.encodeURLParameter()}&page=$page"
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
        val (doc, base) = context.parseHtmlWithMirrors(novelUrl, mirrors)
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

        val chapterPageCount = findChapterPageCount(doc)
        val sortedChapters = parsePagedNovelChapters(doc, base, id)
        return ContentDetails(
            content = Content(
                id = novelUrl,
                title = title,
                url = novelUrl,
                coverUrl = cover,
                author = author,
                latestChapter = sortedChapters.lastOrNull()?.title,
                type = ContentType.NOVEL,
                sourceId = id,
                sourceName = name
            ),
            description = desc,
            status = status,
            chapters = sortedChapters,
            chapterPageCount = chapterPageCount
        )
    }

    override suspend fun getChapterPage(novelUrl: String, page: Int): List<Chapter> {
        require(page >= 1) { "Trang chương phải lớn hơn 0" }
        val pageUrl = chapterPageUrl(novelUrl, page)
        val (doc, base) = context.parseHtmlDirectThenMirrors(pageUrl, mirrors)
        return parsePagedNovelChapters(doc, base, id)
    }
    override suspend fun getChapterContent(chapterUrl: String): ChapterContent.Text {
        val (doc, base) = context.parseHtmlDirectThenMirrors(chapterUrl, mirrors)
        val title = selectRealChapterTitle(doc)

        val contentCandidates = listOf("#chapter-c", ".chapter-c", ".chapter-content", "[itemprop='articleBody']", "#content")
            .mapNotNull { selector -> doc.selectFirst(selector) }
        val contentEl = contentCandidates.firstOrNull { it.text().trim().length >= 100 }
            ?: contentCandidates.firstOrNull()
            ?: error("Không tìm thấy nội dung chương")

        contentEl.select("script, style, .ads, .ad, .ads-holder, div[id*='ads'], div[class*='ads']").remove()

        val selectedParagraphs = contentEl.select("p")
            .map { it.text().trim() }
            .filter { it.isNotEmpty() && !it.contains("quảng cáo", ignoreCase = true) }
        val selectedText = selectedParagraphs.joinToString("\n\n")
        val fallbackText = contentEl.html()
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<p[^>]*>", RegexOption.IGNORE_CASE), "\n\n")
            .replace(Regex("</p>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<[^>]+>"), "")
            .trim()
        val text = selectedText.takeIf { it.length >= 200 } ?: fallbackText
        val paragraphs = if (selectedText.length >= 200) selectedParagraphs else text
            .split(Regex("\\n\\s*\\n|\\n"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val prevRaw = (doc.getElementById("prev_chap")
            ?: doc.getElementsByClass("prev-chap").firstOrNull()
            ?: doc.getElementsByClass("btn-prev").firstOrNull())
            ?.attr("href")
            ?.takeIf { it.isNotEmpty() && it != "#" && !it.startsWith("javascript") }
        val nextRaw = (doc.getElementById("next_chap")
            ?: doc.getElementsByClass("next-chap").firstOrNull()
            ?: doc.getElementsByClass("btn-next").firstOrNull())
            ?.attr("href")
            ?.takeIf { it.isNotEmpty() && it != "#" && !it.startsWith("javascript") }
        val prevHref = prevRaw?.let { resolveNovelUrl(base, it) }
        val nextHref = nextRaw?.let { resolveNovelUrl(base, it) }

        return ChapterContent.Text(
            title = title,
            chapterUrl = chapterUrl,
            text = text,
            prevChapterUrl = prevHref,
            nextChapterUrl = nextHref,
            paragraphs = paragraphs,
            sourceId = id
        )
    }
}
