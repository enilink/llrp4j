package net.enilink.llrp4j.net.tests;

import net.enilink.llrp4j.LlrpContext;
import net.enilink.llrp4j.net.LlrpClient;
import net.enilink.llrp4j.net.LlrpEndpoint;
import net.enilink.llrp4j.net.LlrpServer;
import net.enilink.llrp4j.types.LlrpMessage;
import org.junit.BeforeClass;
import org.junit.Test;
import org.llrp.messages.READER_EVENT_NOTIFICATION;
import org.llrp.modules.LlrpModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static net.enilink.llrp4j.test.TestUtil.mockObject;
import static net.enilink.llrp4j.test.TestUtil.responseType;

public class ClientServerTest {
	final static Logger log = LoggerFactory.getLogger(ClientServerTest.class);

	static final String LOCALHOST = "127.0.0.1";

	static LlrpModule module;

	@BeforeClass
	public static void setup() {
		module = new LlrpModule();
	}

	@Test
	public void testMsgExchange() throws Exception {
		final LlrpContext ctx = LlrpContext.create(module);
		final LlrpServer server = LlrpServer.create(ctx, LOCALHOST);
		final LlrpClient client = LlrpClient.create(ctx, LOCALHOST);

		final Random rnd = new Random(1337);
		final int eventNotifications = 20;
		final List<Class<?>> clientMsgTypes = new ArrayList<>();
		int responses = 0;
		for (Class<?> c : module.getClasses()) {
			if (LlrpMessage.class.isAssignableFrom(c) && !c.getSimpleName().endsWith("_RESPONSE")) {
				clientMsgTypes.add(c);
				if (!void.class.equals(responseType(c))) {
					responses++;
				}
			}
		}
		final CountDownLatch latch = new CountDownLatch(eventNotifications + clientMsgTypes.size() + responses);

		LlrpEndpoint serverEndpoint = new LlrpEndpoint() {
			@Override
			public void messageReceived(LlrpMessage message) {
				latch.countDown();

				Class<?> responseType = message.getResponseType();
				if (!void.class.equals(responseType)) {
					try {
						server.send((LlrpMessage) mockObject(responseType.newInstance(), module.getClasses(), rnd));
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			}

			@Override
			public void errorOccured(String message, Throwable cause) {
			}
		};

		server.endpoint(serverEndpoint);

		LlrpEndpoint clientEndpoint = new LlrpEndpoint() {
			@Override
			public void messageReceived(LlrpMessage message) {
				latch.countDown();
				log.info("Message received: {}", message);
			}

			@Override
			public void errorOccured(String message, Throwable cause) {
			}
		};
		client.endpoint(clientEndpoint);

		new Thread(new Runnable() {
			@Override
			public void run() {
				for (int i = 0; i < eventNotifications; i++) {
					try {
						server.send(mockObject(new READER_EVENT_NOTIFICATION(), module.getClasses(), rnd));
						Thread.sleep(rnd.nextInt(10));
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			}
		}).start();

		for (Class<?> msgType : clientMsgTypes) {
			client.send((LlrpMessage) mockObject(msgType.newInstance(), module.getClasses(), rnd));
			Thread.sleep(rnd.nextInt(10));
		}

		latch.await(10, TimeUnit.SECONDS);

		client.close();
		server.close();
	}

}
