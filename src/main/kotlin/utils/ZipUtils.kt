package utils

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
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

        /**
         * Compares every file entry in a ZIP with an installed directory without
         * modifying either side. This avoids rewriting a running adb.exe on Windows.
         */
        fun archiveMatches(inputStream: InputStream, destDirectory: String): Boolean {
            val destDir = File(destDirectory)
            if (!destDir.isDirectory) return false
            val canonicalDestDir = destDir.canonicalPath + File.separator
            var fileEntryCount = 0

            ZipInputStream(inputStream).use { zipInputStream ->
                var zipEntry = zipInputStream.nextEntry
                while (zipEntry != null) {
                    if (!zipEntry.isDirectory) {
                        fileEntryCount++
                        val installedFile = File(destDir, zipEntry.name)
                        val canonicalPath = installedFile.canonicalPath
                        if (!canonicalPath.startsWith(canonicalDestDir)) return false
                        if (!installedFile.isFile) return false
                        if (zipEntry.size >= 0 && installedFile.length() != zipEntry.size) return false

                        val zipDigest = MessageDigest.getInstance("SHA-256")
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var count: Int
                        while (zipInputStream.read(buffer).also { count = it } > 0) {
                            zipDigest.update(buffer, 0, count)
                        }
                        val installedDigest = installedFile.inputStream().use { input ->
                            val digest = MessageDigest.getInstance("SHA-256")
                            while (input.read(buffer).also { count = it } > 0) {
                                digest.update(buffer, 0, count)
                            }
                            digest.digest()
                        }
                        if (!zipDigest.digest().contentEquals(installedDigest)) return false
                    }
                    zipInputStream.closeEntry()
                    zipEntry = zipInputStream.nextEntry
                }
            }
            return fileEntryCount > 0
        }
    }
}
