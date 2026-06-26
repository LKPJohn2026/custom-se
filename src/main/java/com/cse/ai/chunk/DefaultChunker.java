package com.cse.ai.chunk;

import java.util.ArrayList;
import java.util.List;

import com.cse.index.IndexDocument;

/**
 * Splits document bodies into overlapping character windows, breaking at whitespace
 * when possible.
 */
public final class DefaultChunker implements Chunker {

	@Override
	public List<Chunk> chunk(IndexDocument doc, ChunkingOptions options) {
		String body = doc.body() == null ? "" : doc.body();
		if (body.isBlank()) {
			return List.of(toChunk(doc, 0, "", 0));
		}

		List<Chunk> chunks = new ArrayList<>();
		int start = 0;
		int sequence = 0;
		while (start < body.length() && sequence < options.maxChunksPerDoc()) {
			int end = Math.min(start + options.targetChars(), body.length());
			if (end < body.length()) {
				int breakAt = body.lastIndexOf(' ', end);
				if (breakAt > start) {
					end = breakAt;
				}
			}
			String text = body.substring(start, end).strip();
			if (!text.isEmpty()) {
				chunks.add(toChunk(doc, sequence, text, start));
				sequence++;
			}
			if (end >= body.length()) {
				break;
			}
			start = Math.max(end - options.overlapChars(), start + 1);
		}
		if (chunks.isEmpty()) {
			chunks.add(toChunk(doc, 0, body.strip(), 0));
		}
		return chunks;
	}

	private static Chunk toChunk(IndexDocument doc, int sequence, String text, int charOffset) {
		String title = doc.title() == null ? "" : doc.title();
		String chunkId = doc.id() + "#" + sequence;
		return new Chunk(chunkId, doc.id(), doc.location(), title, text, sequence, charOffset, doc.indexedAt());
	}
}
