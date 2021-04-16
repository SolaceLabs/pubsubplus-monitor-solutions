#!/bin/bash
#provision_aws_cw_solace_alerts.sh
# Wrapper for managing Solace event filter based metric and alerts in AWS Cloudwatch
# Version History
# ---
# 0.3 Jan 14, 2020 
# Ramesh Natarajan, Solace PSG

me="provision_aws_cw_solace_alerts.sh"
myver="0.3"
AWS_ENV_FILE=$PWD/aws.env
VERBOSE="no"

#------------------------------------------------------------------
# usage
#
usage()
{
   echo "
$me ($myver): Provision AWS cloudwatch alerts from Solace event logs

usage: $me action [event-filter-file]
 where action is:
   -help  : this help
   -list  : list metric filters (default if omitted)
   -create: create custom metric filters and alerts
   -delete: delete metric filters
   -debug : create logs with useful aws query commands for troubleshooting

 event-filter-file: file with list of solace events
    ($SOLACE_EVENT_FILTERS_FILE if omitted)
"
}

#------------------------------------------------------------------
# prompt to proceed
#
exit_unless_yes()
{
   echo -n "Proceed ? (y/n) [n]"
   read  yn
   if [ -z "$yn" ] || [ $yn != "y" ]; then
       exit 2
   fi
}

#------------------------------------------------------------------
# run aws (or any) cmd. 
#   write to command.log before running
#
run_cmd()
{
	cmd=$*
	echo "[$(date "+%D %T")] $cmd" >> $COMMAND_LOG_FILE
	$cmd
}

#------------------------------------------------------------------
# parse_event_line
# input: 
#    VPN_AD_MSG_SPOOL_QUOTA_EXCEED,2,3
# output:
#    EVENT_NAME=VPN_AD_MSG_SPOOL_QUOTA_EXCEED
#    DATAPOINTS_TO_ALARM=2
#    EVAL_PERIODS=3
# If last 2 args are missing, leave EVAL vars as is
parse_event_line()
{
	line=$1
	IFS0=$IFS
	IFS=','
	read -ra array <<< "$line"
	EVENT_NAME=${array[0]}
	[[ ${#array[@]} -gt 1 ]] && DATAPOINTS_TO_ALARM=${array[1]}
	[[ ${#array[@]} -gt 2 ]] && EVAL_PERIODS=${array[2]}
	IFS=$IFS0
}

#------------------------------------------------------------------
# process_annotations
# input:
#  @LOG_GROUP_NAME=solace-event
# output:
#  eval LOG_GROUP_NAME=solace-event
process_annotations() {
	line=$1
	
	tag=$(echo $line | cut -f1 -d=)
	val=$(echo $line | cut -f2 -d=)
	tagname=$(echo $tag|sed s/@//)
	value=$(echo $val|sed s/\"//)
    echo $value|grep -q \$ENV && {
        value=$(echo $value|sed "s/\$ENV/$ENV/g")
    }
	echo "[$(date "+%D %T")] Processing annotation $tagname = $value"
	eval "${tagname}"="$value"

   #echo "Override: $line
   #LOG_GROUP_NAME        : $LOG_GROUP_NAME 
   #NOTIFICATION_TOPIC_ARN: $NOTIFICATION_TOPIC_ARN
   #METRIC_FILTER_PREFIX  : $METRIC_FILTER_PREFIX 
   #ALARM_PREFIX          : $ALARM_PREFIX
   #NAMESPACE             : $NAMESPACE
   #"
	
}

#------------------------------------------------------------------
# create_solace_alerts
#   create metric-filter
#   create metric-alarm
create_solace_alerts()
{
   
	EVAL_PERIODS=$DEFAULT_ALARM_EVAL_PERIOD
    DATAPOINTS_TO_ALARM=$DEFAULT_DATAPOINTS_TO_ALARM
	parse_event_line $line
   
   #echo "line            $line
   #EVENT_NAME            $EVENT_NAME
   #DATAPOINTS_TO_ALARM  $DATAPOINTS_TO_ALARM, $DEFAULT_DATA_POINTS_TO_ALARM
   #EVAL_PERIODS    $EVAL_PERIODS, $DEFAULT_EVAL_PERIODS
   #"
   
   FILTER_NAME="${METRIC_FILTER_PREFIX}${EVENT_NAME}"
   ALARM_NAME="${ALARM_PREFIX}${EVENT_NAME}"
   echo "   Creating metric filter: $FILTER_NAME (log-group: $LOG_GROUP_NAME)"
   run_cmd aws  logs put-metric-filter \
      --log-group-name "$LOG_GROUP_NAME" \
      --filter-name "${FILTER_NAME}" \
      --filter-pattern "$EVENT_NAME" \
      --metric-transformation "metricName=$EVENT_NAME,metricNamespace=$NAMESPACE,metricValue=1" \
      $GLOBAL_AWS_ARGS

   echo "   Creating metric alarm : $ALARM_NAME  (trigger: $DATAPOINTS_TO_ALARM / $EVAL_PERIODS)"
   run_cmd aws cloudwatch put-metric-alarm \
      --alarm-name "$ALARM_NAME" \
      --metric-name "$EVENT_NAME" \
      --namespace "$NAMESPACE" \
      --statistic "Sum" \
      --comparison-operator "GreaterThanOrEqualToThreshold" \
      --threshold 1 \
	  --period $ALARM_PERIOD_SECONDS \
	  --evaluation-periods $EVAL_PERIODS \
	  --datapoints-to-alarm $DATAPOINTS_TO_ALARM \
      --alarm-actions "$NOTIFICATION_TOPIC_ARN" \
      $GLOBAL_AWS_ARGS
}

#------------------------------------------------------------------
# delete_solace_metric
#   delete metric-alarm
#   delete metric-filter
delete_solace_metric()
{
   line=$1
   parse_event_line $line
   FILTER_NAME="${METRIC_FILTER_PREFIX}${EVENT_NAME}"
   ALARM_NAME="${ALARM_PREFIX}${EVENT_NAME}"
   echo "   Deleting metric alarm : $ALARM_NAME"
   run_cmd aws cloudwatch delete-alarms \
	   --alarm-names "$ALARM_NAME" \
      $GLOBAL_AWS_ARGS
   
   echo "   Deleting metric filter: $FILTER_NAME"
   run_cmd aws logs delete-metric-filter \
      --log-group-name "$LOG_GROUP_NAME" \
      --filter-name "$FILTER_NAME"  \
      $GLOBAL_AWS_ARGS
}

#------------------------------------------------------------------
# list_solace_metrics
#   list metric filters & metric-alarms
# NOTE: filter-name-prefix doesn't work with "aws logs describe-metric-filters"
list_solace_metrics()
{
	echo "
#-----------------------------------------------------------
# Solace metric filters (filter-prefix: $METRIC_FILTER_PREFIX)
#-----------------------------------------------------------
"
	run_cmd aws logs describe-metric-filters \
	   --filter-name-prefix $METRIC_FILTER_PREFIX \
	   --output table \
	   --query "metricFilters[*].filterName" \
	   --color off \
	   $GLOBAL_AWS_ARGS \
		   | grep -v HPMAP # drop HPMAP metrics in HP env
	                       # hack to work around --filter-name-prefix issue

	echo "
#-----------------------------------------------------------
# Solace metric alarms (alarm-prefix: $ALARM_PREFIX)
#-----------------------------------------------------------
"
	run_cmd aws cloudwatch describe-alarms \
		--alarm-name-prefix $ALARM_PREFIX \
	    --output table \
		--query "MetricAlarms[*].AlarmName" \
	    $GLOBAL_AWS_ARGS
}

#------------------------------------------------------------------
# create_debug_logs
#  create list of aws query commands to capture the state
create_debug_logs()
{
   [ -d out ] || mkdir out
   [ -d out ] || { echo "Unable to create output dir $PWD/out. Exiting"; exit 2;  }
   #rm -f out/* 2> /dev/null
   echo "Creating debug logs ..."

   # generate debug info
   hostname > out/hostname.out
   run_cmd aws logs describe-log-groups --output json \
	   $GLOBAL_AWS_ARGS > out/aws-logs-describe-log-groups.out
   run_cmd aws logs describe-metric-filters --log-group-name $LOG_GROUP_NAME --output json \
	   $GLOBAL_AWS_ARGS >  out/aws-logs-describe-metric-filters.out
   run_cmd aws cloudwatch describe-alarms --alarm-name-prefix Solace --output json \
	   $GLOBAL_AWS_ARGS > out/aws-cloudwatch-describe-alarms.out
   run_cmd aws sns list-topics --query "Topics[*].TopicArn" --output json \
	   $LOG_GROUP_NAME > out/aws-sns-list-topics.out
   # create zip file with all logs
   zipfile="debug-logs-$(hostname).zip"
   rm -f $zipfile
   zip -r $zipfile out || { echo "Unable to create output zip file $zipfile. Exiting"; exit 2;  }
   echo "   debug logs archived in $zipfile"
}

#---------------------------------------------------------------------------------
# MAIN
#---------------------------------------------------------------------------------


#---------------------------------------------------------------------------------
# Read aws.env file
#
if [ -f $AWS_ENV_FILE ]; then
    echo "Reading env file $AWS_ENV_FILE"
    . $AWS_ENV_FILE
else
    echo "Env file $AWS_ENV_FILE not found. Exiting"
    exit 1
fi

#---------------------------------------------------------------------------------
# parse args
#
if [ $# -gt 0 ]; then
   [ $1 == "-verbose" ] && { VERBOSE="yes"; echo "Verbose on"; shift; }
fi

action="-list"
if [ $# -gt 0 ]; then
   if [ $1 == "-env" ]; then
      ENV=$2
      shift; shift
      echo "Got Env $ENV"
   fi
fi
if [ $# -gt 0 ]; then
   action=$1
   shift
fi

event_filter_file=$SOLACE_EVENT_FILTERS_FILE
if [ $# -gt 0 ]; then
   event_filter_file=$1
   shift
fi
[ -f $event_filter_file ] || {
   echo "ERROR: Unable to read input file: $event_filter_file"
   exit 2
}

	#------------------------------------------------------------------
	# process actions
	#
if [ $action == "-help" ]; then
   usage
   exit 1
elif [ $action == "-debug" ]; then
	create_debug_logs
    exit 1
	
	#------------------------------------------------------------------
	# list
	#
elif [ $action == "-list" ]; then
   echo "# List of metric filters in log-group $LOG_GROUP_NAME"
   list_solace_metrics
   
   #------------------------------------------------------------------
   # create
   #
elif [ $action == "-create" ]; then
   echo -n "About to create $(egrep ^[A-Z] $event_filter_file |wc -l) solace event metric from $event_filter_file
"
   exit_unless_yes
   echo "Reading Solace event filters from file : $event_filter_file"
   for line in $(egrep -v ^# $event_filter_file); do
	  [[ -z "${line// }" ]] && continue
	  echo $line | grep -q "^@" && { process_annotations $line; continue; }  
      echo "[$(date "+%D %T")] Processing Solace event filter: $line"
      create_solace_alerts "$line"
   done
   
   #------------------------------------------------------------------
   # delete
   #
elif [ $action == "-delete" ]; then
   echo -n "*** About to DELETE $(egrep ^[A-Z] $event_filter_file |wc -l) solace metric from $event_filter_file
"
   exit_unless_yes
   echo "Reading Solace event filters from file : $event_filter_file"
   for line in $(egrep -v ^# $event_filter_file); do
	  [[ -z "${line// }" ]] && continue
	  echo $line | grep -q "^@" && { process_annotations $line ; continue; }  
      echo "[$(date "+%D %T")] Processing Solace event filter: $line"
      delete_solace_metric $line
   done
else
   echo "ERROR: Unknown action. Should be one of [-list,-create,-delete]"
   exit 2
fi
