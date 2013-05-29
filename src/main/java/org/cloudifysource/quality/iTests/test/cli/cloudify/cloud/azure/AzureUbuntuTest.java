package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.azure;

import org.cloudifysource.quality.iTests.framework.utils.CloudBootstrapper;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.NewAbstractCloudTest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

/**
 * Created with IntelliJ IDEA.
 * User: inbarc
 * CLOUDIFY-1503
 * Date: 5/28/13
 * Time: 1:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class AzureUbuntuTest extends NewAbstractCloudTest {

    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 5  , enabled = true)
    protected void bootstrap() throws Exception {

        CloudBootstrapper bootstrapper= new CloudBootstrapper();
        bootstrapper.timeoutInMinutes(15);

        super.bootstrap(bootstrapper);
    }

    @AfterClass
    public void teardown() {
        try {
            super.teardown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void customizeCloud() throws Exception {
        super.customizeCloud();
        getService().getAdditionalPropsToReplace().put("managementMachineTemplate \"SMALL_LINUX\"", "managementMachineTemplate \"UBUNTU\"")  ;
    }


    @Override
    protected String getCloudName() {
        return "azure";
    }

    @Override
    protected boolean isReusableCloud() {
        return false;
    }
}
