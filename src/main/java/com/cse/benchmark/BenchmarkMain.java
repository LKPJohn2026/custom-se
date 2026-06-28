package com.cse.benchmark;

import java.io.IOException;

import com.cse.cli.ArgumentParser;
import com.cse.index.IndexStore;
import com.cse.server.IndexOpener;

/**
 * CLI entry for {@code -benchmark}.
 */
public final class BenchmarkMain {
	private BenchmarkMain() {
	}

	public static void run(ArgumentParser parser) {
		int threads = parser.getInteger("-threads", 5);
		if (threads <= 0) {
			threads = 5;
		}
		try (IndexStore index = IndexOpener.openOrBuild(parser, threads)) {
			BenchmarkConfig config = BenchmarkConfig.from(parser);
			BenchmarkReport report = new BenchmarkRunner().run(index, config);
			System.out.println(report.oneLiner());
			System.out.println(report.details());
		} catch (IOException e) {
			System.out.println("Benchmark failed: " + e.getMessage());
		} catch (Exception e) {
			System.out.println("Benchmark failed.");
		}
	}
}
