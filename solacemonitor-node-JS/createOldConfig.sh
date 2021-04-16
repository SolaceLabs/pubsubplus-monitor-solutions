#!/bin/bash

set -m

legacy_config_file=config/system.config
new_config_file=config/config.json

host_arr=`cat config/config.json | jq '.solaceHosts[] | .url' | sed -e 's/^"//' -e 's/"$//' | paste -sd " " -`
echo '# router config' > $legacy_config_file
echo "host=( $host_arr )" >> $legacy_config_file


username_arr=`cat config/config.json | jq '.solaceHosts[] | .username' | sed -e 's/^"//' -e 's/"$//' | paste -sd " " -`
echo "username=( $username_arr )" >> $legacy_config_file

pwd_arr=`cat config/config.json | jq '.solaceHosts[] | .pwd' | sed -e 's/^"//' -e 's/"$//' | paste -sd " " -`
echo "pwd=( $pwd_arr )" >> $legacy_config_file

semp_arr=`cat config/config.json | jq '.solaceHosts[] | .sempVer' | sed -e 's/^"//' -e 's/"$//' | paste -sd " " -`
echo "semp=( $semp_arr )" >> $legacy_config_file

echo "" >> $legacy_config_file
echo '# InfluxDB config' >> $legacy_config_file

dbname=`cat config/config.json | jq '.influxDB.db'`
echo db=$dbname >> $legacy_config_file

dbusername=`cat config/config.json | jq '.influxDB.username' | sed -e 's/^"//' -e 's/"$//'`
echo "dbuser=$dbusername" >> $legacy_config_file

pwd=`cat config/config.json | jq '.influxDB.pwd' | sed -e 's/^"//' -e 's/"$//'`
echo "PASSWORD=$pwd" >> $legacy_config_file

dbhost=`cat config/config.json | jq '.influxDB.host'`
echo "dbhost=$dbhost" >> $legacy_config_file

echo "" >> $legacy_config_file
echo '# InfluxDB continuous queries config' >> $legacy_config_file

duration=`cat config/config.json | jq '.influxDB.continuousQuery.duration'`
echo "duration=$duration" >> $legacy_config_file

rpd=`cat config/config.json | jq '.influxDB.continuousQuery.rpd'`
echo "rpd=$rpd" >> $legacy_config_file

rp=`cat config/config.json | jq '.influxDB.continuousQuery.rp'`
echo "rp=$rp" >> $legacy_config_file

admindbuser=`cat config/config.json | jq '.influxDB.continuousQuery.adminUser' | sed -e 's/^"//' -e 's/"$//'`
echo "admindbuser=$admindbuser" >> $legacy_config_file

adminpassword=`cat config/config.json | jq '.influxDB.continuousQuery.adminPassword' | sed -e 's/^"//' -e 's/"$//'`
echo "adminPASSWORD=$adminpassword" >> $legacy_config_file

echo "" >> $legacy_config_file
echo '# interfacestat.sh parameter' >> $legacy_config_file

netif=`cat config/config.json | jq '.interface_stat.networkInterface' | sed -e 's/^"//' -e 's/"$//'`
echo "NETIF=$netif" >> $legacy_config_file

