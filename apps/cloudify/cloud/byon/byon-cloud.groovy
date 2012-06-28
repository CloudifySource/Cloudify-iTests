/***************
 * Cloud configuration file for the Bring-Your-Own-Node (BYON) cloud.
 * See org.cloudifysource.dsl.cloud.Cloud for more details.
 *
 * @author noak
 *
 */
 
cloud {
	// Mandatory. The name of the cloud, as it will appear in the Cloudify UI.
	name = "byon"
	
	/********
	 * General configuration information about the cloud driver implementation.
	 */
	configuration {
		// The cloud-driver implementation class.
		className "org.cloudifysource.esc.driver.provisioning.byon.ByonProvisioningDriver"
		// Optional. The template name for the management machines. Defaults to the first template in the templates section below.
		managementMachineTemplate "TEMPLATE_3"
		// Optional. Indicates whether internal cluster communications should use the machine private IP. Defaults to true.
		connectToPrivateIp true
		//Indicates whether communications with the management servers should use the machine private IP.
		bootstrapManagementOnPublicIp false
	}

	/*************
	 * Provider specific information.
	 */
	provider {
		// Mandatory. The name of the provider.
		provider "byon"
		
		// Mandatory. The HTTP/S URL where cloudify can be downloaded from by newly started machines.
		cloudifyUrl "cloudify.zip"
		// Mandatory. The prefix for new machines started for servies.
		machineNamePrefix "cloudify_agent_"
		// Optional. Defaults to true. Specifies whether cloudify should try to deploy services on the management machine.
		// Do not change this unless you know EXACTLY what you are doing.
		dedicatedManagementMachines true
		managementOnlyFiles ([])
		
		// Optional. Logging level for the intenal cloud provider logger. Defaults to INFO.
		sshLoggingLevel "INFO"
		// Mandatory. Name of the new machine/s started as cloudify management machines. 
		managementGroup "cloudify_manager"
		// Mandatory. Number of management machines to start on bootstrap-cloud. In production, should be 2. Can be 1 for dev.
		numberOfManagementMachines 1
		zones (["agent"])
		reservedMemoryCapacityPerMachineInMB 1024
	}
	
	/*************
	 * Cloud authentication information
	 */
	user {

	}
	
	/***********
	 * Cloud machine templates available with this cloud. 
	 */
	templates ([
				// Mandatory. Template Name.
				TEMPLATE_1 : template{
				// Mandatory. Amount of RAM available to machine.
				machineMemoryMB 1600
				// Mandatory. Files from the local directory will be copied to this directory on the remote machine.
				remoteDirectory "/tmp/gs-files"
				// Optional. template-generic credentials. Can be overridden by specific credentials on each node, in the nodesList section.
				username "tgrid"
				password "tgrid"
				
				// Mandatory. All files from this LOCAL directory will be copied to the remote machine directory.
				localDirectory "tools/cli/plugins/esc/byon/upload"
				
				// Mandatory for BYON.
					custom ([
						// Mandatory for BYON. The nodesList custom property lists the nodes that compose this cloud-like environment.
						// For each node required:
						// An alias for this node. can be static or use a template with an dynamic-index.
						// The server's private (local) IP. can be a single IP, a list, a range or CIDR.
						//Optional - user and password for the node. can be taken from general cloud configuration.
						"nodesList" : ([
										([
											"id" : "byon-mgmt{0}",
											"host-list" : "1.1.1.1"
										])
						])
					])
				},
				TEMPLATE_2 : template{
				machineMemoryMB 1600
				remoteDirectory "/tmp/gs-files"
				username "tgrid"
				password "tgrid"
				// Mandatory. All files from this LOCAL directory will be copied to the remote machine directory.
				localDirectory "tools/cli/plugins/esc/byon/upload"
			
				// Mandatory for BYON.
					custom ([
						"nodesList" : ([
										([
											"id" : "byon-small{0}",
											"host-list" : "2.2.2.2"
										])
						])
					])
				},
				TEMPLATE_3 : template{
				machineMemoryMB 1600
				remoteDirectory "/tmp/gs-files"
				username "tgrid"
				password "tgrid"
				// Mandatory. All files from this LOCAL directory will be copied to the remote machine directory.
				localDirectory "tools/cli/plugins/esc/byon/upload"
			
				// Mandatory for BYON.
					custom ([
						"nodesList" : ([
										([
											"id" : "byon-big{0}",
											"host-list" : "3.3.3.3"
										])
						])
					])
				}
	])
	
	/*****************
	 * Optional. Custom properties used to extend existing drivers or create new ones. 
	 */
	 // Optional. Sets whether to delete the remoteDirectory created by the cloud driver, when shutting down.
	custom ([
		"cleanGsFilesOnShutdown": "true",
		"itemsToClean": ([
			"/tmp/gs-files/gigaspaces/work",
			"/tmp/gs-files/gigaspaces.zip"
		])
	])

}
