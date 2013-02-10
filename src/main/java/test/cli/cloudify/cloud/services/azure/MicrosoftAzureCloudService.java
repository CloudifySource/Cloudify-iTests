package test.cli.cloudify.cloud.services.azure;

import com.gigaspaces.internal.utils.StringUtils;
import framework.tools.SGTestHelper;
import framework.utils.IOUtils;
import framework.utils.LogUtils;
import org.apache.commons.io.FileUtils;
import org.cloudifysource.esc.driver.provisioning.azure.client.MicrosoftAzureException;
import org.cloudifysource.esc.driver.provisioning.azure.client.MicrosoftAzureRestClient;
import org.cloudifysource.esc.driver.provisioning.azure.client.UUIDHelper;
import org.cloudifysource.esc.driver.provisioning.azure.model.*;
import test.cli.cloudify.cloud.services.AbstractCloudService;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeoutException;

public class MicrosoftAzureCloudService extends AbstractCloudService {
    private static final String AZURE_CERT_PROPERTIES = "credentials/cloud/azure/azure-cert.properties";

    private Properties certProperties = getCloudProperties(AZURE_CERT_PROPERTIES);

	private final String AZURE_CERT_PFX = certProperties.getProperty("AZURE_CERT_PFX");

	private String userName = System.getProperty("user.name") + UUIDHelper.generateRandomUUID(3);
	
	public static final String DEFAULT_IMAGE_ID = "5112500ae3b842c8b9c604889f8753c3__OpenLogic-CentOS63DEC20121220";

	private final MicrosoftAzureRestClient azureClient;
	private final String AZURE_SUBSCRIPTION_ID = certProperties.getProperty("AZURE_SUBSCRIPTION_ID");
	private static final String PATH_TO_PFX = SGTestHelper.getSGTestRootDir() + "/src/main/resources/credentials/cloud/azure/azure-cert.pfx";
	private final String PFX_PASSWORD = certProperties.getProperty("PFX_PASSWORD");

	private final String ADDRESS_SPACE = certProperties.getProperty("ADDRESS_SPACE");

	private static final long ESTIMATED_SHUTDOWN_TIME = 5 * 60 * 1000;

	private static final long SCAN_INTERVAL = 10 * 1000; // 10 seconds. long time since it takes time to shutdown the machine

	private static final long SCAN_TIMEOUT = 5 * 60 * 1000; // 5 minutes




	private String password = PFX_PASSWORD;
	private String addressSpace = ADDRESS_SPACE;
	private String affinityLocation = "East US";
	private String affinityGroup = userName + "cloudifyaffinity";
	private String virtualNetworkSiteName = userName + "networksite";
	private String storageAccountName = userName + "cloudifystorage";

	public String getStorageAccountName() {
		return storageAccountName;
	}

	public void setStorageAccountName(String storageAccountName) {
		this.storageAccountName = storageAccountName;
	}

	public MicrosoftAzureCloudService() {
		super("azure");
		azureClient = new MicrosoftAzureRestClient(AZURE_SUBSCRIPTION_ID, 
				PATH_TO_PFX, PFX_PASSWORD, 
				null, null, null);
	}
	
	public MicrosoftAzureRestClient getRestClient() {
		return azureClient;
	}
	
	
	public String getAffinityGroupPrefix() {
		return affinityGroup;
	}

	public void setAffinityGroup(String affinityGroup) {
		this.affinityGroup = affinityGroup;
	}

	public String getVirtualNetworkSiteNamePrefix() {
		return virtualNetworkSiteName;
	}

	public void setVirtualNetworkSiteName(String virtualNetworkSiteName) {
		this.virtualNetworkSiteName = virtualNetworkSiteName;
	}

	public void setAffinityLocation(final String affinityLocation) {
		this.affinityLocation = affinityLocation;
	}

	public void setAddressSpace(final String addressSpace) {
		this.addressSpace = addressSpace;
	}
	
	public void setPassword(final String password) {
		this.password = password;
	}

	@Override
	public void injectCloudAuthenticationDetails() throws IOException {
		copyCustomCloudConfigurationFileToServiceFolder();
		copyPrivateKeyToUploadFolder();
		
		getProperties().put("subscriptionId", AZURE_SUBSCRIPTION_ID);
		getProperties().put("username", userName);
		getProperties().put("password", password);
		getProperties().put("pfxFile", AZURE_CERT_PFX);
		getProperties().put("pfxPassword", PFX_PASSWORD);
		
		final Map<String, String> propsToReplace = new HashMap<String, String>();
		propsToReplace.put("cloudify_agent_", getMachinePrefix().toLowerCase() + "cloudify-agent");
		propsToReplace.put("cloudify_manager", getMachinePrefix().toLowerCase() + "cloudify-manager");
		propsToReplace.put("ENTER_AVAILABILITY_SET", userName);
		propsToReplace.put("ENTER_DEPLOYMENT_SLOT", "Staging");
		propsToReplace.put("ENTER_VIRTUAL_NETWORK_SITE_NAME", virtualNetworkSiteName);
		propsToReplace.put("ENTER_ADDRESS_SPACE", addressSpace);
		propsToReplace.put("ENTER_AFFINITY_GROUP", affinityGroup);
		propsToReplace.put("ENTER_LOCATION", affinityLocation);
		propsToReplace.put("ENTER_STORAGE_ACCOUNT", storageAccountName);
		IOUtils.replaceTextInFile(getPathToCloudGroovy(), propsToReplace);	
	}

	@Override
	public String getUser() {
		return "sgtest";
	}

	@Override
	public String getApiKey() {
		throw new UnsupportedOperationException("Microsoft Azure Cloud Driver does not have an API key concept. this method should have never been called");
	}

	@Override
	public boolean scanLeakedAgentNodes() {		
		final String agentPrefix = getCloud().getProvider().getMachineNamePrefix();		
		return scanNodesWithPrefix(agentPrefix);
	} 
	
	@Override
	public boolean scanLeakedAgentAndManagementNodes() {
		final String agentPrefix = getCloud().getProvider().getMachineNamePrefix();
		final String mgmtPrefix = getCloud().getProvider().getManagementGroup();
		return scanNodesWithPrefix(agentPrefix , mgmtPrefix);
	}
	
	private boolean scanNodesWithPrefix(final String... prefixes) {
		
		LogUtils.log("Scanning leaking nodes with prefix " + StringUtils.arrayToCommaDelimitedString(prefixes));
		
		long scanEndTime = System.currentTimeMillis() + SCAN_TIMEOUT;

		try {

			List<String> leakingAgentNodesPublicIps = new ArrayList<String>();

			HostedServices listHostedServices = azureClient.listHostedServices();
			Deployments deploymentsBeingDeleted = null;

			do {
				if (System.currentTimeMillis() > scanEndTime) {
					throw new TimeoutException("Timed out waiting for deleting nodes to finish. last status was : " + deploymentsBeingDeleted.getDeployments());
				}
				Thread.sleep(SCAN_INTERVAL);
				LogUtils.log("Waiting for all deployments to reach a non 'Deleting' state");
				for (HostedService hostedService : listHostedServices) {
					try {
						List<Deployment> deploymentsForHostedSerice = azureClient.getHostedService(hostedService.getServiceName(), true).getDeployments().getDeployments();
						if (deploymentsForHostedSerice.size() > 0) {
							Deployment deployment = deploymentsForHostedSerice.get(0); // each hosted service will have just one deployment.
							if (deployment.getStatus().toLowerCase().equals("deleting")) {
								LogUtils.log("Found a deployment with name : " + deployment.getName() + " and status : " + deployment.getStatus());
								deploymentsBeingDeleted = new Deployments();
								deploymentsBeingDeleted.getDeployments().add(deployment);
							}
						}
					} catch (MicrosoftAzureException e) {
						LogUtils.log("Failed retrieving deployments from hosted service : " + hostedService.getServiceName() + " Reason --> " + e.getMessage()); 
					}
				}

			}

			while (deploymentsBeingDeleted != null && !(deploymentsBeingDeleted.getDeployments().isEmpty()));


			// now all deployment have reached a steady state.
			// scan again to find out if there are any agents still running

			LogUtils.log("Scanning all remaining hosted services for running nodes");
			for (HostedService hostedService : listHostedServices) {
				List<Deployment> deploymentsForHostedSerice = azureClient.getHostedService(hostedService.getServiceName(), true).getDeployments().getDeployments();
				if (deploymentsForHostedSerice.size() > 0) {
					Deployment deployment = deploymentsForHostedSerice.get(0); // each hosted service will have just one deployment.
					Role role = deployment.getRoleList().getRoles().get(0);
					String hostName = role.getRoleName(); // each deployment will have just one role.
					for (String prefix : prefixes) {
						if (hostName.contains(prefix)) {
							String publicIpFromDeployment = getPublicIpFromDeployment(deployment,prefix);
							LogUtils.log("Found a node with public ip : " + publicIpFromDeployment + " and hostName " + hostName);
							leakingAgentNodesPublicIps.add(publicIpFromDeployment);
						}						
					}
				}
			}


			if (!leakingAgentNodesPublicIps.isEmpty()) {
				for (String ip : leakingAgentNodesPublicIps) {
					LogUtils.log("Attempting to kill node : " + ip);
					long endTime = System.currentTimeMillis() + ESTIMATED_SHUTDOWN_TIME;
					try {
						azureClient.deleteVirtualMachineByIp(ip, false, endTime);
					} catch (final Exception e) {
						LogUtils.log("Failed deleting node with ip : " + ip + ". reason --> " + e.getMessage(), e);
					}
				}
				return false;
			} else {
				return true;
			}
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
		
		
	}

	private String getPublicIpFromDeployment(Deployment deployment, final String prefix) {		
		String publicIp = null;
		Role role = deployment.getRoleList().getRoles().get(0);
		String hostName = role.getRoleName();
		if (hostName.contains(prefix)) {
			ConfigurationSets configurationSets = role.getConfigurationSets();
			for (ConfigurationSet configurationSet : configurationSets) {
				if (configurationSet instanceof NetworkConfigurationSet) {
					NetworkConfigurationSet networkConfigurationSet = (NetworkConfigurationSet) configurationSet;
					publicIp = networkConfigurationSet.getInputEndpoints()
							.getInputEndpoints().get(0).getvIp();
				}
			}
		}
		return publicIp;		
	}

	private void copyCustomCloudConfigurationFileToServiceFolder() throws IOException {

		// copy custom cloud driver configuration to test folder
		String cloudServiceFullPath = this.getPathToCloudFolder();

		File originalCloudDriverConfigFile = new File(cloudServiceFullPath, "azure-cloud.groovy");
		File customCloudDriverConfigFile = new File(SGTestHelper.getSGTestRootDir() + "/src/main/resources/apps/cloudify/cloud/azure", "azure-cloud.groovy");

		Map<File, File> filesToReplace = new HashMap<File, File>();
		filesToReplace.put(originalCloudDriverConfigFile, customCloudDriverConfigFile);

		if (originalCloudDriverConfigFile.exists()) {
			originalCloudDriverConfigFile.delete();
		}
		FileUtils.copyFile(customCloudDriverConfigFile, originalCloudDriverConfigFile);

	}

	private void copyPrivateKeyToUploadFolder() throws IOException {
		File pfxFilePath = new File(SGTestHelper.getSGTestRootDir() + "/src/main/resources/credentials/cloud/azure/azure-cert.pfx");
		File uploadDir = new File(getPathToCloudFolder() + "/upload");
		FileUtils.copyFileToDirectory(pfxFilePath, uploadDir);
	}
}
