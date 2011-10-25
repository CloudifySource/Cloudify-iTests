package test.cli.cloudify.recipes;

import java.io.File;
import java.io.IOException;

import junit.framework.Assert;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.cli.cloudify.AbstractCommandTest;
import test.cli.cloudify.CommandTestUtils;

import com.gigaspaces.cloudify.dsl.internal.ServiceReader;


public class TestRecipeCommandTest extends AbstractCommandTest{
	
	@Override
	@BeforeMethod
	public void beforeTest() {}
	
	final private String SIMPLE_RECIPE_DIR_PATH = CommandTestUtils.getPath("apps/USM/usm/simplejavaprocess");
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", enabled = true)
	public void testInvocationUsingDirAndFile() throws IOException, InterruptedException{
		
		File serviceDir = new File(SIMPLE_RECIPE_DIR_PATH);
		File serviceFile = new File(SIMPLE_RECIPE_DIR_PATH , "simplejava-modifiedservice.groovy"); 
		com.gigaspaces.cloudify.dsl.Service service = ServiceReader.getServiceFromFile(serviceFile , serviceDir).getService();
		int port = (Integer) service.getPlugins().get(0).getConfig().get("port");
		new Thread(new RecipeTestUtil.AsinchronicPortCheck(port)).start();
		
		String consoleOutput = runCommand("test-recipe " + SIMPLE_RECIPE_DIR_PATH + " 30 simplejava-modifiedservice.groovy");
		Assert.assertFalse("The command threw an exception - check the log", consoleOutput.contains("Exception"));
	}
	
	
	@Override
	@AfterMethod
    public void afterTest() {}
}
