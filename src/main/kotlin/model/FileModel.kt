package model

import org.jetbrains.compose.resources.DrawableResource
import top.wkbin.filemanager.generated.resources.Res
import top.wkbin.filemanager.generated.resources.icon_apk
import top.wkbin.filemanager.generated.resources.icon_file
import top.wkbin.filemanager.generated.resources.icon_files
import top.wkbin.filemanager.generated.resources.icon_image
import top.wkbin.filemanager.generated.resources.icon_json
import top.wkbin.filemanager.generated.resources.icon_ppt
import top.wkbin.filemanager.generated.resources.icon_txt
import top.wkbin.filemanager.generated.resources.icon_video
import top.wkbin.filemanager.generated.resources.icon_word
import top.wkbin.filemanager.generated.resources.icon_xls
import top.wkbin.filemanager.generated.resources.icon_xml
import top.wkbin.filemanager.generated.resources.icon_zip
import java.text.DecimalFormat

/**
 * Represents a file or directory in the file system
 */
data class FileItem(
    val isDir: Boolean,
    val fileName: String,
    val size: String,
    val date: String,
    val icon: DrawableResource,
    val link: String?,
    val permissions: String
)

/**
 * Utility class for file operations
 */
object FileUtils {
    private val decimalFormat = DecimalFormat("#.##")

    /**
     * Get the appropriate icon path for a file based on its extension
     */
    private fun getIconPath(isDir: Boolean, name: String): DrawableResource {
        if (isDir) {
            return Res.drawable.icon_files
        }
        val index = name.lastIndexOf(".")
        if (index == -1) {
            return Res.drawable.icon_file
        }
        return when (name.substring(index + 1)) {
            "zip", "rar", "7z" -> Res.drawable.icon_zip
            "ppt","pptx","pps","ppsx" -> Res.drawable.icon_ppt
            "doc","docx","dot","dotx" -> Res.drawable.icon_word
            "xls","xlsx","xlsm" -> Res.drawable.icon_xls
            "apk" -> Res.drawable.icon_apk
            "json" -> Res.drawable.icon_json
            "mp4","avi","mkv","mov","flv","wmv" -> Res.drawable.icon_video
            "png", "jpg", "jpeg" -> Res.drawable.icon_image
            "txt" -> Res.drawable.icon_txt
            "xml" -> Res.drawable.icon_xml
            else -> Res.drawable.icon_file
        }
    }

    /**
     * Format file size to a human-readable string
     */
    private fun formatSize(sizeInBytes: Long): String {
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
     * 格式化时间戳为可读的日期时间字符串
     */
    private fun formatTimestamp(timestamp: String): String {
        return try {
            val seconds = timestamp.toLong()
            val date = java.util.Date(seconds * 1000)
            val sdf = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
            sdf.format(date)
        } catch (e: Exception) {
            timestamp
        }
    }

    /**
     * 解析 stat 命令输出
     * 0: 文件类型 - directory、symbolic link、regular file
     * 1: 文件权限(八进制表示)
     * 2: 硬链接数
     * 3: 文件所有者用户名
     * 4: 文件所属组名
     * 5: 文件大小(字节)
     * 6: 最后修改时间(时间戳)
     * 7: 文件名
     * 8: 符号链接信息 (如果有)
     */
    fun parseStatOutput(statList: List<String>): List<FileItem> {
        return statList.mapNotNull { line ->
            val tokens = line.split("|")
            if (tokens.size < 8) return@mapNotNull null

            val type = tokens[0]
            val permissions = tokens[1]
            val size = tokens[5].toLongOrNull() ?: 0
            val timestamp = tokens[6]
            val name = tokens[7]

            val isDir = type.contains("directory", ignoreCase = true)
            val isLink = type.contains("symbolic link", ignoreCase = true)

            val linkPath = if (isLink && tokens.size > 8) {
                val linkInfo = tokens[8].trim()
                if (linkInfo.contains(" -> ")) {
                    linkInfo.split(" -> ")[1].replace("'", "").trim()
                } else {
                    linkInfo
                }
            } else null

            FileItem(
                isDir = isDir,
                fileName = name,
                size = if (isDir) "" else formatSize(size),
                date = formatTimestamp(timestamp),
                icon = getIconPath(isDir, name),
                link = linkPath,
                permissions = permissions
            )
        }
    }
} 