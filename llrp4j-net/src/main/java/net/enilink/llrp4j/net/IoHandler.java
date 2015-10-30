package net.enilink.llrp4j.net;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.llrp.enumerations.ConnectionAttemptStatusType;
import org.llrp.messages.KEEPALIVE;
import org.llrp.messages.KEEPALIVE_ACK;
import org.llrp.messages.READER_EVENT_NOTIFICATION;
import org.llrp.parameters.ConnectionAttemptEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.enilink.llrp4j.LlrpContext;
import net.enilink.llrp4j.LlrpException;
import net.enilink.llrp4j.bitbuffer.BitBuffer;
import net.enilink.llrp4j.types.LlrpMessage;

class IoHandler {
	class Message {
		int length = -1;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
	}

	/**
	 * Simple deferred object for synchronous messages.
	 */
	class FutureResponse {
		LlrpMessage value;

		synchronized void resolve(LlrpMessage value) {
			this.value = value;
			notifyAll();
		}

		synchronized LlrpMessage get(long timeout) throws InterruptedException {
			if (value == null) {
				wait(timeout);
			}
			return value;
		}
	}

	private static LlrpEndpoint NOOP_ENDPOINT = new LlrpEndpoint() {
		@Override
		public void messageReceived(LlrpMessage message) {
		}

		@Override
		public void errorOccured(String message, Throwable cause) {
		}
	};

	private static Logger log = LoggerFactory.getLogger(IoHandler.class);

	private Map<Long, FutureResponse> syncMessages = new ConcurrentHashMap<>();
	private BlockingQueue<ConnectionAttemptEvent> connectionAttemptEventQueue = new LinkedBlockingQueue<ConnectionAttemptEvent>(
			1);
	private final boolean keepAliveAck;
	private final boolean keepAliveForward;

	private Map<SocketChannel, Message> messages = new HashMap<>();

	private LlrpContext context;

	private volatile LlrpEndpoint endpoint = NOOP_ENDPOINT;

	private IoSession ioSession;

	public IoHandler(LlrpContext context, IoSession ioSession, boolean keepAliveAck, boolean keepAliveForward) {
		this.context = context;
		this.ioSession = ioSession;
		this.keepAliveAck = keepAliveAck;
		this.keepAliveForward = keepAliveForward;
	}

	public void processData(SocketChannel channel, byte[] data) {
		Message message = currentMessage(channel);
		ByteArrayOutputStream b = message.baos;
		int offset = 0;
		if (message.length <= 0) {
			offset = Math.min(6 - b.size(), data.length);
			b.write(data, 0, offset);
			if (b.size() >= 6) {
				BitBuffer bits = BitBuffer.wrap(b.toByteArray());
				// skip reserved bits (3), version (3) and message type (10)
				bits.position(16);
				message.length = (int) bits.getLongUnsigned(32);
			}
		}
		if (message.length > 0) {
			int count = Math.min(message.length - b.size(), data.length - offset);
			b.write(data, offset, count);
			if (b.size() == message.length) {
				messages.remove(channel);

				BitBuffer messageData = BitBuffer.wrap(b.toByteArray()).slice(0, message.length * 8);
				try {
					LlrpMessage m = context.createBinaryDecoder().decodeMessage(messageData);
					handleMessage(m);
				} catch (Exception e) {
					e.printStackTrace();
					endpoint.errorOccured("Error while decoding message", e);
				}
				if (offset + count < data.length) {
					// a following message is already about to be
					// transferred
					int newMessageDataLength = data.length - count - offset;
					byte[] newMessageData = new byte[newMessageDataLength];
					System.arraycopy(data, offset + count, newMessageData, 0, newMessageDataLength);
					processData(channel, newMessageData);
				}
			}
		}
	}

	protected void send(LlrpMessage message) {
		if (log.isDebugEnabled()) {
			log.debug("send {} id={}", message.getClass().getSimpleName(), message.messageID());
		}
		ioSession.send(encodeMessage(message));
	}

	public LlrpMessage transact(LlrpMessage message, long timeout) throws InterruptedException {
		if (log.isDebugEnabled()) {
			log.debug("transact {} id={}", message.getClass().getSimpleName(), message.messageID());
		}

		Class<?> returnMessageType = message.getResponseType();
		if (void.class.equals(returnMessageType)) {
			throw new IllegalArgumentException("Message does not expect return message");
		}
		FutureResponse response = new FutureResponse();
		syncMessages.put(message.messageID(), response);
		send(message);
		return response.get(timeout);
	}

	protected ByteBuffer encodeMessage(LlrpMessage message) {
		BitBuffer bits = BitBuffer.allocateDynamic();
		context.createBinaryEncoder().encodeMessage(message, bits);
		return bits.asByteBuffer();
	}

	protected void handleMessage(LlrpMessage message) {
		if (log.isDebugEnabled()) {
			log.debug("received {} id={}", message.getClass().getSimpleName(), message.messageID());
		}
		if (message instanceof KEEPALIVE) {
			if (keepAliveForward) {
				endpoint.messageReceived(message);
			}
			if (keepAliveAck) {
				send(new KEEPALIVE_ACK());
				return;
			}
		}

		if (message instanceof READER_EVENT_NOTIFICATION) {
			ConnectionAttemptEvent connectionAttemptEvent = ((READER_EVENT_NOTIFICATION) message)
					.getReaderEventNotificationData().getConnectionAttemptEvent();
			if (connectionAttemptEvent != null) {
				if (connectionAttemptEventQueue.isEmpty()) {
					connectionAttemptEventQueue.add(connectionAttemptEvent);
				}
				endpoint.messageReceived(message);
				return;
			}
		}

		// send message only if not already handled by synchronous call
		FutureResponse reponse = syncMessages.remove(message.messageID());
		if (reponse == null) {
			log.debug("Calling messageReceived of endpoint ... ");
			endpoint.messageReceived(message);
		} else {
			reponse.resolve(message);
			log.debug("Adding message " + message.getClass() + " to transaction queue ");
		}
	}

	protected Message currentMessage(SocketChannel channel) {
		Message message = messages.get(channel);
		if (message == null) {
			message = new Message();
			messages.put(channel, message);
		}
		return message;
	}

	protected void handleException(String msg, Exception e) {
		endpoint.errorOccured(msg, e);
	}

	protected void awaitConnectionAttemptEvent(long timeout) {
		try {
			ConnectionAttemptEvent connectionAttemptEvent = connectionAttemptEventQueue.poll(timeout,
					TimeUnit.MILLISECONDS);
			if (connectionAttemptEvent != null) {
				ConnectionAttemptStatusType status = connectionAttemptEvent.status();
				if (status == ConnectionAttemptStatusType.Success) {
					log.info("LLRP reader reported successfull connection attempt (ConnectionAttemptEvent.Status = "
							+ status.toString() + ")");
				} else {
					log.info("LLRP reader reported failed connection attempt (ConnectionAttemptStatus = "
							+ status.toString() + ")");
					throw new LlrpException(status.toString());
				}
			} else {
				throw new LlrpException("Connection request timed out after " + timeout + " ms.");
			}
		} catch (InterruptedException e) {
			throw new LlrpException(e);
		}
	}

	protected void setEndpoint(LlrpEndpoint endpoint) {
		this.endpoint = endpoint == null ? NOOP_ENDPOINT : endpoint;
	}
}