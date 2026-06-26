package com.cse.ai.llm;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal JSON helpers for chat HTTP APIs.
 */
final class ChatJson {
	private static final Pattern CONTENT_FIELD = Pattern.compile("\"content\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
	private static final Pattern DELTA_CONTENT = Pattern.compile("\"content\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
	private static final Pattern ANTHROPIC_DELTA = Pattern.compile("\"text\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");

	private ChatJson() {
	}

	static String ollamaRequest(String model, List<ChatMessage> messages, boolean stream, double temperature) {
		StringBuilder sb = new StringBuilder();
		sb.append("{\"model\":\"").append(escape(model)).append("\",\"stream\":").append(stream);
		sb.append(",\"messages\":[");
		for (int i = 0; i < messages.size(); i++) {
			if (i > 0) {
				sb.append(',');
			}
			ChatMessage msg = messages.get(i);
			sb.append("{\"role\":\"").append(escape(msg.role())).append("\",\"content\":\"")
					.append(escape(msg.content())).append("\"}");
		}
		sb.append("],\"options\":{\"temperature\":").append(temperature).append("}}");
		return sb.toString();
	}

	static String openAiRequest(String model, List<ChatMessage> messages, boolean stream, double temperature,
			int maxTokens) {
		StringBuilder sb = new StringBuilder();
		sb.append("{\"model\":\"").append(escape(model)).append("\",\"stream\":").append(stream);
		sb.append(",\"temperature\":").append(temperature).append(",\"max_tokens\":").append(maxTokens);
		sb.append(",\"messages\":[");
		for (int i = 0; i < messages.size(); i++) {
			if (i > 0) {
				sb.append(',');
			}
			ChatMessage msg = messages.get(i);
			sb.append("{\"role\":\"").append(escape(msg.role())).append("\",\"content\":\"")
					.append(escape(msg.content())).append("\"}");
		}
		sb.append("]}");
		return sb.toString();
	}

	static String anthropicRequest(String model, String system, List<ChatMessage> userMessages, boolean stream,
			int maxTokens) {
		StringBuilder sb = new StringBuilder();
		sb.append("{\"model\":\"").append(escape(model)).append("\",\"max_tokens\":").append(maxTokens);
		sb.append(",\"stream\":").append(stream);
		if (system != null && !system.isBlank()) {
			sb.append(",\"system\":\"").append(escape(system)).append("\"");
		}
		sb.append(",\"messages\":[");
		boolean first = true;
		for (ChatMessage msg : userMessages) {
			if ("system".equals(msg.role())) {
				continue;
			}
			if (!first) {
				sb.append(',');
			}
			first = false;
			sb.append("{\"role\":\"").append(escape(msg.role())).append("\",\"content\":\"")
					.append(escape(msg.content())).append("\"}");
		}
		sb.append("]}");
		return sb.toString();
	}

	static String parseOllamaContent(String json) {
		return extractContent(json);
	}

	static String parseOpenAiDelta(String line) {
		if (line.startsWith("data: ")) {
			line = line.substring(6).trim();
		}
		if ("[DONE]".equals(line)) {
			return "";
		}
		Matcher matcher = DELTA_CONTENT.matcher(line);
		if (matcher.find()) {
			return unescape(matcher.group(1));
		}
		return "";
	}

	static String parseOpenAiComplete(String json) {
		return extractContent(json);
	}

	static String parseAnthropicDelta(String line) {
		Matcher matcher = ANTHROPIC_DELTA.matcher(line);
		if (matcher.find()) {
			return unescape(matcher.group(1));
		}
		return "";
	}

	static String parseAnthropicComplete(String json) {
		Matcher matcher = ANTHROPIC_DELTA.matcher(json);
		StringBuilder sb = new StringBuilder();
		while (matcher.find()) {
			sb.append(unescape(matcher.group(1)));
		}
		return sb.toString();
	}

	private static String extractContent(String json) {
		Matcher matcher = CONTENT_FIELD.matcher(json);
		String last = "";
		while (matcher.find()) {
			last = unescape(matcher.group(1));
		}
		return last;
	}

	private static String escape(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
	}

	private static String unescape(String value) {
		return value.replace("\\n", "\n").replace("\\r", "\r").replace("\\\"", "\"").replace("\\\\", "\\");
	}
}
