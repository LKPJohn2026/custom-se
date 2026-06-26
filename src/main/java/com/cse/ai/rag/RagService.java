package com.cse.ai.rag;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import com.cse.ai.llm.ChatRequest;
import com.cse.ai.profile.AiProfile;
import com.cse.ai.profile.AiSettings;
import com.cse.index.IndexStore;
import com.cse.index.QueryMode;
import com.cse.index.SearchQuery;
import com.cse.server.meta.MetadataStore;
import com.cse.server.session.UserSessionData;

/**
 * Retrieves grounded context and synthesizes answers via an {@link AiProfile}.
 */
public final class RagService {
	private final IndexStore index;
	private final AiSettings settings;
	private final MetadataStore metadata;

	public RagService(IndexStore index, AiSettings settings) {
		this(index, settings, null);
	}

	public RagService(IndexStore index, AiSettings settings, MetadataStore metadata) {
		this.index = index;
		this.settings = settings;
		this.metadata = metadata;
	}

	public RagResponse ask(String question, AiProfile profile, UserSessionData session) throws IOException {
		long retrievalStart = System.nanoTime();
		HybridRetriever retriever = new HybridRetriever(index, profile.embeddings());
		List<ScoredChunk> sources = retriever.retrieve(new SearchQuery(question.strip(), QueryMode.EXACT),
				settings.ragTopK());
		long retrievalMs = (System.nanoTime() - retrievalStart) / 1_000_000;

		int maxChars = settings.ragMaxContextTokens() * 4;
		ChatRequest request = ChatRequest.of(
				PromptBuilder.systemPrompt(),
				PromptBuilder.userPrompt(sources, question, maxChars));

		long generationStart = System.nanoTime();
		String answer = profile.chat().completeChat(request);
		long generationMs = (System.nanoTime() - generationStart) / 1_000_000;

		if (session != null && !session.isPrivateSearch() && metadata != null) {
			metadata.recordQuery(question.strip());
		}

		return new RagResponse(answer, sources, retrievalMs, generationMs, profile.id());
	}

	public Stream<String> askStream(String question, AiProfile profile) throws IOException {
		HybridRetriever retriever = new HybridRetriever(index, profile.embeddings());
		List<ScoredChunk> sources = retriever.retrieve(question, settings.ragTopK());
		int maxChars = settings.ragMaxContextTokens() * 4;
		ChatRequest request = ChatRequest.of(
				PromptBuilder.systemPrompt(),
				PromptBuilder.userPrompt(sources, question, maxChars));
		return profile.chat().streamChat(request);
	}
}
