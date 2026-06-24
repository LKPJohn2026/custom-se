package com.cse.index;

/**
 * A parsed search request.
 */
public record SearchQuery(String raw, QueryMode mode) {
}
