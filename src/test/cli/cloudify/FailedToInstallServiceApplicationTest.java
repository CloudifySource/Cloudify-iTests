package test.cli.cloudify;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class FailedToInstallServiceApplicationTest extends AbstractCommandTest {

	
	
	//public static final String USM_SERVICE_FOLDER_NAME = "simpleService";
	public static final String USM_SERVICE_FOLDER_NAME = "simpleService";
	public static final String USM_SERVICE_NAME = "simple";
	
	//public static final String USM_APPLICATION_FOLDER_NAME = "simpleApplication";
	public static final String USM_APPLICATION_FOLDER_NAME = "simpleApplication";
	
	@Override
	@BeforeMethod
	public void beforeTest() {
		super.beforeTest();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testBadInstallService() throws IOException, InterruptedException {
		testBadServiceInstall(getUsmBadServicePath(USM_SERVICE_FOLDER_NAME), USM_SERVICE_NAME);
	}
	
	private void testBadServiceInstall(String servicePath, String serviceName) throws IOException, InterruptedException {
		String output = runCommand("connect " + this.restUrl +
				";install-service --verbose " + servicePath + 
				";disconnect;");
	
		ProcessingUnit processingUnit = admin.getProcessingUnits().waitFor(serviceName, Constants.PROCESSINGUNIT_TIMEOUT_SEC, TimeUnit.SECONDS);
		assertTrue("Deployed Successfully. Test Failed", 
    		processingUnit == null || processingUnit.waitFor(0, Constants.PROCESSINGUNIT_TIMEOUT_SEC, TimeUnit.SECONDS));
		assertTrue(output.contains("Installation of service \"simple\" failed"));
		
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testBadInstallApplication() throws IOException, InterruptedException {
		testBadApplicationInstall(getUsmBadServicePath(USM_APPLICATION_FOLDER_NAME), USM_SERVICE_NAME);
	}
	
	private void testBadApplicationInstall(String usmBadServicePath,
			String usmServiceName) throws IOException, InterruptedException {
		String output = runCommand("connect " + this.restUrl +
				";install-application --verbose " + usmBadServicePath + 
				";disconnect;");
	
		ProcessingUnit processingUnit = admin.getProcessingUnits().waitFor(usmServiceName, Constants.PROCESSINGUNIT_TIMEOUT_SEC, TimeUnit.SECONDS);
		assertTrue("Deployed Successfully. Test Failed", 
    		processingUnit == null || processingUnit.waitFor(0, Constants.PROCESSINGUNIT_TIMEOUT_SEC, TimeUnit.SECONDS));
		assertTrue(output.contains("Application simple could not be installed"));
		
		
	}

	private String getUsmBadServicePath(String dirOrFilename) {
		return CommandTestUtils.getPath("apps/USM/badUsmServices/" + dirOrFilename);
	}
}
