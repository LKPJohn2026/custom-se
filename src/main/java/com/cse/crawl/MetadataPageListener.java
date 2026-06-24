package com.cse.crawl;

import com.cse.server.meta.MetadataStore;
import com.cse.server.meta.PageMetadata;

import java.time.Instant;

/**
 * Forwards crawl events to {@link MetadataStore}.
 */
public final class MetadataPageListener implements PageListener {
	private final MetadataStore metadata;

	public MetadataPageListener(MetadataStore metadata) {
		this.metadata = metadata;
	}

	@Override
	public void onPageIndexed(String location, String title, String body, String snippet) {
		metadata.putPage(location, new PageMetadata(title, body.length(), snippet, Instant.now()));
	}
}
