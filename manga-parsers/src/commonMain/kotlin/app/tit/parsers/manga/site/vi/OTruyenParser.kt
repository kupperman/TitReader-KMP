package app.tit.parsers.manga.site.vi

import app.tit.content.core.LoaderContext
import app.tit.content.core.MangaParser
import app.tit.content.core.annotation.MangaSourceParser
import app.tit.content.core.model.*
import kotlinx.serialization.json.*

@MangaSourceParser("OTRUYEN", "Ổ Truyện", "vi")
class OTruyenParser(
    private val context: LoaderContext
) : MangaParser {

    override val id: String = "OTRUYEN"
    override val name: String = "Ổ Truyện"
    override val domain: String = "https://otruyenapi.com"

    private val cdnImageBase = "https://img.otruyenapi.com/uploads/comics"

    override suspend fun getList(page: Int, filter: ContentFilter.MangaFilter): List<Content> {
        val category = when (filter.order) {
            SortOrder.HOT -> "dang-phat-hanh"
            SortOrder.COMPLETED -> "hoan-thanh"
            SortOrder.LATEST -> "truyen-moi"
        }
        val url = "$domain/v1/api/danh-sach/$category?page=$page"
        val jsonStr = context.getHtml(url)
        return parseItemsFromJson(jsonStr)
    }

    override suspend fun search(query: String, page: Int): List<Content> {
        val url = "$domain/v1/api/tim-kiem?keyword=$query&page=$page"
        val jsonStr = context.getHtml(url)
        return parseItemsFromJson(jsonStr)
    }

    private fun parseItemsFromJson(jsonStr: String): List<Content> {
        val list = mutableListOf<Content>()
        try {
            val root = Json.parseToJsonElement(jsonStr).jsonObject
            val data = root["data"]?.jsonObject ?: return emptyList()
            val cdnBase = data["APP_DOMAIN_CDN_IMAGE"]?.jsonPrimitive?.contentOrNull ?: cdnImageBase
            val items = data["items"]?.jsonArray ?: return emptyList()

            items.forEach { itemEl ->
                val obj = itemEl.jsonObject
                val slug = obj["slug"]?.jsonPrimitive?.contentOrNull ?: return@forEach
                val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: return@forEach
                val thumb = obj["thumb_url"]?.jsonPrimitive?.contentOrNull

                val coverUrl = if (!thumb.isNullOrEmpty()) {
                    if (thumb.startsWith("http")) thumb else "$cdnBase/uploads/comics/$thumb"
                } else null

                val latestChap = obj["chaptersLatest"]?.jsonArray?.firstOrNull()?.jsonObject?.get("chapter_name")?.jsonPrimitive?.contentOrNull?.let { "Chap $it" }

                val mangaUrl = "$domain/v1/api/truyen-tranh/$slug"

                list.add(
                    Content(
                        id = mangaUrl,
                        title = name,
                        url = mangaUrl,
                        coverUrl = coverUrl,
                        latestChapter = latestChap,
                        type = ContentType.MANGA,
                        sourceId = id,
                        sourceName = this.name
                    )
                )
            }
        } catch (_: Exception) {
            // Ignore parse errors
        }
        return list
    }

    override suspend fun getDetails(mangaUrl: String): ContentDetails {
        val jsonStr = context.getHtml(mangaUrl)
        val root = Json.parseToJsonElement(jsonStr).jsonObject
        val data = root["data"]?.jsonObject ?: error("Dữ liệu truyện không hợp lệ")
        val item = data["item"]?.jsonObject ?: error("Không tìm thấy thông tin truyện")
        val cdnBase = data["APP_DOMAIN_CDN_IMAGE"]?.jsonPrimitive?.contentOrNull ?: cdnImageBase

        val title = item["name"]?.jsonPrimitive?.contentOrNull ?: "Không có tiêu đề"
        val thumb = item["thumb_url"]?.jsonPrimitive?.contentOrNull
        val coverUrl = if (!thumb.isNullOrEmpty()) {
            if (thumb.startsWith("http")) thumb else "$cdnBase/uploads/comics/$thumb"
        } else null

        val desc = item["content"]?.jsonPrimitive?.contentOrNull?.replace(Regex("<[^>]*>"), "")?.trim()
        val author = item["author"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }?.joinToString(", ")

        val statusStr = item["status"]?.jsonPrimitive?.contentOrNull
        val status = if (statusStr?.contains("completed", ignoreCase = true) == true) {
            ContentStatus.COMPLETED
        } else {
            ContentStatus.ONGOING
        }

        val chapters = mutableListOf<Chapter>()
        var order = 1

        val serverList = item["chapters"]?.jsonArray
        serverList?.forEach { serverEl ->
            val serverData = serverEl.jsonObject["server_data"]?.jsonArray
            serverData?.forEach { chapEl ->
                val chapObj = chapEl.jsonObject
                val chapName = chapObj["chapter_name"]?.jsonPrimitive?.contentOrNull ?: "$order"
                val chapApiData = chapObj["chapter_api_data"]?.jsonPrimitive?.contentOrNull ?: return@forEach

                if (chapters.none { it.id == chapApiData }) {
                    chapters.add(
                        Chapter(
                            id = chapApiData,
                            title = "Chapter $chapName",
                            url = chapApiData,
                            order = order++,
                            sourceId = id
                        )
                    )
                }
            }
        }

        return ContentDetails(
            content = Content(
                id = mangaUrl,
                title = title,
                url = mangaUrl,
                coverUrl = coverUrl,
                author = author,
                latestChapter = chapters.lastOrNull()?.title,
                type = ContentType.MANGA,
                sourceId = id,
                sourceName = this.name
            ),
            description = desc,
            status = status,
            chapters = chapters
        )
    }

    override suspend fun getChapterContent(chapterUrl: String): ChapterContent.ImagePages {
        val jsonStr = context.getHtml(chapterUrl)
        val root = Json.parseToJsonElement(jsonStr).jsonObject
        val data = root["data"]?.jsonObject ?: error("Không thể tải nội dung chapter")
        val domainCdn = data["domain_cdn"]?.jsonPrimitive?.contentOrNull ?: ""
        val item = data["item"]?.jsonObject ?: error("Không tìm thấy dữ liệu ảnh chapter")

        val chapterPath = item["chapter_path"]?.jsonPrimitive?.contentOrNull ?: ""
        val chapterName = item["chapter_name"]?.jsonPrimitive?.contentOrNull ?: ""
        val imageFiles = item["chapter_image"]?.jsonArray

        val imageUrls = mutableListOf<String>()
        imageFiles?.forEach { imgEl ->
            val file = imgEl.jsonObject["image_file"]?.jsonPrimitive?.contentOrNull
            if (!file.isNullOrEmpty()) {
                imageUrls.add("$domainCdn/$chapterPath/$file")
            }
        }

        return ChapterContent.ImagePages(
            title = "Chapter $chapterName",
            chapterUrl = chapterUrl,
            imageUrls = imageUrls,
            prevChapterUrl = null,
            nextChapterUrl = null,
            sourceId = id
        )
    }
}