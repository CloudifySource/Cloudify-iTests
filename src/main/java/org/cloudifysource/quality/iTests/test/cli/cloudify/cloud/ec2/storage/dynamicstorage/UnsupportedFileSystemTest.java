package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2.storage.dynamicstorage;

import org.cloudifysource.quality.iTests.framework.utils.ApplicationInstaller;
import org.cloudifysource.quality.iTests.framework.utils.RecipeInstaller;
import org.cloudifysource.quality.iTests.framework.utils.ServiceInstaller;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.AbstractStorageAllocationTest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class UnsupportedFileSystemTest extends AbstractStorageAllocationTest {

	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
	}

	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testLinux() throws Exception {
        storageAllocationTester.testUnsupportedFileSystemLinux();
	}


	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testUbuntu() throws Exception  {
        storageAllocationTester.testUnsupportedFileSystemUbuntu();
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
    public void scanForLeakes() throws Exception {
        super.scanForLeakedVolumesCreatedViaTemplate("SMALL_BLOCK");
    }

	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}

    @Override
    protected String getCloudName() {
        return "ec2";
    }

    @Override
	protected boolean isReusableCloud() {
		return false;
	}
}
