package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.byon;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import org.cloudifysource.quality.iTests.test.cli.cloudify.security.SecurityConstants;

import org.cloudifysource.quality.iTests.framework.utils.CloudBootstrapper;

public class ByonBootstrapTest extends AbstractByonCloudTest {
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void testBootstrap() throws Exception {
		super.bootstrap();
		super.teardown();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void testSecuredBootstrap() throws Exception {	
		CloudBootstrapper bootstrapper = new CloudBootstrapper();
		bootstrapper.secured(true).securityFilePath(SecurityConstants.BUILD_SECURITY_FILE_PATH)
			.keystoreFilePath(SecurityConstants.DEFAULT_KEYSTORE_FILE_PATH).keystorePassword(SecurityConstants.DEFAULT_KEYSTORE_PASSWORD)
			.user(SecurityConstants.USER_PWD_ALL_ROLES).password(SecurityConstants.USER_PWD_ALL_ROLES);
		super.bootstrap(bootstrapper);
		super.teardown();
	}
	
	@AfterMethod(alwaysRun = true)
	public void closeAdmin() {
		super.closeAdmin();
	}

}
