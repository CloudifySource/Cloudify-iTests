/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package test.cli.cloudify.cloud.ec2;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import test.cli.cloudify.cloud.AbstractScalingRulesCloudTest;
import framework.utils.LogUtils;
import framework.utils.TestUtils;


public class Ec2LocationAwareScalingRulesTest extends AbstractScalingRulesCloudTest{

	private static final String LOCATION_AWARE_POSTFIX = "-location-aware";
	private static final String NEWLINE = System.getProperty("line.separator");

	@Override
	protected String getCloudName() {
		return "ec2";// + LOCATION_AWARE_POSTFIX;
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 3, enabled = true)
	@Override
	public void testPetclinicSimpleScalingRules() throws Exception {		
		try {
			LogUtils.log("installing application " + getApplicationName());

			final String applicationPath = getApplicationPath();
			installApplicationAndWait(applicationPath, getApplicationName());

			repititiveAssertNumberOfInstances(getAbsoluteServiceName(), 2);


			// increase web traffic, wait for scale out
			startThreads();
			repititiveAssertNumberOfInstances(getAbsoluteServiceName(), 3);

			// stop web traffic, wait for scale in
			stopThreads();
			repititiveAssertNumberOfInstances(getAbsoluteServiceName(), 2);

			// Try to start a new machine and then cancel it.
			startThreads();
			executor.schedule(new Runnable() {

				@Override
				public void run() {
					stopThreads();

				}
			}, 60, TimeUnit.SECONDS);
			repetitiveNumberOfInstancesHolds(getAbsoluteServiceName(), 2, 500, TimeUnit.SECONDS);
		} finally {
			uninstallApplicationAndWait(getApplicationName());
		}
	}	
		
	@BeforeClass(alwaysRun = true)
	protected void bootstrap(final ITestContext testContext) {
		super.bootstrap(testContext);
	}
	
	@AfterClass(alwaysRun = true)
	protected void teardown() {
		super.teardown();
	}
	
	@BeforeTest
	public void beforeTest() {	
		cloneApplicaitonRecipeAndInjectLocationAware();
		super.beforeTest();	
	}

	
	
	private void cloneApplicaitonRecipeAndInjectLocationAware() {
		
		try {
			FileUtils.copyDirectory(new File(super.getApplicationPath()), new File(this.getApplicationPath()));
			final File newServiceFile = new File(this.getApplicationPath(),"tomcat/tomcat-service.groovy");
			TestUtils.writeTextFile(newServiceFile,
					"service {" +NEWLINE +
						"\textend \"../../../services/tomcat\""+NEWLINE+
						"\tlocationAware true"+NEWLINE+
						"\tnumInstances 2"+NEWLINE+        //initial total number of instances 2
						"\tminAllowedInstances 1"+NEWLINE+ //min 1 per zone
						"\tmaxAllowedInstances 2"+NEWLINE+ //max 2 per zone
					"}");
		} catch (final IOException e) {
			Assert.fail("Failed to create " + this.getApplicationPath(),e);
		}
	}
	
	@AfterTest
	public void afterTest() {
		super.afterTest();
	}
	
	@AfterMethod
	public void cleanUp() {
		//The afterTest Checks for leaks
		super.afterTest();
	}

	@Override
	protected String getApplicationPath() {
		final File applicationPath = new File(super.getApplicationPath());
		final File newApplicationPath = new File(applicationPath.getParentFile(), applicationPath.getName()+LOCATION_AWARE_POSTFIX);
		return newApplicationPath.getAbsolutePath();
	}
	
	
}
