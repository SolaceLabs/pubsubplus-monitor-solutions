#!/bin/bash
# copy_configs
#   Helper script to copy configs for local env
# Ramesh Natarajan, Solace PSG
# Jul 8, 2019

DST=../../../projects/rtview-server/
if [ ! -d $DST ]; then
	echo "Destination Directory $DST not found. Exiting"
	exit 2
fi
echo "Copying files to $DST"
for file in `ls my_alert_actions*sh`; do
	echo " $file"
	[ -f $DST/$file ] && mv $DST/$file $DST/$file.bak-$(date "+%Y%m%d")
	cp -p $file $DST
done
