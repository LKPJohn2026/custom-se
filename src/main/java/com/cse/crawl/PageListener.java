package com.cse.crawl;

/**
 * Callback when a page is crawled and indexed.
 */
public interface PageListener {
	void onPageIndexed(String location, String title, String body, String snippet);
}
