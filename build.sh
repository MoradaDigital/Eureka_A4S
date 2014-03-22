#/bin/sh

javac -classpath lib/Arduino.jar:lib/RXTXcomm.jar src/eureka/Eureka_A4S.java
mkdir eureka
mv src/eureka/*.class eureka/
jar -cfm Eureka_A4S.jar manifest.mf lib/* eureka/*

rm -rf eureka
