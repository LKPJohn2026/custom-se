# custom-se v3.0 Architecture

This document defines the target architecture for **custom-se v3.0**: an
**AI search engine** built on the v2.0 Lucene-backed `IndexStore`, adding
**chunk-level hybrid retrieval** (BM25 + vector) and **RAG-style answers**
with pluggable **local (Ollama / LM Studio)** and **cloud (OpenAI / Claude)**
providers for embeddings and chat.

Use this as the authoritative reference before planning commits. All v3.0 work
should align with the boundaries, interfaces, and migration rules described
here.

---

## 1. Purpose

v2.0 delivers durable keyword search with BM25 ranking, unified orchestration via
`SearchEngine`, and a full web feature set. It answers **which documents match**
a query. v3.0 adds the ability to answer **what those sources say** in natural
language, with citations, while preserving classic `/search` behavior.

### Goals

| Goal | Rationale |
| ---- | --------- |
| **AI Ask mode** | Users ask questions and receive synthesized answers grounded in indexed content |
| **Hybrid retrieval** | Combine BM25 (lexical) and vector similarity (semantic) for better recall |
| **Pluggable AI stacks** | Support Ollama and LM Studio locally; OpenAI and Claude via API keys |
| **Independent embed + chat** | Switch chat provider without re-indexing; switch embedding provider with explicit re-embed |
| **Citations** | Every AI answer links back to source URLs and chunk excerpts |
| **Preserve v2.0 search** | `/search` and CLI keyword search remain unchanged in behavior |
| **CLI compatibility** | `Driver` flags from Project v5.0 remain supported |
| **Fail soft in UI** | No stack traces in user-visible output (existing rule) |

### Non-goals (v3.0)

- Distributed or multi-node indexing
- Replacing Jetty or servlet-based HTML UI
- User account system / OAuth
- Fine-tuning or training custom models
- Replacing Lucene with an external vector database
- Persisting API keys to disk (env and session only)
- Real-time collaborative chat / multi-turn conversation history (optional follow-up)

---

## 2. Current state (v2.0 baseline)

### Index and search

```
IndexStore (interface)
  └── LuceneIndexStore
        ├── addDocument(IndexDocument)   → one Lucene doc per file/URL
        ├── search(SearchQuery, SearchOptions) → BM25
        ├── listTerms / listLocations
        └── exportJson / exportYaml

SearchEngine
  ├── IndexStore
  ├── MetadataStore (optional)
  └── ServerStats (optional)
```

**Consumers today:**

| Component | Access |
| --------- | ------ |
| `Driver` | Builds or loads `IndexStore`; batch search via `SearchEngine` |
| `IndexBuilder` / `IndexOpener` | Lucene index at `data/index` |
| `SearchService` | Thin adapter over `SearchEngine` |
| `SearchHtmlServlet` / `SearchServlet` | `SearchEngine` |
| `WebCrawler` | `IndexStore` + `PageListener` |
| `ThreadFileIndexer` | Writes `IndexDocument` via `IndexStore` |
| `IndexBrowserServlet` / `LocationBrowserServlet` | Browse API on `IndexStore` |

v2.0 indexes **whole documents** (one Lucene document per file/URL). There are
no chunk fields, no vector fields, and no LLM integration.

### Server state

`AppContext` holds:

- `IndexStore index`
- `SearchEngine searchEngine`
- `MetadataStore metadata` (in-memory)
- `ServerStats stats`
- `ServerSettings settings` (`application.properties` + env)
- `CrawlJobManager crawlJobs`
- `RateLimiter searchRateLimiter`

Session data (`UserSessionData`) and global metadata remain **in-memory** in
v3.0 unless explicitly scoped into a later phase.

### Gaps for AI (v2.0 → v3.0)

| Gap | Impact |
| --- | ------ |
| Whole-document indexing | Context windows too large; poor RAG precision |
| No embeddings / vectors | No semantic retrieval for paraphrased queries |
| No provider abstraction | Cannot choose Ollama vs API |
| No streaming responses | LLM answers need SSE or chunked transfer |
| `SearchHit` has no chunk text | Cannot cite specific passages |

---

## 3. Target architecture overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│  CLI (Driver)              Server (Jetty + servlets)                    │
│       │                            │                                    │
│       ├──────── /search ───────────┼──► SearchEngine  (v2, unchanged)  │
│       │                            │         │                          │
│       └──────── /ask (new) ────────┼──► RagService  (v3 orchestration)  │
│                                    │         │                          │
│                                    │    ┌────┴────┐                     │
│                                    │    ▼         ▼                     │
│                                    │ HybridRetriever  LlmClient          │
│                                    │    │              ▲                │
│                                    │    ▼              │                │
│                                    │ IndexStore    AiProfileResolver     │
│                                    │    │              │                │
│                                    │    ▼              │                │
│                                    │ LuceneIndexStore  EmbeddingProvider │
│                                    │ (chunks + vectors)                  │
└─────────────────────────────────────────────────────────────────────────┘

InvertedIndex / ThreadSafeInvertedIndex  →  tests & teaching only (unchanged)
```

### Ask flow (high level)

```
User question
  → AiProfileResolver.resolve(session preferences, server defaults)
  → HybridRetriever.retrieve(query, EmbeddingProvider)
       ├── BM25 on chunk text (Lucene)
       └── KNN on vector field (Lucene)
       └── merge scores (RRF or weighted)
  → PromptBuilder (system + top chunks + question)
  → LlmClient.streamChat()
  → streamed answer + citation list
```

### Design principles

1. **RAG on top of Lucene** — v3 extends `IndexStore`, it does not replace it.
2. **Two capabilities, one profile** — `EmbeddingProvider` and `LlmClient` are
   separate interfaces composed into an `AiProfile`.
3. **Protocol behind interfaces** — servlets and `RagService` never call Ollama
   or OpenAI HTTP endpoints directly.
4. **SearchEngine unchanged** — keyword `/search` stays on the v2 code path.
5. **Index-coupled embeddings** — switching embedding model requires re-embed;
   switching chat model does not.
6. **Secrets via env** — API keys never committed; session overrides optional.
7. **Fail soft in UI** — provider errors show friendly messages, not stack traces.

---

## 4. Package layout (target)

```
src/main/java/com/cse/
  ai/
    profile/
      AiProfile.java              # bundles EmbeddingProvider + LlmClient
      AiProfileDescriptor.java    # UI metadata (name, capabilities)
      AiProfileResolver.java      # resolve stack from session + server config
      AiProfileFactory.java       # build profiles from AiSettings
      AiPreferences.java          # per-session stack selection
      AiCredentialSource.java     # env keys + optional session override
    embed/
      EmbeddingProvider.java      # core embedding contract
      EmbeddingRequest.java
      OllamaEmbeddingProvider.java
      OpenAiEmbeddingProvider.java
      OpenAiCompatibleEmbeddingProvider.java  # LM Studio, etc.
    llm/
      LlmClient.java              # core chat contract
      ChatRequest.java
      ChatMessage.java
      OllamaLlmClient.java
      OpenAiLlmClient.java
      OpenAiCompatibleLlmClient.java          # LM Studio
      AnthropicLlmClient.java                 # Claude
    chunk/
      Chunk.java                  # one indexable passage
      Chunker.java                # splits IndexDocument → List<Chunk>
      ChunkingOptions.java
    rag/
      HybridRetriever.java        # BM25 + vector merge
      ScoredChunk.java            # chunk + score + citation metadata
      RagService.java             # retrieve → prompt → stream answer
      PromptBuilder.java
      RagResponse.java            # answer text + citations + timing
      RetrievalOptions.java
    job/
      EmbeddingIndexJob.java      # background re-embed all chunks
      IndexAiMetadata.java        # embedding provider/model/dims stored in index
  index/
    IndexStore.java               # extended: addChunks, hybridSearch (or via retriever)
    ChunkDocument.java            # NEW — chunk DTO for indexing
    SearchHit.java                # unchanged for keyword search
    lucene/
      LuceneIndexStore.java       # chunk docs + KnnVectorField
      LuceneSchema.java           # + chunkId, parentId, vector fields
      LuceneHybridSearch.java     # NEW — lexical + KNN query builder
  search/
    SearchEngine.java             # unchanged
  server/
    AppContext.java               # + RagService, AiProfileResolver
    AiSettings.java               # NEW — extends config pattern of ServerSettings
    servlet/
      AskServlet.java             # NEW — HTML ask + streaming
      AskApiServlet.java          # NEW — JSON + SSE
      AiSettingsServlet.java      # NEW — stack/model selection
    session/
      UserSessionData.java        # + AiPreferences
  ... (cli, crawl, existing servlets — unchanged routes for v2 endpoints)
```

New AI code belongs in `com.cse.ai`. Lucene vector field details are **only**
allowed under `com.cse.index.lucene`. HTTP client code for providers lives under
`com.cse.ai.embed` and `com.cse.ai.llm`.

---

## 5. Core abstractions

### 5.1 `Chunk` and `Chunker`

A **chunk** is the unit of retrieval and citation for AI search.

```java
public record Chunk(
    String chunkId,       // stable id: parentId + sequence
    String parentId,      // IndexDocument.id (URL or file path)
    String location,      // citation link (same as parent location for v3.0)
    String title,         // parent document title
    String text,          // passage body (stored for citations + LLM context)
    int sequence,         // order within parent document
    int charOffset        // start offset in parent body (optional, for future)
) {}
```

```java
public interface Chunker {
    List<Chunk> chunk(IndexDocument doc, ChunkingOptions options);
}

public record ChunkingOptions(
    int targetChars,      // default ~2000 (~500 tokens heuristic)
    int overlapChars,     // default ~200
    int maxChunksPerDoc   // safety cap
) {}
```

**Indexing rule:** after `IndexDocument` is produced (file index or crawl),
`Chunker` splits it into `List<Chunk>`. Each chunk becomes one Lucene document.

### 5.2 `EmbeddingProvider`

Used at **index time** (embed chunks) and **query time** (embed user question).

```java
public interface EmbeddingProvider {
    String providerId();          // "ollama" | "openai" | "lmstudio"
    String model();
    int dimensions();             // fixed per model — drives Lucene vector field

    float[] embed(String text);
    List<float[]> embedBatch(List<String> texts);  // preferred for indexing
}
```

**Index coupling:** the Lucene index records which embedding model and dimension
were used (`IndexAiMetadata`). Query-time embedding **must match** index
metadata for hybrid vector search. Mismatch → degrade to BM25-only with a user
warning.

### 5.3 `LlmClient`

Used only at **query time** for answer synthesis.

```java
public interface LlmClient {
    String providerId();          // "ollama" | "openai" | "claude" | "lmstudio"
    String model();

    Stream<String> streamChat(ChatRequest request);
    String completeChat(ChatRequest request);
}

public record ChatRequest(
    List<ChatMessage> messages,
    double temperature,
    int maxTokens
) {}

public record ChatMessage(String role, String content) {}  // system | user | assistant
```

Chat provider changes are **not** index-coupled. Users may switch from Ollama to
Claude without re-indexing.

### 5.4 `AiProfile`

Bundles the active stack for a session.

```java
public record AiProfile(
    String id,                    // "ollama" | "openai" | "claude" | "lmstudio" | "custom"
    String displayName,
    EmbeddingProvider embeddings,
    LlmClient chat,
    boolean mixedStack            // true when embed and chat providers differ
) {}
```

**Built-in stacks:**

| Stack id | Embeddings | Chat | Notes |
| -------- | ---------- | ---- | ----- |
| `ollama` | Ollama native API | Ollama native API | Fully local default |
| `lmstudio` | OpenAI-compatible `/v1/embeddings` | OpenAI-compatible `/v1/chat/completions` | Local GUI workflow |
| `openai` | OpenAI API | OpenAI API | Requires `OPENAI_API_KEY` |
| `claude` | Configured separately* | Anthropic Messages API | Requires `ANTHROPIC_API_KEY` |

\*Claude has no embedding API. The `claude` stack uses a configured embedding
backend (`ai.claude.embeddingStack`, default `openai` or `ollama`). The settings
UI must make this explicit.

### 5.5 `AiProfileResolver`

```java
public interface AiProfileResolver {
    AiProfile resolve(AiPreferences sessionPrefs);
    List<AiProfileDescriptor> availableProfiles();
    boolean testEmbeddings(AiProfile profile);
    boolean testChat(AiProfile profile);
}
```

**Resolution order:**

1. Session `AiPreferences` (from `/settings/ai`)
2. Server default (`ai.defaultStack` in config)
3. Fallback to first available profile with valid credentials

Optional request override `?stack=ollama` is restricted to admin/debug use.

### 5.6 `AiPreferences` (session)

```java
public record AiPreferences(
    String stackId,               // built-in or "custom"
    String embeddingProvider,     // optional override
    String chatProvider,          // optional override
    String embeddingModel,
    String chatModel,
    String apiKeySource           // "env" | session-stored (never logged)
) {}
```

Stored in `UserSessionData`. API keys in session are optional for power users;
server env vars are the recommended default for cloud providers.

### 5.7 `HybridRetriever`

```java
public interface HybridRetriever {
    List<ScoredChunk> retrieve(String query, RetrievalOptions options, EmbeddingProvider embedder);
}

public record ScoredChunk(
    Chunk chunk,
    double score,
    double lexicalScore,
    double vectorScore
) {}

public record RetrievalOptions(
    int topK,             // default 8
    double lexicalWeight, // default 0.5
    double vectorWeight,  // default 0.5
    boolean vectorEnabled // false when index metadata mismatch
) {}
```

**Merge strategy (v3.0):** Reciprocal Rank Fusion (RRF) across BM25 and KNN
result lists. Tunable weights are an optional follow-up.

`HybridRetriever` delegates lexical search to `LuceneIndexStore` (or an internal
`LuceneHybridSearch` helper). It does **not** replace `SearchEngine`.

### 5.8 `RagService`

Unified orchestration for Ask mode (parallel to `SearchEngine` for search).

```java
public final class RagService {
    private final HybridRetriever retriever;
    private final AiProfileResolver profileResolver;
    private final MetadataStore metadata;   // nullable
    private final ServerStats stats;        // nullable

    public RagResponse ask(String question, AiPreferences prefs, UserSessionData session);
    public Stream<String> askStream(String question, AiPreferences prefs, UserSessionData session);
}

public record RagResponse(
    String answer,
    List<ScoredChunk> sources,
    long retrievalMs,
    long generationMs,
    String stackId
) {}
```

**Responsibilities:**

- Resolve `AiProfile` from session + server config
- Retrieve top chunks via `HybridRetriever`
- Build prompt with `PromptBuilder` (system instructions + grounded context)
- Stream or complete via `LlmClient`
- Record ask events in stats/metadata (unless private mode)
- Return citations (URL + excerpt) alongside answer

### 5.9 `ScoredChunk` vs `SearchHit`

| Type | Used by | Contains |
| ---- | ------- | -------- |
| `SearchHit` | `/search` (v2) | location, BM25 score, snippet |
| `ScoredChunk` | `/ask` (v3) | chunk text, parent URL, hybrid scores |

Do not overload `SearchHit` for RAG. Keep keyword and AI result types separate.

---

## 6. Lucene implementation (v3 extensions)

### 6.1 Dependencies

No new search backend. Continue Apache Lucene 9.x (pinned with v2.0). Vector
search uses Lucene's `KnnVectorField` and `KnnFloatVectorQuery` (same version as
`lucene-core`).

Optional HTTP client for providers (if not using `java.net.http` directly):

```xml
<!-- only if needed; prefer java.net.http.HttpClient in Java 24 -->
```

Document chosen HTTP approach when implemented.

### 6.2 Document schema (`LuceneSchema` v3)

v3 indexes **chunks**, not whole documents. Parent document metadata is denormalized
onto each chunk for citation.

| Field | Lucene type | Stored | Indexed | Purpose |
| ----- | ----------- | ------ | ------- | ------- |
| `chunkId` | `StringField` | yes | yes | Unique chunk id |
| `parentId` | `StringField` | yes | yes | Parent document id |
| `location` | `StringField` | yes | yes | Citation URL/path |
| `title` | `TextField` | yes | yes | Parent title, boosted |
| `text` | `TextField` | yes | yes | Chunk body (BM25 + stored for LLM) |
| `sequence` | `IntPoint` + `StoredField` | yes | yes | Order within parent |
| `indexedAt` | `LongPoint` + `StoredField` | yes | yes | Index timestamp |
| `vector` | `KnnVectorField` | no | yes | Embedding (float[]) |

**Vector dimensions** are fixed per index (e.g. 768 for `nomic-embed-text`, 1536
for `text-embedding-3-small`). The dimension is recorded in `IndexAiMetadata`.

**Migration from v2 whole-doc index:** v3 re-indexes all content as chunks. A v2
Lucene directory is not automatically upgraded; `-load-index` on a v2 index
without chunk schema triggers a rebuild or migration job (see §10 Phase 1).

### 6.3 Index directory layout

```
<dataDir>/
  lucene/              # Lucene segments (chunk docs + vectors)
  meta.json            # IndexAiMetadata + last commit time
```

`meta.json` (application metadata, not Lucene):

```json
{
  "indexVersion": 3,
  "embeddingProvider": "ollama",
  "embeddingModel": "nomic-embed-text",
  "vectorDimensions": 768,
  "chunking": { "targetChars": 2000, "overlapChars": 200 },
  "indexedAt": "2026-06-25T12:00:00Z"
}
```

### 6.4 Indexing lifecycle (v3)

```
IndexDocument (file or crawl)
  → Chunker.chunk(doc)
  → EmbeddingProvider.embedBatch(chunk texts)
  → LuceneIndexStore.addChunks(chunks, vectors)
  → commit()
```

Runtime crawl (`POST /crawl`):

1. Add parent document through existing crawl path
2. Chunk + embed new content
3. `commit()` after batch
4. Refresh searchers

**Re-embed job** (`POST /admin/reindex-embeddings`, admin only):

1. Read all chunk text from Lucene (stored fields)
2. Re-embed with current session/server `EmbeddingProvider`
3. Replace vector fields (or rebuild index if dimensions changed)

### 6.5 Hybrid query (lexical + vector)

| Mode | Lucene implementation |
| ---- | --------------------- |
| Lexical | BM25 on `text` (+ `title` boost), same analyzer as v2 |
| Vector | `KnnFloatVectorQuery` on `vector` field with query embedding |
| Merge | RRF across top-N from each list → `ScoredChunk` |

When `IndexAiMetadata` does not match the active `EmbeddingProvider`, skip vector
leg and run BM25 only.

---

## 7. Provider implementations

### 7.1 Ollama (local)

| Capability | Endpoint | Default |
| ---------- | -------- | ------- |
| Embeddings | `POST {baseUrl}/api/embeddings` | `nomic-embed-text` |
| Chat | `POST {baseUrl}/api/chat` | `llama3.2` |

Config: `ai.ollama.baseUrl` (default `http://localhost:11434`).

No API key. Connection errors surface as "Ollama unreachable" in the UI.

### 7.2 LM Studio (local, OpenAI-compatible)

| Capability | Endpoint | Default |
| ---------- | -------- | ------- |
| Embeddings | `POST {baseUrl}/v1/embeddings` | user-selected in LM Studio |
| Chat | `POST {baseUrl}/v1/chat/completions` | user-selected in LM Studio |

Config: `ai.lmstudio.baseUrl` (default `http://localhost:1234/v1`).

Implemented via `OpenAiCompatibleEmbeddingProvider` and
`OpenAiCompatibleLlmClient` with a local base URL and no auth header.

### 7.3 OpenAI (cloud)

| Capability | Endpoint | Env |
| ---------- | -------- | --- |
| Embeddings | `https://api.openai.com/v1/embeddings` | `OPENAI_API_KEY` |
| Chat | `https://api.openai.com/v1/chat/completions` | `OPENAI_API_KEY` |

Default models: `text-embedding-3-small`, `gpt-4o-mini` (configurable).

### 7.4 Claude (cloud, chat only)

| Capability | Endpoint | Env |
| ---------- | -------- | --- |
| Chat | `https://api.anthropic.com/v1/messages` | `ANTHROPIC_API_KEY` |
| Embeddings | — | use `ai.claude.embeddingStack` (`ollama` or `openai`) |

Implemented via `AnthropicLlmClient` (Messages API shape differs from OpenAI).

### 7.5 Shared OpenAI-compatible client

`OpenAiCompatibleLlmClient` and `OpenAiCompatibleEmbeddingProvider` accept:

- `baseUrl`
- `apiKey` (nullable for local)
- `model`

This covers LM Studio and can cover other local OpenAI-compatible servers without
duplicating HTTP logic.

---

## 8. Server layer changes

### 8.1 `AppContext`

```java
public class AppContext {
    private final IndexStore index;
    private final SearchEngine searchEngine;
    private final RagService ragService;
    private final AiProfileResolver aiProfileResolver;
    private final RateLimiter askRateLimiter;
    // metadata, stats, settings, crawlJobs — unchanged
}
```

Factory: extend existing `AppContext` constructor to wire `RagService` and
`AiProfileResolver` from `AiSettings`.

### 8.2 New servlets

| Servlet | Route | Purpose |
| ------- | ----- | ------- |
| `AskPageServlet` | `GET /ask` | Ask form (question input, link to AI settings) |
| `AskServlet` | `POST /ask` | Non-streaming or redirect to stream endpoint |
| `AskStreamServlet` | `GET/POST /ask/stream` | SSE stream (`text/event-stream`) |
| `AskApiServlet` | `GET /api/ask` | JSON metadata + SSE or chunked body |
| `AiSettingsServlet` | `GET/POST /settings/ai` | Stack, models, test connection |

Existing servlet routes (`/search`, `/crawl`, browsers, etc.) are **unchanged**.

### 8.3 Streaming protocol (SSE)

Events for `/ask/stream`:

| Event | Payload |
| ----- | ------- |
| `retrieval` | `{ "sources": [ ... ScoredChunk JSON ... ] }` |
| `token` | partial answer text |
| `done` | `{ "elapsedMs": N, "stackId": "ollama" }` |
| `error` | `{ "message": "friendly error" }` |

No stack traces in event payloads.

### 8.4 Session and metadata

`UserSessionData` gains:

- `AiPreferences aiPreferences()`
- `void setAiPreferences(AiPreferences prefs)`

`MetadataStore` (optional v3 addition):

- `recordAsk(String question, String stackId)` for popular questions stats

### 8.5 Rate limiting

| Endpoint | Limiter | Default |
| -------- | ------- | ------- |
| `/search` | `searchRateLimiter` | 120/min (v2) |
| `/ask`, `/ask/stream`, `/api/ask` | `askRateLimiter` | 30/min (configurable) |

LLM calls are expensive; rate limits are separate from keyword search.

### 8.6 Prompt template (default)

`PromptBuilder` uses a fixed system prompt (configurable later):

```
You are a search assistant. Answer using ONLY the provided sources.
If the sources do not contain enough information, say so.
Cite sources by URL when making claims.

Sources:
{chunk_1.url}: {chunk_1.text}
...

Question: {user_question}
```

Token budget: trim chunks oldest/lowest-score first to fit `ai.chat.maxContextTokens`.

---

## 9. CLI compatibility

### Flags (must continue to work)

All v2.0 flags remain supported. Keyword search and indexing behave as in v2.0.

### New flags (v3.0)

| Flag | Description |
| ---- | ----------- |
| `-ask <question>` | Run one-shot RAG ask via CLI; print answer + sources to stdout |
| `-ai-stack <id>` | Stack for `-ask` (default from `ai.defaultStack`) |
| `-reindex-embeddings` | Admin/CLI: re-embed all chunks with current embedding profile |

`Driver` flow with v3.0:

```
parse args
→ open or create IndexStore at index-dir
→ if not load-index: build from -text / -html (chunk + embed on write)
→ commit
→ run -query / -counts / -index export if requested
→ if -ask: RagService.ask() using -ai-stack
→ if -server: AppContext + JettyServer (includes /ask routes)
→ on shutdown: commit + close
```

CLI uses server-default `AiSettings` from env; no session. Missing provider
credentials → friendly error and non-zero exit.

---

## 10. Implementation phases

### Phase 1 — Chunk index foundation

| Step | Deliverable |
| ---- | ----------- |
| 1.1 | `Chunk`, `Chunker`, `ChunkingOptions`; unit tests |
| 1.2 | Extend `LuceneSchema` for chunk fields (text only, no vectors yet) |
| 1.3 | `LuceneIndexStore.addChunks()`; migrate indexing pipeline (file + crawl) |
| 1.4 | `IndexAiMetadata` in `meta.json`; index version flag |
| 1.5 | `HybridRetriever` lexical-only on chunks; extractive preview endpoint |

### Phase 2 — Embeddings and hybrid retrieval

| Step | Deliverable |
| ---- | ----------- |
| 2.1 | `EmbeddingProvider`; `OllamaEmbeddingProvider`, `OpenAiEmbeddingProvider` |
| 2.2 | `KnnVectorField` on index; embed-on-index in crawl/file pipeline |
| 2.3 | `LuceneHybridSearch`; RRF merge in `HybridRetriever` |
| 2.4 | Index metadata compatibility checks; BM25 fallback |
| 2.5 | Tests with mock/fixed-dimension embeddings |

### Phase 3 — LLM providers and RagService

| Step | Deliverable |
| ---- | ----------- |
| 3.1 | `LlmClient`; Ollama, OpenAI, OpenAI-compatible, Anthropic implementations |
| 3.2 | `AiProfile`, `AiProfileFactory`, `AiProfileResolver`, `AiSettings` |
| 3.3 | `RagService`, `PromptBuilder` |
| 3.4 | `AiPreferences` in session; `AiSettingsServlet` |
| 3.5 | Built-in stacks: `ollama`, `openai`, `lmstudio`, `claude` (mixed embed) |

### Phase 4 — Ask UI and streaming

| Step | Deliverable |
| ---- | ----------- |
| 4.1 | `AskPageServlet`, `AskStreamServlet`, `AskApiServlet` |
| 4.2 | SSE streaming; citations in HTML UI |
| 4.3 | `askRateLimiter`; connection test on settings page |
| 4.4 | `EmbeddingIndexJob` + admin re-embed endpoint |
| 4.5 | CLI `-ask`, `-ai-stack`, `-reindex-embeddings` |

### Phase 5 — Hardening and release

| Step | Deliverable |
| ---- | ----------- |
| 5.1 | Provider timeout/retry config; token budget trimming |
| 5.2 | Expanded tests: mock LLM, `RagService` contract, Ask smoke test |
| 5.3 | Update README for v3.0 AI features and configuration |
| 5.4 | Tag `v3.0.0` |

Suggested interim tags (align with v2 phase pattern): `v2.6.0` (phase 1),
`v2.7.0` (phase 2), `v2.8.0` (phase 3), `v2.9.0` (phase 4), `v3.0.0-beta/rc`
(phase 5 pre-release).

---

## 11. Testing strategy

| Layer | What to test |
| ----- | ------------ |
| `Chunker` | Split sizes, overlap, empty body, max cap |
| `EmbeddingProvider` | Mock HTTP; dimension consistency |
| `LlmClient` | Mock streaming; error mapping |
| `AiProfileResolver` | Stack resolution, missing keys, mixed claude stack |
| `HybridRetriever` | BM25-only, vector-only (mock), RRF merge |
| `RagService` | End-to-end with mock embed + mock LLM |
| `LuceneIndexStore` | Chunk add/search, vector field round-trip, re-embed |
| `IndexAiMetadata` | Mismatch detection → vector disabled |
| Servlets | `HttpClient` against ephemeral Jetty; SSE event sequence |
| CLI | `-ask` with mock providers |
| Regression | `SearchParityTest`, `SearchEngineTest`, `ServerSmokeTest` still pass |

Keep all v2.0 tests green. AI tests use mocks by default; optional integration
profile (`-Pai-integration`) for live Ollama when present.

---

## 12. Configuration reference (target)

```properties
# --- v2 settings (unchanged) ---
server.port=8080
server.threads=5
index.directory=./data/index
admin.password=admin
search.defaultLimit=50
search.maxLimit=500
crawl.defaultMax=10

# --- v3 AI settings ---
ai.defaultStack=ollama

# Ollama (local)
ai.ollama.baseUrl=http://localhost:11434
ai.ollama.embeddingModel=nomic-embed-text
ai.ollama.chatModel=llama3.2

# LM Studio (local, OpenAI-compatible)
ai.lmstudio.baseUrl=http://localhost:1234/v1
ai.lmstudio.embeddingModel=
ai.lmstudio.chatModel=

# OpenAI (cloud — key from env only)
ai.openai.embeddingModel=text-embedding-3-small
ai.openai.chatModel=gpt-4o-mini

# Claude (cloud chat — key from env only)
ai.claude.chatModel=claude-sonnet-4-20250514
ai.claude.embeddingStack=ollama

# Chunking
ai.chunk.targetChars=2000
ai.chunk.overlapChars=200
ai.chunk.maxPerDoc=50

# RAG
ai.rag.topK=8
ai.rag.maxContextTokens=6000
ai.ask.rateLimitPerMinute=30

# HTTP timeouts (ms)
ai.http.connectTimeout=5000
ai.http.readTimeout=120000
```

Environment overrides:

| Variable | Maps to |
| -------- | ------- |
| `AI_DEFAULT_STACK` | `ai.defaultStack` |
| `OLLAMA_BASE_URL` | `ai.ollama.baseUrl` |
| `OPENAI_API_KEY` | OpenAI provider auth |
| `ANTHROPIC_API_KEY` | Anthropic provider auth |
| `AI_LMSTUDIO_BASE_URL` | `ai.lmstudio.baseUrl` |

**Never** store API keys in `application.properties` committed to the repository.

---

## 13. Error handling

| Situation | Behavior |
| --------- | -------- |
| Ollama / LM Studio unreachable | Settings test fails; `/ask` returns friendly message; offer keyword search link |
| Missing API key for cloud stack | Profile not listed as available; settings shows "set OPENAI_API_KEY" |
| Embedding model mismatch vs index | Hybrid falls back to BM25; UI warning to re-embed |
| Vector dimension mismatch | Block vector index write; require full re-index |
| LLM timeout | Partial answer if streamed; else error event with retry suggestion |
| Rate limit exceeded | HTTP 429 on `/ask` (same pattern as `/search`) |
| Provider HTTP 4xx/5xx | Log internally; user sees generic provider error message |

---

## 14. What stays unchanged

| Asset | Role in v3.0 |
| ----- | ------------ |
| `SearchEngine` | Keyword `/search` orchestration |
| `SearchHit` | Keyword search results |
| `InvertedIndex` | Reference implementation; unit tests |
| `IndexStore` interface | Extended, not replaced |
| Jetty servlet routes (v2) | `/search`, `/crawl`, browsers, session endpoints |
| `WorkQueue` | Indexing, batch search, crawl parallelism |
| `FileStemmer` / analyzer parity | Stemming reference |
| Session / metadata model | Extended with `AiPreferences`, not replaced |
| CSRF on POST forms | Applies to `/settings/ai`, `/ask` POST |

---

## 15. Resolved decisions

| Decision | Choice | Notes |
| -------- | ------ | ----- |
| Retrieval backend | Lucene BM25 + Lucene KNN on same index | No external vector DB in v3.0 |
| Chunk unit | ~2000 chars with overlap | Tunable via config |
| Score merge | Reciprocal Rank Fusion | Weighted merge optional later |
| Local providers | Ollama native + LM Studio OpenAI-compat | Two code paths, shared compat layer |
| Cloud providers | OpenAI (embed + chat), Claude (chat only) | Claude stack uses configured embed backend |
| API key storage | Environment variables primary | Session override optional; never on disk |
| Keyword search | Unchanged v2 path | `SearchEngine` not merged into `RagService` |
| Streaming | SSE for web | CLI prints complete answer |
| Index migration | Re-chunk + re-embed on v3 upgrade | v2 whole-doc index not auto-migrated |

---

## 16. Glossary

| Term | Meaning |
| ---- | ------- |
| **RAG** | Retrieval-Augmented Generation — retrieve chunks, then generate answer |
| **Chunk** | Smallest indexed passage for retrieval and citation |
| **EmbeddingProvider** | Generates vector embeddings for text |
| **LlmClient** | Generates chat completions (streaming or batch) |
| **AiProfile** | Active stack: embedding provider + chat client + metadata |
| **HybridRetriever** | Combines BM25 and vector search over chunks |
| **RagService** | Orchestrates ask flow (retrieve → prompt → generate) |
| **IndexAiMetadata** | Records embedding model/dims used to build the index |
| **SSE** | Server-Sent Events — streaming protocol for `/ask/stream` |

---

## 17. References

- v2.0 architecture: `docs/architecture-v2.md`
- v2.0 index API: `IndexStore.java`, `LuceneIndexStore.java`
- v2.0 search orchestration: `SearchEngine.java`
- Server wiring: `AppContext.java`, `ServerSettings.java`
- Product feature list: `README.md`
- Release history: `v2.0.0` GitHub release
