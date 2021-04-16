#!/bin/bash

configDir=config

. ${configDir}/system.config

showBridgeCmd="<rpc semp-version=\"SOLTRV\"> <show> <bridge> <bridge-name-pattern>*</bridge-name-pattern> </bridge> </show> </rpc>"
showHostnameCmd="<rpc semp-version=\"SOLTRV\"> <show> <hostname></hostname> </show> </rpc>"

for addr in "${host[@]}"
do

  sempv=${semp[$index]}
  sempv=$(sed 's./.\\/.g' <<< $sempv)  # insert backslash before /

  postfix=`date +%s%N`
  echo $showBridgeCmd > query.$postfix
  sed -i "s/SOLTRV/$sempv/g" query.$postfix
  curl -s -u ${username[$index]}:${pwd[$index]} http://$addr/SEMP -d@query.$postfix > output.$postfix

  echo $showHostnameCmd > query.$postfix
  hostname=`curl -s -u ${username[$index]}:${pwd[$index]} http://$addr/SEMP -d@query.$postfix | grep "<hostname>" | grep "</hostname>" | sed  -e "s/.*<hostname>\(.*\)<\/hostname>.*/\1/"`

  rm query.$postfix


  numOfBridges=`xmllint --xpath "count(//bridge/bridges/bridge)" output.$postfix`
  for curNum in $(seq 1 1 $numOfBridges); do
    bridgeXML=currentBridge-${curNum}.$postfix
    xmllint --xpath "//bridge/bridges/bridge[$curNum]" output.$postfix > $bridgeXML
    mvpn=`grep "<local-vpn-name>" $bridgeXML | sed -e "s/.*<local-vpn-name>\(.*\)<\/local-vpn-name>.*/\1/"`
    bridgeName=`grep "<bridge-name>" $bridgeXML | sed -e "s/.*<bridge-name>\(.*\)<\/bridge-name>.*/\1/"`
    if [ "${bridgeName:0:1}" == "#" ]; then
      rm $bridgeXML
      continue
    fi
    echo $hostname $mvpn $bridgeName
    rm $bridgeXML
  done
  rm output.$postfix
done

