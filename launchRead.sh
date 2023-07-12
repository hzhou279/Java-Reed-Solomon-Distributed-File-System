#!/bin/bash

mvn exec:java -Dexec.mainClass=edu.cmu.reedsolomonfs.client.ReadClient -Dexec.args="cluster 127.0.0.1:8081,127.0.0.1:8082,127.0.0.1:8083,127.0.0.1:8084,127.0.0.1:8085,127.0.0.1:8086" -Dexec.classpathScope="compile" -Dexec.additionalClasspath="/maven-repo/reed-solomon-1.0.jar"