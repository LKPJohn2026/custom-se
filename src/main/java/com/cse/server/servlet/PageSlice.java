package com.cse.server.servlet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Pagination helpers for index browsers.
 */
final class PageSlice {
	private PageSlice() {
	}

	static List<String> slice(Set<String> items, int page, int size) {
		List<String> sorted = new ArrayList<>(new TreeSet<>(items));
		int from = Math.min(page * size, sorted.size());
		int to = Math.min(from + size, sorted.size());
		return sorted.subList(from, to);
	}

	static String pager(String basePath, int page, int size, int total) {
		int pages = (total + size - 1) / size;
		if (pages <= 1) {
			return "<p>" + total + " item(s)</p>";
		}
		StringBuilder sb = new StringBuilder("<p>").append(total).append(" item(s) — page ")
				.append(page + 1).append(" of ").append(pages).append("</p><p>");
		if (page > 0) {
			sb.append("<a href=\"").append(basePath).append("?page=").append(page - 1)
					.append("&size=").append(size).append("\">Previous</a> ");
		}
		if (page + 1 < pages) {
			sb.append("<a href=\"").append(basePath).append("?page=").append(page + 1)
					.append("&size=").append(size).append("\">Next</a>");
		}
		sb.append("</p>");
		return sb.toString();
	}

	static int parsePage(String value) {
		try {
			int p = value == null ? 0 : Integer.parseInt(value);
			return Math.max(p, 0);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	static int parseSize(String value) {
		try {
			int s = value == null ? 100 : Integer.parseInt(value);
			if (s <= 0) {
				return 100;
			}
			return Math.min(s, 500);
		} catch (NumberFormatException e) {
			return 100;
		}
	}
}
