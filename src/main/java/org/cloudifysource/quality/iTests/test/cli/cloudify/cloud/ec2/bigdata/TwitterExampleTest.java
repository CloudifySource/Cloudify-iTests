package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2.bigdata;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import javax.ws.rs.core.HttpHeaders;

import iTests.framework.utils.AssertUtils;
import iTests.framework.utils.LogUtils;
import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.ServiceReader;
import iTests.framework.tools.SGTestHelper;
import iTests.framework.utils.SSHUtils;
import iTests.framework.utils.WebUtils;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.NewAbstractCloudTest;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
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
	private static final String APPLICATION_FOLDER_NAME = "bigDataApp";
	private static final String PROD_APP_FOLDER_NAME = "streaming-bigdata";
	private static final String DEV_APP_FOLDER_NAME = PROD_APP_FOLDER_NAME + "-dev";
	private static final String GLOBAL_COUNTER_PROPERTY = "org.openspaces.bigdata.common.counters.GlobalCounter";
	private final static String ENTRIES_AMOUNT_REST_URL = "/admin/Spaces/Names/space/Spaces/Names/space/RuntimeDetails/CountPerClassName/" + GLOBAL_COUNTER_PROPERTY;
	private static final int EXPECTED_NUMBER_OF_UNIQUE_WORDS_IN_MOCK_TWEETS = 84;
	private static final String BIG_DATA_APP_APPLICATION_GROOVY = "bigDataApp-application.groovy";
	private static final String DEV_APP_OVERRIDE = SGTestHelper.getSGTestRootDir() + "/src/main/resources/apps/cloudify/recipes/"+PROD_APP_FOLDER_NAME+"-dev-override";
	
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
		testTwitter(DEV_APP_FOLDER_NAME, devAppName, false);
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testTwitterProd() throws Exception {
		testTwitter(PROD_APP_FOLDER_NAME, prodAppName, true);
	}
	
	private void testTwitter(String appFolderName, String appName, boolean isProduction) throws Exception {
		LogUtils.log("installing application " + appFolderName + " on " + this.getCloudName());
					
		installApplicationAndWait(getApplicationPath(appFolderName), appName);
		
		verifyApplicationInstallation(appName, isProduction);
		
		final Client client = Client.create(new DefaultClientConfig());
		final WebResource service = client.resource(this.getRestUrl());
		
		if (isProduction) {
			// weaker assert in production since we cannot rely on tweet feed to be active.
			AssertUtils.repetitiveAssertTrue("Expected GlobalCounter of at least one word", new AssertUtils.RepetitiveConditionProvider() {
				
				@Override
				public boolean getCondition() {
					final int numberOfGlobalCounters = getGlobalCounter(service);
					LogUtils.log("Number of global counters is " + numberOfGlobalCounters +". Expected bigger than 0");
					return numberOfGlobalCounters > 0;
				}
			}, OPERATION_TIMEOUT);
		}
		else {
			AssertUtils.repetitiveAssertTrue("Expected GlobalCounter to reach " + EXPECTED_NUMBER_OF_UNIQUE_WORDS_IN_MOCK_TWEETS, new AssertUtils.RepetitiveConditionProvider() {
				
				int prevNumberOfGlobalCounters = 0;
								
				@Override
				public boolean getCondition() {
					final int numberOfGlobalCounters = getGlobalCounter(service);
					LogUtils.log("Number of global counters is " + numberOfGlobalCounters +". Expected " + EXPECTED_NUMBER_OF_UNIQUE_WORDS_IN_MOCK_TWEETS);
					assertTrue("Number of global counters is not expected to decrease. prevNumberOfGlobalCounters=" + prevNumberOfGlobalCounters + " numberOfGlobalCounters="+numberOfGlobalCounters,
							   prevNumberOfGlobalCounters < numberOfGlobalCounters);
					prevNumberOfGlobalCounters = numberOfGlobalCounters;
					return numberOfGlobalCounters == EXPECTED_NUMBER_OF_UNIQUE_WORDS_IN_MOCK_TWEETS;
				}
			}, OPERATION_TIMEOUT);
		}
		
		uninstallApplicationAndWait(appName);
		
		super.scanForLeakedAgentNodes();
	}

	private void verifyApplicationInstallation(String appName, boolean isProduction) throws MalformedURLException,
			Exception {
		LogUtils.log("verifing successful installation");
		restUrl = getRestUrl();

		if (isProduction) {
			final URL cassandraPuAdminUrl = new URL(restUrl + "/admin/ProcessingUnits/Names/"+appName+".cassandra");
			AbstractTestSupport.assertTrue(WebUtils.isURLAvailable(cassandraPuAdminUrl));
		}
		final URL processorPuAdminUrl = new URL(restUrl + "/admin/ProcessingUnits/Names/"+appName+".processor");
		AbstractTestSupport.assertTrue(WebUtils.isURLAvailable(processorPuAdminUrl));
		
		final URL feederPuAdminUrl = new URL(restUrl + "/admin/ProcessingUnits/Names/"+appName+".feeder");
		AbstractTestSupport.assertTrue(WebUtils.isURLAvailable(feederPuAdminUrl));
	}

	private int getGlobalCounter(final WebResource service)  {
		final String newEntriesString = service.path(ENTRIES_AMOUNT_REST_URL).header(HttpHeaders.CACHE_CONTROL, "no-cache").get(String.class);
		LogUtils.log("newEntriesString = " + newEntriesString);
		Map<String, Object> newEntriesAmountJsonMap;
		try {
			newEntriesAmountJsonMap = jsonToMap(newEntriesString);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		final String newEntriesAmountString = (String) newEntriesAmountJsonMap.get(GLOBAL_COUNTER_PROPERTY);			
		final int newEntriesAmount = Integer.parseInt(newEntriesAmountString);
		return newEntriesAmount;
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
		
		
		File prodAppFolder = new File(recipesDir+ PROD_APP_FOLDER_NAME);
		String hostAddress = "127.0.0.1";

		SSHUtils.runCommand(hostAddress, AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2,
				"cd " + prodAppFolder + ";" + "mvn install", username, password);

		final File devAppFolder = new File(recipesDir + DEV_APP_FOLDER_NAME);
		
		FileUtils.copyDirectory(prodAppFolder, devAppFolder);
		FileUtils.copyDirectory(new File(DEV_APP_OVERRIDE), new File(devAppFolder, APPLICATION_FOLDER_NAME));
		
		SSHUtils.runCommand(hostAddress, AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2,
				"cd " + devAppFolder + ";" + "mvn install", username, password);

		String appName = ServiceReader.getApplicationFromFile(getApplicationDslFile(devAppFolder)).getApplication().getName();
		devAppName = appName + "-dev";
		prodAppName = appName + "-prod";
	}

	private File getApplicationDslFile(File appFolder) {
		final String applicationPath = getApplicationPath(DEV_APP_FOLDER_NAME);
		final File applicationDslFilePath = new File(applicationPath + "/"+ BIG_DATA_APP_APPLICATION_GROOVY);
		return applicationDslFilePath;
	}

	private String getRecipesDir() {
		final String recipesDir = SGTestHelper.getBuildDir() + "/recipes/apps/";
		return recipesDir;
	}

	private String getApplicationPath(String appFolderName) {
		return getRecipesDir() + appFolderName + "/" + APPLICATION_FOLDER_NAME;
	}
}
