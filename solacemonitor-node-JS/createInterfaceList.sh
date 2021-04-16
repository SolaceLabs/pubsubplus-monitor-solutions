#!/bin/bash

cmdlistdir="cmd"
configdir="config"

# all the router information is in the system.config file
. ${configdir}/system.config

# DO NOT MODIFY BELOW

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

  curl -s -X POST -u ${username[$index]}:${pwd[$index]} http://$addr/SEMP -d "<rpc> <show> <interface></interface> </show> </rpc>" | grep  phy-interface | grep -v chassis | grep -v eth | grep -v lag | sed -e "s/.*<phy-interface>\(.*\)<\/phy-interface>.*/$hostname \1/"
done

