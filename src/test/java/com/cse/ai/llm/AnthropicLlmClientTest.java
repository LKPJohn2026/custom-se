package com.cse.ai.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

public class AnthropicLlmClientTest {
	private HttpServer server;
	private String messagesUrl;

	@BeforeEach
	public void setUp() throws Exception {
		server = HttpServer.create(new InetSocketAddress(0), 0);
		server.createContext("/v1/messages", exchange -> {
			String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
			byte[] response;
			if (body.contains("\"stream\":true")) {
				response = "data: {\"type\":\"content_block_delta\",\"delta\":{\"type\":\"text_delta\",\"text\":\"Hi\"}}\n"
						.getBytes();
				exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
			} else {
				response = "{\"content\":[{\"type\":\"text\",\"text\":\"Hello\"}]}".getBytes();
				exchange.getResponseHeaders().add("Content-Type", "application/json");
			}
			exchange.sendResponseHeaders(200, response.length);
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(response);
			}
		});
		server.start();
		messagesUrl = "http://localhost:" + server.getAddress().getPort() + "/v1/messages";
	}

	@AfterEach
	public void tearDown() {
		if (server != null) {
			server.stop(0);
		}
	}

	@Test
	public void testCompleteChat() {
		var client = new AnthropicLlmClient(HttpClient.newHttpClient(), messagesUrl, "key", "claude-sonnet");
		assertEquals("Hello", client.completeChat(ChatRequest.of("system", "question")));
	}

	@Test
	public void testStreamChat() {
		var client = new AnthropicLlmClient(HttpClient.newHttpClient(), messagesUrl, "key", "claude-sonnet");
		String text = client.streamChat(ChatRequest.of("system", "question")).collect(Collectors.joining());
		assertEquals("Hi", text);
		assertEquals("claude", client.providerId());
	}
}
