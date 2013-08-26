#!/bin/bash

 BUILD_NUMBER=$1
 INCLUDE=$2
 EXCLUDE=$3
 SUITE_NAME=$4
 MAJOR_VERSION=$5
 MINOR_VERSION=$6
 SUITE_ID=$7
 SUITE_NUMBER=$8
 BYON_MACHINES=$9
 SUPPORTED_CLOUDS=${10}
 EC2_REGION=${11}
 SUITE_WORK_DIR=${12}; export SUITE_WORK_DIR
 SUITE_DEPLOY_DIR=${13}; export SUITE_DEPLOY_DIR
 BRANCH_NAME=${14}
 SUITE_TYPE=${15}
 MAVEN_REPO_LOCAL=${16}
 MAVEN_PROJECTS_VERSION_XAP=${17}
 MAVEN_PROJECTS_VERSION_CLOUDIFY=${18}
 ENABLE_LOGSTASH=${19}
 S3_MIRROR=${20}
 EXT_JAVA_OPTIONS="${EXT_JAVA_OPTIONS} -Dcom.gs.work=${SUITE_WORK_DIR} -Dcom.gs.deploy=${SUITE_DEPLOY_DIR} -Dorg.cloudifysource.rest-client.enable-new-rest-client=true"; export EXT_JAVA_OPTIONS

echo clouds=$SUPPORTED_CLOUDS

mkdir ${BUILD_DIR}/../${SUITE_NAME}
cd ${BUILD_DIR}/../Cloudify-iTests

mvn test -X -e -U -P tgrid-cloudify-iTests \
-DiTests.cloud.enabled=false \
-DiTests.buildNumber=${BUILD_NUMBER} \
-DiTests.enableLogstash=${ENABLE_LOGSTASH} \
-Dsgtest.buildNumber=${BUILD_NUMBER} \
-DiTests.enableLogstash=${ENABLE_LOGSTASH} \
-Dcloudify.home=${BUILD_DIR} \
-Dincludes=${INCLUDE} \
-Dexcludes=${EXCLUDE} \
-Djava.security.policy=policy/policy.all \
-Djava.awt.headless=true \
-DiTests.suiteName=${SUITE_NAME} \
-Dsgtest.suiteName=${SUITE_NAME} \
-DiTests.suiteId=${SUITE_ID} \
-Dsgtest.suiteId=${SUITE_ID} \
-DiTests.summary.dir=${BUILD_DIR}/../${SUITE_NAME} \
-DiTests.numOfSuites=${SUITE_NUMBER} \
-Dsgtest.numOfSuites=${SUITE_NUMBER} \
-Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Jdk14Logger \
-Dcom.gs.logging.level.config=true \
-Djava.util.logging.config.file=/export/tgrid/sgtest3.0-cloudify/bin/..//logging/sgtest_logging.properties \
-Dsgtest.buildFolder=../ \
-DiTests.url=http://192.168.9.121:8087/sgtest3.0-cloudify/ \
-Dcom.gs.work=${SUITE_WORK_DIR} \
-Dcom.gs.deploy=${SUITE_DEPLOY_DIR} \
-Dec2.region=${EC2_REGION} \
-DipList=${BYON_MACHINES} \
-Dsupported-clouds=${SUPPORTED_CLOUDS} \
-Dbranch.name=${BRANCH_NAME} \
-Dmaven.repo.local=${MAVEN_REPO_LOCAL} \
-DgsVersion=${MAVEN_PROJECTS_VERSION_XAP} \
-DcloudifyVersion=${MAVEN_PROJECTS_VERSION_CLOUDIFY} \
-Dorg.cloudifysource.repository.mirror=${S3_MIRROR}

#return java exit code.
exit $?
