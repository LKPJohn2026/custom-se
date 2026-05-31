package edu.usfca.cs272;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link ArgumentParser}.
 */
public class ArgumentParserTest {

	/** Tests for flag detection. */
	@Nested
	public class FlagTests {
		/** Strings starting with a dash and a non-digit are flags. */
		@ParameterizedTest
		@ValueSource(strings = { "-text", "-threads", "-@world", "-a" })
		public void testIsFlag(String arg) {
			assertTrue(ArgumentParser.isFlag(arg));
		}

		/** Values, negative numbers, null, and dashes are not flags. */
		@ParameterizedTest
		@ValueSource(strings = { "text", "-10", "-", "- hello", "" })
		public void testIsNotFlag(String arg) {
			assertFalse(ArgumentParser.isFlag(arg));
		}

		/** A null argument is not a flag. */
		@Test
		public void testNullNotFlag() {
			assertFalse(ArgumentParser.isFlag(null));
		}
	}

	/** Tests for parsing flag/value pairs. */
	@Nested
	public class ParseTests {
		/** A flag with a following value is stored together. */
		@Test
		public void testFlagValuePair() {
			ArgumentParser parser = new ArgumentParser(new String[] { "-text", "input.txt" });

			assertTrue(parser.hasFlag("-text"));
			assertTrue(parser.hasValue("-text"));
			assertEquals("input.txt", parser.getString("-text"));
		}

		/** A flag with no value maps to null. */
		@Test
		public void testFlagWithoutValue() {
			ArgumentParser parser = new ArgumentParser(new String[] { "-partial" });

			assertTrue(parser.hasFlag("-partial"));
			assertFalse(parser.hasValue("-partial"));
			assertNull(parser.getString("-partial"));
		}

		/** A repeated flag keeps the last value. */
		@Test
		public void testRepeatedFlag() {
			ArgumentParser parser = new ArgumentParser(new String[] { "-p", "one", "-p", "two" });

			assertEquals("two", parser.getString("-p"));
			assertEquals(1, parser.numFlags());
		}

		/** numFlags counts unique flags only. */
		@Test
		public void testNumFlags() {
			ArgumentParser parser = new ArgumentParser(new String[] { "-a", "1", "-b", "2", "-c" });
			assertEquals(3, parser.numFlags());
		}
	}

	/** Tests for typed value retrieval with backups. */
	@Nested
	public class TypedValueTests {
		/** getString returns the backup when the flag is missing. */
		@Test
		public void testStringBackup() {
			ArgumentParser parser = new ArgumentParser(new String[] {});
			assertEquals("default", parser.getString("-missing", "default"));
		}

		/** getInteger parses valid numbers and falls back otherwise. */
		@Test
		public void testIntegerParsing() {
			ArgumentParser parser = new ArgumentParser(new String[] { "-threads", "8", "-bad", "abc" });

			assertEquals(8, parser.getInteger("-threads", 5));
			assertEquals(5, parser.getInteger("-bad", 5));
			assertEquals(5, parser.getInteger("-missing", 5));
		}

		/** getPath converts values and never throws on bad input. */
		@Test
		public void testPathParsing() {
			ArgumentParser parser = new ArgumentParser(new String[] { "-text", "input.txt" });

			assertEquals(Path.of("input.txt"), parser.getPath("-text"));
			assertEquals(Path.of("backup"), parser.getPath("-missing", Path.of("backup")));
			assertNull(parser.getPath("-missing"));
		}
	}
}
