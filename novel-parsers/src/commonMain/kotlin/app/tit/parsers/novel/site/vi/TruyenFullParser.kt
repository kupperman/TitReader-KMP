package app.tit.parsers.novel.site.vi

import app.tit.content.core.LoaderContext
import app.tit.content.core.NovelParser
import app.tit.content.core.annotation.NovelSourceParser
import app.tit.content.core.model.*
import io.ktor.http.encodeURLParameter
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

                val cover = (el.selectFirst("[data-image]")?.attr("data-image")?.takeIf { it.isNotEmpty() }
                    ?: el.selectFirst("[data-desk-image]")?.attr("data-desk-image")?.takeIf { it.isNotEmpty() }
                    ?: el.selectFirst(".lazy-image")?.attr("data-image")?.takeIf { it.isNotEmpty() }
                    ?: el.selectFirst("img")?.attr("data-src")?.takeIf { it.isNotEmpty() }
                    ?: el.selectFirst("img")?.attr("src")?.takeIf { it.isNotEmpty() })
                    ?.let { raw ->
                        when {
                            raw.startsWith("//") -> "https:$raw"
                            raw.startsWith("http") -> raw
                            raw.isNotEmpty() -> "$base$raw"
                            else -> null
                        }
                    }

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
        val path = if (page == 1) "/tim-kiem/?tukhoa=${query.encodeURLParameter()}" else "/tim-kiem/?tukhoa=${query.encodeURLParameter()}&page=$page"
        val (doc, base) = context.parseHtmlRace(mirrors, path)
        val list = mutableListOf<Content>()

        doc.select(".list-truyen .row, .truyen-list .row").forEach { el ->
            val titleEl = el.selectFirst("h3 a, .truyen-title a")
            if (titleEl != null) {
                val title = titleEl.text().trim()
                val rawHref = titleEl.attr("href").trim()
                val novelUrl = if (rawHref.startsWith("http")) rawHref else "$base$rawHref"

                val cover = (el.selectFirst("[data-image]")?.attr("data-image")?.takeIf { it.isNotEmpty() }
                    ?: el.selectFirst("[data-desk-image]")?.attr("data-desk-image")?.takeIf { it.isNotEmpty() }
                    ?: el.selectFirst(".lazy-image")?.attr("data-image")?.takeIf { it.isNotEmpty() }
                    ?: el.selectFirst("img")?.attr("data-src")?.takeIf { it.isNotEmpty() }
                    ?: el.selectFirst("img")?.attr("src")?.takeIf { it.isNotEmpty() })
                    ?.let { raw ->
                        when {
                            raw.startsWith("//") -> "https:$raw"
                            raw.startsWith("http") -> raw
                            raw.isNotEmpty() -> "$base$raw"
                            else -> null
                        }
                    }
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
        val (doc, base) = context.parseHtmlWithMirrors(novelUrl, mirrors)
        val title = (doc.selectFirst("h1.title, h1[itemprop='name'], .book-title, .truyen-title")?.text()?.trim()
            ?: doc.select("h1").map { it.text().trim() }.firstOrNull { it.length > 3 && !it.equals("Truyện", ignoreCase = true) }
            ?: "Không có tiêu đề")
        
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

        val chapterPages = mutableListOf(doc to base)
        val lastPage = doc.select("a[href*='/trang-']")
            .mapNotNull { Regex("/trang-(\\d+)").find(it.attr("href"))?.groupValues?.get(1)?.toIntOrNull() }
            .maxOrNull()
            ?.coerceAtMost(100)
            ?: 1

        val rootUrl = novelUrl.substringBefore("/trang-").trimEnd('/')
        for (pageNumber in 2..lastPage) {
            runCatching {
                context.parseHtmlDirectThenMirrors("$rootUrl/trang-$pageNumber/", mirrors)
            }.getOrNull()?.let(chapterPages::add)
        }

        val chapters = parseTruyenFullChapterPages(chapterPages, id)

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
        val (doc, base) = context.parseHtmlDirectThenMirrors(chapterUrl, mirrors)
        println("TIT_CHAPTER parse_document url=$chapterUrl")
        val title = doc.selectFirst(".chapter-title, h2.chapter-title, .title-chapter")?.text()?.trim() ?: "Nội dung chương"

        val contentEl = doc.selectFirst("#chapter-c, .chapter-c, [itemprop='articleBody'], #chapter-content")
            ?: error("Không tìm thấy nội dung chương")
        println("TIT_CHAPTER content_found url=$chapterUrl")
        contentEl.select("script, style, ins, .ads, .ads-holder, div[id*='ads'], div[class*='ads'], a[href*='truyenfull']").remove()
        println("TIT_CHAPTER content_cleaned url=$chapterUrl")

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
        println("TIT_CHAPTER text_ready chars=${text.length} paragraphs=${paragraphs.size} url=$chapterUrl")

        val prevHref = (doc.getElementById("prev_chap") ?: doc.getElementsByClass("btn-prev").firstOrNull())
            ?.attr("href")?.takeIf { it.isNotEmpty() }
        val nextHref = (doc.getElementById("next_chap") ?: doc.getElementsByClass("btn-next").firstOrNull())
            ?.attr("href")?.takeIf { it.isNotEmpty() }
        println("TIT_CHAPTER navigation_ready url=$chapterUrl")

        return ChapterContent.Text(
            title = title,
            chapterUrl = chapterUrl,
            text = text,
            paragraphs = paragraphs,
            prevChapterUrl = prevHref,
            nextChapterUrl = nextHref,
            sourceId = id
        )
    }
}
internal fun parseTruyenFullChapterPages(
    pages: List<Pair<com.fleeksoft.ksoup.nodes.Document, String>>,
    sourceId: String
): List<Chapter> {
    data class ParsedChapter(val title: String, val url: String, val number: Double?)

    val uniqueByUrl = linkedMapOf<String, ParsedChapter>()
    pages.forEach { (doc, base) ->
        doc.select("#list-chapter ul.list-chapter li a, #list-chapter .list-chapter li a, .list-chapter li a")
            .forEach { link ->
                val title = link.attr("title").ifBlank { link.text() }.trim()
                val rawHref = link.attr("href").trim()
                if (title.isEmpty() || rawHref.isEmpty()) return@forEach

                val url = resolveNovelUrl(base, rawHref)
                val number = extractChapterNumber(title) ?: extractChapterNumber(rawHref)
                uniqueByUrl.putIfAbsent(url, ParsedChapter(title, url, number))
            }
    }

    return uniqueByUrl.values
        .sortedWith(compareBy<ParsedChapter> { it.number ?: Double.MAX_VALUE }.thenBy { it.title })
        .mapIndexed { index, parsed ->
            Chapter(
                id = parsed.url,
                title = parsed.title,
                url = parsed.url,
                order = index + 1,
                sourceId = sourceId
            )
        }
}

internal fun extractChapterNumber(value: String): Double? =
    Regex("(?:chương|chapter|chuong)[-\\s_:]*(\\d+(?:[.,]\\d+)?)", RegexOption.IGNORE_CASE)
        .find(value)
        ?.groupValues
        ?.get(1)
        ?.replace(',', '.')
        ?.toDoubleOrNull()

internal fun resolveNovelUrl(base: String, rawHref: String): String = when {
    rawHref.startsWith("https://") || rawHref.startsWith("http://") -> rawHref
    rawHref.startsWith("//") -> "https:$rawHref"
    rawHref.startsWith("/") -> base.trimEnd('/') + rawHref
    else -> base.trimEnd('/') + "/" + rawHref
}
