package org.cloudifysource.quality.iTests.test.cli.cloudify.recipes.inheritence;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.cloudifysource.domain.Application;
import org.cloudifysource.domain.Service;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.packaging.PackagingException;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.AbstractLocalCloudTest;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.testng.annotations.Test;

import com.gigaspaces.log.AllLogEntryMatcher;
import com.gigaspaces.log.ContinuousLogEntryMatcher;
import com.gigaspaces.log.LogEntries;
import com.gigaspaces.log.LogEntry;
import com.gigaspaces.log.LogProcessType;

public class RecipeInheritenceTest extends AbstractLocalCloudTest {

    private Application app;


    /**
     * Install a version of tomcat where the default port is overridden.
     * Instead of 8080 we use 9876.
     * @throws PackagingException
     * @throws IOException
     * @throws InterruptedException
     * @throws DSLException
     */
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
    public void overrideTomcatPortTest() throws PackagingException, IOException, InterruptedException, DSLException {
    	app =  installApplication("travelExtendedTomcatPortOverride");
        Service s1 = app.getServices().get(0);
        Service s2 = app.getServices().get(1);
        Service tomcat = s1.getName().equals("tomcat") ? s1 : s2;
        int tomcatChildPort = tomcat.getNetwork().getPort();
        assertEquals("tomcat's child port was not overridden", 9876, tomcatChildPort);
        assertTrue(ServiceUtils.isPortOccupied(9876));
        assertTrue(ServiceUtils.isPortFree(8080));
        uninstallApplication("travelExtendedTomcatPortOverride");
    }

    /**
     * Install a version of tomcat where the number of instances in overridden.
     * @throws PackagingException
     * @throws IOException
     * @throws InterruptedException
     * @throws DSLException
     */
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
    public void overrideTomcatNumInstancesTest() throws PackagingException, IOException, InterruptedException, DSLException {
       app=  installApplication("travelExtendedTomcatNumInstancesOverride");

        int tomcatInstances = admin.getProcessingUnits().getProcessingUnit("travelExtendedTomcatNumInstancesOverride.tomcat").getInstances().length;
        assertEquals("tomcat instances where overridden to be 3", 3, tomcatInstances);
        uninstallApplication("travelExtendedTomcatNumInstancesOverride");
    }

    /**
     * Install a version of cassandra where the poststart script is overridden.
     * @throws PackagingException
     * @throws IOException
     * @throws InterruptedException
     * @throws DSLException
     */
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
    public void overrideCassandraInitFileTest() throws PackagingException, IOException, InterruptedException, DSLException {
        String EXPECTED_PROCESS_PRINTOUTS = "THIS IS OVERRIDED CASSANDRA_POSTSTART.GROOVY";
        app = installApplication("travelExtended");

        ProcessingUnit processingUnit = admin.getProcessingUnits().getProcessingUnit("travelExtended.cassandra-extend");
        assertNotNull("Processsing unit not found", processingUnit);

        boolean found = processingUnit.waitFor(1, 5, TimeUnit.MINUTES);
        assertTrue("PU Instance not found", found);
        ProcessingUnitInstance cassandraInstance = processingUnit.getInstances()[0];

        long pid = cassandraInstance.getGridServiceContainer().getVirtualMachine().getDetails().getPid();

        ContinuousLogEntryMatcher matcher = new ContinuousLogEntryMatcher(new AllLogEntryMatcher(), new AllLogEntryMatcher());

        sleep(5000);
        assertTrue(checkForOverrideString(cassandraInstance, pid, matcher, EXPECTED_PROCESS_PRINTOUTS));
        uninstallApplication("travelExtended");
    }

    private boolean checkForOverrideString(ProcessingUnitInstance pui,
                                           long pid, ContinuousLogEntryMatcher matcher, final String expectedValue) {
        LogEntries entries = pui.getGridServiceContainer()
                .getGridServiceAgent()
                .logEntries(LogProcessType.GSC, pid, matcher);
        for (LogEntry logEntry : entries) {
            String text = logEntry.getText();
            if (text.contains(expectedValue)) {
                return true;
            }
        }
        return false;
    }
}
