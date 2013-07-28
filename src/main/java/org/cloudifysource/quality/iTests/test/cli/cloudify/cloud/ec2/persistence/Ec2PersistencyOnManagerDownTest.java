package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2.persistence;

import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.AbstractCloudManagementPersistencyTest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * 1. install a scalable service and a simple application
 * 2. shut down one of 2 management machines
 * 3. assert all uninstall, install, and scale attempts fail.
 * 
 * @author adaml
 *
 */
public class Ec2PersistencyOnManagerDownTest  extends AbstractCloudManagementPersistencyTest{
	
    @AfterMethod
    public void afterTest() {
        restoreManagement();
        assertApplicationInstalled(SIMPLE_APPLICATION_NAME);
        assertServiceInstalled(DEFAULT_APPLICATION_NAME, SCALABLE_SERVICE_NAME);
    }
    
	@Override
	protected String getCloudName() {
		return "ec2";
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
	
    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testInstallServiceAndApplicationOnGsmFailure() throws Exception {
    	super.testInstallServiceAndApplicationOnGsmFailure();
    }
}
