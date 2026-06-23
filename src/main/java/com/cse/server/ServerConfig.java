package com.cse.server;

import java.net.URI;
import java.nio.file.Path;

import com.cse.cli.ArgumentParser;

public class ServerConfig {
	public static final int DEFAULT_PORT = 8080;
	public static final int DEFAULT_THREADS = 5;
	public static final int DEFAULT_CRAWL_LIMIT = 1;

	public final int port;
	public final int threads;

	public final Path textPath;
	public final URI htmlSeed;
	public final int crawlLimit;

	private ServerConfig(int port, int threads, Path textPath, URI htmlSeed, int crawlLimit) {
		this.port = port;
		this.threads = threads;
		this.textPath = textPath;
		this.htmlSeed = htmlSeed;
		this.crawlLimit = crawlLimit;
	}

	public static ServerConfig fromArgs(String[] args) {
		ArgumentParser parser = new ArgumentParser(args);

		int port = parser.getInteger("-port", DEFAULT_PORT);
		if (port <= 0) {
			port = DEFAULT_PORT;
		}

		int threads = parser.getInteger("-threads", DEFAULT_THREADS);
		if (threads <= 0) {
			threads = DEFAULT_THREADS;
		}

		Path text = null;
		if (parser.hasFlag("-text") && parser.hasValue("-text")) {
			text = parser.getPath("-text", null);
		}

		URI html = null;
		if (parser.hasFlag("-html") && parser.hasValue("-html")) {
			String seed = parser.getString("-html");
			try {
				html = seed == null ? null : URI.create(seed);
			} catch (IllegalArgumentException ignored) {
				html = null;
			}
		}

		int crawl = parser.hasFlag("-crawl") ? parser.getInteger("-crawl", DEFAULT_CRAWL_LIMIT) : DEFAULT_CRAWL_LIMIT;
		if (crawl <= 0) {
			crawl = DEFAULT_CRAWL_LIMIT;
		}

		// Phase 1: require exactly one index source
		if ((text == null && html == null) || (text != null && html != null)) {
			throw new IllegalArgumentException("Provide exactly one of -text <path> or -html <seed>.");
		}

		return new ServerConfig(port, threads, text, html, crawl);
	}
}

