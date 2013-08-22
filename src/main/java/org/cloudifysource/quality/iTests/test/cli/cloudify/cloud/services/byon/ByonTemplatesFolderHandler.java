/*
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * *****************************************************************************
 */
package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.services.byon;

import iTests.framework.utils.IOUtils;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.internal.DSLUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.CommandTestUtils;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.templates.TemplateDetails;
import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.templates.TemplatesFolderHandler;

public class ByonTemplatesFolderHandler extends TemplatesFolderHandler {
	private final String TEMPLATES_ROOT_PATH = CommandTestUtils.getPath("src/main/resources/templates");
	private final String BASIC_TEMPLATE_FILE_NAME = "byon_basic_template";
	private final String BOOTSTRAP_MANAGEMENT_FILE_NAME = "byon-bootstrap-management.sh";
	
	private static final String UPLOAD_PROPERTY_NAME = "uploadDir";
	private static final String NODE_IP_PROPERTY_NAME = "node_ip";
	private static final String NODE_ID_PROPERTY_NAME = "node_id";

	private final AtomicInteger numOfMachinesInUse;
	private final String[] machines;

	public ByonTemplatesFolderHandler(final File folder, final int numOfMngMachines, final String[] machines) {
		super(folder);
		numOfMachinesInUse = new AtomicInteger(numOfMngMachines);
		this.machines = machines;
	}

	@Override
	public void updatePropertiesFile(final TemplateDetails template) {
		final Properties props = new Properties();
		if (template.getUploadDirName() != null) {
			props.put(UPLOAD_PROPERTY_NAME, template.getUploadDirName());
		}
		String machineIP = template.getMachineIP();
		if (machineIP == null) {
			machineIP = getNextMachineIP(template.isForServiceInstallation());
			template.setMachineIP(machineIP);
		}
		props.put(NODE_IP_PROPERTY_NAME, machineIP);
		props.put(NODE_ID_PROPERTY_NAME, "byon-pc-lab-" + template.getMachineIP() + "{0}");
		File templatePropsFile = template.getTemplatePropertiesFile();
		if (templatePropsFile == null) {
			final String templateFileName = template.getTemplateFile().getName();
			final int templateFileNamePrefixEndIndex = templateFileName.indexOf(".");
			final String templateFileNamePrefix = templateFileName.substring(0, templateFileNamePrefixEndIndex);
			final String proeprtiesFileName = templateFileNamePrefix + DSLUtils.PROPERTIES_FILE_SUFFIX;
			templatePropsFile = new File(folder, proeprtiesFileName);
			template.setTemplatePropertiesFile(templatePropsFile);
		} else {
			if (!templatePropsFile.exists()) {
				templatePropsFile = new File(getFolder(), templatePropsFile.getName());
			} else if (!folder.equals(templatePropsFile.getParentFile())){
				try {
					FileUtils.copyFileToDirectory(templatePropsFile, folder);
				} catch (IOException e) {
					Assert.fail("failed to copy properties file [" + templatePropsFile.getAbsolutePath() + "] to directory [" 
							+ getFolder().getAbsolutePath() + "]. error was: " + e.getMessage());
				}
			}
		}
		try {
			IOUtils.writePropertiesToFile(props, templatePropsFile);
		} catch (IOException e) {
			Assert.fail("failed to write properties to file [" + templatePropsFile.getAbsolutePath());
		}
	}

	private String getNextMachineIP(final boolean forServiceInstallation) {
		if (forServiceInstallation) {
			final int nextMachine = numOfMachinesInUse.getAndIncrement();
			if (machines.length <= nextMachine) {
				Assert.fail("Cannot allocate machine number " + nextMachine + ", there are only " + machines.length
						+ " machines to use.");
			}
			return machines[nextMachine];
		}
		return machines[0];
	}

	@Override
	public File getBasicTemplateFile() {
		return new File(TEMPLATES_ROOT_PATH, BASIC_TEMPLATE_FILE_NAME);
	}

	@Override
	public File getBasicBootstrapManagementFile() {
		return new File(TEMPLATES_ROOT_PATH, BOOTSTRAP_MANAGEMENT_FILE_NAME);
	}
}