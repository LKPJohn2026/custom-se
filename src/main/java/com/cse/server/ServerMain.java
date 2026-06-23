package com.cse.server;

/**
 * Phase 1 web entry point.
 */
public class ServerMain {
	public static void main(String[] args) throws Exception {
		ServerConfig config = ServerConfig.fromArgs(args);
		var index = IndexBuilder.build(config);

		AppContext ctx = new AppContext(index, config.threads);
		JettyServer server = new JettyServer(config.port, ctx);
		server.startAndJoin();
	}
}

