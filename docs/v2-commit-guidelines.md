# v2.0 Commit Guidelines

Companion to [`architecture-v2.md`](architecture-v2.md). Read the architecture
document first; use this file when planning and sequencing commits.

---

## Before you start

1. Read `docs/architecture-v2.md` in full.
2. Resolve open decisions in §16 (Lucene version, analyzer, document id format).
3. Confirm `mvn test` is green on `master`.
4. Create a tracking branch (e.g. `v2-dev`) if you prefer not to land on `master`
   until Phase 3 is complete.

---

## Phase map → commits

Each row is one logical commit (or a small stack of 2–3 if strictly dependent).

### Phase 1 — Foundation

| # | Commit scope | Key files | Tests required |
|---|--------------|-----------|----------------|
| 1 | `chore(deps): add Lucene dependencies` | `pom.xml` | `mvn test` still passes |
| 2 | `feat(index): add IndexDocument, SearchHit, SearchQuery, SearchOptions` | `com.cse.index.*` DTOs | compile only |
| 3 | `feat(index): add IndexStore interface` | `IndexStore.java` | — |
| 4 | `feat(index): add LuceneSchema and analyzer` | `index/lucene/LuceneSchema.java` | analyzer unit test |
| 5 | `feat(index): implement LuceneIndexStore open/add/search/close` | `LuceneIndexStore.java` | `LuceneIndexStoreTest` |
| 6 | `test(index): add SearchParityTest vs InvertedIndex` | `src/test/...` | parity on fixtures |

### Phase 2 — Integration

| # | Commit scope | Key files | Tests required |
|---|--------------|-----------|----------------|
| 7 | `feat(search): add SearchEngine` | `SearchEngine.java` | `SearchEngineTest` |
| 8 | `refactor(search): SearchService delegates to SearchEngine` | `SearchService.java` | `ServerSmokeTest` |
| 9 | `feat(crawl): add PageListener; decouple WebCrawler from server` | `WebCrawler.java`, `PageListener.java` | crawler unit test |
| 10 | `refactor(index): ThreadFileIndexer writes via IndexStore` | `ThreadFileIndexer.java` | integration test |
| 11 | `refactor(server): IndexBuilder and Driver use IndexStore` | `IndexBuilder.java`, `Driver.java` | integration test |
| 12 | `refactor(server): AppContext holds IndexStore and SearchEngine` | `AppContext.java` | `ServerSmokeTest` |

### Phase 3 — Server and CLI completion

| # | Commit scope | Key files | Tests required |
|---|--------------|-----------|----------------|
| 13 | `refactor(servlet): migrate search servlets to SearchHit` | `SearchHtmlServlet`, `SearchPageServlet` | smoke test |
| 14 | `fix(api): route SearchServlet through SearchEngine` | `SearchServlet.java` | API parity test |
| 15 | `feat(servlet): paginate index and location browsers` | browser servlets | servlet test |
| 16 | `feat(cli): add -load-index and -index-dir flags` | `Driver.java`, `ArgumentParser` | CLI test |
| 17 | `feat(index): persist on shutdown; warm start on boot` | `JettyServer`, `Driver` | smoke test with reload |
| 18 | `feat(index): export JSON and YAML from LuceneIndexStore` | `DownloadServlet`, `LuceneIndexStore` | download test |

### Phase 4 — Hardening

| # | Commit scope | Key files | Tests required |
|---|--------------|-----------|----------------|
| 19 | `feat(crawl): background crawl jobs and status endpoint` | `CrawlServlet`, `CrawlJob` | async crawl test |
| 20 | `feat(config): externalize server and index settings` | `application.properties`, config loader | config test |
| 21 | `feat(security): CSRF tokens on POST forms` | servlets, `HtmlRenderer` | security test |
| 22 | `feat(security): rate limit search and crawl` | filter or servlet guard | rate limit test |
| 23 | `test(server): expand servlet and concurrency coverage` | test package | CI green |

### Phase 5 — Release

| # | Commit scope | Key files | Tests required |
|---|--------------|-----------|----------------|
| 24 | `docs(readme): document v2.0 persistence and flags` | `README.md` | — |
| 25 | `chore(release): bump version to 2.0.0` | `pom.xml` | full `mvn test` |

---

## Dependency rules between commits

```
deps (1)
  → DTOs (2) → IndexStore (3) → LuceneSchema (4) → LuceneIndexStore (5) → ParityTest (6)
        → SearchEngine (7) → SearchService refactor (8)
        → PageListener (9) → ThreadFileIndexer (10) → Driver/IndexBuilder (11) → AppContext (12)
              → servlets (13–15) → CLI flags (16) → persist (17) → export (18)
                    → async crawl (19) → config (20) → security (21–22) → tests (23) → release (24–25)
```

Do not merge Phase 3 servlet migrations before Phase 2 commit 12 (AppContext)
is complete.

---

## Per-commit checklist

Copy into each PR description:

```
- [ ] mvn test passes
- [ ] No Lucene imports outside com.cse.index.lucene
- [ ] Search path uses SearchEngine (if touching search)
- [ ] InvertedIndex tests unchanged and passing
- [ ] No stack traces exposed to HTTP clients
- [ ] architecture-v2.md updated if design decision changed
```

---

## What not to put in a single commit

| Avoid | Why |
| ----- | --- |
| Lucene store + all servlet migrations | Too large to review |
| Removing `InvertedIndex` | Out of scope; breaks teaching tests |
| Unrelated README + code | One concern per commit |
| Phase 4 security before Phase 3 persist | Persist is higher priority |

---

## Branch and tag strategy

Push to `origin` after **every** commit. Tag at the end of each phase:

| Phase | Milestone | Tag |
| ----- | --------- | --- |
| 1 | Lucene foundation (`IndexStore`, `LuceneIndexStore`, parity tests) | `v1.2.0` |
| 2 | `SearchEngine`, crawl decoupling, `AppContext` migration | `v1.3.0` |
| 3 | Servlet/CLI completion, persist, export | `v1.4.0` |
| 4 | Async crawl, config, security, test hardening | `v1.5.0` |
| 5a | README + docs | `v2.0.0-beta.1` |
| 5b | Full test pass, release candidate | `v2.0.0-rc.1` |
| 5c | Version bump, final release | `v2.0.0` |

Create a GitHub Release for each tag with a short changelog.

---

## Updating these documents

When a decision in §16 of `architecture-v2.md` is resolved:

1. Edit the table with the chosen option and date.
2. If the commit plan changes, update the phase map in this file.
3. Reference the commit SHA in the architecture doc's §16 if useful.
