package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2.persistence;

import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.AbstractCloudManagementPersistencyTest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * 1. install a scalable service with one instance. 
 * 2. scale that service so that it will have two instances.
 * 3. kill one of the two management machines.
 * 4. assert ESM auto-scale fails both for scale-out and for scale-in.
 *  
 * @author adaml
 *
 */
public class Ec2PersistencyScaleOnManagerDownTest extends AbstractCloudManagementPersistencyTest {

	@Override
	protected String getCloudName() {
		return "ec2";
	}
	
    @AfterMethod
    public void afterTest() {
        restoreManagement();
        assertServiceInstalled(DEFAULT_APPLICATION_NAME, SCALABLE_SERVICE_NAME);
    }

	@Override
	protected boolean isReusableCloud() {
		return false;
	}
	
	@Override
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
		super.initManagementUrlsAndRestClient();
	}

	@Override
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = false)
    public void testAutoScaleServiceOnGsmFailure() throws Exception {
    	super.testAutoScaleServiceOnGsmFailure();
    }
}
