package test.usm;

import static framework.utils.AdminUtils.loadGSCs;
import static framework.utils.AdminUtils.loadGSM;
import static framework.utils.LogUtils.log;

import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.gigaspaces.cloudify.dsl.Service;


public class USMMultipleDeployAndRestartTest extends UsmAbstractTest {

	private Machine machineA, machineB;

	@Override
    @BeforeMethod
    public void beforeTest() {
	    super.beforeTest();
	    
        //1 GSM and 2 GSC at 2 machines
        log("waiting for 2 machines");
        admin.getMachines().waitFor(2);

        log("waiting for 2 GSAs");
        admin.getGridServiceAgents().waitFor(2);

        GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
        GridServiceAgent gsaA = agents[0];
        GridServiceAgent gsaB = agents[1];

        machineA = gsaA.getMachine();
        machineB = gsaB.getMachine();

        //Start GSM A, GSC A, GSM B, GSC B
        log("starting: 1 GSM and 2 GSC at 2 machines");
        GridServiceManager gsmA = loadGSM(machineA); //GSM A
        loadGSCs(machineA, 1); //GSC A
        //loadGSM(machineB); //GSM B
        loadGSCs(machineB, 1); //GSC B


    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "2")
    public void test() throws Exception {
    	Service service = USMTestUtils.usmDeploy(processName, this.serviceFileName);

        ProcessingUnit pu = admin.getProcessingUnits().waitFor(service.getName());
        pu.waitFor(pu.getTotalNumberOfInstances());
        pu.startStatisticsMonitor();


        USMTestUtils.assertMonitors(pu);

        ProcessingUnitInstance restartedOUInstance = pu.getInstances()[0].restartAndWait();
        
        USMTestUtils.assertMonitors(pu);

        Assert.assertEquals(1, admin.getProcessingUnits().getProcessingUnit(service.getName()).getInstances().length);

        pu.undeploy();
        Assert.assertNull(admin.getProcessingUnits().getProcessingUnit(service.getName()));

    }

}
