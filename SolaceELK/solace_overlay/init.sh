#!/bin/bash -x

myhost=`ifconfig | grep 'inet addr' | head -1 | sed 's/ *inet addr:\([.0-9]*\).*/\1/'`

unzip -q elasticsearch-1.4.0.zip
unzip -q kibana-3.1.2.zip
tar zxf logstash-1.4.2.tar.gz

tar zxvf solace_overlay.tgz
sed -i "s/__THISHOST__/$myhost/" kibana-3.1.2/config.js

wdir=`pwd`
cd /var/www/html
ln -s $wdir/kibana-3.1.2 .
cd $wdir


./startall.sh
