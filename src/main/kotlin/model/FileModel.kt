package model

import org.jetbrains.compose.resources.DrawableResource
import top.wkbin.filemanager.generated.resources.Res
import top.wkbin.filemanager.generated.resources.icon_apk
import top.wkbin.filemanager.generated.resources.icon_file
import top.wkbin.filemanager.generated.resources.icon_files
import top.wkbin.filemanager.generated.resources.icon_image
import top.wkbin.filemanager.generated.resources.icon_json
import top.wkbin.filemanager.generated.resources.icon_txt
import top.wkbin.filemanager.generated.resources.icon_xml
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
        try {
            val tokens = line.split("\\s+".toRegex())
            // Permissions
            val permissions = tokens[0]
            
            // 检查是否为软链接
            val isLink = permissions.startsWith("l")
            
            // 目录判断逻辑
            // 1. 直接是目录(d开头)
            // 2. 是软链接(l开头)且指向目录的情况
            val isDir = permissions.startsWith("d") || 
                      (isLink && (line.contains(" -> ") && (
                           line.trimEnd().endsWith("/") || // 链接结尾有斜杠表示指向目录
                           detectDirectoryLink(line)      // 额外检测方法
                      )))

            if (permissions.contains("?")) {
                val linkIndicator = tokens.indexOf("->")
                var link: String? = null
                val name = if (linkIndicator != -1) {
                    link = tokens.last()
                    tokens[linkIndicator - 1]
                } else {
                    tokens.last()
                }

                return FileItem(
                    isDir = isDir,
                    fileName = name,
                    size = "",
                    date = "",
                    icon = getIconPath(isDir, name),
                    link = link,
                    permissions = permissions
                )
            } else {
                // 标准解析逻辑
                // 找到 -> 符号的位置
                val linkIndicator = line.indexOf(" -> ")
                
                // 正常解析文件/目录
                val size = tokens[4].toLongOrNull() ?: 0
                val date = tokens[5]
                val time = tokens[6]
                
                // 解析文件名
                var name = tokens[7].run {
                    if (permissions.startsWith("d") && endsWith("/")) {
                        substring(0, length - 1)
                    } else {
                        this
                    }
                }
                
                // 解析链接目标
                var link: String? = null
                
                // 如果是软链接，处理链接目标
                if (isLink && linkIndicator != -1) {
                    // 提取链接信息
                    link = line.substring(linkIndicator + 4).trim()
                    
                    // 从文件名中移除多余的部分
                    val nameEndIndex = line.indexOf(" -> ")
                    if (nameEndIndex != -1) {
                        val nameStartIndex = line.lastIndexOf(" ", nameEndIndex - 1) + 1
                        name = line.substring(nameStartIndex, nameEndIndex)
                    }
                }
                
                return FileItem(
                    isDir = isDir,
                    fileName = name,
                    size = if (isDir) "" else formatSize(decimalFormat, size),
                    date = "${date.replace("-", "/")} $time",
                    icon = getIconPath(isDir, name),
                    link = link,
                    permissions = permissions
                )
            }
        } catch (e: Exception) {
            // 如果解析失败，返回null
            throw e
        }
    }

    /**
     * 检测链接是否指向目录的增强方法
     * 这里使用常见的指向目录特征进行检测
     */
    private fun detectDirectoryLink(line: String): Boolean {
        // 提取链接目标部分
        val linkParts = line.split(" -> ")
        if (linkParts.size < 2) return false
        
        val linkTarget = linkParts[1].trim()
        
        // 以下特征通常表明目标是目录
        return linkTarget.endsWith("/") ||  // 以斜杠结尾
               linkTarget.contains("bin") || // 常见系统目录
               linkTarget.contains("etc") ||
               linkTarget.contains("lib") ||
               linkTarget.contains("usr") ||
               linkTarget.contains("var") ||
               linkTarget.contains("opt") ||
               linkTarget.contains("home") ||
               linkTarget.contains("mnt") ||
               linkTarget.contains("media") ||
               linkTarget.contains("data") ||
               linkTarget.contains("system") ||
               linkTarget.contains("storage")
    }

    /**
     * Get the appropriate icon path for a file based on its extension
     */
    fun getIconPath(isDir: Boolean, name: String): DrawableResource {
        if (isDir) {
            return Res.drawable.icon_files
        }
        val index = name.lastIndexOf(".")
        if (index == -1) {
            return Res.drawable.icon_file
        }
        return when (name.substring(index + 1)) {
            "apk" -> Res.drawable.icon_apk
            "json" -> Res.drawable.icon_json
            "png", "jpg", "jpeg" -> Res.drawable.icon_image
            "txt" -> Res.drawable.icon_txt
            "xml" -> Res.drawable.icon_xml
            else -> Res.drawable.icon_file
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