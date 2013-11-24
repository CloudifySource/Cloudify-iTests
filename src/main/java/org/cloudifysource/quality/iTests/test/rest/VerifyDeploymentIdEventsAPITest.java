 /* Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/
package org.cloudifysource.quality.iTests.test.rest;

 import junit.framework.Assert;
 import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
 import org.cloudifysource.dsl.rest.response.ServiceDescription;
 import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
 import org.cloudifysource.quality.iTests.test.cli.cloudify.AbstractLocalCloudTest;
 import org.cloudifysource.quality.iTests.test.cli.cloudify.util.NewRestTestUtils;
 import org.cloudifysource.restclient.RestClient;
 import org.cloudifysource.restclient.exceptions.RestClientException;
 import org.testng.annotations.Test;

 import java.util.List;

/**
 * Tests the verification of deploymentId in events API.
 * 
 * If a wrong (not exist) deployment id is passed to one of the events API methods, 
 * a ResourceNotFoundException should be thrown.
 * 
 * @author yael
 *
 */
public class VerifyDeploymentIdEventsAPITest extends AbstractLocalCloudTest {

	private static final String NOT_EXIST_DEPLOYMENT_ID = "not-exist-deployment-id";
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
    public void getLastEventsTest() throws Exception {
	       RestClient restClient = NewRestTestUtils.createAndConnect(restUrl);
			try {
				restClient.getLastEvent(NOT_EXIST_DEPLOYMENT_ID);
				Assert.fail("RestClientException was expected.");
			} catch (RestClientException e) {
				Assert.assertEquals(CloudifyMessageKeys.MISSING_RESOURCE.getName(), e.getMessageCode());
			} 
    }
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
    public void getDeploymentEventsTest() throws Exception {
	       RestClient restClient = NewRestTestUtils.createAndConnect(restUrl);
			try {
				restClient.getDeploymentEvents(NOT_EXIST_DEPLOYMENT_ID, 0, -1);
				Assert.fail("RestClientException was expected.");
			} catch (RestClientException e) {
				Assert.assertEquals(CloudifyMessageKeys.MISSING_RESOURCE.getName(), e.getMessageCode());
			} 
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 2, enabled = true)
    public void getServiceDescriptionsTest() throws Exception {
	       RestClient restClient = NewRestTestUtils.createAndConnect(restUrl);
			try {
                List<ServiceDescription> serviceDescriptions =
                        restClient.getServiceDescriptions(NOT_EXIST_DEPLOYMENT_ID);
                Assert.fail("RestClientException was expected. but instead got " + serviceDescriptions);
			} catch (RestClientException e) {
				Assert.assertEquals(CloudifyMessageKeys.MISSING_RESOURCE.getName(), e.getMessageCode());
			} 
    }
}
