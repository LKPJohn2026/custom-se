# custom-se v2.0 Architecture

This document defines the target architecture for **custom-se v2.0**: a
**Lucene-backed `IndexStore`** behind a stable application API, while preserving
CLI compatibility and the existing v1.1 web feature set.

Use this as the authoritative reference before planning commits. All v2.0 work
should align with the boundaries, interfaces, and migration rules described
here.

---

## 1. Purpose

v1.1.0 delivers a capable single-node search server, but the inverted index is
held entirely in memory (`TreeMap` in `InvertedIndex`), search quality is limited
to a simple TF ratio, and the HTML and JSON search paths diverge. v2.0 addresses
these structural limits without rewriting the product surface.

### Goals

| Goal | Rationale |
| ---- | --------- |
| **Durable index** | Restart the server without rebuilding from source files or re-crawling |
| **Unified search** | One code path for HTML, JSON API, and CLI batch search |
| **Better ranking** | BM25 (Lucene default similarity) replaces `matches / docLength` scoring |
| **Scalable retrieval** | Pagination, bounded result sets, safe index browsers |
| **Clean layering** | Crawl and server code must not depend on each other circularly |
| **CLI compatibility** | `Driver` flags from Project v5.0 remain supported |
| **Preserve v1.1 features** | Sessions, metadata, browsers, crawl UI, downloads, admin |

### Non-goals (v2.0)

- Distributed or multi-node indexing
- Replacing Jetty or servlet-based HTML UI
- User account system / OAuth
- Real-time streaming ingestion
- Removing `InvertedIndex` from the repository

---

## 2. Current state (v1.1.0 baseline)

### Index and search

```
InvertedIndex (TreeMap)
  └── ThreadSafeInvertedIndex (MultiReaderLock wrapper)
        ├── exactIndex(stems)   → TF score, sorted
        ├── partialIndex(stems) → prefix match on stems
        └── indexJson(path)     → export only
```

**Consumers today:**

| Component | Index access |
| --------- | ------------ |
| `Driver` | Builds `ThreadSafeInvertedIndex`, optional `-index` export |
| `IndexBuilder` | Builds index for `ServerMain` |
| `SearchService` | `partialIndex` / `exactIndex` |
| `SearchServlet` | Direct index call (bypasses `SearchService`) |
| `ThreadedSearchProcessor` | Direct index call (CLI batch) |
| `IndexBrowserServlet` | `getWords()`, `getLocations(word)` |
| `DownloadServlet` | `indexJson()` to temp file |
| `WebCrawler` | `addAllWords()` + imports `server.meta` |

### Server state

`AppContext` holds:

- `ThreadSafeInvertedIndex index`
- `MetadataStore metadata` (in-memory)
- `ServerStats stats`
- `WorkQueue` factory for runtime crawls

Session data (`UserSessionData`) and global metadata remain **in-memory** in
v2.0 unless explicitly scoped into a later phase.

---

## 3. Target architecture overview

```
┌─────────────────────────────────────────────────────────────────┐
│  CLI (Driver)          Server (Jetty + servlets)              │
│       │                        │                               │
│       └──────────┬─────────────┘                               │
│                  ▼                                             │
│           SearchEngine  ← unified orchestration                │
│                  │                                             │
│       ┌──────────┼──────────┐                                │
│       ▼          ▼          ▼                                  │
│  IndexStore  MetadataStore  SessionService                     │
│       │                                                        │
│       ▼                                                        │
│  LuceneIndexStore  (runtime)                                   │
│       │                                                        │
│       ▼                                                        │
│  Lucene Directory on disk                                      │
└─────────────────────────────────────────────────────────────────┘

InvertedIndex / ThreadSafeInvertedIndex  →  tests & teaching only
```

### Design principles

1. **Interface at the boundary** — application code depends on `IndexStore`, not
   Lucene types.
2. **SearchEngine owns orchestration** — stats, session, metadata, timing, and
   options parsing live in one place.
3. **Lucene is an implementation detail** — confined to `com.cse.index.lucene`.
4. **InvertedIndex stays** — unit tests and integration tests continue to validate
   the original algorithm; it is not deleted.
5. **Fail soft in UI** — no stack traces in user-visible output (existing rule).

---

## 4. Package layout (target)

```
src/main/java/com/cse/
  cli/                    Driver, ArgumentParser (unchanged contract)
  index/
    InvertedIndex.java           # retained — reference implementation
    ThreadSafeInvertedIndex.java # retained — tests
    ThreadFileIndexer.java       # adapted to write via IndexStore
    IndexStore.java              # NEW — core abstraction
    IndexDocument.java           # NEW — document DTO
    SearchHit.java               # NEW — result DTO (replaces SearchResult at boundary)
    SearchQuery.java             # NEW — query + mode (exact/partial/phrase)
    SearchOptions.java           # NEW — limit, offset, reverse, lucky
    lucene/
      LuceneIndexStore.java      # NEW — Lucene implementation
      LuceneSchema.java          # NEW — field names and analyzers
      LuceneIndexManager.java    # NEW — open/close/commit/snapshot paths
  search/
    Searcher.java
    SearchEngine.java            # NEW — unified search (replaces ad-hoc calls)
    ThreadedSearchProcessor.java # refactored to use SearchEngine
  crawl/
    WebCrawler.java              # refactored — IndexStore + listener, no server imports
    PageListener.java            # NEW — metadata callback interface
  server/
    AppContext.java              # holds IndexStore instead of ThreadSafeInvertedIndex
    IndexBuilder.java            # builds or loads IndexStore
    search/SearchService.java    # thin wrapper over SearchEngine
    ... (servlets, session, meta, view — updated to use IndexStore/SearchHit)
```

New code belongs in `index/` and `search/`. Lucene imports are **only** allowed
under `com.cse.index.lucene`.

---

## 5. Core abstractions

### 5.1 `IndexDocument`

Represents one indexed document (a text file path or crawled URL).

```java
public record IndexDocument(
    String id,          // stable id, typically the location URI/path
    String location,    // display / link target (same as id for v2.0)
    String title,       // optional, from HTML <title> or file name
    String body,        // raw text fed to the analyzer
    long indexedAt      // epoch millis
) {}
```

v2.0 indexes **whole documents** (one Lucene doc per file/URL). Chunking is out
of scope for this version.

### 5.2 `IndexStore`

The single write/read contract for the application.

```java
public interface IndexStore extends AutoCloseable {

    // --- lifecycle ---
    void open(Path indexDir) throws IOException;
    void commit() throws IOException;
    void close() throws IOException;
    boolean isOpen();
    Path indexDirectory();

    // --- writes ---
    void addDocument(IndexDocument doc) throws IOException;
    void deleteDocument(String id) throws IOException;
    long documentCount();

    // --- reads (browse) ---
    Set<String> listTerms();              // paginated in servlets
    Set<String> listLocations();          // paginated in servlets
    Set<String> locationsForTerm(String term);

    // --- search ---
    List<SearchHit> search(SearchQuery query, SearchOptions options);

    // --- export (retain CLI compatibility) ---
    void exportJson(Path path) throws IOException;
    void exportYaml(Path path) throws IOException;
}
```

**Thread safety:** `LuceneIndexStore` must be safe for concurrent searches
during background indexing. Use Lucene's `SearcherManager` / `IndexWriter` commit
pattern or synchronize writes while allowing concurrent readers.

### 5.3 `SearchQuery` and `SearchOptions`

```java
public record SearchQuery(String raw, QueryMode mode) {}

public enum QueryMode {
    EXACT,    // all query terms must match (AND)
    PARTIAL,  // prefix/wildcard on stemmed terms (v1.1 partial behavior)
    PHRASE    // v2.0 addition: quoted phrase match
}

public record SearchOptions(
    int limit,      // default 50, max 500
    int offset,     // default 0
    boolean reverse,
    boolean lucky
) {}
```

Stemming: continue using OpenNLP Snowball (`FileStemmer`) at the application
layer **or** configure a Lucene `Analyzer` that applies the same English stemmer.
**Requirement:** integration tests must show equivalent behavior on the project
test corpus for exact and partial modes.

### 5.4 `SearchHit`

Replaces `InvertedIndex.SearchResult` at application boundaries.

```java
public record SearchHit(
    String location,
    double score,
    int matchCount,       // optional; may be 0 if not computed
    String snippet        // optional highlighted excerpt
) implements Comparable<SearchHit> {
    // sort: score desc, location asc (match v1.1 tie-breaking where practical)
}
```

Servlets and `JsonWriter` map `SearchHit` → HTML/JSON. Do not expose Lucene
`ScoreDoc` outside `com.cse.index.lucene`.

### 5.5 `SearchEngine`

Unified orchestration used by server and CLI.

```java
public final class SearchEngine {
    private final IndexStore index;
    private final MetadataStore metadata;   // nullable for CLI-only use
    private final ServerStats stats;        // nullable for CLI-only use

    public SearchResponse search(
        SearchQuery query,
        SearchOptions options,
        UserSessionData session   // nullable
    );
}
```

**Responsibilities (move here from `SearchService`):**

- Record query in `ServerStats` and `MetadataStore`
- Record query in session history (unless private mode)
- Delegate retrieval to `IndexStore.search`
- Apply reverse sort and lucky redirect logic
- Measure elapsed time

`SearchService` becomes a thin adapter: resolve session from request, parse
parameters, call `SearchEngine`.

`SearchServlet` **must** call `SearchEngine` (fixes v1.1 API parity gap).

### 5.6 `PageListener` (crawl decoupling)

Remove `WebCrawler` → `MetadataStore` dependency.

```java
public interface PageListener {
    void onPageIndexed(String location, String title, String body, String snippet);
}
```

`CrawlServlet` and `Driver` pass a listener that forwards to `MetadataStore`.
`WebCrawler` only knows `IndexStore` and `PageListener`.

---

## 6. Lucene implementation

### 6.1 Dependency

Add to `pom.xml`:

```xml
<dependency>
    <groupId>org.apache.lucene</groupId>
    <artifactId>lucene-core</artifactId>
    <version><!-- pin latest 9.x compatible with Java 24 --></version>
</dependency>
<dependency>
    <groupId>org.apache.lucene</groupId>
    <artifactId>lucene-queryparser</artifactId>
    <version><!-- same as lucene-core --></version>
</dependency>
<dependency>
    <groupId>org.apache.lucene</groupId>
    <artifactId>lucene-analysis-common</artifactId>
    <version><!-- same as lucene-core --></version>
</dependency>
```

Pin one Lucene version across all Lucene artifacts. Document the chosen version
in this file when implemented.

### 6.2 Document schema (`LuceneSchema`)

| Field | Lucene type | Stored | Indexed | Purpose |
| ----- | ----------- | ------ | ------- | ------- |
| `id` | `StringField` | yes | yes | Unique document id (URL/path) |
| `location` | `StringField` | yes | yes | Result link target |
| `title` | `TextField` | yes | yes | Title text, boosted in queries |
| `body` | `TextField` | yes | yes | Main content |
| `indexedAt` | `LongPoint` + `StoredField` | yes | yes | Crawl/index timestamp |

Use `StandardAnalyzer` or a custom analyzer aligned with `FileStemmer` behavior.
Default similarity: **BM25** (Lucene default).

### 6.3 Index directory layout

```
<dataDir>/
  lucene/          # Lucene segments (managed by IndexWriter)
  meta.json        # optional: index version, last commit time (application metadata)
```

**Configuration:**

| Source | Default | Description |
| ------ | ------- | ----------- |
| `-index` flag (CLI) | `index.json` | Export path (unchanged); import via new `-load-index` |
| `-load-index` (new) | — | Open existing Lucene directory instead of rebuilding |
| `INDEX_DIR` env | `./data/index` | Server index directory |
| `ServerConfig` | — | Add `-index-dir` for `ServerMain` |

### 6.4 Persistence lifecycle

```
Startup:
  if -load-index dir exists → IndexStore.open(dir)
  else → build from -text / -html → IndexStore.addDocument* → commit()

Shutdown:
  IndexStore.commit()
  IndexStore.close()
  (optional hook via AppContext.shutdownHook)
```

Runtime crawl (`POST /crawl`):

1. Add documents through `IndexStore.addDocument`
2. `commit()` after crawl batch completes
3. Refresh `SearcherManager` so new docs are visible immediately

---

## 7. Query mapping (v1.1 → v2.0)

| v1.1 behavior | v2.0 implementation |
| ------------- | ------------------- |
| `exactIndex(stems)` | Lucene query: AND of stemmed terms against `body` (+ `title` boost) |
| `partialIndex(stems)` | Lucene query: prefix query per stem (e.g. `term*`) ANDed |
| Score = matches / docLength | BM25 score from Lucene |
| Sort by score, matches, location | Sort by score desc, location asc |
| Multi-word query | Split + stem via `FileStemmer`, build Lucene `BooleanQuery` |
| Phrase (new) | Quoted input → `PhraseQuery` on `body` |

**Compatibility tests:** Add `SearchParityTest` comparing `InvertedIndex` and
`LuceneIndexStore` result **locations** (not necessarily identical scores) on
the integration test corpus.

---

## 8. Server layer changes

### 8.1 `AppContext`

```java
public class AppContext {
    private final IndexStore index;
    private final SearchEngine searchEngine;
    // metadata, stats, threads — unchanged
}
```

Factory: `AppContext.create(IndexStore index, int threads, Path indexDir)`.

### 8.2 Servlet updates

| Servlet | Change |
| ------- | ------ |
| `SearchHtmlServlet` | Parse params → `SearchEngine` |
| `SearchServlet` | Parse params → `SearchEngine` → JSON |
| `IndexBrowserServlet` | `listTerms()` with pagination (`page`, `size`) |
| `LocationBrowserServlet` | `listLocations()` with prefix filter + pagination |
| `DownloadServlet` | `index.exportJson()` / `exportYaml()` |
| `CrawlServlet` | Async job in v2.0 phase 2 (see §10); write via `IndexStore` |

### 8.3 Index browsers and pagination

v1.1 renders all words/locations in one HTML page. v2.0 **must paginate**:

- Default page size: 100
- Query params: `page` (0-based), `size` (max 500)
- Footer shows total count

### 8.4 Metadata and sessions

No schema change in v2.0. `MetadataStore` and `UserSessionData` remain
in-memory. Optional follow-up: persist `meta.json` alongside Lucene directory.

---

## 9. CLI compatibility

### Flags (must continue to work)

| Flag | v2.0 behavior |
| ---- | ------------- |
| `-text` | Index files into `IndexStore` |
| `-html` / `-crawl` | Crawl into `IndexStore` |
| `-query` / `-partial` / `-results` | `SearchEngine` + `ThreadedSearchProcessor` |
| `-counts` | Derive from Lucene doc stats or term enumeration |
| `-index` | Export JSON via `IndexStore.exportJson()` |
| `-server` | Open/commit index dir, start Jetty |

### New flags

| Flag | Description |
| ---- | ----------- |
| `-load-index <dir>` | Open existing Lucene index; skip rebuild |
| `-index-dir <dir>` | Directory for Lucene storage (default `./data/index`) |

`Driver` flow with v2.0:

```
parse args
→ open or create IndexStore at index-dir
→ if not load-index: build from -text / -html
→ commit
→ run -query / -counts / -index export if requested
→ if -server: AppContext + JettyServer
→ on shutdown: commit + close
```

---

## 10. Implementation phases

Work should land in **small, reviewable commits** in this order. Do not skip
phase 1 when starting phase 2.

### Phase 1 — Foundation

| Step | Deliverable |
| ---- | ----------- |
| 1.1 | Add Lucene dependencies; `IndexDocument`, `SearchHit`, `SearchQuery`, `SearchOptions` |
| 1.2 | `IndexStore` interface; `LuceneIndexStore` skeleton (open, add, search, close) |
| 1.3 | `LuceneSchema` + analyzer choice; unit tests with in-memory `ByteBuffersDirectory` |
| 1.4 | `SearchParityTest` against `InvertedIndex` on test fixtures |

### Phase 2 — Integration

| Step | Deliverable |
| ---- | ----------- |
| 2.1 | `SearchEngine`; refactor `SearchService` |
| 2.2 | `ThreadFileIndexer` → writes `IndexDocument` via `IndexStore` |
| 2.3 | `WebCrawler` → `IndexStore` + `PageListener`; remove `server.meta` import |
| 2.4 | `IndexBuilder` / `Driver` / `ServerMain` use `IndexStore` |
| 2.5 | `AppContext` holds `IndexStore` + `SearchEngine` |

### Phase 3 — Server and CLI completion

| Step | Deliverable |
| ---- | ----------- |
| 3.1 | Update all servlets to `SearchHit` / `IndexStore` |
| 3.2 | `SearchServlet` through `SearchEngine` (API parity) |
| 3.3 | Paginated index/location browsers |
| 3.4 | `-load-index`, `-index-dir`; shutdown commit hook |
| 3.5 | `exportJson` / `exportYaml` on `LuceneIndexStore` |

### Phase 4 — Async crawl and hardening

| Step | Deliverable |
| ---- | ----------- |
| 4.1 | Background crawl jobs (`CrawlJob`, status endpoint) |
| 4.2 | External config (`application.properties` / env) for port, password, index dir |
| 4.3 | CSRF tokens on POST forms |
| 4.4 | Rate limiting on `/search` and `/crawl` |
| 4.5 | Expanded servlet and concurrency tests |

### Phase 5 — Documentation and release

| Step | Deliverable |
| ---- | ----------- |
| 5.1 | Update README for v2.0 flags and persistence |
| 5.2 | Update `ServerSmokeTest` and integration tests |
| 5.3 | Tag `v2.0.0` |

---

## 11. Commit guidelines

### Commit message format

Follow existing repo style:

```
feat(index): add IndexStore interface and LuceneIndexStore
fix(search): route SearchServlet through SearchEngine
test(index): add SearchParityTest for exact and partial modes
docs(architecture): document Lucene schema
```

### Rules per commit

1. **One concern per commit** — e.g. interface only, then implementation, then
   wiring.
2. **Keep CI green** — every commit must pass `mvn test`.
3. **Do not break v1.1 behavior** until the Lucene path is wired; use feature
   flags or incremental servlet migration if needed.
4. **No Lucene imports outside `com.cse.index.lucene`** — enforce in code review.
5. **InvertIndex tests must keep passing** — do not modify algorithm behavior in
   `InvertedIndex` during v2.0.
6. **No stack traces in servlet responses** — preserve existing error handling.

### Review checklist

- [ ] Does this change import Lucene outside `index.lucene`?
- [ ] Does search go through `SearchEngine`?
- [ ] Is the index committed on shutdown?
- [ ] Are new endpoints paginated?
- [ ] Are tests added for new behavior?
- [ ] Does `Driver -help` contract remain valid?

---

## 12. Testing strategy

| Layer | What to test |
| ----- | ------------ |
| `LuceneIndexStore` | Add/search/delete, commit/reopen, export |
| `SearchEngine` | Stats, session, metadata side effects |
| `SearchParityTest` | Location sets match `InvertedIndex` on corpus |
| `WebCrawler` | Mock `IndexStore` + `PageListener` |
| Servlets | `HttpClient` against ephemeral Jetty port |
| Concurrency | Parallel search during `addDocument` + `commit` |
| CLI | `SearchEngineIntegrationTest` updated for `IndexStore` |

Keep `InvertedIndexTest`, `ThreadSafeInvertedIndexTest`, and
`SearchEngineIntegrationTest` as regression anchors.

---

## 13. Configuration reference (target)

```properties
# application.properties (classpath or file path via env)
server.port=8080
server.threads=5
index.directory=./data/index
admin.password=admin
search.defaultLimit=50
search.maxLimit=500
crawl.defaultMax=10
```

Environment overrides:

| Variable | Maps to |
| -------- | ------- |
| `SERVER_PORT` | `server.port` |
| `INDEX_DIR` | `index.directory` |
| `ADMIN_PASSWORD` | `admin.password` |

Remove hardcoded `AppContext.ADMIN_PASSWORD` when external config lands (Phase 4).

---

## 14. Error handling

| Situation | Behavior |
| --------- | -------- |
| Index dir corrupt on load | Log error; print friendly message; exit non-zero (CLI) or show error page (server) |
| Search on closed index | `IllegalStateException` caught at servlet → 503 + friendly message |
| Crawl fetch failure | Log; skip URL; continue (existing crawler behavior) |
| Export failure | 500 JSON/HTML without stack trace |

---

## 15. What stays unchanged

| Asset | Role in v2.0 |
| ----- | ------------ |
| `InvertedIndex` | Reference implementation; unit tests |
| `ThreadSafeInvertedIndex` | Concurrency tests; optional CLI fallback behind flag (not required) |
| `WorkQueue` | Indexing, batch search, crawl parallelism |
| `FileStemmer` | Stemming (or parity reference for analyzer) |
| Jetty servlet routes | URL paths unchanged for v1.1 clients |
| Session / metadata model | Same user-visible features |

---

## 16. Resolved decisions

| Decision | Choice | Notes |
| -------- | ------ | ----- |
| Index backend | Embedded Apache Lucene via `LuceneIndexStore` | Single JVM; on-disk persistence; not a remote search cluster |
| Lucene version | 9.12.0 | Pinned across all `org.apache.lucene` artifacts |
| Analyzer | `EnglishAnalyzer` (Lucene) | Parity validated by `SearchParityTest` on project corpus |
| Index id for local files | Absolute path | Matches v1.1 `location` strings |
| Phrase search | Phase 3 | After exact/partial parity tests pass |
| Async crawl | Phase 4 | Blocking crawl acceptable until then |
| `InvertedIndex` | Retained | Reference implementation and unit tests only |

---

## 17. Glossary

| Term | Meaning |
| ---- | ------- |
| **IndexStore** | Application-facing index API |
| **LuceneIndexStore** | Lucene-backed `IndexStore` implementation |
| **SearchEngine** | Orchestrates search + side effects (stats, session) |
| **SearchHit** | One search result at the application boundary |
| **Index directory** | On-disk Lucene `Directory` + optional app metadata |

---

## 18. References

- v1.1 server wiring: `JettyServer.java`, `AppContext.java`
- Current search orchestration: `SearchService.java`
- Current index structure: `InvertedIndex.java`
- Product feature list: `README.md`
- Release history: `v1.0.0`, `v1.1.0` GitHub releases
