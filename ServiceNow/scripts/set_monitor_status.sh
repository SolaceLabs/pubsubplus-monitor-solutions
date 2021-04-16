#!/bin/bash
# set_monitor_status.sh
#   Update ServiceNow.cfg for active/standby status
#   Usage:
#     set_monitor_status.sh [active|standby]
#
# Note: 
#   Assumes , at the end of tag. Don't make send-alerts last tag in block
# 
# Jun 28, 2019 Ramesh Natarajan (nram), Solace PSG
#

PSMON_HOME=/app/psmon/RTViewSolaceMonitor/
JSONLINT=/usr/bin/jsonlint
#
SNOW_HOME=$PSMON_HOME/plugins/ServiceNow
SNOW_CFG=$SNOW_HOME/config/ServiceNow.json
if [ ! -f $SNOW_CFG ]; then
   echo "ERROR: Unable to locate ServiceNow Config file: $SNOW_CFG . Exiting"
   exit 2
fi
[ $# -lt 1 ] && {
   echo "ERROR: Missing Argument"
   echo "Usage: $0 <status> where status: is \"active\" or \"standby\""
   exit
}
STARG=$1
NEWVAL=""
[ $STARG == "active" ]   && NEWVAL="true"
[ $STARG == "standby" ]  && NEWVAL="false"
[ -z $NEWVAL ] && {
   echo "ERROR: Invalid argument $STARG . Exiting"
   echo "Valid values: active, standby"
   exit 2
}
echo "Set config to $STARG (send-alert : $NEWVAL)"
VAL=$(grep send-alert $SNOW_CFG |cut -f2 -d:|sed 's/[ ,\t]//g')
if [ $VAL == $NEWVAL ]; then
   echo "Config is already $STARG. No update required."
   exit 1
fi

# good to make changes. mv file to old version and update send-alerts
TS=$(date "+%y%m%d_%H%M%S.%N")
echo "Backing up config file to $SNOW_CFG.$TS"
mv $SNOW_CFG $SNOW_CFG.$TS
echo "Current send-alert value : $VAL"
sed "s#\(^.*\)\"send-alerts.*#\1\"send-alerts\" : $NEWVAL,#" $SNOW_CFG.$TS > $SNOW_CFG
VAL=$(grep send-alert $SNOW_CFG |cut -f2 -d:|sed 's/[ ,\t]//g')
echo "Updated send-alert value : $VAL"

[ -x $JSONLINT ] && $JSONLINT -q $SNOW_CFG && echo "$SNOW_CFG is valid JSON"
