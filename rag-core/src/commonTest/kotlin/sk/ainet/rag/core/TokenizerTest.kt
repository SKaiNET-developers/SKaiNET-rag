package sk.ainet.rag.core

import sk.ainet.rag.core.text.Tokenizer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TokenizerTest {

    @Test
    fun `folds case and splits on whitespace`() {
        assertEquals(listOf("virtual", "threads"), Tokenizer.tokenize("Virtual Threads"))
    }

    @Test
    fun `punctuation separates while ampersand and percent survive`() {
        assertEquals(
            listOf("records", "sealed", "100%", "f&m"),
            Tokenizer.tokenize("Records/sealed: 100%, (F&M)!"),
        )
    }

    @Test
    fun `stopwords vanish`() {
        assertEquals(listOf("jep", "final"), Tokenizer.tokenize("is the JEP final"))
    }

    @Test
    fun `nothing in nothing out`() {
        assertTrue(Tokenizer.tokenize("").isEmpty())
        assertTrue(Tokenizer.tokenize("--- ... ,,,").isEmpty())
    }

    @Test
    fun `a dotted version splits into its numbers`() {
        assertEquals(listOf("21", "0", "12", "update"), Tokenizer.tokenize("21.0.12 update"))
    }
}
