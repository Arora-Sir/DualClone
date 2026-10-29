package com.mohitarora.dualclone.update

/**
 * Turns the Markdown of a GitHub release body into plain text for the update sheet.
 * The notes are written for a web page, so headings, bullets, backticks and links would otherwise show as raw symbols.
 * The function is pure, so a JVM test proves every rule.
 */
object ReleaseNotesText {

    private val heading = Regex("^\\s{0,3}#{1,6}\\s+")
    private val bullet = Regex("^\\s*[*+-]\\s+")
    private val link = Regex("\\[([^\\]]+)]\\(([^)]+)\\)")

    fun plain(markdown: String): String =
        markdown.lineSequence()
            .map { line ->
                var out = line.trimEnd()
                val isHeading = heading.containsMatchIn(out)
                out = heading.replace(out, "")
                out = if (bullet.containsMatchIn(out)) "• " + bullet.replace(out, "") else out
                out = link.replace(out) { it.groupValues[1] }
                out = out.replace("`", "").replace("**", "").replace("__", "")
                if (isHeading) out.uppercase() else out
            }
            .joinToString("\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
}
