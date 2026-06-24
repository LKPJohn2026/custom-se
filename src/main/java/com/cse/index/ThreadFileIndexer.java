package com.cse.index;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;
import static opennlp.tools.stemmer.snowball.SnowballStemmer.ALGORITHM.ENGLISH;
import opennlp.tools.stemmer.Stemmer;
import opennlp.tools.stemmer.snowball.SnowballStemmer;

import com.cse.concurrent.WorkQueue;
import com.cse.stem.FileStemmer;

/**
 * Multi-threaded file indexer. Walks a path and submits each text file to a
 * work queue to be parsed into the shared thread-safe inverted index.
 */
public class ThreadFileIndexer {

	/**
	 * This task will run the indexFile method in a work queue.
	 */
	private static class IndexFileTask implements Runnable {
		/**
		 * The path of the input file
		 */
		private final Path file;

		/**
		 * The inverted index thread-safe version
		 */
		private final ThreadSafeInvertedIndex index;

		/**
		 * Constructor for IndexFileTask class.
		 *
		 * @param file  the path of the input file
		 * @param index the inverted index thread-safe version
		 */
		public IndexFileTask(Path file, ThreadSafeInvertedIndex index) {
			this.file = file;
			this.index = index;
		}

		/**
		 * This method will run the indexFile method.
		 */
		@Override
		public void run() {
			try {
				InvertedIndex local = new InvertedIndex();
				indexFile(file, local);
				index.addAll(local);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	/**
	 * Indexes a single file into an {@link IndexStore}.
	 */
	private static class IndexStoreTask implements Runnable {
		private final Path file;
		private final IndexStore index;

		IndexStoreTask(Path file, IndexStore index) {
			this.file = file;
			this.index = index;
		}

		@Override
		public void run() {
			try {
				String body = Files.readString(file, UTF_8);
				String location = file.toAbsolutePath().toString();
				index.addDocument(new IndexDocument(location, location, file.getFileName().toString(),
						body, System.currentTimeMillis()));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	/**
	 * Reads the contents of a file or directory and writes it to a text file.
	 *
	 * @param path  the path of the file/directory that is being read
	 * @param index the index structure
	 * @param queue the work queue
	 * @throws IOException if an I/O error occurs
	 */
	public static void indexPath(Path path, ThreadSafeInvertedIndex index, WorkQueue queue) throws IOException {
		if (Files.isDirectory(path)) {
			indexDirectory(path, index, queue);
		} else {
			queue.execute(new IndexFileTask(path, index));
		}
		queue.finish();
	}

	/**
	 * Indexes a file or directory into an {@link IndexStore}.
	 */
	public static void indexPath(Path path, IndexStore index, WorkQueue queue) throws IOException {
		if (Files.isDirectory(path)) {
			indexDirectory(path, index, queue);
		} else {
			queue.execute(new IndexStoreTask(path, index));
		}
		queue.finish();
	}

	/**
	 * This method recursively indexes a directory and its contents into the
	 * inverted index.
	 *
	 * @param input the path of the input file/directory
	 * @param index the inverted index
	 * @param queue the work queue
	 * @throws IOException if an error occurs while reading or writing to files
	 */
	public static void indexDirectory(Path input, ThreadSafeInvertedIndex index, WorkQueue queue) throws IOException {
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(input)) {
			for (Path file : stream) {
				if (Files.isDirectory(file)) {
					indexDirectory(file, index, queue);
				} else if (isTextFile(file)) {
					queue.execute(new IndexFileTask(file, index));
				}
			}
		}
	}

	/**
	 * Recursively indexes a directory into an {@link IndexStore}.
	 */
	public static void indexDirectory(Path input, IndexStore index, WorkQueue queue) throws IOException {
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(input)) {
			for (Path file : stream) {
				if (Files.isDirectory(file)) {
					indexDirectory(file, index, queue);
				} else if (isTextFile(file)) {
					queue.execute(new IndexStoreTask(file, index));
				}
			}
		}
	}

	/**
	 * Indexes the words of a single file (stemmed) into the given inverted index.
	 *
	 * @param file  the path of the input file
	 * @param index the inverted index to populate
	 * @throws IOException if an error occurs while reading the file
	 */
	private static void indexFile(Path file, InvertedIndex index) throws IOException {
		try (BufferedReader stream = Files.newBufferedReader(file, UTF_8)) {
			Stemmer stemmer = new SnowballStemmer(ENGLISH);
			String line;
			String location = file.toString();
			int position = 1;
			while ((line = stream.readLine()) != null) {
				String[] words = FileStemmer.parse(line);
				for (String word : words) {
					index.addWord(stemmer.stem(word).toString(), location, position);
					position++;
				}
			}
		}
	}

	/**
	 * Returns true if the file is a regular text file (.txt or .text).
	 *
	 * @param file the path of the input file
	 * @return true if the file is a text file, false otherwise
	 */
	private static boolean isTextFile(Path file) {
		String lower = file.toString().toLowerCase();
		return Files.isRegularFile(file) && (lower.endsWith(".txt") || lower.endsWith(".text"));
	}

	/**
	 * Private constructor to prevent instantiation of this class
	 */
	private ThreadFileIndexer() {
	}
}
