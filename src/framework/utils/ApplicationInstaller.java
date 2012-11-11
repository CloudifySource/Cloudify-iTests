package framework.utils;

import junit.framework.Assert;
import test.cli.cloudify.CommandTestUtils;

public class ApplicationInstaller extends RecipeInstaller {

	private String applicationName; 
	
	public ApplicationInstaller(String restUrl, String applicationName) {
		super(restUrl);
		this.applicationName = applicationName;
	}

	@Override
	public String getInstallCommand() {
		return "install-application";
	}

	@Override
	public void assertInstall(String output) {
		final String excpectedResult = "Application " + applicationName + " installed successfully";
		if (!isExpectToFail()) {
			AssertUtils.assertTrue(output.toLowerCase().contains(excpectedResult.toLowerCase()));
		} else {
			AssertUtils.assertTrue(output.toLowerCase().contains("operation failed"));
		}
		
	}

	@Override
	public String getUninstallCommand() {
		return "uninstall-application";
	}

	@Override
	public String getRecipeName() {
		return applicationName;
	}

	@Override
	public void assertUninstall(String output) {
		final String excpectedResult = "Application " + applicationName + " uninstalled successfully";
		AssertUtils.assertTrue(output.toLowerCase().contains(excpectedResult.toLowerCase()));
	}
	
	public void uninstallIfFound() {	
		if (getRestUrl() != null) {
			String command = "connect " + getRestUrl() + ";list-services";
			String output;
			try {
				output = CommandTestUtils.runCommandAndWait(command);
				if (output.contains(applicationName)) {
					uninstall();
				}
			} catch (final Exception e) {
				LogUtils.log(e.getMessage(), e);
				Assert.fail(e.getMessage());
			}
		}
	}
}
