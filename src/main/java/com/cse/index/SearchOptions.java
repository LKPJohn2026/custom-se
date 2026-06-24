package com.cse.index;

/**
 * Pagination and display options for search.
 */
public record SearchOptions(int limit, int offset, boolean reverse, boolean lucky) {
	public static final int DEFAULT_LIMIT = 50;
	public static final int MAX_LIMIT = 500;

	public SearchOptions {
		if (limit <= 0) {
			limit = DEFAULT_LIMIT;
		}
		if (limit > MAX_LIMIT) {
			limit = MAX_LIMIT;
		}
		if (offset < 0) {
			offset = 0;
		}
	}

	public static SearchOptions defaults() {
		return new SearchOptions(DEFAULT_LIMIT, 0, false, false);
	}
}
