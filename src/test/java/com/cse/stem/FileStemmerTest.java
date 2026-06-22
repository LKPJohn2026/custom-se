package com.cse.stem;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Unit tests for {@link FileStemmer}.
 */
public class FileStemmerTest {

	/** Tests for the clean method. */
	@Nested
	public class CleanTests {
		/** Cleaning lowercases and strips non-letters. */
		@ParameterizedTest
		@CsvSource({
				"'Hello, World!', 'hello world'",
				"'ABC123', 'abc'",
				"'a-b-c', 'abc'",
				"'  spaced  ', '  spaced  '"
		})
		public void testClean(String input, String expected) {
			assertEquals(expected, FileStemmer.clean(input));
		}

		/** Diacritical marks are removed during cleaning. */
		@Test
		public void testCleanDiacritics() {
			assertEquals("uber", FileStemmer.clean("über"));
		}
	}

	/** Tests for the split and parse methods. */
	@Nested
	public class SplitParseTests {
		/** Blank text splits into an empty array. */
		@Test
		public void testSplitBlank() {
			assertArrayEquals(FileStemmer.EMPTY, FileStemmer.split("   "));
		}

		/** Parse cleans and splits into words. */
		@Test
		public void testParse() {
			assertArrayEquals(new String[] { "hello", "world" }, FileStemmer.parse("Hello, World! 123"));
		}

		/** Parsing empty text yields an empty array. */
		@Test
		public void testParseEmpty() {
			assertEquals(0, FileStemmer.parse("").length);
		}
	}

	/** Tests for stemming. */
	@Nested
	public class StemTests {
		/** listStems preserves order and stems words. */
		@Test
		public void testListStems() {
			List<String> stems = FileStemmer.listStems("cats running quickly");
			assertEquals(List.of("cat", "run", "quick"), stems);
		}

		/** uniqueStems returns a sorted, de-duplicated set. */
		@Test
		public void testUniqueStems() {
			Set<String> stems = FileStemmer.uniqueStems("cats cat cats running");
			assertEquals(Set.of("cat", "run"), stems);
		}

		/** uniqueStems are returned in sorted order. */
		@Test
		public void testUniqueStemsSorted() {
			// stems: zebra->zebra, apple->appl, mango->mango; sorted first is "appl"
			Set<String> stems = FileStemmer.uniqueStems("zebra apple mango");
			assertTrue(stems instanceof java.util.TreeSet);
			assertEquals("appl", stems.iterator().next());
		}
	}
}
