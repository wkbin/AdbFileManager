package view.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

object AnsiTextParser {
    val DEFAULT_TEXT_COLOR = Color(0xFFC9D1D9)

    // Standard 16 ANSI colors tuned for terminal dark background
    private val ANSI_FOREGROUND = mapOf(
        30 to Color(0xFF484F58), // Black
        31 to Color(0xFFFF7B72), // Red
        32 to Color(0xFF7EE787), // Green
        33 to Color(0xFFE3B341), // Yellow
        34 to Color(0xFF58A6FF), // Blue
        35 to Color(0xFFBC8CFF), // Magenta
        36 to Color(0xFF56D4DD), // Cyan
        37 to Color(0xFFC9D1D9), // White / Light Gray
        90 to Color(0xFF8B949E), // Bright Black (Gray)
        91 to Color(0xFFFFA198), // Bright Red
        92 to Color(0xFF56D364), // Bright Green
        93 to Color(0xFFF2CC60), // Bright Yellow
        94 to Color(0xFF79C0FF), // Bright Blue
        95 to Color(0xFFD2A8FF), // Bright Magenta
        96 to Color(0xFF76E3EA), // Bright Cyan
        97 to Color(0xFFFFFFFF)  // Bright White
    )

    private val ANSI_BACKGROUND = mapOf(
        40 to Color(0xFF0D1117), // Black
        41 to Color(0xFF5A1E1E), // Red
        42 to Color(0xFF1B4721), // Green
        43 to Color(0xFF5C4100), // Yellow
        44 to Color(0xFF173E6E), // Blue
        45 to Color(0xFF482974), // Magenta
        46 to Color(0xFF144D52), // Cyan
        47 to Color(0xFF30363D), // White
        100 to Color(0xFF21262D),
        101 to Color(0xFF6E2020),
        102 to Color(0xFF235A2B),
        103 to Color(0xFF745300),
        104 to Color(0xFF1F5396),
        105 to Color(0xFF5C3599),
        106 to Color(0xFF1B6369),
        107 to Color(0xFF484F58)
    )

    // Matches ANSI escape codes: CSI (e.g. \u001B[...m), OSC (e.g. \u001B]...\u0007), and standalone ESC sequences
    private val ANSI_PATTERN = Regex(
        "\u001B\\[([0-9;?<=>]*)([a-zA-Z])|\u001B\\][^\u0007\u001B]*(?:\u0007|\u001B\\\\)|\u001B[PX^_][^\u001B]*\u001B\\\\|\u001B[=><NOc-~]"
    )

    /**
     * Strips all ANSI escape sequences and normalizes line breaks.
     */
    fun stripAnsi(text: String): String {
        if (!text.contains('\u001B') && !text.contains('\r')) return text
        return text
            .replace("\r\n", "\n")
            .replace("\r", "")
            .replace(ANSI_PATTERN, "")
    }

    /**
     * Parses ANSI text containing SGR color and style sequences into a Compose AnnotatedString.
     */
    fun parse(text: String, defaultColor: Color = DEFAULT_TEXT_COLOR): AnnotatedString {
        if (!text.contains('\u001B') && !text.contains('\r')) {
            return AnnotatedString(text)
        }

        val normalized = text.replace("\r\n", "\n").replace("\r", "")

        return buildAnnotatedString {
            var currentIndex = 0
            var foregroundColor: Color? = null
            var backgroundColor: Color? = null
            var isBold = false
            var isDim = false
            var isItalic = false
            var isUnderline = false
            var isStrikethrough = false

            fun getCurrentSpanStyle(): SpanStyle {
                val effectiveColor = when {
                    foregroundColor != null -> foregroundColor!!
                    isDim -> defaultColor.copy(alpha = 0.6f)
                    else -> defaultColor
                }

                val textDecorations = mutableListOf<TextDecoration>()
                if (isUnderline) textDecorations.add(TextDecoration.Underline)
                if (isStrikethrough) textDecorations.add(TextDecoration.LineThrough)

                return SpanStyle(
                    color = effectiveColor,
                    background = backgroundColor ?: Color.Transparent,
                    fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                    textDecoration = when {
                        textDecorations.isEmpty() -> TextDecoration.None
                        textDecorations.size == 1 -> textDecorations.first()
                        else -> TextDecoration.combine(textDecorations)
                    }
                )
            }

            fun appendStyledChunk(chunk: String) {
                if (chunk.isNotEmpty()) {
                    withStyle(getCurrentSpanStyle()) {
                        append(chunk)
                    }
                }
            }

            val matcher = ANSI_PATTERN.toPattern().matcher(normalized)
            while (matcher.find()) {
                val start = matcher.start()
                val end = matcher.end()

                if (start > currentIndex) {
                    appendStyledChunk(normalized.substring(currentIndex, start))
                }
                currentIndex = end

                val paramString = matcher.group(1)
                val command = matcher.group(2)

                // Only process SGR (Select Graphic Rendition) commands, denoted by 'm'
                if (command == "m") {
                    val codes = if (paramString.isNullOrEmpty()) {
                        listOf(0)
                    } else {
                        paramString.split(';').mapNotNull { it.toIntOrNull() }.ifEmpty { listOf(0) }
                    }

                    var index = 0
                    while (index < codes.size) {
                        val code = codes[index]
                        when (code) {
                            0 -> {
                                foregroundColor = null
                                backgroundColor = null
                                isBold = false
                                isDim = false
                                isItalic = false
                                isUnderline = false
                                isStrikethrough = false
                            }
                            1 -> isBold = true
                            2 -> isDim = true
                            3 -> isItalic = true
                            4 -> isUnderline = true
                            9 -> isStrikethrough = true
                            22 -> {
                                isBold = false
                                isDim = false
                            }
                            23 -> isItalic = false
                            24 -> isUnderline = false
                            29 -> isStrikethrough = false
                            in 30..37, in 90..97 -> foregroundColor = ANSI_FOREGROUND[code]
                            39 -> foregroundColor = null
                            in 40..47, in 100..107 -> backgroundColor = ANSI_BACKGROUND[code]
                            49 -> backgroundColor = null
                            38 -> {
                                // Extended foreground color (256-color or 24-bit TrueColor)
                                if (index + 2 < codes.size && codes[index + 1] == 5) {
                                    foregroundColor = parse256Color(codes[index + 2])
                                    index += 2
                                } else if (index + 4 < codes.size && codes[index + 1] == 2) {
                                    foregroundColor = Color(
                                        codes[index + 2].coerceIn(0, 255),
                                        codes[index + 3].coerceIn(0, 255),
                                        codes[index + 4].coerceIn(0, 255)
                                    )
                                    index += 4
                                }
                            }
                            48 -> {
                                // Extended background color (256-color or 24-bit TrueColor)
                                if (index + 2 < codes.size && codes[index + 1] == 5) {
                                    backgroundColor = parse256Color(codes[index + 2])
                                    index += 2
                                } else if (index + 4 < codes.size && codes[index + 1] == 2) {
                                    backgroundColor = Color(
                                        codes[index + 2].coerceIn(0, 255),
                                        codes[index + 3].coerceIn(0, 255),
                                        codes[index + 4].coerceIn(0, 255)
                                    )
                                    index += 4
                                }
                            }
                        }
                        index++
                    }
                }
            }

            if (currentIndex < normalized.length) {
                appendStyledChunk(normalized.substring(currentIndex))
            }
        }
    }

    private fun parse256Color(colorIndex: Int): Color {
        return when (colorIndex) {
            in 0..7 -> ANSI_FOREGROUND[30 + colorIndex] ?: DEFAULT_TEXT_COLOR
            in 8..15 -> ANSI_FOREGROUND[90 + (colorIndex - 8)] ?: DEFAULT_TEXT_COLOR
            in 16..231 -> {
                val index = colorIndex - 16
                val r = (index / 36) * 51
                val g = ((index % 36) / 6) * 51
                val b = (index % 6) * 51
                Color(r, g, b)
            }
            in 232..255 -> {
                val gray = (colorIndex - 232) * 10 + 8
                Color(gray, gray, gray)
            }
            else -> DEFAULT_TEXT_COLOR
        }
    }
}
