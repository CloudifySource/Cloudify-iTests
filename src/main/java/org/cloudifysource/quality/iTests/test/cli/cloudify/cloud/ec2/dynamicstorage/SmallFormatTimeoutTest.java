package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2.dynamicstorage;

import java.util.concurrent.TimeoutException;

import org.cloudifysource.quality.iTests.framework.utils.AssertUtils;
import org.cloudifysource.quality.iTests.framework.utils.ServiceInstaller;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class SmallFormatTimeoutTest extends AbstractDynamicStorageTest {
	
	private static final String FOLDER_NAME = "small-format-timeout";

	private ServiceInstaller installer; 
	
	@Override
	protected String getCloudName() {
		return "ec2";
	}
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testLinux() throws Exception {
		super.testLinux();
	}
	

	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = false)
	public void testUbuntu() throws Exception  {
		super.testUbuntu();
	}
	
	@Override
	public void doTest() throws Exception {
		
		installer = new ServiceInstaller(getRestUrl(), SERVICE_NAME);
		installer.recipePath(FOLDER_NAME);
		installer.timeoutInMinutes(5);
		installer.setDisableSelfHealing(true);
		String installOutput = installer.install();
		
		// the installation should not succeed because the format timeout is extremely small (5 millis)
		// see src/main/resources/apps/USM/usm/dynamicstroage/small-format-time/groovy.service
		// so we expect the TimeoutException to propagate to the CLI.
		AssertUtils.assertTrue("installation output should have contained a TimeoutException", installOutput.contains("TimeoutException"));
		
		installer.uninstall();	
	}
	
	@AfterMethod
	public void scanForLeakes() throws TimeoutException {
		super.scanForLeakedVolumesCreatedViaTemplate("SMALL_BLOCK");
	}
	
	
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}

	@Override
	protected boolean isReusableCloud() {
		return false;
	}

	@Override
	public String getServiceFolder() {
		return FOLDER_NAME;
	}
}