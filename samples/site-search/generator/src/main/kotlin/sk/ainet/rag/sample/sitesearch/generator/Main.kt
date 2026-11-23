package sk.ainet.rag.sample.sitesearch.generator

import sk.ainet.rag.core.document.DocumentStatus
import sk.ainet.rag.core.document.FrontMatterParser
import sk.ainet.rag.core.document.Slugs
import java.io.File
import kotlin.system.exitProcess

/**
 * Renders the demo corpus into the static pages the search widget links to.
 *
 * Each `*.md` becomes `<out>/<sourcePath minus .md>.html` — the same path rule the bundler's
 * `--url-base` applies — and every `##` heading becomes `<h2 id="...">` with its id from
 * [Slugs], the rule the chunker used for anchors. One slug implementation on both sides is
 * what makes every result link scroll to the right section by construction.
 *
 * The corpus is headings and paragraphs, so the renderer handles exactly that: `##`/`###`
 * headings and blank-line-separated paragraphs, HTML-escaped. It is a demo-site generator,
 * not a Markdown engine.
 */
fun main(args: Array<String>) {
    fun value(name: String): String? =
        args.indexOf(name).takeIf { it >= 0 && it + 1 < args.size }?.let { args[it + 1] }

    val docsDir = value("--docs")?.let(::File)
    val outDir = value("--out")?.let(::File)
    if (docsDir == null || outDir == null) {
        System.err.println("usage: generator --docs <dir> --out <dir>")
        exitProcess(2)
    }
    if (!docsDir.isDirectory) fail("docs directory not found: $docsDir")

    val sources = docsDir.walkTopDown()
        .filter { it.isFile && it.extension == "md" }
        .sortedBy { it.relativeTo(docsDir).path }
        .toList()
    if (sources.isEmpty()) fail("no .md documents under $docsDir")

    var written = 0
    for (file in sources) {
        val relative = file.relativeTo(docsDir).path
        val result = FrontMatterParser.parse(relative, file.readText())
        result.issues.forEach { System.err.println(it) }
        val document = result.document ?: fail("unparseable document: $relative")
        if (document.status != DocumentStatus.PUBLISHED) continue

        val target = File(outDir, relative.removeSuffix(".md") + ".html")
        target.parentFile?.mkdirs()
        target.writeText(renderPage(document.title, document.body))
        written++
    }
    println("rendered $written pages into $outDir")
}

private fun renderPage(title: String, body: String): String = buildString {
    append(
        """
        <!doctype html>
        <html lang="en">
        <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>${escape(title)}</title>
        <style>
          :root { --bg: #ffffff; --fg: #1c2733; --muted: #5b6b7b; --accent: #0969da; --line: #d8dee4; }
          @media (prefers-color-scheme: dark) {
            :root { --bg: #0d1117; --fg: #e6edf3; --muted: #9ba7b4; --accent: #58a6ff; --line: #30363d; }
          }
          body { background: var(--bg); color: var(--fg); font: 16px/1.6 system-ui, sans-serif;
                 max-width: 44rem; margin: 0 auto; padding: 1rem 16px 4rem; }
          a { color: var(--accent); }
          h1 { font-size: 1.6rem; } h2 { font-size: 1.2rem; margin-top: 2rem; }
          h2[id] { scroll-margin-top: .75rem; border-bottom: 1px solid var(--line); padding-bottom: .25rem; }
          h2[id]:target { color: var(--accent); }
          .home { color: var(--muted); font-size: .85rem; text-decoration: none; }
        </style>
        </head>
        <body>
        <p><a class="home" href="../">&#8592; Search these docs</a></p>
        """.trimIndent(),
    )
    append("\n<h1>").append(escape(title)).append("</h1>\n")

    val paragraph = StringBuilder()
    fun flush() {
        val text = paragraph.toString().trim()
        paragraph.setLength(0)
        if (text.isNotEmpty()) append("<p>").append(escape(text)).append("</p>\n")
    }
    for (line in body.lines()) {
        when {
            line.startsWith("## ") -> {
                flush()
                val heading = line.removePrefix("## ").trim()
                append("<h2 id=\"").append(Slugs.of(heading)).append("\">")
                append(escape(heading)).append("</h2>\n")
            }

            line.startsWith("### ") -> {
                flush()
                append("<h3>").append(escape(line.removePrefix("### ").trim())).append("</h3>\n")
            }

            line.isBlank() -> flush()
            else -> paragraph.appendLine(line)
        }
    }
    flush()
    append("</body>\n</html>\n")
}

private fun escape(text: String): String = buildString(text.length) {
    for (c in text) {
        when (c) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            else -> append(c)
        }
    }
}

private fun fail(message: String): Nothing {
    System.err.println("site-search generator: $message")
    exitProcess(1)
}
