package com.cse.server.servlet;

import java.io.IOException;

import com.cse.ai.llm.LlmException;
import com.cse.ai.profile.AiProfile;
import com.cse.ai.rag.RagResponse;
import com.cse.server.session.SessionService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * JSON Ask API for programmatic clients.
 */
public class AskApiServlet extends BaseServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		if (!rateLimitAsk(req, resp)) {
			return;
		}
		String question = req.getParameter("q");
		if (question == null || question.isBlank()) {
			resp.setStatus(HttpServletResponse.SC_OK);
			resp.setContentType("application/json; charset=UTF-8");
			resp.getWriter().println("{\"answer\":\"\",\"sources\":[],\"retrievalMs\":0,\"generationMs\":0,\"stackId\":\"\"}");
			return;
		}

		var session = SessionService.get(req);
		AiProfile profile = app().aiProfileResolver().resolve(session.aiPreferences());
		resp.setContentType("application/json; charset=UTF-8");

		try {
			RagResponse response = app().ragService().ask(question, profile, session);
			resp.setStatus(HttpServletResponse.SC_OK);
			resp.getWriter().println(AskResponseWriter.ragResponseJson(
					response.answer(), response.sources(), response.retrievalMs(),
					response.generationMs(), response.stackId()));
		} catch (LlmException e) {
			resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
			resp.getWriter().println(AskResponseWriter.errorJson("Unable to generate an answer."));
		} catch (IOException | RuntimeException e) {
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			resp.getWriter().println(AskResponseWriter.errorJson("Ask failed."));
		}
	}
}
