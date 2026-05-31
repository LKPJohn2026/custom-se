package edu.usfca.cs272;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import edu.usfca.cs272.utils.MultiReaderLock;

/**
 * This class is the thread-safe version of the InvertedIndex class.
 * 
 * @see InvertedIndex the single-threaded version
 */
public class ThreadSafeInvertedIndex extends InvertedIndex {

    /**
     * This lock will be used to lock the map structure for safely reading and
     * writing.
     */
    private final MultiReaderLock lock;

    /**
     * Constructor for ThreadSafeInvertedIndex class.
     */
    public ThreadSafeInvertedIndex() {
        super();
        lock = new MultiReaderLock();
    }

    /**
     * This method add a stem and its location and positions into the index map
     * 
     * @param words    the stem
     * @param location the location of a file
     * @param position the position in the file where the stem is found
     */
    @Override
    public void addWord(String words, String location, int position) {
        lock.writeLock().lock();
        try {
            super.addWord(words, location, position);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * This method add all stems and its location and starting position into the
     * index map
     * 
     * @param words    the stem
     * @param location the location of a file
     * @param start    the starting position in the file where the stem is found
     */
    @Override
    public void addAllWords(List<String> words, String location, int start) {
        lock.writeLock().lock();
        try {
            super.addAllWords(words, location, start);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * This method add to the index map from another InvertedIndex object
     * 
     * @param local the InvertedIndex object to add from
     */
    @Override
    public void addAll(InvertedIndex local) {
        lock.writeLock().lock();
        try {
            super.addAll(local);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * This method returns a set of all locations in the counts map.
     * 
     * @return a set of all locations in the counts map
     */
    @Override
    public Set<String> getLocations() {
        lock.readLock().lock();
        try {
            return super.getLocations();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * This method returns the stem count of a location in the counts map
     * 
     * @param location the location to search for
     * @return 0 if not found or the count otherwise
     */
    @Override
    public int getCount(String location) {
        lock.readLock().lock();
        try {
            return super.getCount(location);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * This method returns a list of all the stem words in the index
     * 
     * @return a list of all the stem words in the index
     */
    @Override
    public Set<String> getWords() {
        lock.readLock().lock();
        try {
            return super.getWords();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * This method returns a set of all the locations in the index map
     * 
     * @param word the stem to search for in the index map
     * @return a set of all the locations in the index map
     */
    @Override
    public Set<String> getLocations(String word) {
        lock.readLock().lock();
        try {
            return super.getLocations(word);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * This method returns a list of all the positions in the index map
     * 
     * @param word     the stem to search for in the index map
     * @param location the location to search for
     * @return a list of all the positions in the index map
     */
    @Override
    public Set<Integer> getPositions(String word, String location) {
        lock.readLock().lock();
        try {
            return super.getPositions(word, location);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * This method checks if a location exists in the counts map
     * 
     * @param location the location to search for in the counts map
     * @return true if it is found, false otherwise
     */
    @Override
    public boolean containsLocation(String location) {
        lock.readLock().lock();
        try {
            return super.containsLocation(location);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * This method check if the index map contains a stem
     * 
     * @param stem the stem to search for in the index map
     * @return true if the index map contains a stem
     */
    @Override
    public boolean containsStem(String stem) {
        lock.readLock().lock();
        try {
            return super.containsStem(stem);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * This method check if the index map contains a location for a stem
     * 
     * @param stem     the stem to search for in the index map
     * @param location the location to search for in the index map
     * @return true if the index map contains a location for a stem, false otherwise
     */
    @Override
    public boolean containsLocation(String stem, String location) {
        lock.readLock().lock();
        try {
            return super.containsLocation(stem, location);
        } finally {
            lock.readLock().unlock();
        }
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
    @Override
    public boolean containsPosition(String stem, String location, int position) {
        lock.readLock().lock();
        try {
            return super.containsPosition(stem, location, position);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * This method returns the size of the counts map
     * 
     * @return the size of the counts map
     */
    @Override
    public int totalCounts() {
        lock.readLock().lock();
        try {
            return super.totalCounts();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * This method returns the total number of stem words in the index map
     * 
     * @return the size of the index map
     */
    @Override
    public int totalStems() {
        lock.readLock().lock();
        try {
            return super.totalStems();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * This method returns the total number of locations in the index map that
     * contain a stem
     * 
     * @param stem the stem to search for in the index map
     * @return the number of locations in the index map or 0 if there is no stem in
     *         the index map
     */
    @Override
    public int totalLocations(String stem) {
        lock.readLock().lock();
        try {
            return super.totalLocations(stem);
        } finally {
            lock.readLock().unlock();
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
    @Override
    public int totalPositions(String word, String location) {
        lock.readLock().lock();
        try {
            return super.totalPositions(word, location);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * This method convert the object to string representation
     * 
     * @return the string representation of the map
     */
    @Override
    public String toString() {
        lock.readLock().lock();
        try {
            return super.toString();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * This method write the index map to a JSON file by calling the function in
     * JsonWriter
     * 
     * @param path the path where the file will be written
     * @throws IOException if an I/O error occurs while writing the file
     */
    @Override
    public void indexJson(Path path) throws IOException {
        lock.readLock().lock();
        try {
            super.indexJson(path);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * This method write the counts map to a JSON file by calling the function in
     * JsonWriter
     * 
     * @param path the path where the file will be written
     * @throws IOException if an I/O error occurs while writing the file
     */
    @Override
    public void countJson(Path path) throws IOException {
        lock.readLock().lock();
        try {
            super.countJson(path);
        } finally {
            lock.readLock().unlock();
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
    @Override
    public List<SearchResult> exactIndex(Set<String> queryWords) {
        lock.readLock().lock();
        try {
            return super.exactIndex(queryWords);
        } finally {
            lock.readLock().unlock();
        }
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
    @Override
    public List<SearchResult> partialIndex(Set<String> queryWords) {
        lock.readLock().lock();
        try {
            return super.partialIndex(queryWords);
        } finally {
            lock.readLock().unlock();
        }
    }
}
