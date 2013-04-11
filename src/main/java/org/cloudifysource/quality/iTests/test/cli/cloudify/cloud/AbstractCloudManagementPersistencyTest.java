package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.cloud.compute.ComputeTemplate;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.quality.iTests.framework.utils.AssertUtils;
import org.cloudifysource.quality.iTests.framework.utils.CloudBootstrapper;
import org.cloudifysource.quality.iTests.framework.utils.IOUtils;
import org.cloudifysource.quality.iTests.framework.utils.JCloudsUtils;
import org.cloudifysource.quality.iTests.framework.utils.LogUtils;
import org.cloudifysource.quality.iTests.framework.utils.SSHUtils;
import org.cloudifysource.quality.iTests.framework.utils.ScriptUtils;
import org.cloudifysource.quality.iTests.framework.utils.ServiceInstaller;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.restclient.GSRestClient;
import org.cloudifysource.restclient.RestException;
import org.jclouds.compute.domain.NodeMetadata;

import com.j_spaces.kernel.PlatformVersion;

/**
 * User: nirb
 * Date: 06/03/13
 */
public abstract class AbstractCloudManagementPersistencyTest extends NewAbstractCloudTest{

    private static final String PATH_TO_TOMCAT_SERVICE = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/custom-tomcat");;

    private static final String BOOTSTRAP_SUCCEEDED_STRING = "Successfully created Cloudify Manager";

    private static final String EC2_USER = "ec2-user";

    private int numOfManagementMachines = 2;

    private Map<String, Integer> installedServices = new HashMap<String, Integer>();

    private List<String> attributesList = new LinkedList<String>();
    
	protected static final String SCALABLE_SERVICE_NAME = "customServiceMonitor";
	private static final String SCALABLE_SERVICE_PATH = CommandTestUtils.getPath("/src/main/resources/apps/cloudify/recipes/customServiceMonitorPersistency");
	protected static final String SIMPLE_APPLICATION_NAME = "simple";
	private static final String MOCK_APPLICATION_NAME = "mock";
	private static final String SIMPLE_APPLICATION_PATH = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/applications/simple");
	protected static final String DEFAULT_APPLICATION_NAME = "default";

	private static final String MOCK_SERVICE_NAME = "mock";

	private String defaultManagementMachineUrl;
	private String backupManagementMachineUrl;
	GSRestClient client;

    protected void installTomcatService(final int numberOfInstances, final String overrideName) throws IOException, InterruptedException {

        copyCustomTomcatToBuild();

        try {

            // replace number of instances
            File customTomcatGroovy = new File(ScriptUtils.getBuildRecipesServicesPath() + "/custom-tomcat", "tomcat-service.groovy");
            IOUtils.replaceTextInFile(customTomcatGroovy.getAbsolutePath(), "ENTER_NUMBER_OF_INSTANCES", "" + numberOfInstances + "");

            // TODO - Once CLOUDIFY-1591 is fixed, use -name option to override a service installation name.
            // replace name if needed
            String actualServiceName;
            if (overrideName != null) {
                actualServiceName = overrideName;
            } else {
                actualServiceName = "tomcat";
            }
            IOUtils.replaceTextInFile(customTomcatGroovy.getAbsolutePath(), "ENTER_NAME", actualServiceName);

            // install the custom tomcat
            ServiceInstaller tomcatInstaller = new ServiceInstaller(getRestUrl(), actualServiceName);
            tomcatInstaller.recipePath("custom-tomcat");
            tomcatInstaller.timeoutInMinutes(10 * numberOfInstances);
            tomcatInstaller.install();

            installedServices.put(actualServiceName, numberOfInstances);
            CloudBootstrapper bootstrapper = getService().getBootstrapper();
            String attributes = bootstrapper.listServiceInstanceAttributes(DEFAULT_APPLICATION_NAME, actualServiceName, 1, false);
            attributesList.add(attributes.substring(attributes.indexOf("home")));

        } finally  {
            deleteCustomTomcatFromBuild();
        }

    }
    
	protected void assertServiceInstalled(String applicationName, String serviceName) {
		try {
			LogUtils.log("asserting service is installed");
			String output = CommandTestUtils.runCommandAndWait("connect " + this.defaultManagementMachineUrl +"; list-services");
			String absolutePUName = ServiceUtils.getAbsolutePUName(applicationName, serviceName);
			assertTrue("Service " + serviceName + " was not found", output.contains(absolutePUName));
		} catch (Exception e) {
			AssertFail("Could not determin if service is properly installed");
		} 
	}

	protected void assertApplicationInstalled(String applicationName) {
		try {
			LogUtils.log("asserting application is installed");
			String output = CommandTestUtils.runCommandAndWait("connect " + this.defaultManagementMachineUrl +"; list-applications");
			assertTrue("Application " + applicationName + " was not found", output.contains(applicationName));
		} catch (Exception e) {
			AssertFail("Could not determin if application is properly installed after restore");
		} 
	}
	
	protected void restoreManagement() {
		try {
			terminateManagementMachineComponents(this.defaultManagementMachineUrl);
		} catch (Throwable e) {
			LogUtils.log("failed terminating management machine with ip " + this.defaultManagementMachineUrl);
		}
		try {
			terminateManagementMachineComponents(this.backupManagementMachineUrl);
		} catch (Throwable e) {
			LogUtils.log("failed terminating management machine with ip " + this.backupManagementMachineUrl);
		}
        CloudBootstrapper bootstrapper = getService().getBootstrapper();
        bootstrapper.scanForLeakedNodes(false);
        bootstrapper.useExisting(true);
        try {
			bootstrapper.bootstrap();
		} catch (Exception e) {
			AssertFail("management restore failed.");
		}
        bootstrapper.setRestUrl(getRestUrl());
		
	}

    private void copyCustomTomcatToBuild() throws IOException {
        deleteCustomTomcatFromBuild();
        FileUtils.copyDirectoryToDirectory(new File(PATH_TO_TOMCAT_SERVICE), new File(ScriptUtils.getBuildRecipesServicesPath()));
    }

    private void deleteCustomTomcatFromBuild() throws IOException {
        File customTomcat = new File(ScriptUtils.getBuildRecipesServicesPath(), "custom-tomcat");
        if (customTomcat.exists()) {
            FileUtils.deleteDirectory(customTomcat);
        }
    }


    /**
     * 1. Shutdown management machines.
     * 2. Bootstrap using the persistence file.
     * 3. Retrieve attributes from space and compare with the ones before the shutdown.
     * 4. Shutdown an instance agent and wait for recovery.
     * @throws Exception
     */
    public void testManagementPersistency() throws Exception {

        shutdownManagement();

        CloudBootstrapper bootstrapper = getService().getBootstrapper();
        bootstrapper.scanForLeakedNodes(false);
        bootstrapper.useExisting(true);
        bootstrapper.bootstrap();
        bootstrapper.setRestUrl(getRestUrl());

        List<String> newAttributesList = new LinkedList<String>();

        for (String serviceName : installedServices.keySet()) {
            String attributes = bootstrapper.listServiceInstanceAttributes(DEFAULT_APPLICATION_NAME, serviceName, 1, false);
            newAttributesList.add(attributes.substring(attributes.indexOf("home")));
        }

        List<String> differenceAttributesList = new LinkedList<String>(attributesList);
        differenceAttributesList.removeAll(newAttributesList);

        AssertUtils.assertTrue("the service attributes post management restart are not the same as the attributes pre restart", differenceAttributesList.isEmpty());

        JCloudsUtils.createContext(getService());
        Set<? extends NodeMetadata> machines = JCloudsUtils.getAllRunningNodes();
        String agentServerId = "no agent server found";

        for(NodeMetadata node : machines){
            if(node.getName() != null && !node.getName().isEmpty() && node.getName().contains(getService().getMachinePrefix()) && node.getName().contains("agent")){
                agentServerId = node.getId();
                break;
            }
        }

        LogUtils.log("Shutting down instance with id " + agentServerId);
        JCloudsUtils.shutdownServer(agentServerId);
        JCloudsUtils.closeContext();

        LogUtils.log("Waiting for service to restart on a new machine");
        final GSRestClient client = new GSRestClient("", "", new URL(getRestUrl()), PlatformVersion.getVersionNumber());

        final AtomicReference<String> brokenService = new AtomicReference<String>();

        AssertUtils.repetitiveAssertTrue("Service didn't break", new AssertUtils.RepetitiveConditionProvider() {
            @Override
            public boolean getCondition() {
                try {

                    // we don't know which service the agent we shutdown belonged to.
                    // query all installed services to find out.
                    for (String serviceName : installedServices.keySet()) {
                        String serviceRestUrl = "ProcessingUnits/Names/" + DEFAULT_APPLICATION_NAME + "." + serviceName;
                        int numberOfInstances = (Integer)client.getAdminData(serviceRestUrl).get("Instances-Size");
                        LogUtils.log("Number of " + serviceName + " instances is " + numberOfInstances);
                        if (numberOfInstances < installedServices.get(serviceName)) {
                            LogUtils.log(serviceName + " service broke. it now has only " + numberOfInstances + " instances");
                            brokenService.set(serviceName);
                        }
                    }
                    return (brokenService.get() != null);
                } catch (RestException e) {
                    throw new RuntimeException(e);
                }

            }
        } , OPERATION_TIMEOUT * 4);

        // now we already know the service that broke.
        // so we wait for it to recover.
        AssertUtils.repetitiveAssertTrue(brokenService.get() + " service did not recover", new AssertUtils.RepetitiveConditionProvider() {
            @Override
            public boolean getCondition() {
                final String brokenServiceRestUrl = "ProcessingUnits/Names/" + DEFAULT_APPLICATION_NAME + "." + brokenService.get();
                try {
                    int numOfInst = (Integer) client.getAdminData(brokenServiceRestUrl).get("Instances-Size");
                    return (installedServices.get(brokenService.get()) == numOfInst);

/* CLOUDIFY-1602
                    int numOfPlannedInstances = Integer.parseInt((String) client.getAdminData(brokenServiceRestUrl).get("PlannedNumberOfInstances"));
                    return (installedServices.get(brokenService.get()) == numOfPlannedInstances);
*/

                } catch (RestException e) {
                    throw new RuntimeException("caught a RestException", e);
                }
            }
        } , OPERATION_TIMEOUT * 3);
    }
    
    /**
     * 1. install a scalable service and a simple application
     * 2. shut down one of 2 management machines
     * 3. assert all uninstall, install, and scale attempts fail.
     * 
     */
    public void testInstallServiceAndApplicationOnGsmFailure() throws Exception {
    	LogUtils.log("Installing service and application required for test");
    	installServiceAndWait(SCALABLE_SERVICE_PATH, SCALABLE_SERVICE_NAME);
    	installApplicationAndWait(SIMPLE_APPLICATION_PATH, SIMPLE_APPLICATION_NAME);
    	
    	LogUtils.log("Killing one of two management machines");
    	terminateManagementMachineComponents(this.backupManagementMachineUrl);
    	boolean result = waitForRestAdminToDetectMachineFailure(this.defaultManagementMachineUrl, 1, 10000 * 6, TimeUnit.SECONDS);
    	assertTrue("Failed waiting for rest admin to detect machine failure.", result == true );
    	
    	LogUtils.log("Attemting to uninstall application. This should fail.");
    	String uninstallApplicationOutput = uninstallApplicationAndWait(SIMPLE_APPLICATION_NAME, true);
    	assertGsmFailureOutput(uninstallApplicationOutput);
    	
    	LogUtils.log("Attemting to uninstall service. This should fail.");
    	String uninstallServiceOutput = uninstallServiceAndWait(SCALABLE_SERVICE_NAME, true);
    	assertGsmFailureOutput(uninstallServiceOutput);
    	
    	LogUtils.log("Attempting to install a simple service. This is expected to fail");
    	String installServiceOutput = installServiceAndWait(SCALABLE_SERVICE_PATH, MOCK_SERVICE_NAME, true);
    	LogUtils.log("asserting service installation failed due to gsm failure");
    	assertGsmFailureOutput(installServiceOutput);
    	
    	LogUtils.log("Attempting to install a simple application. This is expected to fail.");
    	String installApplicationOutput = installApplicationAndWait(SIMPLE_APPLICATION_PATH, MOCK_APPLICATION_NAME, 15, true);
    	assertGsmFailureOutput(installApplicationOutput);
    	
    	LogUtils.log("Attempting to manually scale service " + SCALABLE_SERVICE_NAME + ". This should fail");
    	String manualScaleOutput = scaleServiceUsingCommand(2, true);
    	assertGsmFailureOutput(manualScaleOutput);
    	
    }
    
    private boolean waitForRestAdminToDetectMachineFailure(String restUrl, int expectedNumberOfManagement, long timeout, TimeUnit unit)
    		throws InterruptedException {
    	LogUtils.log("Waiting for rest admin to detect machine failure");
    	long end = System.currentTimeMillis() + unit.toMillis(timeout);
    	while (end > System.currentTimeMillis()) {
    		int gsmCount = 0;
    		try {
    			gsmCount = Integer.parseInt((String)this.client.getAdmin("GridServiceManagers/").get("Size"));
    		} catch (Exception e) {
    			//Do nothing.
    		}
    		if (gsmCount == expectedNumberOfManagement) {
    			return true;
    		}
    		Thread.sleep(2000);
    	}
    	return false;
    }
    
    private String scaleServiceUsingCommand(int numberOfInstances, boolean expectedFail) 
			throws IOException, InterruptedException {
		if (expectedFail) {
			return CommandTestUtils.runCommandExpectedFail("connect " + this.defaultManagementMachineUrl + ";" +
					";set-instances " + SCALABLE_SERVICE_NAME + " " + numberOfInstances);
		} else {
			return CommandTestUtils.runCommandAndWait("connect " + this.defaultManagementMachineUrl + ";" +
									";set-instances " + SCALABLE_SERVICE_NAME + " " + numberOfInstances);
		}
	}
    
    protected void terminateManagementMachineComponents(String restUrl) {
		File pemFile = getPemFile();
		String command = "killall -9 java";
		String ipAddress = restUrl.substring(restUrl.indexOf("//") + 2, restUrl.lastIndexOf(":"));
		Cloud cloud = getService().getCloud();
		String managementMachineTemplate = cloud.getConfiguration().getManagementMachineTemplate();
		ComputeTemplate managementTemplate = cloud.getCloudCompute().getTemplates().get(managementMachineTemplate);
		String userName = managementTemplate.getUsername();
		SSHUtils.runCommand(ipAddress, 10000, command, userName, pemFile);
	}
    
    
    void assertGsmFailureOutput(String installServiceOutput) {
		assertTrue("Expecting install failure output to contain gsm info.",
    				installServiceOutput.contains("Persistency requires all GSM components be available." +
    						" Expecting 2, Found 1: Operation failed."));
	}
    
    /**
     * 1. install a scalable service with one instance. 
     * 2. scale that service so that it will have two instances.
     * 3. kill one of the 2 management machines.
     * 4. assert ESM auto-scale fails both for scale-out and for scale-in. 
     * 
     */
    public void testAutoScaleServiceOnGsmFailure() throws Exception {
    	LogUtils.log("Installing a scalable service");
    	installServiceAndWait(SCALABLE_SERVICE_PATH, SCALABLE_SERVICE_NAME);
    	scaleServiceUsingHook(2);
    	boolean scaleResult = waitForServiceScale(2, DEFAULT_TEST_TIMEOUT / 2 , TimeUnit.MILLISECONDS);
    	assertTrue("service did not scale out as expected", scaleResult);

    	LogUtils.log("killing backup management machine");
    	terminateManagementMachineComponents(this.backupManagementMachineUrl);
    	boolean waitForAdminResult = waitForRestAdminToDetectMachineFailure(this.defaultManagementMachineUrl, 1, 1, TimeUnit.MINUTES);
    	assertTrue("Failed waiting for rest admin to detect machine failure.", waitForAdminResult == true);

    	scaleServiceUsingHook(3);
    	scaleResult = waitForServiceScale(3, DEFAULT_TEST_TIMEOUT / 2, TimeUnit.MILLISECONDS);
    	assertTrue("Service scale was expected to fail", scaleResult != true);

    	scaleServiceUsingHook(1);
    	scaleResult = waitForServiceScale(1, DEFAULT_TEST_TIMEOUT / 2, TimeUnit.MILLISECONDS);
    	assertTrue("Service scale was expected to fail", scaleResult != true);
    }
	
    //This call activates a hook that mocks the service monitors.
    private void scaleServiceUsingHook(int expectedNumberOfInstances) 
    		throws IOException, InterruptedException, RestException {
    	int counterValue = 0;
    	if (expectedNumberOfInstances == 2) {
    		counterValue = 100;
    	} else if (expectedNumberOfInstances == 3) {
    		counterValue = 1000;
    	}
    	int numInstances = (Integer)getServiceProperty(this.defaultManagementMachineUrl, SCALABLE_SERVICE_NAME, DEFAULT_APPLICATION_NAME, "Instances-Size");
    	for (int i = 0; i < numInstances; i++) {
    		CommandTestUtils.runCloudifyCommandAndWait("connect  " + this.defaultManagementMachineUrl 
    				+ ";invoke -instanceid " + (i + 1)  + " --verbose " + SCALABLE_SERVICE_NAME + " set " + counterValue);
    	}
    }
		
    private Object getServiceProperty(String restUrl, String serviceName, String applicationName, String propertyKey) 
    		throws MalformedURLException, RestException {
    	String absolutePUName = ServiceUtils.getAbsolutePUName(applicationName, serviceName);
    	GSRestClient client = new GSRestClient("", "", new URL(restUrl), PlatformVersion.getVersionNumber());
    	return client.getAdmin("ProcessingUnits/Names/" + absolutePUName).get(propertyKey);
    }

    private boolean waitForServiceScale(final int expectedNumOfInstances, long timeout, TimeUnit unit) 
    		throws MalformedURLException, RestException, InterruptedException {
    	long end = System.currentTimeMillis() + unit.toMillis(timeout);
    	while (end > System.currentTimeMillis()) {
    		int instanceSize = ((Integer)getServiceProperty(this.defaultManagementMachineUrl, 
    				SCALABLE_SERVICE_NAME, DEFAULT_APPLICATION_NAME, "Instances-Size"));
    		if (instanceSize == expectedNumOfInstances) {
    			return true;
    		}
    		Thread.sleep(2000);
    	}
    	return false;
    }

    /**
     * 1. Shutdown management machines.
     * 2. Bootstrap without persistence file. (Only for DefaultProvisioningDriver)
     * 3. Check management machines are the same.
     * 4. repeat 1-3, 4 times.
     * @throws Exception
     */
    protected void testRepetitiveShutdownManagersBootstrap() throws Exception {

        // retrieve the rest url's before we start the chaos.
        final Set<String> originalRestUrls = toSet(getService().getRestUrls());

        int repetitions = 4;

        for(int i=0; i < repetitions; i++){

            shutdownManagement();

            CloudBootstrapper bootstrapper = getService().getBootstrapper();
            bootstrapper.scanForLeakedNodes(false);
            bootstrapper.useExisting(true);
            bootstrapper.bootstrap();

            String output = bootstrapper.getLastActionOutput();

            AssertUtils.assertTrue("bootstrap failed", output.contains("Successfully created Cloudify Manager"));

            // check the rest urls are the same;
            final Set<String> newRestUrls = new HashSet<String>();
            for (URL url : getService().getBootstrapper().getRestAdminUrls()) {
                newRestUrls.add(url.toString());
            }
            AssertUtils.assertEquals("Expected rest url's not to change after re-bootstrapping", originalRestUrls, newRestUrls);
        }
    }


    protected void shutdownManagement() throws Exception{

        CloudBootstrapper bootstrapper = getService().getBootstrapper();
        bootstrapper.setRestUrl(getRestUrl());

        LogUtils.log("shutting down managers");
        bootstrapper.shutdownManagers(DEFAULT_APPLICATION_NAME, false);
    }

    private Set<String> toSet(final String[] array) {
        final Set<String> set = new HashSet<String>();
        for (String s : array) {
            set.add(s);
        }
        return set;
    }

    public void testCorruptedPersistencyDirectory() throws Exception {

        String persistencyFolderPath = getService().getCloud().getConfiguration().getPersistentStoragePath();
        String fileToDeletePath = persistencyFolderPath + "/management-space/db.h2.h2.db";
        JCloudsUtils.createContext(getService());
        Set<? extends NodeMetadata> managementMachines = JCloudsUtils.getServersByName(getService().getMachinePrefix() + "cloudify-manager");
        JCloudsUtils.closeContext();

        Iterator<? extends NodeMetadata> managementNodesIterator = managementMachines.iterator();
        String machineIp1 = managementNodesIterator.next().getPublicAddresses().iterator().next();
        String machineIp2 = managementNodesIterator.next().getPublicAddresses().iterator().next();

        SSHUtils.runCommand(machineIp1, OPERATION_TIMEOUT, "rm -rf " + fileToDeletePath, EC2_USER, getPemFile());
        SSHUtils.runCommand(machineIp2, OPERATION_TIMEOUT, "rm -rf " + fileToDeletePath, EC2_USER, getPemFile());

        shutdownManagement();

        CloudBootstrapper bootstrapper = getService().getBootstrapper();
        bootstrapper.setBootstrapExpectedToFail(true);
        bootstrapper.timeoutInMinutes(15);
        bootstrapper.useExisting(true);
        bootstrapper.bootstrap();

        String output = bootstrapper.getLastActionOutput();
        AssertUtils.assertTrue("bootstrap succeeded with a corrupted persistency folder", !output.contains(BOOTSTRAP_SUCCEEDED_STRING));

    }

    @Override
    protected void customizeCloud() throws Exception {
        super.customizeCloud();
        getService().setNumberOfManagementMachines(numOfManagementMachines);
        getService().getProperties().put("persistencePath", "/home/ec2-user/persistence");
    }
    
	protected void initRestClient() 
			throws MalformedURLException, RestException {
		this.client = new GSRestClient("", "", new URL(this.defaultManagementMachineUrl), "2.5.0-Cloudify-ga");//PlatformVersion.getVersionNumber()
	}

	protected void initManagementUrls() {
		String[] restUrls = cloudService.getRestUrls();
    	this.defaultManagementMachineUrl = restUrls[0];
    	this.backupManagementMachineUrl = restUrls[1];
	}

    @Override
    protected abstract String getCloudName();

    @Override
    protected abstract boolean isReusableCloud();
}
