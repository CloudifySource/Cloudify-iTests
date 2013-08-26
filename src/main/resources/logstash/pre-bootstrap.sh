echo starting pre-bootstrap...

#installing java

JAVA_32_URL="http://repository.cloudifysource.org/com/oracle/java/1.6.0_32/jdk-6u32-linux-i586.bin"
JAVA_64_URL="http://repository.cloudifysource.org/com/oracle/java/1.6.0_32/jdk-6u32-linux-x64.bin"

# If not JDK specified, determine which JDK to install based on hardware architecture
if [ -z "$GIGASPACES_AGENT_ENV_JAVA_URL" ]; then
	ARCH=`uname -m`
	echo Machine Architecture -- $ARCH
	if [ "$ARCH" = "i686" ]; then
		export GIGASPACES_AGENT_ENV_JAVA_URL=$JAVA_32_URL
	elif [ "$ARCH" = "x86_64" ]; then
		export GIGASPACES_AGENT_ENV_JAVA_URL=$JAVA_64_URL
	else
		echo Unknown architecture -- $ARCH -- defaulting to 32 bit JDK
		export GIGASPACES_AGENT_ENV_JAVA_URL=$JAVA_32_URL
	fi

fi

if [ "$GIGASPACES_AGENT_ENV_JAVA_URL" = "NO_INSTALL" ]; then
	echo "JDK will not be installed"
else
	echo Previous JAVA_HOME value -- $JAVA_HOME
	export GIGASPACES_ORIGINAL_JAVA_HOME=$JAVA_HOME

	echo Downloading JDK from $GIGASPACES_AGENT_ENV_JAVA_URL
	wget -q -O $WORKING_HOME_DIRECTORY/java.bin $GIGASPACES_AGENT_ENV_JAVA_URL || error_exit $? 101 "Failed downloading Java installation from $GIGASPACES_AGENT_ENV_JAVA_URL"
	chmod +x $WORKING_HOME_DIRECTORY/java.bin
	echo -e "\n" > $WORKING_HOME_DIRECTORY/input.txt
	rm -rf ~/logstash/java || error_exit $? 102 "Failed removing old java installation directory"
	mkdir ~/logstash
	mkdir ~/logstash/java
	cd ~/logstash/java

	echo Installing JDK
	$WORKING_HOME_DIRECTORY/java.bin < $WORKING_HOME_DIRECTORY/input.txt > /dev/null
	mv ~/logstash/java/*/* ~/logstash/java || error_exit $? 103 "Failed moving JDK installation"
	rm -f $WORKING_HOME_DIRECTORY/input.txt
    export JAVA_HOME=/home/ec2-user/logstash/java
fi

echo Downloading logstash to ~/
cd ~/
wget https://s3-eu-west-1.amazonaws.com/gigaspaces-maven-repository-eu/net/logstash/1.1.13/logstash-1.1.13.jar

echo starting log shipper
echo java home: $JAVA_HOME

mkdir ~/logstash/logs
touch ~/logstash/logs/logstash-shipper-log.txt
$JAVA_HOME/bin/java -jar logstash-1.1.13.jar agent -f ~/gs-files/upload/cloudify-overrides/config/logstash/logstash-shipper.conf -l ~/logstash/logs/logstash-shipper-log.txt&
