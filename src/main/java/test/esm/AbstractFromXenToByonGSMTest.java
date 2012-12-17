package test.esm;

import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.cloud.CloudTemplate;
import org.cloudifysource.esc.driver.provisioning.CloudifyMachineProvisioningConfig;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.machine.events.ElasticMachineProvisioningProgressChangedEvent;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnits;
import org.openspaces.admin.pu.elastic.ElasticMachineProvisioningConfig;
import org.openspaces.admin.pu.elastic.ElasticStatefulProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.ElasticStatelessProcessingUnitDeployment;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.grid.gsm.machines.plugins.events.MachineStartRequestedEvent;
import org.openspaces.grid.gsm.machines.plugins.events.MachineStartedEvent;
import org.openspaces.grid.gsm.machines.plugins.events.MachineStopRequestedEvent;
import org.openspaces.grid.gsm.machines.plugins.events.MachineStoppedEvent;

import test.cli.cloudify.cloud.byon.AbstractByonCloudTest;
import test.cli.cloudify.cloud.services.byon.ByonCloudService;
import framework.utils.AssertUtils;
import framework.utils.GridServiceAgentsCounter;
import framework.utils.GridServiceContainersCounter;
import framework.utils.LogUtils;

public class AbstractFromXenToByonGSMTest extends AbstractByonCloudTest {
	
	public final static long OPERATION_TIMEOUT = 5 * 60 * 1000;
	public final static String DefaultByonXapMachineMemoryMB = "5000";
	public final static String standardMachineMemoryMB = "1600";
	public final static int NUM_OF_CORES = 2;
	private MachinesEventsCounter machineEventsCounter;
	
	private GridServiceContainersCounter gscCounter;
    private GridServiceAgentsCounter gsaCounter;
	
    public void repetitiveAssertNumberOfGSAsHolds(int expectedAdded, int expectedRemoved, long timeoutMilliseconds) {
    	gsaCounter.repetitiveAssertNumberOfGSAsHolds(expectedAdded, expectedRemoved, timeoutMilliseconds);
    }
    
    public void repetitiveAssertNumberOfGSAsAdded(int expected, long timeoutMilliseconds) {
    	gsaCounter.repetitiveAssertNumberOfGridServiceAgentsAdded(expected, timeoutMilliseconds);
    }
    
    public void repetitiveAssertNumberOfGSAsRemoved(int expected, long timeoutMilliseconds) {
    	gsaCounter.repetitiveAssertNumberOfGridServiceAgentsRemoved(expected, timeoutMilliseconds);
    }
    
    public void repetitiveAssertGridServiceAgentRemoved(final GridServiceAgent agent, long timeoutMilliseconds) {
    	gsaCounter.repetitiveAssertGridServiceAgentRemoved(agent, timeoutMilliseconds);
    }
    
    public void repetitiveAssertNumberOfGSCsAdded(int expected, long timeoutMilliseconds) {
    	gscCounter.repetitiveAssertNumberOfGridServiceContainersAdded(expected, timeoutMilliseconds);
    }
    
    public void repetitiveAssertNumberOfGSCsRemoved(int expected, long timeoutMilliseconds) {
    	gscCounter.repetitiveAssertNumberOfGridServiceContainersRemoved(expected, timeoutMilliseconds);
    }
	
    public void repetitiveAssertNumberOfGridServiceContainersHolds(final int expectedAdded, final int expectedRemoved, long timeout, TimeUnit timeunit) {
    	gscCounter.repetitiveAssertNumberOfGridServiceContainersHolds(expectedAdded, expectedRemoved, timeout, timeunit);
    }
    
    protected void repetitiveAssertNumberOfMachineEvents(Class<? extends ElasticMachineProvisioningProgressChangedEvent> eventClass, int expected, long timeoutMilliseconds) {
		machineEventsCounter.repetitiveAssertNumberOfMachineEvents(eventClass, expected, timeoutMilliseconds);
	}
    
	
	protected void bootstrapBeforeClass() throws Exception {
		super.bootstrap();
	}
	
    public void beforeTestInit() {
		gscCounter = new GridServiceContainersCounter(admin); 
        gsaCounter = new GridServiceAgentsCounter(admin);
        machineEventsCounter = new MachinesEventsCounter(admin);
		repetitiveAssertNumberOfMachineEvents(MachineStartRequestedEvent.class, 0, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfMachineEvents(MachineStartedEvent.class, 0, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfMachineEvents(MachineStopRequestedEvent.class, 0, OPERATION_TIMEOUT);
        repetitiveAssertNumberOfMachineEvents(MachineStoppedEvent.class, 0, OPERATION_TIMEOUT);
	}
	
    public void afterTest() {
    	if (gscCounter != null) {
        	gscCounter.close();
        }
        if (gsaCounter != null) {
        	gsaCounter.close();
        }
        ProcessingUnits processingUnits = admin.getProcessingUnits();
		if (processingUnits.getSize() > 0) {
        	LogUtils.log(this.getClass() + " test has not undeployed all processing units !!!");
        }
        for (ProcessingUnit pu : processingUnits) {
        	//cleanup
        	if (!pu.undeployAndWait(OPERATION_TIMEOUT,TimeUnit.MILLISECONDS)) {
        		LogUtils.log(this.getClass() + "#afterTest() failed to undeploy " + pu.getName());
        	}
        }
	}
	
	protected void teardownAfterClass() throws Exception {
		super.teardown(admin);
	}

	@Override
	protected String getCloudName() {
		return "byon-xap";
	}
	
	@Override
	protected void afterBootstrap() throws Exception {
		admin = super.createAdmin();
	}
	
	protected void customizeCloud() throws Exception {
		String oldMemory = "machineMemoryMB " + standardMachineMemoryMB;
		String newMemory = "machineMemoryMB " + DefaultByonXapMachineMemoryMB;
		String numOfCores = "numberOfCores "+NUM_OF_CORES;
		// sets number of cores to 4 - can be modified
		getService().getAdditionalPropsToReplace().put(oldMemory, newMemory +"\n"+numOfCores);					
	}

	protected void assertUndeployAndWait(ProcessingUnit pu) {
		LogUtils.log("Undeploying processing unit " + pu.getName());
		boolean success = pu.undeployAndWait(OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
		AssertUtils.assertTrue("Undeployment of "+pu.getName()+"failed",success);
		LogUtils.log("Undeployed processing unit " + pu.getName());		
	}
	
	protected ElasticMachineProvisioningConfig getMachineProvisioningConfig() {
		String templateName = "SMALL_LINUX";
		ByonCloudService cloudService = getService();
		Cloud cloud = cloudService.getCloud();
		final CloudTemplate template = cloud.getTemplates().get(templateName);			
		CloudTemplate managementTemplate = cloud.getTemplates().get(cloud.getConfiguration().getManagementMachineTemplate());
		managementTemplate.getRemoteDirectory();
		final CloudifyMachineProvisioningConfig config = new CloudifyMachineProvisioningConfig(
				cloud, template, templateName,
				managementTemplate.getRemoteDirectory());
		return config;
	}

	protected ProcessingUnit deploy(ElasticStatefulProcessingUnitDeployment deployment) {
		return admin.getGridServiceManagers().getManagers()[0].deploy(deployment);
	} 
	
	protected ProcessingUnit deploy(ElasticStatelessProcessingUnitDeployment deployment) {
		return admin.getGridServiceManagers().getManagers()[0].deploy(deployment);
	} 
	
	protected ProcessingUnit deploy(ElasticSpaceDeployment deployment) {
		return admin.getGridServiceManagers().getManagers()[0].deploy(deployment);
	} 
	
}
