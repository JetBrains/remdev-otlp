package com.jetbrains.otp.freeze

class StackTraceAbbreviator {
    companion object {
        private val ABBREVIATABLE_PREFIXES = listOf(
            "java.", "javax.", "jdk.", "sun.",
            "com.intellij.", "com.jetbrains.", "org.jetbrains."
        )
    }

    fun abbreviateStackTraces(stackTraceText: String): String {
        return stackTraceText.lines().joinToString("\n") { line ->
            if (line.trimStart().startsWith("at ")) {
                val fullName = line.substringAfter("at ").substringBefore("(")
                val abbreviated = abbreviateFullyQualifiedName(fullName)
                if (abbreviated != null) {
                    line.replace(fullName, abbreviated)
                } else line
            } else {
                line
            }
        }
    }

    fun abbreviateFullyQualifiedName(fullName: String): String? {
        val className = if (fullName.contains('/')) {
            fullName.substringAfter('/')
        } else {
            fullName
        }

        if (ABBREVIATABLE_PREFIXES.none { className.startsWith(it) }) {
            return null
        }

        val parts = className.split('.')
        var classNameIndex = parts.size - 1
        for (i in parts.indices.reversed()) {
            if (parts[i].isNotEmpty() && parts[i][0].isUpperCase()) {
                classNameIndex = i
                break
            }
        }

        val result = StringBuilder()
        for (i in 0 until classNameIndex) {
            result.append(parts[i][0]).append('.')
        }

        for (i in classNameIndex until parts.size) {
            if (i > classNameIndex) {
                result.append('.')
            }
            result.append(parts[i])
        }

        return result.toString()
    }

    fun truncateToMaxBytes(text: String, maxBytes: Int): String {
        val bytes = text.toByteArray(Charsets.UTF_8)
        if (bytes.size <= maxBytes) {
            return text
        }

        val suffix = "\n... (truncated)"
        val suffixBytes = suffix.toByteArray(Charsets.UTF_8)
        val maxContentBytes = maxBytes - suffixBytes.size

        if (maxContentBytes <= 0) {
            return suffix.take(maxBytes.coerceAtLeast(0))
        }

        var truncatedBytes = bytes.copyOf(maxContentBytes)

        // Ensure we don't cut in the middle of a UTF-8 multi-byte character
        // UTF-8 continuation bytes have the pattern 10xxxxxx (0x80-0xBF)
        // We need to backtrack to find a complete character boundary
        while (truncatedBytes.isNotEmpty() && (truncatedBytes.last().toInt() and 0xC0) == 0x80) {
            truncatedBytes = truncatedBytes.copyOf(truncatedBytes.size - 1)
        }

        val truncatedText = truncatedBytes.toString(Charsets.UTF_8)
        return truncatedText + suffix
    }
}