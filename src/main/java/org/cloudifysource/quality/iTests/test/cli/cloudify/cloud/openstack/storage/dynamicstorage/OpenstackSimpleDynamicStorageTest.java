package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.openstack.storage.dynamicstorage;

import org.cloudifysource.quality.iTests.framework.utils.ApplicationInstaller;
import org.cloudifysource.quality.iTests.framework.utils.RecipeInstaller;
import org.cloudifysource.quality.iTests.framework.utils.ServiceInstaller;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.AbstractStorageAllocationTest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Author: nirb
 * Date: 21/02/13
 */
public class OpenstackSimpleDynamicStorageTest extends AbstractStorageAllocationTest{

    @Override
    protected String getCloudName() {
        return "hp-grizzly";
    }

    @BeforeClass(alwaysRun = true)
    protected void bootstrap() throws Exception {
        super.bootstrap();
    }


    @Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testLinux() throws Exception {
        final String servicePath = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/dynamicstorage/openstack/create-and-attach");
        storageAllocationTester.testInstallWithDynamicStorageLinux("SMALL_LINUX", servicePath, "create-and-attach");
    }

    @AfterMethod
    public void cleanup() {
        RecipeInstaller installer = storageAllocationTester.getInstaller();
        if (installer instanceof ServiceInstaller) {
            ((ServiceInstaller) installer).uninstallIfFound();
        } else {
            ((ApplicationInstaller) installer).uninstallIfFound();
        }
    }

    @AfterClass(alwaysRun = true)
    public void scanForLeakes() throws Exception {
    	try {
    		super.scanForLeakedVolumesCreatedViaTemplate("SMALL_BLOCK");
    	} finally {
    		super.teardown();
    	}
    }

    
    @Override
    protected boolean isReusableCloud() {
        return false;
    }
}
