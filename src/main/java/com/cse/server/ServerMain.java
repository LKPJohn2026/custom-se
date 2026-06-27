package com.cse.server;

import com.cse.ai.config.AiConfigException;
import com.cse.ai.config.AiConfigValidator;
import com.cse.ai.config.EnvFileLoader;
import com.cse.ai.profile.AiSettings;

/**
 * Phase 1 web entry point.
 */
public class ServerMain {
	public static void main(String[] args) throws Exception {
		try {
			EnvFileLoader.loadRequired();
			AiSettings settings = AiSettings.load();
			AiConfigValidator.validate(settings, settings.defaultStack());
		} catch (AiConfigException e) {
			System.out.println(e.getMessage());
			return;
		}

		ServerConfig config = ServerConfig.fromArgs(args);
		var index = IndexBuilder.build(config);

		AppContext ctx = new AppContext(index, config.threads);
		JettyServer server = new JettyServer(config.port, ctx);
		server.startAndJoin();
	}
}

