# Contribution Guide

SKaiNET-rag follows the contribution culture of the main
[SKaiNET](https://github.com/SKaiNET-developers/SKaiNET) repository: keep
ordinary fixes small, focused, and easy to review. This guide is adapted from
[SKaiNET's CONTRIBUTING.md](https://github.com/SKaiNET-developers/SKaiNET/blob/main/CONTRIBUTING.md);
where the two disagree on org-wide process, the main repository wins.

## Where this repository sits

SKaiNET-rag is the **application layer** of the SKaiNET ecosystem — retrieval,
bundling, and the agent loop — one level above model inference:

```
SKaiNET-rag  →  cartridges (Maven)  →  SKaiNET-transformers  →  SKaiNET core
```

The one structural rule every review enforces: **`rag-core` never gains a model
dependency.** It stays pure retrieval — tokenizer, chunker, BM25, vector index,
fusion, bundle codec, agent loop — behind the `Embedder`/`Generator` seams.
Anything that needs weights or an inference runtime enters as a
`rag-embedder-*` / `rag-generator-*` edge module consuming a
[SKaiNET cartridge](https://github.com/SKaiNET-developers/SKaiNET-cartridge)
as a Maven artifact, never as a nested source clone.

## Design records

SKaiNET uses the SKEEP process (SKaiNET Evolution and Enhancement Process) for
durable architectural decisions; see the main repository's
[contribution guide](https://github.com/SKaiNET-developers/SKaiNET/blob/main/CONTRIBUTING.md)
and the published
[contributor docs](https://skainet-developers.github.io/SKaiNET/skainet/contributing/getting-started.html).
For this repository, the same instinct applies at smaller scale: if a change
affects the public API, the bundle format (a new LRB section or version), a
seam contract, or the cartridge-consumption rules, **open an issue describing
the design before implementing the whole feature** — the issue is the durable
record a maintainer six months from now will want. Typos, obvious one-line
fixes, dependency bumps and test-only changes need no prior design record.

Bundle-format changes deserve special care: the LRB container is consumed by
every compiled target and encoding is deterministic by contract (identical
content → identical bytes). A format change needs a version bump, a reader for
the old version or an explicit migration note, and round-trip tests.

## Testing expectations

* `./gradlew build` compiles and tests **every target** (jvm, iOS simulator,
  macosArm64) — a change is not done when only jvmTest is green.
* Retrieval changes must keep the eval harness thresholds (hit-rate on the
  checked-in query set) and the cross-target golden-parity tests green:
  embedding and ranking results are asserted identical across backends.
* Tests that need a model snapshot on disk must **skip loudly** (print the
  reason) when it is absent, never fail and never silently pass.
* Model weights are never committed to this repository.

## General contribution expectations

* keep changes clear and well-scoped;
* prefer project-local patterns over new abstractions;
* add tests when behavior changes;
* update docs (including module READMEs) when user-facing behavior changes;
* keep generated or unrelated churn out of focused PRs.

If you are unsure whether a change needs a design issue first, open a small
draft and ask before implementing the whole feature.

## Code of Conduct

This project follows the [Contributor Covenant 3.0](CODE_OF_CONDUCT.md),
shared with the rest of the SKaiNET organization.
