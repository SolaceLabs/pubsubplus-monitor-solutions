#!/bin/bash

#
# This script compress the files 'store' directory.
#
# This feature is optional.
#
# To run, install as cronjob, e.g.
#
#   */10 * * * * cd /ap/solacemon;./compress.sh
#
# Note that the script assume a fix directory structure.
#

PROG_NAME=`basename $0`

# default to compressing the files every 10 minutes
CHECK_INTERVAL_SEC=600
# Archived response files would kept for 14 days by default
ARCHIVE_DAYS=14
# Max number of files to compress each time
NUM_FILES=10000

if [ ! -z "$SMON_RESPONSES_ARCHIVE_INTVL_SEC" ]; then
  if [ $SMON_RESPONSES_ARCHIVE_INTVL_SEC -ge 30 -a $SMON_RESPONSES_ARCHIVE_INTVL_SEC -le 1800 ]; then
    CHECK_INTERVAL_SEC=$SMON_RESPONSES_ARCHIVE_INTVL_SEC
  fi
fi

if [ ! -z "$SMON_RESPONSES_ARCHIVE_DAYS" ]; then
  if [ $SMON_RESPONSES_ARCHIVE_DAYS -ge 1 -a $SMON_RESPONSES_ARCHIVE_DAYS -le 360 ]; then
    ARCHIVE_DAYS=$SMON_RESPONSES_ARCHIVE_DAYS
  fi
fi

if [ ! -z "$SMON_RESPONSES_ARCHIVE_FILES" ]; then
  if [ $SMON_RESPONSES_ARCHIVE_FILES -ge 500 -a $SMON_RESPONSES_ARCHIVE_FILES -le 90000 ]; then
    NUM_FILES=$SMON_RESPONSES_ARCHIVE_FILES
  fi
fi

DATE=`date '+%Y-%m-%dT%H-%M-%S'`
echo "${DATE}: $PROG_NAME is compressing the responses archive every $CHECK_INTERVAL_SEC seconds (max $NUM_FILES files)..."
echo "${DATE}: $PROG_NAME would remove compressed archive files older than $ARCHIVE_DAYS days..."

cd ./store

ARCHIVED=0

while true; do
  sleep $CHECK_INTERVAL_SEC

  DATE=`date '+%Y-%m-%dT%H-%M-%S'`
  fname="store+$DATE.tgz"

  # in order not to make a single file too large
  find . -type f -not -name "*.tgz" | head -${NUM_FILES} | tar -czf $fname --remove-file -T -

  # Remove archive files older than $ARCHIVE_DAYS days
  # This removal would be done only once when the current time falls onto 00:XX each day
  CUR_HOUR=`date +%H`
  if [ $CUR_HOUR -eq 0 -a $ARCHIVED -eq 0 ]; then
    find ./*.tgz -mtime +$ARCHIVE_DAYS -exec rm -f {} \;
    ARCHIVED=1
  fi
  if [ $CUR_HOUR -ne 0 -a $ARCHIVED -ne 0 ]; then
    ARCHIVED=0
  fi
done

DATE=`date '+%Y-%m-%dT%H-%M-%S'`
echo "${DATE}: $PROG_NAME unexpectedly exited"