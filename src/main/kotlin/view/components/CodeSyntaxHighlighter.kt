package view.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

internal enum class SyntaxTokenType { COMMENT, STRING, NUMBER, KEYWORD, PROPERTY, TAG }

internal data class SyntaxToken(
    val start: Int,
    val endExclusive: Int,
    val type: SyntaxTokenType
)

internal data class EditorSyntaxColors(
    val plain: Color,
    val comment: Color,
    val string: Color,
    val number: Color,
    val keyword: Color,
    val property: Color,
    val tag: Color
)

internal fun syntaxLanguage(fileName: String): String = when (fileName.substringAfterLast('.', "").lowercase()) {
    "kt", "kts" -> "Kotlin"
    "java" -> "Java"
    "js", "jsx", "ts", "tsx" -> "JavaScript"
    "json" -> "JSON"
    "xml", "html", "htm" -> "Markup"
    "css", "scss" -> "CSS"
    "py" -> "Python"
    "sh", "bash", "zsh" -> "Shell"
    "yaml", "yml" -> "YAML"
    "sql" -> "SQL"
    "md", "markdown" -> "Markdown"
    "properties", "ini", "conf", "cfg", "toml", "gradle" -> "Config"
    else -> "Plain text"
}

internal fun syntaxTokens(text: String, fileName: String): List<SyntaxToken> {
    if (text.isEmpty() || text.length > MAX_HIGHLIGHT_LENGTH) return emptyList()
    val language = syntaxLanguage(fileName)
    if (language == "Plain text") return emptyList()

    val occupied = BooleanArray(text.length)
    val tokens = mutableListOf<SyntaxToken>()

    fun add(regex: Regex, type: SyntaxTokenType, group: Int = 0) {
        regex.findAll(text).forEach { match ->
            val range = match.groups[group]?.range ?: return@forEach
            if (range.first < 0 || range.last >= text.length || (range.first..range.last).any { occupied[it] }) {
                return@forEach
            }
            (range.first..range.last).forEach { occupied[it] = true }
            tokens += SyntaxToken(range.first, range.last + 1, type)
        }
    }

    when (language) {
        "Markup" -> {
            add(Regex("<!--[\\s\\S]*?-->"), SyntaxTokenType.COMMENT)
            add(Regex("</?[A-Za-z][^>]*?>"), SyntaxTokenType.TAG)
            add(Regex("(['\"])(?:\\\\.|(?!\\1).)*?\\1"), SyntaxTokenType.STRING)
        }
        "Markdown" -> {
            add(Regex("(?m)^\\s{0,3}#{1,6}\\s+.*$"), SyntaxTokenType.TAG)
            add(Regex("`{1,3}[^`]*`{1,3}"), SyntaxTokenType.STRING)
            add(Regex("(?m)^\\s*>.*$"), SyntaxTokenType.COMMENT)
            add(Regex("\\[[^]]+]\\([^)]+\\)"), SyntaxTokenType.PROPERTY)
        }
        else -> {
            if (language in setOf("Kotlin", "Java", "JavaScript", "CSS", "SQL", "Config")) {
                add(Regex("/\\*[\\s\\S]*?\\*/|(?m)//.*$"), SyntaxTokenType.COMMENT)
            }
            if (language in setOf("Python", "Shell", "YAML", "Config")) {
                add(Regex("(?m)(?<!\\\\)#.*$"), SyntaxTokenType.COMMENT)
            }
            if (language == "SQL") add(Regex("(?m)--.*$"), SyntaxTokenType.COMMENT)
            if (language in setOf("JSON", "YAML", "Config")) {
                add(Regex("(?m)([A-Za-z_][\\w.-]*|\"(?:\\\\.|[^\"\\\\])*\")(?=\\s*[:=])"), SyntaxTokenType.PROPERTY)
            }
            add(Regex("\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'"), SyntaxTokenType.STRING)
            add(Regex("(?<![\\w.])(?:0[xX][0-9a-fA-F]+|\\d+(?:\\.\\d+)?)(?![\\w.])"), SyntaxTokenType.NUMBER)
            val keywords = keywordsFor(language)
            if (keywords.isNotEmpty()) {
                add(Regex("\\b(?:${keywords.joinToString("|") { Regex.escape(it) }})\\b", RegexOption.IGNORE_CASE), SyntaxTokenType.KEYWORD)
            }
        }
    }
    return tokens.sortedBy { it.start }
}

internal fun highlightedText(
    text: String,
    fileName: String,
    colors: EditorSyntaxColors
): AnnotatedString {
    val tokens = syntaxTokens(text, fileName)
    if (tokens.isEmpty()) return AnnotatedString(text)
    return buildAnnotatedString {
        var cursor = 0
        tokens.forEach { token ->
            if (cursor < token.start) append(text.substring(cursor, token.start))
            val style = when (token.type) {
                SyntaxTokenType.COMMENT -> SpanStyle(colors.comment, fontStyle = FontStyle.Italic)
                SyntaxTokenType.STRING -> SpanStyle(colors.string)
                SyntaxTokenType.NUMBER -> SpanStyle(colors.number)
                SyntaxTokenType.KEYWORD -> SpanStyle(colors.keyword, fontWeight = FontWeight.SemiBold)
                SyntaxTokenType.PROPERTY -> SpanStyle(colors.property)
                SyntaxTokenType.TAG -> SpanStyle(colors.tag, fontWeight = FontWeight.SemiBold)
            }
            withStyle(style) { append(text.substring(token.start, token.endExclusive)) }
            cursor = token.endExclusive
        }
        if (cursor < text.length) append(text.substring(cursor))
    }
}

private fun keywordsFor(language: String): Set<String> = when (language) {
    "Kotlin" -> setOf("as", "break", "class", "continue", "data", "do", "else", "false", "for", "fun", "if", "in", "interface", "is", "null", "object", "package", "private", "protected", "public", "return", "sealed", "suspend", "this", "throw", "true", "try", "typealias", "val", "var", "when", "while")
    "Java" -> setOf("abstract", "boolean", "break", "case", "catch", "class", "continue", "default", "do", "else", "enum", "extends", "false", "final", "finally", "for", "if", "implements", "import", "instanceof", "interface", "new", "null", "package", "private", "protected", "public", "return", "static", "super", "switch", "this", "throw", "throws", "true", "try", "void", "while")
    "JavaScript" -> setOf("async", "await", "break", "case", "catch", "class", "const", "continue", "debugger", "default", "delete", "do", "else", "export", "extends", "false", "finally", "for", "from", "function", "if", "import", "in", "instanceof", "let", "new", "null", "of", "return", "static", "super", "switch", "this", "throw", "true", "try", "typeof", "undefined", "var", "while", "yield")
    "Python" -> setOf("and", "as", "assert", "async", "await", "break", "class", "continue", "def", "del", "elif", "else", "except", "False", "finally", "for", "from", "global", "if", "import", "in", "is", "lambda", "None", "nonlocal", "not", "or", "pass", "raise", "return", "True", "try", "while", "with", "yield")
    "SQL" -> setOf("alter", "and", "as", "asc", "begin", "by", "case", "create", "delete", "desc", "distinct", "drop", "else", "end", "from", "group", "having", "in", "index", "insert", "into", "is", "join", "limit", "not", "null", "on", "or", "order", "select", "set", "table", "then", "union", "update", "values", "when", "where")
    "JSON" -> setOf("true", "false", "null")
    "Shell" -> setOf("case", "do", "done", "elif", "else", "esac", "export", "fi", "for", "function", "if", "in", "local", "return", "then", "while")
    else -> emptySet()
}

private const val MAX_HIGHLIGHT_LENGTH = 500_000
