package model

import java.text.DecimalFormat

/**
 * Represents a file or directory in the file system
 */
data class FileItem(
    val isDir: Boolean,
    val fileName: String,
    val size: String,
    val date: String,
    val icon: String,
    val link: String?,
)

/**
 * Utility class for file operations
 */
object FileUtils {
    /**
     * Parse the output of 'ls -l' command to a list of FileItems
     */
    fun parseLsOutput(lsList: List<String>): List<FileItem> {
        val decimalFormat = DecimalFormat("#.##")
        return lsList.filter {
            it.isNotEmpty() && it.first() in listOf('d', 'l', '-')
        }.mapNotNull { line ->
            try {
                parseLsLine(line, decimalFormat)
            } catch (e: Exception) {
                null
            }
        }.sortedBy {
            val index = it.fileName.lastIndexOf(".")
            if (index == -1) {
                it.fileName
            } else {
                it.fileName.substring(index + 1)
            }
        }.sortedByDescending { it.isDir }
    }
    
    private fun parseLsLine(line: String, decimalFormat: DecimalFormat): FileItem {
        val tokens = line.split("\\s+".toRegex())
        // Permissions
        val permissions = tokens[0]
        val isDir = permissions.first() in listOf('d', 'l')

        if (permissions.contains("?")) {
            val isLink = tokens.contains("->")
            var link: String? = null
            val name = if (isLink) {
                link = tokens.last()
                tokens[tokens.size - 3]
            } else {
                tokens.last()
            }

            return FileItem(
                isDir = isDir,
                fileName = name,
                size = "",
                date = "",
                icon = getIconPath(isDir, name),
                link = link
            )
        } else {
            // Normal file/directory
            val size = tokens[4].toLongOrNull() ?: 0
            val date = tokens[5]
            val time = tokens[6]

            val name = tokens[7].run {
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
            return FileItem(
                isDir = isDir,
                fileName = name,
                size = if (isDir) "" else formatSize(decimalFormat, size),
                date = "${date.replace("-", "/")} $time",
                icon = getIconPath(isDir, name),
                link = link
            )
        }
    }

    /**
     * Get the appropriate icon path for a file based on its extension
     */
    fun getIconPath(isDir: Boolean, name: String): String {
        if (isDir) {
            return "res/icons/icon_files.png"
        }
        val index = name.lastIndexOf(".")
        if (index == -1) {
            return "res/icons/icon_file.png"
        }
        return when (name.substring(index + 1)) {
            "apk" -> "res/icons/icon_apk.png"
            "json" -> "res/icons/icon_json.png"
            "png", "jpg", "jpeg" -> "res/icons/icon_image.png"
            "txt" -> "res/icons/icon_txt.png"
            "xml" -> "res/icons/icon_xml.png"
            else -> "res/icons/icon_file.png"
        }
    }
    
    /**
     * Format file size to a human-readable string
     */
    fun formatSize(decimalFormat: DecimalFormat, sizeInBytes: Long): String {
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
    
    /**
     * Format JSON for display
     */
    fun formatJson(jsonString: String, prettyPrint: Boolean = true): String {
        return try {
            if (prettyPrint) {
                // You'll need to implement pretty printing
                jsonString // Placeholder
            } else {
                jsonString
            }
        } catch (e: Exception) {
            "Invalid JSON: ${e.message}"
        }
    }
} 