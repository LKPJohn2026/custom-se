package com.cse.index;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.cse.index.InvertedIndex.SearchResult;

/**
 * Unit tests for {@link InvertedIndex}.
 */
public class InvertedIndexTest {

	/** Tests for adding words and the resulting structure. */
	@Nested
	public class AddWordTests {
		/** A single added word is reflected in stems, locations, and positions. */
		@Test
		public void testAddSingleWord() {
			InvertedIndex index = new InvertedIndex();
			index.addWord("apple", "a.txt", 1);

			assertTrue(index.containsStem("apple"));
			assertTrue(index.containsLocation("apple", "a.txt"));
			assertTrue(index.containsPosition("apple", "a.txt", 1));
			assertEquals(1, index.totalStems());
		}

		/** Duplicate positions for the same word/location are not double-counted. */
		@Test
		public void testAddDuplicatePosition() {
			InvertedIndex index = new InvertedIndex();
			index.addWord("apple", "a.txt", 1);
			index.addWord("apple", "a.txt", 1);

			assertEquals(1, index.totalPositions("apple", "a.txt"));
		}

		/** The count for a location tracks the highest position seen. */
		@Test
		public void testCountTracksHighestPosition() {
			InvertedIndex index = new InvertedIndex();
			index.addWord("apple", "a.txt", 1);
			index.addWord("banana", "a.txt", 5);

			assertEquals(5, index.getCount("a.txt"));
		}

		/** addAllWords adds a list with incrementing positions. */
		@Test
		public void testAddAllWords() {
			InvertedIndex index = new InvertedIndex();
			index.addAllWords(List.of("a", "b", "c"), "a.txt", 1);

			assertTrue(index.containsPosition("a", "a.txt", 1));
			assertTrue(index.containsPosition("b", "a.txt", 2));
			assertTrue(index.containsPosition("c", "a.txt", 3));
		}
	}

	/** Tests for querying an empty or absent entry. */
	@Nested
	public class EmptyAndAbsentTests {
		/** A fresh index reports empty totals and no membership. */
		@Test
		public void testEmptyIndex() {
			InvertedIndex index = new InvertedIndex();

			assertEquals(0, index.totalStems());
			assertEquals(0, index.totalCounts());
			assertFalse(index.containsStem("missing"));
			assertEquals(0, index.getCount("missing"));
		}

		/** Absent stems and locations return empty, unmodifiable collections. */
		@Test
		public void testAbsentLookups() {
			InvertedIndex index = new InvertedIndex();

			assertTrue(index.getLocations("missing").isEmpty());
			assertTrue(index.getPositions("missing", "a.txt").isEmpty());
			assertEquals(0, index.totalLocations("missing"));
			assertFalse(index.containsLocation("a.txt"));
		}
	}

	/** Tests for merging two indexes via addAll. */
	@Nested
	public class AddAllTests {
		/** Merging a disjoint index combines all stems. */
		@Test
		public void testAddAllDisjoint() {
			InvertedIndex first = new InvertedIndex();
			first.addWord("apple", "a.txt", 1);

			InvertedIndex second = new InvertedIndex();
			second.addWord("banana", "b.txt", 1);

			first.addAll(second);

			assertTrue(first.containsStem("apple"));
			assertTrue(first.containsStem("banana"));
			assertEquals(2, first.totalStems());
		}

		/** Merging overlapping entries unions the positions. */
		@Test
		public void testAddAllOverlapping() {
			InvertedIndex first = new InvertedIndex();
			first.addWord("apple", "a.txt", 1);

			InvertedIndex second = new InvertedIndex();
			second.addWord("apple", "a.txt", 3);

			first.addAll(second);

			assertEquals(2, first.totalPositions("apple", "a.txt"));
			assertTrue(first.containsPosition("apple", "a.txt", 1));
			assertTrue(first.containsPosition("apple", "a.txt", 3));
		}
	}

	/** Tests for exact and partial search. */
	@Nested
	public class SearchTests {
		/** Builds a small index used across search tests. */
		private InvertedIndex build() {
			InvertedIndex index = new InvertedIndex();
			index.addWord("apple", "a.txt", 1);
			index.addWord("apricot", "a.txt", 2);
			index.addWord("apple", "b.txt", 1);
			index.addWord("banana", "b.txt", 2);
			return index;
		}

		/** Exact search only matches whole stems. */
		@Test
		public void testExactSearch() {
			InvertedIndex index = build();
			List<SearchResult> results = index.exactIndex(Set.of("apple"));

			assertEquals(2, results.size());
		}

		/** Exact search does not match by prefix. */
		@Test
		public void testExactSearchNoPrefix() {
			InvertedIndex index = build();
			List<SearchResult> results = index.exactIndex(Set.of("ap"));

			assertTrue(results.isEmpty());
		}

		/** Partial search matches stems by prefix. */
		@Test
		public void testPartialSearch() {
			InvertedIndex index = build();
			List<SearchResult> results = index.partialIndex(Set.of("ap"));

			// a.txt has apple+apricot (2 matches), b.txt has apple (1 match)
			assertEquals(2, results.size());
			assertEquals("a.txt", results.get(0).getLocation());
			assertEquals(2, results.get(0).getMatches());
		}

		/** Results are sorted by score descending. */
		@Test
		public void testResultsSortedByScore() {
			InvertedIndex index = build();
			List<SearchResult> results = index.partialIndex(Set.of("ap"));

			assertTrue(results.get(0).getScore() >= results.get(1).getScore());
		}

		/** Score equals matches divided by the location word count. */
		@Test
		public void testScoreCalculation() {
			InvertedIndex index = build();
			List<SearchResult> results = index.exactIndex(Set.of("apple"));

			// each location has 2 words, apple matches once -> score 0.5
			for (SearchResult result : results) {
				assertEquals(0.5, result.getScore(), 1e-9);
			}
		}
	}
}
