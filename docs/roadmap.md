# custom-se Roadmap

Release history, milestone outcomes, and planned work. Architecture context:
[`architecture.md`](architecture.md).

---

## 1. Vision

**custom-se** is a teaching-grade, single-node search engine that grew from an
in-memory inverted index into a **Lucene-backed keyword search server** with
**RAG Ask mode** over the same index. The roadmap prioritizes:

1. Correct, testable search fundamentals
2. Durable persistence and unified APIs
3. Grounded AI answers with pluggable local and cloud providers

Non-goals remain: distributed indexing, OAuth, external vector DB, model training.

---

## 2. Release timeline

```
v1.0 ──► v1.5 ──► v2.0 ──► v2.4.1 ──► v3.0.0
 │         │         │          │           │
 Jetty    harden   Lucene     Voyage      RAG hardening
 UI       config   BM25       + .env      timeouts, tests
```

![Evolution v1 to v3](img/evolution-v1-v3.png)

### v1.x — Web server on in-memory index

| Version | Highlights | Status |
| ------- | ---------- | ------ |
| **v1.0.0** | Jetty server, HTML search UI, CLI batch tools | ✅ Shipped |
| **v1.1.0** | Session tracking (history, visited, favorites), global metadata | ✅ Shipped |
| **v1.2.0–v1.5.0** | Lucene foundation phases (IndexStore, SearchEngine integration) | ✅ Shipped |

**Outcome:** Proved servlet UI and session model; exposed index persistence and
ranking limits.

### v2.x — Lucene keyword search

| Version | Highlights | Status |
| ------- | ---------- | ------ |
| **v2.0.0** | `LuceneIndexStore`, BM25, `-load-index`, unified `SearchEngine`, async crawl, CSRF, rate limits | ✅ Shipped |
| **v2.1.0** | Chunk model, chunk Lucene schema, `IndexAiMetadata` | ✅ Shipped |
| **v2.2.0** | `EmbeddingProvider`, vectors, `HybridRetriever`, RRF | ✅ Shipped |
| **v2.3.0** | `LlmClient`, `AiProfile`, `RagService`, `/settings/ai` | ✅ Shipped |
| **v2.4.0** | `/ask` UI, SSE, CLI `-ask`, re-embed job | ✅ Shipped |
| **v2.4.1** | Voyage embeddings for Claude; `.env` validation | ✅ Shipped |

**Outcome:** Durable whole-document search (v2.0) evolved into chunk + vector index
(v2.1–v2.2) and full RAG stack (v2.3–v2.4).

### v3.x — AI search product

| Version | Highlights | Status |
| ------- | ---------- | ------ |
| **v3.0.0-beta.1** | Feature-complete Ask mode, docs | ✅ Shipped |
| **v3.1.0** | `-benchmark` keyword search latency reporting | ✅ Shipped |
| **v3.0.0** | HTTP timeouts/retries, token budget trimming, contract tests | ✅ Shipped |

**Outcome:** Production-shaped single-node RAG search; keyword `/search` unchanged.

---

## 3. Phase completion map (implementation)

| Phase | Theme | Tag | Key deliverables |
| ----- | ----- | --- | ---------------- |
| P1 | Lucene foundation | v1.2.0 | IndexStore, LuceneIndexStore, schema |
| P2 | SearchEngine integration | v1.3.0 | Unified search, servlet wiring |
| P3 | Server/CLI completion | v1.4.0 | Pagination, warm start, export |
| P4 | Hardening | v1.5.0 | Config, async crawl, CSRF, limits |
| P5 | v2 release | v2.0.0 | BM25 persistence, documentation |
| P6 | Chunk index | v2.1.0 | Chunker, chunk schema, meta.json |
| P7 | Hybrid retrieval | v2.2.0 | Embeddings, KNN, RRF |
| P8 | RAG core | v2.3.0 | LLM clients, RagService |
| P9 | Ask UI + CLI | v2.4.0 | SSE, `/ask`, `-ask` |
| P10 | Config polish | v2.4.1 | Voyage, `.env` |
| P11 | v3 hardening | v3.0.0 | Timeouts, tests, README |

---

## 4. Current capabilities (v3.0.0)

| Capability | Route / entry | Notes |
| ---------- | ------------- | ----- |
| Keyword search | `/search`, `/api/search`, CLI `-query` | BM25, exact/partial |
| RAG Ask | `/ask`, `/ask/stream`, `/api/ask`, CLI `-ask` | Hybrid retrieve + LLM |
| Index persistence | `data/index`, `-load-index` | Lucene + meta.json |
| Crawl | `-html`, `POST /crawl` | Background jobs |
| AI stacks | `.env`, `/settings/ai` | ollama, openai, lmstudio, claude |
| Re-embed | `/admin/re-embed`, `-reindex-embeddings` | After model change |
| Sessions | cookies | history, favorites, private mode |

---

## 5. Near-term backlog (post-v3.0)

Priority order is suggestive; none are committed releases yet.

| Item | Motivation | Complexity |
| ---- | ---------- | ---------- |
| **Multi-turn chat history** | Follow-up questions in Ask UI | Medium — session storage + prompt assembly |
| **Persist AI preferences** | Survive server restart per user | Low — optional file or DB |
| **Weighted hybrid tuning UI** | Expose lexical vs vector balance | Low — extends `RetrievalOptions` |
| **`-Pai-integration` Maven profile** | CI optional live Ollama tests | Low |
| **Phrase search polish** | `QueryMode.PHRASE` UX in HTML | Low |
| **Highlighted snippets** | Lucene highlighter on `/search` | Medium |
| **v2 → v3 index migration tool** | Auto re-chunk whole-doc indexes | Medium — one-shot job |
| **Persist metadata store** | Survive restart for stats | Low — JSON beside index |

---

## 6. Medium-term ideas

| Item | Options | Trade-off |
| ---- | ------- | --------- |
| **Semantic cache** | Cache embed(query) + top-K | Faster repeat asks; stale if index changes |
| **Query routing** | Auto `/search` vs `/ask` | Better UX; risk of unnecessary LLM cost |
| **Citation highlighting** | Offset map in chunks | Better UI; more indexing metadata |
| **OpenTelemetry** | Trace retrieve + LLM latency | Ops visibility; new dependency |
| **Docker Compose stack** | Jetty + Ollama one command | Easier onboarding; maintenance |

---

## 7. Explicit non-goals

These are out of scope unless requirements change fundamentally:

- Elasticsearch / OpenSearch cluster
- Pinecone / Qdrant / dedicated vector DB
- User authentication / OAuth
- Fine-tuning or hosting custom models
- Real-time collaborative editing of index
- Multi-node sharding or replication
- Billing / quota per user beyond rate limits

---

## 8. Versioning policy

| Tag pattern | Meaning |
| ----------- | ------- |
| `vMAJOR.MINOR.PATCH` | Semver-style product releases |
| `v2.1.0`–`v2.4.x` | Incremental v3 feature milestones (keyword-first product) |
| `v3.0.0-beta.N` | Feature freeze, docs pass |
| `v3.0.0-rc.N` | Release candidate |
| `v3.0.0` | Stable AI search release |

Each phase tag requires `mvn test` green on `master`.

---

## 9. How to propose changes

1. Check [`architecture.md`](architecture.md) ADRs for prior decisions.
2. Add a row to §5 or §6 above with motivation and complexity.
3. For architectural shifts, add a new ADR section before implementation.
4. Update [`protocol.md`](protocol.md) if wire formats change.
5. Update [`runbook.md`](runbook.md) if operations change.

---

## 10. References

- [GitHub releases](https://github.com/LKPJohn2026/custom-se/releases)
- [`README.md`](../README.md) — quick start
- [`architecture.md`](architecture.md) — design and ADRs
