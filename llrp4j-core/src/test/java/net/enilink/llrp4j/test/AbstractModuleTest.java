package net.enilink.llrp4j.test;

import static net.enilink.llrp4j.test.TestUtil.mockObject;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.junit.Assert;
import org.junit.Test;

import net.enilink.llrp4j.BinaryDecoder;
import net.enilink.llrp4j.BinaryEncoder;
import net.enilink.llrp4j.LlrpContext;
import net.enilink.llrp4j.Module;
import net.enilink.llrp4j.XmlDecoder;
import net.enilink.llrp4j.XmlEncoder;
import net.enilink.llrp4j.bitbuffer.BitBuffer;
import net.enilink.llrp4j.types.LlrpMessage;
import net.enilink.llrp4j.xml.IndentingXMLStreamWriter;

public abstract class AbstractModuleTest {
	final Module module;
	final Module combined;

	public AbstractModuleTest(Module module, Module... required) {
		this.module = module;
		this.combined = new Module();
		combined.include(module);
		for (Module r : required) {
			combined.include(r);
		}
	}

	@Test
	public void testModule() throws Exception {
		final Random rnd = new Random(1337);
		LlrpContext ctx = LlrpContext.create(combined);

		final List<Class<?>> msgTypes = new ArrayList<>();
		for (Class<?> c : module.getClasses()) {
			if (LlrpMessage.class.isAssignableFrom(c)) {
				msgTypes.add(c);
			}
		}
		for (Class<?> msgType : msgTypes) {
			LlrpMessage msg = createMsg(msgType, rnd);

			try {
				testBinaryEncoding(ctx, msg);
			} catch (Exception e) {
				throw new AssertionError("Binary encoding test failed.", e);
			}

			try {
				testXmlEncoding(ctx, msg);
			} catch (Exception e) {
				throw new AssertionError("XML encoding test failed.", e);
			}
		}
	}

	protected void testBinaryEncoding(LlrpContext ctx, LlrpMessage msg) throws Exception {
		BinaryEncoder encoder = ctx.createBinaryEncoder();

		BitBuffer buffer = BitBuffer.allocateDynamic();
		encoder.encodeMessage(msg, buffer);

		byte[] bytes = buffer.asByteArray();

		BinaryDecoder decoder = ctx.createBinaryDecoder();
		LlrpMessage msg2 = decoder.decodeMessage(BitBuffer.wrap(bytes));

		Assert.assertEquals("Decoded binary message should be the same as the original message.", msg, msg2);
	}

	protected void testXmlEncoding(LlrpContext ctx, LlrpMessage msg) throws Exception {
		String xml = toXml(ctx, msg);

		XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(xml));
		XmlDecoder decoder = ctx.createXmlDecoder();
		LlrpMessage msg2 = decoder.decodeMessage(reader);

		try {
			Assert.assertEquals("Decoded XML message should be the same as the original message.", msg, msg2);
		} catch (Throwable e) {
			Assert.assertEquals(toXml(ctx, msg2), xml);
			throw e;
		}
		Assert.assertEquals("Encoding an XML message should always produce the same result.", xml, toXml(ctx, msg2));
	}

	protected String toXml(LlrpContext ctx, LlrpMessage msg) throws Exception {
		XmlEncoder encoder = ctx.createXmlEncoder();
		XMLOutputFactory xof = XMLOutputFactory.newInstance();
		StringWriter sw = new StringWriter();
		XMLStreamWriter writer = xof.createXMLStreamWriter(sw);
		encoder.encodeMessage(msg, new IndentingXMLStreamWriter(writer));
		return sw.toString();
	}

	protected LlrpMessage createMsg(Class<?> msgType, Random rnd) throws Exception {
		return (LlrpMessage) mockObject(msgType.newInstance(), combined.getClasses(), rnd);
	}
}
