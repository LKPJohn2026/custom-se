package com.cse.ai.profile;

import com.cse.ai.embed.EmbeddingProvider;
import com.cse.ai.llm.ChatRequest;
import com.cse.ai.llm.LlmClient;
import com.cse.ai.llm.LlmException;

/**
 * Resolves the active {@link AiProfile} from server defaults and session preferences.
 */
public final class AiProfileResolver {
	private final AiSettings settings;
	private final AiProfileFactory factory;

	public AiProfileResolver(AiSettings settings) {
		this.settings = settings;
		this.factory = new AiProfileFactory(settings);
	}

	public AiProfile resolve(AiPreferences preferences) {
		String stackId = preferences == null
				? settings.defaultStack()
				: preferences.effectiveStackId(settings.defaultStack());
		return factory.build(stackId);
	}

	public java.util.List<AiProfileDescriptor> availableProfiles() {
		return factory.descriptors();
	}

	public boolean testEmbeddings(AiProfile profile) {
		try {
			EmbeddingProvider embed = profile.embeddings();
			float[] vector = embed.embed("connection test");
			return vector.length == embed.dimensions();
		} catch (RuntimeException e) {
			return false;
		}
	}

	public boolean testChat(AiProfile profile) {
		try {
			LlmClient chat = profile.chat();
			String answer = chat.completeChat(ChatRequest.of(
					"You are a test assistant.", "Reply with OK only."));
			return answer != null && !answer.isBlank();
		} catch (LlmException e) {
			return false;
		}
	}
}
