package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.privateEc2;

import iTests.framework.utils.AssertUtils;
import iTests.framework.utils.ScriptUtils;

import java.io.File;

import org.cloudifysource.dsl.Application;
import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.quality.iTests.framework.utils.ApplicationInstaller;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.NewAbstractCloudTest;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.privateEc2.PrivateEc2Service;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class PrivateEc2Test extends NewAbstractCloudTest {

    @Override
    protected String getCloudName() {
        return "privateEc2";
    }

    @Override
    protected boolean isReusableCloud() {
        return false;
    }

    @BeforeClass(alwaysRun = true)
    protected void bootstrap() throws Exception {
        super.bootstrap(new PrivateEc2Service(), null);
    }

    @Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testSampleApplication() throws Exception {

        String applicationPath = ScriptUtils.getBuildRecipesApplicationsPath() + "/sampleApplication";

        String restUrl = getRestUrl();
        ApplicationInstaller installer = new ApplicationInstaller(restUrl, null);
        installer.setApplicationName("sampleApplication");
        installer.recipePath(applicationPath);
        installer.cloudConfiguration(ScriptUtils.getBuildPath() + "/cfn-templates");
        installer.waitForFinish(true);
        installer.install();

        Application application = ServiceReader.getApplicationFromFile(new File(applicationPath)).getApplication();

        String command = "connect " + restUrl + ";use-application sampleApplication;list-services";
        String output = CommandTestUtils.runCommandAndWait(command);

        for (Service singleService : application.getServices()) {
            AssertUtils.assertTrue("the service " + singleService.getName() + " is not running", output.contains(singleService.getName()));
        }
    }

    @AfterClass(alwaysRun = true)
    protected void teardown() throws Exception {
        super.teardown();
    }
}
