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

package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.rackspace.examples;

import org.cloudifysource.quality.iTests.test.AbstractTestSupport;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.AbstractExamplesTest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * This class runs two test on Rackspace cloud.
 * 
 * 1. bootstrap to cloud
 * 2. run travel
 * 3. uninstall travel.
 * 4. run petclinic
 * 5. uninstall petclinic
 * 6. teardown cloud
 * 7. scan for any leaked machines
 * 
 * @author elip
 *
 */
public class RackspaceExamplesTest extends AbstractExamplesTest {
	
	@Override
	protected String getCloudName() {
		return "rackspace";
	}

	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
	}
	
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testTravel() throws Exception {
		super.testTravel();
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testPetclinicSimple() throws Exception {
		super.testPetclinicSimple();
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testPetclinic() throws Exception {
		super.testPetclinic();
	}
	
	@Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testHelloWorld() throws Exception {
		super.testHelloWorld();
	}

    @Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testTravelChef() throws Exception {
        super.testTravelChef();
    }

    @Test(timeOut = AbstractTestSupport.DEFAULT_TEST_TIMEOUT * 4, enabled = true)
    public void testStatelessAndStateful() throws Exception {
        super.testStatelessAndStateful();
    }

	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}	
}
