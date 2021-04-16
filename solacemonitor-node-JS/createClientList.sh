#!/bin/bash

cmdlistdir="cmd"
configdir="config"

# DO NOT MODIFY BELOW

# all the router information is in the system.config file
. ${configdir}/system.config

# Please change the following line for the list of client to be monitored
CLIENT_LIST=( MEOW A B C )

for addr in "${host[@]}"
do

  # Mapping hostname from host array
  #
  #
  postfix=`date +%s%N`
  echo `grep hostname $cmdlistdir/cmdlist | cut -d' ' -f 3-` > query.$postfix
  curl -s -u ${username[$index]}:${pwd[$index]} http://$addr/SEMP -d@query.$postfix > processing/hostname.$postfix
  rm query.$postfix

  parse=( hostname );

  for i in "${parse[@]}"
  do
    val=`grep "<$i>" processing/hostname.$postfix | grep "</$i>" | sed -e "s/.*<$i>\(.*\)<\/$i>.*/\1/"`
    hostname=$val
    #echo "val = $val"
  done
  unset parse
  rm processing/hostname.$postfix

  for i in "${CLIENT_LIST[@]}"
  do
    curl -s -X POST -u ${username[$index]}:${pwd[$index]} http://$addr/SEMP -d "<rpc> <show> <client> <name>*${i}*</name> </client> </show> </rpc>" | grep "<name>" | grep "</name>" | sed -e "s/.*<name>\(.*\)<\/name>.*/$hostname \1/"
  done

done

