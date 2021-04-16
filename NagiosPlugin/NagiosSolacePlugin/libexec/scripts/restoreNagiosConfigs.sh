#!/bin/bash

# this script restores a backup taken by the script
# backupNagiosConfigs.sh
# the files are extracted to "/" and the original folder
# structure is preserved
# inputs: backed up archive

 usage() {
	echo
	echo "Usage: $0 <Path to backed up archive>"
	echo
	exit 1
 }

##usage##
if [ $# -lt 1 ]; then
        usage
fi

ARCHIVE_NAME=$1

#perform restore
tar zxvfp ${ARCHIVE_NAME} -C / 
