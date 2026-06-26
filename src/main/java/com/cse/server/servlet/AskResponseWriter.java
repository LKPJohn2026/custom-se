package com.cse.server.servlet;

import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;

import com.cse.ai.rag.ScoredChunk;

/**
 * Helpers for Ask SSE and JSON responses.
 */
final class AskResponseWriter {
	private AskResponseWriter() {
	}

	static void writeSseEvent(PrintWriter out, String event, String data) {
		out.print("event: ");
		out.println(event);
		for (String line : data.split("\n", -1)) {
			out.print("data: ");
			out.println(line);
		}
		out.println();
		out.flush();
	}

	static String sourcesJson(List<ScoredChunk> sources) {
		StringBuilder out = new StringBuilder("{\"sources\":[");
		for (int i = 0; i < sources.size(); i++) {
			if (i > 0) {
				out.append(',');
			}
			out.append(scoredChunkJson(sources.get(i)));
		}
		out.append("]}");
		return out.toString();
	}

	static String ragResponseJson(String answer, List<ScoredChunk> sources, long retrievalMs,
			long generationMs, String stackId) {
		StringBuilder out = new StringBuilder();
		out.append("{\"answer\":").append(toJsonString(answer)).append(',');
		out.append("\"sources\":[");
		for (int i = 0; i < sources.size(); i++) {
			if (i > 0) {
				out.append(',');
			}
			out.append(scoredChunkJson(sources.get(i)));
		}
		out.append("],\"retrievalMs\":").append(retrievalMs);
		out.append(",\"generationMs\":").append(generationMs);
		out.append(",\"stackId\":").append(toJsonString(stackId));
		out.append('}');
		return out.toString();
	}

	static String scoredChunkJson(ScoredChunk source) {
		var chunk = source.chunk();
		StringBuilder out = new StringBuilder("{");
		out.append("\"chunkId\":").append(toJsonString(chunk.chunkId())).append(',');
		out.append("\"location\":").append(toJsonString(chunk.location())).append(',');
		out.append("\"title\":").append(toJsonString(chunk.title())).append(',');
		out.append("\"text\":").append(toJsonString(chunk.text())).append(',');
		out.append("\"score\":").append(String.format(Locale.US, "%.8f", source.score())).append(',');
		out.append("\"lexicalScore\":").append(String.format(Locale.US, "%.8f", source.lexicalScore())).append(',');
		out.append("\"vectorScore\":").append(String.format(Locale.US, "%.8f", source.vectorScore()));
		out.append('}');
		return out.toString();
	}

	static String tokenJson(String text) {
		return "{\"text\":" + toJsonString(text) + "}";
	}

	static String errorJson(String message) {
		return "{\"message\":" + toJsonString(message) + "}";
	}

	static String doneJson(long elapsedMs, String stackId) {
		return "{\"elapsedMs\":" + elapsedMs + ",\"stackId\":" + toJsonString(stackId) + "}";
	}

	static String toJsonString(String s) {
		if (s == null) {
			return "null";
		}
		StringBuilder b = new StringBuilder();
		b.append('"');
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
			case '"':
				b.append("\\\"");
				break;
			case '\\':
				b.append("\\\\");
				break;
			case '\n':
				b.append("\\n");
				break;
			case '\r':
				b.append("\\r");
				break;
			case '\t':
				b.append("\\t");
				break;
			default:
				b.append(c);
			}
		}
		b.append('"');
		return b.toString();
	}
}
