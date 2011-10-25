package test.gsm.component.rebalancing.xen;

import org.openspaces.admin.Admin;
import org.openspaces.grid.gsm.rebalancing.RebalancingSlaEnforcement;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import test.gsm.AbstractXenGSMTest;
import test.utils.AdminUtils;

public class AbstractRebalancingSlaEnforcementXenTest extends AbstractXenGSMTest {

    protected static final int NUMBER_OF_OBJECTS = 100;
    protected final static String ZONE = "testzone";
    
    protected RebalancingSlaEnforcement rebalancing;
        
    @Override
    protected Admin newAdmin() {
        return AdminUtils.createSingleThreadAdmin();
    }
    
    @Override
    @BeforeMethod
    public void beforeTest() {
        super.beforeTest();
        rebalancing = new RebalancingSlaEnforcement();
        rebalancing.enableTracing();
    }

    @Override
    @AfterMethod
    public void afterTest() {
        try {
            rebalancing.destroy();
        }
        finally {
            super.afterTest();
        }
    }


}
