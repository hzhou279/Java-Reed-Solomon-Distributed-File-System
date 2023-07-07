#!/bin/bash
handle_sigint() {
    echo "Caught SIGINT, executing additional code..."
    # Your additional code here
    ./terminateChunkserver.sh
    exit 1
}

# Set up the trap
trap 'handle_sigint' SIGINT

./terminateChunkserver.sh

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

command1=("mvn" "exec:java" "-Dexec.mainClass=edu.cmu.reedsolomonfs.server.Chunkserver.Chunkserver" "-Dexec.args=chunkserver1 cluster 127.0.0.1:8081 127.0.0.1:8081,127.0.0.1:8082,127.0.0.1:8083,127.0.0.1:8084,127.0.0.1:8085,127.0.0.1:8086 0")
command2=("mvn" "exec:java" "-Dexec.mainClass=edu.cmu.reedsolomonfs.server.Chunkserver.Chunkserver" "-Dexec.args=chunkserver2 cluster 127.0.0.1:8082 127.0.0.1:8081,127.0.0.1:8082,127.0.0.1:8083,127.0.0.1:8084,127.0.0.1:8085,127.0.0.1:8086 1")
command3=("mvn" "exec:java" "-Dexec.mainClass=edu.cmu.reedsolomonfs.server.Chunkserver.Chunkserver" "-Dexec.args=chunkserver3 cluster 127.0.0.1:8083 127.0.0.1:8081,127.0.0.1:8082,127.0.0.1:8083,127.0.0.1:8084,127.0.0.1:8085,127.0.0.1:8086 2")
command4=("mvn" "exec:java" "-Dexec.mainClass=edu.cmu.reedsolomonfs.server.Chunkserver.Chunkserver" "-Dexec.args=chunkserver4 cluster 127.0.0.1:8084 127.0.0.1:8081,127.0.0.1:8082,127.0.0.1:8083,127.0.0.1:8084,127.0.0.1:8085,127.0.0.1:8086 3")
command5=("mvn" "exec:java" "-Dexec.mainClass=edu.cmu.reedsolomonfs.server.Chunkserver.Chunkserver" "-Dexec.args=chunkserver5 cluster 127.0.0.1:8085 127.0.0.1:8081,127.0.0.1:8082,127.0.0.1:8083,127.0.0.1:8084,127.0.0.1:8085,127.0.0.1:8086 4")
command6=("mvn" "exec:java" "-Dexec.mainClass=edu.cmu.reedsolomonfs.server.Chunkserver.Chunkserver" "-Dexec.args=chunkserver6 cluster 127.0.0.1:8086 127.0.0.1:8081,127.0.0.1:8082,127.0.0.1:8083,127.0.0.1:8084,127.0.0.1:8085,127.0.0.1:8086 5")


# Run commands concurrently
"${command1[@]}" &
"${command2[@]}" &
"${command3[@]}" &
"${command4[@]}" &
"${command5[@]}" &
"${command6[@]}" &

# Wait for all commands to finish
wait