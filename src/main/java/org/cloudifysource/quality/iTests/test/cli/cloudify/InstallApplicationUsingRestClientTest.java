package org.cloudifysource.quality.iTests.test.cli.cloudify;

import iTests.framework.utils.AssertUtils;
import iTests.framework.utils.LogUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.cloudifysource.dsl.Application;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.DSLReader;
import org.cloudifysource.dsl.internal.DSLUtils;
import org.cloudifysource.dsl.internal.CloudifyConstants.DeploymentState;
import org.cloudifysource.dsl.internal.packaging.Packager;
import org.cloudifysource.dsl.internal.packaging.PackagingException;
import org.cloudifysource.dsl.rest.request.InstallApplicationRequest;
import org.cloudifysource.dsl.rest.response.ApplicationDescription;
import org.cloudifysource.dsl.rest.response.UploadResponse;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.cloudifysource.shell.exceptions.CLIException;
import org.cloudifysource.shell.rest.RestAdminFacade;
import org.testng.annotations.Test;

import com.j_spaces.kernel.PlatformVersion;

/**
 * unstall application on localcloud using the rest API
 * 
 * @author adaml
 *
 */
public class InstallApplicationUsingRestClientTest extends AbstractLocalCloudTest {
	private static final String APPLICATION_NAME = "simple";
	private String APPLICATION_FOLDER_PATH = "D:/Users/adaml/Documents/gitWorkspace/Cloudify-iTests/src/" +
						"main/resources/apps/USM/usm/applications/simple";
	private static final int INSTALL_TIMEOUT_MILLIS = 60 * 15 * 1000;
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, groups = "1")
    public void testApplicationInstall() 
    		throws IOException, PackagingException, 
    		DSLException, RestClientException, CLIException {
		final String version = PlatformVersion.getVersion();
		final URL url = new URL(restUrl);
		final RestClient client = new RestClient(url, "", "", version);
		client.connect();
		
		final File appFolder = new File(APPLICATION_FOLDER_PATH);
		final DSLReader dslReader = createDslReader(appFolder);
		final Application application = dslReader.readDslEntity(Application.class);
		final File packedFile = Packager.packApplication(application, appFolder);
		final UploadResponse uploadResponse = client.upload(packedFile.getName(), packedFile);
		final String uploadKey = uploadResponse.getUploadKey();
		
		InstallApplicationRequest request = new InstallApplicationRequest();
		request.setApplcationFileUploadKey(uploadKey);

		//Test will run in unsecured mode.
		request.setAuthGroups("");
		//no debugging.
		request.setDebugAll(false);
		request.setSelfHealing(true);
		request.setApplicationName(APPLICATION_NAME);
		//set timeout
		request.setTimeoutInMillis(INSTALL_TIMEOUT_MILLIS);
		
		//make install service API call
		client.installApplication(APPLICATION_NAME, request);
		//wait for the application to reach STARTED state.
		waitForApplicationInstall();
		
		//make un-install service API call
		client.uninstallApplication(APPLICATION_NAME);
		//wait for the application to be removed.
		waitForApplicationUninstall();
		
	}
	
	void waitForApplicationInstall()
			throws CLIException, RestClientException {
		final RestAdminFacade adminFacade = new RestAdminFacade();
		adminFacade.connect(null, null, restUrl.toString(), false);
		
		LogUtils.log("Waiting for application deployment state to be " + DeploymentState.STARTED) ;
		AssertUtils.repetitiveAssertTrue(APPLICATION_NAME + " application failed to deploy", new AssertUtils.RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				try {
					final List<ApplicationDescription> applicationDescriptionsList = adminFacade.getApplicationDescriptionsList();
					for (ApplicationDescription applicationDescription : applicationDescriptionsList) {
						if (applicationDescription.getApplicationName().equals(APPLICATION_NAME)) {
							if (applicationDescription.getApplicationState().equals(DeploymentState.STARTED)) {
								return true;
							}
						}
					}
				} catch (final CLIException e) {
					e.printStackTrace();
				}
				return false;
			}
		} , AbstractTestSupport.OPERATION_TIMEOUT * 3);
	}
	
	void waitForApplicationUninstall()
			throws CLIException, RestClientException {
		final RestAdminFacade adminFacade = new RestAdminFacade();
		adminFacade.connect(null, null, restUrl.toString(), false);
		
		LogUtils.log("Waiting for USM_State to be " + CloudifyConstants.USMState.RUNNING);
		AssertUtils.repetitiveAssertTrue("uninstall failed for application " + APPLICATION_NAME, new AssertUtils.RepetitiveConditionProvider() {
			@Override
			public boolean getCondition() {
				try {
					final List<ApplicationDescription> applicationDescriptionsList = adminFacade.getApplicationDescriptionsList();
					for (ApplicationDescription applicationDescription : applicationDescriptionsList) {
						if (applicationDescription.getApplicationName().equals(APPLICATION_NAME)) {
							return false;
						}
					}
					return true;
				} catch (final CLIException e) {
					LogUtils.log("Failed getting application list.");
				}
				return false;
			}
		} , AbstractTestSupport.OPERATION_TIMEOUT * 3);
	}
	
	private DSLReader createDslReader(final File applicationFile) {
		final DSLReader dslReader = new DSLReader();
		final File dslFile = DSLReader.findDefaultDSLFile(DSLUtils.APPLICATION_DSL_FILE_NAME_SUFFIX, applicationFile);
		dslReader.setDslFile(dslFile);
		dslReader.setCreateServiceContext(false);
		dslReader.addProperty(DSLUtils.APPLICATION_DIR, dslFile.getParentFile().getAbsolutePath());
		return dslReader;
	}

}
