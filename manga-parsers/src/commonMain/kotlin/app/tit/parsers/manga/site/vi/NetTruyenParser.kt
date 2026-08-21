package app.tit.parsers.manga.site.vi

import app.tit.content.core.LoaderContext
import app.tit.content.core.MangaParser
import app.tit.content.core.annotation.MangaSourceParser
import app.tit.content.core.model.*
import com.fleeksoft.ksoup.Ksoup
import kotlinx.serialization.json.*

@MangaSourceParser("NETTRUYEN", "NetTruyen", "vi")
class NetTruyenParser(
    private val context: LoaderContext
) : MangaParser {

    override val id: String = "NETTRUYEN"
    override val name: String = "NetTruyen"
    override val domain: String get() = mirrors.first()

    private val mirrors = listOf(
        "https://nettruyenx.net",
        "https://nettruyenx.com",
        "https://nettruyenviet.com",
        "https://nettruyena.com",
        "https://nettruyenco.vn"
    )

    override suspend fun getList(page: Int, filter: ContentFilter.MangaFilter): List<Content> {
        val path = if (page == 1) "/" else "/?page=$page"
        val (doc, base) = context.parseHtmlRace(mirrors, path)
        val list = mutableListOf<Content>()

        doc.select(".items .item, .list-manga .item, .Item").forEach { el ->
            val link = el.selectFirst("h3 a, .title a, a.jtip")
            if (link != null) {
                val title = link.text().trim()
                val rawHref = link.attr("href").trim()
                val mangaUrl = if (rawHref.startsWith("http")) rawHref else "$base$rawHref"

                val cover = el.selectFirst("img")?.let { img ->
                    img.attr("data-original")
                        .ifEmpty { img.attr("data-src") }
                        .ifEmpty { img.attr("data-retries") }
                        .ifEmpty { img.attr("src") }
                }?.let { raw ->
                    when {
                        raw.startsWith("//") -> "https:$raw"
                        raw.startsWith("http") -> raw
                        raw.isNotEmpty() && !raw.startsWith("/assets/") -> "$base$raw"
                        else -> null
                    }
                }

                val latestChap = el.selectFirst(".chapter a, .comic-item .chapter")?.text()?.trim()

                if (title.isNotEmpty() && mangaUrl.isNotEmpty()) {
                    list.add(
                        Content(
                            id = mangaUrl,
                            title = title,
                            url = mangaUrl,
                            coverUrl = cover,
                            latestChapter = latestChap,
                            type = ContentType.MANGA,
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
        val path = if (page == 1) "/tim-truyen?keyword=$query" else "/tim-truyen?keyword=$query&page=$page"
        val (doc, base) = context.parseHtmlRace(mirrors, path)
        val list = mutableListOf<Content>()

        doc.select(".items .item, .list-manga .item").forEach { el ->
            val link = el.selectFirst("h3 a, .title a, a.jtip")
            if (link != null) {
                val title = link.text().trim()
                val rawHref = link.attr("href").trim()
                val mangaUrl = if (rawHref.startsWith("http")) rawHref else "$base$rawHref"

                val cover = el.selectFirst("img")?.let { img ->
                    img.attr("data-original")
                        .ifEmpty { img.attr("data-src") }
                        .ifEmpty { img.attr("data-retries") }
                        .ifEmpty { img.attr("src") }
                }?.let { raw ->
                    when {
                        raw.startsWith("//") -> "https:$raw"
                        raw.startsWith("http") -> raw
                        raw.isNotEmpty() && !raw.startsWith("/assets/") -> "$base$raw"
                        else -> null
                    }
                }

                val latestChap = el.selectFirst(".chapter a")?.text()?.trim()

                if (title.isNotEmpty() && mangaUrl.isNotEmpty()) {
                    list.add(
                        Content(
                            id = mangaUrl,
                            title = title,
                            url = mangaUrl,
                            coverUrl = cover,
                            latestChapter = latestChap,
                            type = ContentType.MANGA,
                            sourceId = id,
                            sourceName = name
                        )
                    )
                }
            }
        }
        return list
    }

    override suspend fun getDetails(mangaUrl: String): ContentDetails {
        val doc = context.parseHtml(mangaUrl)
        val title = doc.selectFirst("h1.title-detail, h1")?.text()?.trim() ?: "Không có tiêu đề"

        val cover = doc.selectFirst(".detail-info img, .col-image img")?.attr("src")
            ?: doc.selectFirst(".detail-info img")?.attr("data-src")

        val author = doc.selectFirst(".author .col-xs-8")?.text()?.trim()
        val desc = doc.selectFirst(".detail-content p, .shortened")?.text()?.trim()
        val status = if (doc.selectFirst(".status .col-xs-8")?.text()?.contains("Hoàn", ignoreCase = true) == true) {
            ContentStatus.COMPLETED
        } else {
            ContentStatus.ONGOING
        }

        val fallbackChapters = parseNetTruyenHtmlChapters(doc, domain, id)
        val slug = Regex("gOpts\\.comicSlug\\s*=\\s*['\"]([^'\"]+)")
            .find(doc.html())
            ?.groupValues
            ?.get(1)
            ?: mangaUrl.substringAfter("/truyen-tranh/").substringBefore('/').substringBefore('?')
        val chapters = runCatching {
            val json = context.getHtml("$domain/Comic/Services/ComicService.asmx/ChapterList?slug=$slug")
            parseNetTruyenChapterJson(json, slug, domain, id).ifEmpty { fallbackChapters }
        }.getOrElse { fallbackChapters }
        return ContentDetails(
            content = Content(
                id = mangaUrl,
                title = title,
                url = mangaUrl,
                coverUrl = cover,
                author = author,
                latestChapter = chapters.lastOrNull()?.title,
                type = ContentType.MANGA,
                sourceId = id,
                sourceName = name
            ),
            description = desc,
            status = status,
            chapters = chapters
        )
    }

    override suspend fun getChapterContent(chapterUrl: String): ChapterContent.ImagePages {
        val doc = context.parseHtml(chapterUrl)
        val title = doc.selectFirst(".top .title, h1")?.text()?.trim() ?: "Nội dung chương"

        val imageUrls = mutableListOf<String>()
        doc.select(".reading-detail .page-chapter img, .reading-detail img").forEach { img ->
            val src = img.attr("data-original").ifEmpty { img.attr("data-src") }.ifEmpty { img.attr("src") }.trim()
            if (src.isNotEmpty() && src.startsWith("http") && !imageUrls.contains(src)) {
                imageUrls.add(src)
            }
        }

        val prevHref = doc.selectFirst("a.prev")?.attr("href")?.takeIf { it.isNotEmpty() }
        val nextHref = doc.selectFirst("a.next")?.attr("href")?.takeIf { it.isNotEmpty() }

        return ChapterContent.ImagePages(
            title = title,
            chapterUrl = chapterUrl,
            imageUrls = imageUrls,
            prevChapterUrl = prevHref,
            nextChapterUrl = nextHref,
            sourceId = id
        )
    }
}
internal fun parseNetTruyenHtmlChapters(
    doc: com.fleeksoft.ksoup.nodes.Document,
    base: String,
    sourceId: String
): List<Chapter> = doc.select("#chapter_list li.row .chapter > a")
    .mapNotNull { link ->
        val title = link.text().trim()
        val rawHref = link.attr("href").trim()
        val number = Regex("(?:chapter|chương|chap)\\s*[-_:]?\\s*(\\d+(?:[.,]\\d+)?)", RegexOption.IGNORE_CASE)
            .find(title)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()
        if (title.isBlank() || rawHref.isBlank() || number == null) null
        else Triple(title, if (rawHref.startsWith("http")) rawHref else base.trimEnd('/') + "/" + rawHref.trimStart('/'), number)
    }
    .distinctBy { it.second }
    .sortedBy { it.third }
    .mapIndexed { index, item -> Chapter(item.second, item.first, item.second, index + 1, sourceId) }

internal fun parseNetTruyenChapterJson(
    json: String,
    slug: String,
    base: String,
    sourceId: String
): List<Chapter> {
    data class Parsed(val title: String, val url: String, val number: Double)
    val root = Json.parseToJsonElement(json).jsonObject
    return root["data"]?.jsonArray.orEmpty()
        .mapNotNull { element ->
            val item = element.jsonObject
            val number = item["chapter_num"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
            val title = item["chapter_name"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                .ifBlank { "Chapter ${formatNetTruyenChapterNumber(number)}" }
            val numberPath = formatNetTruyenChapterNumber(number)
            Parsed(title, "${base.trimEnd('/')}/truyen-tranh/$slug/chuong-$numberPath", number)
        }
        .distinctBy { it.url }
        .sortedBy { it.number }
        .mapIndexed { index, parsed -> Chapter(parsed.url, parsed.title, parsed.url, index + 1, sourceId) }
}

private fun formatNetTruyenChapterNumber(number: Double): String =
    if (number % 1.0 == 0.0) number.toLong().toString() else number.toString().trimEnd('0').trimEnd('.')