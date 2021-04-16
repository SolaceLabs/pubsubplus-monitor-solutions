#!/bin/bash
# show_monitor_status.sh
#   Check ServiceNow.cfg for active/standby status
#   Usage:
#     show_monitor_status.sh
# 
# Jun 28, 2019 Ramesh Natarajan (nram), Solace PSG
#

PSMON_HOME=/app/psmon/RTViewSolaceMonitor/
#
SNOW_HOME=$PSMON_HOME/plugins/ServiceNow
SNOW_CFG=$SNOW_HOME/config/ServiceNow.json
if [ ! -f $SNOW_CFG ]; then
   echo "ERROR: Unable to locate ServiceNow Config file: $SNOW_CFG . Exiting"
   exit 2
fi

SOURCE=$(grep source $SNOW_CFG |cut -f2 -d:|sed 's/[ ,\t]//g')
VAL=$(grep send-alert $SNOW_CFG |cut -f2 -d:|sed 's/[ ,\t]//g')
[ -z $SOURCE ] && {
   echo "ERROR: Unable to read Monitor source name from config. Missing source: tag"
   exit 2
}
[ -z $VAL ] && {
   echo "ERROR: Unable to read Monitor status from config. Missing send-alert: tag"
   exit 2
}

if [ $VAL == "true" ]; then
   echo "Monitor host $SOURCE is active"
   exit 1
fi
if [ $VAL == "false" ]; then
   echo "Monitor host $SOURCE is standby"
   exit 1
fi
echo "ERROR: Unkown Monitor host status $SOURCE. Invalid value for send-alert: tag ($VAL)"
exit 2
