# SKaiNET-rag

The RAG layer of the SKaiNET ecosystem: a general skeleton for **local RAG apps in pure Kotlin
Multiplatform**, built on [SKaiNET](https://github.com/SKaiNET-developers/SKaiNET). No Python,
no vendor inference SDK: the same compiled Kotlin indexes the corpus at build time and answers
queries on the device, so build/runtime embedding parity holds by construction. Models and
embedders arrive as [SKaiNET cartridges](https://github.com/SKaiNET-developers/SKaiNET-cartridge)
built from open blueprints.

The repository keeps one structural rule: **`rag-core` never gains a model dependency** — it is
pure retrieval behind the `Embedder`/`Generator` seams, and anything needing weights or an
inference runtime enters as a `rag-embedder-*`/`rag-generator-*` edge module consuming a
cartridge from Maven.

Targets: **jvm · iosArm64 · iosSimulatorArm64 · macosArm64** (Android consumes the jvm
artifacts; D8 dexes Java 21 bytecode).

## Modules

| Module | What it is |
|---|---|
| `rag-core` | commonMain retrieval stack: tokenizer, Markdown chunker, BM25 inverted index with a **binary sparse (CSR-style) on-disk codec**, brute-force vector index, reciprocal-rank fusion, the **LRB2 bundle container**, embedding-parity contract, output gate, and the `TOOL:`/`ANSWER:` agent loop behind `Embedder`/`Generator` seams. |
| `rag-embedder-skainet` | The deterministic hashed-random-projection embedder on SKaiNET tensors (`skainet-hrp-v1`, 256-d). The floor that makes hybrid retrieval testable, and the weight-free default every target has. |
| `rag-embedder-leaf` | The first **neural** embedder: MongoDB's `mdbr-leaf-ir` (768-d) arriving as the `embedding-leaf-skainet` **cartridge from `~/.m2`** — the first model this skeleton consumes exactly the way the cartridge story says models are distributed. jvm + macosArm64 (tracking the cartridge's targets; iOS waits on upstream klibs). Needs the pinned model snapshot on disk; without it the bundle degrades to BM25 with a logged reason. |
| `rag-runtime` | The `LocalRag` facade: bundle bytes in, passages out. The bundle manifest's model id selects one of the configured `EmbedderProvider`s (HRP by default; hosts list the cartridge providers they compiled in), then the parity check guards the space; any mismatch degrades loudly to BM25-only, which is a supported state. The `Generator` seam is where a generator cartridge (llm/v1) plugs in. |
| `rag-bundler` | JVM CLI: Markdown docs in, one `.lrb` bundle out, embeddings computed **in-process** through the same embedder the runtime uses (`--embedder hrp\|leaf`). |
| `samples/corpus-java` | The demo-corpus generator: vendored [Java Version Almanac](https://javaalmanac.io) JSON (LTS releases 8–25, CC BY-SA 4.0) in, Markdown documents out — facts from the data, prose from templates in this repo, deterministic for a given snapshot. Output is checked in under `samples/docs-java`. |
| `samples/cli` | One common-source CLI compiled both as a JVM app and a **native macosArm64 executable**, answering from the same bundle. |
| `samples/app` | The **Compose Multiplatform** sample: the entire UI (search screen, status, passage cards) is one `commonMain` `App()` composable shared by Android, desktop and iOS; the bundle ships as a compose resource so every target loads identical bytes. `samples/app-android` is the thin `com.android.application` shell AGP 9 requires (KMP module + application plugin cannot share a module). |

## Quick start

```bash
./gradlew build                      # compiles + tests every target (incl. iOS sim, macOS)
./gradlew :samples:corpus-java:run --args="--data $PWD/samples/corpus-java/data --out $PWD/samples/docs-java"
./gradlew :rag-bundler:run --args="--docs $PWD/samples/docs-java --out $PWD/build/demo/docs.lrb --content-version 1 --categories release,jvm,language,api,tools,internal"
./gradlew :samples:cli:jvmRun --args="$PWD/build/demo/docs.lrb 'which JEP introduced virtual threads'"
./samples/cli/build/bin/macosArm64/releaseExecutable/cli.kexe build/demo/docs.lrb "class file version of Java 17"
```

The same corpus with **neural vectors** instead of the hashed projection — publish the
`embedding-leaf-skainet` cartridge to `~/.m2` (in the blueprints repo:
`./gradlew :blueprints:embedding-leaf-skainet:publishToMavenLocal`, plus its fetch step for the
model snapshot), then:

```bash
./gradlew :rag-bundler:run --args="--docs $PWD/samples/docs-java --out $PWD/build/demo/docs-leaf.lrb --content-version 1 --categories release,jvm,language,api,tools,internal --embedder leaf"
./gradlew :samples:cli:jvmRun --args="$PWD/build/demo/docs-leaf.lrb 'which JEP introduced virtual threads'"
```

The CLI has both providers compiled in; the bundle decides which one runs.

## Bundle format (LRB2)

Flat section table: `magic "LRB2"`, section count, then `(name, u32 length, payload)` triples.

- `manifest.json` — versions, counts, BM25 params, and the embedding parity block (omitted
  entirely for a BM25-only bundle; an explicit null would invite a reader to think one was
  intended).
- `chunks.json` — the displayed/stored chunks.
- `bm25.bin` — the inverted index, CSR in spirit: sorted term dictionary, delta+varint chunk
  ids per posting, exact f32 weighted frequencies. No zeros stored, no JSON parse on load,
  ranking bit-identical to the index the build produced.
- `vectors.bin` — row-major little-endian f32, `chunkCount × dims`, pre-normalized.
- `clusters.json` — precomputed Q&A, matched by BM25 so it works with no model present.

Encoding is deterministic: identical content → identical bytes.

## Cartridge direction

The `Embedder` and `Generator` seams are the app side of the SKaiNET cartridge contract:

- an **embedder cartridge** fills `Embedder` (`task: "embedding"`, `execution_mode: "eager"` on
  the SKaiNET backend — the path that works on iOS, where dlopen runtimes do not). The neural
  candidates come from SKaiNET-transformers, which already verifies real embedding models: the
  **BERT DSL path** (`llm-inference:bert`, commonMain) covers **LEAF (MongoDB/mdbr-leaf-ir/mt),
  BGE and E5-style** encoders with MEAN/CLS pooling, the sentence-transformers `2_Dense`
  projection, L2 normalization and retrieval prefixes — verified against sentence-transformers —
  and **T5/GTR** adds `GtrEmbedder`. Its `llm-api` module already defines a provider-neutral
  `EmbeddingModel` SPI (`embedQuery`/`embedDocument` asymmetry built in) that our `Embedder`
  seam adapts to in a few lines; decoder families (SmolLM2, Gemma, Qwen, BitNet, Apertus) are
  generation candidates, not embedders;
- a **generator cartridge** fills `Generator` (ADR-011 `llm/v1`; the agent loop, grammar, and
  output gate live here, not in the cartridge);
- the tokenizer ships *inside* a model cartridge as an `artifacts/tokenizer` role, per the spec's
  self-containment rule.

Cartridges are consumed as Maven artifacts (today from `~/.m2`), never as nested source clones.

## Status

The **LEAF-on-BERT
embedder cartridge is wired in** (`rag-embedder-leaf`, consumed from `~/.m2`): the first neural
embedding path, end to end — bundler, runtime selection, JVM and native macOS CLI with
bit-identical scores. Planned next: the generator cartridge (ADR-011 `llm/v1`) behind the
`Generator` seam, iOS for the LEAF path once upstream publishes inference-bert klibs, an Xcode
shell project for `samples/app`'s iOS target, and the Gradle plugin wrapping `rag-bundler`.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) — including the layering rule reviews enforce — and the
[Code of Conduct](CODE_OF_CONDUCT.md) shared with the SKaiNET organization. Licensed under
[MIT](LICENSE).

## Acknowledgements

The architecture is inspired by and based on ideas from Ayushi Gupta's
[LocalRAG](https://github.com/droidyayu/LocalRAG) ("Keep It Local", NextAppCon 2026): the
embedding-parity contract, authored chunk boundaries, gated generation, and the two-keyword
agent grammar. The implementation here is original; provenance details in
[NOTICE.md](NOTICE.md). The demo corpus derives from the
[Java Version Almanac](https://javaalmanac.io) (CC BY-SA 4.0).
