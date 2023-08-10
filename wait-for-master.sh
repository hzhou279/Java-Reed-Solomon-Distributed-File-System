#!/bin/bash
set -e

until nc -z master 8080; do
  echo "$(date) - waiting for master service..."
  sleep 1
done
