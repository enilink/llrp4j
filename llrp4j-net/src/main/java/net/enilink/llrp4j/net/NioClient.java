package net.enilink.llrp4j.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

class NioClient implements Runnable, AutoCloseable {
	// The host:port combination to connect to
	private InetAddress hostAddress;
	private int port;

	// The selector we'll be monitoring
	private Selector selector;

	// The buffer into which we'll read data when it's available
	private ByteBuffer readBuffer = ByteBuffer.allocate(8192);

	// A list of PendingChange instances
	private List<ChangeRequest> pendingChanges = new LinkedList<>();

	// Maps a SocketChannel to a list of ByteBuffer instances
	private final Queue<ByteBuffer> pendingData = new ConcurrentLinkedQueue<>();

	private final IoHandler handler;

	private SocketChannel channel;

	public NioClient(InetAddress hostAddress, int port, IoHandler handler, int timeout) throws IOException {
		this.hostAddress = hostAddress;
		this.port = port;
		this.handler = handler;
		this.selector = initSelector();
		try {
			this.channel = initConnection(true, timeout);
		} catch (IOException e) {
			close();
			throw e;
		}
	}

	public void send(ByteBuffer data) {
		// Indicate we want the interest ops set changed
		synchronized (this.pendingChanges) {
			this.pendingChanges.add(new ChangeRequest(channel, ChangeRequest.CHANGEOPS, SelectionKey.OP_WRITE));
		}

		// And queue the data we want written
		pendingData.add(data);

		// Finally, wake up our selecting thread so it can make the required
		// changes
		this.selector.wakeup();
	}

	public void run() {
		while (selector.isOpen()) {
			try {
				// Process any pending changes
				synchronized (this.pendingChanges) {
					Iterator<ChangeRequest> changes = this.pendingChanges.iterator();
					while (changes.hasNext()) {
						ChangeRequest change = changes.next();
						switch (change.type) {
						case ChangeRequest.CHANGEOPS:
							SelectionKey key = change.socket.keyFor(selector);
							if (key != null && key.isValid()) {
								key.interestOps(change.ops);
							}
							break;
						case ChangeRequest.REGISTER:
							change.socket.register(selector, change.ops);
							break;
						}
					}
					this.pendingChanges.clear();
				}

				// Wait for an event one of the registered channels
				selector.select();
				if (!selector.isOpen()) {
					return;
				}

				// Iterate over the set of keys for which events are available
				Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
				while (selectedKeys.hasNext()) {
					SelectionKey key = selectedKeys.next();
					selectedKeys.remove();

					if (!key.isValid()) {
						continue;
					}

					// Check what event is available and deal with it
					if (key.isConnectable()) {
						this.finishConnection(key);
					} else if (key.isReadable()) {
						this.read(key);
					} else if (key.isWritable()) {
						this.write(key);
					}
				}
			} catch (Exception e) {
				handler.handleException("Error while processing network channel", e);
			}
		}
	}

	private void read(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();

		// Clear out our read buffer so it's ready for new data
		this.readBuffer.clear();

		// Attempt to read off the channel
		int numRead;
		try {
			numRead = socketChannel.read(this.readBuffer);
		} catch (IOException e) {
			// The remote forcibly closed the connection, cancel
			// the selection key and close the channel.
			key.cancel();
			socketChannel.close();
			return;
		}

		if (numRead == -1) {
			// Remote entity shut the socket down cleanly. Do the
			// same from our end and cancel the channel.
			key.channel().close();
			key.cancel();
			return;
		}

		// Make a correctly sized copy of the data before handling it by another
		// method
		byte[] receivedData = new byte[numRead];
		System.arraycopy(this.readBuffer.array(), 0, receivedData, 0, numRead);
		handler.processData(socketChannel, receivedData);
	}

	private void write(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();

		// Write until there's no more data ...
		while (!pendingData.isEmpty()) {
			ByteBuffer buf = (ByteBuffer) pendingData.element();
			socketChannel.write(buf);
			if (buf.remaining() > 0) {
				// ... or the socket's buffer fills up
				break;
			}
			pendingData.remove();
		}

		if (pendingData.isEmpty()) {
			// We wrote away all data, so we're no longer interested
			// in writing on this socket. Switch back to waiting for
			// data.
			key.interestOps(SelectionKey.OP_READ);
		}
	}

	private void finishConnection(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();

		// Finish the connection. If the connection operation failed
		// this will raise an IOException.
		try {
			socketChannel.finishConnect();
		} catch (IOException e) {
			// Cancel the channel's registration with our selector
			key.cancel();
			return;
		}

		// Register an interest in writing on this channel
		key.interestOps(SelectionKey.OP_WRITE);
	}

	private SocketChannel initConnection(boolean connectBlocking, int timeout) throws IOException {
		SocketChannel socketChannel = SocketChannel.open();
		socketChannel.configureBlocking(connectBlocking);

		// Kick off connection establishment
		try {
			if (connectBlocking) {
				// When in blocking mode, call connect on the underlying socket
				// to be able to specify connection timeout.
				socketChannel.socket().connect(new InetSocketAddress(this.hostAddress, this.port), timeout);
			} else {
				socketChannel.connect(new InetSocketAddress(this.hostAddress, this.port));
			}
		} catch (IOException e) {
			socketChannel.close();
			throw e;
		}

		// set non-blocking mode
		if (connectBlocking) {
			socketChannel.socket().setSoTimeout(0);
			socketChannel.configureBlocking(false);
			socketChannel.register(this.selector, SelectionKey.OP_CONNECT);
			synchronized (this.pendingChanges) {
				this.pendingChanges
						.add(new ChangeRequest(socketChannel, ChangeRequest.CHANGEOPS, SelectionKey.OP_WRITE));
			}
		} else {
			// Queue a channel registration since the caller is not the
			// selecting thread. As part of the registration we'll register
			// an interest in connection events. These are raised when a channel
			// is ready to complete connection establishment.
			synchronized (this.pendingChanges) {
				this.pendingChanges
						.add(new ChangeRequest(socketChannel, ChangeRequest.REGISTER, SelectionKey.OP_CONNECT));
			}
		}
		return socketChannel;
	}

	private Selector initSelector() throws IOException {
		// Create a new selector
		return SelectorProvider.provider().openSelector();
	}

	public void close() throws IOException {
		try {
			if (channel != null) {
				channel.close();
				channel = null;
			}
		} finally {
			// Closing the selector exits the client loop thread 
			selector.close();
		}
	}
}
