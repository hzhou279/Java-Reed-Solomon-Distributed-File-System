#!/bin/bash

# Generate random numbers between 0 and 5
random_number1=$((RANDOM % 6))
# Generate the second random number and check for equality with num1
while true; do
  random_number2=$((RANDOM % 6))
  if [[ $random_number2 != $random_number1 ]]; then
    break
  fi
done

# Add random numbers to 8081
port_number1=$((random_number1 + 8081))
port_number2=$((random_number2 + 8081))

echo "Port number 1: $port_number1"
echo "Port number 2: $port_number2"

# Add random numbers to 8081 to obtain folder names
folder_name1="chunkserver-$((random_number1))"
folder_name2="chunkserver-$((random_number2))"

# Define the folder paths
folder_path1="./ClientClusterCommTestFiles/Disks/$folder_name1"
folder_path2="./ClientClusterCommTestFiles/Disks/$folder_name2"

# Delete folders
rm -r "$folder_path1"
rm -r "$folder_path2"

# Terminate processes using fuser
fuser -k "$port_number1/tcp"
fuser -k "$port_number2/tcp"