package org.cloudifysource.quality.iTests.framework.utils.storage;

import iTests.framework.utils.AssertUtils;
import iTests.framework.utils.IOUtils;
import iTests.framework.utils.LogUtils;
import iTests.framework.utils.ScriptUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.domain.Service;
import org.cloudifysource.domain.context.blockstorage.LocalStorageOperationException;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.DSLReader;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.dsl.internal.packaging.PackagingException;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.driver.provisioning.storage.StorageProvisioningException;
import org.cloudifysource.esc.driver.provisioning.storage.VolumeDetails;
import org.cloudifysource.quality.iTests.framework.utils.ApplicationInstaller;
import org.cloudifysource.quality.iTests.framework.utils.RecipeInstaller;
import org.cloudifysource.quality.iTests.framework.utils.ServiceInstaller;
import org.cloudifysource.quality.iTests.framework.utils.compute.ComputeApiHelper;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.CloudService;
import org.cloudifysource.restclient.GSRestClient;
import org.cloudifysource.restclient.RestException;

import com.j_spaces.kernel.PlatformVersion;

/**
 *
 * Provides some generic test cases to be used by tests that run on different clouds.
 * User: elip, noak
 */
public class StorageAllocationTester {
	
	private static final long AFTER_ATTACH_TIMEOUT = 10 * 1000;
    private static final String SIMPLE_STORAGE_SERVICE_WITH_CUSTOM_COMMANDS = "simple-storage-with-custom-commands";
    private static final String LOCATION_AWARE_SIMPLE_STORAGE_SERVICE = "multi-az";
    
	private String restUrl;
    private StorageApiHelper storageApiHelper;
    private CloudService cloudService;
    private ComputeApiHelper computeApiHelper;

    public RecipeInstaller getInstaller() {
        return installer;
    }

    private RecipeInstaller installer;

    public StorageAllocationTester(String restUrl, StorageApiHelper storageApiHelper, CloudService cloudService, ComputeApiHelper computeApiHelper) {
        this.restUrl = restUrl;
        this.storageApiHelper = storageApiHelper;
        this.cloudService = cloudService;
        this.computeApiHelper = computeApiHelper;
    }

    private static final String RECIPES_SERVICES_FOLDER = ScriptUtils.getBuildRecipesServicesPath();

    /* Test Methods to be called by test classes */
    public void testStorageVolumeMountedLinux(String computeTemplateName) throws Exception {
    	testStorageVolumeMountedLinux(computeTemplateName, SIMPLE_STORAGE_SERVICE_WITH_CUSTOM_COMMANDS);
    }
    
    public void testStorageVolumeMountedLinux(String computeTemplateName, final String folderName) throws Exception {
        final String servicePath = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/staticstorage/" + folderName);
        String targetFolderName = copyServiceToRecipesFolder(servicePath, folderName);
        setTemplate(RECIPES_SERVICES_FOLDER + "/" + targetFolderName, computeTemplateName, true);
        testStorageVolumeMounted(targetFolderName);
    }

    public void testStorageVolumeMountedUbuntu(String computeTemplateName) throws Exception {
        String folderName = SIMPLE_STORAGE_SERVICE_WITH_CUSTOM_COMMANDS;
        final String servicePath = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/staticstorage/" + folderName);
        folderName = copyServiceToRecipesFolder(servicePath, folderName);
        setTemplate(RECIPES_SERVICES_FOLDER + "/" + folderName, computeTemplateName, false);
        testStorageVolumeMounted(folderName);
    }

    public void testWriteToStorageLinux(String computeTemplateName) throws Exception {
        String folderName = SIMPLE_STORAGE_SERVICE_WITH_CUSTOM_COMMANDS;
        final String servicePath = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/staticstorage/" + folderName);
        folderName = copyServiceToRecipesFolder(servicePath, folderName);
        setTemplate(RECIPES_SERVICES_FOLDER + "/" + folderName, computeTemplateName, true);
        testWriteToStorage(folderName);
    }

    public void testWriteToStorageUbuntu(String computeTemplateName) throws Exception {
        String folderName = SIMPLE_STORAGE_SERVICE_WITH_CUSTOM_COMMANDS;
        final String servicePath = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/staticstorage/" + folderName);
        folderName = copyServiceToRecipesFolder(servicePath, folderName);
        setTemplate(RECIPES_SERVICES_FOLDER + "/" + folderName, computeTemplateName, false);
        testWriteToStorage(folderName);
    }

    public void testMountLinux(String computeTemplateName) throws Exception {
        String folderName = SIMPLE_STORAGE_SERVICE_WITH_CUSTOM_COMMANDS;
        final String servicePath = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/staticstorage/" + folderName);
        folderName = copyServiceToRecipesFolder(servicePath, folderName);
        setTemplate(RECIPES_SERVICES_FOLDER + "/" + folderName, computeTemplateName, true);
        testMount(folderName);
    }

    public void testMountUbuntu(String computeTemplateName) throws Exception {
        String folderName = SIMPLE_STORAGE_SERVICE_WITH_CUSTOM_COMMANDS;
        final String servicePath = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/staticstorage/" + folderName);
        folderName = copyServiceToRecipesFolder(servicePath, folderName);
        setTemplate(RECIPES_SERVICES_FOLDER + "/" + folderName, computeTemplateName, false);
        testMount(folderName);
    }

    public void testInstallWithStorageLinux(String computeTemplateName) throws Exception {
    	testInstallWithStorageLinux(computeTemplateName, true);
    }
    
    public void testInstallWithStorageLinux(String computeTemplateName, final boolean deleteOnExit) throws Exception {
        String folderName = "simple-storage";
        final String servicePath = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/staticstorage/" + folderName);
        folderName = copyServiceToRecipesFolder(servicePath, folderName);
        setTemplate(RECIPES_SERVICES_FOLDER + "/" + folderName, computeTemplateName, true);
        testInstallWithStorage(folderName, deleteOnExit);
    }

    public void testInstallWithStorageUbuntu(String computeTemplateName) throws Exception {
        String folderName = "simple-storage";
        final String servicePath = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/staticstorage/" + folderName);
        folderName = copyServiceToRecipesFolder(servicePath, folderName);
        setTemplate(RECIPES_SERVICES_FOLDER + "/" + folderName, computeTemplateName, false);
        testInstallWithStorage(folderName);
    }

    public void testInstallWithDynamicStorageLinux(String computeTemplateName) throws Exception {
        String folderName = "create-and-attach";
        final String servicePath = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/dynamicstorage/" + folderName);
        folderName = copyServiceToRecipesFolder(servicePath, folderName);
        setTemplate(RECIPES_SERVICES_FOLDER + "/" + folderName, computeTemplateName, true);
        testInstallWithStorage(folderName);
    }

    public void testInstallWithDynamicStorageLinux(String computeTemplateName, String servicePath, String folderName) 
    		throws Exception {
        folderName = copyServiceToRecipesFolder(servicePath, folderName);
        setTemplate(RECIPES_SERVICES_FOLDER + "/" + folderName, computeTemplateName, true);
        testInstallWithStorage(folderName);
    }

    public void testInstallWithDynamicStorageUbuntu(String computeTemplateName) throws Exception {
        String folderName = "create-and-attach";
        final String servicePath = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/dynamicstorage/" + folderName);
        folderName = copyServiceToRecipesFolder(servicePath, folderName);
        setTemplate(RECIPES_SERVICES_FOLDER + "/" + folderName, computeTemplateName, false);
        testInstallWithStorage(folderName);
    }

    public void testDeleteOnExitFalseLinux(String computeTemplateName) throws Exception {
        String folderName = "simple-storage";
        final String servicePath = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/staticstorage/" + folderName);
        folderName = copyServiceToRecipesFolder(servicePath, folderName);
        setTemplate(RECIPES_SERVICES_FOLDER + "/" + folderName, computeTemplateName, true);
        testDeleteOnExitFalse(folderName);
    }
    
    public void testDeleteOnExitTrueLinux(String computeTemplateName) throws Exception {
        String folderName = "simple-storage";
        final String servicePath = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/staticstorage/" + folderName);
        folderName = copyServiceToRecipesFolder(servicePath, folderName);
        setTemplate(RECIPES_SERVICES_FOLDER + "/" + folderName, computeTemplateName, true);
        testDeleteOnExitTrue(folderName);
    }
    
    public void testDeleteOnExitFalseLinuxGlobalSla(String computeTemplateName) throws Exception {
        String folderName = "simple-storage-global-sla";
        final String servicePath = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/staticstorage/" + folderName);
        folderName = copyServiceToRecipesFolder(servicePath, folderName);
        setTemplate(RECIPES_SERVICES_FOLDER + "/" + folderName, computeTemplateName, true);
        testDeleteOnExitFalse(folderName);
    }

    public void testDeleteOnExitFalseUbuntu(String computeTemplateName) throws Exception {
        String folderName = "simple-storage";
        final String servicePath = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/staticstorage/" + folderName);
        folderName = copyServiceToRecipesFolder(servicePath, folderName);
        setTemplate(RECIPES_SERVICES_FOLDER + "/" + folderName, computeTemplateName, false);
        testDeleteOnExitFalse(folderName);
    }

    public void testFaultyInstallLinux(String computeTemplateName) throws Exception {
        String folderName = "faulty-install";
        final String servicePath = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/staticstorage/" + folderName);
        folderName = copyServiceToRecipesFolder(servicePath, folderName);
        setTemplate(RECIPES_SERVICES_FOLDER + "/" + folderName, computeTemplateName, true);
        testFaultyInstall(folderName);
    }
    
    public void testFaultyInstallLinux(String computeTemplateName, final String folderName) throws Exception {
        final String servicePath = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/staticstorage/" + folderName);
        String targetFolderName = copyServiceToRecipesFolder(servicePath, folderName);
        setTemplate(RECIPES_SERVICES_FOLDER + "/" + targetFolderName, computeTemplateName, true);
        testFaultyInstall(targetFolderName);
    }
    

    public void testFaultyInstallUbuntu(String computeTemplateName) throws Exception {
        String folderName = "faulty-install";
        final String servicePath = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/staticstorage/" + folderName);
        folderName = copyServiceToRecipesFolder(servicePath, folderName);
        setTemplate(RECIPES_SERVICES_FOLDER + "/" + folderName, computeTemplateName, false);
        testFaultyInstall(folderName);
    }

    public void testFailedToAttachLinux(String computeTemplateName) throws Exception {
        String folderName = "simple-storage";
        final String servicePath = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/staticstorage/" + folderName);
        folderName = copyServiceToRecipesFolder(servicePath, folderName);
        setTemplate(RECIPES_SERVICES_FOLDER + "/" + folderName, computeTemplateName, true);
        testFailedToAttach(folderName);
    }

    public void testFailedToAttachUbuntu(String computeTemplateName) throws Exception {
        final String folderName = "simple-storage";
        final String servicePath = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/staticstorage/" + folderName);
        copyServiceToRecipesFolder(servicePath, folderName);
        setTemplate(RECIPES_SERVICES_FOLDER + "/" + folderName, computeTemplateName, false);
        testFailedToAttach(folderName);
    }

    public void testTwoTemplates() throws Exception {

        installer = new ApplicationInstaller(restUrl, "groovy-App");
        installer.recipePath(CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/staticstorage/groovyApp"));
        installer.install();

        final String groovy1VolumePrefix = cloudService.getCloud().getCloudStorage().getTemplates().get("GROOVY1").getNamePrefix();
        LogUtils.log("Volume prefix for groovy1 is " + groovy1VolumePrefix);
        final String groovy2VolumePrefix = cloudService.getCloud().getCloudStorage().getTemplates().get("GROOVY2").getNamePrefix();
        LogUtils.log("Volume prefix for groovy2 is " + groovy2VolumePrefix);

        LogUtils.log("Retrieving volumes for groovy1");
        Set<VolumeDetails> groovy1Volumes = storageApiHelper.getVolumesByPrefix(groovy1VolumePrefix);
        AssertUtils.assertEquals("Wrong number of volumes detected after installation for service groovy1", 1, groovy1Volumes.size());

        LogUtils.log("Retrieving volumes for groovy2");
        Set<VolumeDetails> groovy2Volumes = storageApiHelper.getVolumesByPrefix(groovy2VolumePrefix);
        AssertUtils.assertEquals("Wrong number of volumes detected after installation for service groovy2", 1, groovy2Volumes.size());

        AssertUtils.assertTrue("Both volumes should not be attached to different same instance",
                !storageApiHelper.getVolumeAttachments(groovy1Volumes.iterator().next().getId())
                        .equals(storageApiHelper.getVolumeAttachments(groovy2Volumes.iterator().next().getId())));

        installer.uninstall();
    }

    public void testMultitenantStorageAllocation() throws Exception {

        installer = new ApplicationInstaller(restUrl, "groovy-App-multitenant");
        installer.recipePath(CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/staticstorage/groovyApp-multitenant"));
        installer.install();

        final String groovy1VolumePrefix = cloudService.getCloud().getCloudStorage().getTemplates().get("GROOVY1").getNamePrefix();
        LogUtils.log("Volume prefix for groovy1 is " + groovy1VolumePrefix);
        final String groovy2VolumePrefix = cloudService.getCloud().getCloudStorage().getTemplates().get("GROOVY2").getNamePrefix();
        LogUtils.log("Volume prefix for groovy2 is " + groovy2VolumePrefix);

        LogUtils.log("Retrieving volumes for groovy1");
        Set<VolumeDetails> groovy1Volumes = storageApiHelper.getVolumesByPrefix(groovy1VolumePrefix);
        AssertUtils.assertEquals("Wrong number of volumes detected after installation for service groovy1", 1, groovy1Volumes.size());
        LogUtils.log("Found volume : " + groovy1Volumes.iterator().next());

        LogUtils.log("Retrieving volumes for groovy2");
        Set<VolumeDetails> groovy2Volumes = storageApiHelper.getVolumesByPrefix(groovy2VolumePrefix);
        AssertUtils.assertEquals("Wrong number of volumes detected after installation for service groovy2", 1, groovy2Volumes.size());
        LogUtils.log("Found volume : " + groovy2Volumes.iterator().next());

        AssertUtils.assertEquals("Both volumes should be attached to different same instance",
                storageApiHelper.getVolumeAttachments(groovy1Volumes.iterator().next().getId()),
                storageApiHelper.getVolumeAttachments(groovy2Volumes.iterator().next().getId()));

        installer.uninstall();
    }

    public void testConcurrentAllocation() throws Exception {

        String folderName = "concurrent";
        folderName = copyServiceToRecipesFolder(CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/dynamicstorage/" + folderName), folderName);
        String serviceName = ServiceReader.readService(new File(ScriptUtils.getBuildRecipesServicesPath() + "/" + folderName)).getName();

        installer = new ServiceInstaller(restUrl, serviceName);
        installer.recipePath(folderName);
        installer.setDisableSelfHealing(true);
        installer.install();

        LogUtils.log("Retrieving all volumes with prefix " + getVolumePrefixForTemplate("INSTANCE_1"));
        Set<VolumeDetails> volumesByName = storageApiHelper.getVolumesByPrefix(getVolumePrefixForTemplate("INSTANCE_1"));
        LogUtils.log("Volumes are : " + volumesByName);

        // two volumes should have been created since the service has two instances, each creating a volume.
        AssertUtils.assertEquals("Wrong number of volumes created", 2, volumesByName.size());

        LogUtils.log("Collecting attachments of newly created");
        Set<String> attachmentIps = new HashSet<String>();

        for (VolumeDetails vol : volumesByName) {
            // the install should have created and attached a volume with a name prefix of the class name. see customizeCloud below.
            AssertUtils.assertNotNull("could not find the required volume after install service", vol);
            // also check it is attached.
            Set<String> volumeAttachments = storageApiHelper.getVolumeAttachments(vol.getId());
            AssertUtils.assertEquals("the volume should have one attachements", 1, volumeAttachments.size());
            attachmentIps.add(volumeAttachments.iterator().next());
        }
        LogUtils.log("Attachments are " + attachmentIps);

        // the volumes should be attached to different instances
        AssertUtils.assertEquals("the volumes are not attached to two different instances", 2, attachmentIps.size());

        installer.uninstall();

        // after uninstall the volumes should be deleted
        for (VolumeDetails vol : volumesByName) {
        	LogUtils.log("Found volume: " + vol);
        	
            VolumeDetails currentVol = storageApiHelper.getVolumeById(vol.getId());
            if (currentVol != null) {
            	LogUtils.log("Volume status: " + storageApiHelper.getVolumeStatus(currentVol.getId()));
            }
            AssertUtils.assertTrue("volume with id " + vol.getId() + " was not deleted after uninstall", currentVol == null || storageApiHelper.isVolumeDeleting(currentVol.getId()));
        }

    }

    /**
     * Installs a service that also uses a static storage.
     * Verifies the a volume was created and attached to a compute instance.
     * Performs fail-over - the instance is killed and the test waits for a new one instance to be created, for the 
     * service to be installed and for the volume to be created and attached.
     * Verifies the volume is indeed created and attached accordingly.
     * Eventually uninstalls the service.
     * @throws Exception .
     */
    public void testFailover() throws Exception {

        final String serviceName = installServiceWithStaticStorage();

        LogUtils.log("Searching for volumes created by the service installation");
        // the install should have created and attached a volume with a name prefix of the class name. see customizeCloud below.
        VolumeDetails ourVolume = storageApiHelper.getVolumesByPrefix(getVolumePrefixForTemplate("SMALL_BLOCK")).iterator().next();

        AssertUtils.assertNotNull("could not find the required volume after install service", ourVolume);
        LogUtils.log("Found volume : " + ourVolume);
        // also check it is attached.
        AssertUtils.assertEquals("the volume should have one attachments", 1, storageApiHelper.getVolumeAttachments(ourVolume.getId()).size());

        Set<String> volumeAttachments = storageApiHelper.getVolumeAttachments(ourVolume.getId());
        String instanceId = volumeAttachments.iterator().next();

        // triggering fail-over and waiting for a complete service recovery
        performFailover(instanceId, serviceName);
        
        LogUtils.log("Deleting the original volume created by the service installation");
        storageApiHelper.deleteVolume(ourVolume.getId());

        LogUtils.log("Searching for volumes created by the service failover healing");
        // the install should have created and attached a volume with a name prefix of the class name. see customizeCloud below.
        Set<VolumeDetails> allVolumes = storageApiHelper.getVolumesByPrefix(getVolumePrefixForTemplate("SMALL_BLOCK"));
        VolumeDetails newVolume = allVolumes.iterator().next();

        AssertUtils.assertNotNull("could not find the required volume after service failover healing", newVolume);
        LogUtils.log("Found volume : " + newVolume);
        // also check it is attached.
        AssertUtils.assertEquals("the volume should have one attachments", 1, storageApiHelper.getVolumeAttachments(newVolume.getId()).size());

        installer.uninstall();
    }
	
    
    /**
     * Installs a service that also uses a static storage, and deployed on multiple availability zones.
     * Verifies the a volume was created and attached to a compute instance.
     * The original volume and instance location is noted, and later compared to the location of the new
     * Performs fail-over - the instance is killed and the test waits for a new one instance to be created, for the 
     * service to be installed and for the volume to be created and attached.
     * Verifies the volume is indeed created and attached accordingly, in the original zone.
     * Eventually uninstalls the service.
     * 
     * @throws Exception .
     */
	 public void testFailoverLocationAware() throws Exception {

		final String serviceName = installLocationAwareServiceWithStaticStorage();
		final String storageTemplateName = getVolumePrefixForTemplate("SMALL_BLOCK");
		
		testFailoverLocationAware(serviceName, storageTemplateName);
		
		installer.uninstall();		
	 }
	 
	 
    /**
     * Uses a pre-installed service that also uses a static storage, and deployed on multiple availability zones.
     * Verifies the a volume was created and attached to a compute instance.
     * The original volume and instance location is noted, and later compared to the location of the new
     * Performs fail-over - the instance is killed and the test waits for a new one instance to be created, for the 
     * service to be installed and for the volume to be created and attached.
     * Verifies the volume is indeed created and attached accordingly, in the original zone.
     * 
     * @throws Exception .
     */
	 public void testFailoverLocationAware(final String serviceName, final String storageTemplateName) throws Exception {
		 
		 // get all volumes with the prefix of this service
        LogUtils.log("Searching for volumes created by the service installation");
        final String volumePrefix = getVolumePrefixForTemplate(storageTemplateName);
        LogUtils.log("Searching volumes with prefix: " + volumePrefix);
        Set<VolumeDetails> originalVolumeSet = storageApiHelper.getVolumesByPrefix(volumePrefix);
        AssertUtils.assertTrue("Failed to find volumes with prefix: " + volumePrefix, originalVolumeSet != null 
        		&& !originalVolumeSet.isEmpty());
        
        // take the first volume in the set (doesn't matter which) and note the volume's location
        VolumeDetails originalVolume = originalVolumeSet.iterator().next();
        String volumeId = originalVolume.getId();
        AssertUtils.assertNotNull("could not find the required volume after install service", originalVolume);
        LogUtils.log("Found volume : " + originalVolume);
        String originalVolumeLocation = originalVolume.getLocation();
        LogUtils.log("Original volume location: " + originalVolumeLocation);
        
        
        // check it is attached to an instance, in the same location
        Set<String> originalAttachedInstances = storageApiHelper.getVolumeAttachments(volumeId);
        AssertUtils.assertEquals("the volume should have one attachments, but has: " + originalAttachedInstances.size(), 1, 
        		originalAttachedInstances.size());
        String instanceId = originalAttachedInstances.iterator().next();
        MachineDetails instance = computeApiHelper.getServerById(instanceId);        
        AssertUtils.assertNotNull("Failed to find the instanc attached to volume " + volumeId 
        		+ ", expected instance id: " + instanceId, instance);        		
        String originalInstanceLocation = instance.getLocationId();
        LogUtils.log("Original instance location: " + originalInstanceLocation);

        // ******************************************************************************
        // ********************** this is the important part! ***************************
        // ****** triggering fail-over and waiting for a complete service recovery ******
        // ******************************************************************************
        performFailover(instanceId, serviceName);
        
        // delete the volume after fail over
        // TODO noak: remove that after we support volume clean-up/re-attachment following failover
        LogUtils.log("Deleting the original volume created by the service installation");
        storageApiHelper.deleteVolume(originalVolume.getId());

        // the install should have created and attached a single volume with the same prefix and in the same location
        LogUtils.log("Searching for volumes created by the service failover healing");
        Set<VolumeDetails> newVolumeSet = storageApiHelper.getVolumesByPrefix(getVolumePrefixForTemplate(storageTemplateName));
        Set<VolumeDetails> createdVolumes = getNewlyCreatedVolumes(originalVolumeSet, newVolumeSet);
        AssertUtils.assertTrue("A new volume was not created after fail-over", createdVolumes != null 
        		&& !createdVolumes.isEmpty());
        AssertUtils.assertTrue("Too many volumes created after fail-over: " + createdVolumes, createdVolumes.size() == 1);
        VolumeDetails newVolume = createdVolumes.iterator().next();
        LogUtils.log("Found volume : " + newVolume);

        // verify the new volume is in the same AZ as the original volume
        String newVolumeLocation = newVolume.getLocation();
        AssertUtils.assertTrue("the original volume location was " + originalVolumeLocation + " but the new volume" 
        		+ " is located on " + newVolumeLocation, originalVolumeLocation.equalsIgnoreCase(newVolumeLocation));

        // check it is attached to an instance, in the same location
        Set<String> newAttachedInstances = storageApiHelper.getVolumeAttachments(newVolume.getId());
        AssertUtils.assertEquals("the volume should have one attachments", 1, newAttachedInstances.size());
        String newInstanceId = newAttachedInstances.iterator().next();
        // verify the new instance is found (not null)
        MachineDetails newInstance = computeApiHelper.getServerById(newInstanceId);
        AssertUtils.assertNotNull("could not find the new instance after service failover healing, instance id: " 
        		+ newInstanceId, newInstance);
        LogUtils.log("After fail over and healing, found new instance with id: " + newInstanceId);
        
        // verify the new instance is in the same AZ as the original instance
        String newInstanceLocation = newInstance.getLocationId();
        AssertUtils.assertTrue("the original instance location was " + originalInstanceLocation + " but the new " 
        		+ " instance is located on " + newInstanceLocation, originalInstanceLocation.equalsIgnoreCase(newInstanceLocation));
    }
	 
    
	private String installServiceWithStaticStorage() throws IOException, DSLException, PackagingException, Exception,
			InterruptedException {

        final String servicePath = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/staticstorage/" + SIMPLE_STORAGE_SERVICE_WITH_CUSTOM_COMMANDS);
        String folderName = copyServiceToRecipesFolder(servicePath, SIMPLE_STORAGE_SERVICE_WITH_CUSTOM_COMMANDS);
        setTemplate(RECIPES_SERVICES_FOLDER + "/" + folderName, "SMALL_LINUX", false);

        final String serviceName = ServiceReader.readService(new File(ScriptUtils.getBuildRecipesServicesPath() + "/" + folderName)).getName();

        installer = new ServiceInstaller(restUrl, serviceName);
        installer.recipePath(folderName);
        installer.setDisableSelfHealing(true);
        installer.install();
		return serviceName;
	}
	
	
	private String installLocationAwareServiceWithStaticStorage() throws IOException, DSLException, PackagingException, Exception,
			InterruptedException {

		final String servicePath = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/staticstorage/" + LOCATION_AWARE_SIMPLE_STORAGE_SERVICE);
		String folderName = copyServiceToRecipesFolder(servicePath, LOCATION_AWARE_SIMPLE_STORAGE_SERVICE);
		setTemplate(RECIPES_SERVICES_FOLDER + "/" + folderName, "SMALL_LINUX", false);
		
		final String serviceName = ServiceReader.readService(new File(ScriptUtils.getBuildRecipesServicesPath() + "/" + folderName)).getName();
		
		installer = new ServiceInstaller(restUrl, serviceName);
		installer.recipePath(folderName);
		installer.setDisableSelfHealing(true);
		installer.install();
		return serviceName;
	}
    

    /**
     * triggering fail-over and waiting for a complete service recovery.
     * 
     * @param instanceId The id of the instance to terminate
     * @param serviceName The name of the service the instance is associated with
     * @throws Exception
     */
	private void performFailover(final String instanceId, final String serviceName)
			throws Exception {
		
		final GSRestClient client = new GSRestClient("", "", new URL(restUrl), PlatformVersion.getVersionNumber());
		
		final String serviceRestUrl = "ProcessingUnits/Names/default." + serviceName;
        final int originalNumberOfInstances = (Integer) client.getAdminData(serviceRestUrl).get("Instances-Size");
        LogUtils.log("Original number of " + serviceName + " instances is " + originalNumberOfInstances + " (before failover)");
        
		LogUtils.log("Shutting down agent");
        computeApiHelper.shutdownServerById(instanceId);

        LogUtils.log("Waiting for service " + serviceName + " to terminate a machine");
		
		AssertUtils.repetitiveAssertTrue("Service " + serviceName + " didn't break", new AssertUtils.RepetitiveConditionProvider() {
            @Override
            public boolean getCondition() {
            	boolean done = false;
	            try {
	                int newNumberOfInstances = (Integer) client.getAdminData(serviceRestUrl).get("Instances-Size");
	                LogUtils.log("Number of " + serviceName + " instances is " + newNumberOfInstances);
	                if (newNumberOfInstances < originalNumberOfInstances) {
	                    LogUtils.log(serviceName + " service broke. it now has only " + newNumberOfInstances + " instance(s)");
	                    done = true;
	                }
	                return done;
	            } catch (RestException e) {
	                throw new RuntimeException(e);
	            }
            }
        } , AbstractTestSupport.OPERATION_TIMEOUT * 4);


        LogUtils.log("Waiting for service to recover");
        AssertUtils.repetitiveAssertTrue(serviceName + " service did not recover", new AssertUtils.RepetitiveConditionProvider() {
            @Override
            public boolean getCondition() {
            	boolean done = false;
                final String brokenServiceRestUrl = "ProcessingUnits/Names/default." + serviceName;
                try {
                    int newNumberOfInstances = (Integer) client.getAdminData(brokenServiceRestUrl).get("Instances-Size");
                    LogUtils.log("Number of " + serviceName + " instances is " + newNumberOfInstances);
                    if (newNumberOfInstances == originalNumberOfInstances) {
                    	LogUtils.log(serviceName + " recovered. it now has " + newNumberOfInstances + " instance(s)");
                    	done = true;
                    }
                    return done;
                } catch (RestException e) {
                    throw new RuntimeException("caught a RestException", e);
                }
            }
        } , AbstractTestSupport.OPERATION_TIMEOUT * 3);

        LogUtils.log("Waiting for USM_State to be " + CloudifyConstants.USMState.RUNNING);
        AssertUtils.repetitiveAssertTrue(serviceName + " service did not reach USM_State of RUNNING", new AssertUtils.RepetitiveConditionProvider() {
            @Override
            public boolean getCondition() {
                final String brokenServiceRestUrl = "ProcessingUnits/Names/default." + serviceName;
                try {
                    String usmState = (String) client.getAdminData(brokenServiceRestUrl + "/Instances/0/Statistics/Monitors/USM/Monitors/USM_State").get("USM_State");
                    LogUtils.log("USMState is " + usmState);
                    return (Integer.valueOf(usmState) == CloudifyConstants.USMState.RUNNING.ordinal());
                } catch (RestException e) {
                	LogUtils.log("Failed retrieving usm state : " + e.getMessage(), e);
                	return false;
                }
            }
        } , AbstractTestSupport.OPERATION_TIMEOUT * 3);
        
        // the USM state might become "Running" too fast, even before volume recovery completed
        // sleep to allow volume recovery to complete
        // see CLOUDIFY-1551
        Thread.sleep(AFTER_ATTACH_TIMEOUT);
	}
	
	
    private String getVolumePrefixForTemplate(final String template) {
        return cloudService.getCloud().getCloudStorage().getTemplates().get(template).getNamePrefix();
    }
    

    public void testDynamicStorageAttachmentLinux() throws Exception {
        String folderName = "attach-only";
        final String servicePath = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/dynamicstorage/" + folderName);
        folderName = copyServiceToRecipesFolder(servicePath, folderName);
        setTemplate(RECIPES_SERVICES_FOLDER + "/" + folderName, "SMALL_LINUX", true);
        testDynamicStorageAttachment(folderName);
    }

    public void testDynamicStorageAttachmentUbuntu() throws Exception {
        String folderName = "attach-only";
        final String servicePath = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/dynamicstorage/" + folderName);
        folderName = copyServiceToRecipesFolder(servicePath, folderName);
        setTemplate(RECIPES_SERVICES_FOLDER + "/" + folderName, "SMALL_UBUNTU", false);
        testDynamicStorageAttachment(folderName);
    }

    public void testNonPrivileged() throws Exception {

        String folderName = "non-sudo";
        folderName = copyServiceToRecipesFolder(CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/dynamicstorage/" + folderName), folderName);
        setTemplate(RECIPES_SERVICES_FOLDER + "/" + folderName, "SMALL_LINUX", true);
        String serviceName = ServiceReader.readService(new File(ScriptUtils.getBuildRecipesServicesPath() + "/" + folderName)).getName();

        final String expectedOutput = "Cannot format when not running in privileged mode";

        installer = new ServiceInstaller(restUrl, serviceName);
        installer.recipePath(folderName);
        installer.timeoutInMinutes(10);
        installer.setDisableSelfHealing(true);
        installer.expectToFail(true);
        String installOutput = installer.install();

        // the installation should not succeed because the user is not sudo
        // see src/main/resources/apps/USM/usm/dynamicstroage/non-sudo/groovy.service
        // so we expect the IllegalStateException to propagate to the CLI.
        AssertUtils.assertTrue("installation output should have contained '" + expectedOutput + "'", installOutput.toLowerCase().contains(expectedOutput.toLowerCase()));

        installer.uninstall();

    }

    public void testSmallCreateVolumeTimeout() throws Exception {

        String folderName = "small-create-volume-timeout";
        folderName = copyServiceToRecipesFolder(CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/dynamicstorage/" + folderName), folderName);
        setTemplate(RECIPES_SERVICES_FOLDER + "/" + folderName, "SMALL_LINUX", true);
        String serviceName = ServiceReader.readService(new File(ScriptUtils.getBuildRecipesServicesPath() + "/" + folderName)).getName();

        installer = new ServiceInstaller(restUrl, serviceName);
        installer.recipePath(folderName);
        installer.setDisableSelfHealing(true);
        installer.expectToFail(true);
        installer.install();
    }

    public void testSmallFormatTimeoutLinux() throws Exception {
        String folderName = "small-format-timeout";
        final String servicePath = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/dynamicstorage/" + folderName);
        folderName = copyServiceToRecipesFolder(servicePath, folderName);
        setTemplate(RECIPES_SERVICES_FOLDER + "/" + folderName, "SMALL_LINUX", true);
        testSmallFormatTimeout(folderName);
    }

    public void testSmallFormatTimeoutUbuntu() throws Exception {
        String folderName = "small-format-timeout";
        final String servicePath = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/dynamicstorage/" + folderName);
        folderName = copyServiceToRecipesFolder(servicePath, folderName);
        setTemplate(RECIPES_SERVICES_FOLDER + "/" + folderName, "SMALL_UBUNTU", false);
        testSmallFormatTimeout(folderName);
    }

    public void testUnsupportedFileSystemLinux() throws Exception {
        String folderName = "unsupported-fs";
        final String servicePath = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/dynamicstorage/" + folderName);
        folderName = copyServiceToRecipesFolder(servicePath, folderName);
        setTemplate(RECIPES_SERVICES_FOLDER + "/" + folderName, "SMALL_LINUX", true);
        testUnsupportedFileSystem(folderName);
    }

    public void testUnsupportedFileSystemUbuntu() throws Exception {
        String folderName = "unsupported-fs";
        final String servicePath = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/dynamicstorage/" + folderName);
        folderName = copyServiceToRecipesFolder(servicePath, folderName);
        setTemplate(RECIPES_SERVICES_FOLDER + "/" + folderName, "SMALL_UBUNTU", false);
        testUnsupportedFileSystem(folderName);
    }

    /* Private logical tests methods */

    private void testUnsupportedFileSystem(final String folderName) throws Exception {

        String serviceName = ServiceReader.readService(new File(ScriptUtils.getBuildRecipesServicesPath() + "/" + folderName)).getName();

        final String expectedException = LocalStorageOperationException.class.getSimpleName();

        installer = new ServiceInstaller(restUrl, serviceName);
        installer.recipePath(folderName);
        installer.timeoutInMinutes(10);
        installer.setDisableSelfHealing(true);
        installer.expectToFail(true);
        String installOutput = installer.install();

        // this installs a service that tries to mount a device onto a non supported file system(foo)
        // see src/main/resources/apps/USM/usm/dynamicstroage/unsupported-fs/groovy.service
        // so we except the LocalStorageOperationException to propagate to the CLI.

        AssertUtils.assertTrue("install output should have contained " + expectedException, installOutput.contains(expectedException));

        installer.uninstall();

    }

    private void testSmallFormatTimeout(final String folderName) throws Exception {

        String serviceName = ServiceReader.readService(new File(ScriptUtils.getBuildRecipesServicesPath() + "/" + folderName)).getName();

        installer = new ServiceInstaller(restUrl, serviceName);
        installer.recipePath(folderName);
        installer.timeoutInMinutes(10);
        installer.setDisableSelfHealing(true);
        installer.expectToFail(true);
        String installOutput = installer.install();

        // the installation should not succeed because the format timeout is extremely small (5 millis)
        // see src/main/resources/apps/USM/usm/dynamicstroage/small-format-time/groovy.service
        // so we expect the TimeoutException to propagate to the CLI.
        AssertUtils.assertTrue("installation output should have contained a TimeoutException", installOutput.contains("TimeoutException"));

        installer.uninstall();

    }

    private void testDynamicStorageAttachment(final String folderName) throws Exception {

        Service service = ServiceReader.readService(new File(ScriptUtils.getBuildRecipesServicesPath() + "/" + folderName));
        String serviceName = service.getName();

        installer = new ServiceInstaller(restUrl, serviceName);
        installer.recipePath(folderName);
        installer.setDisableSelfHealing(true);
        installer.install();

        String machinePrefix;
        String templateName;

        LogUtils.log("Retrieving machine prefix for the installed service");
        if (service.getIsolationSLA().getGlobal().isUseManagement()) {
            machinePrefix = cloudService.getCloud().getProvider().getManagementGroup();
            templateName = cloudService.getCloud().getConfiguration().getManagementMachineTemplate();
        } else {
            machinePrefix = cloudService.getCloud().getProvider().getMachineNamePrefix();
            templateName = service.getCompute().getTemplate();
        }
        LogUtils.log("Machine prefix is " + machinePrefix);

        String locationId = cloudService.getCloud().getCloudCompute().getTemplates().get(templateName).getLocationId();
        LogUtils.log("Creating volume in location " + locationId);
        VolumeDetails details = storageApiHelper.createVolume("SMALL_BLOCK", locationId);
        LogUtils.log("Volume created : " + details);

        LogUtils.log("Attaching volume with id " + details.getId() + " to service");
        ((ServiceInstaller)installer).invoke("attachVolume " + details.getId() + " " + "/dev/xvdc");

        LogUtils.log("Checking volume with id " + details.getId() + " is really attached");
        VolumeDetails volume = storageApiHelper.getVolumeById(details.getId());
        Set<String> volumeAttachments = storageApiHelper.getVolumeAttachments(volume.getId());
        AssertUtils.assertEquals("volume with id " + volume.getId() + " should have one attachement after invoking attachVolume", 1, volumeAttachments.size());
        LogUtils.log("Volume attachment is " + volumeAttachments);

        LogUtils.log("Detaching volume with id " + details.getId() + " from service");
        ((ServiceInstaller)installer).invoke("detachVolume" + " " + details.getId());

        LogUtils.log("Checking volume with id " + details.getId() + " is really detached");
        volume = storageApiHelper.getVolumeById(details.getId());
        volumeAttachments = storageApiHelper.getVolumeAttachments(volume.getId());
        // volume should still exist
        AssertUtils.assertTrue("volume with id " + details.getId() + " should not have been deleted after calling detachVolume(delteOnExit = false)", volume != null);
        // though it should have no attachments
        AssertUtils.assertTrue("volume with id " + details.getId() + " should not have no attachments after calling detachVolume(delteOnExit = false)",
                volumeAttachments == null || volumeAttachments.isEmpty());

        LogUtils.log("Deleting volume with id " + details.getId());
        storageApiHelper.deleteVolume(volume.getId());

        installer.uninstall();

    }

    private void testFailedToAttach(final String folderName) throws Exception {

        Service service = ServiceReader.readService(new File(ScriptUtils.getBuildRecipesServicesPath() + "/" + folderName));
        String serviceName = service.getName();

        installer = new ServiceInstaller(restUrl, serviceName);
        installer.recipePath(folderName);
        installer.timeoutInMinutes(5);
        installer.expectToFail(true);
        installer.setDisableSelfHealing(true); // TODO - remove this once we handle CLOUDIFY-1670
        installer.install();

        installer.expectToFail(false);

        waitForPendingVolumes(service);

        installer.uninstall();
    }

    private void testFaultyInstall(final String folderName) throws Exception {

        Service service = ServiceReader.readService(new File(ScriptUtils.getBuildRecipesServicesPath() + "/" + folderName));
        String serviceName = service.getName();

        installer = new ServiceInstaller(restUrl, serviceName);
        installer.recipePath(folderName);
        installer.timeoutInMinutes(5);
        installer.expectToFail(true);
        installer.setDisableSelfHealing(true); // TODO - remove this once we handle CLOUDIFY-1670
        installer.install();

        installer.expectToFail(false);

        waitForPendingVolumes(service);

        installer.uninstall();
    }

    private void testWriteToStorage(final String folderName) throws Exception {

        Service service = ServiceReader.readService(new File(ScriptUtils.getBuildRecipesServicesPath() + "/" + folderName));

        installer = new ServiceInstaller(restUrl, service.getName());
        installer.recipePath(folderName);
        installer.setDisableSelfHealing(true);
        installer.install();

        LogUtils.log("Creating a new file called foo.txt in the storage volume. " + "running 'touch /storage/foo.txt' command on remote machine.");
        invokeCommand(service.getName(), "writeToStorage");

        LogUtils.log("listing all files inside mounted storage folder. running 'ls /storage/' command");
        String listFilesResult = invokeCommand(service.getName(), "listFilesInStorage");

        AssertUtils.assertTrue("File was not created in storage volume. Output was " + listFilesResult, listFilesResult.contains("foo.txt"));

        installer.uninstall();
    }

    private void testMount(final String folderName) throws Exception {

        Service service = ServiceReader.readService(new File(ScriptUtils.getBuildRecipesServicesPath() + "/" + folderName));
        String serviceName = service.getName();

        installer = new ServiceInstaller(restUrl, serviceName);
        installer.recipePath(folderName);
        installer.setDisableSelfHealing(true);
        installer.install();

        LogUtils.log("Creating a new file called foo.txt in the storage volume. " + "running 'touch /storage/foo.txt' command on remote machine.");
        invokeCommand(serviceName, "writeToStorage");

        LogUtils.log("listing all files inside mounted storage folder. running 'ls /storage/' command");
        String listFilesResult = invokeCommand(serviceName, "listFilesInStorage");

        AssertUtils.assertTrue("File was not created in storage volume. Output was " + listFilesResult, listFilesResult.contains("foo.txt"));

        LogUtils.log("Unmounting volume from file system");
        invokeCommand(serviceName, "unmount");

        LogUtils.log("Detaching volume");
        VolumeDetails vol = storageApiHelper.getVolumesByPrefix(getVolumePrefixForTemplate("SMALL_BLOCK")).iterator().next();
        String attachmentId = storageApiHelper.getVolumeAttachments(vol.getId()).iterator().next();
        storageApiHelper.detachVolume(vol.getId(), computeApiHelper.getServerById(attachmentId).getPrivateAddress());

        //asserting the file is not in the mounted directory
        LogUtils.log("listing all files inside mounted storage folder. running 'ls /storage/' command");
        listFilesResult = invokeCommand(serviceName, "listFilesInStorage");

        AssertUtils.assertTrue("The newly created file is in the mounted directory after detachment", !listFilesResult.contains("foo.txt"));

        LogUtils.log("Reattaching the volume to the service machine");

        MachineDetails agent = computeApiHelper.getServerById(attachmentId);
        storageApiHelper.attachVolume(vol.getId(), cloudService.getCloud().getCloudStorage().getTemplates().get("SMALL_BLOCK").getDeviceName(), agent.getPrivateAddress());
        invokeCommand(serviceName, "mount");

        //asserting the file is in the mounted directory
        LogUtils.log("listing all files inside mounted storage folder. running 'ls /storage/' command");
        listFilesResult = invokeCommand(serviceName, "listFilesInStorage");

        AssertUtils.assertTrue("the created file is not in the mounted directory after reattachment", listFilesResult.contains("foo.txt"));
    }

    private void testStorageVolumeMounted(final String folderName) throws Exception {

        String serviceName = ServiceReader.readService(new File(ScriptUtils.getBuildRecipesServicesPath() + "/" + folderName)).getName();

        String expectedMountOutput = "/dev/xvdc on /storage type ext4 (rw)";
        if(cloudService.getCloud().getName().equalsIgnoreCase("hp-grizzly")){
            expectedMountOutput = "/dev/vdc on /storage type ext4 (rw)";
        }

        installer = new ServiceInstaller(restUrl, serviceName);
        installer.recipePath(folderName);
        installer.setDisableSelfHealing(true);
        installer.install();

        LogUtils.log("Listing all mounted devices. running command 'mount -l' on remote machine");
        String listMountedResult = invokeCommand(serviceName, "listMount");

        AssertUtils.assertTrue("device is not in the mounted devices list: " + listMountedResult,
                listMountedResult.contains(expectedMountOutput));

        installer.uninstall();
    }

    
    private void testInstallWithStorage(final String folderName) throws Exception {
    	testInstallWithStorage(folderName, true);
    }
    
    private void testInstallWithStorage(final String folderName, final boolean deleteOnExit) throws Exception {

        String serviceName = ServiceReader.readService(new File(ScriptUtils.getBuildRecipesServicesPath() + "/" + folderName)).getName();
        installer = new ServiceInstaller(restUrl, serviceName);
        installer.recipePath(folderName);
        installer.install();

        String volumesPrefix = getVolumePrefixForTemplate("SMALL_BLOCK");
        LogUtils.log("Searching for volumes created by the service installation");
        // the install should have created and attached a volume with a name prefix of the class name. see customizeCloud below.
        Set<VolumeDetails> volumesAfterInstallation = storageApiHelper.getVolumesByPrefix(volumesPrefix);
        AssertUtils.assertTrue("No volumes were created after the service installation.", !volumesAfterInstallation.isEmpty());
        VolumeDetails ourVolume = volumesAfterInstallation.iterator().next();

        AssertUtils.assertNotNull("could not find the required volume after install service", ourVolume);
        LogUtils.log("Found volume : " + ourVolume);

        // also check it is attached.
        AssertUtils.assertEquals("the volume should have one attachments", 1, 
        		storageApiHelper.getVolumeAttachments(ourVolume.getId()).size());

        // TODO elip - assert Volume configuration?
        LogUtils.log("Undeploying service: " + serviceName);
        String uninstallOutput = installer.uninstall();
        String expectedOutput = "Successfully undeployed " + serviceName;
        AssertUtils.assertTrue("Uninstall-service did not return the expected output: \"" + expectedOutput + "\"."
        		+ " The returned output was: " + uninstallOutput, uninstallOutput.contains(expectedOutput));
        
        // assert volume was detached
        Set<VolumeDetails> volumesAfterUninstall = storageApiHelper.getVolumesByPrefix(volumesPrefix);
        
        if (deleteOnExit) {
        	// volume is expected to be deleted after uninstall
            AssertUtils.assertTrue("Uninstall-service did not remove volumes with prefix: " + volumesPrefix, 
            		volumesAfterUninstall.isEmpty());
        } else {
        	// volume should remain after uninstall, verify that it's still there
            AssertUtils.assertTrue("Uninstall-service wronfully removed volumes with prefix: " + volumesPrefix, 
            		volumesAfterUninstall.size() > 0);

            // now we can remove the volume
            VolumeDetails volumeDetails = volumesAfterUninstall.iterator().next();
            LogUtils.log("Volume " +   volumeDetails.getName() + "[" + volumeDetails.getId() + "]" +
            		" found after uninstall. This is expected. Deleting the volume now.");
            
            storageApiHelper.deleteVolume(volumeDetails.getId());
        }
                
    }

    private void testDeleteOnExitFalse(final String folderName) throws Exception {

        String serviceName = ServiceReader.readService(new File(ScriptUtils.getBuildRecipesServicesPath() + "/" + folderName)).getName();

        installer = new ServiceInstaller(restUrl, serviceName);
        installer.recipePath(folderName);
        installer.setDisableSelfHealing(true);
        installer.install();

        LogUtils.log("Searching for volumes created by the service installation");
        // the install should have created and attached a volume with a name prefix of the class name. see customizeCloud below.
        VolumeDetails ourVolume = storageApiHelper.getVolumesByPrefix(getVolumePrefixForTemplate("SMALL_BLOCK")).iterator().next();

        AssertUtils.assertNotNull("could not find the required volume after install service", ourVolume);
        LogUtils.log("Found volume : " + ourVolume);
        // also check it is attached.
        AssertUtils.assertEquals("the volume should have one attachments", 1, storageApiHelper.getVolumeAttachments(ourVolume.getId()).size());

        // TODO elip - assert Volume configuration?

        installer.uninstall();

        LogUtils.log("Searching for volumes created by the service after uninstall");
        // the install should have created and attached a volume with a name prefix of the class name. see customizeCloud below.
        ourVolume = storageApiHelper.getVolumesByPrefix(getVolumePrefixForTemplate("SMALL_BLOCK")).iterator().next();

        AssertUtils.assertNotNull("could not find the required volume after uninstall service", ourVolume);
        LogUtils.log("Found volume : " + ourVolume);
        storageApiHelper.deleteVolume(ourVolume.getId());

    }
    
    
    private void testDeleteOnExitTrue(final String folderName) throws Exception {

        String serviceName = ServiceReader.readService(new File(ScriptUtils.getBuildRecipesServicesPath() + "/" + folderName)).getName();

        installer = new ServiceInstaller(restUrl, serviceName);
        installer.recipePath(folderName);
        installer.setDisableSelfHealing(true);
        installer.install();

        LogUtils.log("Searching for volumes created by the service installation");
        // the install should have created and attached a volume with a name prefix of the class name. see customizeCloud below.
        VolumeDetails ourVolume = storageApiHelper.getVolumesByPrefix(getVolumePrefixForTemplate("SMALL_BLOCK")).iterator().next();

        AssertUtils.assertNotNull("could not find the required volume after install service", ourVolume);
        LogUtils.log("Found volume : " + ourVolume);
        // also check it is attached.
        AssertUtils.assertEquals("the volume should have one attachments", 1, storageApiHelper.getVolumeAttachments(ourVolume.getId()).size());

        // TODO elip - assert Volume configuration?

        installer.uninstall();

        LogUtils.log("Searching for volumes created by the service after uninstall");
        // the install should have created and attached a volume with a name prefix of the class name. see customizeCloud below.
        Set<VolumeDetails> volumeDetails = storageApiHelper.getVolumesByPrefix(getVolumePrefixForTemplate("SMALL_BLOCK"));

        AssertUtils.assertTrue("Found a leaking volume after uninstall service", volumeDetails.isEmpty());
    }
    

    private String copyServiceToRecipesFolder(final String path, final String originalFolderName) throws IOException, DSLException, PackagingException {
        String buildRecipesServicesPath = ScriptUtils.getBuildRecipesServicesPath();

        String folderName = originalFolderName + System.currentTimeMillis();

        File serviceFolder = new File(buildRecipesServicesPath, folderName);
        if (serviceFolder.exists()) {
            FileUtils.deleteDirectory(serviceFolder);
        }
        serviceFolder.mkdir();
        FileUtils.copyDirectory(new File(path), serviceFolder);
        return folderName;
    }

    public void setTemplate(final String path, final String computeTemplateName, boolean useManagement) throws Exception {
        File serviceFile = DSLReader.findDefaultDSLFile(org.cloudifysource.dsl.internal.DSLUtils.SERVICE_DSL_FILE_NAME_SUFFIX, new File(path));
        Map<String, String> props = new HashMap<String,String>();
        props.put("ENTER_TEMPLATE", computeTemplateName);
        if (!useManagement) {
            props.put("useManagement true", "useManagement false");
        }
        IOUtils.replaceTextInFile(serviceFile.getAbsolutePath(), props);
    }

    private String invokeCommand(String serviceName, String commandName)
            throws IOException, InterruptedException {
        ServiceInstaller installer = new ServiceInstaller(restUrl, serviceName);
        return installer.invoke(commandName);
    }

    private void waitForPendingVolumes(final Service service) throws Exception {

        final long endTime = System.currentTimeMillis() + 5 * 60 * 1000;

        while (System.currentTimeMillis() < endTime) {
            Set<VolumeDetails> creatingVolumes = getCreatingVolumes(service);
            if (!creatingVolumes.isEmpty()) {
                LogUtils.log("Found volumes that are still being created : " + creatingVolumes);
                Thread.sleep(1000 * 5);
            } else {
                return;
            }
        }
    }

    private Set<VolumeDetails> getCreatingVolumes(Service service) throws StorageProvisioningException {
        final String storageTemplateName = service.getStorage().getTemplate();
        String volumePrefixForTemplate = getVolumePrefixForTemplate(storageTemplateName);
        LogUtils.log("Retrieving volumes that are being created. using prefix : " + volumePrefixForTemplate);
        Set<VolumeDetails> volumesByPrefix = storageApiHelper.getVolumesByPrefix(volumePrefixForTemplate);
        Set<VolumeDetails> creatingVolumes = new HashSet<VolumeDetails>();
        for (VolumeDetails volumeDetails : volumesByPrefix) {
            if (storageApiHelper.isVolumeCreating(volumeDetails.getId())) {
                LogUtils.log("Found a volume in status CREATING : " + volumeDetails + " . Sleeping to allow it to reach a new status");
                creatingVolumes.add(volumeDetails);
            }
        }
        if (creatingVolumes.isEmpty()) {
            LogUtils.log("Could not find any volumes with prefix " + volumePrefixForTemplate + " that are being created");
        }
        return creatingVolumes;
    }
    
    /**
     * Compares the sets of the original volumes and the new volumes and returns only the newly created volumes
     * @param originalVolumeSet
     * @param newVolumeSet
     * @return
     */
    private Set<VolumeDetails> getNewlyCreatedVolumes(final Set<VolumeDetails> originalVolumeSet, final Set<VolumeDetails> newVolumeSet) {
    	Set<VolumeDetails> createdVolumes = new HashSet<VolumeDetails>();
    	String volumeId;
    	
    	for (VolumeDetails volume : newVolumeSet) {
    		boolean volumeFound = false;
    		volumeId = volume.getId();
    		
    		for (VolumeDetails originalVolume : originalVolumeSet) {
    			if (originalVolume.getId().equalsIgnoreCase(volumeId)) {
    				volumeFound = true;
    				break;
    			}
    		}
    		
    		if (!volumeFound) {
    			createdVolumes.add(volume);
    		}
    	}
    	
    	return createdVolumes;
    }
}
