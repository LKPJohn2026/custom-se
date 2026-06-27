package com.cse.ai.embed;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal JSON helpers for embedding HTTP responses.
 */
final class EmbeddingJson {
	private EmbeddingJson() {
	}

	static String ollamaRequest(String model, String text) {
		return "{\"model\":\"" + escape(model) + "\",\"prompt\":\"" + escape(text) + "\"}";
	}

	static String openAiRequest(String model, String text) {
		return "{\"model\":\"" + escape(model) + "\",\"input\":\"" + escape(text) + "\"}";
	}

	static String voyageRequest(String model, List<String> texts, String inputType, int outputDimension) {
		StringBuilder input = new StringBuilder("[");
		for (int i = 0; i < texts.size(); i++) {
			if (i > 0) {
				input.append(',');
			}
			input.append('"').append(escape(texts.get(i))).append('"');
		}
		input.append(']');
		return "{\"model\":\"" + escape(model) + "\",\"input\":" + input
				+ ",\"input_type\":\"" + escape(inputType) + "\",\"output_dimension\":" + outputDimension + "}";
	}

	static float[] parseOllamaEmbedding(String json) {
		return parseEmbeddingArray(json, "\"embedding\"");
	}

	static float[] parseOpenAiEmbedding(String json) {
		return parseOpenAiEmbeddings(json).get(0);
	}

	static List<float[]> parseOpenAiEmbeddings(String json) {
		List<float[]> vectors = new ArrayList<>();
		int searchFrom = 0;
		while (true) {
			int dataIdx = json.indexOf("\"embedding\"", searchFrom);
			if (dataIdx < 0) {
				break;
			}
			vectors.add(parseEmbeddingArray(json.substring(dataIdx), "\"embedding\""));
			searchFrom = dataIdx + 1;
		}
		if (vectors.isEmpty()) {
			throw new EmbeddingException("Response missing embedding vectors");
		}
		return vectors;
	}

	private static float[] parseEmbeddingArray(String json, String field) {
		int fieldIdx = json.indexOf(field);
		if (fieldIdx < 0) {
			throw new EmbeddingException("Response missing " + field);
		}
		int start = json.indexOf('[', fieldIdx);
		int end = json.indexOf(']', start);
		if (start < 0 || end < 0) {
			throw new EmbeddingException("Malformed embedding array");
		}
		String inner = json.substring(start + 1, end).trim();
		if (inner.isEmpty()) {
			return new float[0];
		}
		String[] parts = inner.split(",");
		float[] values = new float[parts.length];
		for (int i = 0; i < parts.length; i++) {
			values[i] = Float.parseFloat(parts[i].trim());
		}
		return values;
	}

	private static String escape(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}
