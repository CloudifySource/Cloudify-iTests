@echo on

@rem - Replace default configuration files with local-machine specific files.

set WEBUI_TMP_DIR=%BUILD_TEST_DIR%

@echo cloning webuitf
set GIT_SSL_NO_VERIFY=true
pushd %WEBUI_TMP_DIR%

if %BRANCH_NAME%==trunk (
    call C:\Git\bin\git.exe clone --depth 1 https://github.com/CloudifySource/Cloudify-iTests-webuitf.git
) else (
    call C:\Git\bin\git.exe clone -b %BRANCH_NAME% --depth 1 https://github.com/CloudifySource/Cloudify-iTests-webuitf.git
)

popd
set Cloudify_iTests_webuitf=%WEBUI_TMP_DIR%\Cloudify-iTests-webuitf

@echo deploying webuitf...
pushd %Cloudify_iTests_webuitf%
mvn clean install s3client:deploy -U -DgsVersion=%MAVEN_PROJECTS_VERSION_XAP% -DcloudifyVersion=%MAVEN_PROJECTS_VERSION_CLOUDIFY%
popd

rmdir /s /q %Cloudify_iTests_webuitf%



:_skip