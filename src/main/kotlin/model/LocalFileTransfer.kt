package model

import java.io.File

fun copyDroppedFilesToDirectory(files: List<File>, destinationDirectory: File) {
    require(destinationDirectory.isDirectory) {
        "目标不是本地目录：${destinationDirectory.absolutePath}"
    }
    files.forEach { source ->
        require(source.exists()) { "拖放源文件不存在：${source.absolutePath}" }
        val destination = File(destinationDirectory, source.name)
        if (source.isDirectory) {
            check(source.copyRecursively(destination, overwrite = true)) {
                "复制目录失败：${source.absolutePath}"
            }
        } else {
            source.copyTo(destination, overwrite = true)
        }
    }
}
