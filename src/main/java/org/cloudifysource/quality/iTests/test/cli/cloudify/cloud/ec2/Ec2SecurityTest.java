package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import iTests.framework.utils.LogUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.NewAbstractSecurityCloudTest;
import org.cloudifysource.quality.iTests.test.cli.cloudify.security.SecurityConstants;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class Ec2SecurityTest extends NewAbstractSecurityCloudTest {

	private static final String SIMPLE_APP_NAME = "simple";
	private static final String SIMPLE_APP_PATH = CommandTestUtils.getPath("src/main/resources/apps/USM/usm/applications/" + SIMPLE_APP_NAME);
	private static final String SIMPLE_SERVICE_NAME = "simple";
	private static final String SIMPLE_SERVICE_PATH = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/" + SIMPLE_SERVICE_NAME);
	
	private static final String GROOVY_APP_NAME = "groovyApp";
	private static final String GROOVY_APP_PATH = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/applications/" + GROOVY_APP_NAME);
	private static final String GROOVY_SERVICE_NAME = "groovy";
	private static final String GROOVY2_SERVICE_NAME = "groovy2";
	
	private static final String INSTANCE_VERIFICATION_STRING = "instance #1";
	private static final String ACCESS_DENIED_MESSAGE = "no_permission_access_is_denied";
	
	private static final int TIMEOUT_IN_MINUTES = 60;

	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();		
	}

	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}
	
	@AfterMethod(alwaysRun = true)
	protected void uninstall() throws Exception {
		
		uninstallApplicationIfFound(SIMPLE_APP_NAME, SecurityConstants.USER_PWD_ALL_ROLES, SecurityConstants.USER_PWD_ALL_ROLES);
		uninstallApplicationIfFound(GROOVY_APP_NAME, SecurityConstants.USER_PWD_ALL_ROLES, SecurityConstants.USER_PWD_ALL_ROLES);

		uninstallServiceIfFound(SIMPLE_SERVICE_NAME, SecurityConstants.USER_PWD_ALL_ROLES, SecurityConstants.USER_PWD_ALL_ROLES);
		uninstallServiceIfFound(GROOVY_SERVICE_NAME, SecurityConstants.USER_PWD_ALL_ROLES, SecurityConstants.USER_PWD_ALL_ROLES);
		uninstallServiceIfFound(GROOVY2_SERVICE_NAME, SecurityConstants.USER_PWD_ALL_ROLES, SecurityConstants.USER_PWD_ALL_ROLES);
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT*5, enabled = true)
	public void installAndUninstallTest() throws IOException, InterruptedException {

		installAndUninstall(SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, false);
		installAndUninstall(SecurityConstants.USER_PWD_APP_MANAGER, SecurityConstants.USER_PWD_APP_MANAGER, false);
		installAndUninstall(SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, false);
		installAndUninstall(SecurityConstants.USER_PWD_CLOUD_ADMIN, SecurityConstants.USER_PWD_CLOUD_ADMIN, false);
		installAndUninstall(SecurityConstants.USER_PWD_VIEWER, SecurityConstants.USER_PWD_VIEWER, true);
		installAndUninstall(SecurityConstants.USER_PWD_NO_ROLE, SecurityConstants.USER_PWD_NO_ROLE, true);
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void installAndUninstallWithDifferentUsersTest() throws IOException, InterruptedException {

		String output = "no output";
		
		installApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, false, null);

		output = uninstallApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.USER_PWD_VIEWER, SecurityConstants.USER_PWD_VIEWER, true, null);
		assertTrue("uninstall access granted to " + SecurityConstants.VIEWER_DESCRIPTIN, output.contains(ACCESS_DENIED_MESSAGE));

		uninstallApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.USER_PWD_APP_MANAGER, SecurityConstants.USER_PWD_APP_MANAGER, false, null);
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void installWithoutCredentialsTest() throws IOException, InterruptedException{

		String output = "no output";
		output = installApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, null, null, true, null);

		assertTrue("install access granted to an Anonymous user" , output.contains(SecurityConstants.BAD_CREDENTIALS_MESSAGE));
	}

	@Test(timeOut = EXTENDED_TEST_TIMEOUT, enabled = true)
	public void installingAndViewingTest() throws IOException, InterruptedException{

		installApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.USER_PWD_APP_MANAGER, SecurityConstants.USER_PWD_APP_MANAGER, false, null);
		installApplicationAndWait(GROOVY_APP_PATH, GROOVY_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, false, null);

		verifyVisibleLists(SecurityConstants.APP_MANAGER_DESCRIPTIN, SecurityConstants.USER_PWD_CLOUD_ADMIN, SecurityConstants.USER_PWD_CLOUD_ADMIN, SecurityConstants.CLOUD_ADMIN_DESCRIPTIN, SIMPLE_APP_NAME, false);
		verifyVisibleLists(SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, SecurityConstants.USER_PWD_CLOUD_ADMIN, SecurityConstants.USER_PWD_CLOUD_ADMIN, SecurityConstants.CLOUD_ADMIN_DESCRIPTIN, GROOVY_APP_NAME, false);
		
		verifyVisibleLists(SecurityConstants.APP_MANAGER_DESCRIPTIN, SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_DESCRIPTION, SIMPLE_APP_NAME, true);
		verifyVisibleLists(SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_DESCRIPTION, GROOVY_APP_NAME, true);
		
		verifyVisibleLists(SecurityConstants.APP_MANAGER_DESCRIPTIN, SecurityConstants.USER_PWD_APP_MANAGER, SecurityConstants.USER_PWD_APP_MANAGER, SecurityConstants.APP_MANAGER_DESCRIPTIN, SIMPLE_APP_NAME, true);
		verifyVisibleLists(SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, SecurityConstants.USER_PWD_APP_MANAGER, SecurityConstants.USER_PWD_APP_MANAGER, SecurityConstants.APP_MANAGER_DESCRIPTIN, GROOVY_APP_NAME, true);
		
		verifyVisibleLists(SecurityConstants.APP_MANAGER_DESCRIPTIN, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, SIMPLE_APP_NAME, true);
		verifyVisibleLists(SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, GROOVY_APP_NAME, true);
		
		verifyVisibleLists(SecurityConstants.APP_MANAGER_DESCRIPTIN, SecurityConstants.USER_PWD_VIEWER, SecurityConstants.USER_PWD_VIEWER, SecurityConstants.VIEWER_DESCRIPTIN, SIMPLE_APP_NAME, false);
		verifyVisibleLists(SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, SecurityConstants.USER_PWD_VIEWER, SecurityConstants.USER_PWD_VIEWER, SecurityConstants.VIEWER_DESCRIPTIN, GROOVY_APP_NAME, true);
		
		verifyVisibleLists(SecurityConstants.APP_MANAGER_DESCRIPTIN, SecurityConstants.USER_PWD_NO_ROLE, SecurityConstants.USER_PWD_NO_ROLE, SecurityConstants.NO_ROLE_DESCRIPTIN, SIMPLE_APP_NAME, false);
		verifyVisibleLists(SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, SecurityConstants.USER_PWD_NO_ROLE, SecurityConstants.USER_PWD_NO_ROLE, SecurityConstants.NO_ROLE_DESCRIPTIN, GROOVY_APP_NAME, false);

	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void loginTest() throws IOException, InterruptedException {

		String output = "no output";
		
		output = login(SecurityConstants.USER_PWD_VIEWER, SecurityConstants.USER_PWD_VIEWER, false);		
		assertTrue("login failed for: " + SecurityConstants.VIEWER_DESCRIPTIN, output.contains("Logged in successfully"));			

	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void connectWithNonexistentUserTest() throws IOException, InterruptedException {

		String output = connect(SecurityConstants.USER_PWD_CLOUD_ADMIN + "bad", SecurityConstants.USER_PWD_CLOUD_ADMIN, true);		
		assertTrue("connect succeeded for user: " + SecurityConstants.USER_PWD_CLOUD_ADMIN + "bad", output.contains(SecurityConstants.BAD_CREDENTIALS_MESSAGE));			

	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void connectWithNoPasswordTest() throws IOException, InterruptedException {
		
		String output = connect(SecurityConstants.USER_PWD_CLOUD_ADMIN, null, true);		
		assertTrue("connect succeeded for: " + SecurityConstants.CLOUD_ADMIN_DESCRIPTIN + " without providing a password", output.contains(SecurityConstants.BAD_CREDENTIALS_MESSAGE));			
		
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void loginWithNonexistentUserTest() throws IOException, InterruptedException {

		String output = "no output";
		
		output = login(SecurityConstants.USER_PWD_CLOUD_ADMIN + "bad", SecurityConstants.USER_PWD_CLOUD_ADMIN, true);					

		assertTrue("login succeeded for user: " + SecurityConstants.USER_PWD_CLOUD_ADMIN + "bad", output.contains(SecurityConstants.BAD_CREDENTIALS_MESSAGE));			
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void connectWithWrongPassword() throws IOException, InterruptedException {

		String output = connect(SecurityConstants.USER_PWD_CLOUD_ADMIN, SecurityConstants.USER_PWD_CLOUD_ADMIN + "bad", true);		
		assertTrue("connect succeeded for password: " + SecurityConstants.USER_PWD_CLOUD_ADMIN + "bad", output.contains(SecurityConstants.BAD_CREDENTIALS_MESSAGE));			

	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void loginWithWrongPassword() throws IOException, InterruptedException {

		String output = "no output";
		
		output = login(SecurityConstants.USER_PWD_CLOUD_ADMIN, SecurityConstants.USER_PWD_CLOUD_ADMIN + "bad", true);
		
		assertTrue("login succeeded for password: " + SecurityConstants.USER_PWD_CLOUD_ADMIN + "bad", output.contains(SecurityConstants.BAD_CREDENTIALS_MESSAGE));
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void installWithWrongGroup() throws IOException, InterruptedException {
		
		String output = "no output";
		
		output = installApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, true, "ROLE_CLOUDADMINS");
		
		assertTrue("install succeeded with authGroup ROLE_CLOUDADMINS for: " + SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, output.contains(ACCESS_DENIED_MESSAGE));
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void installAndUninstallWithDifferentGroup() throws IOException, InterruptedException {
		
		String output = "no output";
		
		installApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, false, "ROLE_CLOUDADMINS");
		output = uninstallApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, true, null);
		
		assertTrue("unseen application uninstall succeeded", output.contains(ACCESS_DENIED_MESSAGE));
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void tamperWithSecurityFileTest() throws Exception{

		String fakeCloudAdminUserAndPassword = "John";

		String originalFilePath = SecurityConstants.BUILD_SECURITY_FILE_PATH;
		String backupFilePath = originalFilePath + ".tempBackup";
		String fakeFilePath = CommandTestUtils.getPath("src/main/config/security/custom-spring-security.xml");
		File originalFile = new File(originalFilePath);
		File backupFile = new File(backupFilePath);
		File fakeFile = new File(fakeFilePath);
		String output = "no output";

		LogUtils.log("moving " + originalFilePath + " to " + backupFilePath);
		FileUtils.moveFile(originalFile, backupFile);
		
		try {
			LogUtils.log("copying " + fakeFilePath + " to " + originalFilePath);
			FileUtils.copyFile(fakeFile, originalFile);
			
			output = installApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, fakeCloudAdminUserAndPassword, fakeCloudAdminUserAndPassword, true, null);

		} 
		finally {			
			LogUtils.log("deleting " + originalFilePath);
			try{
				FileUtils.deleteQuietly(originalFile);
			}
			catch(Exception e) {
				LogUtils.log("deletion of " + originalFilePath + " failed", e);
			}
			
			LogUtils.log("moving " + backupFilePath + " to " + originalFilePath);
			try{
				FileUtils.moveFile(backupFile, originalFile);
			}
			catch(Exception e) {
				LogUtils.log("moving of " + backupFilePath + " failed", e);
			}
		}
				
		assertTrue("install access granted to viewer " + fakeCloudAdminUserAndPassword, output.contains(ACCESS_DENIED_MESSAGE));			
	}

	protected void verifyVisibleLists(String installer, String viewerName, String viewerPassword, String viewerDescription, String appName, boolean isVisible) throws IOException, InterruptedException {
		
		String output = "no output";
		
		if(isVisible){
			output = listApplications(viewerName, viewerPassword, false);
			assertTrue(viewerDescription + " doesn't see the application of " + installer, output.contains(appName));
		}
		else{			
			output = listApplications(viewerName, viewerPassword, true);
			assertTrue(viewerDescription + " sees the application of " + installer, !output.contains(appName));
		}
		
		if(isVisible){
			if(appName.equalsIgnoreCase(SIMPLE_APP_NAME)){	
				
				output = listServices(viewerName, viewerPassword, SIMPLE_APP_NAME, false);
				assertTrue(viewerDescription + " doesn't see the services of " + installer, output.contains(SIMPLE_APP_NAME + "." + SIMPLE_SERVICE_NAME));
			}
			else{
				
				output = listServices(viewerName, viewerPassword, GROOVY_APP_NAME, true);
				assertTrue(viewerDescription + " doesn't see the services of " + installer, output.contains(GROOVY_APP_NAME + "." + GROOVY_SERVICE_NAME) && output.contains(GROOVY_APP_NAME + "." + GROOVY2_SERVICE_NAME));
				
			}
				
		}
		else{	
			if(appName.equalsIgnoreCase(SIMPLE_APP_NAME)){
				output = listServices(viewerName, viewerPassword, SIMPLE_APP_NAME, true);
				assertTrue(viewerDescription + " sees the services of " + installer, !output.contains(SIMPLE_APP_NAME + "." + SIMPLE_SERVICE_NAME));			
			}
			else{
				
				output = listServices(viewerName, viewerPassword, GROOVY_APP_NAME, true);
				assertTrue(viewerDescription + " sees the services of " + installer, !(output.contains(GROOVY_APP_NAME + "." + GROOVY_SERVICE_NAME) || output.contains(GROOVY_APP_NAME + "." + GROOVY2_SERVICE_NAME)));							
			}
		}
		
		
		if(appName.equalsIgnoreCase(SIMPLE_APP_NAME)){
			
			
			
			if(isVisible){	
				output = listInstances(viewerName, viewerPassword, SIMPLE_APP_NAME, SIMPLE_SERVICE_NAME, false);
				assertTrue(viewerDescription + " doesn't see the instances of " + installer, output.contains(INSTANCE_VERIFICATION_STRING));
			}
			else{
				output = listInstances(viewerName, viewerPassword, SIMPLE_APP_NAME, SIMPLE_SERVICE_NAME, true);
				assertTrue(viewerDescription + " sees the instances of " + installer, !output.contains(INSTANCE_VERIFICATION_STRING));
				
			}
			
		}
		else{	
			if(isVisible){
				output = listInstances(viewerName, viewerPassword, GROOVY_APP_NAME, GROOVY_SERVICE_NAME, false);
				assertTrue(viewerDescription + " doesn't see the instances of " + installer, output.contains(INSTANCE_VERIFICATION_STRING));			
			}
			else{
				output = listInstances(viewerName, viewerPassword, GROOVY_APP_NAME, GROOVY_SERVICE_NAME, true);
				assertTrue(viewerDescription + " sees the instances of " + installer, !output.contains(INSTANCE_VERIFICATION_STRING));							
			}
		}
	}
	
	public void installAndUninstall(String user, String password, boolean isInstallExpectedToFail) throws IOException, InterruptedException{
		
		String output = installApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, user, password, isInstallExpectedToFail, null);
		
		if(isInstallExpectedToFail){
			assertTrue("application installation access granted to " + user, output.contains(ACCESS_DENIED_MESSAGE));
		}
		
		if(output.contains("Application " + SIMPLE_APP_NAME + " installed successfully")){			
			uninstallApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, user, password, isInstallExpectedToFail, null);
		}
				
		output = installServiceAndWait(SIMPLE_SERVICE_PATH, SIMPLE_SERVICE_NAME, TIMEOUT_IN_MINUTES, user, password, isInstallExpectedToFail, null);
		
		if(isInstallExpectedToFail){
			assertTrue("service installation access granted to " + user, output.contains(ACCESS_DENIED_MESSAGE));
		}
		
		if(output.contains("Service \"" + SIMPLE_SERVICE_NAME + "\" successfully installed")){			
			uninstallServiceAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, user, password, isInstallExpectedToFail, null);
		}

	}
	
	
	
	

//	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 20, enabled = true)
//	public void testRoles() throws IOException, InterruptedException, RestException {
//		doTest(SIMPLE_APP_NAME, SERVICE_NAME);
//	}

//	private void doTest(final String appName, final String serviceName) throws IOException, InterruptedException, RestException {

		// test each Rest server found
//		for (String restUrl : cloudService.getRestUrls()) {

//			installApplication(restUrl, SIMPLE_APP_NAME, SecurityConstants.CLOUD_ADMIN_USER_PWD,
//					SecurityConstants.CLOUD_ADMIN_USER_PWD, null/*authGroups*/);
//			String output = listApplications(restUrl, SecurityConstants.CLOUD_ADMIN_USER_PWD,
//					SecurityConstants.CLOUD_ADMIN_USER_PWD);
//			assertTrue("list-applications did not display application " + SIMPLE_APP_NAME + " for user: "
//					+ SecurityConstants.VIEWER_USER_PWD, output.contains(SIMPLE_APP_NAME));
//			uninstallApplication(restUrl, SIMPLE_APP_NAME, SecurityConstants.CLOUD_ADMIN_USER_PWD,
//					SecurityConstants.CLOUD_ADMIN_USER_PWD, null/*authGroups*/);

			/*output = listApplications(restUrl, SecurityConstants.APP_MANAGER_USER_PWD, SecurityConstants.APP_MANAGER_USER_PWD);
			assertTrue("list-applications did not display application " + APP_NAME + " for user: "
					+ SecurityConstants.APP_MANAGER_USER_PWD, output.contains("Recipe test completed"));

			output = listApplications(restUrl, SecurityConstants.VIEWER_USER_PWD, SecurityConstants.VIEWER_USER_PWD);
			assertTrue("list-applications did not display application " + APP_NAME + " for user: "
					+ SecurityConstants.VIEWER_USER_PWD, output.contains("Recipe test completed"));

			installApplication(restUrl, APP_NAME, SecurityConstants.CLOUD_ADMIN_USER_PWD,
					SecurityConstants.CLOUD_ADMIN_USER_PWD, "ROLE_APP_MANAGERS");
			output = listApplications(restUrl, SecurityConstants.CLOUD_ADMIN_USER_PWD, SecurityConstants.CLOUD_ADMIN_USER_PWD);
			assertTrue("list-applications did not display application " + APP_NAME + " for user: "
					+ SecurityConstants.CLOUD_ADMIN_USER_PWD, output.contains("Recipe test completed"));

			output = listApplications(restUrl, SecurityConstants.APP_MANAGER_USER_PWD, SecurityConstants.APP_MANAGER_USER_PWD);
			assertTrue("list-applications did not display application " + APP_NAME + " for user: "
					+ SecurityConstants.APP_MANAGER_USER_PWD, output.contains("Recipe test completed"));

			output = listApplications(restUrl, SecurityConstants.VIEWER_USER_PWD, SecurityConstants.VIEWER_USER_PWD);
			assertTrue("list-applications display application " + APP_NAME + " for unauthorized user: "
					+ SecurityConstants.VIEWER_USER_PWD, output.contains("Recipe test completed"));*/
//		}
//	}

//	private String listApplications(final String restUrl, final String user, final String password) throws IOException,
//			InterruptedException {
//		
//		return CommandTestUtils.runCommandAndWait("connect -user " + user + " -password " + password + " " + restUrl 
//				+ ";list-applications");
//	}

//	private void checkPuStatus(final String restUrl, final String cloudifyUsername, final String cloudifyPassword) throws IOException, InterruptedException, RestException {
//		String absolutePUName = ServiceUtils.getAbsolutePUName(SIMPLE_APP_NAME, SERVICE_NAME);
//		GSRestClient client = new GSRestClient(cloudifyUsername, cloudifyPassword, new URL(restUrl),
//				PlatformVersion.getVersionNumber());
//		Map<String, Object> entriesJsonMap  = client.getAdminData("ProcessingUnits/Names/" + absolutePUName + "/Status");
//		String serviceStatus = (String)entriesJsonMap.get("DeclaringClass-Enumerator");
//
//		AssertUtils.assertTrue("service is not intact", serviceStatus.equalsIgnoreCase("INTACT"));	
//	}

	@Override
	protected String getCloudName() {
		return "ec2";
	}

	@Override
	protected boolean isReusableCloud() {
		return false;
	}
}
