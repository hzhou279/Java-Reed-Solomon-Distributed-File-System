# 

#!/bin/bash

# Define the ports
port1="8083"
port2="8084"

# Find the process IDs (PIDs) associated with the first port
pids1=$(lsof -i :$port1 -t)

# Find the process IDs (PIDs) associated with the second port
pids2=$(lsof -i :$port2 -t)

# Merge the process IDs
pids="$pids1"$'\n'"$pids2"

# Check if there are any running processes
if [ -z "$pids" ]; then
  echo "No processes found running on ports $port1 and $port2."
  exit 1
fi

# Terminate the processes
echo "Terminating processes running on ports $port1 and $port2..."
echo "$pids" | xargs kill

# Wait for the processes to terminate
sleep 2

# Check if the processes have been terminated successfully
running_pids=$(lsof -i :$port1 -i :$port2 -t)

if [ -z "$running_pids" ]; then
  echo "Processes terminated successfully."
else
  echo "Failed to terminate processes."
fi