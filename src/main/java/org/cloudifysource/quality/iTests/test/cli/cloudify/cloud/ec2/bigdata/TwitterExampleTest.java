package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2.bigdata;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.quality.iTests.framework.tools.SGTestHelper;
import org.cloudifysource.quality.iTests.framework.utils.AssertUtils;
import org.cloudifysource.quality.iTests.framework.utils.IOUtils;
import org.cloudifysource.quality.iTests.framework.utils.LogUtils;
import org.cloudifysource.quality.iTests.framework.utils.SSHUtils;
import org.cloudifysource.quality.iTests.framework.utils.ScriptUtils;
import org.cloudifysource.quality.iTests.framework.utils.WebUtils;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.NewAbstractCloudTest;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;

public class TwitterExampleTest extends NewAbstractCloudTest {

	private String username = "tgrid";
	private String password = "tgrid";
	private String restUrl;
	private String devAppName;
	private String prodAppName;
	private final String applicationFolderName = "bigDataApp";
	private final String prodAppFolderName = "streaming-bigdata";
	private final String devAppFolderName = prodAppFolderName + "-dev";
	private final static int REPEATS = 3;
	private static final String GLOBAL_COUNTER_PROPERTY = "org.openspaces.bigdata.common.counters.GlobalCounter";
	private final static String ENTRIES_AMOUNT_REST_URL = "/admin/Spaces/Names/space/Spaces/Names/space/RuntimeDetails/CountPerClassName/" + GLOBAL_COUNTER_PROPERTY;
	
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
		prepareApplication();
	}

	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testTwitterDev() throws Exception {
		testTwitter(devAppFolderName, devAppName);
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testTwitterProd() throws Exception {
		testTwitter(prodAppFolderName, prodAppName);
	}
	
	private void testTwitter(String appFolderName, String appName) throws Exception {
		LogUtils.log("installing application " + appFolderName + " on " + this.getCloudName());
					
		installApplicationAndWait(getApplicationPath(appFolderName), appName);
		
		LogUtils.log("verifing successful installation");
		restUrl = getRestUrl();
		URL cassandraPuAdminUrl = new URL(restUrl + "/admin/ProcessingUnits/Names/big_data_app.cassandra");
		URL processorPuAdminUrl = new URL(restUrl + "/admin/ProcessingUnits/Names/big_data_app.processor");
		URL feederPuAdminUrl = new URL(restUrl + "/admin/ProcessingUnits/Names/big_data_app.feeder");

		AbstractTestSupport.assertTrue(WebUtils.isURLAvailable(cassandraPuAdminUrl));
		AbstractTestSupport.assertTrue(WebUtils.isURLAvailable(processorPuAdminUrl));
		AbstractTestSupport.assertTrue(WebUtils.isURLAvailable(feederPuAdminUrl));
		
		Client client = Client.create(new DefaultClientConfig());
		final WebResource service = client.resource(this.getRestUrl());
		String entriesString = service.path(ENTRIES_AMOUNT_REST_URL).get(String.class);
		Map<String, Object> entriesAmountJsonMap = jsonToMap(entriesString);
		String entriesAmountString = (String) entriesAmountJsonMap.get(GLOBAL_COUNTER_PROPERTY);
		int entriesAmount = Integer.parseInt(entriesAmountString);
		
		String newEntriesString;
		Map<String, Object> newEntriesAmountJsonMap;
		String newEntriesAmountString;
		int newEntriesAmount;
		
		for(int i = 0; i < REPEATS; i++){
			
			Thread.sleep(70000);
			
			newEntriesString = service.path(ENTRIES_AMOUNT_REST_URL).get(String.class);
			newEntriesAmountJsonMap = jsonToMap(newEntriesString);
			newEntriesAmountString = (String) newEntriesAmountJsonMap.get(GLOBAL_COUNTER_PROPERTY);			
			newEntriesAmount = Integer.parseInt(newEntriesAmountString);
			
			AssertUtils.assertTrue("TokenCounter entries are not written to the space. Entries in space: " + newEntriesAmount, newEntriesAmount > entriesAmount);
			
			entriesAmount = newEntriesAmount;
		}
		
		uninstallApplicationAndWait(appName);
		
		super.scanForLeakedAgentNodes();
	}

	@Override
	protected void beforeBootstrap() throws Exception {
		String suiteName = System.getProperty("iTests.suiteName");
		if(suiteName != null && "CLOUDIFY_XAP".equalsIgnoreCase(suiteName)){
			/* copy premium license to cloudify-overrides in order to run xap pu's */
			String overridesFolder = getService().getPathToCloudFolder() + "/upload/cloudify-overrides";
			File cloudifyPremiumLicenseFile = new File(SGTestHelper.getSGTestRootDir() + "/src/main/config/gslicense.xml");
			FileUtils.copyFileToDirectory(cloudifyPremiumLicenseFile, new File(overridesFolder));
		}				
	}

	@Override
	protected String getCloudName() {
		return "ec2";
	}

	@Override
	protected boolean isReusableCloud() {
		return false;
	}
	
	private static Map<String, Object> jsonToMap(final String response) throws IOException {
		final JavaType javaType = TypeFactory.type(Map.class);
		ObjectMapper om = new ObjectMapper();
		return om.readValue(response, javaType);
	}


	private void prepareApplication() throws IOException, InterruptedException, DSLException {
		final String recipesDir = getRecipesDir();
		
		File prodAppFolder = new File(recipesDir+ prodAppFolderName);
		String hostAddress = "127.0.0.1";
		SSHUtils.runCommand(hostAddress, AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2,
				"cd " + prodAppFolder + ";" + "mvn install", username, password);
		
		File devAppFolder = new File(recipesDir + devAppFolderName);
		FileUtils.copyDirectory(prodAppFolder, devAppFolder);
		replaceSpringProfilesActive(devAppFolder,"feeder", "twitter-feeder", "list-feeder");
		replaceSpringProfilesActive(devAppFolder,"processor", "cassandra-archiver,cassandra-discovery", "file-archiver");
		SSHUtils.runCommand(hostAddress, AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2,
				"cd " + devAppFolder + ";" + "mvn install", username, password);
		
		String applicationPath = getApplicationPath(devAppFolderName);
		File applicationDslFilePath = new File(applicationPath + "/bigDataApp-application.groovy");
		String appName = ServiceReader.getApplicationFromFile(applicationDslFilePath).getApplication().getName();
		devAppName = appName + "-dev";
		prodAppName = appName + "-prod";
	}

	private String getRecipesDir() {
		final String recipesDir = SGTestHelper.getBuildDir() + "/recipes/apps/";
		return recipesDir;
	}

	private String getApplicationPath(String appFolderName) {
		return getRecipesDir() + appFolderName + "/" + applicationFolderName;
	}

	/**
	 * Modify the service recipe to use a different spring profile
	 */
	private void replaceSpringProfilesActive(File appFolder, String module, String oldProfile, String newProfile) throws IOException {
		String recipeFile = appFolder+"/"+applicationFolderName+"/"+module+"/"+module+"-service.groovy";
		IOUtils.replaceTextInFile(recipeFile,
				"springProfilesActive \""+oldProfile+"\"", 
				"springProfilesActive \""+newProfile+"\"");
	}

}
