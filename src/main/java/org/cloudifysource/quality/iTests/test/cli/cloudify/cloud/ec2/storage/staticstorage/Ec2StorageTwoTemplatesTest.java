package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2.storage.staticstorage;

import iTests.framework.tools.SGTestHelper;

import java.io.File;

import org.cloudifysource.quality.iTests.framework.utils.ApplicationInstaller;
import org.cloudifysource.quality.iTests.framework.utils.RecipeInstaller;
import org.cloudifysource.quality.iTests.framework.utils.ServiceInstaller;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.AbstractStorageAllocationTest;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.ec2.Ec2CloudService;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Author: nirb
 * Date: 28/02/13
 */
public class Ec2StorageTwoTemplatesTest extends AbstractStorageAllocationTest {

    @BeforeClass(alwaysRun = true)
    protected void bootstrap() throws Exception {
        super.bootstrap();
    }

    @Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    protected void testTwoTemplatesAllocation() throws Exception {
        storageAllocationTester.testTwoTemplates();
    }

    @Override
    protected void customizeCloud() throws Exception {
        super.customizeCloud();
        File customCloudFile = new File(SGTestHelper.getCustomCloudConfigDir(getCloudName()) + "/storage-two-templates-multitenant/ec2-cloud.groovy");
        ((Ec2CloudService)getService()).setCloudGroovy(customCloudFile);
    }

    @Override
    protected String getCloudName() {
        return "ec2";
    }

    @Override
    protected boolean isReusableCloud() {
        return false;
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

    @AfterClass
    public void scanForLeakesFromGroovy1() throws Exception {
        super.scanForLeakedVolumesCreatedViaTemplate("GROOVY1");
    }

    @AfterClass
    public void scanForLeakesFromGroovy2() throws Exception {
        super.scanForLeakedVolumesCreatedViaTemplate("GROOVY2");
    }


    @AfterClass(alwaysRun = true)
    protected void teardown() throws Exception {
        super.teardown();
    }
}
