package com.cse.server.session;

import java.io.Serializable;
import java.time.Instant;

/**
 * A value stored with a timestamp.
 */
public record TimestampedEntry(String value, Instant timestamp) implements Serializable {
	private static final long serialVersionUID = 1L;

	public TimestampedEntry(String value) {
		this(value, Instant.now());
	}
}
