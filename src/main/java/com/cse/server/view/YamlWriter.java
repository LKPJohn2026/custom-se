package com.cse.server.view;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Minimal YAML writer for index export.
 */
public final class YamlWriter {
	private YamlWriter() {
	}

	public static void writeIndex(Map<String, TreeMap<String, TreeSet<Integer>>> index, Writer out)
			throws IOException {
		out.write("index:\n");
		for (var wordEntry : index.entrySet()) {
			out.write("  " + yamlKey(wordEntry.getKey()) + ":\n");
			for (var locEntry : wordEntry.getValue().entrySet()) {
				out.write("    " + yamlKey(locEntry.getKey()) + ":\n");
				Set<Integer> positions = locEntry.getValue();
				if (!positions.isEmpty()) {
					for (int pos : positions) {
						out.write("      - " + pos + "\n");
					}
				} else {
					out.write("      []\n");
				}
			}
		}
		out.flush();
	}

	private static String yamlKey(String key) {
		if (key.matches("^[a-zA-Z0-9_-]+$")) {
			return key;
		}
		return "\"" + key.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
	}
}
