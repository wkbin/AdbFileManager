package utils

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

class ZipUtils {
    companion object {

        fun unzip(zipFilePath: String, destDirectory: String) {
            unzip(FileInputStream(zipFilePath), destDirectory)
        }

        @Throws(Exception::class)
        fun unzip(inputStream: InputStream, destDirectory: String) {
            val buffer = ByteArray(1024)
            val destDir = File(destDirectory)
            if (!destDir.exists()) {
                destDir.mkdir()
            }
            val zipInputStream = ZipInputStream(inputStream)
            var zipEntry = zipInputStream.nextEntry
            while (zipEntry != null) {
                val newFile = File(destDirectory + File.separator + zipEntry.name)
                // 创建目标文件夹（如果存在子文件夹）
                if (zipEntry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile.mkdirs()
                    val fileOutputStream = FileOutputStream(newFile)
                    var length = zipInputStream.read(buffer)
                    while (length > 0) {
                        fileOutputStream.write(buffer, 0, length)
                        length = zipInputStream.read(buffer)
                    }
                    fileOutputStream.close()
                }
                zipInputStream.closeEntry()
                zipEntry = zipInputStream.nextEntry
            }
            zipInputStream.close()
        }
    }
}