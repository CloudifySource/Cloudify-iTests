
cloud {

	// Mandatory. The name of the cloud, as it will appear in the Cloudify UI.
	name = "Azure"
	configuration {
	
		// Mandatory - Azure IaaS cloud driver.
		className "org.cloudifysource.esc.driver.provisioning.azure.MicrosoftAzureCloudDriver"
		
		// Optional. The template name for the management machines. Defaults to the first template in the templates section below.
		managementMachineTemplate "SMALL_LINUX"
		
		// Optional. Indicates whether internal cluster communications should use the machine private IP. Defaults to true.
		connectToPrivateIp true
	}

	provider {
	
		// Optional 
		provider "azure"
			
		/*************************************************************************************************************************
		 * Optional. The HTTP/S URL where cloudify can be downloaded from by newly started machines. Defaults to downloading the *
		 * cloudify version matching that of the client from the cloudify CDN.													 *
		 * Change this if your compute nodes do not have access to an internet connection, or if you prefer to use a			 *
		 * different HTTP server instead.																						 *
		************************************************ *************************************************************************/
		
		// cloudifyUrl "http://repository.cloudifysource.org/org/cloudifysource/2.2.0/gigaspaces-cloudify-2.2.0-m2-b2492-9.zip"
		
		machineNamePrefix "sgtest_azure_cloudify_agent_"
		
		dedicatedManagementMachines true
		managementOnlyFiles ([])
		
		managementGroup "sgtest_azure_cloudify_manager"
		numberOfManagementMachines 1
		zones (["agent"])
		reservedMemoryCapacityPerMachineInMB 1024
		
		sshLoggingLevel "WARNING"
		
		
	}
	
	user {
		
		// Azure subscription id
		user "3226dcf0-3130-42f3-b68f-a2019c09431e"
			
	}

	templates ([
				SMALL_LINUX : template{
				
					imageId "OpenLogic__OpenLogic-CentOS-62-20120531-en-us-30GB.vhd"
					machineMemoryMB 1600
					hardwareId "Small"
					localDirectory "upload"
					
					username "sgtest"
					password "1408Rokk"
					
					remoteDirectory "/home/sgtest/gs-files"
					
					custom ([
					
						// Optional. each availability set represents a different fault domain.
						
						"azure.availability.set" : "sgtestset1",
						
						// Choose whether do deploy this instance in Staging or Production environment. defaults to Staging
						
						"azure.deployment.slot": "Staging",
						
						/**************************************************************
						 * Mandatory only for templates used for management machines. *
						 * Put this file under the path specified in 'localDirectory' *
						***************************************************************/
						
						"azure.pfx.file": "azure-cert.pfx",
						
						// Password that was used to create the certificate
						
						"azure.pfx.password" : "1408Rokk"
					])
				},
				
				TOMCAT : template{
				
					imageId "OpenLogic__OpenLogic-CentOS-62-20120531-en-us-30GB.vhd"
					machineMemoryMB 1600
					hardwareId "Small"
					localDirectory "upload"
					
					username "sgtest"
					password "1408Rokk"
					
					remoteDirectory "/home/sgtest/gs-files"
					
					custom ([
					
						// Optional. each availability set represents a different fault domain.
						
						"azure.availability.set" : "sgtestset1",
						
						// Choose whether do deploy this instance in Staging or Production environment. defaults to Staging
						
						"azure.deployment.slot": "Staging",
						
						"azure.endpoints" : ([
												([
													"name" : "TOMCAT",
													"port" : "8080",
													"protocol" : "TCP"
												])
											]),
						
						"azure.pfx.file": "azure-cert.pfx",
						
						// Password that was used to create the certificate
						
						"azure.pfx.password" : "1408Rokk"
					])
				}
			])
			
	custom ([
			
		/*****************************************************************************************
		 * A Virtaul Network Site name.																 *
		 * All VM's will belong to this network site. 												 *
		 * If the specified network site does not exist, it will be created automatically for you.	 *
		 * in this case, you must specify the 'azure.address.space' property					 *	 
		******************************************************************************************/
		
		"azure.networksite.name" : "sgtesttestnetwork",
		
		/***************************************************************************************
		 * CIDR notation specifying the Address Space for your Virtaul Network. 			   *
		 * All VM's will be assigned a private ip from this address space.					   *
		****************************************************************************************/
		
		"azure.address.space" : "10.4.0.0/16",
		
		/****************************************************************************************	
		 * An Affinity Group name.																*
		 * if the specified group does not exist, one will be created automatically for you.	*
		 * in this case, you must specify the 'azure.affinity.location' property				*
		*****************************************************************************************/
		
		"azure.affinity.group" : "sgtestcloudifyaffinitygroup",

		/********************************************************************************************************************************
		 * The MS Data Center location. 																								*
		 * All VM's will be launched onto this Data Center. see http://matthew.sorvaag.net/2011/06/windows-azure-data-centre-locations/	*	
		 * Mandatory only if the affinity group specifed above is not a pre-existing one.												*
		*********************************************************************************************************************************/
		
		"azure.affinity.location" : "East US",
		
		/*****************************************************************************************
		 * A Storage Account name.																 *
		 * All OS Disk Images will be stored in this account. 									 *
		 * If the specified account does not exist, it will be created automatically for you.	 *
		******************************************************************************************/

		"azure.storage.account" : "sgtestcloudifystorage",
		
		// Specify whether or not to delete the network (if found) when you execute a teardown command. 
		
		/*************************************************************************************************************************
		 * If set to 'true', the storage account, affinity group, and network specified above will be deleted upon teardown.	 *
		 * NOTE : if you are using pre exsisting services and you dont want them to be deleted, please set this value to 'false' *
		**************************************************************************************************************************/
		
		"azure.cleanup.on.teardown" : "true",

		// Enable/Disable Cloud Requests Logging. 
		
		"azure.wireLog": "true"
	])
}
