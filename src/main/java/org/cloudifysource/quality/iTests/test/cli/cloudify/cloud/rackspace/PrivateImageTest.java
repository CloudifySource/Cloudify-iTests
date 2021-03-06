package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.rackspace;

import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.AbstractExamplesTest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class PrivateImageTest extends AbstractExamplesTest {
	
	@Override
	protected String getCloudName() {
		return "rackspace";
	}

	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testHelloWorld() throws Exception {
		super.testHelloWorld();
	}

	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}
	
	@Override
	protected void customizeCloud() throws Exception {
        super.customizeCloud();
        getService().getProperties().put("linuxImageId", "DFW/2452746d-8d75-41d0-a49c-68be8ad32f2b");
	}
	
}
