#!/bin/bash
rm -rf chunkserver*
declare -a ports=("8081" "8082" "8083", "8084", "8085", "8086")

# Iterate the string array using for loop
for port in ${ports[@]}; do
    echo $port
    PID=$(lsof -t -i:$port) # get the process id
    if [ -z "$PID" ]
    then
        echo "No process running on port $port"
    else
        echo "Killing process on port $port with pid $PID"
        kill -9 $PID
    fi
done

wait
echo "All processes killed successfully"