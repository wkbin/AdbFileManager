package model

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalFileModelTest {
    @Test
    fun `local file metadata preserves unicode name and raw size`() {
        val directory = Files.createTempDirectory("afm-local-pane-").toFile()
        try {
            val file = directory.resolve("本地 文件.txt")
            file.writeText("中文内容", Charsets.UTF_8)

            val item = FileUtils.fromLocalFile(file)

            assertEquals("本地 文件.txt", item.fileName)
            assertFalse(item.isDir)
            assertEquals(file.length(), item.sizeBytes)
            assertTrue(item.permissions.startsWith("r"))
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun `local directory is represented as a directory`() {
        val directory = Files.createTempDirectory("afm-local-folder-").toFile()
        try {
            val item = FileUtils.fromLocalFile(directory)
            assertTrue(item.isDir)
            assertEquals(0L, item.sizeBytes)
        } finally {
            directory.deleteRecursively()
        }
    }
}
