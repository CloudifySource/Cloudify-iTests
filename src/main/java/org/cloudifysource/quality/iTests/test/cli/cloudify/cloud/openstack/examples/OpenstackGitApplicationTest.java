package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.openstack.examples;

import org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.ec2.examples.Ec2GitApplicationsTest;
import org.testng.annotations.Test;

@Test
public class OpenstackGitApplicationTest extends Ec2GitApplicationsTest {

    @Override
    protected String getCloudName() {
        return "openstack";
    }
}
