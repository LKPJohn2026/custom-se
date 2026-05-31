# project-john-la-usfca-98

A multi-threaded search engine written in Java. It builds an inverted index
from text files or crawled web pages and supports exact and partial search
queries, with results written as JSON.

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
mvn test      # run the tests
mvn package   # build the JAR
```

Continuous integration runs the same compile → test → package steps on every
push and pull request to `main` (see `.github/workflows/maven.yml`).

## Usage

Run the `Driver` class with any combination of the following flags:

| Flag       | Argument         | Description                                                      |
| ---------- | ---------------- | ---------------------------------------------------------------- |
| `-text`    | path             | File or directory to index                                       |
| `-query`   | path             | File of search queries to run                                    |
| `-partial` | _(none)_         | Use partial search instead of exact search                       |
| `-html`    | seed URI         | Crawl the web starting from the given seed                       |
| `-threads` | count (default 5)| Number of worker threads to use                                  |
| `-counts`  | path (`counts.json`) | Write word counts as JSON                                    |
| `-index`   | path (`index.json`)  | Write the inverted index as JSON                            |
| `-results` | path (`results.json`)| Write search results as JSON                                |

### Example

```sh
java edu.usfca.cs272.Driver -text input/ -query queries.txt -partial -results results.json -threads 8
```
