package view

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class TerminalPanelTest {
    @Test
    fun `dropped file path is quoted and appended to existing command`() {
        val file = File("C:\\My Apps\\demo.apk")

        assertEquals(
            "adb install \"${file.absolutePath}\"",
            appendDroppedFilePaths("adb install", listOf(file))
        )
    }

    @Test
    fun `multiple dropped paths preserve existing trailing space`() {
        val first = File("C:\\one.apk")
        val second = File("C:\\two files\\two.apk")

        assertEquals(
            "adb push \"${first.absolutePath}\" \"${second.absolutePath}\"",
            appendDroppedFilePaths("adb push ", listOf(first, second))
        )
    }
}
