package app.tit.parsers.manga.site.vi

import app.tit.content.core.LoaderContext
import app.tit.content.core.MangaParser
import app.tit.content.core.annotation.MangaSourceParser
import app.tit.content.core.model.*

@MangaSourceParser("TRUYENQQ", "TruyệnQQ", "vi")
class TruyenQQParser(
    private val context: LoaderContext
) : MangaParser {

    override val id: String = "TRUYENQQ"
    override val name: String = "TruyệnQQ"
    override val domain: String get() = mirrors.first()

    // Multi-domain list with latest active mirrors at the front and fallbacks preserved
    private val mirrors: List<String> = listOf(
        "https://truyenqqko.com",
        "https://truyenqqgo.com",
        "https://foxtruyen2.com",
        "https://truyenqqto.com",
        "https://truyenqqvn.com",
        "https://truyenqq.com"
    )

    override suspend fun getList(page: Int, filter: ContentFilter.MangaFilter): List<Content> {
        val path = when (filter.order) {
            SortOrder.HOT -> if (page == 1) "/truyen-yeu-thich.html" else "/truyen-yeu-thich/trang-$page.html"
            SortOrder.COMPLETED -> if (page == 1) "/truyen-hoan-thanh.html" else "/truyen-hoan-thanh/trang-$page.html"
            SortOrder.LATEST -> if (page == 1) "/doc-truyen" else "/doc-truyen?page=$page"
        }

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

                val cover = el.selectFirst("img")?.attr("data-src")
                    ?: el.selectFirst("img")?.attr("data-original")
                    ?: el.selectFirst("img")?.attr("src")

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
        val path = if (page == 1) "/tim-kiem?q=$query" else "/tim-kiem/trang-$page?q=$query"
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

                val cover = el.selectFirst("img")?.attr("data-src")
                    ?: el.selectFirst("img")?.attr("data-original")
                    ?: el.selectFirst("img")?.attr("src")

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

        val cover = doc.selectFirst(".book_avatar img, .detail-info img, .cover img")?.attr("src")
            ?: doc.selectFirst(".book_avatar img, .detail-info img")?.attr("data-src")

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

        val imageUrls = mutableListOf<String>()
        doc.select(".page-chapter img, .chapter_content img, .story-see-content img, .reading-detail img").forEach { img ->
            val src = img.attr("data-original").ifEmpty { img.attr("data-src") }.ifEmpty { img.attr("src") }.trim()
            if (src.isNotEmpty() && src.startsWith("http") && !imageUrls.contains(src)) {
                imageUrls.add(src)
            }
        }

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
