package com.cse.benchmark;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.cse.index.IndexStore;
import com.cse.index.SearchOptions;
import com.cse.search.SearchEngine;

/**
 * Runs in-process keyword search benchmarks via {@link SearchEngine}.
 */
public final class BenchmarkRunner {

	public BenchmarkReport run(IndexStore index, BenchmarkConfig config) throws IOException {
		List<String> queries = loadQueries(config.queryFile(), index);
		if (queries.isEmpty()) {
			throw new IOException("No benchmark queries available. Provide -benchmark-queries file.");
		}

		SearchEngine engine = new SearchEngine(index);
		SearchOptions options = new SearchOptions(config.limit(), 0, false, false);

		for (int i = 0; i < config.warmup(); i++) {
			String q = queries.get(i % queries.size());
			engine.search(SearchEngine.parseQuery(q, config.partial()), options, null);
		}

		List<Long> samples = new ArrayList<>(config.iterations());
		for (int i = 0; i < config.iterations(); i++) {
			String q = queries.get(i % queries.size());
			var response = engine.search(SearchEngine.parseQuery(q, config.partial()), options, null);
			samples.add(response.elapsedMs());
		}

		long documents = index.listLocations().size();
		long chunks = index.documentCount();
		long indexBytes = BenchmarkReport.directorySize(index.indexDirectory());
		long corpusBytes = config.corpusPath() == null ? 0 : BenchmarkReport.directorySize(config.corpusPath());

		return new BenchmarkReport(
				documents,
				chunks,
				corpusBytes,
				indexBytes,
				LatencyStats.fromMillis(samples),
				HardwareInfo.capture(),
				config.partial(),
				queries.size());
	}

	static List<String> loadQueries(Path queryFile, IndexStore index) throws IOException {
		if (queryFile != null && Files.isRegularFile(queryFile)) {
			return readQueryFile(queryFile);
		}
		return fallbackQueries(index);
	}

	private static List<String> readQueryFile(Path queryFile) throws IOException {
		List<String> queries = new ArrayList<>();
		for (String line : Files.readAllLines(queryFile)) {
			String q = line.strip();
			if (q.isEmpty() || q.startsWith("#")) {
				continue;
			}
			queries.add(q);
		}
		return queries;
	}

	private static List<String> fallbackQueries(IndexStore index) {
		List<String> terms = new ArrayList<>(index.listTerms());
		if (terms.isEmpty()) {
			return List.of();
		}
		int limit = Math.min(10, terms.size());
		return terms.subList(0, limit);
	}
}
