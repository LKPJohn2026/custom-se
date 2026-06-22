package com.cse.index;

import java.util.TreeMap;
import java.util.TreeSet;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.cse.io.JsonWriter;

/**
 * A class that implements a inverted index for a given text file.
 * 
 * @author Phong La
 */
public class InvertedIndex {

	/**
	 * The counts map that maps the location of a file and the number of stems in
	 * it.
	 */
	private final TreeMap<String, Integer> counts;

	/**
	 * The index map that maps the stem and its locations and positions.
	 */
	private final TreeMap<String, TreeMap<String, TreeSet<Integer>>> index;

	/**
	 * Constructor for InvertedIndex class
	 */
	public InvertedIndex() {
		this.counts = new TreeMap<>();
		this.index = new TreeMap<>();
	}

	/**
	 * This method add a stem and its location and positions into the index map
	 * 
	 * @param words    the stem
	 * @param location the location of a file
	 * @param position the position in the file where the stem is found
	 */
	public void addWord(String words, String location, int position) {
		// get the inner map for the stem, if it doesn't exist then create one
		TreeMap<String, TreeSet<Integer>> file = index.computeIfAbsent(words, k -> new TreeMap<>());

		// get the set of positions in a file where the stem is found, if it doesn't
		// exist then create one
		TreeSet<Integer> positions = file.computeIfAbsent(location, k -> new TreeSet<>());

		// add the position to the set of positions
		positions.add(position);

		// update the highest position in the set of positions as count
		counts.merge(location, position, Integer::max);
	}

	/**
	 * This method add all stems and its location and starting position into the
	 * index map
	 * 
	 * @param words    the stem
	 * @param location the location of a file
	 * @param start    the starting position in the file where the stem is found
	 */
	public void addAllWords(List<String> words, String location, int start) {
		for (String word : words) {
			addWord(word, location, start);
			// increment the starting position
			start++;
		}
	}

	/**
	 * This method add to the index map from another InvertedIndex object
	 * 
	 * @param local the InvertedIndex object to add from
	 */
	public void addAll(InvertedIndex local) {
		for (var localEntry : local.index.entrySet()) {
			String localWord = localEntry.getKey();
			var localLocations = localEntry.getValue();
			var thisLocations = this.index.get(localWord);

			if (thisLocations == null) {
				this.index.put(localWord, localLocations);
			} else {
				for (var localLocation : localLocations.entrySet()) {
					String location = localLocation.getKey();
					var localPositions = localLocation.getValue();
					var thisPositions = thisLocations.get(location);

					if (thisPositions == null) {
						thisLocations.put(location, localPositions);
					} else {
						thisPositions.addAll(localPositions);
					}
				}
			}
		}

		for (var localCount : local.counts.entrySet()) {
			String location = localCount.getKey();
			int count = localCount.getValue();
			this.counts.merge(location, count, Integer::max);
		}
	}

	/**
	 * This method returns a set of all locations in the counts map.
	 * 
	 * @return a set of all locations in the counts map
	 */
	public Set<String> getLocations() {
		return Collections.unmodifiableSet(counts.keySet());
	}

	/**
	 * This method returns the stem count of a location in the counts map
	 * 
	 * @param location the location to search for
	 * @return 0 if not found or the count otherwise
	 */
	public int getCount(String location) {
		return counts.getOrDefault(location, 0);
	}

	/**
	 * This method returns a list of all the stem words in the index
	 * 
	 * @return a list of all the stem words in the index
	 */
	public Set<String> getWords() {
		return Collections.unmodifiableSet(index.keySet());
	}

	/**
	 * This method returns a set of all the locations in the index map
	 * 
	 * @param word the stem to search for in the index map
	 * @return a set of all the locations in the index map
	 */
	public Set<String> getLocations(String word) {
		TreeMap<String, TreeSet<Integer>> fileMap = index.get(word);
		if (fileMap != null) {
			return Collections.unmodifiableSet(fileMap.keySet());
		} else {
			return Collections.emptySet();
		}
	}

	/**
	 * This method returns a list of all the positions in the index map
	 * 
	 * @param word     the stem to search for in the index map
	 * @param location the location to search for
	 * @return a list of all the positions in the index map
	 */
	public Set<Integer> getPositions(String word, String location) {
		TreeMap<String, TreeSet<Integer>> fileMap = index.get(word);
		if (fileMap != null) {
			TreeSet<Integer> positions = fileMap.get(location);
			if (positions != null) {
				return Collections.unmodifiableSet(positions);
			}
		}
		return Collections.emptySet();
	}

	/**
	 * This method checks if a location exists in the counts map
	 * 
	 * @param location the location to search for in the counts map
	 * @return true if it is found, false otherwise
	 */
	public boolean containsLocation(String location) {
		return counts.containsKey(location);
	}

	/**
	 * This method check if the index map contains a stem
	 * 
	 * @param stem the stem to search for in the index map
	 * @return true if the index map contains a stem
	 */
	public boolean containsStem(String stem) {
		return index.containsKey(stem);
	}

	/**
	 * This method check if the index map contains a location for a stem
	 * 
	 * @param stem     the stem to search for in the index map
	 * @param location the location to search for in the index map
	 * @return true if the index map contains a location for a stem, false otherwise
	 */
	public boolean containsLocation(String stem, String location) {
		TreeMap<String, TreeSet<Integer>> fileMap = index.get(stem);
		return (fileMap != null && fileMap.containsKey(location));
	}

	/**
	 * This method check if the index map contains a position for a stem in a
	 * location
	 * 
	 * @param stem     the stem to search for in the index map
	 * @param location the location to search for in the index map
	 * @param position the position of the word in the location
	 * @return true if the index map contains a position for a stem in a location,
	 *         false otherwise
	 */
	public boolean containsPosition(String stem, String location, int position) {
		TreeMap<String, TreeSet<Integer>> fileMap = index.get(stem);
		if (fileMap != null) {
			TreeSet<Integer> positions = fileMap.get(location);
			return positions != null && positions.contains(position);
		}
		return false;
	}

	/**
	 * This method returns the size of the counts map
	 * 
	 * @return the size of the counts map
	 */
	public int totalCounts() {
		return counts.size();
	}

	/**
	 * This method returns the total number of stem words in the index map
	 * 
	 * @return the size of the index map
	 */
	public int totalStems() {
		return index.size();
	}

	/**
	 * This method returns the total number of locations in the index map that
	 * contain a stem
	 * 
	 * @param stem the stem to search for in the index map
	 * @return the number of locations in the index map or 0 if there is no stem in
	 *         the index map
	 */
	public int totalLocations(String stem) {
		TreeMap<String, TreeSet<Integer>> location = index.get(stem);
		if (location != null) {
			return location.size();
		} else {
			return 0;
		}
	}

	/**
	 * This method returns the total number of positions in the index map that
	 * contain a stem in a location
	 * 
	 * @param word     the stem to search for in the index map
	 * @param location the location to search for in the index map
	 * @return the number of positions in the index map
	 */
	public int totalPositions(String word, String location) {
		TreeMap<String, TreeSet<Integer>> fileMap = index.get(word);
		if (fileMap != null) {
			TreeSet<Integer> positions = fileMap.get(location);
			if (positions != null) {
				return positions.size();
			}
		}
		return 0;
	}

	/**
	 * This method convert the object to string representation
	 * 
	 * @return the string representation of the map
	 */
	@Override
	public String toString() {
		return index.toString();
	}

	/**
	 * This method write the index map to a JSON file by calling the function in
	 * JsonWriter
	 * 
	 * @param path the path where the file will be written
	 * @throws IOException if an I/O error occurs while writing the file
	 */
	public void indexJson(Path path) throws IOException {
		JsonWriter.writeInvertedIndex(index, path);
	}

	/**
	 * This method write the counts map to a JSON file by calling the function in
	 * JsonWriter
	 * 
	 * @param path the path where the file will be written
	 * @throws IOException if an I/O error occurs while writing the file
	 */
	public void countJson(Path path) throws IOException {
		JsonWriter.writeObject(counts, path);
	}

	/**
	 * This is a custom class to store the matches and score of each line per
	 * location.
	 */
	public class SearchResult implements Comparable<SearchResult> {
		/**
		 * The score of each line.
		 */
		private double score;

		/**
		 * The count to all the words that matches in the location.
		 */
		private int matches;

		/**
		 * The location of the line.
		 */
		private final String location;

		/**
		 * Constructor for the SearchResult object
		 * 
		 * @param location the location of the line
		 */
		public SearchResult(String location) {
			this.matches = 0;
			this.score = 0.0;
			this.location = location;
		}

		/**
		 * This method updates the matches and score of the SearchResult object
		 * 
		 * @param count the number of matches
		 */
		private void update(int count) {
			this.matches += count;
			this.score = (double) this.matches / counts.get(location);
		}

		/**
		 * Getter method for matches field
		 * 
		 * @return number of matches
		 */
		public int getMatches() {
			return matches;
		}

		/**
		 * Getter method for score field
		 * 
		 * @return score
		 */
		public double getScore() {
			return score;
		}

		/**
		 * Getter method for location field
		 * 
		 * @return location of the line
		 */
		public String getLocation() {
			return location;
		}

		/**
		 * Method to compare 2 SearchResult objects based on score, matches and location
		 * 
		 * @return 1 if the object is greater than the parameter, -1 if it is lesser or
		 *         0 otherwise
		 */
		@Override
		public int compareTo(SearchResult other) {
			int cmp = Double.compare(other.score, this.score);
			if (cmp != 0) {
				return cmp;
			}

			cmp = Integer.compare(other.matches, this.matches);
			if (cmp != 0) {
				return cmp;
			}

			return this.location.compareTo(other.location);
		}
	}

	/**
	 * This method will search for the exact words in the index map and return a
	 * list of SearchResult object.
	 * 
	 * @param queryWords a set of words to search for
	 * @return a list of SearchResult objects that contains the matches, score, and
	 *         position of each word
	 * 
	 * @see #getPositions method for fileMap
	 */
	public List<SearchResult> exactIndex(Set<String> queryWords) {
		List<SearchResult> results = new ArrayList<>();
		Map<String, SearchResult> lookup = new HashMap<>();
		// loop through each word in the queryWords and the index map
		for (String word : queryWords) {
			var fileMap = index.get(word);
			if (fileMap != null) {
				searchHelper(fileMap, results, lookup);
			}
		}
		Collections.sort(results);
		return results;
	}

	/**
	 * This method will search for the partial words in the index map and return a
	 * list of SearchResult object.
	 * 
	 * @param queryWords a set of words to search for
	 * @return a list of SearchResult objects that contains the matches, score, and
	 *         position of each word
	 * 
	 * @see #getPositions method for fileMap
	 */
	public List<SearchResult> partialIndex(Set<String> queryWords) {
		List<SearchResult> results = new ArrayList<>();
		Map<String, SearchResult> lookup = new HashMap<>();
		// loop through each word in the queryWords and the index tailmap
		for (String word : queryWords) {
			var tailMap = index.tailMap(word);
			for (var entry : tailMap.entrySet()) {
				String stem = entry.getKey();
				if (!stem.startsWith(word)) {
					break;
				}
				// loop through the inner map of the index tailmap
				var fileMap = entry.getValue();
				searchHelper(fileMap, results, lookup);
			}
		}
		Collections.sort(results);
		return results;
	}

	/**
	 * This method will search for the exact words in the index map and update the
	 * SearchResult object with the matches, score, and position of each word.
	 * 
	 * @param fileMap the inner map of the index map
	 * @param results the list of SearchResult objects
	 * @param lookup  the map to store the location and SearchResult object
	 */
	private void searchHelper(TreeMap<String, TreeSet<Integer>> fileMap, List<SearchResult> results,
			Map<String, SearchResult> lookup) {
		for (var entry : fileMap.entrySet()) {
			String location = entry.getKey();
			int count = entry.getValue().size();
			// add the location and count to the lookup map and the results list
			SearchResult current = lookup.get(location);
			if (current == null) {
				current = new SearchResult(location);
				lookup.put(location, current);
				results.add(current);
			}
			current.update(count);
		}
	}
}
