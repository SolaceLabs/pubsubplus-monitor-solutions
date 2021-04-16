#!/bin/bash

# this script backs up the "etc" directory in Nagios containing 
# configuration information
# inputs: path to NAGIOS_HOME: usually /usr/local/nagios
#       : destination directory - defaults to "/tmp"
# output: backed up archive

 usage() {
	echo
	echo "Usage: $0 <Path to NAGIOS_HOME> <destination dir(optional - defaults to /tmp> "
	echo
	exit 1
 }

##usage##
if [ $# -lt 1 ]; then
        usage
fi

#remove training /
BACKUP_DIR=${1%/}
BACKUP_DIR="${BACKUP_DIR}/etc"
echo $BACKUP_DIR

DEST_DIR=$2

## if destination directorys is not specified then
##  default to /tmp
if [ -z $DEST_DIR ]; then
	DEST_DIR="/tmp"
else
	#remove trailing /
	DEST_DIR=${2%/}
fi

BACKUP_DATE=`date +%Y_%m_%d_%H_%M_%S`
ARCHIVE_NAME="${DEST_DIR}/backup-${BACKUP_DATE}.tgz"

#perform backup
tar pcfzP ${ARCHIVE_NAME} ${BACKUP_DIR} 2>&1

echo "Archive created at: $ARCHIVE_NAME"
