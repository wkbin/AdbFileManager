package utils


import java.text.DecimalFormat

data class RemountFile(
    val isDir: Boolean,
    val fileName: String,
    val size: String,
    val date: String,
    val icon: String,
    val link: String?,
)

object FileManagerUtils {

    fun parseLsToRemountFileList(lsList: List<String>): List<RemountFile> {
        val decimalFormat = DecimalFormat("#.##")
        return lsList.filter {
            it.first() in listOf('d', 'l', '-') && !it.contains("?")
        }.map { line ->
            val tokens = line.split("\\s+".toRegex())
            // 权限
            val permissions = tokens[0]
            // links
            val links = tokens[1].toIntOrNull() ?: 0
            val owner = tokens[2]
            val group = tokens[3]
            val size = tokens[4].toLongOrNull() ?: 0
            val date = tokens[5]
            val time = tokens[6]
            val isDir = permissions.first() in listOf('d', 'l')

            println("tokens = ${tokens}")
            val name = tokens[7].run {
                println("name = $this")
                if (permissions.startsWith("d")) {
                    substring(0, length - 1)
                } else {
                    this
                }
            }
            val link = if (permissions.startsWith("l")) {
                tokens[9].run {
                    if (startsWith("/")) {
                        substring(1, length)
                    } else {
                        this
                    }
                }
            } else {
                null
            }
            RemountFile(
                isDir = isDir,
                fileName = name,
                size = if (isDir) "" else formatSize(decimalFormat, size),
                date = "${date.replace("-", "/")} $time",
                icon = getIconPath(isDir, name),
                link = link
            )
        }.sortedBy {
            val index = it.fileName.lastIndexOf(".")
            if (index == -1) {
                it.fileName
            } else {
                it.fileName.substring(index + 1)
            }
        }.sortedByDescending { it.isDir }
    }

    private fun getIconPath(isDir: Boolean, name: String): String {
        if (isDir) {
            return R.icons.iconFiles
        }
        val index = name.lastIndexOf(".")
        if (index == -1) {
            return R.icons.iconFile
        }
        return when (name.substring(index + 1)) {
            "apk" -> R.icons.iconApk
            "json" -> R.icons.iconJson
            "png", "jpg", "jpeg" -> R.icons.iconImage
            "txt" -> R.icons.iconTxt
            "xml" -> R.icons.iconXml
            else -> R.icons.iconFile
        }
    }

    private fun formatSize(decimalFormat: DecimalFormat, sizeInBytes: Long): String {
        val kiloBytes = sizeInBytes / 1024.0
        val megaBytes = kiloBytes / 1024.0
        val gigaBytes = megaBytes / 1024.0

        return when {
            gigaBytes >= 1 -> "${decimalFormat.format(gigaBytes)} GB"
            megaBytes >= 1 -> "${decimalFormat.format(megaBytes)} MB"
            kiloBytes >= 1 -> "${decimalFormat.format(kiloBytes)} KB"
            else -> "$sizeInBytes B"
        }
    }
}