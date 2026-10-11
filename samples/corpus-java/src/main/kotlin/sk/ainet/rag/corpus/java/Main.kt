package sk.ainet.rag.corpus.java

import java.io.File
import kotlin.system.exitProcess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Generates the demo corpus from vendored Java Version Almanac data (javaalmanac.io,
 * CC BY-SA 4.0 — see data/SNAPSHOT.md).
 *
 * The JSON holds facts: titles, JEP/JSR numbers, dates, class-file versions. The sentences
 * around them are template text written here, so the generated Markdown carries this
 * repository's license. The output is deterministic for a given data snapshot and
 * `--last-reviewed` date: facts in, identical bytes out, same as the bundler downstream.
 */
fun main(args: Array<String>) {
    val options = Options.parse(args) ?: run {
        System.err.println(Options.USAGE)
        exitProcess(2)
    }

    val dataDir = File(options.data)
    if (!dataDir.isDirectory) fail("data directory not found: ${options.data}")

    val json = Json { ignoreUnknownKeys = true }
    val releases = LTS_VERSIONS.map { version ->
        val file = File(dataDir, "$version.json")
        if (!file.isFile) fail("missing vendored data file: ${file.path}")
        json.decodeFromString<Release>(file.readText())
    }

    val outDir = File(options.out)
    outDir.mkdirs()

    val documents = buildList {
        add("java-lts-overview.md" to renderOverview(releases, options.lastReviewed))
        for (release in releases) {
            add("java-${release.version}.md" to renderRelease(release, options.lastReviewed))
            for ((category, features) in release.featuresByCategory()) {
                val slug = category.slug
                add(
                    "java-${release.version}-$slug.md" to
                        renderFeatures(release, category, features, options.lastReviewed),
                )
            }
        }
    }

    for ((name, content) in documents) {
        File(outDir, name).writeText(content)
    }
    println("wrote ${documents.size} documents to ${outDir.path} (LTS versions: ${LTS_VERSIONS.joinToString()})")
}

private val LTS_VERSIONS = listOf("8", "11", "17", "21", "25")

private fun fail(message: String): Nothing {
    System.err.println("corpus-java: $message")
    exitProcess(1)
}

internal data class Options(
    val data: String,
    val out: String,
    val lastReviewed: String,
) {
    companion object {
        const val USAGE: String =
            "usage: corpus-java --data <dir> --out <dir> [--last-reviewed YYYY-MM-DD]"

        fun parse(args: Array<String>): Options? {
            fun value(name: String): String? =
                args.indexOf(name).takeIf { it >= 0 && it + 1 < args.size }?.let { args[it + 1] }
            return Options(
                data = value("--data") ?: return null,
                out = value("--out") ?: return null,
                lastReviewed = value("--last-reviewed") ?: java.time.LocalDate.now().toString(),
            )
        }
    }
}

// --- vendored data model (subset of data.javaalmanac.io/v1/jdk/versions/{n}) ---

@Serializable
internal data class Release(
    val version: String,
    val name: String,
    val ga: String,
    val eol: String? = null,
    val umbrellajsr: String? = null,
    val latestversion: String? = null,
    val status: String,
    val bytecode: String? = null,
    val unicode: String? = null,
    val scm: List<Scm> = emptyList(),
    val updates: List<Update> = emptyList(),
    val features: List<Feature> = emptyList(),
) {
    /** Grouped in the almanac's own category order; feature order inside a group is preserved. */
    fun featuresByCategory(): List<Pair<FeatureCategory, List<Feature>>> =
        FeatureCategory.entries.mapNotNull { category ->
            val matching = features.filter { it.category == category.id }
            if (matching.isEmpty()) null else category to matching
        }
}

@Serializable
internal data class Feature(
    val title: String,
    val category: String,
    val preview: Boolean = false,
    val incubator: Boolean = false,
    val revision: Int = 0,
    val refs: List<Ref> = emptyList(),
)

@Serializable
internal data class Ref(val type: String, val identifier: String)

@Serializable
internal data class Scm(val type: String, val url: String)

@Serializable
internal data class Update(val version: String, val tag: String? = null, val date: String? = null)

internal enum class FeatureCategory(val id: String, val slug: String, val phrase: String, val plural: String) {
    JVM("jvm", "jvm", "a JVM and runtime change", "JVM and runtime changes"),
    LANGUAGE("lang", "language", "a language change", "language changes"),
    API("api", "api", "an API change", "API changes"),
    TOOLS("tools", "tools", "a tooling change", "tooling changes"),
    INTERNAL("internal", "internal", "an internal change", "internal changes"),
}

// --- rendering ---

private fun frontMatter(id: String, title: String, category: String, aliases: List<String>, lastReviewed: String) =
    buildString {
        appendLine("---")
        appendLine("id: $id")
        appendLine("title: $title")
        appendLine("category: $category")
        if (aliases.isNotEmpty()) appendLine("aliases: [${aliases.joinToString(", ")}]")
        appendLine("last_reviewed: $lastReviewed")
        appendLine("status: published")
        appendLine("---")
        appendLine()
    }

private fun renderOverview(releases: List<Release>, lastReviewed: String) = buildString {
    append(
        frontMatter(
            id = "java-lts-overview",
            title = "Java LTS releases overview",
            category = "release",
            aliases = listOf("LTS", "long-term support", "class file version", "release dates"),
            lastReviewed = lastReviewed,
        ),
    )
    appendLine("This corpus covers the long-term support (LTS) releases of Java: ${releases.joinToString { it.name }}.")
    appendLine()

    appendLine("## Release timeline")
    for (release in releases) {
        appendLine("${release.name} reached general availability on ${release.ga}.")
    }
    appendLine()

    appendLine("## Class-file versions")
    for (release in releases) {
        val bytecode = release.bytecode ?: continue
        appendLine("Class files compiled for ${release.name} carry major version $bytecode.")
    }
    appendLine(
        "To find out which Java release a class file needs, read its major version: " +
            releases.mapNotNull { r -> r.bytecode?.let { "${it.substringBefore('.')} means ${r.name}" } }
                .joinToString(", ") + ".",
    )
    appendLine()

    appendLine("## Support windows")
    for (release in releases) {
        val eol = release.eol ?: continue
        appendLine("Support for ${release.name} is scheduled to end in $eol.")
    }
}

private fun renderRelease(release: Release, lastReviewed: String) = buildString {
    val name = release.name
    append(
        frontMatter(
            id = "java-${release.version}",
            title = name,
            category = "release",
            aliases = listOf("JDK ${release.version}", "Java SE ${release.version}"),
            lastReviewed = lastReviewed,
        ),
    )
    appendLine("Release facts for $name, a ${release.status} release of the Java platform.")
    appendLine()

    appendLine("## Release facts")
    append("$name reached general availability on ${release.ga}")
    if (release.status == "LTS") append(" as a long-term support (LTS) release")
    appendLine(".")
    release.eol?.let { appendLine("Its support window is scheduled to end in $it.") }
    release.umbrellajsr?.let { appendLine("The release is defined by umbrella JSR $it.") }
    appendLine()

    if (release.bytecode != null || release.unicode != null) {
        appendLine("## Class files and Unicode")
        release.bytecode?.let {
            appendLine("The compiler in $name emits class files with major version $it.")
        }
        release.unicode?.let { appendLine("$name bundles Unicode version $it.") }
        appendLine()
    }

    if (release.updates.isNotEmpty()) {
        appendLine("## Maintenance updates")
        val latest = release.updates.last()
        append("$name has received ${release.updates.size - 1} maintenance updates in this snapshot; ")
        append("the most recent is ${latest.version}")
        latest.date?.let { append(", released $it") }
        appendLine(".")
        release.scm.firstOrNull()?.let {
            appendLine("The reference implementation sources are maintained at ${it.url}.")
        }
    }
}

private fun renderFeatures(
    release: Release,
    category: FeatureCategory,
    features: List<Feature>,
    lastReviewed: String,
) = buildString {
    val name = release.name
    append(
        frontMatter(
            id = "java-${release.version}-${category.slug}",
            title = "$name ${category.plural}",
            category = category.slug,
            aliases = features.flatMap { feature -> feature.jepRefs() }.take(8),
            lastReviewed = lastReviewed,
        ),
    )
    appendLine("$name shipped ${features.size} ${category.plural}. Each section covers one of them.")
    appendLine()

    for (feature in features) {
        appendLine("## ${feature.title}")
        append("${feature.title} is ${category.phrase} in $name")
        val refs = feature.refs.mapNotNull { it.label() }
        if (refs.isNotEmpty()) append(", specified by ${refs.joinToString(" and ")}")
        appendLine(".")
        when {
            feature.incubator -> appendLine(
                "$name ships it as an incubating module: the API lives in the jdk.incubator " +
                    "namespace and is not yet part of the Java SE standard.",
            )
            feature.preview -> appendLine(
                "$name ships it as a preview feature: it must be enabled with --enable-preview " +
                    "and can still change before it becomes final.",
            )
            else -> appendLine("It shipped as a final, fully supported change in $name.")
        }
        appendLine()
    }
}

private fun Feature.jepRefs(): List<String> =
    refs.filter { it.type == "JEP" }.map { "JEP ${it.identifier}" }

private fun Ref.label(): String? = when (type) {
    "JEP" -> "JEP $identifier"
    "JSR" -> "JSR $identifier"
    "JDKIssue" -> "issue JDK-$identifier"
    else -> null // AlmanacFeature is an internal cross-link, not a public designation
}
