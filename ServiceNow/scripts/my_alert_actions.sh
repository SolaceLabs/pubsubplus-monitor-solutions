#!/bin/bash
# **************************************************************************
# my_alert_actions.sh: Alert Command Script for Unix/Linux
# Copyright (c) 2009 Sherrill-Lubinski Corporation. All Rights Reserved.
# **************************************************************************
# Modifications for ServiceNow REST POST - Solace PSG
#

PSMON_HOME=/app/psmon/RTViewSolaceMonitor/
#
SNOW_HOME=$PSMON_HOME/plugins/ServiceNow
SNOW_CFG=$SNOW_HOME/config/ServiceNow.json
SNOW_POST=$SNOW_HOME/bin/ServiceNowPost.py
PSMON_OUT_FILE=$SNOW_HOME/log/solace-monitor-alerts.out
if [ ! -f $SNOW_CFG ]; then
   echo "ERROR: Unable to locate ServiceNow Config file: $SNOW_CFG . Exiting"
   exit 2
fi

if [ ! -f $SNOW_POST ]; then
   echo "ERROR: Unable to find ServiceNow POST script : $SNOW_POST . Exiting"
   exit 2
fi

touch $PSMON_OUT_FILE || {
   echo "ERROR: Unable to write to log file : $PSMON_OUT_FILE . Exiting"
   exit 2
}

val=$(grep verbose $SNOW_CFG |cut -f2 -d:|sed 's/[ ,\t]//g')
VERBOSE=
[ $val == "true" ] && VERBOSE="-v"


ds=$(date "+%Y/%m/%d-%H.%M.%S")
echo "$ds SET $* ($# args)" >> $PSMON_OUT_FILE
[ "$VERBOSE" == "-v" ] && echo "Running < $SNOW_POST $VERBOSE --set --argv $* >" >> $PSMON_OUT_FILE

cd $SNOW_HOME
$SNOW_POST $VERBOSE --dir $SNOW_HOME --set --argv $* >> $PSMON_OUT_FILE 2>&1

exit
