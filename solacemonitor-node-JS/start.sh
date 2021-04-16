#!/bin/bash


export PATH=$PATH:/root/.nvm/versions/node/v10.15.3/bin

PID_FILE=./pid.file

cmdlistdir="cmd"

intervals=($(cat ${cmdlistdir}/cmdlist ${cmdlistdir}/mvpncmdlist ${cmdlistdir}/clientcmdlist ${cmdlistdir}/cachecmdlist ${cmdlistdir}/bridgecmdlist ${cmdlistdir}/clientusernamecmdlist ${cmdlistdir}/clientpatterncmdlist ${cmdlistdir}/queuecmdlist ${cmdlistdir}/interfacecmdlist | awk '{print $2}' | sort | uniq))

mkdir -p processing
mkdir -p store
mkdir -p intermediate
mkdir -p log

printUsage () {
  echo "Usage: `basename $0` [--log]"
}

if [ -s $PID_FILE ]; then
  echo "$PID_FILE present, please run stop.sh first"
  exit 1
fi

rm -f $PID_FILE

WITH_LOG=0
if [ "x$1" == "x--log" -o "x$1" == "x-l" ]; then
  WITH_LOG=1
fi
if [ "x$1" == "x--help" -o "x$1" == "x-h" ]; then
  printUsage
  exit
fi

# create the old system.config file
if [ ./config/config.json -nt ./config/system.config -o ! -f ./config/system.config ]; then
  ./createOldConfig.sh
fi

# Launching pollrouter
#
for i in "${intervals[@]}"
do
  echo "Creating processing directory queues $i"
  mkdir -p processing/$i

  echo "Polling at interval $i"
  #./pollrouter.sh $i >> log/pollrouter.${i}.log 2>&1 &
  #echo $! "nohup ./pollrouter.sh $i >> log/pollrouter.${i}.log 2>&1 &" >> $PID_FILE
  node --stack-size=2000 pollrouter.js $i >> log/pollrouter.${i}.log 2>&1 &
  echo $! "nohup node --stack-size=2000 pollrouter.js $i >> log/pollrouter.${i}.log 2>&1 &" >> $PID_FILE


done

sleep 2

# Launching postdb
#
for i in "${intervals[@]}"
do
  echo "Processing directory/queue  $i"

  #./postdb.sh $i "*" >> log/postdb.${i}.log 2>&1 &
  #echo $! "nohup ./postdb.sh $i \"*\" >> log/postdb.${i}.log 2>&1 &" >> $PID_FILE
  node --stack-size=2000 postdb.js $i "*" >> log/postdb.${i}.log 2>&1 &
  echo $! "nohup node --stack-size=2000 postdb.js $i \"*\" >> log/postdb.${i}.log 2>&1 &" >> $PID_FILE


done

# Launching accessory scripts

nohup ./interfacestat.sh >> log/interfacestat.log 2>&1 &
echo $! "nohup ./interfacestat.sh >> log/interfacestat.log 2>&1 &" >> $PID_FILE

nohup ./queueprogress.sh >> log/queueprogress.log 2>&1 &
echo $! "nohup ./queueprogress.sh >> log/queueprogress.log 2>&1 &" >> $PID_FILE

nohup ./hostresources.sh >> log/hostresources.log 2>&1 &
echo $! "nohup ./hostresources.sh >> log/hostresources.log 2>&1 &" >> $PID_FILE

nohup ./pingrouter.sh >> log/pingrouter.log 2>&1 &
echo $! "nohup ./pingrouter.sh >> log/pingrouter.log 2>&1 &" >> $PID_FILE

nohup ./compress.sh >> log/compress.log 2>&1 &
echo $! "nohup ./compress.sh >> log/compress.log 2>&1 &" >> $PID_FILE

#nohup ./alertWebHook.sh >> log/alertWebHook.log 2>&1 &
#echo $! "nohup ./alertWebHook.sh >> log/alertWebHook.log 2>&1 &" >> $PID_FILE

#nohup ./postAlertToTEC.sh >> log/postAlertToTEC.log 2>&1 &
#echo $! "nohup ./postAlertToTEC.sh >> log/postAlertToTEC.log 2>&1 &" >> $PID_FILE

