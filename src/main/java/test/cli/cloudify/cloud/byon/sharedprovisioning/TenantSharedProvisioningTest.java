package test.cli.cloudify.cloud.byon.sharedprovisioning;

import java.io.IOException;
import java.util.Set;

import org.cloudifysource.dsl.utils.ServiceUtils;
import org.openspaces.admin.machine.Machine;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import framework.utils.AssertUtils;
import framework.utils.ProcessingUnitUtils;

public class TenantSharedProvisioningTest extends AbstractSharedProvisioningByonCloudTest {
		
	private static final String APPLICATION_ONE = "application1";
	private static final String APPLICATION_TWO = "application2";
	
	private static final String SERVICE_ONE = "service1";
	private static final String SERVICE_TWO = "service2";

	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testTwoTenant() throws IOException, InterruptedException {

		super.installManualTenantSharedProvisioningApplicationAndWait("ROLE_CLOUDADMINS", APPLICATION_ONE);
		super.installManualTenantSharedProvisioningApplicationAndWait("ROLE_APPMANAGERS", APPLICATION_TWO);
		
		Set<Machine> applicationOneMachines = ProcessingUnitUtils.getMachinesOfApplication(admin, APPLICATION_ONE);
		Set<Machine> applicationTwoMachines = ProcessingUnitUtils.getMachinesOfApplication(admin, APPLICATION_TWO);
		
		// make sure they dont overlap
		AssertUtils.assertTrue("applications have ovelapping machines even though the isolation is app based", 
				!applicationOneMachines.removeAll(applicationTwoMachines));
		
		super.uninstallApplicationAndWait(APPLICATION_ONE);
		super.uninstallApplicationAndWait(APPLICATION_TWO);
		
	}
	
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testTwoServicesOnOneTenant() throws IOException, InterruptedException {
		super.installManualTenantSharedProvisioningServiceAndWait("ROLE_CLOUDADMINS", SERVICE_ONE);
		super.installManualTenantSharedProvisioningServiceAndWait("ROLE_CLOUDADMINS", SERVICE_TWO);
		
		Set<Machine> serviceOneMachines = ProcessingUnitUtils.getMachinesOfService(admin, ServiceUtils.getAbsolutePUName(APPLICATION_ONE, SERVICE_ONE));
		Set<Machine> serviceTwoMachines = ProcessingUnitUtils.getMachinesOfService(admin, ServiceUtils.getAbsolutePUName(APPLICATION_ONE, SERVICE_TWO));
		
		AssertUtils.assertTrue("services should be deployed on the same machine since they belong to the same tenant", 
				serviceOneMachines.equals(serviceTwoMachines));
		
		super.uninstallServiceAndWait(SERVICE_ONE);
		super.uninstallServiceAndWait(SERVICE_TWO);
		
		
	}
	
	@AfterMethod
	public void cleanup() throws IOException, InterruptedException {
		super.uninstallApplicationIfFound(APPLICATION_ONE);
		super.uninstallApplicationIfFound(APPLICATION_TWO);
		super.uninstallApplicationIfFound(SERVICE_ONE);
		super.uninstallApplicationIfFound(SERVICE_TWO);
	}
	
	
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}

}
