#! /bin/bash

echo starting pre-bootstrap...

# BYON machines may be dirty
echo checking for previous java installation
if [ -d "~/java" ]; then
	echo cleaning java installation from home directory
	rm -rf ~/java
fi

echo checking for previous gigaspaces installation
if [ -d "~/gigaspaces" ]; then
	echo cleaning gigaspaces installation from home directory
	rm -rf ~/gigaspaces
fi

echo Downloading logstash to ~/
cd ~/
wget https://s3-eu-west-1.amazonaws.com/gigaspaces-maven-repository-eu/net/logstash/1.2.2/logstash-1.2.2.jar --no-check-certificate

echo starting log shipper
echo java home: $JAVA_HOME

mkdir ~/logstash/logs
touch ~/logstash/logs/logstash-shipper-log.txt
$JAVA_HOME/bin/java -jar logstash-1.2.2.jar agent -f /tmp/`whoami`/gs-files/upload/cloudify-overrides/config/logstash/logstash-shipper.conf -l ~/logstash/logs/logstash-shipper-log.txt&
