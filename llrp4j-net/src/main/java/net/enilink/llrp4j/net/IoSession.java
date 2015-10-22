package net.enilink.llrp4j.net;

import java.nio.ByteBuffer;

interface IoSession {
	void send(ByteBuffer data);
}
