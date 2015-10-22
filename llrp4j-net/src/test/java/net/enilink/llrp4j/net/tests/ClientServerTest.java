package net.enilink.llrp4j.net.tests;

import org.junit.Test;
import org.llrp.modules.LlrpModule;

import net.enilink.llrp4j.LlrpContext;
import net.enilink.llrp4j.net.LlrpClient;
import net.enilink.llrp4j.net.LlrpEndpoint;
import net.enilink.llrp4j.net.LlrpServer;
import net.enilink.llrp4j.types.LlrpMessage;

public class ClientServerTest {
	static final String LOCALHOST = "127.0.0.1";

	@Test
	public void testConnect() throws Exception {
		LlrpEndpoint endpoint = new LlrpEndpoint() {
			@Override
			public void messageReceived(LlrpMessage message) {
			}

			@Override
			public void errorOccured(String message, Throwable cause) {
			}
		};
		LlrpContext ctx = LlrpContext.create(new LlrpModule());
		LlrpServer server = LlrpServer.create(ctx, endpoint, LOCALHOST);
		LlrpClient client = LlrpClient.create(ctx, endpoint, LOCALHOST);
		client.close();
		server.close();
	}
}
