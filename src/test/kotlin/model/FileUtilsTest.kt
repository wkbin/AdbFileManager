package model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileUtilsTest {
    @Test
    fun `stat parser preserves raw size and timestamp for accurate sorting and progress`() {
        val files = FileUtils.parseStatOutput(
            listOf(
                "regular file|-rw-r--r--|1|shell|shell|943718400|100|large.bin|large.bin",
                "regular file|-rw-r--r--|1|shell|shell|1073741824|20|newer.bin|newer.bin",
                "directory|drwxr-xr-x|2|shell|shell|4096|300|folder|folder"
            )
        )

        assertEquals(943_718_400L, files[0].sizeBytes)
        assertEquals(100L, files[0].modifiedEpochSeconds)
        assertEquals(1_073_741_824L, files[1].sizeBytes)
        assertTrue(files[2].isDir)
        assertEquals("", files[2].size)

        val sortedBySize = files.filterNot { it.isDir }.sortedBy { it.sizeBytes }
        assertEquals(listOf("large.bin", "newer.bin"), sortedBySize.map { it.fileName })
        assertFalse(sortedBySize.map { it.size }.sorted() == sortedBySize.map { it.size })

        val sortedByDate = files.sortedBy { it.modifiedEpochSeconds }
        assertEquals(listOf("newer.bin", "large.bin", "folder"), sortedByDate.map { it.fileName })
    }
}
