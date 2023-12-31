#!/bin/bash

# The command to be run is given as an argument
command=$1

case $command in
  build)
    echo "Building Docker image..."
    docker build -t reed-solomon-fs:latest .
    ;;
  start)
    echo "Starting Docker Compose services..."
    docker-compose up -d
    ;;
  stop)
    echo "Stopping Docker Compose services..."
    docker-compose down
    ;;
  client)
    echo "Starting client..."
    docker-compose -f docker-compose.client.yml up
    ;;
  client-cli)
    echo "Starting command line interface...."
    docker-compose -f docker-compose.client-cli.yml run client_cli
    ;;
  *)
    echo "Invalid command. Valid commands are: build, start, stop"
    ;;
esac
