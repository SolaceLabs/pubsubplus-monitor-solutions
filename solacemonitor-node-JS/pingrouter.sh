#!/bin/bash

#
# This script ping from the current host to the Solace Message Routers.
# 
# The current host is where the pollrouter.sh resides.  The idea is to monitor
# using RTT the link health between the gathering host and the Solace message routers.
# 
# To calculation is based on the below:
#
# ping output format
#
# :
# :
# 5 packets transmitted, 5 received, 0% packet loss, time 4005ms
# rtt min/avg/max/mdev = 2.075/3.256/5.104/1.048 ms
#
# The script take the average measurement of 5 pings, 1 second apart. 
#

CHECK_INTERVAL_SEC=60

if [ ! -z "$SMON_ROUTER_PING_INTVL_SEC" ]; then
  if [ $SMON_ROUTER_PING_INTVL_SEC -ge 15 -a $SMON_ROUTER_PING_INTVL_SEC -le 600 ]; then
    CHECK_INTERVAL_SEC=$SMON_ROUTER_PING_INTVL_SEC
  fi
fi

configDir=config

. ${configDir}/system.config

influxhost=$dbhost

# DO NOT MODIFY BELOW


while true; do
  for addr in "${host[@]}"
  do
    if [[ $addr = *":"* ]]; then
      IFS=:
      addrArr=( $addr )
      addr=${addrArr[0]}
      unset IFS
    fi
    val=`ping -c 5 $addr | tail -1 | awk '{print $4}' | cut -d '/' -f 2`
    echo "pingrouter,pinghost=$addr value=$val"  > ping_insert
    cat ping_insert
    curl -u $dbuser:$PASSWORD --silent --output /dev/null -i -XPOST "http://$influxhost/write?db=$db" --data-binary @ping_insert
  done

  rm ping_insert

  sleep $CHECK_INTERVAL_SEC
done

