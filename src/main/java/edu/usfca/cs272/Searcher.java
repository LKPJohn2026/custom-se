package edu.usfca.cs272;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import edu.usfca.cs272.InvertedIndex.SearchResult;

public interface Searcher {
    default void processFile(Path path, boolean partial) throws IOException {
        try (var reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                processLine(line, partial);
            }
        }
    }

    void processLine(String line, boolean partial);

    void searchJson(Path path, boolean partial) throws IOException;

    Set<String> getQueries(boolean partial);

    List<SearchResult> getSearchResults(String query, boolean partial);

    String toString(boolean partial);
}
