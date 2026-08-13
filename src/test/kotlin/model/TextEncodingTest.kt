package model

import java.nio.charset.Charset
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TextEncodingTest {
    @Test
    fun `ASCII detection is normalized to UTF-8 for future edits`() {
        assertEquals("UTF-8", normalizeDetectedTextEncoding("US-ASCII"))
        assertEquals("UTF-8", normalizeDetectedTextEncoding("ASCII"))
    }

    @Test
    fun `encoding is preserved when it can represent all text`() {
        val encoded = encodeTextLosslessly("中文内容", "GBK")

        assertEquals("GBK", encoded.encoding)
        assertFalse(encoded.upgradedToUtf8)
        assertEquals("中文内容", encoded.bytes.toString(Charset.forName("GBK")))
    }

    @Test
    fun `single byte encoding upgrades to UTF-8 instead of replacing Chinese`() {
        val encoded = encodeTextLosslessly("hello 中文", "US-ASCII")

        assertEquals("UTF-8", encoded.encoding)
        assertTrue(encoded.upgradedToUtf8)
        assertEquals("hello 中文", encoded.bytes.toString(Charsets.UTF_8))
        assertContentEquals("hello 中文".toByteArray(Charsets.UTF_8), encoded.bytes)
    }

    @Test
    fun `GBK upgrades losslessly when text contains unmappable characters`() {
        val encoded = encodeTextLosslessly("中文 😀", "GBK")

        assertEquals("UTF-8", encoded.encoding)
        assertTrue(encoded.upgradedToUtf8)
        assertEquals("中文 😀", encoded.bytes.toString(Charsets.UTF_8))
    }
}
