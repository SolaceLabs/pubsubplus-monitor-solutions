#!/bin/bash

# This script automatically populate continuous queries into influxdb.
#
# The required input are cqmeasurementlist and cqcmdlist:
#
#    - cqmeasurementlist contains a list of measurement to downsample as well as how to downsample (e.g. take a mean value or take the max value),
#    - cqcmdlist contains the continuous query command list.

# Below are the templates (cqcmdlist) which cq.sh script uses to generate the actual insert statement for populating the continuous queries into influxdb. 
#
# CREATE CONTINUOUS QUERY "CQ" ON DB BEGIN SELECT mean(value) AS mean_value INTO DB.RPD."DOWNSAMPLEDME" FROM DB.RP."ME" GROUP BY time(DURATION), * END
# CREATE CONTINUOUS QUERY "CQ" ON DB BEGIN SELECT max(value) AS max_value INTO DB.RPD."DOWNSAMPLEDME" FROM DB.RP."ME" GROUP BY time(DURATION), * END
#
#
# To use:
#
#  1. Create a new retention policy 'a_year', e.g. from within influx
# 
#      create retention policy a_year on capitalmondb duration 365d replication 1
#
#  2. Modify the retention policy for autogen from perpectual to a reasonable value, e.g. from within influx 
#
#      alter retention policy autogen on capitalmondb duration 720h replication 1
#
#  3. Run the cq.sh script.
#
#
# Note:
#
#  A. To verify a downsampled measurement (notice the addition of a_year)
#
#      select * from a_year."downsampled_cachecluster-num-clusters-configured"
#

#

# DO NOT MODIFY BELOW
cmdlistdir="cmd"
configdir="config"

. ${configdir}/system.config
dbuser=$admindbuser
PASSWORD=$adminPASSWORD

while read method measurement
do
	postfix=`date +%s%N`
	cp ${cmdlistdir}/cqcmdlist cqcmdlist.$postfix
	sed -i "s/DURATION/$duration/g" cqcmdlist.$postfix
	sed -i "s/RPD/$rpd/g" cqcmdlist.$postfix # must be before RP
	sed -i "s/RP/$rp/g" cqcmdlist.$postfix
	sed -i "s/DB/$db/g" cqcmdlist.$postfix
	#sed -i "s/TARGETDB/$db/g" cqcmdlist.$postfix

	downsampled_me="downsampled_$measurement"
	sed -i "s@DOWNSAMPLEDME@"$downsampled_me"@g" cqcmdlist.$postfix   # use @ as delimiter rather than /
	continuous_query="cq_${duration}_${measurement}"   # need {} as concat two variables with underscore
        sed -i "s@CQ@$continuous_query@g" cqcmdlist.$postfix
        sed -i "s@ME@$measurement@g" cqcmdlist.$postfix

	echo "Processing $measurement"

	if [ "$method" == "max" ]
	then
		cmd=`grep "max(value)" cqcmdlist.$postfix`
		echo "q=$cmd" > insert.$postfix
		httpcode=`curl -u $dbuser:$PASSWORD -w "%{http_code}" --silent --output /dev/null -i -XPOST "http://$dbhost/query" --data-binary @insert.$postfix`
		returncode=$?
  	  	if [ "$returncode" == "0" ]  # curl's return code, 0 means no issue.
         	then
             		echo "HTTP return code: $httpcode"
             		rm insert.$postfix
          	else
             		echo "ERROR: Non zero curl return code"
           	fi
	elif [ "$method" == "mean" ] 
	then
                cmd=`grep "mean(value)" cqcmdlist.$postfix`
                echo "q=$cmd" > insert.$postfix
                httpcode=`curl -u $dbuser:$PASSWORD -w "%{http_code}" --silent --output /dev/null -i -XPOST "http://$dbhost/query" --data-binary @insert.$postfix`
                returncode=$?
                if [ "$returncode" == "0" ]  # curl's return code, 0 means no issue.
                then
                        echo "HTTP return code: $httpcode"
                        rm insert.$postfix
                else
                        echo "ERROR: Non zero curl return code"
                fi
	else
		echo "Must be none method"
	fi

	rm cqcmdlist.$postfix

done < ${configdir}/cqmeasurementlist

