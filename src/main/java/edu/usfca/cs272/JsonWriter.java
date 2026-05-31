package edu.usfca.cs272;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import edu.usfca.cs272.InvertedIndex.SearchResult;

/**
 * Outputs several simple data structures in "pretty" JSON format where newlines
 * are used to separate elements and nested elements are indented using spaces.
 *
 * Warning: This class is not thread-safe. If multiple threads access this class
 * concurrently, access must be synchronized externally.
 *
 */
public class JsonWriter {
	/**
	 * Indents the writer by the specified number of times. Does nothing if the
	 * indentation level is 0 or less.
	 *
	 * @param writer the writer to use
	 * @param indent the number of times to indent
	 * @throws IOException if an I/O error occurs while writing
	 */
	public static void writeIndent(Writer writer, int indent) throws IOException {
		while (indent-- > 0) {
			writer.write("  ");
		}
	}

	/**
	 * Indents and then writes the String element.
	 *
	 * @param element the element to write
	 * @param writer  the writer to use
	 * @param indent  the number of times to indent
	 * @throws IOException if an I/O error occurs while writing
	 */
	public static void writeIndent(String element, Writer writer, int indent) throws IOException {
		writeIndent(writer, indent);
		writer.write(element);
	}

	/**
	 * Indents and then writes the text element surrounded by {@code " "} quotation
	 * marks.
	 *
	 * @param element the element to write
	 * @param writer  the writer to use
	 * @param indent  the number of times to indent
	 * @throws IOException if an IO error occurs
	 */
	public static void writeQuote(String element, Writer writer, int indent) throws IOException {
		writeIndent(writer, indent);
		writer.write('"');
		writer.write(element);
		writer.write('"');
	}

	/**
	 * Writes the elements as a pretty JSON array.
	 *
	 * @param elements the elements to write
	 * @param writer   the writer to use
	 * @param indent   the initial indent level; the first bracket is not indented,
	 *                 inner elements are indented by one, and the last bracket is
	 *                 indented at the initial indentation level
	 * @throws IOException if an IO error occurs
	 *
	 * @see Writer#write(String)
	 * @see #writeIndent(Writer, int)
	 * @see #writeIndent(String, Writer, int)
	 */
	public static void writeArray(Collection<? extends Number> elements, Writer writer, int indent) throws IOException {
		writeIndent("[", writer, 0);
		if (!elements.isEmpty()) {
			writer.write("\n");
			var iterator = elements.iterator();
			Number firstElement = iterator.next();
			writeIndent(firstElement.toString(), writer, indent + 1);

			String seperator = ",\n";
			while (iterator.hasNext()) {
				writer.write(seperator);
				Number nextElement = iterator.next();
				writeIndent(nextElement.toString(), writer, indent + 1);
			}
		}
		writer.write("\n");
		writeIndent("]", writer, indent);
	}

	/**
	 * Writes the elements as a pretty JSON array to file.
	 *
	 * @param elements the elements to write
	 * @param path     the file path to use
	 * @throws IOException if an IO error occurs
	 *
	 * @see Files#newBufferedReader(Path, java.nio.charset.Charset)
	 * @see StandardCharsets#UTF_8
	 * @see #writeArray(Collection, Writer, int)
	 */
	public static void writeArray(Collection<? extends Number> elements, Path path) throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(path, UTF_8)) {
			writeArray(elements, writer, 0);
		}
	}

	/**
	 * Returns the elements as a pretty JSON array.
	 *
	 * @param elements the elements to use
	 * @return a {@link String} containing the elements in pretty JSON format
	 *
	 * @see StringWriter
	 * @see #writeArray(Collection, Writer, int)
	 */
	public static String writeArray(Collection<? extends Number> elements) {
		try {
			StringWriter writer = new StringWriter();
			writeArray(elements, writer, 0);
			return writer.toString();
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * Writes the elements as a pretty JSON object.
	 *
	 * @param elements the elements to write
	 * @param writer   the writer to use
	 * @param indent   the initial indent level; the first bracket is not indented,
	 *                 inner elements are indented by one, and the last bracket is
	 *                 indented at the initial indentation level
	 * @throws IOException if an IO error occurs
	 *
	 * @see Writer#write(String)
	 * @see #writeIndent(Writer, int)
	 * @see #writeIndent(String, Writer, int)
	 */
	public static void writeObject(Map<String, ? extends Number> elements, Writer writer, int indent)
			throws IOException {
		writeIndent("{", writer, 0);
		if (!elements.isEmpty()) {
			writer.write("\n");
			var iterator = elements.entrySet().iterator();
			var firstEntry = iterator.next();
			writeQuote(firstEntry.getKey(), writer, indent + 1);
			writer.write(": ");
			writeIndent(firstEntry.getValue().toString(), writer, 0);

			String seperator = ",\n";
			while (iterator.hasNext()) {
				writer.write(seperator);
				var nextEntry = iterator.next();
				String key = nextEntry.getKey();
				Number value = nextEntry.getValue();
				writeQuote(key, writer, indent + 1);
				writer.write(": ");
				writeIndent(value.toString(), writer, 0);
			}
		}

		writer.write("\n");
		writeIndent("}", writer, indent);
	}

	/**
	 * Writes the elements as a pretty JSON object to file.
	 *
	 * @param elements the elements to write
	 * @param path     the file path to use
	 * @throws IOException if an IO error occurs
	 *
	 * @see Files#newBufferedReader(Path, java.nio.charset.Charset)
	 * @see StandardCharsets#UTF_8
	 * @see #writeObject(Map, Writer, int)
	 */
	public static void writeObject(Map<String, ? extends Number> elements, Path path) throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(path, UTF_8)) {
			writeObject(elements, writer, 0);
		}
	}

	/**
	 * Returns the elements as a pretty JSON object.
	 *
	 * @param elements the elements to use
	 * @return a {@link String} containing the elements in pretty JSON format
	 *
	 * @see StringWriter
	 * @see #writeObject(Map, Writer, int)
	 */
	public static String writeObject(Map<String, ? extends Number> elements) {
		try {
			StringWriter writer = new StringWriter();
			writeObject(elements, writer, 0);
			return writer.toString();
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * Writes the elements as a pretty JSON object with nested arrays. The generic
	 * notation used allows this method to be used for any type of map with any type
	 * of nested collection of number objects.
	 *
	 * @param elements the elements to write
	 * @param writer   the writer to use
	 * @param indent   the initial indent level; the first bracket is not indented,
	 *                 inner elements are indented by one, and the last bracket is
	 *                 indented at the initial indentation level
	 * @throws IOException if an IO error occurs
	 *
	 * @see Writer#write(String)
	 * @see #writeIndent(Writer, int)
	 * @see #writeIndent(String, Writer, int)
	 * @see #writeArray(Collection)
	 */
	public static void writeObjectArrays(Map<String, ? extends Collection<? extends Number>> elements, Writer writer,
			int indent) throws IOException {
		writeIndent("{", writer, 0);
		if (!elements.isEmpty()) {
			writer.write("\n");
			var iterator = elements.entrySet().iterator();
			var firstEntry = iterator.next();
			writeQuote(firstEntry.getKey(), writer, indent + 1);
			writer.write(": ");
			writeArray(firstEntry.getValue(), writer, indent + 1);

			String seperator = ",\n";
			while (iterator.hasNext()) {
				writer.write(seperator);
				var entry = iterator.next();
				String key = entry.getKey();
				var value = entry.getValue();
				writeQuote(key, writer, indent + 1);
				writer.write(": ");
				writeArray(value, writer, indent + 1);
			}
		}
		writer.write("\n");
		writeIndent("}", writer, indent);
	}

	/**
	 * Writes the elements as a pretty JSON object with nested arrays to file.
	 *
	 * @param elements the elements to write
	 * @param path     the file path to use
	 * @throws IOException if an IO error occurs
	 *
	 * @see Files#newBufferedReader(Path, java.nio.charset.Charset)
	 * @see StandardCharsets#UTF_8
	 * @see #writeObjectArrays(Map, Writer, int)
	 */
	public static void writeObjectArrays(Map<String, ? extends Collection<? extends Number>> elements, Path path)
			throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(path, UTF_8)) {
			writeObjectArrays(elements, writer, 0);
		}
	}

	/**
	 * Returns the elements as a pretty JSON object with nested arrays.
	 *
	 * @param elements the elements to use
	 * @return a {@link String} containing the elements in pretty JSON format
	 *
	 * @see StringWriter
	 * @see #writeObjectArrays(Map, Writer, int)
	 */
	public static String writeObjectArrays(Map<String, ? extends Collection<? extends Number>> elements) {
		try {
			StringWriter writer = new StringWriter();
			writeObjectArrays(elements, writer, 0);
			return writer.toString();
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * Writes the elements as a pretty JSON array with nested objects. The generic
	 * notation used allows this method to be used for any type of collection with
	 * any type of nested map of String keys to number objects.
	 *
	 * @param elements the elements to write
	 * @param writer   the writer to use
	 * @param indent   the initial indent level; the first bracket is not indented,
	 *                 inner elements are indented by one, and the last bracket is
	 *                 indented at the initial indentation level
	 * @throws IOException if an IO error occurs
	 *
	 * @see Writer#write(String)
	 * @see #writeIndent(Writer, int)
	 * @see #writeIndent(String, Writer, int)
	 * @see #writeObject(Map)
	 */
	public static void writeArrayObjects(Collection<? extends Map<String, ? extends Number>> elements, Writer writer,
			int indent) throws IOException {
		writeIndent("[", writer, 0);
		if (!elements.isEmpty()) {
			writer.write("\n");
			var iterator = elements.iterator();
			var firstElement = iterator.next();
			writeIndent(writer, indent + 1);
			writeObject(firstElement, writer, indent + 1);

			String seperator = ",\n";
			while (iterator.hasNext()) {
				writer.write(seperator);
				var nextElement = iterator.next();
				writeIndent(writer, indent + 1);
				writeObject(nextElement, writer, indent + 1);
			}
		}
		writer.write("\n");
		writeIndent("]", writer, indent);
	}

	/**
	 * Writes the elements as a pretty JSON array with nested objects to file.
	 *
	 * @param elements the elements to write
	 * @param path     the file path to use
	 * @throws IOException if an IO error occurs
	 *
	 * @see Files#newBufferedReader(Path, java.nio.charset.Charset)
	 * @see StandardCharsets#UTF_8
	 * @see #writeArrayObjects(Collection)
	 */
	public static void writeArrayObjects(Collection<? extends Map<String, ? extends Number>> elements, Path path)
			throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(path, UTF_8)) {
			writeArrayObjects(elements, writer, 0);
		}
	}

	/**
	 * Returns the elements as a pretty JSON array with nested objects.
	 *
	 * @param elements the elements to use
	 * @return a {@link String} containing the elements in pretty JSON format
	 *
	 * @see StringWriter
	 * @see #writeArrayObjects(Collection)
	 */
	public static String writeArrayObjects(Collection<? extends Map<String, ? extends Number>> elements) {
		try {
			StringWriter writer = new StringWriter();
			writeArrayObjects(elements, writer, 0);
			return writer.toString();
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * Write the element as a pretty JSON objects with nested arrays in an inverted
	 * index map.
	 * 
	 * @param indexMap map should include the stem words as the key, and a nested
	 *                 map with path as key, and the position of the stem words
	 * @param writer   the writer to use
	 * @param indent   the initial indent level; the first bracket is not indented,
	 *                 inner elements are indented by one, and the last bracket is
	 *                 indented at the initial indentation level
	 * @throws IOException if an IO error occurs
	 * 
	 * @see #writeIndent(String, Writer, int)
	 * @see #writeQuote(String, Writer, int)
	 * @see #writeObjectArrays(Map, Writer, int)
	 */
	public static void writeInvertedIndex(
			Map<String, ? extends Map<String, ? extends Collection<? extends Number>>> indexMap, Writer writer,
			int indent) throws IOException {
		writeIndent("{", writer, 0);
		if (!indexMap.isEmpty()) {
			writer.write("\n");
			var iterator = indexMap.entrySet().iterator();
			var firstEntry = iterator.next();
			writeQuote(firstEntry.getKey(), writer, indent + 1);
			writer.write(": ");
			writeObjectArrays(firstEntry.getValue(), writer, indent + 1);

			String seperator = ",\n";
			while (iterator.hasNext()) {
				writer.write(seperator);
				var entry = iterator.next();
				String key = entry.getKey();
				var value = entry.getValue();
				writeQuote(key, writer, indent + 1);
				writer.write(": ");
				writeObjectArrays(value, writer, indent + 1);
			}
		}
		writer.write("\n");
		writeIndent("}", writer, indent);
	}

	/**
	 * Write the element as a pretty JSON objects with nested arrays in an inverted
	 * index map to a file.
	 * 
	 * @param index inverted index map
	 * @param path  path to file
	 * @throws IOException if an IO error occurs
	 */
	public static void writeInvertedIndex(
			Map<String, ? extends Map<String, ? extends Collection<? extends Number>>> index, Path path)
			throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(path, UTF_8)) {
			writeInvertedIndex(index, writer, 0);
		}
	}

	/**
	 * Returns the element as a pretty JSON objects with nested arrays in an
	 * inverted index map
	 * 
	 * @param indexMap map should include the stem words as the key, and a nested
	 *                 map with path as key, and the position of the stem words
	 * @return a {@link String} containing the elements in pretty JSON format
	 * 
	 * @see StringWriter
	 * @see #writeInvertedIndex(Map, Writer, int)
	 */
	public static String writeInvertedIndex(
			Map<String, ? extends Map<String, ? extends Collection<? extends Number>>> indexMap) {
		try {
			StringWriter writer = new StringWriter();
			writeInvertedIndex(indexMap, writer, 0);
			return writer.toString();
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * Write the search results to a file in pretty JSON format.
	 * 
	 * @param results search results
	 * @param writer  writer for output
	 * @param indent  initial indent level
	 * @throws IOException if an IO error occurs
	 */
	public static void writeSearchResults(Map<String, ? extends Collection<SearchResult>> results, Writer writer,
			int indent) throws IOException {
		writeIndent("{", writer, indent - 1);
		if (!results.isEmpty()) {
			writer.write("\n");
			var iterator = results.entrySet().iterator();
			var firstEntry = iterator.next();
			String key = firstEntry.getKey();
			Collection<SearchResult> value = firstEntry.getValue();
			writeSearchResultObject(key, value, writer, indent);

			String seperator = ",\n";
			while (iterator.hasNext()) {
				writer.write(seperator);
				var nextEntry = iterator.next();
				String nextKey = nextEntry.getKey();
				Collection<SearchResult> nextValue = nextEntry.getValue();
				writeSearchResultObject(nextKey, nextValue, writer, indent);
			}
		}
		writer.write("\n");
		writeIndent("}", writer, indent - 1);
	}

	/**
	 * Write the search results single object to a file in pretty JSON.
	 * 
	 * @param key    the query line
	 * @param value  the search results list
	 * @param writer the writer for output
	 * @param indent the initial indent level
	 * @throws IOException if an IO error occurs
	 */
	public static void writeSearchResultObject(String key, Collection<SearchResult> value, Writer writer, int indent)
			throws IOException {
		writeQuote(key, writer, indent);
		writer.write(": ");
		writeSearchResultsArray(value, writer, indent);
	}

	/**
	 * Write the search results array to a file in pretty JSON.
	 * 
	 * @param value  the search results list
	 * @param writer the writer for output
	 * @param indent the initial indent level
	 * @throws IOException if an IO error occurs
	 */
	public static void writeSearchResultsArray(Collection<SearchResult> value, Writer writer, int indent)
			throws IOException {
		writeIndent("[", writer, indent - 1);
		if (!value.isEmpty()) {
			writer.write("\n");
			var iterator = value.iterator();
			var firstElement = iterator.next();
			writeSearchResultsStats(firstElement, writer, indent + 1);

			String seperator = ",\n";
			while (iterator.hasNext()) {
				writer.write(seperator);
				var nextElement = iterator.next();
				writeSearchResultsStats(nextElement, writer, indent + 1);
			}
		}
		writer.write("\n");
		writeIndent("]", writer, indent);
	}

	/**
	 * Write the single search results' parameters to a file in pretty JSON.
	 * 
	 * @param sr     the search result object
	 * @param writer the writer for output
	 * @param indent the initial indent level
	 * @throws IOException if an IO error occurs
	 */
	public static void writeSearchResultsStats(SearchResult sr, Writer writer, int indent) throws IOException {
		writeIndent("{", writer, indent);
		writer.write("\n");
		writeQuote("count", writer, indent + 1);
		writer.write(": ");
		writeIndent(Integer.toString(sr.getMatches()), writer, 0);
		writer.write(",\n");
		writeQuote("score", writer, indent + 1);
		writer.write(": ");
		String format = String.format("%.8f", sr.getScore());
		writeIndent(format, writer, 0);
		writer.write(",\n");
		writeQuote("where", writer, indent + 1);
		writer.write(": ");
		writeQuote(sr.getLocation(), writer, 0);
		writer.write("\n");
		writeIndent("}", writer, indent);
	}

	/**
	 * Write a JSON object to the given Writer.
	 * 
	 * @param results the map of search results
	 * @param path    the path to the file where the results should be written
	 * @throws IOException if an I/O error occurs while writing the file
	 */
	public static void writeSearchResults(Map<String, ? extends Collection<SearchResult>> results, Path path)
			throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(path, UTF_8)) {
			writeSearchResults(results, writer, 1);
		}
	}

	/**
	 * Private constructor to prevent instantiation.
	 */
	private JsonWriter() {
	}
}
