package com.cse.server;

/**
 * Phase 1 web entry point.
 */
public class ServerMain {
	public static void main(String[] args) throws Exception {
		ServerConfig config = ServerConfig.fromArgs(args);
		var index = IndexBuilder.build(config);

		JettyServer server = new JettyServer(config.port, index);
		server.startAndJoin();
	}
}

