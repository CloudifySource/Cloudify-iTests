package test.webui.console;

import static framework.utils.AdminUtils.loadGSCs;
import static framework.utils.AdminUtils.loadGSM;
import static framework.utils.LogUtils.log;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import framework.utils.AssertUtils;
import framework.utils.ProcessingUnitUtils;
import framework.utils.AssertUtils.RepetitiveConditionProvider;

import test.webui.AbstractSeleniumTest;
import test.webui.objects.LoginPage;
import test.webui.objects.console.ConsoleTab;
import test.webui.objects.console.SpaceTreeSidePanel;
import test.webui.objects.console.SpaceTreeSidePanel.SpaceTreeNode;

public class SpaceTreeNodeFilterTest extends AbstractSeleniumTest {
	
	Machine machineA;
	ProcessingUnit pu;
	GridServiceManager gsmA;
	
	@BeforeMethod(alwaysRun = true)
	public void startSetup() {
		log("waiting for 1 machine");
		admin.getMachines().waitFor(1);

		log("waiting for 1 GSA");
		admin.getGridServiceAgents().waitFor(1);

		GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
		GridServiceAgent gsaA = agents[0];

		machineA = gsaA.getMachine();

		log("starting: 1 GSM and 2 GSC's on 1 machine");
		gsmA = loadGSM(machineA); 
		loadGSCs(machineA, 2);
		
		// deploy a pu
		SpaceDeployment deployment = new SpaceDeployment("Test1").partitioned(1, 0).maxInstancesPerVM(1);
		pu = gsmA.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.INTACT);
		
		// deploy a pu
		deployment = new SpaceDeployment("Test2").partitioned(1, 0).maxInstancesPerVM(1);
		pu = gsmA.deploy(deployment);
		ProcessingUnitUtils.waitForDeploymentStatus(pu, DeploymentStatus.INTACT);
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = {"cloudify" , "xap"})
	public void filterTest() throws InterruptedException {
		
		LoginPage loginPage = getLoginPage();
		
		ConsoleTab consoleTab = loginPage.login().switchToConsole();
		
		final SpaceTreeSidePanel sidePanel = consoleTab.getSpaceTreeSidePanel();
		
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				SpaceTreeNode test1 = sidePanel.getSpaceTreeNode("Test1");
				SpaceTreeNode test2 = sidePanel.getSpaceTreeNode("Test2");
				return ((test1 != null) && (test2 != null));
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, 5000);
		
		sidePanel.filterSpaces("Test1");
		
		condition = new RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				SpaceTreeNode test2 = sidePanel.getSpaceTreeNode("Test2");
				return (test2 == null);
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, 5000);
		
	}

}
