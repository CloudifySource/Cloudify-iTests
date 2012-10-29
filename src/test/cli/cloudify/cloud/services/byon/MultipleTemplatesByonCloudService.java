package test.cli.cloudify.cloud.services.byon;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.exec.util.StringUtils;

import framework.tools.SGTestHelper;
import framework.utils.LogUtils;

/**
 * This service provides a multi-template cloud driver.
 * you can query it for the template names and for which host is assigned to which template
 * @author elip
 *
 */
public class MultipleTemplatesByonCloudService extends ByonCloudService {
	
	/* holds a list of hosts assigned to a certain template */
	protected Map<String, List<String>> hostsPerTemplate = new HashMap<String, List<String>>();
		
	/* template names */
	protected static final String SMALL_LINUX = "SMALL_LINUX";
	protected static final String TEMPLATE_1 = "TEMPLATE_1";
	protected static final String TEMPLATE_2 = "TEMPLATE_2";
	protected static final String TEMPLATE_3 = "TEMPLATE_3";

	private static final int NUM_HOSTS_PER_TEMPLATE = 2;

	public MultipleTemplatesByonCloudService(String uniqueName) {
		super(uniqueName);
	}
	
	@Override
	public void injectAuthenticationDetails() throws IOException {
		super.injectAuthenticationDetails();
		
		List<String> assignableHosts = new ArrayList<String>(Arrays.asList(getMachines()));
		
		File multiTemplatesGroovy = new File(SGTestHelper.getSGTestRootDir()
				+ "/apps/cloudify/cloud/byon/byon-cloud.groovy");

		// replace the cloud groovy file with a customized one
		File fileToBeReplaced = new File(getPathToCloudFolder(), "byon-cloud.groovy");
		Map<File, File> filesToReplace = new HashMap<File, File>();
		filesToReplace.put(fileToBeReplaced, multiTemplatesGroovy);
		addFilesToReplace(filesToReplace);
		
		hostsPerTemplate.put(TEMPLATE_1, new ArrayList<String>());
		hostsPerTemplate.put(TEMPLATE_2, new ArrayList<String>());
		hostsPerTemplate.put(TEMPLATE_3, new ArrayList<String>());
				
		LogUtils.log("Assigning hosts for templates");
		for (String templateName : hostsPerTemplate.keySet()) {
			
			List<String> hostsForTemplate = new ArrayList<String>();
			for (int i = 0 ; i < NUM_HOSTS_PER_TEMPLATE ; i++) {
				String host = assignableHosts.iterator().next();
				if (host != null) {
					LogUtils.log("Host " + host + " was assigned to template " + templateName);
					hostsForTemplate.add(host);
					assignableHosts.remove(host);
				}
				
			}
			hostsPerTemplate.put(templateName, hostsForTemplate);
		}
		
		/* from the remaining hosts, construct the template for the management machine */
		List<String> managementTemplateHosts = new ArrayList<String>(); 
		for (String host : assignableHosts) {
			LogUtils.log("Host " + host + " was assigned to template " + SMALL_LINUX);
			managementTemplateHosts.add(host);
		}
		hostsPerTemplate.put(SMALL_LINUX, managementTemplateHosts);
		
		Map<String, String> props = new HashMap<String,String>();
		for (String template : hostsPerTemplate.keySet()) {
			props.put(template + "_HOSTS", StringUtils.toString(hostsPerTemplate.get(template).toArray(new String[] {}), ","));			
		}
		getAdditionalPropsToReplace().putAll(props);
	}
	
	public Map<String, List<String>> getHostsPerTemplate() {
		return hostsPerTemplate;
	}
	
	public String[] getTemlpateNames() {
		return new String [] {TEMPLATE_1, TEMPLATE_2, TEMPLATE_3};
	}
}