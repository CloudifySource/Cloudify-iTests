package test.cli.cloudify.xen;

import org.openspaces.admin.esm.ElasticServiceManager;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.cli.cloudify.CommandTestUtils;

import org.cloudifysource.dsl.utils.ServiceUtils;

import framework.tools.SGTestHelper;
import framework.utils.LogUtils;

public class StockdemoAppFailOverUsingXenTest extends AbstractApplicationFailOverXenTest {

	private final String stockdemoAppDirPath = SGTestHelper.getSGTestRootDir().replace("\\", "/") + "/apps/USM/usm/applications/stockdemo";
	private String cassandraHostIp;
	
	@BeforeClass
	public void beforeClass()  {
		super.beforeTest();
		assignCassandraPorts(stockdemoAppDirPath);
																			   
		startAgent(0 ,"stockAnalytics" ,"stockAnalyticsMirror" ,"StockDemo");
	    startAgent(0 ,"stockAnalyticsProcessor" ,"stockAnalyticsSpace","stockAnalyticsFeeder");
	    startAgent(0 ,"stockAnalyticsProcessor" ,"stockAnalyticsSpace" ,"cassandra");

	    repetitiveAssertNumberOfGSAsAdded(4, OPERATION_TIMEOUT);
	    repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);
	    
	    cassandraHostIp = admin.getZones().getByName("cassandra").getGridServiceAgents().getAgents()[0].getMachine().getHostAddress();
	}

	@Override
	@BeforeMethod
	public void beforeTest() {
		
	}
	
	@Override
	@AfterMethod
	public void afterTest() {
		try {
			CommandTestUtils.runCommandAndWait("connect " + restUrl + " ;uninstall-application stockdemo");
		} catch (Exception e) {	}
		
		assertAppUninstalled("stockdemo");
	}
	
	@AfterClass
	public void AfterClass(){		
		super.afterTest();
	}
	
/////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1", enabled = true)
	public void testStockdemoApplication() throws Exception {	    
		installApp(cassandraPort1 ,cassandraHostIp, cassandraPort2 ,cassandraHostIp , stockdemoAppDirPath);
		assertStockdemoAppInstalled(cassandraPort1 ,cassandraHostIp, cassandraPort2 ,cassandraHostIp);
	}
	
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testStockdemoAppCassandraPuInstFailOver() throws Exception{
		installApp(cassandraPort1 ,cassandraHostIp, cassandraPort2 ,cassandraHostIp , stockdemoAppDirPath);
		assertStockdemoAppInstalled(cassandraPort1 ,cassandraHostIp, cassandraPort2 ,cassandraHostIp);
		
		String cassandraAbsolutePUName = ServiceUtils.getAbsolutePUName("stockdemo", "cassandra");
		ProcessingUnit cassandra = admin.getProcessingUnits().getProcessingUnit(cassandraAbsolutePUName);
		int cassandraPuInstancesAfterInstall = cassandra.getInstances().length;
		LogUtils.log("destroying the pu instance holding cassandra");
		cassandra.getInstances()[0].destroy();
		assertPuInstanceKilled("cassandra" , cassandraPort1 , cassandraHostIp , cassandraPuInstancesAfterInstall);
		assertPuInstanceRessurected("cassandra" , cassandraPort1 , cassandraHostIp , cassandraPuInstancesAfterInstall);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testStockdemoAppCassandraGSCFailOver() throws Exception{
		installApp(cassandraPort1 ,cassandraHostIp, cassandraPort2 ,cassandraHostIp , stockdemoAppDirPath);
		assertStockdemoAppInstalled(cassandraPort1 ,cassandraHostIp, cassandraPort2 ,cassandraHostIp);
		
		String cassandraAbsolutePUName = ServiceUtils.getAbsolutePUName("stockdemo", "cassandra");
		ProcessingUnit cassandra = admin.getProcessingUnits().getProcessingUnit(cassandraAbsolutePUName);
		int cassandraPuInstancesAfterInstall = cassandra.getInstances().length;
		LogUtils.log("restarting GSC containing cassandra");
		cassandra.getInstances()[0].getGridServiceContainer().kill();
		assertPuInstanceKilled("cassandra" , cassandraPort1 ,cassandraHostIp , cassandraPuInstancesAfterInstall);
		assertPuInstanceRessurected("cassandra" , cassandraPort1 ,cassandraHostIp , cassandraPuInstancesAfterInstall);
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testStockdemoAppStockAnalyticsSpacePuInstFailOver() throws Exception{
		installApp(cassandraPort1 ,cassandraHostIp, cassandraPort2 ,cassandraHostIp , stockdemoAppDirPath);
		assertStockdemoAppInstalled(cassandraPort1 ,cassandraHostIp, cassandraPort2 ,cassandraHostIp);
		
		String stockAnalyticsSpaceAbsolutePUName = ServiceUtils.getAbsolutePUName("stockdemo", "stockAnalyticsSpace");
		ProcessingUnit stockAnalyticsSpace = admin.getProcessingUnits().getProcessingUnit(stockAnalyticsSpaceAbsolutePUName);
		int spacePuInstancesAfterInstall = stockAnalyticsSpace.getInstances().length;
		LogUtils.log("destroying the pu instance holding stockAnalyticsSpace");
		stockAnalyticsSpace.getInstances()[0].destroy();
		assertPuInstanceKilled("stockAnalyticsSpace" , spacePuInstancesAfterInstall);
		assertPuInstanceRessurected("stockAnalyticsSpace" , spacePuInstancesAfterInstall);
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testStockdemoAppStockAnalyticsSpaceGSCFailOver() throws Exception{
		installApp(cassandraPort1 ,cassandraHostIp, cassandraPort2 ,cassandraHostIp , stockdemoAppDirPath);
		assertStockdemoAppInstalled(cassandraPort1 ,cassandraHostIp, cassandraPort2 ,cassandraHostIp);
		
		String stockAnalyticsSpaceAbsolutePUName = ServiceUtils.getAbsolutePUName("stockdemo", "stockAnalyticsSpace");
		ProcessingUnit stockAnalyticsSpace = admin.getProcessingUnits().getProcessingUnit(stockAnalyticsSpaceAbsolutePUName);
		int spacePuInstancesAfterInstall = stockAnalyticsSpace.getInstances().length;
		LogUtils.log("restarting GSC containing stockAnalyticsFeeder");
		stockAnalyticsSpace.getInstances()[0].getGridServiceContainer().kill();
		assertPuInstanceKilled("stockAnalyticsSpace" , spacePuInstancesAfterInstall);
		assertPuInstanceRessurected("stockAnalyticsSpace" , spacePuInstancesAfterInstall);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT , groups="1", enabled = true)
	public void testEsmRestart() throws Exception {
		installApp(cassandraPort1 ,cassandraHostIp, cassandraPort2 ,cassandraHostIp , stockdemoAppDirPath);
		assertStockdemoAppInstalled(cassandraPort1 ,cassandraHostIp, cassandraPort2 ,cassandraHostIp);
		
		ElasticServiceManager esm = admin.getElasticServiceManagers().waitForAtLeastOne();
		LogUtils.log("killing esm");
		killEsmAndWait(esm);
		
		LogUtils.log("asserting esm is managing enviroment");
		assertEsmIsManagingEnvBySearchingLogs(esm);
	}
}
