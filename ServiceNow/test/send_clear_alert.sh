#!/bin/bash
# send_clea_alert.sh

PSMON_HOME=/app/psmon/RTViewSolaceMonitor/
#
echo "=== Sending Test VPN Clear Alert ==="
$PSMON_HOME/projects/rtview-server/my_alert_actions.cleared.sh \
SL-SOLMON-1 +SolEventModuleVpnAlert+ "+nram.aws.ec2-13-56-151-95.us-west-1~default~ip-172-31-17-237_VPN_BRIDGING_LINK_DOWN_1560978859818+" +1011+ +2+ +Message VPN 0 default Bridge TESTVPN_default from v:ip-172-31-17-237 VPN TESTVPN down: connection down 18 args TESTING from send_clear_alert.sh
