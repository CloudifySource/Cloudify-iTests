package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon.failover;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * User: nirb
 * Date: 06/03/13
 */
public class RepetitiveShutdownManagersBootstrapTest extends AbstractByonManagementPersistencyTest{

    @BeforeMethod(alwaysRun = true)
    public void bootstrapAndInit() throws Exception{
        super.prepareTest();
    }

    @AfterMethod(alwaysRun = true)
    public void afterTest() throws Exception{
        super.afterTest();
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testRepetitiveShutdownManagersBootstrap() throws Exception {
        super.testRepetitiveShutdownManagersBootstrap();
    }

}
