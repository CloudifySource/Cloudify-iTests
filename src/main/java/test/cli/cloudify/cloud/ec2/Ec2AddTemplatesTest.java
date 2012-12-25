package test.cli.cloudify.cloud.ec2;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import junit.framework.Assert;

import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.NodeMetadata;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import test.cli.cloudify.CommandTestUtils;
import test.cli.cloudify.cloud.NewAbstractCloudTest;
import test.cli.cloudify.cloud.services.ec2.Ec2CloudService;
import framework.utils.IOUtils;
import framework.utils.JCloudsUtils;
import framework.utils.LogUtils;

/**
 * 
 * @author yael
 *
 */
public class Ec2AddTemplatesTest extends NewAbstractCloudTest {

	private final String SERVICE_FOLDER_PATH = CommandTestUtils.getPath("src/main/resources/apps/USM/usm/services/simpleWithTemplates/simpleForEC2");
	private final String SERVICE_NAME = "simpleForEC2";
	private final String TEMPLATE_FOLDER_PATH = SERVICE_FOLDER_PATH + "/templates";
	private final String TEMPLATE_NAME = "UBUNTU_TEST";
	private final String TEMPLATE_PROPERTIES_FILE_PATH = TEMPLATE_FOLDER_PATH + "/ubuntu-template.properties";
	private final String UBUNTU_IMAGE_ID = "us-east-1/ami-82fa58eb";



	@Override
	@BeforeClass(alwaysRun = true)
	protected void bootstrap() throws Exception {
		super.bootstrap();
	}

	@Override
	@AfterClass(alwaysRun = true)
	protected void teardown() throws Exception {
		super.teardown();
	}

	@Override
	protected String getCloudName() {
		return "ec2";
	}

	@Override
	protected boolean isReusableCloud() {
		return false;
	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT * 4, enabled = true)
	public void testAddTemplateAndInstallService() 
			throws IOException, InterruptedException {	

		// update properties
		updatePropertiesFile();

		// add templates
		String command = "connect " + getRestUrl() + ";add-templates " + TEMPLATE_FOLDER_PATH;
		String output = CommandTestUtils.runCommandAndWait(command);
		Assert.assertTrue(output.contains("Templates added successfully"));
		Assert.assertTrue(output.contains(TEMPLATE_NAME));

		//install service
		installServiceAndWait(SERVICE_FOLDER_PATH, SERVICE_NAME);
		assertImageID(UBUNTU_IMAGE_ID);
		
		uninstallServiceIfFound(SERVICE_NAME);	
		// remove templates
		command = "connect " + getRestUrl() + ";remove-template " + TEMPLATE_NAME;
		output = CommandTestUtils.runCommandAndWait(command);
		Assert.assertTrue(output.contains("Template " + TEMPLATE_NAME + " removed successfully"));
		assertTempalteRemoved(TEMPLATE_NAME);
	}

	private void assertImageID(String expectedImageID) {
		JCloudsUtils.createContext(getService());
		Set<? extends ComputeMetadata> allNodes = JCloudsUtils.getAllNodes();
		List<String> foundImages = new LinkedList<String>();
		for (ComputeMetadata computeMetadata : allNodes) {
			NodeMetadata nodeMetadata = (NodeMetadata)computeMetadata;
			String imageId = nodeMetadata.getImageId();
			foundImages.add(imageId);
			if (expectedImageID.equals(imageId)) {
				return;
			}
		}
		Assert.fail("Expecting to find image id " + expectedImageID + ", but found " + foundImages);
		
	}

	private void updatePropertiesFile() throws IOException {
		Ec2CloudService service = (Ec2CloudService)getService();
		File propsFile = new File(TEMPLATE_PROPERTIES_FILE_PATH);		
		Properties props = new Properties();
		props.put("keyPair", '"' + service.getKeyPair() + '"');
		props.put("keyFile", '"' + service.getKeyPair() + ".pem" + '"');
		props.put("hardwareId", '"' + "m1.small" + '"');
		props.put("locationId", '"' + "us-east-1" + '"');
		props.put("ubuntuImageId", '"' + "us-east-1/ami-82fa58eb" + '"');
		IOUtils.writePropertiesToFile(props, propsFile);
	}


	private void assertTempalteRemoved(String templateName) {
		String command = "connect " + getRestUrl() + ";list-templates";
		try {
			String output = CommandTestUtils.runCommandAndWait(command);
			Assert.assertFalse(output.contains(templateName));

		} catch (final Exception e) {
			LogUtils.log(e.getMessage(), e);
			Assert.fail(e.getMessage());
		}
	}

}
