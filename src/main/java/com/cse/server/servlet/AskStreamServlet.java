package com.cse.server.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

import com.cse.ai.llm.LlmException;
import com.cse.ai.profile.AiProfile;
import com.cse.ai.rag.RagStreamContext;
import com.cse.server.session.SessionService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * SSE streaming endpoint for Ask mode.
 */
public class AskStreamServlet extends BaseServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		handle(req, resp, false);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		handle(req, resp, true);
	}

	private void handle(HttpServletRequest req, HttpServletResponse resp, boolean requireCsrf) throws IOException {
		if (requireCsrf && !requireCsrf(req, resp)) {
			return;
		}
		if (!rateLimitAsk(req, resp)) {
			return;
		}
		String question = req.getParameter("q");
		if (question == null || question.isBlank()) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing question");
			return;
		}

		resp.setStatus(HttpServletResponse.SC_OK);
		resp.setContentType("text/event-stream; charset=UTF-8");
		resp.setHeader("Cache-Control", "no-cache");
		resp.setHeader("Connection", "keep-alive");
		PrintWriter out = resp.getWriter();

		var session = SessionService.get(req);
		AiProfile profile = app().aiProfileResolver().resolve(session.aiPreferences());
		long start = System.currentTimeMillis();

		try {
			RagStreamContext ctx = app().ragService().prepareStream(question, profile);
			AskResponseWriter.writeSseEvent(out, "retrieval", AskResponseWriter.sourcesJson(ctx.sources()));

			Iterator<String> tokens = ctx.tokens().iterator();
			while (tokens.hasNext()) {
				AskResponseWriter.writeSseEvent(out, "token", AskResponseWriter.tokenJson(tokens.next()));
			}

			long elapsed = System.currentTimeMillis() - start;
			AskResponseWriter.writeSseEvent(out, "done",
					AskResponseWriter.doneJson(elapsed, ctx.stackId()));
		} catch (LlmException e) {
			AskResponseWriter.writeSseEvent(out, "error",
					AskResponseWriter.errorJson("Unable to generate an answer. Check AI settings."));
		} catch (IOException | RuntimeException e) {
			AskResponseWriter.writeSseEvent(out, "error",
					AskResponseWriter.errorJson("Ask failed. Try keyword search instead."));
		}
	}
}
