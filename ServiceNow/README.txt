Solace PubSub+ Monitor ServiceNow Plugin 
Version 1.1

ABOUT
=============================================================================
Scripts to REST Post Alerts from Solace Monitor to ServiceNow endpoint.

REQUIREMENTS & PRE-REQUISITES
=============================================================================

1. On RTView Solace Monitor Admin Page, Under Alerts view,
   Make sure the following options are enabled:
       [X] Enable Alert notification
           Notification Platform (*) Unix <-- Select Unix
       [X] Notify on New Alerts
       [X] Notify on first severity Change
       [X] Notify On cleared Alerts
   Save and Restart Server if required.

2. Python 2.7
   Python JSON module
   Install required Python Modules using pip / package manager on your host such as rpm
   Eg:
   $ sudo rpm -i python-json-3.4-8.mga6.noarch.rpm or
   $ sudo yum localinstall python-json-3.4-8.mga6.noarch.rpm
     Assumes python-json-3.4-8.mga6.noarch.rpm in local repository or wget
 
   $ sudo yum install python-flask 
     Optional package and used my simple_rest_server used for testing. 
     Ok to continue with out this
 
   $ sudo npm install jsonlint -g 
     Couldn't find rpm/yum repository. 
     This is an optional tool and its ok to skip installing this
 
3. Make sure the connection names don't have spaces or special characters in them.
   Stick to char,numbers and dashes

INSTALLATION
=============================================================================

1. Get the latest SolaceMonitor_ServiceNow_Plugin  from Solace filedrop and place it /tmp
   https://sftp.solacesystems.com/psg/TMX-AMER-011_Solace_Monitor_2019/Tools/
   Eg: /tmp/SolaceMonitor_ServiceNow_Plugin-1.0.5.tar.gz

2.1 If this is the first time deploying the plugins, create plugins directory
    $ cd /app/psmon/RTViewSolaceMonitor/
    $ mkdir plugins

2.2 If you are re-deploying a different version, save the current copy.
 $ cd /app/psmon/RTViewSolaceMonitor/plugins
 $ mv ServiceNow ServiceNow.bak-$(date "+%Y%m%d")

3. . Unzip the file under RTViewSolaceMonitor/plugins folder.
    $ cd /app/psmon/RTViewSolaceMonitor/plugins
    $ tar xvzf /tmp/SolaceMonitor_ServiceNow_Plugin-1.0.5.tar.gz

4.  Run scripts/fix_configs
 $ cd /app/psmon/RTViewSolaceMonitor/plugins/ServiceNow/scripts
 $ $ ./fix_configs.sh /app/psmon/RTViewSolaceMonitor
    Fixing config files with
    SOURCE_NAME  : ip-172-31-29-216.us-west-1.compute.internal
    PSMON_HOME   : /app/psmon/RTViewSolaceMonitor

 - /app/psmon/RTViewSolaceMonitor/plugins/ServiceNow/scripts/my_alert_actions.cleared.sh
 - /app/psmon/RTViewSolaceMonitor/plugins/ServiceNow/scripts/my_alert_actions.sh
 - /app/psmon/RTViewSolaceMonitor/plugins/ServiceNow/scripts/send_clear_alert.sh
 - /app/psmon/RTViewSolaceMonitor/plugins/ServiceNow/scripts/send_set_alert.sh
 - /app/psmon/RTViewSolaceMonitor/plugins/ServiceNow/scripts/set_monitor_status.sh
 - /app/psmon/RTViewSolaceMonitor/plugins/ServiceNow/scripts/show_monitor_status.sh
 - /app/psmon/RTViewSolaceMonitor/plugins/ServiceNow/config/ServiceNow.json


5.  Edit the above files and make sure they are modified correctly.

6. Run copy_scripts to move files to right location

     $ cd /app/psmon/RTViewSolaceMonitor/ServiceNow/plugins/scripts
     $ ./copy_configs.sh
    Copying files to ../../../projects/rtview-server/
     - my_alert_actions.cleared.sh
     - my_alert_actions.sh

7. Run plugins/ServiceNow/test/send_test_alert.sh and plugins/ServiceNow/test/send_clear_alert.sh
    $ ./send_set_alert.sh
    === Sending Test VPN Alert ===
    === Sending Test Client Alert ===
    === Sending Test System Alert ===
    === Sending Test SEMP Alert ===

8. Check Logs and ServiceNow portal
    Logs: plugins/ServiceNow/log/ServiceNowPost.log &
          plugins/ServiceNow/solace-monitor-alerts.out

USAGE
=============================================================================

1. Suppressing sending alerts to some servicenow endpoints.

    To stop sending alerts to some ServiceNow endpoint, add
    "enabled" : "false" in ServiceNow.json /

    Eg:

	"servers": [{
		{
			"url": "http://localhost:3000",
			"endpoint": "post",
			"username": "",
			"password": "",
			"enabled": true
		},
		{
			"url": "http://localhost:3001",
			"endpoint": "post",
			"username": "",
			"password": "",
			"enabled": false
		}
	]

	In this case, alerts will be sent to localhost:3000 but not to localhost:3001

2. Configuring a monitor instance in standby

    If you have multiple PubSub+ Monitor instances with a single active node,
    keep the others in standby. This can be done by either setting
    "send-alerts" : "false"
    in ServiceNow.json host block

    This can also be done with the following script:

		[ec2-user@ip-172-31-29-216 scripts]$ ./set_monitor_status.sh standby
        Set config to standby (send-alert : false)
        Backing up config file to /app/psmon/RTViewSolaceMonitor//plugins/ServiceNow/config/ServiceNow.json.190912_134737.566229273
        Current send-alert value : true
        Updated send-alert value : false
        /app/psmon/RTViewSolaceMonitor//plugins/ServiceNow/config/ServiceNow.json is valid JSON

        [ec2-user@ip-172-31-29-216 scripts]$ ./show_monitor_status.sh
        Monitor host "ip-172-31-29-216" is standby


3. Running a local REST server for testing

    Use simple_rest_server.py in test dir. It takes an optional port argument with the default
    port of 3000

    [ec2-user@ip-172-31-29-216 test]$ ./simple_rest_server.py -h
        usage: simple_rest_server [-h] [--ip IP] [--port PORT] [-v]

        simple_rest_server

        optional arguments:
          -h, --help     show this help message and exit

        Optional:
          --ip IP        IP to start service
          --port PORT    port to start service
          -v, --verbose  Verbose mode

    [ec2-user@ip-172-31-29-216 test]$ ./simple_rest_server.py
    Starting REST service at: 127.0.0.1:3000 ...
     * Serving Flask app "simple_rest_server" (lazy loading)
     * Environment: production
       WARNING: This is a development server. Do not use it in a production deployment.
       Use a production WSGI server instead.
     * Debug mode: off
     * Running on http://127.0.0.1:3000/ (Press CTRL+C to quit)


4. Suppressing alerts from specific node/connection.

    To supress alerts from specific node/connection, add them to alert_blocklist/nodes block
    in ServiceNow.jsob. This is useful when alerts are expected from these nodes during ignorable
    activities like testing, planned maintenance, etc.

    	"alert_blacklist": {
			"nodes" : [
              "META-Comment: List of Nodes to suppress alerts",
              "aws-ip172.31.17.237",
              "nram-test-node"
			],
        ...
		}

		Alerts from aws-ip172.31.17.237 & nram-test-node will no longer be sent to ServiceNow
		endpoints.

5. Suppressing alerts by event name

   Similar to connection level, alerts can also be supressed by event name.
   Use events sub-block.

   This is for suppressing temporary alert conditions. For more permenant solution
   use ./rtvapm/solmon/soleventmodule/config/soleventmodule.properties
   ALERT WHITELIST / BLACKLIST

   Eg:

		        "alert_blacklist": {
                        "nodes" : [
                          "META-Comment: List of nodes/events to suppress alerts"
                        ],
                        "events" : [
                           "META-Comment: List of nodes/events to suppress alerts",
                           "SYSTEM_AUTHENTICATION_SESSION_OPENED",
                           "VPN_BRIDGING_LINK_REJECTED"
                        ]
                },

HISTORY
=============================================================================
1.1   Added support for disabling alert post by connection. (RT 37320)
      Cleaned up resource names for syslog alerts so ServiceNow can group by key (RT 37334)

1.0.5 Fixed show_monitor_status.sh script errors
      Added fix_configs and copy_configs for simple installation
	  Moving all shell scripts under scripts/ folder
	  Updated the docs
	  
1.0.4 Added support for multiple ServiceNow endpoints
      Made running jsonlint optional

1.0.3 Added script (set_monitor_status.sh) to change status (active/standby)
      Read verbose value from ServiceNow.cfg instead of hard-coding
 
1.0.2 Added TMX custom alerts regex filters in ServiceNow.json
      Config option to send/ignore alerts to ServiceNow
      Config option to add additional-info
      Removed source/node/type from resource field to avoid dups

1.0.1 Fix to handle SEMP Alerts.
      Add all records from INDEX to additional_info section
      README updates
      Renamed SNowJsonConfig to SNowConfig

1.0   Initial Version

-----------------------------------------------------------------------------
Sep 10, 2019, Ramesh Natarajan (nram), Solace PSG
