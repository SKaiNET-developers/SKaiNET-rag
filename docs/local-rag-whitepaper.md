# Local RAG: Retrieval-Augmented Generation Without the Round Trip

**A technical assessment of fully on-device retrieval-augmented question answering**

*skainet-localrag project, October 2026*

---

## Abstract

Retrieval-augmented generation (RAG) is usually presented as a cloud architecture: a hosted
vector database, a hosted embedding endpoint, and a hosted frontier model. This paper examines
the opposite corner of the design space — RAG in which the index, the embedder, and the
generator all live inside a consumer application and no byte of user data leaves the device. We
argue that for a specific but large class of workloads (closed-corpus product documentation,
help-center QA, policy lookup), local RAG is no longer a compromise but the rational default:
the corpus is small, the queries are predictable, the latency and cost structure is strictly
better, and the privacy analysis collapses from a data-protection project to a non-event. We
describe the architecture of a complete implementation in Kotlin Multiplatform on SKaiNET
tensors, including the on-disk representation of sparse and dense retrieval structures, context
budgeting under positional-attention constraints, output gating for small generators, and the
operational cadence of shipping indexes, apps, and model weights at three different speeds. We
report our own microbenchmarks (Pixel 8a: 249 µs median query embedding; ~19 ms to re-embed a
53-chunk corpus) and give an honest account of where the approach breaks down.

---

## 1. The data tax of cloud question answering

Consider the baseline every product team reaches for first: forward the user's question, plus
some context, to a hosted large language model. For a regulated business this creates three
recurring costs that have nothing to do with model quality; A. Gupta's NextAppCon 2026 talk
"Keep It Local" frames them crisply from the perspective of a fintech operating seven regulated
entities [1]:

1. **A new data flow.** The user's question — which may embed account state, KYC status, or
   order details — now transits to a third-party model vendor. Every regulated entity must
   review that flow; every vendor change re-opens the review. The engineering is trivial; the
   compliance surface is not.
2. **A new bill, for answers you already own.** The answers to "why is my withdrawal pending?"
   are already written in the product's own help corpus. Cloud QA charges per token to
   paraphrase documentation the company authored, and the marginal cost per answer never
   reaches zero.
3. **Correlated demand.** Help questions spike exactly when the system is under load — when
   markets open, when a feature breaks, when a policy changes. Provisioned cloud QA must be
   sized for the spike. On-device QA inverts this: *every user brings their own compute*, so
   demand and capacity are correlated by construction.

Call the sum of these the **data tax**: the recurring privacy, cost, and capacity burden of
routing owned knowledge through rented inference. Local RAG is the refusal to pay it.

## 2. Why now: the brain fits in a pocket

Local RAG was not a serious option two years ago; the generator was the blocker. Two numbers
from current-generation small models change the calculus (figures as presented in [1], sourced
there from the litert-community model cards):

- **Gemma 4 E4B is a 3.7 GB single file** — a text decoder and its embeddings, the whole
  "brain" — and scores **69.4 % on MMLU-Pro**, above the previous generation's 27-billion
  parameter Gemma 3 27B at **67.6 %**. A phone-class model now outperforms last year's
  workstation-class model on a graduate-level benchmark.
- **Generation runs at ~52 tokens/s** (Gemma 4 E2B on a flagship phone GPU), with a first
  token in ~0.3 s — comfortably inside conversational latency, with **zero bytes sent
  anywhere**: no API key, no round trip, nothing to bill.

The retrieval side was never the blocker. BM25 has been a solved, CPU-cheap problem since the
1990s, and the corpora in scope here — hundreds of documentation sections, not the open web —
fit in kilobytes of index. The asymmetry matters: a local RAG system can ship retrieval
*today* on every device, and treat the generator as a progressively-enhanced layer gated on
device capability.

## 3. The design space

### 3.1 Prompting, fine-tuning, or RAG

Three ways exist to give a model proprietary knowledge, and they differ most sharply in their
*cost of change* [1]:

| | Changes | The catch | New knowledge costs |
|---|---|---|---|
| **Prompting** | How the model answers (tone, format, rules) | The help corpus does not fit in context; every token costs memory and prefill time | Doesn't fit |
| **Fine-tuning** | What the model remembers | Stale on the next policy change; cannot cite its source | Shipping gigabytes of new weights |
| **RAG** | What the model *reads, at question time* | Three subsystems to get right | Rebuilding an index: **kilobytes** |

For a corpus that changes weekly (fees, policies, UI flows), the change-cost column decides the
argument. RAG turns knowledge updates into a data deployment rather than a model deployment.

### 3.2 The retrieval engine ladder

Retrieval is a contract, not an engine: *question in, top-k passages out*. Behind that contract
sits a ladder of engines in increasing semantic power and operational weight [1]:

1. **Key-value lookup** — exact IDs, error codes, SKUs. Fastest; no ranking.
2. **SQLite FTS** — already inside every mobile app via the platform database; keyword search
   for free, but tokenizer behavior is tied to the SQLite build on the device.
3. **BM25 over a prebuilt inverted index** — ranked keyword search, milliseconds, fully
   deterministic, explainable, no model file. A sensible default.
4. **Dense embeddings** — search by meaning with a local encoder; required for paraphrase
   robustness ("cash out" → *Withdraw funds*), at the price of shipping and versioning an
   embedding model.

A hybrid of (3) and (4), fused by rank, captures most of the value of each; see §4.3.

### 3.3 Where the index lives

A hosted vector database buys elastic scale and pays for it with a network hop per search,
per-query and per-GB billing, and the return of the data-flow review (the documents now sit on
someone else's servers). An index inside the application package inverts every term: a search
is **a function call, ~5 ms, free, offline** [1]. The scale ceiling this accepts is real but
irrelevant at help-corpus size: brute-force scoring of a few hundred chunks is microseconds of
arithmetic; our own measurements in §7 put the entire embed-plus-scan path well under a
millisecond.

## 4. Anatomy of a local RAG

### 4.1 Build-time indexing and the parity contract

The index is computed at build time by one command and shipped as a read-only artifact; the
phone only ever reads it. The pipeline is conventional — parse front matter (and fail the
build on a malformed document, so a broken doc never ships), chunk at headings with a token
cap, tokenize (lowercase, strip stop words), build the weighted BM25 index, optionally embed
each chunk, pack one bundle file.

The non-obvious part is the **parity contract**. Build-time and run-time are two executions of
what must be *the same text-processing function*: if the tokenizer, the chunker, or the
embedder on the build machine diverges from the one on the phone, nothing errors — queries are
simply mapped into a slightly different space than the one the index describes, and **search
just gets quietly worse** [1]. Two defenses exist, and they are complementary:

- a **manifest contract**: the bundle records the embedder identity (model id, dimensionality,
  normalization, prompt prefixes), and the runtime refuses the vectors — falling back to
  BM25-only, loudly — on any field mismatch;
- **parity by construction**: build-time and run-time text processing are literally the same
  compiled code, not two implementations of a spec. §7 describes how Kotlin Multiplatform
  makes this the path of least resistance rather than an engineering feat.

### 4.2 Sparse and dense representations on disk

A retrieval index holds two mathematically different objects, and each deserves its native
storage format.

**The sparse side.** BM25 scores a chunk *c* for query terms *q*:

```
score(c, q) = Σ_{t ∈ q} idf(t) · f(t,c)·(k₁+1) / ( f(t,c) + k₁·(1 − b + b·|c|/avg|c|) )
```

with `idf(t) = ln(1 + (N − n_t + 0.5)/(n_t + 0.5))`, k₁ = 1.2, b = 0.75, and f(t,c) a
*field-weighted* term frequency (aliases ×3, titles ×2, body ×1 — an alias is the cheapest
retrieval win there is). The term–chunk matrix behind this is extremely sparse: a term occurs
in a handful of chunks, not in all N. The inverted index — term → postings list of
(chunk, weight) pairs — *is* the sparse representation; no zeros are ever materialized. The
dense N-vector of scores exists only transiently, per query, as an accumulator.

What is easy to get wrong is the *serialization*. Dumping the postings map as JSON (the
obvious first implementation) spends most of its bytes on repeated keys, quoting, and decimal
text, and — worse for a phone — requires a full JSON parse at app start. Our implementation
replaces it with a CSR-spirited binary section: a sorted term dictionary, then one postings
run per term with **delta-encoded, varint-compressed chunk ids** and exact float32 weighted
frequencies, plus the chunk-length array and the k₁/b/avg-length header. The properties that
matter on-device:

- *no parse step* — decoding is a linear scan of primitives;
- *locality* — one term's postings are contiguous, so the layout is amenable to lazy or
  memory-mapped section reads as corpora grow;
- *bit-identical ranking* — weights stay exact f32, so the decoded index ranks exactly like
  the one the build produced, which keeps golden tests meaningful.

**The dense side.** Chunk embeddings are the opposite shape: a fully dense `N × d` float32
matrix, pre-normalized so cosine similarity degenerates to a dot product:

```
cos(u, v) = u·v   when ‖u‖ = ‖v‖ = 1
```

Its correct storage format is the trivial one — row-major little-endian f32, `4·N·d` bytes,
loaded as one block. At help-corpus scale (53 chunks × 256 dims ≈ 54 KB) brute-force scan is
microseconds and an ANN structure would add a dependency, a file format, and a failure mode to
save time nobody is losing. A floor on cosine score (ours: 0.03) is kept so that, in a small
corpus, chunks that merely *exist* do not enter the candidate list purely by occupying a rank.

**The container.** Both sections, plus chunk text and the manifest, live in one flat
section-table file (magic, section count, then name/length/payload triples). Encoding is
deterministic — identical content yields identical bytes — which makes bundle-level golden
tests and reproducible-build checks one `contentEquals` call.

### 4.3 Hybrid fusion

Lexical and semantic rankings are combined with reciprocal rank fusion:

```
RRF(c) = Σ_lists 1 / (k + rank_list(c)),   k = 60
```

RRF needs no score calibration between heterogeneous scorers, which is precisely the situation
(BM25 scores and cosines share no scale). One ordering subtlety carries a lesson: category and
app-version filtering must run *after* fusion, because a chunk removed early still occupied a
rank in one input list and would skew the fused order of the survivors.

### 4.4 Context budgeting

Retrieval earns its keep in what it lets you *not* send to the generator. The context for each
turn is small, ordered, and fresh [1]: fixed instructions first, then the top-4 passages
(best first, ≤ 4,000 characters), then a short recent-history window, then the question. Two
findings justify the discipline:

- Liu et al. show that language models use long contexts non-uniformly: accuracy is highest
  when the relevant document is at the beginning or end of the context and degrades — in the
  measured settings, below the no-documents baseline — when it sits in the middle [2]. Rank
  order must therefore be *preserved into the prompt*; retrieval's best passage belongs first,
  not wherever concatenation happens to put it.
- On-device, every token of context is re-read ("prefilled") on every round at CPU speeds.
  A small, stable prefix also maximizes any prefix-cache reuse across rounds.

### 4.5 Generation behind a gate

A 2–4 B parameter generator paraphrasing financial documentation will, occasionally, restate a
number wrongly — and a wrong fee shown once cannot be taken back. The architectural answer is
to treat generation as *untrusted* and gate it [1]:

- **Buffered, not streamed.** The whole answer is produced and checked before one character
  reaches the user. Streaming UX is sacrificed for retractability.
- **Figures are copied, never generated.** Every digit token in the answer must appear
  verbatim in the retrieved passages or tool observations (digits typed in the question are
  blanked first). A rounded figure fails; "about 1,200 dollars" against a source that says
  1,204.50 is a blocked answer, not a helpful one.
- **Guidance, not advice.** Advisory phrasing ("you should", "will rise", "guaranteed") is
  refused categorically; term-overlap with the sources is enforced so the model cannot wander
  off-corpus. Any rejection yields a fixed fallback — never a silent retry that launders the
  same hallucination through different words.

### 4.6 Tool calling without a function-calling API

Small open-weight models have no structured function-calling endpoint; the loop is a grammar
instead [1]. The model's first line either requests data (`TOOL: name | args`) or commits to
an answer (`ANSWER: …`). The host executes the tool, pastes the observation back into the
transcript, and iterates — bounded (six rounds), de-duplicated (a repeated identical call
forces an answer instead of burning another multi-second CPU prefill to learn nothing), and
judged (the final text passes the same output gate, with tool observations as the evidence
set). Anything off-grammar ends the turn honestly as "unresolved" rather than being repaired
by heuristics. Two keywords and a loop replace an API — and, importantly for the architecture,
the *grammar and the gate belong to the host library*, so swapping generators cannot relax
the safety properties.

## 5. Operational reality: three things ship at three speeds

A local RAG deployment is three artifacts with different weights and cadences [1]:

| What changes | How often | How it reaches the device | What it invalidates |
|---|---|---|---|
| Docs + index | Weekly | Signed bundle, kilobytes, no store release | The index; retrieval tests must re-pass |
| App + runtime | Your release cadence | Normal store update | Nothing |
| Model weights | Rarely, on purpose | Multi-GB download or store AI pack; *plan it like a launch* | Every answer; re-run answer evals |

Two failure modes deserve their reputation:

- **The one that bites: a new embedding model.** Every stored vector is now a point in a
  different space. Cosine similarity between spaces is meaningless — not degraded, meaningless.
  Index and embedder must ship *together*, atomically, which is exactly what the manifest
  contract enforces when someone inevitably forgets.
- **The quiet one: tokenizer or chunker drift.** Both sides must match. Nothing errors;
  recall just decays. This is the failure the parity-by-construction architecture (§7) is
  designed to make structurally impossible, and the chunker-version field in the manifest
  exists to catch the residual cases.

Bundle updates over the air additionally need the boring hygiene of any trusted artifact:
version monotonicity, signature verification, atomic swap.

## 6. An honest balance sheet

**Advantages.**

- *Privacy as a property, not a policy.* The question never leaves the process. The
  data-protection analysis of the QA feature becomes trivially short.
- *Zero marginal cost and anti-correlated capacity* (§1).
- *Latency floor.* Retrieval is a ~5 ms function call [1]; our measured query-embedding cost
  is a quarter of a millisecond (§7). No network variance, no cold regions, works in
  airplane mode.
- *Determinism and testability.* A frozen corpus, a deterministic index, and greedy decoding
  make golden tests and eval harnesses (hit-rate@k floors in CI) actually reproducible.
- *Knowledge updates are kilobytes* (§3.1).

**Disadvantages, stated plainly.**

- *Model-quality ceiling.* A 4 B-parameter generator is not a frontier model. Complex
  multi-hop reasoning, long-form synthesis, and graceful handling of adversarial phrasing are
  all worse; the gate converts some of that gap into refusals rather than errors, which is the
  right trade but still a cost.
- *Memory pressure.* Gigabyte-class weights on an 8 GB phone coexist badly with the camera
  and the OS. The system must unload models under memory pressure and keep degrading to
  BM25-only — a supported state, not an error state — and that degradation path must be
  tested, not assumed.
- *Battery and thermal budget.* Minutes of sustained CPU/GPU generation are user-visible in
  the battery screen. Retrieval is free; generation is not.
- *Evaluation is harder.* No server logs means no passive quality telemetry. Eval sets must
  be curated, run on-device in CI farms, and privacy-preserving feedback instrumented
  deliberately.
- *No server-side observability.* A bad answer in the field cannot be replayed from logs you
  do not have. Invest in deterministic replay (frozen bundle + greedy decoding gets you most
  of the way).
- *Update distribution is physics.* A model-weight fix reaches users at app-store and
  download speeds, not at server-deploy speeds. A prompt-injection or gate bug lives longer
  in the field.
- *Multilingual gaps.* Small models and small embedders trail frontier models furthest
  outside English; a corpus-language mismatch can silently route everything to BM25-quality
  answers.
- *The corpus is attack surface.* RAG grounds the model in whatever the index contains; a
  poisoned or sloppily-reviewed document becomes authoritative context. Local RAG narrows
  this to a supply-chain problem (only build-time content enters the index, and a broken doc
  fails the build), but documentation review is now part of the security boundary.

The honest summary: local RAG buys privacy, cost, latency, and determinism, and pays in model
headroom, operational tooling, and update agility. For closed-corpus product QA the exchange
rate is favorable; for open-domain research assistance it is not.

## 7. Parity by construction: one compiled stack, every platform

Everything in §4–§5 can be built with a Python indexer and a per-platform runtime; most
implementations are. The residual risk of that shape is the parity contract's blind spot: two
stacks implementing "the same" tokenizer and embedder, drifting. The skainet-localrag skeleton
removes the two-stack premise. The tokenizer, chunker, BM25 index and its binary codec, the
fusion, the embedder, the output gate, and the agent loop are one Kotlin Multiplatform
codebase compiled for JVM (build machines and Android), iosArm64, and macosArm64, with tensors
on SKaiNET, a Kotlin-native ML runtime. The build-time bundler runs the *same compiled
embedder in-process* that the phone runs at query time; a golden-vector test pins the
embedding space across platforms, and the cross-platform test suite (including the end-to-end
bundle round trip and an eval-harness hit-rate floor) runs on every target in CI.

Measured on our hardware (all numbers ours; Pixel 8a, Android 17, arm64; medians over 300
iterations after warmup; deterministic 256-d hashed-projection embedder):

- query embedding (38 chars): **249 µs** median (p90 484 µs);
- chunk embedding (369 chars): **362 µs** median;
- re-embedding the full 53-chunk demo corpus: **~19 ms** — index refresh costs less than two
  animation frames, which makes on-device incremental indexing (user notes, downloaded
  content) a realistic extension rather than a build-only story. On an Apple-silicon JVM the
  same code embeds a chunk in ~35 µs.

The embedder measured here is deliberately not a neural model: a deterministic hashed random
projection (signed term frequencies through pseudo-random ±1/√d rows) that makes hybrid
retrieval *testable* while sitting behind the same seam a neural encoder slots into. The
seams are the point. `Embedder` and `Generator` are narrow interfaces behind the parity
check, and model distribution is delegated to **cartridges** — signed, self-contained model
packages built from open blueprints (descriptor, manifest with per-artifact digests and
Ed25519 signatures, provenance) in the SKaiNET ecosystem [3]. An embedding cartridge fills
the `Embedder` seam (`task: "embedding"`, eager execution on the SKaiNET backend — the path
that also works on iOS, where dlopen'd runtime libraries are not an option); a generator
cartridge fills `Generator` under the proposed `llm/v1` profile, with the tool grammar and
the output gate remaining host-side so a cartridge swap cannot weaken them. The tokenizer
ships *inside* its model's cartridge as a first-class artifact role — tokenizer/model drift
(§5) is thereby a packaging impossibility rather than a discipline.

The result is a skeleton in which the classic silent failures of RAG deployments — twin-stack
drift, embedder/index version skew, tokenizer mismatch — are either compile-time
impossibilities or loud, tested fallbacks; and in which "run it on iOS" is a link target, not
a second engineering project.

## 8. Conclusion

The question "can product QA run entirely on the device?" has quietly become "why doesn't
it?". The generator cleared the quality bar in the current model generation; retrieval
cleared it decades ago. What remains genuinely hard is operational: parity between build and
run, the three-speed shipping cadence, gated generation, on-device evaluation. These are
engineering problems with known shapes, and a single-codebase, cartridge-fed architecture
removes the largest class of them by construction. For closed-corpus QA, the data tax is now
optional.

---

## References

[1] A. Gupta, "Keep It Local: RAG Without the Round Trip," next.app devCon 2026, Berlin.
Figures cited from the talk's slides, which source model statistics to litert-community model
cards (gemma-4-E4B-it / gemma-4-E2B-it; Samsung S26 Ultra GPU) and the Gemma 3 27B model card
(MMLU-Pro).

[2] N. F. Liu, K. Lin, J. Hewitt, A. Paranjape, M. Bevilacqua, F. Petroni, P. Liang, "Lost in
the Middle: How Language Models Use Long Contexts," *Transactions of the Association for
Computational Linguistics*, 2024.

[3] SKaiNET cartridge specification and blueprints,
github.com/SKaiNET-developers/SKaiNET-cartridge and
github.com/SKaiNET-developers/SKaiNET-cartridge-blueprints.

[4] This repository: measured latencies (`SkaiNetEmbedderBenchmark`, Pixel 8a, Android 17,
300 iterations after 50 warmup), the LRB2 container and binary sparse BM25 codec
(`rag-core/src/commonMain/kotlin/sk/ainet/rag/core/index/Bm25Binary.kt`), and the
cross-platform golden-vector parity tests.
