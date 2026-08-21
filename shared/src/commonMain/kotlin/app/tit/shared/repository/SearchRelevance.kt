package app.tit.shared.repository

/**
 * KMP-safe Vietnamese search normalization. This deliberately avoids java.text.Normalizer,
 * which is unavailable to commonMain/iOS.
 */
internal fun String.normalizeForSearch(): String = buildString(length) {
    lowercase().forEach { character ->
        val folded = when (character) {
            in "àáạảãâầấậẩẫăằắặẳẵ" -> 'a'
            in "èéẹẻẽêềếệểễ" -> 'e'
            in "ìíịỉĩ" -> 'i'
            in "òóọỏõôồốộổỗơờớợởỡ" -> 'o'
            in "ùúụủũưừứựửữ" -> 'u'
            in "ỳýỵỷỹ" -> 'y'
            'đ' -> 'd'
            else -> character
        }
        append(if (folded.isLetterOrDigit()) folded else ' ')
    }
}.split(Regex("\\s+")).filter { it.isNotBlank() }.joinToString(" ")

internal fun isRelevantSearchResult(title: String, query: String): Boolean {
    val normalizedTitle = title.normalizeForSearch()
    val queryTokens = query.normalizeForSearch().split(' ').filter { it.isNotBlank() }
    return queryTokens.isEmpty() || queryTokens.any(normalizedTitle::contains)
}
