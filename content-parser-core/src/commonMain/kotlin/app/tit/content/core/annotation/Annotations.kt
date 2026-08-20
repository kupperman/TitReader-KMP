package app.tit.content.core.annotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class NovelSourceParser(
    val id: String,
    val name: String,
    val lang: String = "vi"
)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class MangaSourceParser(
    val id: String,
    val name: String,
    val lang: String = "vi"
)
