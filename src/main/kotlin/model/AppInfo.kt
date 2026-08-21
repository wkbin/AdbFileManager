package model

/** Information displayed by the application manager. */
data class AppInfo(
    val packageName: String,
    val label: String = packageName,
    val iconData: ByteArray? = null,
    val detailsLoaded: Boolean = false
)
