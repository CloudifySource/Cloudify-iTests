package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2win;

import iTests.framework.utils.LogUtils;
import org.apache.commons.io.FileUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.NewAbstractSecurityCloudTest;
import org.cloudifysource.quality.iTests.test.cli.cloudify.security.SecurityConstants;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

public class Ec2WinSecurityTest extends NewAbstractSecurityCloudTest {

	private static final String SIMPLE_APP_NAME = "simple";
	private static final String SIMPLE_APP_PATH = CommandTestUtils.getPath("src/main/resources/apps/USM/usm/applications/" + SIMPLE_APP_NAME);
	private static final String SIMPLE_SERVICE_NAME = "simple";
	private static final String SIMPLE_SERVICE_PATH = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/" + SIMPLE_SERVICE_NAME);
	
	private static final String GROOVY_APP_NAME = "groovyApp";
	private static final String GROOVY_APP_PATH = CommandTestUtils.getPath("/src/main/resources/apps/USM/usm/applications/" + GROOVY_APP_NAME);
	private static final String GROOVY_SERVICE_NAME = "groovy";
	private static final String GROOVY2_SERVICE_NAME = "groovy2";
	
	private static final String INSTANCE_VERIFICATION_STRING = "instance #1";
	
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

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 10, enabled = true)
	public void installAndUninstallTest() throws Exception {

		installAndUninstall(SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, false);
		installAndUninstall(SecurityConstants.USER_PWD_APP_MANAGER, SecurityConstants.USER_PWD_APP_MANAGER, false);
		installAndUninstall(SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, false);
		installAndUninstall(SecurityConstants.USER_PWD_CLOUD_ADMIN, SecurityConstants.USER_PWD_CLOUD_ADMIN, false);
		installAndUninstall(SecurityConstants.USER_PWD_VIEWER, SecurityConstants.USER_PWD_VIEWER, true);
		installAndUninstall(SecurityConstants.USER_PWD_NO_ROLE, SecurityConstants.USER_PWD_NO_ROLE, true);
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void installAndUninstallWithDifferentUsersTest() throws Exception {

		String output = "no output";
		
		// installing the "simple" application as Don, which has roles "appmanager" and "viewer". The auth-groups will be Don's group: "Cellcom".
		installApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, false, null);

		// attempting to uninstall the application as John, which has only the "viewer" role. Should fail!
		output = uninstallApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.USER_PWD_VIEWER, SecurityConstants.USER_PWD_VIEWER, true, null);
		assertTrue("uninstall access granted to " + SecurityConstants.VIEWER_DESCRIPTIN, output.contains(SecurityConstants.ACCESS_DENIED_MESSAGE));

		// attempting to uninstall the application as Dan, which has the "appmanager" role but is not in group "GE". Should fail!
		uninstallApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.USER_PWD_APP_MANAGER, SecurityConstants.USER_PWD_APP_MANAGER, true, null);
				
		// attempting to uninstall the service as Amanda, which has the "cloudadmin" role, and is in group "Cellcom". Should succeed.
		uninstallApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.USER_PWD_CLOUD_ADMIN, SecurityConstants.USER_PWD_CLOUD_ADMIN, false, null);
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void installWithoutCredentialsTest() throws IOException, InterruptedException{

		String output = "no output";
		output = installApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, null, null, true, null);

		assertTrue("install access granted to an Anonymous user" , output.contains(SecurityConstants.UNAUTHORIZED));
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void installingAndViewingTest() throws IOException, InterruptedException{

		// installing the "simple" application as Dan, which has the role "appmanager". The auth-groups will be Dan's group: "GE".
		installApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.USER_PWD_APP_MANAGER, SecurityConstants.USER_PWD_APP_MANAGER, false, null);
		// installing the "groovy" application as Don, which has roles "appmanager" and "viewer". The auth-groups will be Don's group: "Cellcom".
		installApplicationAndWait(GROOVY_APP_PATH, GROOVY_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, false, null);

		// checks if app "simple" can be viewed by Amanda, which has role "cloudadmin" and groups "Bezeq, GE, Cellcom". Expected - true.
		verifyVisibleLists(SecurityConstants.APP_MANAGER_DESCRIPTIN, SecurityConstants.USER_PWD_CLOUD_ADMIN, SecurityConstants.USER_PWD_CLOUD_ADMIN, SecurityConstants.CLOUD_ADMIN_DESCRIPTIN, SIMPLE_APP_NAME, true);
		// same check for app "groovy". Expected - true.
		verifyVisibleLists(SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, SecurityConstants.USER_PWD_CLOUD_ADMIN, SecurityConstants.USER_PWD_CLOUD_ADMIN, SecurityConstants.CLOUD_ADMIN_DESCRIPTIN, GROOVY_APP_NAME, true);
		
		// checks if app "simple" can be viewed by Dana, which has roles "cloudadmin" and "appmanager", and group "Bezeq". Expected - false, because Dana is not a member of GE.
		verifyVisibleLists(SecurityConstants.APP_MANAGER_DESCRIPTIN, SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_DESCRIPTION, SIMPLE_APP_NAME, false);
		// same check for app "groovy". Expected - false, because Dana is not a member of Cellcom.
		verifyVisibleLists(SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, SecurityConstants.CLOUD_ADMIN_AND_APP_MANAGER_DESCRIPTION, GROOVY_APP_NAME, false);
		
		// checks if app "simple" can be viewed by Dan, which has the role "appmanager", and group "GE". Expected - true.
		verifyVisibleLists(SecurityConstants.APP_MANAGER_DESCRIPTIN, SecurityConstants.USER_PWD_APP_MANAGER, SecurityConstants.USER_PWD_APP_MANAGER, SecurityConstants.APP_MANAGER_DESCRIPTIN, SIMPLE_APP_NAME, true);
		// same check for app "groovy". Expected - false, because Dan is not a member of Cellcom.
		verifyVisibleLists(SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, SecurityConstants.USER_PWD_APP_MANAGER, SecurityConstants.USER_PWD_APP_MANAGER, SecurityConstants.APP_MANAGER_DESCRIPTIN, GROOVY_APP_NAME, false);
		
		// checks if app "simple" can be viewed by Don, which has roles "appmanager" and "viewer", and group "Cellcom". Expected - false, because Don is not a member of GE.
		verifyVisibleLists(SecurityConstants.APP_MANAGER_DESCRIPTIN, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, SIMPLE_APP_NAME, false);
		// same check for app "groovy". Expected - true.
		verifyVisibleLists(SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, GROOVY_APP_NAME, true);
		
		// checks if app "simple" can be viewed by John, which has the role "viewer", and groups "GE, Cellcom". Expected - true.
		verifyVisibleLists(SecurityConstants.APP_MANAGER_DESCRIPTIN, SecurityConstants.USER_PWD_VIEWER, SecurityConstants.USER_PWD_VIEWER, SecurityConstants.VIEWER_DESCRIPTIN, SIMPLE_APP_NAME, true);
		// same check for app "groovy". Expected - true.
		verifyVisibleLists(SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, SecurityConstants.USER_PWD_VIEWER, SecurityConstants.USER_PWD_VIEWER, SecurityConstants.VIEWER_DESCRIPTIN, GROOVY_APP_NAME, true);
		
		// checks if app "simple" can be viewed by Jane, which has no roles, and is in group "Bezeq". Expected - false, because Jane does not have any role.
		verifyVisibleLists(SecurityConstants.APP_MANAGER_DESCRIPTIN, SecurityConstants.USER_PWD_NO_ROLE, SecurityConstants.USER_PWD_NO_ROLE, SecurityConstants.NO_ROLE_DESCRIPTIN, SIMPLE_APP_NAME, false);
		// same check for app "groovy". Expected - false, because Jane does not have any role.
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
		assertTrue("connect succeeded for user: " + SecurityConstants.USER_PWD_CLOUD_ADMIN + "bad", 
				output.contains(SecurityConstants.UNAUTHORIZED));			

	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void connectWithNoPasswordTest() throws IOException, InterruptedException {
		
		String output = connect(SecurityConstants.USER_PWD_CLOUD_ADMIN, null, true);		
		assertTrue("connect succeeded for: " + SecurityConstants.CLOUD_ADMIN_DESCRIPTIN + " without providing a password", 
				output.contains(SecurityConstants.UNAUTHORIZED));			
		
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void loginWithNonexistentUserTest() throws IOException, InterruptedException {

		String output = "no output";
		
		output = login(SecurityConstants.USER_PWD_CLOUD_ADMIN + "bad", SecurityConstants.USER_PWD_CLOUD_ADMIN, true);					

		assertTrue("login succeeded for user: " + SecurityConstants.USER_PWD_CLOUD_ADMIN + "bad", 
				output.contains(SecurityConstants.UNAUTHORIZED));			
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void connectWithWrongPassword() throws IOException, InterruptedException {

		String output = connect(SecurityConstants.USER_PWD_CLOUD_ADMIN, SecurityConstants.USER_PWD_CLOUD_ADMIN + "bad", true);		
		assertTrue("connect succeeded for password: " + SecurityConstants.USER_PWD_CLOUD_ADMIN + "bad", 
				output.contains(SecurityConstants.UNAUTHORIZED));			

	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void loginWithWrongPassword() throws IOException, InterruptedException {

		String output = "no output";
		
		output = login(SecurityConstants.USER_PWD_CLOUD_ADMIN, SecurityConstants.USER_PWD_CLOUD_ADMIN + "bad", true);
		
		assertTrue("login succeeded for password: " + SecurityConstants.USER_PWD_CLOUD_ADMIN + "bad", 
				output.contains(SecurityConstants.UNAUTHORIZED));
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void installWithWrongGroup() throws IOException, InterruptedException {
		
		String output = "no output";
		
		output = installApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, true, "ROLE_CLOUDADMINS");
		
		assertTrue("install succeeded with authGroup ROLE_CLOUDADMINS for: " + SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, output.contains(SecurityConstants.ACCESS_DENIED_MESSAGE));
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void installAndUninstallWithDifferentGroup() throws Exception {
		
		String output = "no output";
		
		// installing the "simple" application as Dana, which has roles "cloudadmin" and "appmanager". The auth-groups will be Dana's group: "Bezeq".
		installApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, SecurityConstants.USER_PWD_CLOUD_ADMIN_AND_APP_MANAGER, false, SecurityConstants.BEZEQ_GROUP);
		// attempting to uninstall the application as Don, which has roles "appmanager" and "viewer" but is not in group "Bezeq". Should fail!
		output = uninstallApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, SecurityConstants.USER_PWD_APP_MANAGER_AND_VIEWER, true, null);
		
		assertTrue("Application \"" + SIMPLE_APP_NAME + "\" was wrongfully visible to and uninstalled by " + SecurityConstants.APP_MANAGER_AND_VIEWER_DESCRIPTIN, output.contains(SecurityConstants.RESOURCE_NOT_FOUND));
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
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
				
		assertTrue("install access granted to viewer " + fakeCloudAdminUserAndPassword, output.contains(SecurityConstants.ACCESS_DENIED_MESSAGE));			
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
	
	public void installAndUninstall(String user, String password, boolean isInstallExpectedToFail) throws Exception {
		
		String output = installApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, user, password, isInstallExpectedToFail, null);
		
		if(isInstallExpectedToFail){
			assertTrue("application installation access granted to " + user, output.contains(SecurityConstants.ACCESS_DENIED_MESSAGE));
		}
		
		if(output.contains("Application " + SIMPLE_APP_NAME + " installed successfully")){			
			uninstallApplicationAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, user, password, isInstallExpectedToFail, null);
		}
				
		output = installServiceAndWait(SIMPLE_SERVICE_PATH, SIMPLE_SERVICE_NAME, TIMEOUT_IN_MINUTES, user, password, isInstallExpectedToFail, null);
		
		if(isInstallExpectedToFail){
			assertTrue("service installation access granted to " + user, output.contains(SecurityConstants.ACCESS_DENIED_MESSAGE));
		}
		
		if(output.contains("Service \"" + SIMPLE_SERVICE_NAME + "\" successfully installed")){			
			uninstallServiceAndWait(SIMPLE_APP_PATH, SIMPLE_APP_NAME, TIMEOUT_IN_MINUTES, user, password, isInstallExpectedToFail, null);
		}

	}

	@Override
	protected String getCloudName() {
		return "ec2-win";
	}

	@Override
	protected boolean isReusableCloud() {
		return false;
	}
}
