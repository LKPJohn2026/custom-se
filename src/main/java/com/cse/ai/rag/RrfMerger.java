package com.cse.ai.rag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reciprocal rank fusion across lexical and vector result lists.
 */
public final class RrfMerger {
	private static final int K = 60;

	private RrfMerger() {
	}

	public static List<ScoredChunk> merge(List<ScoredChunk> lexical, List<ScoredChunk> vector, int topK) {
		Map<String, ChunkScores> scores = new HashMap<>();
		accumulate(scores, lexical, true);
		accumulate(scores, vector, false);
		return scores.values().stream()
				.sorted((a, b) -> Double.compare(b.rrfScore, a.rrfScore))
				.limit(topK)
				.map(ChunkScores::toScoredChunk)
				.toList();
	}

	private static void accumulate(Map<String, ChunkScores> scores, List<ScoredChunk> hits, boolean lexical) {
		for (int rank = 0; rank < hits.size(); rank++) {
			ScoredChunk hit = hits.get(rank);
			double contribution = 1.0 / (K + rank + 1);
			ChunkScores entry = scores.computeIfAbsent(hit.chunk().chunkId(),
					id -> new ChunkScores(hit.chunk()));
			entry.rrfScore += contribution;
			if (lexical) {
				entry.lexicalScore = Math.max(entry.lexicalScore, hit.lexicalScore());
			} else {
				entry.vectorScore = Math.max(entry.vectorScore, hit.vectorScore());
			}
		}
	}

	private static final class ChunkScores {
		private final com.cse.ai.chunk.Chunk chunk;
		private double rrfScore;
		private double lexicalScore;
		private double vectorScore;

		private ChunkScores(com.cse.ai.chunk.Chunk chunk) {
			this.chunk = chunk;
		}

		private ScoredChunk toScoredChunk() {
			return new ScoredChunk(chunk, rrfScore, lexicalScore, vectorScore);
		}
	}
}
