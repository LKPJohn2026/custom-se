# custom-se

A multi-threaded search engine written in Java. It builds an inverted index from
local text files or crawled web pages, supports exact and partial search, and
can run as a command-line tool or as an embedded **Jetty** web server with a
full HTML UI.

**Current version:** 1.1.0

## Features

### Core engine

- Multi-threaded inverted indexing with a thread-safe index and work queue
- Exact and partial search over stemmed queries
- Web crawling from a seed URI with configurable page limits
- JSON export of the index, word counts, and search results

### Web server (v1.1.0)

- Servlet-rendered HTML UI with shared layout (Bulma CSS, dark/light theme, logo)
- Cookie-based **multi-user session tracking** (history, visited, favorites, private mode)
- **In-memory global metadata** (top-5 popular queries and most-visited results)
- Runtime crawl of new seed URLs (skips already-indexed pages)
- Index and location browsers with partial filtering
- Index download as JSON or YAML
- Password-protected graceful shutdown
- JSON API endpoints alongside the HTML UI

## Project structure

```
src/main/java/com/cse/
  cli/          Command-line entry point and argument parsing
  index/        Inverted index and threaded file indexing
  search/       Search interface and threaded query processing
  crawl/        Web crawler
  stem/         Text parsing, cleaning, and stemming
  io/           JSON output
  concurrent/   Work queue and read/write lock
  net/          HTTP fetching and HTML processing
  server/       Jetty server, servlets, sessions, metadata, and HTML rendering
    meta/       In-memory global metadata (queries, visits, page snippets)
    search/     Server-side search orchestration
    servlet/    HTTP endpoint handlers
    session/    Per-user session state (cookies)
    view/       Shared HTML layout and YAML export
src/main/resources/web/
  logo.svg      Search engine logo served at /logo.svg
```

## Requirements

- Java 24 (JDK)
- Maven

## Building and testing

```sh
mvn compile   # compile the sources
mvn test      # run unit + integration tests
mvn package   # build the JAR (target/custom-se-1.1.0.jar)
```

### Test suites

| Suite | Location | What it covers |
|-------|----------|----------------|
| Unit tests | `com.cse.cli`, `com.cse.index`, `com.cse.concurrent`, etc. | Core logic, concurrency, argument parsing |
| Server tests | `com.cse.server` | Smoke tests, session data, metadata ranking |
| Integration tests | `com.cse.integration` | End-to-end CLI runs on small inputs |

```sh
mvn test                                          # full suite
mvn -Dtest=ServerSmokeTest test                   # web server smoke test
mvn -Dtest=UserSessionDataTest test               # session behavior
mvn -Dtest=SearchEngineIntegrationTest test       # CLI integration
```

Continuous integration runs compile → test → package on every push and pull
request (see `.github/workflows/maven.yml`).

## Command-line usage

Run `com.cse.cli.Driver` with any combination of the following flags:

| Flag | Argument | Description |
| ---- | -------- | ----------- |
| `-text` | path | File or directory to index |
| `-query` | path | File of search queries to run |
| `-partial` | _(none)_ | Use partial search instead of exact search |
| `-html` | seed URI | Crawl the web starting from the given seed |
| `-crawl` | count (default 1) | Maximum pages to crawl when using `-html` |
| `-threads` | count (default 5) | Number of worker threads |
| `-counts` | path (`counts.json`) | Write word counts as JSON |
| `-index` | path (`index.json`) | Write the inverted index as JSON |
| `-results` | path (`results.json`) | Write search results as JSON |
| `-server` | port (default 8080) | Start the Jetty web server after indexing |

### CLI examples

Index local files and write search results:

```sh
mvn exec:java -Dexec.mainClass="com.cse.cli.Driver" \
  -Dexec.args="-text input/ -query queries.txt -partial -results results.json -threads 8"
```

Index local files and start the web server:

```sh
mvn exec:java -Dexec.mainClass="com.cse.cli.Driver" \
  -Dexec.args="-text input/ -server 8080 -threads 5"
```

Crawl a seed URL, then serve search over the crawled index:

```sh
mvn exec:java -Dexec.mainClass="com.cse.cli.Driver" \
  -Dexec.args="-html https://example.com/ -crawl 25 -server 8080 -threads 5"
```

After packaging:

```sh
java -cp target/custom-se-1.1.0.jar com.cse.cli.Driver \
  -text input/ -query queries.txt -partial -results results.json -threads 8
```

## Web server

The web server is started with `-server [port]` on `Driver`, or via the
standalone entry point `com.cse.server.ServerMain`:

```sh
mvn exec:java -Dexec.mainClass="com.cse.server.ServerMain" \
  -Dexec.args="-text input/ -port 8080 -threads 5"
```

Open `http://localhost:8080/` in a browser.

### Search

| Endpoint | Method | Description |
| -------- | ------ | ----------- |
| `/` | GET | Search form with partial/exact, reverse sort, and "I'm Feeling Lucky" |
| `/search?q=...` | GET | HTML results with count, score, timing stats, and favorite stars |
| `/api/health` | GET | JSON health check (`{"status":"ok"}`) |
| `/api/search?q=...&partial=true\|false&limit=...` | GET | JSON search results |

Search parameters on `/search`:

| Parameter | Description |
| --------- | ----------- |
| `q` | Query string (multi-word supported) |
| `partial` | `true` for partial search, omit/`false` for exact |
| `reverse` | `true` to reverse result order |
| `lucky` | `1` to redirect to the top result |

### User tracking (per session)

Each browser receives a session cookie. State is stored server-side and is
isolated per user.

| Endpoint | Method | Description |
| -------- | ------ | ----------- |
| `/history` | GET | View search history with timestamps |
| `/history/clear` | POST | Clear search history |
| `/visited` | GET | View clicked result links with timestamps |
| `/visited/clear` | POST | Clear visited results |
| `/favorites` | GET | View starred results |
| `/favorites/toggle` | POST | Add or remove a favorite (`where` parameter) |
| `/favorites/clear` | POST | Clear all favorites |
| `/private/toggle` | POST | Toggle private mode (disables tracking, clears stored data) |
| `/visit` | POST | Record a result click (`where` parameter) |
| `/theme/toggle` | POST | Toggle dark/light mode |

Private mode prevents new history, visited, and favorite entries from being
stored. Enabling it also clears any existing tracked data for that session.

### Global metadata (in-memory)

Server-wide statistics are kept in memory and reset when the server stops.

| Endpoint | Method | Description |
| -------- | ------ | ----------- |
| `/stats/queries` | GET | Top 5 most popular search queries |
| `/stats/visited` | GET | Top 5 most visited result links |

Page snippets (title, content length, crawl timestamp) are recorded automatically
when pages are crawled via `-html` or the `/crawl` endpoint.

### Index exploration and export

| Endpoint | Method | Description |
| -------- | ------ | ----------- |
| `/index` | GET | Browse all indexed words |
| `/index?word=...` | GET | View locations for a single word |
| `/locations` | GET | Browse all indexed locations |
| `/locations?prefix=...` | GET | Filter locations by substring |
| `/download?file=index&type=json` | GET | Download the index as JSON |
| `/download?file=index&type=yaml` | GET | Download the index as YAML |

### Runtime crawling

| Endpoint | Method | Description |
| -------- | ------ | ----------- |
| `/crawl` | GET | Form to submit a new seed URI and optional max page count |
| `/crawl` | POST | Crawl additional pages (`seed`, optional `max`) |

URLs already present in the index are skipped and do not count toward the max.

### Admin

| Endpoint | Method | Description |
| -------- | ------ | ----------- |
| `/admin` | GET | Shutdown form |
| `/admin/shutdown` | POST | Graceful server stop (password required) |

Default admin password: `admin` (configured in `AppContext.ADMIN_PASSWORD`).

### Server footer

Every HTML page includes a footer with uptime, total queries executed, indexed
word and location counts, and server start time.

## Architecture overview

```
Browser
  │
  ├─ GET /              → SearchPageServlet
  ├─ GET /search        → SearchHtmlServlet ─┬─ SessionService (per-user state)
  │                                        ├─ MetadataStore (global stats)
  │                                        ├─ ThreadSafeInvertedIndex
  │                                        └─ HtmlRenderer (shared layout)
  ├─ GET /history, /visited, /favorites, /stats/*, /index, /locations
  ├─ POST /crawl, /admin/shutdown, /private/toggle, /theme/toggle
  └─ GET /api/search    → SearchServlet (JSON)
```

- **`AppContext`** holds the shared index, metadata store, server stats, and
  thread pool for runtime crawls.
- **`UserSessionData`** stores per-session history, visited links, favorites,
  theme preference, private mode flag, and last-visit timestamp.
- **`MetadataStore`** tracks query and visit counts plus optional page metadata
  from crawls.

## Releases

| Version | Highlights |
| ------- | ---------- |
| [v1.1.0](https://github.com/LKPJohn2026/custom-se/releases/tag/v1.1.0) | Session tracking, global metadata, index/location browsers, runtime crawl, downloads, admin shutdown, UI polish |
| [v1.0.0](https://github.com/LKPJohn2026/custom-se/releases/tag/v1.0.0) | Jetty web server, HTML form and results, JSON API, `-server` flag |

## License

See repository for license details.
