# custom-se Runbook

Operational procedures for running, debugging, and maintaining **custom-se v3.0**
in development and single-host deployment.

Related: [`architecture.md`](architecture.md), [`protocol.md`](protocol.md).

---

## 1. Prerequisites checklist

| Requirement | Verify |
| ----------- | ------ |
| JDK 24 | `java -version` |
| Maven 3.9+ | `mvn -version` |
| `.env` present | `cp .env.example .env` and edit |
| Index directory writable | `data/index/` or `INDEX_DIR` |
| AI stack reachable | See §4 per stack |

---

## 2. Startup procedures

### 2.1 Build and test

```bash
cd /path/to/custom-se
mvn -B package
```

Expect: compile → tests → `target/custom-se-3.1.0.jar`.

### 2.2 First-time server start

```bash
cp .env.example .env
# Edit .env for your stack (see §4)

mvn exec:java -Dexec.mainClass="com.cse.cli.Driver" \
  -Dexec.args="-text input/ -server 8080 -threads 5"
```

Or with existing index:

```bash
mvn exec:java -Dexec.mainClass="com.cse.cli.Driver" \
  -Dexec.args="-text input/ -server 8080 -load-index -index-dir data/index"
```

**Startup validation:** `AiConfigValidator` runs before Jetty binds. Missing `.env`
or required keys → process exits with message (no partial server).

### 2.3 Packaged JAR

```bash
java -cp target/custom-se-3.1.0.jar com.cse.cli.Driver \
  -text input/ -server 8080 -load-index
```

Working directory must contain `.env`.

---

## 3. Health checks

| Check | Command | Expected |
| ----- | ------- | -------- |
| Server up | `curl -sf http://localhost:8080/api/health` | `{"status":"ok"}` |
| Keyword search | `curl -sf 'http://localhost:8080/api/search?q=test&limit=5'` | JSON with `results` |
| Ask (JSON) | `curl -sf 'http://localhost:8080/api/ask?q=summary'` | JSON with `answer`, `sources` |
| AI connectivity | Browser → `/settings/ai` → Test | Success banner |

---

## 4. AI stack setup

### 4.1 Ollama (local default)

```bash
# Install Ollama, then:
ollama serve
ollama pull nomic-embed-text
ollama pull llama3.2
```

`.env`:

```
AI_DEFAULT_STACK=ollama
OLLAMA_BASE_URL=http://localhost:11434
```

**Symptoms if down:** settings test fails; `/ask` error event "Check AI settings".

### 4.2 LM Studio

1. Start LM Studio local server on port 1234.
2. Load embedding + chat models in UI.

`.env`:

```
AI_DEFAULT_STACK=lmstudio
AI_LMSTUDIO_BASE_URL=http://localhost:1234/v1
```

### 4.3 OpenAI

`.env`:

```
AI_DEFAULT_STACK=openai
OPENAI_API_KEY=sk-...
```

Models configured in `application.properties` (`ai.openai.*`).

### 4.4 Claude + Voyage

`.env`:

```
AI_DEFAULT_STACK=claude
ANTHROPIC_API_KEY=sk-ant-...
VOYAGE_API_KEY=pa-...
```

Re-embed after switching **to** Claude stack if index was built with different dimensions.

---

## 5. Index operations

### 5.1 Index layout

```
data/index/
  lucene/     # Lucene segments — do not edit manually
  meta.json   # IndexAiMetadata
```

### 5.2 Rebuild from source

```bash
rm -rf data/index/lucene data/index/meta.json   # destructive
mvn exec:java -Dexec.mainClass="com.cse.cli.Driver" \
  -Dexec.args="-text input/ -index-dir data/index -threads 8"
```

### 5.3 Warm start (reuse index)

```bash
mvn exec:java -Dexec.mainClass="com.cse.cli.Driver" \
  -Dexec.args="-server 8080 -load-index -index-dir data/index"
```

### 5.4 Re-embed (same text, new embedding model)

**When:** Changed embedding model/dimensions/provider in settings.

**Web:**

1. Open `/admin`
2. POST re-embed with admin password (default `admin`, override via `ADMIN_PASSWORD`)

**CLI:**

```bash
mvn exec:java -Dexec.mainClass="com.cse.cli.Driver" \
  -Dexec.args="-reindex-embeddings -ai-stack ollama -index-dir data/index -load-index"
```

**Note:** Dimension change may require full re-index from source, not just re-embed.

### 5.5 Export index

```bash
curl -sf 'http://localhost:8080/download?file=index&type=json' -o index-export.json
```

Or CLI legacy export: `-index index.json` on `Driver`.

---

## 6. Crawl operations

### 6.1 CLI crawl + serve

```bash
mvn exec:java -Dexec.mainClass="com.cse.cli.Driver" \
  -Dexec.args="-html https://example.com/ -crawl 50 -server 8080 -threads 5"
```

### 6.2 Runtime background crawl

1. Open `/crawl`
2. Submit seed URL and max pages
3. Monitor `/crawl/status`

Already-indexed URLs are skipped. Each new page: chunk → embed → commit.

---

## 7. Ask mode operations

### 7.1 SSE stream (manual test)

```bash
curl -N 'http://localhost:8080/ask/stream?q=What+is+indexed%3F'
```

Expect events: `retrieval` → multiple `token` → `done`.

### 7.2 Rate limits

| Endpoint group | Default limit | Config |
| -------------- | ------------- | ------ |
| `/search`, `/crawl` | 120/min | hardcoded in `AppContext` |
| `/ask`, `/ask/stream`, `/api/ask` | 30/min | `ai.ask.rateLimitPerMinute` |

HTTP 429 when exceeded. Wait 60 seconds or restart server (in-memory limiter).

### 7.3 Token budget

If answers truncate sources, increase `ai.rag.maxContextTokens` or reduce `ai.rag.topK`
in `application.properties`.

---

## 8. Troubleshooting

### 8.1 Server won't start — config validation

| Message pattern | Fix |
| --------------- | --- |
| `.env not found` | `cp .env.example .env` |
| Missing `OPENAI_API_KEY` | Set key or switch stack to `ollama` |
| Missing `VOYAGE_API_KEY` | Required for `claude` stack |

### 8.2 Search returns no results

1. Confirm index built: check footer word/location counts on HTML pages.
2. Try partial search: `/search?q=term&partial=true`.
3. Verify `data/index/lucene` exists and `-load-index` used if restarting.

### 8.3 Ask returns empty sources

1. Index may lack chunks — rebuild from v3 pipeline (not v2 whole-doc index).
2. Query may not match — try keyword `/search` first.
3. Check `meta.json` for `indexVersion: 3`.

### 8.4 Ask works but no vector recall (BM25 only)

**Cause:** `EmbeddingIndexCompatibility` mismatch — active embedder ≠ index metadata.

**Fix:** Re-embed or rebuild index with current stack:

```bash
# Check meta.json embeddingProvider / embeddingModel / vectorDimensions
cat data/index/meta.json
```

Align `.env` / `/settings/ai` stack with index metadata, then `/admin/re-embed`.

### 8.5 Corrupt Lucene index

**Symptoms:** IOException on open; search 503.

**Fix:**

```bash
mv data/index data/index.bak.$(date +%s)
mkdir -p data/index
# Re-index from source files or crawl
```

### 8.6 CSRF failures on POST

Ensure form includes CSRF token from session. For API testing of POST endpoints,
use browser session or disable CSRF only in dev (not supported out of box).

### 8.7 High LLM latency / timeouts

Adjust in `application.properties`:

```properties
ai.http.connectTimeoutMs=10000
ai.http.readTimeoutMs=180000
ai.http.maxRetries=3
```

Reduce `ai.rag.topK` or `ai.rag.maxContextTokens` to shrink prompts.

---

## 9. Shutdown

### 9.1 Graceful (recommended)

1. `/admin` → shutdown with password
2. Server commits index and stops Jetty

### 9.2 CLI interrupt

Ctrl+C on `Driver` — shutdown hook should `commit()` + `close()` index.

### 9.3 Force kill

`kill -9` risks incomplete Lucene commit. Prefer graceful shutdown; if forced,
run `mvn test` or open index with `-load-index` to verify integrity.

---

## 10. Security notes (single-host dev)

| Topic | Guidance |
| ----- | -------- |
| Admin password | Change `ADMIN_PASSWORD` from default `admin` |
| API keys | `.env` only; never commit; rotate if leaked |
| Network | Bind localhost in untrusted networks; no built-in TLS |
| Rate limits | Mitigate abuse; not a substitute for auth |
| Private mode | Users can disable tracking per session |

---

## 11. CI parity (local)

Match GitHub Actions before push:

```bash
mvn -B compile
mvn -B test
mvn -B package -DskipTests
```

Workflow: `.github/workflows/maven.yml` (JDK 24 Temurin).

---

## 12. Log locations

custom-se uses Log4j2. Default console output includes:

- Index open/commit events
- Crawl progress
- Provider HTTP failures (internal detail)
- Rate limit denials

No stack traces are sent to HTTP clients.

---

## 13. Backup and restore

**Backup:**

```bash
tar czf custom-se-index-$(date +%Y%m%d).tar.gz data/index/
```

Include `.env` separately (secrets — store securely).

**Restore:**

```bash
tar xzf custom-se-index-YYYYMMDD.tar.gz
mvn exec:java -Dexec.mainClass="com.cse.cli.Driver" \
  -Dexec.args="-server 8080 -load-index -index-dir data/index"
```

Ensure `.env` stack matches index embedding metadata or plan re-embed.

---

## 14. Quick reference

| Task | Command / route |
| ---- | --------------- |
| Start server | `-server 8080` on `Driver` |
| Health | `GET /api/health` |
| Keyword search | `GET /api/search?q=...` |
| Ask | `GET /api/ask?q=...` or `/ask` UI |
| AI settings | `/settings/ai` |
| Re-embed | `/admin/re-embed` or `-reindex-embeddings` |
| Crawl status | `/crawl/status` |
| Benchmark | `-benchmark -load-index -index-dir data/index` |
| Run tests | `mvn -B test` |

---

## 15. Benchmark (keyword search)

Measure in-process keyword latency via `SearchEngine` (no HTTP overhead):

```bash
mvn exec:java -Dexec.mainClass="com.cse.cli.Driver" \
  -Dexec.args="-load-index -index-dir data/index -benchmark \
    -benchmark-queries benchmark-queries.txt -corpus input/ \
    -warmup 20 -iterations 100 -partial"
```

| Flag | Default | Purpose |
| ---- | ------- | ------- |
| `-benchmark` | — | Run benchmark and exit |
| `-benchmark-queries` | index terms | Query file (one per line, `#` comments) |
| `-corpus` | — | Raw corpus path for size in report |
| `-warmup` | 20 | Warmup iterations (not measured) |
| `-iterations` | 100 | Timed samples |
| `-limit` | 50 | Search result cap per query |
| `-partial` | off | Partial vs exact search mode |

Output one-liner format:

```text
Indexed [N documents / X corpus] ([Y index, Z chunks]) with [median ms] median query latency on [hardware].
```

Use `-corpus` for accurate corpus size; without it the report shows `0 B corpus`.

---

## 16. Escalation path

1. Reproduce with keyword `/search` (isolates index vs AI).
2. Test stack at `/settings/ai/test`.
3. Check `meta.json` vs active embedder.
4. Review [`architecture.md`](architecture.md) ADRs for expected behavior.
5. File issue with: version, stack, steps, redacted `.env` keys list (not values).
