@echo setting up runtime variables

set VERSION=%1
set MILESTONE=%2
set BUILD_NUMBER=%3
set BUILD_VERSION=%4
set SUITE_NAME=%5
set INCLUDE=%6
set EXCLUDE=%7
set BUILD_LOG_URL=%8
set BRANCH_NAME=%9

shift
shift
set SVN_BRANCH_DIRECTORY=%8
set EC2_REGION=%9

shift
set REVERSE_PROXY=%9

shift
set SUITE_TYPE=%9

shift
set MAVEN_PROJECTS_VERSION_XAP=%9

shift
set MAVEN_PROJECTS_VERSION_CLOUDIFY=%9

shift
set ENABLE_LOGSTASH=%9

set SGTEST_RUNTIME_FOLDER=C:\Users\ca\sgtest-cloudify3.0
set BUILD_TEST_DIR=C:\%BUILD_NUMBER%

@echo cleaning build folder..
@if exist %BUILD_TEST_DIR% rmdir %BUILD_TEST_DIR% /s /q

@echo exporting Cloudify-iTests

@mkdir %BUILD_TEST_DIR%

set ITESTS_HOME=%BUILD_TEST_DIR%\Cloudify-iTests

cd %BUILD_TEST_DIR%
if %BRANCH_NAME%==trunk (
    call C:\Git\bin\git.exe clone --depth 1 https://github.com/CloudifySource/Cloudify-iTests.git
) else (
    call C:\Git\bin\git.exe clone -b %BRANCH_NAME% --depth 1 https://github.com/CloudifySource/Cloudify-iTests.git
)
call mvn scm:export -DconnectionUrl=scm:svn:svn://svn-srv/SVN/cloudify/trunk/quality/frameworks/SGTest-credentials -DexportDirectory=%BUILD_TEST_DIR%/Cloudify-iTests/src/main/resources/credentials

@echo starting sgtest execution
@call %ITESTS_HOME%\src\main\scripts\deploy\bin\windows\startSG.bat %VERSION% %MILESTONE% %BUILD_NUMBER% %BUILD_VERSION% %SUITE_NAME% %INCLUDE% %EXCLUDE% %BUILD_LOG_URL% %BRANCH_NAME% %SVN_BRANCH_DIRECTORY% %EC2_REGION% %REVERSE_PROXY% %SUITE_TYPE% %MAVEN_PROJECTS_VERSION_XAP% %MAVEN_PROJECTS_VERSION_CLOUDIFY%

@echo cleaning local build folder - %BUILD_TEST_DIR%
if exist %BUILD_TEST_DIR% rmdir %BUILD_TEST_DIR% /s /q