package com.cse.server.meta;

import java.time.Instant;

/**
 * Metadata captured when a web page is crawled.
 */
public record PageMetadata(String title, int contentLength, String snippet, Instant crawledAt) {
}
