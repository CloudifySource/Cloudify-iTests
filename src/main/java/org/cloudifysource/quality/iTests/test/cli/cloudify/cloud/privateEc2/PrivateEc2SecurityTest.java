package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.privateEc2;

import static org.cloudifysource.quality.iTests.test.cli.cloudify.security.SecurityConstants.BUILD_SECURITY_FILE_PATH;
import static org.cloudifysource.quality.iTests.test.cli.cloudify.security.SecurityConstants.DEFAULT_KEYSTORE_FILE_PATH;
import static org.cloudifysource.quality.iTests.test.cli.cloudify.security.SecurityConstants.DEFAULT_KEYSTORE_PASSWORD;
import static org.cloudifysource.quality.iTests.test.cli.cloudify.security.SecurityConstants.UNAUTHORIZED;
import static org.cloudifysource.quality.iTests.test.cli.cloudify.security.SecurityConstants.USER_PWD_ALL_ROLES;
import static org.cloudifysource.quality.iTests.test.cli.cloudify.security.SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER;
import static org.cloudifysource.quality.iTests.test.cli.cloudify.security.SecurityConstants.USER_PWD_CLOUD_ADMIN;
import static org.cloudifysource.quality.iTests.test.cli.cloudify.security.SecurityConstants.USER_PWD_VIEWER;
import iTests.framework.utils.AssertUtils;
import iTests.framework.utils.ScriptUtils;

import java.io.IOException;

import org.cloudifysource.quality.iTests.framework.utils.ApplicationInstaller;
import org.cloudifysource.quality.iTests.framework.utils.CloudBootstrapper;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.NewAbstractSecurityCloudTest;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.CloudService;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.privateEc2.PrivateEc2Service;
import org.cloudifysource.quality.iTests.test.cli.cloudify.security.SecurityConstants;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class PrivateEc2SecurityTest extends NewAbstractSecurityCloudTest {

    private static final String APP_PATH = ScriptUtils.getBuildRecipesApplicationsPath() + "/sampleApplication";
    private static final String APP_NAME = "sampleApplication";

    @Override
    protected String getCloudName() {
        return "privateEc2";
    }

    @Override
    protected boolean isReusableCloud() {
        return false;
    }

    @Override
    @BeforeClass(alwaysRun = true)
    protected void bootstrap() throws Exception {
        CloudBootstrapper securedBootstrapper = new CloudBootstrapper();
        securedBootstrapper.secured(true).securityFilePath(BUILD_SECURITY_FILE_PATH).user(USER_PWD_ALL_ROLES).password(USER_PWD_ALL_ROLES);
        securedBootstrapper.keystoreFilePath(DEFAULT_KEYSTORE_FILE_PATH).keystorePassword(DEFAULT_KEYSTORE_PASSWORD);

        CloudService service = new PrivateEc2Service(true);
        service.setBootstrapper(securedBootstrapper);

        super.bootstrap(service);
    }

    @AfterClass(alwaysRun = true)
    protected void teardown() throws Exception {
        super.teardown();
    }

    @AfterMethod(alwaysRun = true)
    protected void uninstall() throws Exception {
        uninstallApplicationIfFound(APP_NAME, SecurityConstants.USER_PWD_ALL_ROLES, SecurityConstants.USER_PWD_ALL_ROLES);
    }

    protected String installApplicationAndWait(final String cloudifyUsername, final String cloudifyPassword, boolean isExpectedToFail)
            throws IOException, InterruptedException {
        ApplicationInstaller applicationInstaller = new ApplicationInstaller(getRestUrl(), APP_NAME);
        applicationInstaller.recipePath(APP_PATH);
        applicationInstaller.cloudConfiguration(ScriptUtils.getBuildPath() + "/cfn-templates");
        applicationInstaller.waitForFinish(true);
        applicationInstaller.cloudifyUsername(cloudifyUsername);
        applicationInstaller.cloudifyPassword(cloudifyPassword);
        applicationInstaller.expectToFail(isExpectedToFail);
        return applicationInstaller.install();
    }

    protected void installAndUninstallApplication(final String cloudifyUsername, final String cloudifyPassword, boolean isExpectedToFail)
            throws IOException, InterruptedException {
        String output = this.installApplicationAndWait(cloudifyUsername, cloudifyPassword, isExpectedToFail);

        if (isExpectedToFail) {
            assertTrue("application installation access granted to " + cloudifyUsername, output.contains(SecurityConstants.ACCESS_DENIED_MESSAGE));
        } else {
            AssertUtils.assertTrue(output.contains("Application " + APP_NAME + " installed successfully"));
            uninstallApplicationAndWait(APP_PATH, APP_NAME, TIMEOUT_IN_MINUTES, cloudifyUsername, cloudifyPassword, isExpectedToFail, null);
        }
    }

    protected void uninstallApplicationIfFound(String applicationName, final String cloudifyUsername, final String cloudifyPassword) throws IOException,
            InterruptedException {
        ApplicationInstaller applicationInstaller = new ApplicationInstaller(getRestUrl(), applicationName);
        applicationInstaller.waitForFinish(true);
        applicationInstaller.cloudifyUsername(cloudifyUsername);
        applicationInstaller.cloudifyPassword(cloudifyPassword);
        applicationInstaller.uninstallIfFound();
    }

    @Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void installSampleApplicationTest() throws Exception {
        this.installAndUninstallApplication(USER_PWD_APP_MANAGER_AND_VIEWER, USER_PWD_APP_MANAGER_AND_VIEWER, false);
        this.installAndUninstallApplication(USER_PWD_VIEWER, USER_PWD_VIEWER, true);
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
    public void installWithoutCredentialsTest() throws IOException, InterruptedException {
        String output = "no output";
        output = installApplicationAndWait(null, null, true);
        assertTrue("install access granted to an Anonymous user", output.toLowerCase().contains(SecurityConstants.UNAUTHORIZED.toLowerCase()));
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
    public void loginWithNonexistentUserTest() throws IOException, InterruptedException {
        String output = "no output";
        output = login(USER_PWD_CLOUD_ADMIN + "bad", USER_PWD_CLOUD_ADMIN, true);
        assertTrue("login succeeded for user: " + USER_PWD_CLOUD_ADMIN + "bad", output.toLowerCase().contains(UNAUTHORIZED.toLowerCase()));
    }
}
