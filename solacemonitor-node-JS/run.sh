#!/bin/bash

# This script is supposed to be executed by Docker on container startup.
# To put it in another way, it's to be executed by "CMD" in Docker.

# this is the only parameter that can be tuned for this script
CHECK_INTERVAL_SEC=30

# check if the config file is present or not
if [ ! -s ./config/config.json ]; then
  echo "./config/config.json not found, exiting!"
  exit 1
fi

# make sure to clean up the pid.file
./stop.sh

# then start all the necessary processes
./start.sh

# endless loop to make sure that all started processes continue to run
while true; do
  sleep $CHECK_INTERVAL_SEC
  ./checkAndRestart.sh >> log/checkAndRestart.log 2>&1
done

# this line should never be executed, right??!!
echo "run.sh exited for some reason!"

