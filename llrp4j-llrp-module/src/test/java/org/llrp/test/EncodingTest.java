package org.llrp.test;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.junit.Assert;
import org.junit.Test;
import org.llrp.enumerations.AccessSpecState;
import org.llrp.enumerations.AccessSpecStopTriggerType;
import org.llrp.enumerations.AirProtocols;
import org.llrp.messages.ADD_ACCESSSPEC;
import org.llrp.modules.LlrpModule;
import org.llrp.parameters.AccessSpec;
import org.llrp.parameters.C1G2Read;
import org.llrp.parameters.C1G2TagSpec;
import org.llrp.parameters.C1G2TargetTag;

import net.enilink.llrp4j.BinaryDecoder;
import net.enilink.llrp4j.BinaryEncoder;
import net.enilink.llrp4j.LlrpContext;
import net.enilink.llrp4j.XmlDecoder;
import net.enilink.llrp4j.XmlEncoder;
import net.enilink.llrp4j.bitbuffer.BitBuffer;
import net.enilink.llrp4j.types.BitList;
import net.enilink.llrp4j.types.LlrpMessage;
import net.enilink.llrp4j.xml.IndentingXMLStreamWriter;

public class EncodingTest {

	private <E> List<E> list(@SuppressWarnings("unchecked") E... elements) {
		return new ArrayList<>(Arrays.<E> asList(elements));
	}

	ADD_ACCESSSPEC createMsg() {
		ADD_ACCESSSPEC msg = new ADD_ACCESSSPEC();
		AccessSpec spec = new AccessSpec();
		spec.currentState(AccessSpecState.Disabled);
		spec.accessSpecID(2).antennaID(3).protocolID(AirProtocols.EPCGlobalClass1Gen2).roSpecID(2);
		spec.accessCommand().accessCommandOpSpec().add(new C1G2Read());
		spec.accessCommand().airProtocolTagSpec(new C1G2TagSpec().c1G2TargetTag(list(new C1G2TargetTag().mb(1)
				.tagMask(new BitList(new byte[] { 0b1111 })).tagData(new BitList(new byte[] { 0b111 })))));
		spec.accessSpecStopTrigger().accessSpecStopTrigger(AccessSpecStopTriggerType.Null).operationCountValue(1);
		// spec.custom().add(new ImpinjAccessSpecConfiguration());
		msg.accessSpec(spec);
		return msg;
	}

	@Test
	public void testBinary() throws Exception {
		LlrpContext ctx = LlrpContext.create(new LlrpModule());
		BinaryEncoder encoder = ctx.createBinaryEncoder();

		LlrpMessage msg = createMsg();

		BitBuffer buffer = BitBuffer.allocateDynamic();
		encoder.encodeMessage(msg, buffer);

		byte[] bytes = buffer.asByteArray();

		buffer.position(0);
		BinaryDecoder decoder = ctx.createBinaryDecoder();
		LlrpMessage msg2 = decoder.decodeMessage(BitBuffer.wrap(bytes));

		Assert.assertEquals(msg, msg2);
	}

	protected String toXml(LlrpContext ctx, LlrpMessage msg) throws Exception {
		XmlEncoder encoder = ctx.createXmlEncoder();
		XMLOutputFactory xof = XMLOutputFactory.newInstance();
		StringWriter sw = new StringWriter();
		XMLStreamWriter writer = xof.createXMLStreamWriter(sw);
		encoder.encodeMessage(msg, new IndentingXMLStreamWriter(writer));
		return sw.toString();
	}

	@Test
	public void testXml() throws Exception {
		LlrpContext ctx = LlrpContext.create(new LlrpModule());
		LlrpMessage msg = createMsg();
		String xml = toXml(ctx, msg);

		XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(xml));
		XmlDecoder decoder = ctx.createXmlDecoder();
		LlrpMessage msg2 = decoder.decodeMessage(reader);

		Assert.assertEquals(msg, msg2);
		Assert.assertEquals(xml, toXml(ctx, msg2));
	}
}
