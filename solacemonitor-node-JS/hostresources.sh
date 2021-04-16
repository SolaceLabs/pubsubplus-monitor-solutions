#!/bin/bash

# This script should be installed on all the linux system that forms the capitalmon system.
#
# It surveys the host for CPU and MEM usage and post them to influxdb.
#
#
# To run manually:
#
#   nohup ./hostresources.sh > /dev/null 2>&1 & 
#
#
# hostresources.sh can also be started and ended by the start.sh and stop.sh.
#

# INPUT
#

if [ -x /usr/bin/hostname ]; then
  curhost=`/usr/bin/hostname -s`
else
  curhost=`/bin/hostname -s`
fi
sleeptime=30
#

if [ -x /usr/bin/grep ]; then
  GREP=/usr/bin/grep
else
  GREP=/bin/grep
fi
AWK=/usr/bin/awk
if [ -x /usr/bin/df ]; then
  DF=/usr/bin/df
else
  DF=/bin/df
fi
SAR=/usr/bin/sar
if [ -x /usr/bin/rm ]; then
  RM=/usr/bin/rm
else
  RM=/bin/rm
fi
FREE=/usr/bin/free
if [ -x /usr/bin/date ]; then
  DATE=/usr/bin/date
else
  DATE=/bin/date
fi
CURL=/usr/bin/curl


# DO NOT MODIFY BELOW

configdir=config
. ${configdir}/system.config

writeToDB () {
  httpcode=`$CURL -u $dbuser:$PASSWORD -w "%{http_code}" --silent --output /dev/null -i -XPOST "http://$dbhost/write?db=$db" --data-binary @$1`
  returncode=$?
  if [ "$returncode" == "0" ]
  then
    if [ $httpcode -ge 300 ]; then echo `$DATE` "WARN: Error writing to DB with HTTP return code: $httpcode"; cat $1; fi
  else
    echo `$DATE` "ERROR: Non zero curl return code"
  fi
}

while true
do
  ##
  ## Memory
  ##
  parse=( total used free shared buffcache available );

  idx=2
  output=`$FREE -m | grep Mem`
  timestamp=`$DATE +%s%N`

  for field in "${parse[@]}"
  do
    val=`echo $output | awk "{ print \\$$idx }"`
    echo "host-mem-$field,host=$curhost value=$val $timestamp"  >> host_mem_records.$timestamp

    idx=$((idx+1))
  done
  #cat host_mem_records.$timestamp
  writeToDB host_mem_records.$timestamp
  $RM -f host_mem_records.$timestamp

  ##
  ## CPU
  ## CPU     %user     %nice   %system   %iowait    %steal     %idle
  ##
  timestamp=`$DATE +%s%N`
  $SAR -u -P ALL 1 1 | $GREP Average | $GREP -v CPU | while read line
  do
    parse=( user nice system iowait steal idle );
    cpunum=`echo $line | $AWK "{ print \\$2 }"`
    idx=3
    for field in "${parse[@]}"
    do
      val=`echo $line | $AWK "{ print \\$$idx }"`
      echo "host-cpu-$field,host=$curhost,cpu=$cpunum value=$val $timestamp"  >> host_cpu_records.$timestamp
      idx=$((idx+1))
    done
  done
  #cat host_cpu_records.$timestamp
  writeToDB host_cpu_records.$timestamp
  $RM -f host_cpu_records.$timestamp

  ##
  ## Filesystem
  ##
  timestamp=`$DATE +%s%N`
  $DF -k | $GREP "^/" | while read line
  do
    parse=( size used avail );
    mountpt=`echo $line | $AWK "{ print \\$6 }"`
    idx=2
    for field in "${parse[@]}"
    do
      val=`echo $line | $AWK "{ print \\$$idx }"`
      echo "host-fs-$field,host=$curhost,mountpt=$mountpt value=$val $timestamp"  >> host_fs_records.$timestamp
      idx=$((idx+1))
    done
  done
  #cat host_fs_records.$timestamp
  writeToDB host_fs_records.$timestamp
  $RM -f host_fs_records.$timestamp

  sleep $sleeptime

done
