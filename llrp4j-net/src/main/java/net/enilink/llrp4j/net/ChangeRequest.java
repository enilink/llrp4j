package net.enilink.llrp4j.net;

import java.nio.channels.SocketChannel;

class ChangeRequest {
	public static final int REGISTER = 1;
	public static final int CHANGEOPS = 2;
	public static final int CLOSE = 3;

	final SocketChannel socket;
	final int type;
	final int ops;

	ChangeRequest(SocketChannel socket, int type, int ops) {
		this.socket = socket;
		this.type = type;
		this.ops = ops;
	}
}