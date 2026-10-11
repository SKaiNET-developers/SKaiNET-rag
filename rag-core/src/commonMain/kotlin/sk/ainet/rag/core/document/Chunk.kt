package sk.ainet.rag.core.document

import kotlinx.serialization.Serializable

/**
 * The unit of retrieval: one section of one document, carrying everything the runtime needs to
 * rank it and show it.
 *
 * Two views of the text travel together. [embeddedText] — title and heading prepended, Markdown
 * stripped — is what the index and the embedder consume; a section body alone ("Tap Confirm")
 * carries too little context to retrieve well. [displayText] is the untouched section body, the
 * only thing a user should ever be shown.
 */
@Serializable
data class Chunk(
    val chunkId: String,
    val docId: String,
    val title: String,
    val heading: String?,
    val category: String,
    val aliases: List<String>,
    val screen: String?,
    val appVersionMin: String?,
    val appVersionMax: String?,
    val embeddedText: String,
    val displayText: String,
    val tokenCount: Int,
)
