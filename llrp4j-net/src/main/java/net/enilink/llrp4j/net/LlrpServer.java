package net.enilink.llrp4j.net;

import java.io.Closeable;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.llrp.enumerations.ConnectionAttemptStatusType;
import org.llrp.messages.READER_EVENT_NOTIFICATION;
import org.llrp.parameters.ConnectionAttemptEvent;
import org.llrp.parameters.UTCTimestamp;

import net.enilink.llrp4j.LlrpContext;
import net.enilink.llrp4j.types.LlrpMessage;

public class LlrpServer implements Closeable {
	private NioServer nioServer;
	private IoHandler handler;

	private volatile SocketChannel channel;

	public LlrpServer(LlrpContext context, String host, int port) throws IOException {
		handler = createHandler(context);
		nioServer = new NioServer(InetAddress.getByName(host), port, handler) {
			@Override
			protected SocketChannel acceptChannel(ServerSocketChannel serverSocketChannel) throws IOException {
				SocketChannel newChannel = super.acceptChannel(serverSocketChannel);
				READER_EVENT_NOTIFICATION evtNotification = new READER_EVENT_NOTIFICATION();
				evtNotification.readerEventNotificationData().timestamp(new UTCTimestamp().microseconds(
						BigInteger.valueOf(System.currentTimeMillis()).multiply(BigInteger.valueOf(1000))));
				ConnectionAttemptEvent connectionAttemptEvent = evtNotification.readerEventNotificationData()
						.connectionAttemptEvent();
				if (channel == null) {
					channel = newChannel;
					connectionAttemptEvent.status(ConnectionAttemptStatusType.Success);
					send(newChannel, handler.encodeMessage(evtNotification));
				} else {
					connectionAttemptEvent
							.status(ConnectionAttemptStatusType.Failed_A_Client_Initiated_Connection_Already_Exists);
					send(newChannel, handler.encodeMessage(evtNotification));
					synchronized (this.pendingChanges) {
						// Indicate we want the interest ops set changed
						this.pendingChanges.add(new ChangeRequest(newChannel, ChangeRequest.CLOSE, 0));
					}
				}
				return channel;
			}
		};
		new Thread(nioServer).start();
	}

	private IoHandler createHandler(LlrpContext context) {
		IoSession ioSession = new IoSession() {
			@Override
			public void send(ByteBuffer data) {
				nioServer.send(channel, data);
			}
		};
		return new IoHandler(context, ioSession, true, false);
	}

	public static LlrpServer create(LlrpContext context, String host) throws IOException {
		return create(context, host, LlrpConstants.DEFAULT_PORT);
	}

	public static LlrpServer create(LlrpContext context, String host, int port) throws IOException {
		return new LlrpServer(context, host, port);
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

	public LlrpServer setEndpoint(LlrpEndpoint endpoint) {
		handler.setEndpoint(endpoint);
		return this;
	}

	public void close() throws IOException {
		if (nioServer != null) {
			nioServer.close();
			nioServer = null;
		}
	}
}
