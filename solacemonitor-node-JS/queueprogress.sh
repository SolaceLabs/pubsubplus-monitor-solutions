#!/bin/bash

# This script checks the number of files in the processing/<time scale> directory.
#
# The still to be processed SEMP response is stored in such directories. So the higher the number of files
# the 'deeper' is the queue, in other words, high number equates to postdb.sh falling behind in processing
# SEMP response.
#
# The queue depth is post to the influxdb.
#
# Note:
#
# Below approach was tried but hogging up too much process time
#
# while true; do find processing -type d -print0 | while read -d '' -r dir; do     files=("$dir"/*);     printf "%5d files in queue %s\n" "${#files[@]}" "$dir"; done; sleep 3; clear; done
#
#
# The script requires the password file generated via gpg.
#
# To run manually:
#
#   nohup ./queueprogress.sh > /dev/null 2>&1 &
#
#
# queueprogress.sh can also be started and ended by the start.sh and stop.sh.
#



# INPUT 
configdir="config"
checkinterval=2
#

# DO NOT MODIFY BELOW

echo sourcing the ${configdir}/system.config file
. ${configdir}/system.config

while true
do

  for d in processing/*/
  do
    v=`ls $d | grep -v inse | wc -l`
#   echo "$d $v"
    echo "$d value=$v" > progress_insert

    httpcode=`curl -u $dbuser:$PASSWORD -w "%{http_code}" --silent --output /dev/null -i -XPOST "http://$dbhost/write?db=$db" --data-binary @progress_insert`
    returncode=$?
    if [ "$returncode" == "0" ]
    then
      if [ $httpcode -ge 300 ]; then echo `date` "WARN: Error writing to DB with HTTP return code: $httpcode"; cat progress_insert; fi
    else
      echo `date` "ERROR: Non zero curl return code"
    fi

  done

  sleep $checkinterval 
  #clear
  rm progress_insert

done
