package com.cse.index;

/**
 * One document indexed by {@link IndexStore}.
 */
public record IndexDocument(
		String id,
		String location,
		String title,
		String body,
		long indexedAt) {
}
