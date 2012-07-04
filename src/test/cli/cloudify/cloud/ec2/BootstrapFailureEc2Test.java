package test.cli.cloudify.cloud.ec2;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeState;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.cli.cloudify.CloudTestUtils;
import test.cli.cloudify.cloud.AbstractCloudTest;
import test.cli.cloudify.cloud.JcloudsUtils;
import test.cli.cloudify.cloud.services.ec2.Ec2CloudService;
import framework.tools.SGTestHelper;
import framework.utils.AssertUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;
import framework.utils.LogUtils;

/**
 * This test makes a bootstrap on ec2 fail by changing the JAVA_HOME path to a bad one in the bootstrap-management.sh file.
 * <p>After the bootstrap fails, the test checks if the management machine was shutdown.
 * 
 * @author nirb
 *
 */
public class BootstrapFailureEc2Test extends AbstractCloudTest{

	private static final String CLOUD_SERVICE_UNIQUE_NAME = "BootstrapFailureEc2Test";
	private Ec2CloudService service;
	private NodeMetadata managementMachine;
	private static final long TIME_TO_TERMINATE_IN_MILLS = 60000;
	private String machineName = this.getClass().getName() + "_" + CloudTestUtils.SGTEST_MACHINE_PREFIX + System.currentTimeMillis() + "_";
	private boolean managementMachineTerminated = false;

	@BeforeMethod
	public void init() throws IOException, InterruptedException {	
		
		setCloudService("EC2", CLOUD_SERVICE_UNIQUE_NAME, false);
		service = (Ec2CloudService)getService();
		service.setMachinePrefix(machineName);	
	}
	
	
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void installTest() throws IOException, InterruptedException {
		
		//replace the bootstrap-management with a bad version, to fail the bootstrap.
		File standardBootstrapManagement = new File(service.getPathToCloudFolder() + "/upload", "bootstrap-management.sh");
		File badBootstrapManagement = new File(SGTestHelper.getSGTestRootDir() + "/apps/cloudify/cloud/ec2/bad-bootstrap-management.sh");
		Map<File, File> filesToReplace = new HashMap<File, File>();
		filesToReplace.put(standardBootstrapManagement, badBootstrapManagement);
		service.addFilesToReplace(filesToReplace);
		
		try {
			service.bootstrapCloud();
		} catch (AssertionError ae) {
			System.out.println(ae.getMessage());
		}
	
		JcloudsUtils.createContext(service);
		Set<? extends NodeMetadata> machines = JcloudsUtils.getServersByName(machineName);
		Assert.assertTrue(machines != null);
		managementMachine = machines.iterator().next();
				
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				Set<? extends NodeMetadata> machines = JcloudsUtils.getServersByName(machineName);
				managementMachine = machines.iterator().next();
				if (managementMachine.getState() == NodeState.TERMINATED) {
					managementMachineTerminated = true;
				}
				return managementMachineTerminated;
			}
		};
		
		AssertUtils.repetitiveAssertTrue("management machine was not terminated", condition, TIME_TO_TERMINATE_IN_MILLS);
	}

	@AfterMethod(alwaysRun = true)
	public void teardown() {
		JcloudsUtils.closeContext();
		
		try {
			if (!managementMachineTerminated) {
				service.teardownCloud();				
			}
		}
		catch (Throwable e) {
			LogUtils.log("caught an exception while tearing down ec2", e);
			sendTeardownCloudFailedMail("ec2", e);
		}
	}
	
}