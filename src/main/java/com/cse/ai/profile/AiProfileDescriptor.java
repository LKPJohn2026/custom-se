package com.cse.ai.profile;

/**
 * Describes a built-in AI stack for the settings UI.
 */
public record AiProfileDescriptor(
		String id,
		String displayName,
		boolean available,
		String note) {
}
