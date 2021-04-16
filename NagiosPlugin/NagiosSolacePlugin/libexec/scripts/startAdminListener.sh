#!/bin/bash

# Wrapper script that calls fireAdminListener.pl to 
# start admin listener processes


 usage() {
	echo
	echo "This is a wrapper that calls the perl script to start admin listener processes."
	echo "Pre-requisites: set the environment variables in adminListinerConfig.sh"
	echo "Usage: $0"
	echo
	exit 1
 }

# source env variables
source /usr/local/nagios/libexec/scripts/adminListenerConfig.sh

##usage##
if [[ $#>0 && $1 == '-h' ]];then
	usage
	exit 0
fi

## set LD LIBRARY PATH
echo "setting LD_LIBRARY_PATH to $SOLCLIENT_HOME/lib.."
export LD_LIBRARY_PATH=$SOLCLIENT_HOME/lib:$LD_LIBRARY_PATH

# ASSUMPTION: the VPN config file is the same as that of the hostname directive of the VPN
for file in "$VPN_DIR"/*
do
	
	#get VPN name from full VPN config file name
	VPN_CFG_FULL_NAME=$(basename $file)
	VPN_NAME="${VPN_CFG_FULL_NAME%%.*}"
	OUTPUT="$(ps aux | grep $ADMIN_LISTENER | grep $VPN_NAME | awk '{print $2,$11,$12,$13}' | grep -v grep | grep -v $0)"

	if [ -n "$OUTPUT" ]; then
        	echo "Admin listener already running for cfg file:$VPN_CFG_FULL_NAME..skipping.."

	else
		#call perl script
        	CMD="$START_ADMIN_LISTENER $ADMIN_LISTENER $file $CREDENTIALS_DIR"
        	#echo $CMD
        	$CMD
        fi
done

echo "Done!"




