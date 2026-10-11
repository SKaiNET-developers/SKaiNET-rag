package sk.ainet.rag.core.retrieve

import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Admits one generation at a time, process-wide, with newest-wins semantics: claiming the slot
 * cancels whoever held it.
 *
 * Both halves of that are deliberate. The underlying engine conversation tolerates no concurrent
 * use, and when the user has already asked a second question, finishing the first one's answer is
 * wasted compute nobody will read. Owners release the slot on completion, so back-to-back calls
 * proceed without friction.
 */
class SingleFlight {
    private val guard = Mutex()
    private var owner: Job? = null

    /** For flow builders: the collector holds the slot for the lifetime of its emission. */
    suspend fun <T> FlowCollector<T>.collectSingle(block: suspend FlowCollector<T>.() -> Unit) {
        claim()
        try {
            block()
        } finally {
            release()
        }
    }

    /** For plain suspend calls: the slot is held until [block] returns or throws. */
    suspend fun <R> run(block: suspend () -> R): R {
        claim()
        try {
            return block()
        } finally {
            release()
        }
    }

    private suspend fun claim() {
        val me = currentCoroutineContext()[Job]
        guard.withLock {
            owner?.takeIf { it !== me }?.cancel()
            owner = me
        }
    }

    private suspend fun release() {
        val me = currentCoroutineContext()[Job]
        guard.withLock {
            if (owner === me) owner = null
        }
    }
}
