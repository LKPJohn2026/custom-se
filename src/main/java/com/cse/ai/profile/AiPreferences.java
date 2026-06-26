package com.cse.ai.profile;

import java.io.Serializable;

/**
 * Per-session AI stack selection.
 */
public record AiPreferences(
		String stackId,
		String embeddingProvider,
		String chatProvider,
		String embeddingModel,
		String chatModel) implements Serializable {

	private static final long serialVersionUID = 1L;

	public static AiPreferences defaults(String stackId) {
		return new AiPreferences(stackId, "", "", "", "");
	}

	public String effectiveStackId(String serverDefault) {
		if (stackId != null && !stackId.isBlank()) {
			return stackId;
		}
		return serverDefault;
	}
}
