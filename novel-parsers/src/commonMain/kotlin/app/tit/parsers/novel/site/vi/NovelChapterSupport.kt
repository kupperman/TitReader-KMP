package app.tit.parsers.novel.site.vi

import app.tit.content.core.model.Chapter
import com.fleeksoft.ksoup.nodes.Document

internal fun parsePagedNovelChapters(doc: Document, base: String, sourceId: String): List<Chapter> {
    val unique = linkedMapOf<String, Pair<String, Double?>>()
    val strict = doc.select("#list-chapter ul.list-chapter li a, #chapter-list ul.list-chapter li a, #chapter-list li a, .list-chapter li a")
    val links = if (strict.isNotEmpty()) strict else doc.select("a[href*='chuong-']")
    links.forEach { link ->
        val title = link.attr("title").ifBlank { link.text() }.trim()
        val rawHref = link.attr("href").trim()
        if (title.isNotEmpty() && rawHref.isNotEmpty()) {
            val url = resolveNovelUrl(base, rawHref)
            unique.putIfAbsent(url, title to (extractChapterNumber(title) ?: extractChapterNumber(rawHref)))
        }
    }
    return unique.entries
        .sortedWith(compareBy<Map.Entry<String, Pair<String, Double?>>> { it.value.second ?: Double.MAX_VALUE }.thenBy { it.value.first })
        .mapIndexed { index, entry ->
            Chapter(entry.key, entry.value.first, entry.key, entry.value.second?.toInt() ?: index + 1, sourceId)
        }
}

internal fun findChapterPageCount(doc: Document): Int =
    doc.select("a[href*='/trang-']")
        .mapNotNull { Regex("/trang-(\\d+)", RegexOption.IGNORE_CASE).find(it.attr("href"))?.groupValues?.get(1)?.toIntOrNull() }
        .maxOrNull()
        ?.coerceAtLeast(1)
        ?: 1

internal fun chapterPageUrl(novelUrl: String, page: Int): String =
    if (page <= 1) novelUrl.substringBefore('#') else novelUrl.substringBefore('#').trimEnd('/') + "/trang-$page/"

internal fun selectRealChapterTitle(doc: Document): String {
    val candidates = mutableListOf<String>()
    listOf(".title-chapter", "h1.chapter-title", "h2.chapter-title", ".chapter-c h1", ".chapter-c h2", "#chapter-c h1", "#chapter-c h2", "h1", "h2")
        .forEach { selector ->
            doc.select(selector).forEach { element ->
                element.attr("title").trim().takeIf { it.isNotEmpty() }?.let(candidates::add)
                element.text().trim().takeIf { it.isNotEmpty() }?.let(candidates::add)
            }
        }
    val chapterPattern = Regex("(?:chương|chapter)\\s*[-_:]?\\s*\\d+", RegexOption.IGNORE_CASE)
    return candidates.firstOrNull { chapterPattern.containsMatchIn(it) }
        ?: doc.title().substringBefore(" - ").trim().takeIf { chapterPattern.containsMatchIn(it) }
        ?: "Nội dung chương"
}