package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon.agentrestart;

import iTests.framework.utils.LogUtils;
import iTests.framework.utils.MavenUtils;
import iTests.framework.utils.SSHUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.utils.IPUtils;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon.AbstractByonCloudTest;
import org.cloudifysource.restclient.GSRestClient;
import org.cloudifysource.restclient.RestException;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.pu.ProcessingUnit;

import com.j_spaces.kernel.PlatformVersion;

/**
 * 
 * @author adaml
 *
 */
public class AbstractAgentMaintenanceModeTest extends AbstractByonCloudTest {

	protected static final String SERVICE_NAME = "simpleRestartAgent";
	protected static final String APP_NAME = "default";
	protected final static String absolutePuName = ServiceUtils.getAbsolutePUName(APP_NAME, SERVICE_NAME);
	protected static final String REBOOT_COMMAND = "sudo reboot";
	protected static final long DEFAULT_WAIT_MINUTES = DEFAULT_TEST_TIMEOUT / 2;
	protected static final int INFINITY_MINUTES = 600;
	
	
	protected void startMaintenanceMode(final long timeoutInSeconds) throws IOException, InterruptedException {
    	String output = CommandTestUtils.runCommandAndWait("connect " + this.getRestUrl() + ";" 
    			+ " invoke simpleRestartAgent startMaintenanceMode " + timeoutInSeconds);
    	assertTrue(output.contains("invocation completed successfully."));
	}
    
	protected void stopMaintenanceMode(final String processingUnitName) throws IOException, InterruptedException {
		String output = CommandTestUtils.runCommandAndWait("connect " + this.getRestUrl() + ";" 
				+ " invoke simpleRestartAgent stopMaintenanceMode");
		assertTrue(output.contains("invocation completed successfully."));
	}
    
    protected String getServicePath(final String serviceName) {
    	return CommandTestUtils.getPath("src/main/resources/apps/USM/usm/" + serviceName);
    }
    
    	
    protected void restartAgentMachine(final String puName) throws IOException {
    	final ProcessingUnit pu = admin.getProcessingUnits()
    			.waitFor(puName, DEFAULT_WAIT_MINUTES, TimeUnit.MINUTES);
    	pu.waitFor(1);
    	final String hostName = pu.getInstances()[0].getMachine().getHostName();
    	
    	final String ip = IPUtils.resolveHostNameToIp(hostName);
    	assertMachineState(ip, true);
    	LogUtils.log("rebooting machine with ip " + ip);
    	LogUtils.log(SSHUtils.runCommand(ip, DEFAULT_TEST_TIMEOUT / 2, 
    						REBOOT_COMMAND, 
    						MavenUtils.username, 
    						MavenUtils.password));
    	assertMachineState(ip, false);
    	LogUtils.log("Machine with ip " + ip + " is rebooting...");
    }
	
    private void assertMachineState(String ip, boolean isRunningExpected) 
    		throws UnknownHostException, IOException {
    	LogUtils.log("checking if machine is running");
    	if (isRunningExpected) {
    		assertTrue("Machine is expected to be running", InetAddress.getByName(ip).isReachable(3000));
    		LogUtils.log("Machine with ip " + ip + " is running");
    	} else {
    		assertTrue("Expecting machine to be in resart mode", InetAddress.getByName(ip).isReachable(3000));
    		LogUtils.log("Machine with ip " + ip + " is rebooting");
    	}
    }
    
	protected void assertNumberOfMachines(final int numberOfMachines) {
		assertTrue("expecting number of machines to be " + numberOfMachines + " but found " +  admin.getMachines().getSize(),
				admin.getMachines().getSize() == numberOfMachines);
	}
	
	protected void gracefullyShutdownAgent(final String absolutePUName) {
		LogUtils.log("Shutting down the only agent for service " + absolutePUName);
		ProcessingUnit serviceProcessingUnit = admin.getProcessingUnits().waitFor(absolutePUName, 1, TimeUnit.MINUTES);
		serviceProcessingUnit.waitFor(1, 1, TimeUnit.MINUTES);
		GridServiceAgent gridServiceAgent = serviceProcessingUnit.getInstances()[0].getMachine().getGridServiceAgent();
		gridServiceAgent.shutdown();
		LogUtils.log("agent shut down successfully");
    }
	
	protected String getServiceIP(String serviceName) throws RestException, MalformedURLException {
		final GSRestClient client = new GSRestClient("", "", new URL(getRestUrl()), PlatformVersion
                .getVersionNumber());
		LogUtils.log("getting private IP for service named '" + serviceName + "'");
		String privateIpUrl = "ProcessingUnits/Names/" + serviceName + "/Instances/0/JVMDetails/EnvironmentVariables/GIGASPACES_AGENT_ENV_PRIVATE_IP";
		String ipAddress = (String) client.getAdminData(privateIpUrl).get("GIGASPACES_AGENT_ENV_PRIVATE_IP");
		LogUtils.log("found service ip address: " + ipAddress);
        return ipAddress;
	}
}
