package com.cse.crawl;

import com.cse.concurrent.WorkQueue;
import com.cse.index.InvertedIndex;
import com.cse.net.HtmlCleaner;
import com.cse.net.HtmlFetcher;
import com.cse.net.LinkFinder;
import com.cse.stem.FileStemmer;

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

    /** Maximum number of pages to crawl. */
    private final int maxPages;

    /**
     * Constructs a new crawler with no page limit.
     *
     * @param index the inverted index to populate
     * @param queue the work queue for parallel crawling
     */
    public WebCrawler(InvertedIndex index, WorkQueue queue) {
        this(index, queue, Integer.MAX_VALUE);
    }

    /**
     * Constructs a new crawler.
     *
     * @param index the inverted index to populate
     * @param queue the work queue for parallel crawling
     * @param maxPages the maximum number of pages to crawl
     */
    public WebCrawler(InvertedIndex index, WorkQueue queue, int maxPages) {
        this.index = index;
        this.visited = new HashSet<>();
        this.queue = queue;
        this.maxPages = maxPages;
    }

    /**
     * Begins crawling from the given seed URI. Blocks until all discovered
     * pages have been processed.
     *
     * @param uri the seed URI to start crawling from
     */
    public void crawl(URI uri) {
        URI cleaned = LinkFinder.clean(uri);
        synchronized (visited) {
            if (visited.size() < maxPages) {
                visited.add(cleaned);
                queue.execute(new CrawlerTask(cleaned));
            }
        }
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
                URI cleanedUri = LinkFinder.clean(uri);
                String html = HtmlFetcher.fetch(cleanedUri, 3);
                if (html == null || html.isBlank()) {
                    return;
                }

                String cleaned = HtmlCleaner.stripHtml(html);
                List<String> stems = FileStemmer.listStems(cleaned);
                index.addAllWords(stems, cleanedUri.toString(), 1);

                List<URI> links = LinkFinder.listUris(cleanedUri, HtmlCleaner.stripBlockElements(html));

                synchronized (visited) {
                    for (URI link : links) {
                        if (visited.size() >= maxPages) {
                            break;
                        }
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
