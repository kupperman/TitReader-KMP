package app.tit.parsers.manga.site.vi

import app.tit.content.core.LoaderContext
import app.tit.content.core.MangaParser
import app.tit.content.core.annotation.MangaSourceParser
import app.tit.content.core.model.*
import com.fleeksoft.ksoup.Ksoup

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

        val chapters = mutableListOf<Chapter>()
        var order = 1
        doc.select(".list-chapter li.row a, #nt_listchapter a").forEach { a ->
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
