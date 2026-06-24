package com.cse.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads server settings from classpath {@code application.properties} with env overrides.
 */
public final class ServerSettings {
	private final int port;
	private final int threads;
	private final String indexDirectory;
	private final String adminPassword;
	private final int searchDefaultLimit;

	private ServerSettings(Properties props) {
		this.port = intProp(props, "server.port", "SERVER_PORT", 8080);
		this.threads = intProp(props, "server.threads", "SERVER_THREADS", 5);
		this.indexDirectory = strProp(props, "index.directory", "INDEX_DIR", "data/index");
		this.adminPassword = strProp(props, "admin.password", "ADMIN_PASSWORD", AppContext.ADMIN_PASSWORD);
		this.searchDefaultLimit = intProp(props, "search.defaultLimit", "SEARCH_DEFAULT_LIMIT", 50);
	}

	public static ServerSettings load() {
		Properties props = new Properties();
		try (InputStream in = ServerSettings.class.getResourceAsStream("/application.properties")) {
			if (in != null) {
				props.load(in);
			}
		} catch (IOException e) {
			// use defaults
		}
		return new ServerSettings(props);
	}

	public int port() {
		return port;
	}

	public int threads() {
		return threads;
	}

	public String indexDirectory() {
		return indexDirectory;
	}

	public String adminPassword() {
		return adminPassword;
	}

	public int searchDefaultLimit() {
		return searchDefaultLimit;
	}

	private static String strProp(Properties props, String key, String env, String defaultValue) {
		String envVal = System.getenv(env);
		if (envVal != null && !envVal.isBlank()) {
			return envVal;
		}
		return props.getProperty(key, defaultValue);
	}

	private static int intProp(Properties props, String key, String env, int defaultValue) {
		String envVal = System.getenv(env);
		if (envVal != null) {
			try {
				return Integer.parseInt(envVal);
			} catch (NumberFormatException ignored) {
			}
		}
		String val = props.getProperty(key);
		if (val == null) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(val);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}
}
