package test.cli.cloudify;

import java.io.IOException;
import java.util.Map;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.pu.service.ServiceDetails;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.gigaspaces.cloudify.dsl.utils.ServiceUtils;

import framework.tools.SGTestHelper;

public class ConfigSlurperTest extends AbstractCommandTest {
	
	@Test
	public void test() throws IOException, InterruptedException{
		String serviceDir = SGTestHelper.getSGTestRootDir().replace("\\", "/") + "/apps/USM/usm/slurper";
		String command = "connect " + restUrl + ";install-service --verbose " + serviceDir + ";exit";

			runCommand(command);

			ProcessingUnit pu = admin.getProcessingUnits().waitFor(ServiceUtils.getAbsolutePUName(DEFAULT_APPLICTION_NAME, "slurper"));
			
			Map<String, ServiceDetails> serviceDetails = pu.getInstances()[0].getServiceDetailsByServiceId();
			Map<String, Object> attributes = serviceDetails.get("USM").getAttributes();
			Assert.assertTrue(attributes.get("icon").equals("icon.png"));
			Assert.assertTrue(attributes.get("jmx-port").toString().equals("9999"));
			Assert.assertTrue(attributes.get("name").equals("slurper"));
			Assert.assertTrue(attributes.get("type").equals("WEB_SERVER"));
			Assert.assertTrue(attributes.get("tripletype").equals("WEB_SERVERWEB_SERVERWEB_SERVER"));
			Assert.assertTrue(attributes.get("placeholderProp").toString().equals("string"));
			
			if(admin.getMachines().getMachines()[0].getOperatingSystem().getDetails().getName().equals("Linux"))
				Assert.assertTrue(attributes.get("systemProp").toString().equals(":"));
			else
				Assert.assertTrue(attributes.get("systemProp").toString().equals(";"));
			
			
			command = "connect " + restUrl + ";" + "uninstall-service slurper;exit";
			runCommand(command);

	

	}
}
