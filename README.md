# custom-se

A multi-threaded search engine written in Java. It builds an inverted index
from text files or crawled web pages and supports exact and partial search
queries, with results written as JSON.

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
```

## Requirements

- Java 24 (JDK)
- Maven

## Features

- Multi-threaded inverted indexing backed by a thread-safe index and a work
  queue
- Exact and partial search
- JSON output for the index, word counts, and search results
- Web crawling from a seed URI

## Building and Testing

```sh
mvn compile   # compile the sources
mvn test      # run unit + integration tests
mvn package   # build the JAR
```

### Test suites

| Suite | Location | What it covers |
|-------|----------|----------------|
| Unit tests | `com.cse.cli`, `com.cse.index`, etc. | Core logic, concurrency, argument parsing |
| Integration tests | `com.cse.integration` | End-to-end CLI runs on small inputs |

```sh
# Default test run
mvn test

# Run a single test class
mvn -Dtest=SearchEngineIntegrationTest test
```

Continuous integration runs the same compile → test → package steps on every
push and pull request to `main` (see `.github/workflows/maven.yml`).

## Usage

Run the `com.cse.cli.Driver` class with any combination of the following flags:

| Flag       | Argument         | Description                                                      |
| ---------- | ---------------- | ---------------------------------------------------------------- |
| `-text`    | path             | File or directory to index                                       |
| `-query`   | path             | File of search queries to run                                    |
| `-partial` | _(none)_         | Use partial search instead of exact search                       |
| `-html`    | seed URI         | Crawl the web starting from the given seed                       |
| `-crawl`   | count (default 1)| Maximum number of pages to crawl when using `-html`              |
| `-threads` | count (default 5)| Number of worker threads to use                                  |
| `-counts`  | path (`counts.json`) | Write word counts as JSON                                    |
| `-index`   | path (`index.json`)  | Write the inverted index as JSON                            |
| `-results` | path (`results.json`)| Write search results as JSON                                |

### Example

```sh
mvn exec:java -Dexec.mainClass="com.cse.cli.Driver" -Dexec.args="-text input/ -query queries.txt -partial -results results.json -threads 8"
```

Or after packaging:

```sh
java -cp target/custom-se-1.0.0.jar com.cse.cli.Driver -text input/ -query queries.txt -partial -results results.json -threads 8
```

## Phase 1 Web Server

Run the web server entry point `com.cse.server.ServerMain`.

### Build index from local text files

```sh
mvn exec:java -Dexec.mainClass="com.cse.server.ServerMain" -Dexec.args="-text input/ -port 8080 -threads 5"
```

### Build index from a crawl seed URL

```sh
mvn exec:java -Dexec.mainClass="com.cse.server.ServerMain" -Dexec.args="-html https://example.com/ -crawl 25 -port 8080 -threads 5"
```

### Endpoints

- `GET /` static UI
- `GET /api/health` returns `{\"status\":\"ok\"}`
- `GET /api/search?q=...&partial=true|false&limit=...` returns JSON search results
