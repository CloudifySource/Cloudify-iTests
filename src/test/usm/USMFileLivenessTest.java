package test.usm;

import static test.utils.AdminUtils.loadGSC;
import static test.utils.AdminUtils.loadGSM;

import java.util.concurrent.TimeUnit;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class USMFileLivenessTest extends UsmAbstractTest {


	@BeforeMethod
	@Override
	public void beforeTest() {
		super.beforeTest();
		loadGSM(admin.getGridServiceAgents().waitForAtLeastOne(10, TimeUnit.SECONDS));
		loadGSC(admin.getGridServiceAgents().waitForAtLeastOne(10, TimeUnit.SECONDS));
		this.processName = "SimpleFilewriteAndPortOpener-service";
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
	public void legitUSMConfigurationTest() throws Exception{
		//The filewritter process will output Hello_World to the file and
		//that string will act as the regx
		this.serviceFileName = "fileAndRegexFound-service.groovy";
		assertOnePUForProcess();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void wrongFilenameTest() throws Exception{

		this.serviceFileName = "fileNotFound-service.groovy";
		assertZeroPUsForProcess();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void wrongRegxTest() throws Exception{

		this.serviceFileName = "regexNotFound-service.groovy";
		assertZeroPUsForProcess();
	}
	

}
