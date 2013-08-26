#!/bin/bash

 SUITE_TYPE=$1 
 BUILD_NUMBER=$2
 SUITE_NAME=$3
 MAJOR_VERSION=$4
 MINOR_VERSION=$5
 DEPLOY_ROOT_BIN_DIR=$6
 BUILD_LOG_URL=$7
 MAVEN_REPO_LOCAL=$8
 MAVEN_PROJECTS_VERSION_XAP=$9
 MAVEN_PROJECTS_VERSION_CLOUDIFY=${10}
 ENABLE_LOGSTASH=${11}

pushd ../local-builds/${BUILD_NUMBER}/Cloudify-iTests

TYPE=cloudify

if [ ! -z `echo ${SUITE_NAME} | grep BigData` ] ; then TYPE=xap-premium; fi

CLOUDIFY_HOME=/export/tgrid/sgtest3.0-cloudify/deploy/local-builds/${BUILD_NUMBER}/gigaspaces-${TYPE}-${MAJOR_VERSION}-${MINOR_VERSION}

mvn exec:java -Dexec.mainClass="iTests.framework.testng.report.TestsReportMerger" -Dexec.args="${SUITE_NAME} ../${SUITE_NAME} ../${SUITE_NAME}" -Dcloudify.home=${CLOUDIFY_HOME} -Dmaven.repo.local=${MAVEN_REPO_LOCAL} -DgsVersion=${MAVEN_PROJECTS_VERSION_XAP} -DcloudifyVersion=${MAVEN_PROJECTS_VERSION_CLOUDIFY}

mvn exec:java -Dexec.mainClass="iTests.framework.testng.report.wiki.WikiReporter" -Dexec.args="../${SUITE_NAME} ${SUITE_TYPE} ${BUILD_NUMBER} ${MAJOR_VERSION} ${MINOR_VERSION} ${BUILD_LOG_URL}" -Dcloudify.home=${CLOUDIFY_HOME} -Dmysql.host="192.168.9.44" -Dmysql.user="sa" -Dmysql.pass="" -Dmaven.repo.local=${MAVEN_REPO_LOCAL} -DgsVersion=${MAVEN_PROJECTS_VERSION_XAP} -DcloudifyVersion=${MAVEN_PROJECTS_VERSION_CLOUDIFY} -DiTests.enableLogstash=${ENABLE_LOGSTASH}


popd



#return java exit code. 
exit $?