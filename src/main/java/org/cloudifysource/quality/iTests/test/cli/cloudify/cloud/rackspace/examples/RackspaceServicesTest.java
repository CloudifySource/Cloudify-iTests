package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.rackspace.examples;

import iTests.framework.utils.ScriptUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.AbstractServicesTest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class RackspaceServicesTest extends AbstractServicesTest{

	@Override
	protected String getCloudName() {
		return "rackspace";
	}

	@Override
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testSolr() throws Exception {
		super.testService(ScriptUtils.getBuildRecipesServicesPath() + "/solr", null);
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testActivemq() throws Exception{
		super.testService(ScriptUtils.getBuildRecipesServicesPath() + "/activemq", null);
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
	public void testHsqldb() throws Exception{
		super.testService(ScriptUtils.getBuildRecipesServicesPath() + "/hsqldb", null);
	}

    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
    public void testApacheLB() throws Exception {
        super.testService(ScriptUtils.getBuildRecipesServicesPath() + "/apacheLB", null, 30);
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
    public void testTomcat() throws Exception {
        super.testService(ScriptUtils.getBuildRecipesServicesPath() + "/tomcat", null);
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT * 2, enabled = true)
    public void testCassandra() throws Exception {
        super.testService(ScriptUtils.getBuildRecipesServicesPath() + "/cassandra", null);
    }

    //add mongo


    @Override
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}

}
