package com.cse.ai.chunk;

import java.util.List;

import com.cse.index.IndexDocument;

/**
 * Splits a document body into retrieval-sized chunks.
 */
public interface Chunker {

	List<Chunk> chunk(IndexDocument doc, ChunkingOptions options);
}
