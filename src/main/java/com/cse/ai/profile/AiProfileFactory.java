package com.cse.ai.profile;

import java.util.ArrayList;
import java.util.List;

import com.cse.ai.embed.EmbeddingProvider;
import com.cse.ai.embed.OllamaEmbeddingProvider;
import com.cse.ai.embed.OpenAiCompatibleEmbeddingProvider;
import com.cse.ai.embed.OpenAiEmbeddingProvider;
import com.cse.ai.llm.AnthropicLlmClient;
import com.cse.ai.llm.LlmClient;
import com.cse.ai.llm.OllamaLlmClient;
import com.cse.ai.llm.OpenAiCompatibleLlmClient;
import com.cse.ai.llm.OpenAiLlmClient;

/**
 * Builds {@link AiProfile} instances from {@link AiSettings}.
 */
public final class AiProfileFactory {
	private final AiSettings settings;

	public AiProfileFactory(AiSettings settings) {
		this.settings = settings;
	}

	public AiProfile build(String stackId) {
		return switch (stackId) {
			case "openai" -> openAiStack();
			case "lmstudio" -> lmStudioStack();
			case "claude" -> claudeStack();
			default -> ollamaStack();
		};
	}

	public List<AiProfileDescriptor> descriptors() {
		List<AiProfileDescriptor> list = new ArrayList<>();
		list.add(new AiProfileDescriptor("ollama", "Ollama (local)", true, "Local embeddings and chat"));
		list.add(new AiProfileDescriptor("lmstudio", "LM Studio (local)", true, "OpenAI-compatible local server"));
		list.add(new AiProfileDescriptor("openai", "OpenAI (cloud)",
				!settings.openAiApiKey().isBlank(), "Requires OPENAI_API_KEY"));
		list.add(new AiProfileDescriptor("claude", "Claude (cloud)",
				!settings.anthropicApiKey().isBlank(),
				"Chat via Claude; embeddings from " + settings.claudeEmbeddingStack()));
		return list;
	}

	private AiProfile ollamaStack() {
		EmbeddingProvider embed = new OllamaEmbeddingProvider(settings.ollamaBaseUrl(),
				settings.ollamaEmbeddingModel(), settings.ollamaEmbeddingDimensions());
		LlmClient chat = new OllamaLlmClient(settings.ollamaBaseUrl(), settings.ollamaChatModel());
		return new AiProfile("ollama", "Ollama", embed, chat, false);
	}

	private AiProfile openAiStack() {
		EmbeddingProvider embed = new OpenAiEmbeddingProvider(settings.openAiApiKey(),
				settings.openaiEmbeddingModel(), settings.openaiEmbeddingDimensions());
		LlmClient chat = new OpenAiLlmClient(settings.openAiApiKey(), settings.openaiChatModel());
		return new AiProfile("openai", "OpenAI", embed, chat, false);
	}

	private AiProfile lmStudioStack() {
		String embedModel = settings.lmstudioEmbeddingModel().isBlank() ? "local-embed"
				: settings.lmstudioEmbeddingModel();
		String chatModel = settings.lmstudioChatModel().isBlank() ? "local-chat" : settings.lmstudioChatModel();
		EmbeddingProvider embed = new OpenAiCompatibleEmbeddingProvider(settings.lmstudioBaseUrl(), embedModel,
				settings.lmstudioEmbeddingDimensions());
		LlmClient chat = new OpenAiCompatibleLlmClient(settings.lmstudioBaseUrl(), chatModel);
		return new AiProfile("lmstudio", "LM Studio", embed, chat, false);
	}

	private AiProfile claudeStack() {
		EmbeddingProvider embed = buildEmbeddingStack(settings.claudeEmbeddingStack());
		LlmClient chat = new AnthropicLlmClient(settings.anthropicApiKey(), settings.claudeChatModel());
		return new AiProfile("claude", "Claude", embed, chat, true);
	}

	private EmbeddingProvider buildEmbeddingStack(String stackId) {
		return switch (stackId) {
			case "openai" -> new OpenAiEmbeddingProvider(settings.openAiApiKey(),
					settings.openaiEmbeddingModel(), settings.openaiEmbeddingDimensions());
			default -> new OllamaEmbeddingProvider(settings.ollamaBaseUrl(),
					settings.ollamaEmbeddingModel(), settings.ollamaEmbeddingDimensions());
		};
	}
}
