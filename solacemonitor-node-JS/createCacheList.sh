#!/bin/bash

cmdlistdir="cmd"
configdir="config"

# all the router information is in the system.config file
. ${configdir}/system.config

# Please change the following line for the list of client to be monitored
MVPN_LIST=( sinopac )

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
  done
  unset parse
  rm processing/hostname.$postfix

  cp showcache query.$postfix
  curl -s -u ${username[$index]}:${pwd[$index]} http://$addr/SEMP -d@query.$postfix > processing/showcache.$postfix
  num=`xmllint --xpath "count(//cache-instances//cache-instance)" processing/showcache.$postfix`
  
  for curNum in $(seq 1 1 $num)
  do
    mvpn=`xmllint --xpath "//cache-instances//cache-instance[$curNum]" processing/showcache.$postfix | egrep "<message-vpn>" | sed -e "s/.*>\(.*\)<.*/\1/"`
    dcache=`xmllint --xpath "//cache-instances//cache-instance[$curNum]" processing/showcache.$postfix | egrep "<distributed-cache>" | sed -e "s/.*>\(.*\)<.*/\1/"`
    cluster=`xmllint --xpath "//cache-instances//cache-instance[$curNum]" processing/showcache.$postfix | egrep "<cache-cluster>" | sed -e "s/.*>\(.*\)<.*/\1/"`
    cname=`xmllint --xpath "//cache-instances//cache-instance[$curNum]" processing/showcache.$postfix | egrep "<name>" | sed -e "s/.*>\(.*\)<.*/\1/"`
    echo $hostname $mvpn $dcache $cluster $cname
  done
  rm query.$postfix processing/showcache.$postfix
done

