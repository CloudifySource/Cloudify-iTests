package test.cli.cloudify.cloud.ec2;

import java.io.IOException;
import java.util.HashMap;

import org.testng.ITestContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import test.cli.cloudify.CloudTestUtils;
import test.cli.cloudify.CommandTestUtils;
import test.cli.cloudify.cloud.NewAbstractCloudTest;
import test.cli.cloudify.cloud.services.ec2.Ec2CloudService;
import framework.utils.LogUtils;
import framework.utils.ScriptUtils;

public class PrivateImageEc2Test extends NewAbstractCloudTest {	

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1", enabled = true)
	public void installTest()
			throws Exception {
		doTest("petclinic", "petclinic");
	}

	protected void doTest(final String applicationFolderName, final String applicationName)
			throws IOException, InterruptedException {
		LogUtils.log("installing application " + applicationName);
		final String applicationPath = ScriptUtils.getBuildPath() + "/recipes/apps/" + applicationFolderName;
		try {
			installApplicationAndWait(applicationPath, applicationName);
		} finally {
			if (getService() != null && getService().getRestUrls() != null) {
				final String command = "connect " + getRestUrl() + ";list-applications";
				final String output = CommandTestUtils.runCommandAndWait(command);
				if (output.contains(applicationName)) {
					uninstallApplicationAndWait(applicationName);
				}
			}
		}
	}

	@Override
	protected String getCloudName() {
		return "ec2";
	}

	@Override
	protected boolean isReusableCloud() {
		return false;
	}
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap(final ITestContext testContext) {
		super.bootstrap(testContext);
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardown() {
		super.teardown();
	}
	
	@AfterMethod
	public void cleanUp() {
		//The test itself will make sure to remove 
		//any application before terminating
		super.scanAgentNodesLeak();
	}

	@Override
	protected void customizeCloud() {

		final Ec2CloudService ec2Service = (Ec2CloudService) cloud;

		ec2Service.setAdditionalPropsToReplace(new HashMap<String, String>());
		ec2Service.getAdditionalPropsToReplace().put("imageId \"us-east-1/ami-76f0061f\"",
				"imageId \"us-east-1/ami-93b068fa\"");
		ec2Service.getAdditionalPropsToReplace().put("keyFile \"ec2-sgtest.pem\"",
				"keyFile \"ec2-sgtest.pem\"\n\t\t\t\t\tusername \"ec2-user\"\n");
		ec2Service.setMachinePrefix(this.getClass().getName() + CloudTestUtils.SGTEST_MACHINE_PREFIX);

	};

}
