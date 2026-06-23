package com.cse.crawl;

import com.cse.concurrent.WorkQueue;
import com.cse.index.InvertedIndex;
import com.cse.net.HtmlCleaner;
import com.cse.net.HtmlFetcher;
import com.cse.net.LinkFinder;
import com.cse.stem.FileStemmer;

import java.net.URI;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.cse.server.meta.MetadataStore;
import com.cse.server.meta.PageMetadata;

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

    /** Locations already indexed; skipped without counting toward max. */
    private final Set<String> knownLocations;

    /** Optional metadata store for page snippets and stats. */
    private final MetadataStore metadata;

    /** Count of newly crawled pages (not skipped). */
    private int newPagesCrawled;

    /**
     * Constructs a new crawler with no page limit.
     *
     * @param index the inverted index to populate
     * @param queue the work queue for parallel crawling
     */
    public WebCrawler(InvertedIndex index, WorkQueue queue) {
        this(index, queue, Integer.MAX_VALUE, Set.of(), null);
    }

    /**
     * Constructs a new crawler.
     *
     * @param index the inverted index to populate
     * @param queue the work queue for parallel crawling
     * @param maxPages the maximum number of pages to crawl
     */
    public WebCrawler(InvertedIndex index, WorkQueue queue, int maxPages) {
        this(index, queue, maxPages, Set.of(), null);
    }

    /**
     * Constructs a crawler that skips known locations and records metadata.
     *
     * @param index the inverted index to populate
     * @param queue the work queue for parallel crawling
     * @param maxNewPages maximum newly crawled pages (skipped URLs do not count)
     * @param knownLocations locations already in the index
     * @param metadata optional metadata store for page snippets
     */
    public WebCrawler(InvertedIndex index, WorkQueue queue, int maxNewPages,
            Set<String> knownLocations, MetadataStore metadata) {
        this.index = index;
        this.visited = new HashSet<>();
        this.queue = queue;
        this.maxPages = maxNewPages;
        this.knownLocations = knownLocations == null ? Set.of() : knownLocations;
        this.metadata = metadata;
        seedKnown();
    }

    private void seedKnown() {
        for (String loc : knownLocations) {
            try {
                visited.add(LinkFinder.clean(URI.create(loc)));
            } catch (Exception e) {
                // ignore invalid stored locations
            }
        }
    }

    /**
     * @return number of newly crawled pages from the last crawl
     */
    public int newPagesCrawled() {
        return newPagesCrawled;
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
            if (!visited.contains(cleaned) && newPagesCrawled < maxPages) {
                visited.add(cleaned);
                queue.execute(new CrawlerTask(cleaned, true));
            }
        }
        queue.finish();
    }

    /**
     * Crawls from a new seed, skipping URLs already in the index.
     *
     * @param uri seed URI
     * @return number of newly crawled pages
     */
    public int crawlAdditional(URI uri) {
        newPagesCrawled = 0;
        crawl(uri);
        return newPagesCrawled;
    }

    /** A single crawl task: fetch, index, and discover links. */
    private class CrawlerTask implements Runnable {
        /** The URI to crawl. */
        private final URI uri;

        /**
         * @param uri the URI to crawl
         */
        private final boolean countsAsNew;

        public CrawlerTask(URI uri, boolean countsAsNew) {
            this.uri = uri;
            this.countsAsNew = countsAsNew;
        }

        @Override
        public void run() {
            try {
                URI cleanedUri = LinkFinder.clean(uri);
                String html = HtmlFetcher.fetch(cleanedUri, 3);
                if (html == null || html.isBlank()) {
                    return;
                }

                if (countsAsNew) {
                    synchronized (visited) {
                        newPagesCrawled++;
                    }
                }

                String cleaned = HtmlCleaner.stripHtml(html);
                List<String> stems = FileStemmer.listStems(cleaned);
                String location = cleanedUri.toString();
                index.addAllWords(stems, location, 1);
                recordMetadata(location, html, cleaned);

                List<URI> links = LinkFinder.listUris(cleanedUri, HtmlCleaner.stripBlockElements(html));

                synchronized (visited) {
                    for (URI link : links) {
                        if (newPagesCrawled >= maxPages) {
                            break;
                        }
                        if (!visited.contains(link)) {
                            visited.add(link);
                            boolean isKnown = knownLocations.contains(link.toString());
                            if (!isKnown) {
                                queue.execute(new CrawlerTask(link, true));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Unable to crawl: " + uri);
            }
        }

        private void recordMetadata(String location, String html, String cleaned) {
            if (metadata == null) {
                return;
            }
            String title = extractTitle(html);
            String snippet = cleaned.length() > 200 ? cleaned.substring(0, 200) + "…" : cleaned;
            metadata.putPage(location, new PageMetadata(title, cleaned.length(), snippet, Instant.now()));
        }

        private static String extractTitle(String html) {
            int start = html.toLowerCase().indexOf("<title>");
            if (start < 0) {
                return "";
            }
            start += 7;
            int end = html.toLowerCase().indexOf("</title>", start);
            if (end < 0) {
                return "";
            }
            return HtmlCleaner.stripHtml(html.substring(start, end)).strip();
        }
    }
}
