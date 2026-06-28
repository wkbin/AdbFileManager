package utils

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

class ZipUtils {
    companion object {

        fun unzip(zipFilePath: String, destDirectory: String) {
            FileInputStream(zipFilePath).use { inputStream ->
                unzip(inputStream, destDirectory)
            }
        }

        @Throws(Exception::class)
        fun unzip(inputStream: InputStream, destDirectory: String) {
            val buffer = ByteArray(1024)
            val destDir = File(destDirectory).also { dir ->
                if (!dir.exists()) {
                    dir.mkdirs()
                }
            }
            val canonicalDestDir = destDir.canonicalPath + File.separator

            ZipInputStream(inputStream).use { zipInputStream ->
                var zipEntry = zipInputStream.nextEntry
                while (zipEntry != null) {
                    val newFile = File(destDirectory, zipEntry.name)
                    val canonicalPath = newFile.canonicalPath

                    // Prevent Zip Slip path traversal attack
                    if (!canonicalPath.startsWith(canonicalDestDir)) {
                        throw SecurityException(
                            "Zip entry '${zipEntry.name}' is outside of the target directory"
                        )
                    }

                    if (zipEntry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile?.mkdirs()
                        FileOutputStream(newFile).use { fileOutputStream ->
                            var length: Int
                            while (zipInputStream.read(buffer).also { length = it } > 0) {
                                fileOutputStream.write(buffer, 0, length)
                            }
                        }
                    }

                    zipInputStream.closeEntry()
                    zipEntry = zipInputStream.nextEntry
                }
            }
        }
    }
}