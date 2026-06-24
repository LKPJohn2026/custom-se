package com.cse.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.cse.cli.ArgumentParser;
import com.cse.index.IndexStore;
import com.cse.index.lucene.LuceneIndexStore;

/**
 * Opens or creates a Lucene index directory from CLI flags.
 */
public final class IndexOpener {
	private IndexOpener() {
	}

	public static Path resolveIndexDir(ArgumentParser parser) {
		if (parser.hasFlag("-index-dir") && parser.hasValue("-index-dir")) {
			return parser.getPath("-index-dir", IndexBuilder.DEFAULT_INDEX_DIR);
		}
		return IndexBuilder.DEFAULT_INDEX_DIR;
	}

	public static boolean shouldLoadExisting(ArgumentParser parser, Path indexDir) {
		if (parser.hasFlag("-load-index")) {
			return true;
		}
		return Files.isDirectory(indexDir) && hasLuceneSegments(indexDir);
	}

	public static IndexStore open(ArgumentParser parser) throws IOException {
		Path indexDir = resolveIndexDir(parser);
		LuceneIndexStore store = new LuceneIndexStore();
		store.open(indexDir);
		return store;
	}

	private static boolean hasLuceneSegments(Path indexDir) {
		Path[] check = { indexDir.resolve("segments_1"), indexDir };
		try (var stream = Files.list(indexDir)) {
			return stream.anyMatch(p -> p.getFileName().toString().startsWith("segments_"));
		} catch (IOException e) {
			return false;
		}
	}
}
