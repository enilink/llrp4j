package org.llrp.impinj.test;

import org.junit.Test;
import org.llrp.impinj.modules.ImpinjModule;
import org.llrp.modules.LlrpModule;

import net.enilink.llrp4j.test.AbstractModuleTest;

public class ImpinjModuleTest extends AbstractModuleTest {
	public ImpinjModuleTest() {
		super(new ImpinjModule(), new LlrpModule());
	}

	@Test
	public void testModule() throws Exception {
		super.testModule();
	}
}
