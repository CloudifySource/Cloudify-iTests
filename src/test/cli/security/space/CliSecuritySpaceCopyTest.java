package test.cli.security.space;

import static test.utils.LogUtils.log;

import java.util.Random;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
import org.openspaces.admin.space.SpaceInstance;
import org.openspaces.core.GigaSpace;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.cli.common.SpaceMessage;
import test.utils.CliUtils;

import com.gigaspaces.cluster.activeelection.SpaceMode;

/***
 * 
 * Topology: 1 GSA on 1 Machine, 1 GSM, 2 GSC's. (secured)
 * Deploy secure space 'A' partitioned 1,1 
 * Deploy secure space 'B' partitioned 1,1 
 * fill 'A' with 1000 'SpaceMessage' objects
 *
 * Tests: test 'space copy' from 'A' to 'B' with good credentials
 * bad credentials, no credentials, insufficient privileges
 * and verify expected result in space instances 
 * (i.e: object count in each space instance: primary and backup)
 * Moreover, in case of expected failure, verify expected console output.
 * 
 * @author Dan Kilman
 *
 */
public class CliSecuritySpaceCopyTest extends CliSecurityAbstractSpaceTest {

    ProcessingUnit datagrid1;
    ProcessingUnit datagrid2;
    GigaSpace aGigaSpace;
    GigaSpace bGigaSpace;
    GigaSpace aGigaSpaceBackup;
    GigaSpace bGigaSpaceBackup;

    @Override
    @BeforeMethod
    public void beforeTest() {
        
        super.beforeTest();
        
        log("Deploying spaces 'A' and 'B' partitioned 1,1");
        datagrid1 = gsm.deploy(new SpaceDeployment("A").partitioned(1, 1).maxInstancesPerVM(1).secured(true));
        datagrid2 = gsm.deploy(new SpaceDeployment("B").partitioned(1, 1).maxInstancesPerVM(1).secured(true));
        
        assertTrue(datagrid1.waitFor(datagrid1.getTotalNumberOfInstances()));
        assertTrue(datagrid2.waitFor(datagrid1.getTotalNumberOfInstances()));
        
        GigaSpace gigaSpace = datagrid1.getSpaces()[0].getGigaSpace();
       
        log("Filling space 'A' with 1000 SpaceMessage objects");
        Random random = new Random();
        for (int i=0; i<1000; i++) {
            gigaSpace.write(new SpaceMessage("SpaceMessage", random.nextInt(1000)));
        }
        
        SpaceInstance[] aSpaces = datagrid1.getSpace().getInstances();
        SpaceInstance[] bSpaces = datagrid2.getSpace().getInstances();
        
        SpaceInstance aSpacePrimaryInstance = aSpaces[0].getMode().equals(SpaceMode.PRIMARY) ? aSpaces[0] : aSpaces[1]; 
        SpaceInstance aSpaceBackupInstance = aSpaces[0].getMode().equals(SpaceMode.BACKUP) ? aSpaces[0] : aSpaces[1]; 
        SpaceInstance bSpacePrimaryInstance = bSpaces[0].getMode().equals(SpaceMode.PRIMARY) ? bSpaces[0] : bSpaces[1]; 
        SpaceInstance bSpaceBackupInstance = bSpaces[0].getMode().equals(SpaceMode.BACKUP) ? bSpaces[0] : bSpaces[1]; 

        aGigaSpace = aSpacePrimaryInstance.getGigaSpace();
        bGigaSpace = bSpacePrimaryInstance.getGigaSpace();
        aGigaSpaceBackup = aSpaceBackupInstance.getGigaSpace();
        bGigaSpaceBackup = bSpaceBackupInstance.getGigaSpace();
        

    }
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void testSpaceCopyWithGoodCredentials() {
        String[] args = { 
                "-user", "Master",
                "-password", "master",
                "space", "copy", 
                "jini://*/A_container1/A",
                "jini://*/B_container1/B" 
        };
        CliUtils.invokeGSMainOn(args);

        // Assert 'space copy'
        assertEquals(1000, aGigaSpace.count(null));
        assertEquals(1000, bGigaSpace.count(null));

        // nasty hack to count space backup instance
        try {
            CliUtils.assertCountInTarget(aGigaSpaceBackup.getSpace(), 1000, new SpaceMessage());
            CliUtils.assertCountInTarget(bGigaSpaceBackup.getSpace(), 0, new SpaceMessage());
        } catch (Exception e) {
            Assert.fail("While trying to count backup space instance", e);
        }
    }
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void testSpaceCopyWithNoCredentials() {
        String[] args = { 
                "space", "copy", 
                "jini://*/A_container1/A",
                "jini://*/B_container1/B" 
        };
        String mainOutput = CliUtils.invokeGSMainOn(args);

        String regex = "AuthenticationException";
        assertEquals("Testing: " + regex, 1, CliUtils.patternCounter(regex, mainOutput));
        
        // Assert 'space copy'
        assertEquals(1000, aGigaSpace.count(null));
        assertEquals(0, bGigaSpace.count(null));

        // nasty hack to count space backup instance
        try {
            CliUtils.assertCountInTarget(aGigaSpaceBackup.getSpace(), 1000, new SpaceMessage());
            CliUtils.assertCountInTarget(bGigaSpaceBackup.getSpace(), 0, new SpaceMessage());
        } catch (Exception e) {
            Assert.fail("While trying to count backup space instance", e);
        }
    }
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void testSpaceCopyWithBadCredentials() {
        String[] args = { 
                "-user", "NoSuchUser",
                "-password", "BadPassword",
                "space", "copy", 
                "jini://*/A_container1/A",
                "jini://*/B_container1/B" 
        };
        String mainOutput = CliUtils.invokeGSMainOn(args);
        
        String regex = "AuthenticationException";
        assertEquals("Testing: " + regex, 1, CliUtils.patternCounter(regex, mainOutput));
        
        regex = "BadCredentialsException";
        assertEquals("Testing: " + regex, 1, CliUtils.patternCounter(regex, mainOutput));
        
        // Assert 'space copy'
        assertEquals(1000, aGigaSpace.count(null));
        assertEquals(0, bGigaSpace.count(null));

        // nasty hack to count space backup instance
        try {
            CliUtils.assertCountInTarget(aGigaSpaceBackup.getSpace(), 1000, new SpaceMessage());
            CliUtils.assertCountInTarget(bGigaSpaceBackup.getSpace(), 0, new SpaceMessage());
        } catch (Exception e) {
            Assert.fail("While trying to count backup space instance", e);
        }
    }
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void testSpaceCopyWithInsufficientPrivileges() {
        String[] args = { 
                "-user", "Writer",
                "-password", "writer",
                "space", "copy", 
                "jini://*/A_container1/A",
                "jini://*/B_container1/B" 
        };
        String mainOutput = CliUtils.invokeGSMainOn(args);
        
        String regex = "AccessDeniedException";
        assertEquals("Testing: " + regex, 1, CliUtils.patternCounter(regex, mainOutput));
        
        regex = "User \\[Writer\\] lacks \\[Read\\] privileges";
        assertEquals("Testing: " + regex, 1, CliUtils.patternCounter(regex, mainOutput));
        
        // Assert 'space copy'
        assertEquals(1000, aGigaSpace.count(null));
        assertEquals(0, bGigaSpace.count(null));

        // nasty hack to count space backup instance
        try {
            CliUtils.assertCountInTarget(aGigaSpaceBackup.getSpace(), 1000, new SpaceMessage());
            CliUtils.assertCountInTarget(bGigaSpaceBackup.getSpace(), 0, new SpaceMessage());
        } catch (Exception e) {
            Assert.fail("While trying to count backup space instance", e);
        }
    }
    
}
