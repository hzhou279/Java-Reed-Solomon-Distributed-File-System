#!/bin/bash

# /usr/bin/env /home/vscode/.vscode-remote/extensions/redhat.java-1.20.0-linux-x64/jre/17.0.7-linux-x86_64/bin/java @/tmp/cp_vuk5cdv54yla9eeb6dl1w69c.argfile edu.cmu.reedsolomonfs.server.Master.Master



# A flag for whether to run the clean compile command
# Set it to 0 (don't run) by default
run_compile=0

# Parse the command line arguments
while getopts "c" opt; do
  case ${opt} in
    c)
      # If the -c flag is provided, set the run_compile flag to 1
      run_compile=1
      ;;
    \?)
      echo "Invalid option: -$OPTARG" 1>&2
      exit 1
      ;;
  esac
done

# If the run_compile flag is 1, then run the clean compile command
if [ "$run_compile" -eq "1" ]; then
    mvn clean compile -X
fi


# command=("mvn" "exec:java" "-Dexec.mainClass=edu.cmu.reedsolomonfs.server.Master.Master")
# "${command[@]}"

# mvn compile \
#   -Dmaven.compiler.source=17 \
#   -Dmaven.compiler.target=17 \
#   -Dmaven.compiler.useIncrementalCompilation=false \
#   -Dmdep.includeScope=compile \
#   -Dmdep.outputFile=classpath.txt \
#   -DadditionalClasspath="/maven-repo/reed-solomon-1.0.jar"

# javac -cp "/maven-repo/reed-solomon-1.0.jar" src/main/java/edu/cmu/reedsolomonfs/server/Chunkserver/ChunkserverDiskRecoveryMachine.java
mvn compile -DadditionalClasspath="/maven-repo/reed-solomon-1.0.jar"
mvn exec:java -Dexec.mainClass=edu.cmu.reedsolomonfs.server.Master.Master -Dexec.args="cluster 127.0.0.1:8081,127.0.0.1:8082,127.0.0.1:8083,127.0.0.1:8084,127.0.0.1:8085,127.0.0.1:8086" -Dexec.classpathScope="compile" -Dexec.additionalClasspath="/maven-repo/reed-solomon-1.0.jar"
