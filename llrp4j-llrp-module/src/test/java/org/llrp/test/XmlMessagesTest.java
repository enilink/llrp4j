package org.llrp.test;

import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.junit.Assert;
import org.junit.Test;
import org.llrp.modules.LlrpModule;

import net.enilink.llrp4j.LlrpContext;
import net.enilink.llrp4j.XmlDecoder;
import net.enilink.llrp4j.XmlEncoder;
import net.enilink.llrp4j.types.LlrpMessage;
import net.enilink.llrp4j.xml.IndentingXMLStreamWriter;

public class XmlMessagesTest {

	@Test
	public void testXmlFiles() throws Exception {
		String prefix = "/messages/";

		testFile(prefix + "SET_READER_CONFIG-1.xml");
		testFile(prefix + "ADD_ROSPEC-1.xml");
	}

	protected void testFile(String file) throws Exception {
		try {
			LlrpContext ctx = LlrpContext.create(new LlrpModule());
			XmlDecoder decoder = ctx.createXmlDecoder();

			XMLInputFactory xif = XMLInputFactory.newInstance();
			InputStream in = getClass().getResourceAsStream(file);
			LlrpMessage msg = decoder.decodeMessage(xif.createXMLStreamReader(in));
			
			String xml = toXml(ctx, msg);
			
			XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(xml));
			LlrpMessage msg2 = decoder.decodeMessage(reader);

			Assert.assertEquals(msg, msg2);
			Assert.assertEquals(xml, toXml(ctx, msg2));
		} catch (Exception e) {
			throw new RuntimeException("Error in file " + file, e);
		}
	}

	protected String toXml(LlrpContext ctx, LlrpMessage msg) throws Exception {
		XmlEncoder encoder = ctx.createXmlEncoder();
		XMLOutputFactory xof = XMLOutputFactory.newInstance();
		StringWriter sw = new StringWriter();
		XMLStreamWriter writer = xof.createXMLStreamWriter(sw);
		encoder.encodeMessage(msg, new IndentingXMLStreamWriter(writer));
		return sw.toString();
	}
}
