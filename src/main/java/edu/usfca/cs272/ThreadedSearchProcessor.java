package edu.usfca.cs272;

import edu.usfca.cs272.utils.WorkQueue;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

import edu.usfca.cs272.InvertedIndex.SearchResult;

import opennlp.tools.stemmer.snowball.SnowballStemmer;
import static opennlp.tools.stemmer.snowball.SnowballStemmer.ALGORITHM.ENGLISH;

/**
 * This class is used to search in the Inverted Index through 2 ways: exact
 * search and partial search.
 */
public class ThreadedSearchProcessor implements Searcher {

    /**
     * This map will be used to store the exact search results. The key is the query
     * line, and the value is a SearchResults custom object.
     */
    private final Map<String, List<SearchResult>> exactResults;

    /**
     * This map will be used to store the partial search results. The key is the
     * query line, and the value is a SearchResults custom object.
     */
    private final Map<String, List<SearchResult>> partialResults;

    /**
     * This InvertedIndex object will be used to search for the query line in the
     * index structure.
     */
    private final ThreadSafeInvertedIndex index;

    /**
     * The work queue used for multi-threaded processing.
     */
    private final WorkQueue queue;

    /**
     * Constructor for SearchProcessing class.
     *
     * @param index the index object to be parsed in
     * @param queue the work queue for multi-threaded processing
     */
    public ThreadedSearchProcessor(ThreadSafeInvertedIndex index, WorkQueue queue) {
        this.exactResults = new TreeMap<>();
        this.partialResults = new TreeMap<>();
        this.index = index;
        this.queue = queue;
    }

    /**
     * Returns the appropriate results map based on the partial flag.
     *
     * @param partial indicates whether to use partial or exact map.
     * @return the results map to use
     */
    private Map<String, List<SearchResult>> getResults(boolean partial) {
        return partial ? partialResults : exactResults;
    }

    /**
     * Returns the appropriate search method based on the partial flag.
     *
     * @param partial indicates whether to use partial or exact search.
     * @return the search method to use
     */
    private Function<Set<String>, List<SearchResult>> getSearchMethod(boolean partial) {
        return partial ? index::partialIndex : index::exactIndex;
    }

    @Override
    public void processFile(Path path, boolean partial) throws IOException {
        Searcher.super.processFile(path, partial);
        queue.finish();
    }

    /**
     * This method will process the line and store the search results in the results
     * map.
     *
     * @param line    the line to process.
     * @param partial indicates whether the search is partial or exact.
     */
    public void processLine(String line, boolean partial) {
        queue.execute(() -> {
            Set<String> uniqueStems = FileStemmer.uniqueStems(line);
            String sortedLine = String.join(" ", uniqueStems);

            if (sortedLine.isBlank()) {
                return;
            }

            Map<String, List<SearchResult>> results = getResults(partial);
            Function<Set<String>, List<SearchResult>> searchMethod = getSearchMethod(partial);

            synchronized (results) {
                if (results.containsKey(sortedLine)) {
                    return;
                }
            }

            List<SearchResult> searchResults = searchMethod.apply(uniqueStems);

            synchronized (results) {
                if (!results.containsKey(sortedLine)) {
                    results.put(sortedLine, searchResults);
                }
            }
        });
    }

    /**
     * This method will write the search results to a JSON file.
     *
     * @param path    the path of the file that will be written.
     * @param partial indicates whether to use partial or exact map.
     * @throws IOException if there is an error writing to the file.
     */
    public void searchJson(Path path, boolean partial) throws IOException {
        Map<String, List<SearchResult>> results = getResults(partial);
        synchronized (results) {
            JsonWriter.writeSearchResults(results, path);
        }
    }

    /**
     * Get the queries that have been processed.
     *
     * @param partial indicates whether to use partial or exact map.
     * @return the set of queries that have been processed.
     */
    public Set<String> getQueries(boolean partial) {
        Map<String, List<SearchResult>> results = getResults(partial);
        synchronized (results) {
            return Collections.unmodifiableSet(results.keySet());
        }
    }

    /**
     * Get the search results for a specific query.
     *
     * @param query   the query to get the search results for.
     * @param partial indicates whether to use partial or exact map.
     * @return the search results for the query.
     */
    public List<SearchResult> getSearchResults(String query, boolean partial) {
        SnowballStemmer stemmer = new SnowballStemmer(ENGLISH);
        Set<String> uniqueStems = FileStemmer.uniqueStems(query, stemmer);
        String sortedLine = String.join(" ", uniqueStems);
        Map<String, List<SearchResult>> results = getResults(partial);
        synchronized (results) {
            List<SearchResult> searchResults = results.get(sortedLine);
            if (searchResults != null) {
                return Collections.unmodifiableList(searchResults);
            } else {
                return Collections.emptyList();
            }
        }
    }

    /**
     * Return the search results as a string.
     *
     * @param partial indicates whether to use partial or exact map.
     * @return the search results as a string.
     */
    public String toString(boolean partial) {
        Map<String, List<SearchResult>> results = getResults(partial);
        synchronized (results) {
            return results.toString();
        }
    }
}
