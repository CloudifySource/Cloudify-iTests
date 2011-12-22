
cloud {
	name = "ec2"
	configuration {
		className "com.gigaspaces.cloudify.esc.driver.provisioning.jclouds.DefaultCloudProvisioning"
		managementMachineTemplate "SMALL_LINUX_32"
		connectToPrivateIp true
	}

	provider {
		provider "aws-ec2"
		localDirectory "tools/cli/plugins/esc/ec2/upload"
		remoteDirectory "/home/ec2-user/gs-files"
		cloudifyUrl "http://s3.amazonaws.com/gigaspaces-sgtest/gigaspaces.zip"
		machineNamePrefix "gs_esm_gsa_"
		
		dedicatedManagementMachines true
		managementOnlyFiles ([])
		

		sshLoggingLevel "WARNING"
		managementGroup "sgtest_management_machine"
		numberOfManagementMachines 2
		zones (["agent"])
		reservedMemoryCapacityPerMachineInMB 1024
		
	}
	user {
		user "0VCFNJS3FXHYC7M6Y782"
		apiKey "fPdu7rYBF0mtdJs1nmzcdA8yA/3kbV20NgInn4NO"
		keyFile "cloud-demo.pem"
	}
	templates ([
				SMALL_LINUX_32 : template{
					imageId "us-east-1/ami-76f0061f"
					machineMemoryMB 1600
					hardwareId "m1.small"
					locationId "us-east-1"
					options ([
						"securityGroups" : ["default"] as String[],
						"keyPair" : "cloud-demo"
					])
				}
			])
}
