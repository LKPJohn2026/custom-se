package edu.usfca.cs272;

import edu.usfca.cs272.utils.WorkQueue;
import edu.usfca.cs272.utils.HtmlFetcher;
import edu.usfca.cs272.utils.HtmlCleaner;
import edu.usfca.cs272.utils.LinkFinder;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Crawls web pages starting from a seed URI, strips HTML, stems the text,
 * and adds the results to the inverted index.
 *
 * @author Phong La
 */
public class WebCrawler {

    /** The shared inverted index to populate. */
    private final InvertedIndex index;

    /** Tracks already-visited URIs to avoid re-crawling. */
    private final Set<URI> visited;

    /** The work queue for submitting crawl tasks. */
    private final WorkQueue queue;

    /**
     * Constructs a new crawler.
     *
     * @param index the inverted index to populate
     * @param queue the work queue for parallel crawling
     */
    public WebCrawler(InvertedIndex index, WorkQueue queue) {
        this.index = index;
        this.visited = new HashSet<>();
        this.queue = queue;
    }

    /**
     * Begins crawling from the given seed URI. Blocks until all discovered
     * pages have been processed.
     *
     * @param uri the seed URI to start crawling from
     */
    public void crawl(URI uri) {
        synchronized (visited) {
            visited.add(uri);
        }
        queue.execute(new CrawlerTask(uri));
        queue.finish();
    }

    /** A single crawl task: fetch, index, and discover links. */
    private class CrawlerTask implements Runnable {
        /** The URI to crawl. */
        private final URI uri;

        /**
         * @param uri the URI to crawl
         */
        public CrawlerTask(URI uri) {
            this.uri = uri;
        }

        @Override
        public void run() {
            try {
                String html = HtmlFetcher.fetch(uri, 3);
                if (html == null || html.isBlank()) {
                    return;
                }

                String cleaned = HtmlCleaner.stripHtml(html);
                List<String> stems = FileStemmer.listStems(cleaned);
                index.addAllWords(stems, uri.toString(), 1);

                List<URI> links = LinkFinder.listUris(uri, html);

                synchronized (visited) {
                    for (URI link : links) {
                        if (!visited.contains(link)) {
                            visited.add(link);
                            queue.execute(new CrawlerTask(link));
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Unable to crawl: " + uri);
            }
        }
    }
}
