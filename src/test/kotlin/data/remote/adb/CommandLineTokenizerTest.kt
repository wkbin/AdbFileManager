package data.remote.adb

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CommandLineTokenizerTest {
    @Test
    fun `tokenizes adb arguments and quoted paths`() {
        assertEquals(
            listOf("install", "C:\\My Apps\\demo.apk", "--user", "0"),
            CommandLineTokenizer.tokenize("install \"C:\\My Apps\\demo.apk\" --user 0")
        )
    }

    @Test
    fun `preserves empty quoted argument`() {
        assertEquals(listOf("shell", "", "value with spaces"), CommandLineTokenizer.tokenize("shell \"\" 'value with spaces'"))
    }

    @Test
    fun `rejects unclosed quote`() {
        assertFailsWith<IllegalArgumentException> {
            CommandLineTokenizer.tokenize("install \"unfinished")
        }
    }

    @Test
    fun `finds current completion token including quoted value`() {
        assertEquals(
            CompletionToken(8, "My Fi"),
            findCompletionToken("cd /tmp \"My Fi")
        )
    }

    @Test
    fun `finds longest common completion prefix`() {
        assertEquals("system", longestCommonPrefix(listOf("system", "system_ext")))
        assertEquals("", longestCommonPrefix(emptyList()))
    }
}
