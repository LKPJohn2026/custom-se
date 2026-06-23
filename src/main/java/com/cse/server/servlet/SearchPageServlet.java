package com.cse.server.servlet;

import java.io.IOException;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class SearchPageServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setStatus(HttpServletResponse.SC_OK);
		resp.setContentType("text/html; charset=UTF-8");

		resp.getWriter().println("""
				<!doctype html>
				<html lang="en">
				  <head>
				    <meta charset="utf-8" />
				    <meta name="viewport" content="width=device-width, initial-scale=1" />
				    <title>custom-se</title>
				  </head>
				  <body>
				    <h1>custom-se</h1>
				    <form action="/search" method="get">
				      <label>
				        Query:
				        <input type="text" name="q" />
				      </label>
				      <button type="submit">Search</button>
				    </form>
				  </body>
				</html>
				""");
	}
}

