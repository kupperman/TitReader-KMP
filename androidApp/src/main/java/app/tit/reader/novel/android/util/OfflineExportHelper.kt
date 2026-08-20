package app.tit.reader.novel.android.util

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object OfflineExportHelper {

    suspend fun exportMangaChapterToCbz(
        context: Context,
        okHttpClient: OkHttpClient,
        imageUrls: List<String>,
        outputUri: Uri
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val outputStream: OutputStream = contentResolver.openOutputStream(outputUri) ?: return@withContext false
            val zipOut = ZipOutputStream(outputStream)

            imageUrls.forEachIndexed { index, url ->
                val request = Request.Builder().url(url).build()
                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val bytes = response.body?.bytes()
                    if (bytes != null) {
                        val fileName = String.format("%03d.jpg", index + 1)
                        val entry = ZipEntry(fileName)
                        zipOut.putNextEntry(entry)
                        zipOut.write(bytes)
                        zipOut.closeEntry()
                    }
                }
            }

            zipOut.finish()
            zipOut.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun exportNovelChapterToTxt(
        context: Context,
        title: String,
        text: String,
        outputUri: Uri
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val outputStream: OutputStream = contentResolver.openOutputStream(outputUri) ?: return@withContext false
            val fullContent = "$title\n\n$text\n"
            outputStream.write(fullContent.toByteArray(Charsets.UTF_8))
            outputStream.flush()
            outputStream.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}