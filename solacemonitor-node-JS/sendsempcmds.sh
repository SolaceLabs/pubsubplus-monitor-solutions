#!/bin/bash

#
# This script sends maintenance SEMP commands to each routers listed in the config/system.config.
# It is not a long running process. User should configure it as a cron job. An example would be:
#  
#    0 0 * * * cd /ap/solacemon;./sendsempcmds.sh D > log/sendsempcmds.log 2>&1
#
# The SEMP commands to be sent to the routers are listed in cmd/maintenancecmdlist file. The content of
# of the file is listed here as a reference:
#
#    R1 D <rpc semp-version="SOLTRV"> <clear> <stats> <client></client> </stats> </clear> </rpc>
#    R1 D <rpc semp-version="SOLTRV"> <clear> <stats> <ssl></ssl> </stats> </clear> </rpc>
#
# Each line is a command that would be sent to the routers when the script is executed. Each line is
# consisted of three fields sperated by a "space" character.
#
# In the above example, a value of "R1" is in the first field. It denotes the type the command belongs to.
# A command type decrees how the command would to be processed before being sent to the router. Normally,
# the processing would involve replacing some parameters. Type "R1", however, doesn't do any parameter
# replacement as the commands of this type are all router level. The script currently just handles "R1"
# type. Other types would be ignored.
# 
# The second field "D" here denotes the frequency at which the command would be sent. The character "D"
# doesn't have any concrete meaning. But it is being passed as a parameter to this script as a filter.
# It means that this script would only process the command lines with this character in the second field.
# You might have different commands that you want to execute at different time. You would then
# use different characters in the second field to group them and then schedule multiple cron jobs to
# run them (each with a different character as the first parameter).
#
# These directories are in the form of processing/<time scale> .
#
# For example processing/60 stores all the responses which are maked as collecting once every 60 seconds in the 'cmdlist'.
#
# The time scale is defeind in the cmdlist, e.g. in cmdlist file,
#
#   :
#   2 10 <rpc semp-version="SOLTRV"> <show> <router-name></router-name> </show> </rpc>
#   :
#   :
#       2 specifies command type
#       10 specifies poll once every 10s
#
# The required supporting files are *cmdlist and *list.
#
# The script is started by start.sh
#

cmdlistdir="cmd"
configdir="config"

# DO NOT MODIFY BELOW

# all the router information is in the system.config file
. ${configdir}/system.config

hostnamedone=0

index=0  # must stay before while true and before for loop

# this function posts data to InfluxDB
# $1 is the admin username of the router
# $2 is the password of $1
# $3 is the request file, it would be deleted when the function exits
# $4 is the tmp reply file, it would be deleted when the function exits
# $5 is the router's admin IP:Port
sendSEMPToRouter () {
  echo `date` "Sending request to router addr ${5}:"
  echo `cat $3`

  httpcode=`curl -u ${1}:${2} -w "%{http_code}" --silent --output $4 -XPOST "http://$5/SEMP" --data-binary @$3`
  returncode=$?

  if [ "$returncode" == "0" ]
  then
    if [ $httpcode -ge 300 ]; then
      echo `date` "WARN: Error sending SEMP command to router with return code: $httpcode"; cat $1; cat $2
    else
      echo `date` "Return code: $httpcode"
    fi
    echo "Reply:"
    cat $4
    echo
  else
    echo `date` "ERROR: Non zero curl return code"
  fi

  # clean up
  rm $3
  rm $4
}

for addr in "${host[@]}"
do

  sempv=${semp[$index]}
  sempv=$(sed 's./.\\/.g' <<< $sempv)  # insert backslash before /

  # Mapping hostname from host array
  #
  #
  postfix=`date +%s%N`
  cp ${cmdlistdir}/cmdlist cmdlist.$postfix
  sed -i "s/SOLTRV/$sempv/g" cmdlist.$postfix
  echo `grep hostname cmdlist.$postfix | cut -d' ' -f 3-` > query.$postfix
  curl -s -u ${username[$index]}:${pwd[$index]} http://$addr/SEMP -d@query.$postfix > processing/hostname.$postfix
  rm query.$postfix cmdlist.$postfix

  parse=( hostname );

  for i in "${parse[@]}"
  do
      val=`grep "<$i>" processing/hostname.$postfix | grep "</$i>" | sed -e "s/.*<$i>\(.*\)<\/$i>.*/\1/"`
      hostname=$val
  done
  unset parse
  rm processing/hostname.$postfix

  if [ -z $hostname ]; then
    echo `date` "ERROR: Router is not there at $addr"
    continue
  fi

  echo `date` "Start sending SEMP commands to router ${hostname}..."

  # Processing maintenancecmdlist
  #
  #
  postfix=`date +%s%N`
  cp ${cmdlistdir}/maintenancecmdlist maintenancecmdlist.$postfix
  sed -i "s/SOLTRV/$sempv/g" maintenancecmdlist.$postfix

  while read type interval cmd
  do
    if [ "$interval" == "$1" ]
    then
      echo `date` "Processing command of interval $interval for router $hostname"

      if [ "$type" == "R1" ];
      then
        echo `date` "Processing command of type $type"
        echo $cmd > query.$postfix
        time=$postfix
        #curl -s -u ${username[$index]}:${pwd[$index]} http://$addr/SEMP -d@query.$postfix > processing/$interval/$1+$type+$hostname+$time
        sendSEMPToRouter ${username[$index]} ${pwd[$index]} query.$postfix reply.$postfix $addr
      else
        # catch all for command of unknown type
        echo `date` "Dont't know how to process maintenance command of type $type"
      fi

    fi # interval = $1
  done < maintenancecmdlist.$postfix

  rm maintenancecmdlist.$postfix

  index=$((index+1))
done   # Finish looping through hosts

