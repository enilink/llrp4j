package org.llrp.test;

import org.junit.Test;
import org.llrp.modules.LlrpModule;

import net.enilink.llrp4j.test.AbstractModuleTest;

public class LlrpModuleTest extends AbstractModuleTest {
	public LlrpModuleTest() {
		super(new LlrpModule());
	}

	@Test
	public void testModule() throws Exception {
		super.testModule();
	}
}
