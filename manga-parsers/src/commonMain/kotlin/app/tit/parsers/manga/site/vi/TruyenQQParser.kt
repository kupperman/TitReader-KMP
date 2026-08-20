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

    private val mirrors: List<String> = listOf(
        "https://truyenqqko.com"
    )

    override suspend fun getList(page: Int, filter: ContentFilter.MangaFilter): List<Content> {
        val path = when (filter.order) {
            SortOrder.HOT -> if (page == 1) "/top-tuan" else "/top-tuan/trang-$page"
            SortOrder.COMPLETED -> if (page == 1) "/truyen-hoan-thanh" else "/truyen-hoan-thanh/trang-$page"
            SortOrder.LATEST -> if (page == 1) "/truyen-moi-cap-nhat" else "/truyen-moi-cap-nhat/trang-$page"
        }

        val (doc, base) = context.parseHtmlRace(mirrors, path)
        val list = mutableListOf<Content>()

        doc.select(".list_grid > li, ul.grid > li, .story-item").forEach { el ->
            val link = el.selectFirst(".book_info h3 a")
                ?: el.selectFirst(".book_info a")
                ?: el.selectFirst(".book_name a")
                ?: el.selectFirst("h3 a, .title a, a.story-title")
                ?: el.selectFirst(".book_avatar a")
                ?: el.selectFirst("a[href*='/truyen-tranh/']")

            if (link != null) {
                val title = link.text().trim().ifEmpty { link.attr("title").trim() }
                    .ifEmpty { el.selectFirst("img")?.attr("alt")?.trim() ?: "" }
                val rawHref = link.attr("href").trim().ifEmpty {
                    el.selectFirst(".book_avatar a")?.attr("href")?.trim() ?: ""
                }
                val mangaUrl = if (rawHref.startsWith("http")) rawHref else "$base$rawHref"

                val cover = el.selectFirst("img")?.let { img ->
                    img.attr("src")
                        .ifEmpty { img.attr("data-src") }
                        .ifEmpty { img.attr("data-original") }
                        .ifEmpty { img.attr("data-fb") }
                }?.let { raw ->
                    when {
                        raw.startsWith("//") -> "https:$raw"
                        raw.startsWith("http") -> raw
                        raw.isNotEmpty() && !raw.contains("no_image") -> "$base$raw"
                        else -> null
                    }
                }

                val latestChap = el.selectFirst(".last_chapter a, .chapter a, .story-chapter a, a[href*='-chap-']")?.text()?.trim()

                if (title.isNotEmpty() && mangaUrl.isNotEmpty() && !mangaUrl.contains("/the-loai/") && list.none { it.url == mangaUrl }) {
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
        val path = if (page == 1) "/tim-kiem-nang-cao?q=$query" else "/tim-kiem-nang-cao/trang-$page?q=$query"
        val (doc, base) = context.parseHtmlRace(mirrors, path)
        val list = mutableListOf<Content>()

        doc.select(".list_grid > li, ul.grid > li, .story-item").forEach { el ->
            val link = el.selectFirst(".book_info h3 a")
                ?: el.selectFirst(".book_info a")
                ?: el.selectFirst(".book_name a")
                ?: el.selectFirst("h3 a, .title a, a.story-title")
                ?: el.selectFirst(".book_avatar a")
                ?: el.selectFirst("a[href*='/truyen-tranh/']")

            if (link != null) {
                val title = link.text().trim().ifEmpty { link.attr("title").trim() }
                    .ifEmpty { el.selectFirst("img")?.attr("alt")?.trim() ?: "" }
                val rawHref = link.attr("href").trim().ifEmpty {
                    el.selectFirst(".book_avatar a")?.attr("href")?.trim() ?: ""
                }
                val mangaUrl = if (rawHref.startsWith("http")) rawHref else "$base$rawHref"

                val cover = el.selectFirst("img")?.let { img ->
                    img.attr("src")
                        .ifEmpty { img.attr("data-src") }
                        .ifEmpty { img.attr("data-original") }
                        .ifEmpty { img.attr("data-fb") }
                }?.let { raw ->
                    when {
                        raw.startsWith("//") -> "https:$raw"
                        raw.startsWith("http") -> raw
                        raw.isNotEmpty() && !raw.contains("no_image") -> "$base$raw"
                        else -> null
                    }
                }

                val latestChap = el.selectFirst(".last_chapter a, .chapter a, .story-chapter a, a[href*='-chap-']")?.text()?.trim()

                if (title.isNotEmpty() && mangaUrl.isNotEmpty() && !mangaUrl.contains("/the-loai/") && list.none { it.url == mangaUrl }) {
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
        val title = doc.selectFirst("h1[itemprop='name'], h1.title, .story-detail h1, h1")?.text()?.trim() ?: "Không có tiêu đề"

        val cover = doc.selectFirst(".book_avatar img, .story-thumb img, .thumb img, img[itemprop='image']")?.let { img ->
            img.attr("src")
                .ifEmpty { img.attr("data-src") }
                .ifEmpty { img.attr("data-original") }
                .ifEmpty { img.attr("data-fb") }
        }?.let { raw ->
            when {
                raw.startsWith("//") -> "https:$raw"
                raw.startsWith("http") -> raw
                raw.isNotEmpty() && !raw.contains("no_image") -> "$domain$raw"
                else -> null
            }
        }

        val author = doc.selectFirst(".author a, .org a, a[href*='tac-gia']")?.text()?.trim()
        val desc = doc.selectFirst(".story-detail-info, .detail-content, .story-desc, .desc")?.text()?.trim()
        val status = if (doc.selectFirst(".status, .info")?.text()?.contains("Hoàn", ignoreCase = true) == true) {
            ContentStatus.COMPLETED
        } else {
            ContentStatus.ONGOING
        }

        val chapters = mutableListOf<Chapter>()
        var order = 1
        val chapElements = doc.select(".works-chapter-item a, .list_chapter a, .chapter_list a, a[href*='-chap-']")
        chapElements.forEach { a ->
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
                latestChapter = chapters.firstOrNull()?.title,
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
        val title = doc.selectFirst(".top .title, h1, .chapter-title")?.text()?.trim() ?: "Nội dung chương"
        val imageUrls = mutableListOf<String>()

        doc.select(".chapter_content img, .story-see-content img, .page-chapter img, img.lazy, .chapter_img img, .reading-detail img").forEach { img ->
            val src = img.attr("data-cdn")
                .ifEmpty { img.attr("data-original") }
                .ifEmpty { img.attr("data-src") }
                .ifEmpty { img.attr("src") }
                .trim()

            val cleanUrl = when {
                src.startsWith("//") -> "https:$src"
                src.startsWith("http") -> src
                src.isNotEmpty() && !src.contains("no_image") -> "$domain$src"
                else -> null
            }

            if (cleanUrl != null && !cleanUrl.contains("loading") && !cleanUrl.contains("logo") && !imageUrls.contains(cleanUrl)) {
                imageUrls.add(cleanUrl)
            }
        }

        // Fallback: Trích xuất ảnh trực tiếp từ mã Javascript obfuscated của TruyenQQ
        if (imageUrls.isEmpty()) {
            val htmlContent = doc.html()
            val imgRegex = Regex("https?://[^\"'\\s\\\\)]+?\\.(?:jpg|jpeg|png|webp)", RegexOption.IGNORE_CASE)
            imgRegex.findAll(htmlContent).forEach { match ->
                val link = match.value
                val isStoryImg = (link.contains("truyenvua") || link.contains("hinhtruyen") || link.contains("tintruyen") || link.contains("cdn") || link.contains("truyenqq")) &&
                    !link.contains("logo") && !link.contains("template") && !link.contains("frontend") && !link.contains("avatar") && !link.contains("banner")
                if (isStoryImg && !imageUrls.contains(link)) {
                    imageUrls.add(link)
                }
            }
        }

        val prevHref = doc.selectFirst("a.prev_chap, a.prev, a.btn-prev")?.attr("href")?.takeIf { it.isNotEmpty() }?.let {
            if (it.startsWith("http")) it else "$domain$it"
        }
        val nextHref = doc.selectFirst("a.next_chap, a.next, a.btn-next")?.attr("href")?.takeIf { it.isNotEmpty() }?.let {
            if (it.startsWith("http")) it else "$domain$it"
        }

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