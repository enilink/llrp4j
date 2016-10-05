package net.enilink.llrp4j.net;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;

import net.enilink.llrp4j.LlrpContext;
import net.enilink.llrp4j.types.LlrpMessage;

public class LlrpClient implements Closeable {
	private NioClient nioClient;
	private IoHandler handler;

	protected LlrpClient(LlrpContext context, String host, int port, int timeout) throws IOException {
		handler = createHandler(context);
		nioClient = new NioClient(InetAddress.getByName(host), port, handler, timeout);
		new Thread(nioClient).start();
		try {
			handler.awaitConnectionAttemptEvent(timeout);
		} catch(RuntimeException ex) {
			nioClient.close();
			throw ex;
		}
	}

	private IoHandler createHandler(LlrpContext context) {
		IoSession ioSession = new IoSession() {
			@Override
			public void send(ByteBuffer data) {
				nioClient.send(data);
			}
		};
		return new IoHandler(context, ioSession, true, false);
	}

	public static LlrpClient create(LlrpContext context, String host) throws IOException {
		return create(context, host, LlrpConstants.DEFAULT_PORT);
	}

	public static LlrpClient create(LlrpContext context, String host, int port) throws IOException {
		return create(context, host, LlrpConstants.DEFAULT_PORT, LlrpConstants.DEFAULT_TIMEOUT);
	}

	public static LlrpClient create(LlrpContext context, String host, int port, int timeout) throws IOException {
		return new LlrpClient(context, host, port, timeout);
	}

	public void send(LlrpMessage message) {
		handler.send(message);
	}

	public LlrpMessage transact(LlrpMessage message, long timeout) throws InterruptedException {
		return handler.transact(message, timeout);
	}

	public LlrpMessage transact(LlrpMessage message) throws InterruptedException {
		return transact(message, LlrpConstants.DEFAULT_TIMEOUT);
	}

	public LlrpClient endpoint(LlrpEndpoint endpoint) {
		handler.setEndpoint(endpoint);
		return this;
	}

	public void close() throws IOException {
		if (nioClient != null) {
			nioClient.close();
			nioClient = null;
		}
	}
}
