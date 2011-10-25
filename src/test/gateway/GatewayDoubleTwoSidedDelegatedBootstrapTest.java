package test.gateway;

import static test.utils.LogUtils.log;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsa.GridServiceContainerOptions;
import org.openspaces.admin.gsa.GridServiceManagerOptions;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.Space;
import org.openspaces.core.GigaSpace;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import test.data.Stock;
import test.utils.AssertUtils;
import test.utils.SetupUtils;
import test.utils.TeardownUtils;
import test.utils.TestUtils;

/**
 * @author sagib
 * @since 8.0.3
 */
public class GatewayDoubleTwoSidedDelegatedBootstrapTest extends AbstractGatewayTest {

    private Admin admin1;
    private Admin admin2;
    private Admin admin3;
    private GigaSpace gigaSpace;
    private GigaSpace gigaSpace1;
    private GigaSpace gigaSpace2;

    private String group1 = null;
    private String group2 = null;
    private String group3 = null;

    private GridServiceManager gsmNY;
    private GridServiceManager gsmISR;
    private GridServiceManager gsmLON;

    public GatewayDoubleTwoSidedDelegatedBootstrapTest() {
        if (isDevMode()) {
            group1 = "israel-" + getUserName();
            group2 = "london-" + getUserName();
            group3 = "ny-" + getUserName();

        } else {
            group1 = GROUP1;
            group2 = GROUP2;
            group3 = GROUP3;
        }

    }

    @AfterMethod
    public void tearDown() {
        TeardownUtils.teardownAll(admin1, admin2, admin3);
        admin1.close();
        admin2.close();
        admin3.close();
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups="3", enabled=true)
    public void test() throws Exception {
        initialize();

        final Stock[] stocks = new Stock[ENTRY_SIZE];
        for (int i = 0; i < stocks.length; i++) {
            stocks[i] = new Stock(i);
        }
        gigaSpace.writeMultiple(stocks);
        Thread.sleep(5000);

        Assert.assertEquals(0, gigaSpace1.count(null));

        ProcessingUnit sink = admin1.getProcessingUnits().waitFor("ISRAEL-SINK");
        sink.waitFor(1);
        
        Thread.sleep(10000);
        enableIncomingReplication(admin1, "ISRAEL", "NY");
        
        // Verify delegators & sinks are connected.
        Space space = admin3.getSpaces().waitFor("NYSpace");
        Space space1 = admin1.getSpaces().waitFor("israelSpace");
        Space space2 = admin2.getSpaces().waitFor("londonSpace");

        assertGatewayReplicationConnected(space, 1);
        assertGatewayReplicationConnected(space1, 1);
        assertGatewayReplicationConnected(space2, 0);

        TestUtils.repetitive(new Runnable() {
            public void run() {
                AssertUtils.assertEquivalenceArrays("", gigaSpace.readMultiple(new Stock()), gigaSpace1.readMultiple(new Stock()));
                Assert.assertEquals(ENTRY_SIZE, gigaSpace.count(new Stock()));
            }
        }, OPERATION_TIMEOUT);

        ProcessingUnit spacePu = admin1.getProcessingUnits().waitFor("ISRAEL");
        spacePu.waitFor(4);
        spacePu.undeploy();

        sink.getInstances()[0].restartAndWait();
        deployIsraelSite();

        Thread.sleep(20000);

        bootstrap(admin1, "ISRAEL", "NY");
        // Verify delegators & sinks are connected.
        space = admin3.getSpaces().waitFor("NYSpace");
        space1 = admin1.getSpaces().waitFor("israelSpace");
        space2 = admin2.getSpaces().waitFor("londonSpace");

        assertGatewayReplicationConnected(space, 1);
        assertGatewayReplicationConnected(space1, 1);
        assertGatewayReplicationConnected(space2, 0);

        TestUtils.repetitive(new Runnable() {

            public void run() {
                AssertUtils.assertEquivalenceArrays("", gigaSpace.readMultiple(new Stock()), gigaSpace1.readMultiple(new Stock()));
            }

        }, (int)DEFAULT_TEST_TIMEOUT);

    }

    public void setUpTestCase() {
        gigaSpace.clear(null);
        gigaSpace1.clear(null);
        gigaSpace2.clear(null);
        assertGatewayReplicationHasNotingToReplicate(admin3, admin1, admin2);
    }

    private void initialize() throws Exception {
        log("initializing..");
        admin1 = new AdminFactory().addGroups(group1).createAdmin();
        admin2 = new AdminFactory().addGroups(group2).createAdmin();
        admin3 = new AdminFactory().addGroups(group3).createAdmin();

        SetupUtils.assertCleanSetup(admin1);
        SetupUtils.assertCleanSetup(admin2);
        SetupUtils.assertCleanSetup(admin3);

        GridServiceAgent gsa1 = admin1.getGridServiceAgents().waitForAtLeastOne();
        GridServiceAgent gsa2 = admin2.getGridServiceAgents().waitForAtLeastOne();
        GridServiceAgent gsa3 = admin3.getGridServiceAgents().waitForAtLeastOne();

        gsa3.startGridService(new GridServiceManagerOptions());
        gsa3.startGridService(new GridServiceContainerOptions());
        gsa3.startGridService(new GridServiceContainerOptions());

        gsa1.startGridService(new GridServiceManagerOptions());
        gsa1.startGridService(new GridServiceContainerOptions());
        gsa1.startGridService(new GridServiceContainerOptions());

        gsa2.startGridService(new GridServiceManagerOptions());
        gsa2.startGridService(new GridServiceContainerOptions());
        gsa2.startGridService(new GridServiceContainerOptions());

        gsmISR = admin1.getGridServiceManagers().waitForAtLeastOne();
        gsmLON = admin2.getGridServiceManagers().waitForAtLeastOne();
        gsmNY = admin3.getGridServiceManagers().waitForAtLeastOne();

        log("deploying PUs");
        deployIsraelSite();
        deployNYSite();
        deployLondonSite();
        deployIsraelSink();
        deployIsraelRouting();
        deployNYSink();
        deployNYRouting();
        deployLondonGW();

        admin3.getGridServiceContainers().waitFor(3);
        admin1.getGridServiceContainers().waitFor(3);
        admin2.getGridServiceContainers().waitFor(3);

        Space space = admin3.getSpaces().waitFor("NYSpace");
        Space space1 = admin1.getSpaces().waitFor("israelSpace");
        Space space2 = admin2.getSpaces().waitFor("londonSpace");

        gigaSpace = space.getGigaSpace();
        gigaSpace1 = space1.getGigaSpace();
        gigaSpace2 = space2.getGigaSpace();

        log("validating gateway components");

        // Verify delegators & sinks are connected.
        assertGatewayReplicationDisconnected(space, 1);  //requires-bootstrap="true"
        assertGatewayReplicationConnected(space1, 1);
        assertGatewayReplicationConnected(space2, 0);

        log("finished initialziation");

    }

    private void deployLondonGW() {
        Map<String, String> props = new HashMap<String, String>();
        props.put("localGatewayName", "LONDON");
        props.put("targetGatewayName", "NY");
        props.put("target2GatewayName", "ISRAEL");
        props.put("localClusterUrl", "jini://*/*/londonSpace");
        props.put("gatewayLookupGroup", UUID.randomUUID().toString());
        props.put("localGatewayHost", gsmLON.getVirtualMachine().getMachine().getHostName());
        props.put("localGatewayDiscoveryPort", LONDON_GATEWAY_DISCOVERY_PORT);
        props.put("localGatewayLrmiPort", LONDON_GATEWAY_LRMI_PORT);
        props.put("targetGatewayHost", gsmNY.getVirtualMachine().getMachine().getHostName());
        props.put("targetGatewayDiscoveryPort", NY_GATEWAY_DISCOVERY_PORT);
        props.put("targetGatewayLrmiPort", NY_GATEWAY_LRMI_PORT);
        props.put("target2GatewayHost", gsmISR.getVirtualMachine().getMachine().getHostName());
        props.put("target2GatewayDiscoveryPort", ISRAEL_GATEWAY_DISCOVERY_PORT);
        props.put("target2GatewayLrmiPort", ISRAEL_GATEWAY_LRMI_PORT);
        deployGateway(gsmLON, siteDeployment("./apps/gateway/gatewayDelegatorWith2TargetsOnly", "LONDON-GW", props));
    }

    private void deployNYRouting() {
        Map<String, String> props = new HashMap<String, String>();
        props.put("localGatewayName", "NY");
        props.put("targetGatewayName", "ISRAEL");
        props.put("targetDelegateThrough", "LONDON");
        props.put("localClusterUrl", "jini://*/*/NYSpace");
        props.put("gatewayLookupGroup", UUID.randomUUID().toString());
        props.put("localGatewayHost", gsmNY.getVirtualMachine().getMachine().getHostName());
        props.put("localGatewayDiscoveryPort", NY_GATEWAY_DISCOVERY_PORT);
        props.put("localGatewayLrmiPort", NY_GATEWAY_LRMI_PORT);
        props.put("targetGatewayHost", gsmLON.getVirtualMachine().getMachine().getHostName());
        props.put("targetGatewayDiscoveryPort", LONDON_GATEWAY_DISCOVERY_PORT);
        props.put("targetGatewayLrmiPort", LONDON_GATEWAY_LRMI_PORT);
        deployGateway(gsmNY, siteDeployment("./apps/gateway/gatewayRoutingComponents", "NY-ROUTING", props));
    }

    private void deployNYSink() {
        Map<String, String> props = new HashMap<String, String>();
        props.put("localGatewayName", "NY");
        props.put("targetGatewayName", "ISRAEL");
        props.put("target2GatewayName", "LONDON");
        props.put("localClusterUrl", "jini://*/*/NYSpace");
        props.put("gatewayLookupGroup", UUID.randomUUID().toString());
        props.put("localGatewayHost", gsmNY.getVirtualMachine().getMachine().getHostName());
        props.put("localGatewayDiscoveryPort", NY_GATEWAY_DISCOVERY_PORT);
        props.put("localGatewayLrmiPort", NY_GATEWAY_LRMI_PORT);
        props.put("targetGatewayHost", gsmISR.getVirtualMachine().getMachine().getHostName());
        props.put("targetGatewayDiscoveryPort", ISRAEL_GATEWAY_DISCOVERY_PORT);
        props.put("targetGatewayLrmiPort", ISRAEL_GATEWAY_LRMI_PORT);
        props.put("target2GatewayHost", gsmLON.getVirtualMachine().getMachine().getHostName());
        props.put("target2GatewayDiscoveryPort", LONDON_GATEWAY_DISCOVERY_PORT);
        props.put("target2GatewayLrmiPort", LONDON_GATEWAY_LRMI_PORT);
        deployGateway(gsmNY, siteDeployment("./apps/gateway/gatewayTwoSourcesSinkOnly", "NY-SINK", props));
    }

    private void deployIsraelRouting() {
        Map<String, String> props = new HashMap<String, String>();
        props.put("localGatewayName", "ISRAEL");
        props.put("targetGatewayName", "NY");
        props.put("targetDelegateThrough", "LONDON");
        props.put("localClusterUrl", "jini://*/*/israelSpace");
        props.put("gatewayLookupGroup", UUID.randomUUID().toString());
        props.put("localGatewayHost", gsmISR.getVirtualMachine().getMachine().getHostName());
        props.put("localGatewayDiscoveryPort", ISRAEL_GATEWAY_DISCOVERY_PORT);
        props.put("localGatewayLrmiPort", ISRAEL_GATEWAY_LRMI_PORT);
        props.put("targetGatewayHost", gsmLON.getVirtualMachine().getMachine().getHostName());
        props.put("targetGatewayDiscoveryPort", LONDON_GATEWAY_DISCOVERY_PORT);
        props.put("targetGatewayLrmiPort", LONDON_GATEWAY_LRMI_PORT);
        deployGateway(gsmISR, siteDeployment("./apps/gateway/gatewayRoutingComponents", "ISRAEL-ROUTING", props));
    }

    private void deployIsraelSink() {
        Map<String, String> props = new HashMap<String, String>();
        props.put("localGatewayName", "ISRAEL");
        props.put("targetGatewayName", "NY");
        props.put("target2GatewayName", "LONDON");
        props.put("localClusterUrl", "jini://*/*/israelSpace");
        props.put("gatewayLookupGroup", UUID.randomUUID().toString());
        props.put("localGatewayHost", gsmISR.getVirtualMachine().getMachine().getHostName());
        props.put("localGatewayDiscoveryPort", ISRAEL_GATEWAY_DISCOVERY_PORT);
        props.put("localGatewayLrmiPort", ISRAEL_GATEWAY_LRMI_PORT);
        props.put("targetGatewayHost", gsmNY.getVirtualMachine().getMachine().getHostName());
        props.put("targetGatewayDiscoveryPort", NY_GATEWAY_DISCOVERY_PORT);
        props.put("targetGatewayLrmiPort", NY_GATEWAY_LRMI_PORT);
        props.put("target2GatewayHost", gsmLON.getVirtualMachine().getMachine().getHostName());
        props.put("target2GatewayDiscoveryPort", LONDON_GATEWAY_DISCOVERY_PORT);
        props.put("target2GatewayLrmiPort", LONDON_GATEWAY_LRMI_PORT);
        deployGateway(gsmISR, siteDeployment("./apps/gateway/gatewayTwoSourcesSinkOnlyBootstrap", "ISRAEL-SINK", props));
    }

    private void deployLondonSite() {
        Map<String, String> props = new HashMap<String, String>();
        props.put("localGatewayName", "LONDON");
        props.put("spaceUrl", "/./londonSpace");
        deploySite(gsmLON, siteDeployment("./apps/gateway/clusterWithoutTargets", "LONDON", props));
    }

    private void deployNYSite() {
        Map<String, String> props = new HashMap<String, String>();
        props.put("localGatewayName", "NY");
        props.put("gatewayTarget", "ISRAEL");
        props.put("spaceUrl", "/./NYSpace");
        deploySite(gsmNY, siteDeployment("./apps/gateway/cluster", "NY", props));
    }

    private void deployIsraelSite() {
        Map<String, String> props = new HashMap<String, String>();
        props.put("localGatewayName", "ISRAEL");
        props.put("gatewayTarget", "NY");
        props.put("spaceUrl", "/./israelSpace");
        deploySite(gsmISR, siteDeployment("./apps/gateway/cluster", "ISRAEL", props)
                .numberOfInstances(2).numberOfBackups(1));
    }
}