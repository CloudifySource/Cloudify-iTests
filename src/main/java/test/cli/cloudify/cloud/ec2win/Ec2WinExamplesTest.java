package test.cli.cloudify.cloud.ec2win;

import java.io.File;
import java.io.IOException;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import test.cli.cloudify.CommandTestUtils;
import test.cli.cloudify.cloud.AbstractExamplesTest;
import framework.tools.SGTestHelper;
import framework.utils.LogUtils;

public class Ec2WinExamplesTest extends AbstractExamplesTest {
	
	private static final String WINDOWS_APPS_PATH = CommandTestUtils.getPath("src/main/resources/apps/USM/usm/applications/windows");
	
	@Override
	protected String getCloudName() {
		return "ec2-win";
	}
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		try {
			prepareApplications();
		} catch (final Exception e) {
			Assert.fail("Failed preparing windows applications for deployment. Reason : " + e.getMessage());
		}
		super.bootstrap();
	}
	
	private void prepareApplications() throws IOException {
		prepareApplication("travel-win");
		prepareApplication("petclinic-simple-win");
		prepareApplication("petclinic-win");
		prepareApplication("helloworld-win");
	}
		
	private void prepareApplication(String applicationName) throws IOException {
		
		String applicationSGPath = WINDOWS_APPS_PATH + "/" + applicationName;
		String applicationBuildPath = SGTestHelper.getBuildDir() + "/recipes/apps/";
		
		LogUtils.log("copying " + applicationSGPath + " to " + applicationBuildPath);
		FileUtils.copyDirectoryToDirectory(new File(applicationSGPath), new File(applicationBuildPath));
		
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testTravel() throws Exception {
		super.testTravel();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testPetclinicSimple() throws Exception {
		super.testPetclinicSimple();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testPetclinic() throws Exception {
		super.testPetclinic();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = false)
	public void testHelloWorld() throws Exception {
		super.testHelloWorld();
	}

	
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}
}
