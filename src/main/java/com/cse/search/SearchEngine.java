package com.cse.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.cse.index.IndexStore;
import com.cse.index.QueryMode;
import com.cse.index.SearchHit;
import com.cse.index.SearchOptions;
import com.cse.index.SearchQuery;
import com.cse.server.ServerStats;
import com.cse.server.meta.MetadataStore;
import com.cse.server.session.UserSessionData;

/**
 * Unified search orchestration for CLI and server.
 */
public class SearchEngine {
	private final IndexStore index;
	private final MetadataStore metadata;
	private final ServerStats stats;

	public SearchEngine(IndexStore index) {
		this(index, null, null);
	}

	public SearchEngine(IndexStore index, MetadataStore metadata, ServerStats stats) {
		this.index = index;
		this.metadata = metadata;
		this.stats = stats;
	}

	public record SearchResponse(List<SearchHit> results, long elapsedMs, String luckyRedirect) {
	}

	public SearchResponse search(SearchQuery query, SearchOptions options, UserSessionData session)
			throws IOException {
		long start = System.nanoTime();
		if (stats != null) {
			stats.recordQuery();
		}

		String raw = query.raw();
		if (raw == null || raw.isBlank()) {
			return new SearchResponse(List.of(), 0, null);
		}

		String q = raw.strip();
		if (session != null && !session.isPrivateSearch()) {
			session.addSearch(q);
		}
		if (metadata != null) {
			metadata.recordQuery(q);
		}

		List<SearchHit> results = new ArrayList<>(index.search(query, options));
		if (options.reverse()) {
			Collections.reverse(results);
		}

		long elapsed = (System.nanoTime() - start) / 1_000_000;
		String redirect = null;
		if (options.lucky() && !results.isEmpty()) {
			redirect = results.get(0).location();
		}
		return new SearchResponse(results, elapsed, redirect);
	}

	public static SearchQuery parseQuery(String raw, boolean partial) {
		if (raw == null) {
			return new SearchQuery("", QueryMode.EXACT);
		}
		String text = raw.strip();
		if (text.length() >= 2 && text.startsWith("\"") && text.endsWith("\"")) {
			return new SearchQuery(text, QueryMode.PHRASE);
		}
		return new SearchQuery(text, partial ? QueryMode.PARTIAL : QueryMode.EXACT);
	}
}
