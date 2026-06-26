package com.cse.ai.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

public class OllamaLlmClientTest {
	private HttpServer server;
	private String baseUrl;

	@BeforeEach
	public void setUp() throws Exception {
		server = HttpServer.create(new InetSocketAddress(0), 0);
		server.createContext("/api/chat", exchange -> {
			String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
			byte[] response;
			if (body.contains("\"stream\":true")) {
				response = "{\"message\":{\"content\":\"Hel\"}}\n{\"message\":{\"content\":\"lo\"}}\n".getBytes();
				exchange.getResponseHeaders().add("Content-Type", "application/x-ndjson");
			} else {
				response = "{\"message\":{\"role\":\"assistant\",\"content\":\"Hello\"}}".getBytes();
				exchange.getResponseHeaders().add("Content-Type", "application/json");
			}
			exchange.sendResponseHeaders(200, response.length);
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(response);
			}
		});
		server.start();
		baseUrl = "http://localhost:" + server.getAddress().getPort();
	}

	@AfterEach
	public void tearDown() {
		if (server != null) {
			server.stop(0);
		}
	}

	@Test
	public void testCompleteChat() {
		var client = new OllamaLlmClient(HttpClient.newHttpClient(), baseUrl, "llama3.2");
		String answer = client.completeChat(ChatRequest.of("system", "user question"));
		assertEquals("Hello", answer);
	}

	@Test
	public void testStreamChat() {
		var client = new OllamaLlmClient(HttpClient.newHttpClient(), baseUrl, "llama3.2");
		String streamed = client.streamChat(ChatRequest.of("system", "user question"))
				.collect(Collectors.joining());
		assertEquals("Hello", streamed);
		assertEquals("ollama", client.providerId());
		assertFalse(client.model().isBlank());
	}
}
