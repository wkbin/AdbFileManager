package view.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CodeSyntaxHighlighterTest {
    @Test
    fun `detects language from common editable file extensions`() {
        assertEquals("Kotlin", syntaxLanguage("Main.kt"))
        assertEquals("JSON", syntaxLanguage("settings.JSON"))
        assertEquals("Markup", syntaxLanguage("layout.xml"))
        assertEquals("Plain text", syntaxLanguage("notes.txt"))
    }

    @Test
    fun `kotlin tokenizer keeps comments strings numbers and keywords separate`() {
        val source = """
            // val inside comment
            val message = "return 42"
            if (message.isNotEmpty()) return 42
        """.trimIndent()

        val tokens = syntaxTokens(source, "sample.kt")
        assertTrue(tokens.any { it.type == SyntaxTokenType.COMMENT && source.substring(it.start, it.endExclusive).startsWith("//") })
        assertTrue(tokens.any { it.type == SyntaxTokenType.STRING && source.substring(it.start, it.endExclusive) == "\"return 42\"" })
        assertTrue(tokens.any { it.type == SyntaxTokenType.NUMBER && source.substring(it.start, it.endExclusive) == "42" })
        assertTrue(tokens.count { it.type == SyntaxTokenType.KEYWORD } >= 3)
        assertTrue(tokens.zipWithNext().all { (left, right) -> left.endExclusive <= right.start })
    }

    @Test
    fun `json tokenizer highlights keys without treating their contents as keywords`() {
        val source = """{"enabled": true, "count": 12}"""
        val tokens = syntaxTokens(source, "config.json")
        assertTrue(tokens.any { it.type == SyntaxTokenType.PROPERTY && source.substring(it.start, it.endExclusive) == "\"enabled\"" })
        assertTrue(tokens.any { it.type == SyntaxTokenType.KEYWORD && source.substring(it.start, it.endExclusive) == "true" })
        assertTrue(tokens.any { it.type == SyntaxTokenType.NUMBER && source.substring(it.start, it.endExclusive) == "12" })
    }
}
