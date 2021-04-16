#!/bin/bash

cmdlistdir="cmd"
configdir="config"

# all the router information is in the system.config file
. ${configdir}/system.config

HOST=$1
MVPN=$2
DCACHE=$3
TOPIC=$4

if [ "x$HOST" == "x" -o "x$MVPN" == "x" -o "x$DCACHE" == "x" -o "x$TOPIC" == "x" ]
then
  echo "Usage: `basename $0` <hostname> <mvpn> <dist-cache> <topic to be cleared>"
  exit -1
fi

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
  
  FOUND=0
  if [ $hostname == $HOST ]
  then

    TOPIC=`echo $TOPIC | sed -e "s/>/\&gt;/"`

    echo "clearing cache for host $HOST, mvpn $MVPN, dist-cache $DCACHE with topic $TOPIC..."

    query="<rpc> <admin> <distributed-cache> <name>$DCACHE</name> <vpn-name>$MVPN</vpn-name> <delete-messages> <topic>${TOPIC}</topic> </delete-messages> </distributed-cache> </admin> </rpc>"
  
    #echo $query
    echo $query > query.$postfix
    curl -s -u ${username[$index]}:${pwd[$index]} http://$addr/SEMP -d@query.$postfix
    FOUND=1
    rm query.$postfix
  fi

  if [ $FOUND -eq 1 ]
  then
    exit 0
  fi

done

echo host $HOST not found

