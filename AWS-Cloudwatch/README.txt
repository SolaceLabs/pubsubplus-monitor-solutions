ABOUT
=======================================================================
Script to create Cloudwatch Custom Metric and associate alert based on 
Syslog event filters.

This assumes user already has a working aws CLI session login using IAM 
roles with adequate privilidges.

INPUT FILE
=======================================================================
Input file already has predefined event filters for cloudwatch alerting.


SETUP
=======================================================================
Unzip the bundle
This will place the scripts in AWSTools/ directory and input file 
solace-event-filters.txt under in/ 
Edit aws.env and modify the varaibles as required

USAGE REFERENCE
=======================================================================
* Create Solace metric and alerts from input file

	$ bash provision_aws_cw_solace_alerts.sh -create

	About to create       93 solace event metric in
	log-group : nram-hpmap
	namespace : solace-events
	Proceed ? (y/n) [n]y
	Reading Solace event filters from file : solace-event-filters.txt
	Processing solace event filter:
	   Creating Metric: SYSTEM_CHASSIS_BLADE_DOWN
	   Creating Metric Alarm
	Processing solace event filter:
	   Creating Metric: SYSTEM_CHASSIS_BLADE_POST_CRITICAL_FAILURE
	   Creating Metric Alarm
	Processing solace event filter:
	   Creating Metric: SYSTEM_CHASSIS_BOOT_DISK_FAIL
	   Creating Metric Alarm
	...
	
* List existing metric filters  
	$ bash provision_aws_cw_solace_alerts.sh
	# List of Metric filters in log-group nram-hpmap
	CLIENT_AD_MAX_EGRESS_FLOWS_EXCEEDED
	CLIENT_AD_MAX_INGRESS_FLOWS_EXCEEDED
	CLIENT_AD_TRANSACTED_SESSIONS_EXCEED
	... 

* Delete metric filters from input file
	$ bash provision_aws_cw_solace_alerts.sh -delete 

	*** About to DELETE        93 solace event metrics in
	*** log-group : nram-hpmap
	Proceed ? (y/n) [n]y
	Reading Solace event filters from file : /tmp/delete_alert_list.txt
	   Deleting Metric: CLIENT_AD_MAX_EGRESS_FLOWS_EXCEEDED
	   Deleting Metric: CLIENT_AD_MAX_INGRESS_FLOWS_EXCEEDED
	   Deleting Metric: CLIENT_AD_TRANSACTED_SESSIONS_EXCEED
	   Deleting Metric: CLIENT_AD_TRANSACTED_SESSION_FAIL
	   Deleting Metric: CLIENT_AD_TRANSACTIONS_EXCEED
	   Deleting Metric: CLIENT_CLIENT_ACK_NOT_ALLOWED
