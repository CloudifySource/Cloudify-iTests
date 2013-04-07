package org.cloudifysource.quality.iTests.test.webui;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.quality.iTests.framework.tools.SGTestHelper;
import org.cloudifysource.quality.iTests.framework.utils.AssertUtils;
import org.cloudifysource.quality.iTests.framework.utils.AssertUtils.RepetitiveConditionProvider;
import org.cloudifysource.quality.iTests.framework.utils.LogUtils;
import org.cloudifysource.quality.iTests.framework.utils.ProcessingUnitUtils;
import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.AbstractLocalCloudTest;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverBackedSelenium;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import com.gigaspaces.webuitf.LoginPage;
import com.gigaspaces.webuitf.WebConstants;
import com.gigaspaces.webuitf.dashboard.DashboardTab;
import com.gigaspaces.webuitf.util.AjaxUtils;
import com.j_spaces.kernel.PlatformVersion;
import com.j_spaces.kernel.SystemProperties;
import com.thoughtworks.selenium.Selenium;


/**
 * This abstract class is the super class of all Selenium tests, every test class must inherit this class. 
 * Contains only annotated methods witch are invoked according to the annotation.
 * @author elip
 *
 */

public abstract class AbstractWebUILocalCloudTest extends AbstractLocalCloudTest {
	
	protected static final String DEFAULT_ACTIVEMQ_FULL_SERVICE_NAME = ServiceUtils.getAbsolutePUName(DEFAULT_APPLICATION_NAME, "activemq");
	protected static final String DEFAULT_CASSANDRA_FULL_SERVICE_NAME = ServiceUtils.getAbsolutePUName(DEFAULT_APPLICATION_NAME, "cassandra");
	protected static final String DEFAULT_HSQLDB_FULL_SERVICE_NAME = ServiceUtils.getAbsolutePUName(DEFAULT_APPLICATION_NAME, "hsqldb");
	protected static final String DEFAULT_SOLR_FULL_SERVICE_NAME = ServiceUtils.getAbsolutePUName(DEFAULT_APPLICATION_NAME, "solr");
	protected static final String DEFAULT_TOMCAT_SERVICE_FULL_NAME = ServiceUtils.getAbsolutePUName(DEFAULT_APPLICATION_NAME, "tomcat");
    private static final String WEBUI_REVERSE_PROXY_URL = "http://localhost/reverse-proxy-testing/Gs_webui.html";

    public static String METRICS_ASSERTION_SUFFIX = " metric that is defined in the dsl is not displayed in the metrics panel";
    
    protected static long waitingTime = 30000;

	private ChromeDriverService chromeService;

    private WebDriver driver;
    private Selenium selenium;
    private AjaxUtils helper;
    
    private final String defaultBrowser = 
    	(System.getProperty("selenium.browser") != null) ? System.getProperty("selenium.browser"): "Firefox";
    		
	@AfterMethod(alwaysRun = true)
	public void killWebServices() throws InterruptedException {
		stopWebBrowser();
		restorePreviousBrowser();
	}   
    
	@BeforeMethod
	public void setLocators() throws UnknownHostException {
		InetAddress localHost = InetAddress.getLocalHost();
		String hostAddress = localHost.getHostAddress();
		String locatorUrl = hostAddress + ":" + String.valueOf(CloudifyConstants.DEFAULT_LOCALCLOUD_LUS_PORT);
		System.setProperty( SystemProperties.JINI_LUS_LOCATORS, locatorUrl );
	}
	
	@BeforeMethod
	protected void startBrowser() throws InterruptedException {

        String isReverseProxy = System.getProperty("reverse.proxy");
        String url;

        if(isReverseProxy != null && isReverseProxy.equals("true")){
            url = WEBUI_REVERSE_PROXY_URL;
        }
        else{
            ProcessingUnit webui = admin.getProcessingUnits().waitFor("webui");
            ProcessingUnitUtils.waitForDeploymentStatus(webui, DeploymentStatus.INTACT);
            AbstractTestSupport.assertTrue(webui != null);
            AbstractTestSupport.assertTrue(webui.getInstances().length != 0);
            url = ProcessingUnitUtils.getWebProcessingUnitURL(webui, false).toString();
        }

		startWebBrowser(url);
		
	}
	
    private void startWebBrowser(String uRL) throws InterruptedException {
    	LogUtils.log("Launching browser...");
    	String browser = System.getProperty("selenium.browser");
    	LogUtils.log("Current browser is " + browser);
    	
    	for (int i = 0 ; i < 3 ; i++) {
    		try {
    			if (browser == null) {
    				driver = new FirefoxDriver();
    			}
    			else {
    				if (browser.equals("Firefox")) {
    					driver = new FirefoxDriver();

    				}
    				else {
    					if (browser.equals("IE")) {
    						DesiredCapabilities desired = DesiredCapabilities.internetExplorer();
    						desired.setCapability(InternetExplorerDriver.INTRODUCE_FLAKINESS_BY_IGNORING_SECURITY_DOMAINS, true);
    						driver = new InternetExplorerDriver(desired);
    					}
    					else {
    						DesiredCapabilities desired = DesiredCapabilities.chrome();
							desired.setCapability("chrome.switches", Arrays.asList("--start-maximized"));
							String chromeDriverExePath = SGTestHelper.getSGTestRootDir() + "/src/main/resources/webui/chromedriver.exe";
							chromeService = new ChromeDriverService.Builder().usingAnyFreePort().usingDriverExecutable(new File(chromeDriverExePath)).build();
							LogUtils.log("Starting Chrome Driver Server...");
							chromeService.start();
							driver = new RemoteWebDriver(chromeService.getUrl(), desired);
    					}
    				}
    			}
    			break;

    		}
    		catch (Exception e) {
    			LogUtils.log("Failed to lanch browser, retyring...Attempt number " + (i + 1));
    		}
    	}
    	if (driver == null) {
    		LogUtils.log("unable to lauch browser, test will fail on NPE");
    	}
    	
    	int seconds = 0;
    	
    	if (driver != null) {
        	driver.get(uRL);
        	if ((browser == null) || browser.equals("Firefox")) {
				maximize(); // this method is supported only on Firefox
        	}
        	selenium = new WebDriverBackedSelenium(driver, uRL);
        	helper = new AjaxUtils(driver);
        	Thread.sleep(3000);
        	while (seconds < 30) {
        		try {
        			helper.waitForElement(By.xpath(WebConstants.Xpath.loginButton), AjaxUtils.ajaxWaitingTime*2);
        			LogUtils.log("Web server connection established");
        			break;
        		}
        		catch (NoSuchElementException e) {
        			LogUtils.log("Unable to connect to Web server, retrying...Attempt number " + (seconds + 1));
        			driver.navigate().refresh();
        			Thread.sleep(1000);
        			seconds++;
        		}
        	}
        	if (seconds == 30) {
        		LogUtils.log("Could not establish a connection to webui server, Test will fail");
        	}
    	}
    }
    
	public LoginPage getLoginPage() {
		return new LoginPage(selenium, driver);
	}
    
    private void maximize() {
    	driver.manage().window().setSize(new Dimension(1280, 1024)); 
	}
    
    public void stopWebBrowser() throws InterruptedException {
    	LogUtils.log("Killing browser...");
    	
    	if(selenium != null){
	    	selenium.stop();
			selenium = null;
			Thread.sleep(1000);
    	}
		
		if (chromeService != null && chromeService.isRunning()) {
			LogUtils.log("Chrome Driver Server is still running, shutting it down...");
			chromeService.stop();
			chromeService = null;
		}
    }	
	
	public boolean verifyAlertThrown() {
		return selenium.isElementPresent(WebConstants.Xpath.okAlert);
	}
	
	public DashboardTab refreshPage() throws InterruptedException {
		driver.navigate().refresh();
		Thread.sleep(10000);
		return new DashboardTab(selenium, driver);
	}

	public void takeScreenShot(Class<?> cls, String testMethod, String picName) {
		
		if (!isDevMode()) {
			
			String suiteName = "webui-" + System.getProperty("selenium.browser");
	    	String browser = System.getProperty("selenium.browser");
	    	File scrFile = null;
	    	
			if((browser == null) || (!browser.equalsIgnoreCase("Chrome"))){
				scrFile = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);				
			}
			else{
				WebDriver augmentedDriver = new Augmenter().augment(driver);				
				scrFile = ((TakesScreenshot)augmentedDriver).getScreenshotAs(OutputType.FILE);				
			}
						
			String buildDir = SGTestHelper.getSGTestRootDir() + "/deploy/local-builds/build_" + PlatformVersion.getBuildNumber();

			String testLogsDir = cls.getName() + "." + testMethod + "()";

			String to = buildDir + "/" + suiteName + "/" + testLogsDir + "/" + picName + ".png";

			try {
				FileUtils.copyFile(scrFile, new File(to));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void setBrowser(String browser) {
		System.setProperty("selenium.browser", browser);
	}
	
	public void restorePreviousBrowser() {
		LogUtils.log("restoring browser setting to " + defaultBrowser);
		setBrowser(defaultBrowser);
	}
	
	public void repetitiveAssertTrueWithScreenshot(String message, RepetitiveConditionProvider condition, Class<?> cls, String methodName, String picName) {
		
		try {
			AssertUtils.repetitiveAssertTrue(null, condition, waitingTime);
		}
		catch (AssertionError err) {
			takeScreenShot(cls, methodName, picName);
			LogUtils.log(message, err);
			AssertUtils.assertFail("Test Failed");
		}
		
	}
	
	public void assertTrueWithScreenshot(boolean condition, Class<?> cls, String methodName, String picName) {
		
		try {
			AbstractTestSupport.assertTrue(condition);
		}
		catch (AssertionError err) {
			takeScreenShot(cls, methodName, picName);
			LogUtils.log("Stacktrace: ", err);
			AssertUtils.assertFail("Test Failed");
		}
		
	}
    
	public boolean isDevMode() {
		return SGTestHelper.isDevMode();
	}
}
