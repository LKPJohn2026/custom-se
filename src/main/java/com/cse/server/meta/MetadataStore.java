package com.cse.server.meta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Server-wide metadata (non-persistent, in-memory).
 */
public class MetadataStore {
	private final Map<String, AtomicLong> queryCounts = new ConcurrentHashMap<>();
	private final Map<String, AtomicLong> visitCounts = new ConcurrentHashMap<>();
	private final Map<String, PageMetadata> pages = new ConcurrentHashMap<>();

	public void recordQuery(String query) {
		if (query == null || query.isBlank()) {
			return;
		}
		String key = query.strip().toLowerCase();
		queryCounts.computeIfAbsent(key, k -> new AtomicLong()).incrementAndGet();
	}

	public void recordVisit(String location) {
		if (location == null || location.isBlank()) {
			return;
		}
		visitCounts.computeIfAbsent(location, k -> new AtomicLong()).incrementAndGet();
	}

	public void putPage(String location, PageMetadata meta) {
		if (location != null && meta != null) {
			pages.put(location, meta);
		}
	}

	public PageMetadata getPage(String location) {
		return pages.get(location);
	}

	public List<Map.Entry<String, Long>> topQueries(int n) {
		return topN(queryCounts, n);
	}

	public List<Map.Entry<String, Long>> topVisits(int n) {
		return topN(visitCounts, n);
	}

	public void resetAll() {
		queryCounts.clear();
		visitCounts.clear();
		pages.clear();
	}

	private static List<Map.Entry<String, Long>> topN(Map<String, AtomicLong> map, int n) {
		List<Map.Entry<String, Long>> list = new ArrayList<>();
		for (var e : map.entrySet()) {
			list.add(Map.entry(e.getKey(), e.getValue().get()));
		}
		list.sort(Comparator.<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue).reversed());
		if (list.size() > n) {
			return list.subList(0, n);
		}
		return list;
	}
}
