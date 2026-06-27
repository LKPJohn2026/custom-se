# custom-se Protocol Reference

Wire formats and API contracts for **custom-se v3.0**: HTTP routes, SSE streaming,
JSON shapes, and external AI provider endpoints.

Architecture context: [`architecture.md`](architecture.md).

---

## 1. HTTP conventions

| Rule | Value |
| ---- | ----- |
| Default port | `8080` (`SERVER_PORT` / `server.port`) |
| Charset | UTF-8 on all text responses |
| Session | Cookie-based; `SessionService` assigns per-browser session id |
| CSRF | Required on POST forms (token in session); `/ask/stream` POST requires CSRF |
| Rate limits | Search/crawl: 120/min; Ask: 30/min (per session key) — HTTP 429 when exceeded |
| Errors (UI) | Friendly messages only; no stack traces in HTML or JSON |

---

## 2. Keyword search API

### 2.1 HTML search

```
GET /search?q={query}&partial=true|false&reverse=true|false&lucky=1
```

| Parameter | Description |
| --------- | ----------- |
| `q` | Query string (multi-word supported) |
| `partial` | `true` for prefix/partial mode |
| `reverse` | `true` to reverse result order |
| `lucky` | `1` redirects to top result URL |

**Response:** HTML page with ranked `SearchHit` rows (location, score, timing).

### 2.2 JSON search

```
GET /api/search?q={query}&partial=true|false&limit={n}
```

**Response (200):**

```json
{
  "query": "example terms",
  "partial": false,
  "count": 3,
  "elapsedMs": 12,
  "results": [
    {
      "location": "/path/or/url",
      "score": 1.234,
      "matchCount": 0,
      "snippet": "optional excerpt"
    }
  ]
}
```

| Field | Notes |
| ----- | ----- |
| `limit` | Capped by `search.maxLimit` (default max 500) |
| `score` | Lucene BM25 score |

### 2.3 Health

```
GET /api/health
```

```json
{"status":"ok"}
```

---

## 3. Ask API (RAG)

### 3.1 HTML Ask page

```
GET /ask
```

Renders question form; JavaScript opens SSE stream to `/ask/stream`.

### 3.2 SSE streaming (primary web protocol)

```
GET /ask/stream?q={question}
POST /ask/stream   (Content-Type: application/x-www-form-urlencoded, CSRF token)
```

**Response headers:**

```
Content-Type: text/event-stream; charset=UTF-8
Cache-Control: no-cache
Connection: keep-alive
```

**Event sequence:**

```
event: retrieval
data: {"sources":[...]}

event: token
data: {"text":"partial"}

event: token
data: {"text":" answer"}

event: done
data: {"elapsedMs":4521,"stackId":"ollama"}
```

**Error event:**

```
event: error
data: {"message":"Unable to generate an answer. Check AI settings."}
```

| Event | When | Payload |
| ----- | ---- | ------- |
| `retrieval` | After hybrid retrieve, before LLM | `{ "sources": [ ScoredChunk, ... ] }` |
| `token` | Each streamed LLM delta | `{ "text": "<delta>" }` |
| `done` | Stream complete | `{ "elapsedMs": N, "stackId": "<id>" }` |
| `error` | Provider or internal failure | `{ "message": "<friendly>" }` |

**`ScoredChunk` JSON fields:**

| Field | Type | Description |
| ----- | ---- | ----------- |
| `chunkId` | string | Stable chunk identifier |
| `location` | string | Citation URL or file path |
| `title` | string | Parent document title |
| `text` | string | Chunk passage text |
| `score` | number | Combined RRF score |
| `lexicalScore` | number | BM25 leg score |
| `vectorScore` | number | KNN leg score |

Multi-line `data` fields follow SSE spec (each line prefixed with `data: `).

### 3.3 JSON Ask (non-streaming)

```
GET /api/ask?q={question}
```

**Response (200):**

```json
{
  "answer": "Synthesized answer text.",
  "sources": [ { "chunkId": "...", "location": "...", "title": "...", "text": "...", "score": 0.032, "lexicalScore": 1.5, "vectorScore": 0.89 } ],
  "retrievalMs": 120,
  "generationMs": 3400,
  "stackId": "ollama"
}
```

| Status | Meaning |
| ------ | ------- |
| 200 | Success (empty answer if `q` missing) |
| 429 | Ask rate limit exceeded |
| 503 | LLM provider error (`LlmException`) |
| 500 | Other failure |

Error body: `{ "message": "..." }`.

---

## 4. AI settings API

```
GET  /settings/ai          HTML stack selection form
POST /settings/ai          Save session AiPreferences (CSRF)
POST /settings/ai/test     Test embedding + chat connectivity (CSRF)
```

**Resolution order for active stack:**

1. Session `AiPreferences` (from `/settings/ai`)
2. `AI_DEFAULT_STACK` / `ai.defaultStack`
3. First available profile with valid credentials

**Built-in stack ids:** `ollama`, `openai`, `lmstudio`, `claude`.

---

## 5. Admin protocols

```
GET  /admin
POST /admin/re-embed       password + CSRF → background EmbeddingIndexJob
POST /admin/shutdown       password + CSRF → graceful Jetty stop
```

Re-embed reads stored chunk text from Lucene, re-embeds with the active
`EmbeddingProvider`, updates vector fields. Required after embedding model change.

---

## 6. Crawl protocol

```
GET  /crawl                Crawl form + status link
POST /crawl                seed={url}&max={n}  (CSRF)
GET  /crawl/status         JSON job status
```

Runtime crawl: chunk → embed → Lucene commit. URLs already in index are skipped.

---

## 7. Session and metadata routes

| Route | Method | Purpose |
| ----- | ------ | ------- |
| `/history`, `/visited`, `/favorites` | GET | Session lists |
| `/history/clear`, `/visited/clear`, `/favorites/clear` | POST | Clear (CSRF) |
| `/favorites/toggle`, `/visit`, `/private/toggle`, `/theme/toggle` | POST | Session actions |
| `/stats/queries`, `/stats/visited` | GET | Global top-5 (in-memory) |

Private mode stops recording and clears existing session tracking data.

---

## 8. Index export

```
GET /download?file=index&type=json
GET /download?file=index&type=yaml
```

Exports Lucene index metadata and terms (not raw vectors in human-readable export).

---

## 9. External AI provider protocols

All provider HTTP uses `HttpExchange` with `AiHttpConfig`:

| Setting | Default |
| ------- | ------- |
| `ai.http.connectTimeoutMs` | 5000 |
| `ai.http.readTimeoutMs` | 120000 |
| `ai.http.maxRetries` | 2 |

### 9.1 Ollama (local)

| Capability | Method | Path |
| ---------- | ------ | ---- |
| Embeddings | POST | `{OLLAMA_BASE_URL}/api/embeddings` |
| Chat (stream) | POST | `{OLLAMA_BASE_URL}/api/chat` |

Default models: `nomic-embed-text` (768 dims), `llama3.2`.

No `Authorization` header.

### 9.2 OpenAI (cloud)

| Capability | Method | Path |
| ---------- | ------ | ---- |
| Embeddings | POST | `https://api.openai.com/v1/embeddings` |
| Chat | POST | `https://api.openai.com/v1/chat/completions` |

Header: `Authorization: Bearer {OPENAI_API_KEY}`.

Default models: `text-embedding-3-small` (1536 dims), `gpt-4o-mini`.

### 9.3 LM Studio (local, OpenAI-compatible)

| Capability | Method | Path |
| ---------- | ------ | ---- |
| Embeddings | POST | `{AI_LMSTUDIO_BASE_URL}/embeddings` |
| Chat | POST | `{AI_LMSTUDIO_BASE_URL}/chat/completions` |

Default base: `http://localhost:1234/v1`. Models selected in LM Studio UI.

Implemented via `OpenAiCompatibleEmbeddingProvider` / `OpenAiCompatibleLlmClient`.

### 9.4 Claude + Voyage (cloud)

| Capability | Provider | Method | Path |
| ---------- | -------- | ------ | ---- |
| Embeddings | Voyage AI | POST | `https://api.voyageai.com/v1/embeddings` |
| Chat | Anthropic | POST | `https://api.anthropic.com/v1/messages` |

Headers:

- Voyage: `Authorization: Bearer {VOYAGE_API_KEY}`
- Anthropic: `x-api-key: {ANTHROPIC_API_KEY}`, `anthropic-version: 2023-06-01`

Voyage `input_type`:

| Phase | Value |
| ----- | ----- |
| Indexing (`embedBatch`) | `document` |
| Query (`embedQuery`) | `query` |

Default models: `voyage-4` (1024 dims), `claude-sonnet-4-20250514`.

---

## 10. CLI protocol

`com.cse.cli.Driver` flags compose a batch pipeline (not HTTP):

```
-text path          Index files
-html seed          Crawl from seed
-query path         Batch search
-partial             Partial search mode
-server [port]       Start Jetty (requires .env)
-load-index          Open existing Lucene dir
-index-dir path     Lucene directory
-ask "question"      One-shot RAG to stdout
-ai-stack id         Stack for -ask / -reindex-embeddings
-reindex-embeddings  Re-embed all chunks
```

Exit codes: non-zero on config validation failure, index errors, or missing AI credentials.

---

## 11. Index metadata protocol (`meta.json`)

Written beside Lucene segments; drives embedding compatibility checks.

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

| Mismatch | Runtime behavior |
| -------- | ---------------- |
| Provider/model/dims ≠ active embedder | Vector leg disabled; BM25-only + warning |
| Missing vectors in index | BM25-only retrieval |

---

## 12. Prompt protocol (RAG)

`PromptBuilder` constructs messages for `LlmClient`:

1. **System:** instruct model to answer only from sources; cite URLs; admit insufficient evidence.
2. **User:** numbered source blocks (`location`: `text`) + question.
3. **Budget:** trim lowest-score chunks first until under `ai.rag.maxContextTokens`.

Chat request record:

```java
ChatRequest(List<ChatMessage> messages, double temperature, int maxTokens)
ChatMessage(String role, String content)  // system | user | assistant
```

---

## 13. Error code summary

| Situation | HTTP / event | User message pattern |
| --------- | ------------ | -------------------- |
| Rate limit | 429 | Too many requests |
| Missing question | 400 | Missing question |
| LLM failure | SSE `error` / 503 | Check AI settings |
| Provider unreachable | SSE `error` / settings test fail | Provider name + retry |
| Index closed | 503 | Search temporarily unavailable |
| CSRF failure | 403 | Form rejected |

Internal exceptions are logged server-side; never included in client payloads.
