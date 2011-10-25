package test.webui.topology.applicationmap;

import static test.utils.AdminUtils.loadGSCs;
import static test.utils.AdminUtils.loadGSM;
import static test.utils.LogUtils.log;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.utils.AssertUtils;
import test.utils.AssertUtils.RepetitiveConditionProvider;
import test.utils.LogUtils;
import test.utils.ProcessingUnitUtils;
import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.services.PuTreeGrid;
import test.webui.objects.services.PuTreeGrid.WebUIProcessingUnit;
import test.webui.objects.services.ServicesTab;
import test.webui.objects.topology.ApplicationMap;
import test.webui.objects.topology.ApplicationMap.ApplicationNode;
import test.webui.objects.topology.TopologyTab;

public class PuStatusChangedTest extends AbstractSeleniumTest {
	
	Machine machineA;
	ProcessingUnit pu;
	
    @BeforeMethod(alwaysRun = true)
    public void setup() {
        //1 GSM and 1 GSC at 1 machines
        log("waiting for 1 machine");
        admin.getMachines().waitFor(1);

        log("waiting for 1 GSA");
        admin.getGridServiceAgents().waitFor(1);

        GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
        GridServiceAgent gsaA = agents[0];

        machineA = gsaA.getMachine();

        log("starting: 1 GSM and 1 GSC at 1 machines");
        GridServiceManager gsmA = loadGSM(machineA); 
        loadGSCs(machineA, 2);
        
		LogUtils.log("deploying processing unit...");
		SpaceDeployment deployment = new SpaceDeployment("Test").partitioned(2, 1).maxInstancesPerVM(1).setContextProperty("com.gs.application", "App");
		pu = gsmA.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.INTACT);
    }

	 @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = {"cloudify" , "xap"})
    public void statusChanged() throws InterruptedException {
    	
		// get new login page
		LoginPage loginPage = getLoginPage();
		
		// get new topology tab
		TopologyTab topologyTab = loginPage.login().switchToTopology();
		
		final ApplicationMap applicationMap = topologyTab.getApplicationMap();
		
		applicationMap.selectApplication("App");
		
		ApplicationNode testNode = applicationMap.getApplicationNode("Test");
		
		assertTrue(testNode.getStatus().equals(DeploymentStatus.INTACT));
		
		ServicesTab servicesTab = topologyTab.switchToServices();
		
		final PuTreeGrid puTreeGrid = servicesTab.getPuTreeGrid();
		
		admin.getGridServiceContainers().getContainers()[0].kill();
		admin.getGridServiceContainers().waitFor(1);
		pu.waitFor(2);
		ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.COMPROMISED);
		
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
			
			public boolean getCondition() {
				WebUIProcessingUnit testPu = null;
				testPu = puTreeGrid.getProcessingUnit("Test");
				return ((testPu != null) && (testPu.getStatus().equals(DeploymentStatus.COMPROMISED)));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition,20000);
		
		topologyTab = servicesTab.switchToTopology();
	
		ApplicationNode testNode2 = applicationMap.getApplicationNode("Test");
		assertTrue(testNode2.getStatus().equals(DeploymentStatus.COMPROMISED));
		assertTrue(applicationMap.getApplicationNode("Test").isDisplayed());
    	
    }
}
