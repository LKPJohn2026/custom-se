package com.cse.index.lucene;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cse.index.IndexAiMetadata;

/**
 * Reads and writes {@code meta.json} next to the Lucene index directory.
 */
final class IndexMetadataIO {
	private static final Pattern INT_FIELD = Pattern.compile("\"(\\w+)\"\\s*:\\s*(-?\\d+)");
	private static final Pattern STRING_FIELD = Pattern.compile("\"(\\w+)\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
	private static final Pattern CHUNK_TARGET = Pattern.compile("\"targetChars\"\\s*:\\s*(\\d+)");
	private static final Pattern CHUNK_OVERLAP = Pattern.compile("\"overlapChars\"\\s*:\\s*(\\d+)");

	private IndexMetadataIO() {
	}

	static void write(Path metaFile, IndexAiMetadata meta) throws IOException {
		String json = """
				{
				  "indexVersion": %d,
				  "embeddingProvider": "%s",
				  "embeddingModel": "%s",
				  "vectorDimensions": %d,
				  "chunking": { "targetChars": %d, "overlapChars": %d },
				  "indexedAt": "%s"
				}
				""".formatted(
				meta.indexVersion(),
				escape(meta.embeddingProvider()),
				escape(meta.embeddingModel()),
				meta.vectorDimensions(),
				meta.chunkTargetChars(),
				meta.chunkOverlapChars(),
				escape(meta.indexedAt()));
		Files.writeString(metaFile, json);
	}

	static Optional<IndexAiMetadata> read(Path metaFile) throws IOException {
		if (!Files.isRegularFile(metaFile)) {
			return Optional.empty();
		}
		String json = Files.readString(metaFile);
		int indexVersion = intValue(json, "indexVersion", 0);
		String embeddingProvider = stringValue(json, "embeddingProvider");
		String embeddingModel = stringValue(json, "embeddingModel");
		int vectorDimensions = intValue(json, "vectorDimensions", 0);
		int targetChars = chunkTarget(json);
		int overlapChars = chunkOverlap(json);
		String indexedAt = stringValue(json, "indexedAt");
		return Optional.of(new IndexAiMetadata(indexVersion, embeddingProvider, embeddingModel,
				vectorDimensions, targetChars, overlapChars, indexedAt));
	}

	private static int intValue(String json, String field, int defaultValue) {
		Matcher matcher = INT_FIELD.matcher(json);
		while (matcher.find()) {
			if (field.equals(matcher.group(1))) {
				return Integer.parseInt(matcher.group(2));
			}
		}
		return defaultValue;
	}

	private static String stringValue(String json, String field) {
		Matcher matcher = STRING_FIELD.matcher(json);
		while (matcher.find()) {
			if (field.equals(matcher.group(1))) {
				return unescape(matcher.group(2));
			}
		}
		return "";
	}

	private static int chunkTarget(String json) {
		Matcher matcher = CHUNK_TARGET.matcher(json);
		if (matcher.find()) {
			return Integer.parseInt(matcher.group(1));
		}
		return 2000;
	}

	private static int chunkOverlap(String json) {
		Matcher matcher = CHUNK_OVERLAP.matcher(json);
		if (matcher.find()) {
			return Integer.parseInt(matcher.group(1));
		}
		return 200;
	}

	private static String escape(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	private static String unescape(String value) {
		return value.replace("\\\"", "\"").replace("\\\\", "\\");
	}
}
