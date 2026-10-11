package sk.ainet.rag.sample.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import sk.ainet.rag.LocalRag
import sk.ainet.rag.core.agent.LocalRagState
import sk.ainet.rag.core.agent.Passage
import sk.ainet.rag.sample.app.resources.Res

/**
 * The whole sample UI, shared by Android, desktop and iOS from this one commonMain function —
 * the same property the retrieval stack already has. The bundle rides along as a compose
 * resource, so every target loads identical bytes through one call.
 */
@Composable
fun App() {
    val rag = remember {
        LocalRag(
            LocalRag.Config(
                loadBundle = { Res.readBytes("files/docs.lrb") },
                appVersion = "1.0",
                topK = 4,
            ),
        )
    }
    LaunchedEffect(rag) { rag.initialize() }
    val state by rag.state.collectAsState()

    var query by remember { mutableStateOf("") }
    var passages by remember { mutableStateOf<List<Passage>>(emptyList()) }
    var searched by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun search() {
        if (query.isBlank()) return
        scope.launch {
            passages = rag.retrieveOnly(query)
            searched = true
        }
    }

    MaterialTheme {
        Scaffold { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("SKaiNET Local RAG", style = MaterialTheme.typography.titleLarge)
                StatusLine(state)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("Ask the documentation") },
                    )
                    Spacer(Modifier.width(12.dp))
                    Button(onClick = ::search, enabled = state is LocalRagState.Ready) {
                        Text("Search")
                    }
                }

                if (searched && passages.isEmpty()) {
                    Text("No passages found.", style = MaterialTheme.typography.bodyMedium)
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(passages, key = { it.chunkId }) { passage ->
                        PassageCard(passage)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusLine(state: LocalRagState) {
    when (state) {
        is LocalRagState.Idle, is LocalRagState.Loading -> Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(Modifier.width(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Loading bundle…", style = MaterialTheme.typography.bodySmall)
        }

        is LocalRagState.Ready -> Text(
            "${state.chunkCount} chunks · " +
                (if (state.usingVectors) "hybrid retrieval (BM25 + SKaiNET vectors)" else "BM25 only"),
            style = MaterialTheme.typography.bodySmall,
        )

        is LocalRagState.Failed -> Text(
            "Failed: ${state.reason}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun PassageCard(passage: Passage) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                listOfNotNull(passage.title, passage.heading).joinToString(" — "),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                "${passage.source} · score ${passage.score}",
                style = MaterialTheme.typography.labelSmall,
            )
            Text(passage.text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
