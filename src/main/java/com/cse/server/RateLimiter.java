package com.cse.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple per-key rate limiter (requests per minute).
 */
public final class RateLimiter {
	private final int maxPerMinute;
	private final Map<String, Window> windows = new ConcurrentHashMap<>();

	public RateLimiter(int maxPerMinute) {
		this.maxPerMinute = maxPerMinute;
	}

	public boolean tryAcquire(String key) {
		long minute = System.currentTimeMillis() / 60_000;
		Window window = windows.computeIfAbsent(key, k -> new Window(minute));
		synchronized (window) {
			if (window.minute != minute) {
				window.minute = minute;
				window.count.set(0);
			}
			return window.count.incrementAndGet() <= maxPerMinute;
		}
	}

	private static final class Window {
		long minute;
		final AtomicInteger count = new AtomicInteger();

		Window(long minute) {
			this.minute = minute;
		}
	}
}
