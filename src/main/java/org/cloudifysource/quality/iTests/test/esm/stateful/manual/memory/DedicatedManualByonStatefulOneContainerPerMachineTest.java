package org.cloudifysource.quality.iTests.test.esm.stateful.manual.memory;

import iTests.framework.utils.DeploymentUtils;
import iTests.framework.utils.GsmTestUtils;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.esm.AbstractFromXenToByonGSMTest;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.ElasticStatefulProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.core.util.MemoryUnit;
import org.testng.annotations.*;

import java.io.IOException;

public class DedicatedManualByonStatefulOneContainerPerMachineTest extends AbstractFromXenToByonGSMTest {
	
	@BeforeMethod
    public void beforeTest() {
		super.beforeTestInit();
	}
	
	@BeforeClass
	protected void bootstrap() throws Exception {
		super.bootstrapBeforeClass();
	}
	
	@AfterMethod
    public void afterTest() {
		super.afterTest();
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardownAfterClass() throws Exception {
		super.teardownAfterClass();
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT, groups = "1")
    public void testElasticStatefulProcessingUnitDeployment() throws IOException {
    
        repetitiveAssertNumberOfGSCsAdded(0, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfGSAsAdded(1, OPERATION_TIMEOUT);
        final ProcessingUnit pu = super.deploy(
        		new ElasticStatefulProcessingUnitDeployment(DeploymentUtils.getProcessingUnit("simpledata", "processor"))
				.maxMemoryCapacity(256*2, MemoryUnit.MEGABYTES)
				.memoryCapacityPerContainer(256,MemoryUnit.MEGABYTES)
				.dedicatedMachineProvisioning(getMachineProvisioningConfig())
				.singleMachineDeployment()
				.scale(
						new ManualCapacityScaleConfigurer()
						.memoryCapacity(512, MemoryUnit.MEGABYTES)
						.atMostOneContainerPerMachine()
						.create()));

		int expectedNumberOfContainers = 2;
        int expectedNumberOfMachines = 2;
		GsmTestUtils.waitForScaleToCompleteIgnoreCpuSla(pu, expectedNumberOfContainers, expectedNumberOfMachines, OPERATION_TIMEOUT);

        repetitiveAssertNumberOfGSAsAdded(expectedNumberOfMachines, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfGSAsRemoved(0, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfGSCsAdded(expectedNumberOfContainers, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfGSCsRemoved(0, OPERATION_TIMEOUT);
        
		for (GridServiceAgent gsa : admin.getGridServiceAgents()) {
			int containersOnMachine = gsa.getMachine().getGridServiceContainers().getSize();
			AbstractTestSupport.assertEquals(1, containersOnMachine);
		}

		assertUndeployAndWait(pu);
    } 
}
