package model

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocalFileTransferTest {
    @Test
    fun `copies dropped unicode file into current local directory`() {
        val root = Files.createTempDirectory("afm-drop-").toFile()
        try {
            val sourceDir = root.resolve("source").apply { mkdirs() }
            val destinationDir = root.resolve("destination").apply { mkdirs() }
            val source = sourceDir.resolve("远程 文件.txt").apply { writeText("内容") }

            copyDroppedFilesToDirectory(listOf(source), destinationDir)

            assertEquals("内容", destinationDir.resolve(source.name).readText())
            assertTrue(source.exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `copies dropped directory recursively`() {
        val root = Files.createTempDirectory("afm-drop-dir-").toFile()
        try {
            val source = root.resolve("source/目录").apply { mkdirs() }
            source.resolve("子文件.txt").writeText("nested")
            val destination = root.resolve("destination").apply { mkdirs() }

            copyDroppedFilesToDirectory(listOf(source), destination)

            assertEquals("nested", destination.resolve("目录/子文件.txt").readText())
        } finally {
            root.deleteRecursively()
        }
    }
}
