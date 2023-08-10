#!/bin/bash

# Define your image tag
image_tag="reed-solomon-fs"

# Parse the command line arguments
while getopts "brs" opt; do
  case ${opt} in
    b)
      # If the -b flag is provided, build the Docker image
      echo "Building Docker image..."
      docker build -t $image_tag .
      ;;
    r)
      # If the -r flag is provided, run the Docker container
      echo "Running Docker container..."
      docker run -p 8080:8080 $image_tag
      ;;
    s)
      # If the -s flag is provided, stop the Docker container
      echo "Stopping Docker container..."
      docker stop $(docker ps -a -q --filter ancestor=$image_tag --format="{{.ID}}")
      ;;
    \?)
      echo "Invalid option: -$OPTARG" 1>&2
      exit 1
      ;;
  esac
done
