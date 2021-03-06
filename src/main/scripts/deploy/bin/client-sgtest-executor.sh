#!/bin/sh -x

##############################################
# remote-sgteest-executer.sh args
# 1 arg deploy root directory (sgtest/deploy).
# 2 build root directory
# 3 java type
# 4 lookup groups
##############################################

 DEPLOY_ROOT_BIN_DIR=$1
 BUILD_DIR=$2; export BUILD_DIR
 JAVA_TYPE=$3
 LOOKUPGROUPS=$4; export LOOKUPGROUPS
 BUILD_NUMBER=$5
 INCLUDE=$6
 EXCLUDE=$7
 SUITE_NAME=$8
 MAJOR_VERSION=$9
 MINOR_VERSION=${10}
 SUITE_ID=${11}
 SUITE_NUMBER=${12}
 BYON_MACHINES=${13}
 SUPPORTED_CLOUDS=${14}
 EC2_REGION=${15}
 SUITE_WORK_DIR=${16}
 SUITE_DEPLOY_DIR=${17}
 BRANCH_NAME=${18}
 SUITE_TYPE=${19}
 MAVEN_REPO_LOCAL=${20}
 MAVEN_PROJECTS_VERSION_XAP=${21}
 MAVEN_PROJECTS_VERSION_CLOUDIFY=${22}
 ENABLE_LOGSTASH=${23}
 S3_MIRROR=${24}
 BACKWARDS_RECIPES_BRANCH=${25}

 # set shell.
# TERM=xterm export TERM
 JSHOMEDIR=${BUILD_DIR}; export JSHOMEDIR

 # set path of sgtest/bin (#1 script argument)
 SGTEST_ROOT_BIN_DIR=${DEPLOY_ROOT_BIN_DIR}/../../bin/


 ${CONFIG_JAVA_SCRIPT} ${JAVA_TYPE}


 cd ${SGTEST_ROOT_BIN_DIR}

 #start sgtest
 ${SGTEST_ROOT_BIN_DIR}/sgtest-cmd.sh ${BUILD_NUMBER} ${INCLUDE} ${EXCLUDE} ${SUITE_NAME} ${MAJOR_VERSION}
 ${MINOR_VERSION} ${SUITE_ID} ${SUITE_NUMBER} ${BYON_MACHINES} ${SUPPORTED_CLOUDS} ${EC2_REGION} ${SUITE_WORK_DIR}
 ${SUITE_DEPLOY_DIR} ${BRANCH_NAME} ${SUITE_TYPE} ${MAVEN_REPO_LOCAL} ${MAVEN_PROJECTS_VERSION_XAP} ${MAVEN_PROJECTS_VERSION_CLOUDIFY} ${ENABLE_LOGSTASH} ${S3_MIRROR} ${BACKWARDS_RECIPES_BRANCH}

 RESULT=$?

 #write a result file
 RESULT_INDICATOR_FILE="${DEPLOY_ROOT_BIN_DIR}/../result_indicator/sgtest_end${SUITE_NAME}${SUITE_ID}"

 touch ${RESULT_INDICATOR_FILE}
 echo "result ${RESULT}" >> ${RESULT_INDICATOR_FILE}

 #exit with sgtest System.exit status.
 exit $?
