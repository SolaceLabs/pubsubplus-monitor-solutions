#!/bin/bash

  usage() {
        echo
        echo "This is script stops all running admin listener processes"
        echo "Pre-requisites: set the environment variables in adminListnerConfig.sh"
        echo "Usage: $0"
        echo
        exit 1
 }


source /usr/local/nagios/libexec/scripts/adminListenerConfig.sh

##usage##
if [[ $#>0 && $1 == '-h' ]]; then
	usage
	exit 0
fi

echo
OUTPUT="$(ps aux | grep $ADMIN_LISTENER | awk '{print $2,$11,$12,$13}' | grep -v grep | grep -v $0)"

if [ -n "$OUTPUT" ]; then
	echo "**Going to stop the following admin listener processes**"
	echo
	echo "${OUTPUT}"
	#kill all the matching processes
	CMD="pkill -f $ADMIN_LISTENER "
	#echo $CMD
	$CMD
	echo
	echo "Done!"
else
	echo "There are no admin listener processes currently running.."
fi

echo





