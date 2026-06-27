package com.cse.ai.embed;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.cse.ai.chunk.Chunk;
import com.cse.index.IndexStore;
import com.cse.server.EmbedJobManager;

/**
 * Re-embeds all chunk documents in the index with the given provider.
 */
public final class EmbeddingIndexJob {
	private static final int BATCH_SIZE = 32;

	private EmbeddingIndexJob() {
	}

	public static void run(IndexStore store, EmbeddingProvider embedder, EmbedJobManager jobs) {
		try {
			List<Chunk> all = store.listAllChunks();
			if (all.isEmpty()) {
				jobs.finish(0, "No chunks to re-embed");
				return;
			}
			jobs.start(all.size());
			for (int i = 0; i < all.size(); i += BATCH_SIZE) {
				int end = Math.min(i + BATCH_SIZE, all.size());
				List<Chunk> batch = all.subList(i, end);
				List<String> texts = batch.stream().map(Chunk::text).toList();
				List<float[]> vectors = embedder.embedBatchDocuments(texts);
				store.addChunks(batch, vectors, embedder);
				jobs.progress(end);
			}
			store.commit();
			jobs.finish(all.size(), "Re-embedded " + all.size() + " chunks");
		} catch (IOException | RuntimeException e) {
			jobs.fail(e.getMessage() == null ? "Re-embed failed" : e.getMessage());
		}
	}

	public static int runSync(IndexStore store, EmbeddingProvider embedder) throws IOException {
		List<Chunk> all = store.listAllChunks();
		if (all.isEmpty()) {
			return 0;
		}
		for (int i = 0; i < all.size(); i += BATCH_SIZE) {
			int end = Math.min(i + BATCH_SIZE, all.size());
			List<Chunk> batch = new ArrayList<>(all.subList(i, end));
			List<String> texts = batch.stream().map(Chunk::text).toList();
			List<float[]> vectors = embedder.embedBatchDocuments(texts);
			store.addChunks(batch, vectors, embedder);
		}
		store.commit();
		return all.size();
	}
}
