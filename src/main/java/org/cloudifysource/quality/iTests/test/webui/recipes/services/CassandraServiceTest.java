package org.cloudifysource.quality.iTests.test.webui.recipes.services;

import iTests.framework.utils.AssertUtils;
import iTests.framework.utils.AssertUtils.RepetitiveConditionProvider;

import java.io.IOException;

import org.openspaces.admin.pu.DeploymentStatus;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.gigaspaces.webuitf.LoginPage;
import com.gigaspaces.webuitf.MainNavigation;
import com.gigaspaces.webuitf.dashboard.DashboardTab;
import com.gigaspaces.webuitf.dashboard.ServicesGrid;
import com.gigaspaces.webuitf.dashboard.ServicesGrid.ApplicationServicesGrid;
import com.gigaspaces.webuitf.dashboard.ServicesGrid.ApplicationsMenuPanel;
import com.gigaspaces.webuitf.dashboard.ServicesGrid.Icon;
import com.gigaspaces.webuitf.dashboard.ServicesGrid.InfrastructureServicesGrid;
import com.gigaspaces.webuitf.topology.TopologyTab;
import com.gigaspaces.webuitf.topology.applicationmap.ApplicationMap;
import com.gigaspaces.webuitf.topology.applicationmap.ApplicationNode;
import com.gigaspaces.webuitf.topology.healthpanel.HealthPanel;

public class CassandraServiceTest extends AbstractSeleniumServiceRecipeTest  {
		
	private static final String COMMIT_LOG_ACTIVE_TASKS = "gs-metric-title-CUSTOM_Commit_Log_Active_Tasks";
	private static final String COMPACTION_MANAGER_PENDING_TASKS = "gs-metric-title-CUSTOM_Compaction_Manager_Pending_Tasks";
	private static final String COMPACTION_MANAGER_COMPLETED_TASKS = "gs-metric-title-CUSTOM_Compaction_Manager_Completed_Tasks";
	private static final String TOTAL_PROCESS_VIRTUAL_MEMORY = "gs-metric-title-CUSTOM_Total_Process_Virtual_Memory";
	private static final String PROCESS_CPU_USAGE = "gs-metric-title-CUSTOM_Process_Cpu_Usage";
	
	@Override
	@BeforeMethod
	public void install() throws IOException, InterruptedException {
		setCurrentRecipe("cassandra");
		super.install();
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT)
	public void cassandraRecipeTest() throws InterruptedException, IOException {

		// get new login page
		LoginPage loginPage = getLoginPage();

		MainNavigation mainNav = loginPage.login();

		DashboardTab dashboardTab = mainNav.switchToDashboard();
		
		final InfrastructureServicesGrid infrastructureServicesGrid = dashboardTab.getServicesGrid().getInfrastructureGrid();
		
		RepetitiveConditionProvider condition = new RepetitiveConditionProvider() {
			
			@Override
			public boolean getCondition() {
				return ((infrastructureServicesGrid.getESMInst().getCount() == 1) 
						&& (infrastructureServicesGrid.getESMInst().getIcon().equals(Icon.OK)));
			}
		};
		AssertUtils.repetitiveAssertTrue("No esm in showing in the dashboard", condition, waitingTime);

		ServicesGrid servicesGrid = dashboardTab.getServicesGrid();

		ApplicationsMenuPanel appMenu = servicesGrid.getApplicationsMenuPanel();

		appMenu.selectApplication(MANAGEMENT_APPLICATION_NAME);

		final ApplicationServicesGrid applicationServicesGrid = servicesGrid.getApplicationServicesGrid();

		condition = new RepetitiveConditionProvider() {		
			@Override
			public boolean getCondition() {
				return applicationServicesGrid.getWebModule().getCount() == 2;
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);

		appMenu.selectApplication(DEFAULT_APPLICATION_NAME);

		condition = new RepetitiveConditionProvider() {		
			@Override
			public boolean getCondition() {
				return applicationServicesGrid.getNoSqlDbModule().getCount() == 1;
			}
		};
		AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);

		TopologyTab topologyTab = mainNav.switchToTopology();

		final ApplicationMap appMap = topologyTab.getApplicationMap();

		topologyTab.selectApplication(MANAGEMENT_APPLICATION_NAME);
		

		ApplicationNode restful = appMap.getApplicationNode("rest");

		assertTrue(restful != null);
		assertTrue(restful.getStatus().equals(DeploymentStatus.INTACT));

		ApplicationNode webui = appMap.getApplicationNode("webui");

		assertTrue(webui != null);
		assertTrue(webui.getStatus().equals(DeploymentStatus.INTACT));

		topologyTab.selectApplication(DEFAULT_APPLICATION_NAME);

		takeScreenShot(this.getClass(),"cassandraRecipeTest", "topology");

		final ApplicationNode simple = appMap.getApplicationNode(DEFAULT_CASSANDRA_SERVICE_NAME);

		assertTrue(simple != null);
		condition = new RepetitiveConditionProvider() {
			
			@Override
			public boolean getCondition() {
				return simple.getStatus().equals(DeploymentStatus.INTACT);
			}
		};
		repetitiveAssertTrueWithScreenshot(
				"cassandra service is displayed as " + simple.getStatus() + 
					"even though it is installed", condition, this.getClass(), "cassandraRecipeTest", "cassandra-service");

		HealthPanel healthPanel = topologyTab.getTopologySubPanel().switchToHealthPanel();

		takeScreenShot(this.getClass(),"cassandraRecipeTest", "topology-healthpanel");
		
		assertTrue("Process Cpu Usage " + METRICS_ASSERTION_SUFFIX, healthPanel.getMetric(PROCESS_CPU_USAGE) != null);
		assertTrue("Total Process Virtual Memory " + METRICS_ASSERTION_SUFFIX, healthPanel.getMetric(TOTAL_PROCESS_VIRTUAL_MEMORY) != null);
		assertTrue("Compaction Manager Completed Tasks " + METRICS_ASSERTION_SUFFIX , healthPanel.getMetric(COMPACTION_MANAGER_COMPLETED_TASKS) != null);
		assertTrue("Compaction Manager Pending Tasks " + METRICS_ASSERTION_SUFFIX, healthPanel.getMetric(COMPACTION_MANAGER_PENDING_TASKS) != null);
		assertTrue("Commit Log Active Tasks" + METRICS_ASSERTION_SUFFIX , healthPanel.getMetric(COMMIT_LOG_ACTIVE_TASKS) != null);

		mainNav.switchToServices();

		uninstallService("cassandra", true);
	}
}