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

	protected LlrpClient(LlrpContext context, LlrpEndpoint endpoint, String host, int port, int timeout)
			throws IOException {
		handler = createHandler(context, endpoint);
		nioClient = new NioClient(InetAddress.getByName(host), port, handler);
		new Thread(nioClient).start();
		handler.awaitConnectionAttemptEvent(timeout);
	}

	private IoHandler createHandler(LlrpContext context, LlrpEndpoint endpoint) {
		IoSession ioSession = new IoSession() {
			@Override
			public void send(ByteBuffer data) {
				nioClient.send(data);
			}
		};
		return new IoHandler(context, endpoint, ioSession, true, false);
	}

	public static LlrpClient create(LlrpContext context, LlrpEndpoint endpoint, String host) throws IOException {
		return create(context, endpoint, host, LlrpConstants.DEFAULT_PORT);
	}

	public static LlrpClient create(LlrpContext context, LlrpEndpoint endpoint, String host, int port)
			throws IOException {
		return create(context, endpoint, host, LlrpConstants.DEFAULT_PORT, LlrpConstants.DEFAULT_TIMEOUT);
	}

	public static LlrpClient create(LlrpContext context, LlrpEndpoint endpoint, String host, int port, int timeout)
			throws IOException {
		return new LlrpClient(context, endpoint, host, port, timeout);
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

	public void close() throws IOException {
		if (nioClient != null) {
			nioClient.close();
			nioClient = null;
		}
	}
}
