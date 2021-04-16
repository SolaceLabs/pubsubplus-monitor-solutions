#!/bin/bash
# fix_configs
#   Helper script to fix configs for local env
# Usage:
#   fix_configs  <psmon-home> [<source-name>]
#   eg: ./fix_configs /app/psmon/RTViewSolaceMonitor solace-monitor.dev 
#   hostname used if source-name is not supplied
#
# Ramesh Natarajan, Solace PSG
# Jul 8, 2019


usage() {
   echo "
$0 - Fix ServiceNow Plugin configs

Usage:
$0 [-h] [-f] <psmon-directory> [<source-name>]
   -h              : usage help
   -f              : force config changes. Without this, files won't be regenerated if already fixed.
   psmon-directory : RTViewSolaceMonitor install dir
   source-name     : Source name to use for ServiceNow. Default is hostname

Sample Usage:
   $0  /app/psmon/RTViewSolaceMonitor
   $0  /app/psmon/RTViewSolaceMonitor solace-monitor.dev
   "
}

fix_file() {
	file=$1
    [ $force == 1 ] && cp org/$file .
    grep -q CHANGE_ME $file || {
		#echo " - $file"
		return
    }
    if [ $force == 1 ] ; then
		echo " * $file"
	else
		echo " + $file"
		mv $file org 
	fi
    sed "s^CHANGE_ME_SOURCE_NAME^$SOURCE_NAME^g" org/$file | \
    sed "s#CHANGE_ME_PSMON_HOME#$PSMON_HOME#g" > $file
}

fix_dir() {
	chmod 754 *.sh 2> /dev/null
	chmod 644 *.json 2> /dev/null
	chmod 440 org/* 2> /dev/null
}

prep_dir() {	
   echo "> $PWD"
   [ ! -d org ] && mkdir org 2> /dev/null
}

# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
if [[ $# -lt 1 ]] || [[ $1 == "-h" ]]; then
   usage
   exit 1
fi

force=0
if [ $1 == "-f" ]; then
    force=1
    shift
fi

if [ $# -lt 1 ]; then
   usage
   exit 1
fi

PSMON_HOME=$1
SOURCE_NAME=${2:-$(hostname)}

if [ ! -d $PSMON_HOME ]; then
	echo "Invalid psmon-directory argument: $PSMON_HOME. Exiting"
	exit 2
fi
echo "Fixing config files with 
---
SOURCE_NAME  : $SOURCE_NAME
PSMON_HOME   : $PSMON_HOME
---"
[ $force == 1 ] && echo " * Running with force option *"
echo -n "Do you want to continue (y/n) [n] ?"
read a
[ $a != "y" ] && { echo "Exiting."; exit 1; }

prep_dir
for file in $(ls *.sh); do
	fix_file $file 
done
fix_dir

cd ../config
prep_dir
for file in $(ls *.json); do
	fix_file $file 
done
fix_dir

cd ../test
prep_dir
for file in $(ls send*.sh); do
	fix_file $file
done
fix_dir
