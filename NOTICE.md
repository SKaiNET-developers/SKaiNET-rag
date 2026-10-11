# Provenance and acknowledgements

This library's architecture is inspired by and based on ideas from
[LocalRAG](https://github.com/droidyayu/LocalRAG) by Ayushi Gupta, presented as the "Keep It
Local: RAG Without the Round Trip" sample at NextAppCon 2026 — in particular the build/runtime
embedding-parity contract, authored chunk boundaries, the gated-generation stance ("the model
never produces a number"), and the two-keyword tool-loop grammar. Thank you.

The code in this repository is an independent implementation of those ideas. Early private
revisions began as a port of the upstream sample; because that repository publishes no license,
every ported source file and the upstream demo corpus have since been rewritten or replaced with
original work before publication:

- `rag-core` sources are original implementations of the concepts above (verified against the
  upstream expression file by file during the rewrite).
- The demo and evaluation corpus is generated from [Java Version Almanac](https://javaalmanac.io)
  data by Marc R. Hoffmann and Cay S. Horstmann, licensed
  [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/) — attribution and snapshot
  details in `samples/corpus-java/data/SNAPSHOT.md`. Only the almanac's data files are used;
  its prose feature articles are all-rights-reserved and are not included.
- The evaluation query set, the LRB2 bundle container, the binary BM25 codec, the embedders, the
  runtime facade, the bundler, the corpus generator, the samples and the docs are original work
  in this repository.

Everything here is distributed under the MIT license in `LICENSE`.
