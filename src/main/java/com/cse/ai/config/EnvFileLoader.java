package com.cse.ai.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads {@code .env} from the working directory for AI keys and local URLs.
 */
public final class EnvFileLoader {
	public static final String ENV_FILE = ".env";

	private static volatile Map<String, String> cached = Map.of();

	private EnvFileLoader() {
	}

	public static Map<String, String> loadOptional() {
		try {
			Path path = Path.of(ENV_FILE);
			if (!Files.isRegularFile(path)) {
				cached = Map.of();
				return cached;
			}
			cached = parse(path);
			return cached;
		} catch (IOException e) {
			cached = Map.of();
			return cached;
		}
	}

	public static Map<String, String> loadRequired() {
		Path path = Path.of(ENV_FILE);
		if (!Files.isRegularFile(path)) {
			throw new AiConfigException(
					"Missing .env file in the project directory. Copy .env.example to .env and configure your stack.");
		}
		try {
			cached = parse(path);
			return cached;
		} catch (IOException e) {
			throw new AiConfigException("Unable to read .env: " + e.getMessage());
		}
	}

	public static String get(String key) {
		String fromFile = cached.get(key);
		if (fromFile != null && !fromFile.isBlank()) {
			return fromFile.trim();
		}
		String fromEnv = System.getenv(key);
		return fromEnv == null ? "" : fromEnv.trim();
	}

	static void resetForTests() {
		cached = Map.of();
	}

	static void setCachedForTests(Map<String, String> values) {
		cached = values == null ? Map.of() : Map.copyOf(values);
	}

	static Map<String, String> parseForTests(Path path) throws IOException {
		return parse(path);
	}

	private static Map<String, String> parse(Path path) throws IOException {
		Map<String, String> values = new LinkedHashMap<>();
		for (String line : Files.readAllLines(path)) {
			String trimmed = line.strip();
			if (trimmed.isEmpty() || trimmed.startsWith("#")) {
				continue;
			}
			int eq = trimmed.indexOf('=');
			if (eq <= 0) {
				continue;
			}
			String key = trimmed.substring(0, eq).strip();
			String value = trimmed.substring(eq + 1).strip();
			if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
				value = value.substring(1, value.length() - 1);
			}
			values.put(key, value);
		}
		return Collections.unmodifiableMap(values);
	}
}
