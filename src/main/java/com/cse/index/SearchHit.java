package com.cse.index;

/**
 * One search result at the application boundary.
 */
public record SearchHit(
		String location,
		double score,
		int matchCount,
		String snippet) implements Comparable<SearchHit> {

	public SearchHit(String location, double score, int matchCount) {
		this(location, score, matchCount, null);
	}

	@Override
	public int compareTo(SearchHit other) {
		int cmp = Double.compare(other.score, this.score);
		if (cmp != 0) {
			return cmp;
		}
		return this.location.compareTo(other.location);
	}
}
