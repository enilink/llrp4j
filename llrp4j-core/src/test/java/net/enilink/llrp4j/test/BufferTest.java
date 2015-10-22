package net.enilink.llrp4j.test;

import org.junit.Assert;
import org.junit.Test;

import net.enilink.llrp4j.bitbuffer.BitBuffer;

public class BufferTest {

	@Test
	public void test() throws Exception {
		BitBuffer b1 = BitBuffer.allocateDynamic();
		b1.putInt(23, 16).putLong(243, 32).putInt(67);

		Assert.assertEquals(243, b1.slice(16, 32).getIntUnsigned(32));
		b1.rewind();
		b1.getIntUnsigned(16);
		b1.getIntUnsigned(32);
		Assert.assertEquals(67, b1.slice(b1.position(), 32).getIntUnsigned(32));
	}
}
