package view.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AnsiTextParserTest {

    @Test
    fun `plain text without ANSI remains unchanged`() {
        val input = "Hello, world!"
        val annotated = AnsiTextParser.parse(input)
        assertEquals("Hello, world!", annotated.text)
        assertTrue(annotated.spanStyles.isEmpty())
    }

    @Test
    fun `stripAnsi removes SGR color codes and other escape sequences`() {
        val input = "\u001B[1;34maudio\u001B[m \u001B[1;36mlib\u001B[m \u001B[0;0mboard_type\u001B[m"
        val stripped = AnsiTextParser.stripAnsi(input)
        assertEquals("audio lib board_type", stripped)
    }

    @Test
    fun `parse correctly converts ls colored output to AnnotatedString`() {
        val input = "\u001B[1;34maudio\u001B[m  \u001B[1;36mlib\u001B[m  \u001B[0mfile.txt\u001B[m"
        val annotated = AnsiTextParser.parse(input)

        assertEquals("audio  lib  file.txt", annotated.text)
        assertTrue(annotated.spanStyles.isNotEmpty())

        // Check that 'audio' (index 0..5) has blue color and bold
        val audioStyle = annotated.spanStyles.find { it.start == 0 && it.end == 5 }
        assertNotNull(audioStyle)
        assertEquals(FontWeight.Bold, audioStyle.item.fontWeight)
        assertEquals(Color(0xFF58A6FF), audioStyle.item.color)

        // Check that 'lib' (index 7..10) has cyan color and bold
        val libStyle = annotated.spanStyles.find { it.start == 7 && it.end == 10 }
        assertNotNull(libStyle)
        assertEquals(FontWeight.Bold, libStyle.item.fontWeight)
        assertEquals(Color(0xFF56D4DD), libStyle.item.color)
    }

    @Test
    fun `crlf and orphan carriage returns are normalized`() {
        val input = "line1\r\nline2\rline3"
        val annotated = AnsiTextParser.parse(input)
        assertEquals("line1\nline2line3", annotated.text)
    }

    @Test
    fun `strips non-SGR sequences such as cursor and clear screen codes`() {
        val input = "\u001B[2J\u001B[H\u001B[?25hReady"
        val stripped = AnsiTextParser.stripAnsi(input)
        assertEquals("Ready", stripped)
    }
}
