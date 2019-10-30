#!/bin/sh
set -x

cd sonar-application/build/distributions
rm -rf sonarqube-*
unzip sonar-application-*.zip
cd sonarqube-*
#java -jar ./lib/sonar-application*.jar -Dsonar.web.javaAdditionalOpts=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
java -jar ./lib/sonar-application*.jar

