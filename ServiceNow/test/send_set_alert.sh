#!/bin/bash
# send_set_alert.sh

PSMON_HOME=/app/psmon/RTViewSolaceMonitor/
#

echo "=== Sending Test VPN Alert ==="
$PSMON_HOME/projects/rtview-server/my_alert_actions.sh \
SL-SOLMON-1 +SolEventModuleVpnAlert+ "+nram.aws.ec2-13-56-151-95.us-west-1~default~ip-172-31-17-237_VPN_BRIDGING_LINK_DOWN_1560977926991+" +1005+ +2+ +Message VPN 0 default Bridge TESTVPN_default from v:ip-172-31-17-237 VPN TESTVPN down: connection down TESTING from send_set_alarm.sh

echo "=== Sending Test Client Alert ==="
$PSMON_HOME/projects/rtview-server/my_alert_actions.sh \
SL-SOLMON-1 +SolEventModuleClientAlert+ "+nram.aws.ec2-13-56-151-95.us-west-1~TESTVPN~rnatarajan.kan.solacesystems.com/76825/#00100001~ip-172-31-17-237_CLIENT_CLIENT_OPEN_FLOW_1561000752174+" +1022+ +2+ +Client rnatarajan.kan.solacesystems.com/76825/#00100001 username testuser Pub flow session flow name 4d30ac7bf99f4e60b0c9286c19a2d43f , publisher id 38, last message id 0, window size 50 TESTING from send_set_alarm.sh

echo "=== Sending Test System Alert ==="
$PSMON_HOME/projects/rtview-server/my_alert_actions.sh \
SL-SOLMON-1 +SolEventModuleBrokerAlert+ "+nram.aws.ec2-13-56-151-95.us-west-1~ip-172-31-17-237_SYSTEM_CLIENT_ACL_PUBLISH_DENIAL_1561002220496+" +1024+ +2+ +3 publish denials were recorded in the last minute due to ACL profile configuration TESTING from send_set_alarm.sh

echo "=== Sending Test SEMP Alert ==="
$PSMON_HOME/projects/rtview-server/my_alert_actions.sh \
SOLMON-1 +SolEndpointPendingMsgsHigh+ "+vmr-mr8mqb1387rx.messaging.solace.cloud~msgvpn-8ksiwso3hl7~Test_Queue+" +1000+ +1+ +High Warning Limit exceeded, current value: 110.0 limit: 100.0
