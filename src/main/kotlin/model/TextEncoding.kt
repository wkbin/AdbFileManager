package model

import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

data class EncodedText(
    val bytes: ByteArray,
    val encoding: String,
    val upgradedToUtf8: Boolean
)

/**
 * Encodes editor content without ever replacing unmappable characters with '?'.
 * If the detected/original encoding cannot represent newly entered text, UTF-8 is
 * used as the lossless fallback.
 */
fun encodeTextLosslessly(text: String, preferredEncoding: String): EncodedText {
    val preferred = Charset.forName(preferredEncoding)
    val preferredBytes = tryEncodeStrictly(text, preferred)
    if (preferredBytes != null) {
        return EncodedText(preferredBytes, preferred.name(), upgradedToUtf8 = false)
    }

    val utf8 = Charsets.UTF_8
    val utf8Bytes = tryEncodeStrictly(text, utf8)
        ?: throw CharacterCodingException()
    return EncodedText(utf8Bytes, utf8.name(), upgradedToUtf8 = true)
}

/** ASCII has no encoding markers, so UTF-8 is the safest editable interpretation. */
fun normalizeDetectedTextEncoding(detectedEncoding: String?): String {
    val detected = detectedEncoding?.takeIf(Charset::isSupported) ?: return Charsets.UTF_8.name()
    val charset = Charset.forName(detected)
    return if (charset.newEncoder().maxBytesPerChar() == 1f &&
        charset.name().equals("US-ASCII", ignoreCase = true)
    ) {
        Charsets.UTF_8.name()
    } else {
        charset.name()
    }
}

private fun tryEncodeStrictly(text: String, charset: Charset): ByteArray? = try {
    val buffer: ByteBuffer = charset.newEncoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
        .encode(CharBuffer.wrap(text))
    ByteArray(buffer.remaining()).also(buffer::get)
} catch (_: CharacterCodingException) {
    null
}
