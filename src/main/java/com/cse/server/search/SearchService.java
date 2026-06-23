package com.cse.server.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.cse.index.InvertedIndex.SearchResult;
import com.cse.index.ThreadSafeInvertedIndex;
import com.cse.server.AppContext;
import com.cse.server.session.UserSessionData;
import com.cse.stem.FileStemmer;

/**
 * Server-side search orchestration with session and metadata tracking.
 */
public final class SearchService {
	private SearchService() {
	}

	public record SearchResponse(List<SearchResult> results, long elapsedMs, String luckyRedirect) {
	}

	public static SearchResponse search(AppContext app, UserSessionData session, String query,
			boolean partial, boolean reverse, boolean lucky) {
		long start = System.nanoTime();
		app.stats().recordQuery();

		if (query == null || query.isBlank()) {
			return new SearchResponse(Collections.emptyList(), 0, null);
		}

		String q = query.strip();
		if (session != null && !session.isPrivateSearch()) {
			session.addSearch(q);
		}
		app.metadata().recordQuery(q);

		Set<String> stems = FileStemmer.uniqueStems(q);
		List<SearchResult> results = partial
				? app.index().partialIndex(stems)
				: app.index().exactIndex(stems);

		if (reverse) {
			results = new ArrayList<>(results);
			Collections.reverse(results);
		}

		long elapsed = (System.nanoTime() - start) / 1_000_000;
		String redirect = null;
		if (lucky && !results.isEmpty()) {
			redirect = results.get(0).getLocation();
		}
		return new SearchResponse(results, elapsed, redirect);
	}
}
