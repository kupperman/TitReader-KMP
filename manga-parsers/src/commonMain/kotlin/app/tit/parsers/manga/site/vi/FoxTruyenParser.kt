package app.tit.parsers.manga.site.vi

import app.tit.content.core.LoaderContext
import app.tit.content.core.MangaParser
import app.tit.content.core.annotation.MangaSourceParser
import app.tit.content.core.model.*

@MangaSourceParser("FOXTRUYEN", "FoxTruyen", "vi")
class FoxTruyenParser(
    private val context: LoaderContext
) : MangaParser {

    override val id: String = "FOXTRUYEN"
    override val name: String = "FoxTruyen"
    override val domain: String get() = mirrors.first()

    private val mirrors: List<String> = listOf(
        "https://foxtruyen2.com"
    )

    override suspend fun getList(page: Int, filter: ContentFilter.MangaFilter): List<Content> {
        val path = when (filter.order) {
            SortOrder.HOT -> if (page == 1) "/top-tuan.html" else "/top-tuan/trang-$page.html"
            SortOrder.COMPLETED -> if (page == 1) "/top-binh-chon.html" else "/top-binh-chon/trang-$page.html"
            SortOrder.LATEST -> if (page == 1) "/truyen-moi-cap-nhat.html" else "/truyen-moi-cap-nhat/trang-$page.html"
        }

        val (doc, base) = context.parseHtmlRace(mirrors, path)
        val list = mutableListOf<Content>()

        doc.select(".list_item_home .item_home, .item_home, .list_grid li, ul.grid > li, .story-item, .item, .book_avatar").forEach { el ->
            val link = el.selectFirst(".title-book, .image-cover a, .book_info h3 a, .book_info a, h3 a, .title a, a.story-title, .book_avatar a")

            if (link != null) {
                val title = link.text().trim().ifEmpty { link.attr("title").trim() }
                    .ifEmpty { el.selectFirst("img")?.attr("alt")?.trim() ?: "" }
                val rawHref = link.attr("href").trim().ifEmpty {
                    el.selectFirst(".image-cover a")?.attr("href")?.trim() ?: ""
                }
                val mangaUrl = if (rawHref.startsWith("http")) rawHref else "$base$rawHref"

                val cover = el.selectFirst("img")?.let { img ->
                    img.attr("data-src")
                        .ifEmpty { img.attr("data-original") }
                        .ifEmpty { img.attr("data-fb") }
                        .ifEmpty { img.attr("src") }
                }?.let { raw ->
                    when {
                        raw.startsWith("//") -> "https:$raw"
                        raw.startsWith("http") -> raw
                        raw.isNotEmpty() && !raw.contains("no_image") -> "$base$raw"
                        else -> null
                    }
                }

                val latestChap = el.selectFirst(".last_chapter a, .chapter a, .story-chapter a")?.text()?.trim()

                if (title.isNotEmpty() && mangaUrl.isNotEmpty() && !mangaUrl.contains("/the-loai/")) {
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
        val path = if (page == 1) "/tim-kiem.html?q=$query" else "/tim-kiem/trang-$page.html?q=$query"
        val (doc, base) = context.parseHtmlRace(mirrors, path)
        val list = mutableListOf<Content>()

        doc.select(".list_grid li, ul.grid > li, .story-item, .item").forEach { el ->
            val link = el.selectFirst(".book_info h3 a")
                ?: el.selectFirst(".book_info a")
                ?: el.selectFirst("h3 a, .title a, a.story-title")
                ?: el.selectFirst(".book_avatar a")

            if (link != null) {
                val title = link.text().trim().ifEmpty { link.attr("title").trim() }
                    .ifEmpty { el.selectFirst("img")?.attr("alt")?.trim() ?: "" }
                val rawHref = link.attr("href").trim().ifEmpty {
                    el.selectFirst(".book_avatar a")?.attr("href")?.trim() ?: ""
                }
                val mangaUrl = if (rawHref.startsWith("http")) rawHref else "$base$rawHref"

                val cover = el.selectFirst("img")?.let { img ->
                    img.attr("data-src")
                        .ifEmpty { img.attr("data-original") }
                        .ifEmpty { img.attr("data-fb") }
                        .ifEmpty { img.attr("src") }
                }?.let { raw ->
                    when {
                        raw.startsWith("//") -> "https:$raw"
                        raw.startsWith("http") -> raw
                        raw.isNotEmpty() && !raw.contains("no_image") -> "$base$raw"
                        else -> null
                    }
                }

                val latestChap = el.selectFirst(".last_chapter a, .chapter a")?.text()?.trim()

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
        val title = doc.selectFirst("h1[itemprop='name'], h1.title, h1")?.text()?.trim() ?: "Không có tiêu đề"

        val cover = doc.selectFirst(".book_avatar img, .detail-info img, .cover img")?.let { img ->
            img.attr("data-src").ifEmpty { img.attr("src") }
        }

        val author = doc.selectFirst(".author a, .org a, .author p")?.text()?.trim()
        val desc = doc.selectFirst(".story-detail-info, .detail-content p, .story-desc")?.text()?.trim()
        val status = if (doc.selectFirst(".status p, .status")?.text()?.contains("Hoàn", ignoreCase = true) == true) {
            ContentStatus.COMPLETED
        } else {
            ContentStatus.ONGOING
        }

        val chapters = mutableListOf<Chapter>()
        var order = 1
        doc.select(".works-chapter-list a, .list_chapter a, .chapter_list a, a[href*='-chap-']").forEach { a ->
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
        val title = doc.selectFirst(".chapter-title, .heading-title, h1")?.text()?.trim() ?: "Nội dung chapter"

        val imageUrls = parseFoxChapterImages(doc)

        val prevHref = doc.selectFirst("a.btn-prev, a.prev")?.attr("href")?.takeIf { it.isNotEmpty() }
        val nextHref = doc.selectFirst("a.btn-next, a.next")?.attr("href")?.takeIf { it.isNotEmpty() }

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
internal fun parseFoxChapterImages(doc: com.fleeksoft.ksoup.nodes.Document): List<String> {
    val content = doc.selectFirst(".content_detail.content_detail_manga") ?: return emptyList()
    val imageExtension = Regex("\\.(?:webp|jpe?g|png)(?:[?#].*)?$", RegexOption.IGNORE_CASE)
    return content.select("img")
        .mapNotNull { image ->
            image.attr("src").ifBlank { image.attr("data-src") }.ifBlank { image.attr("data-original") }
                .trim()
                .takeIf { it.startsWith("https://hinhgg.com/") && imageExtension.containsMatchIn(it) }
        }
        .filterNot { it.contains("logo", ignoreCase = true) || it.contains("banner", ignoreCase = true) }
        .distinct()
}