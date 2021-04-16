------------------------------------------------------
| Solace Systems                                     |
| CS - SolGeneos Monitors                            |
| Release Notes                                      |
| February 2016                                      |
------------------------------------------------------

(c) Copyright 2015 Solace Systems, Inc.
All rights reserved.

This file contains the following information:

  * Release Limitations
  * Release Contents
  * Features Introduced
  * Supported Environments
  * Known Issues
  * Installation Instructions
  * Copyright & License
  * How to Contact Solace Systems


Release Limitations
-------------------
None

Release Contents
----------------
This Release (v1.0.1) contains the following SolGeneos Monitors:

    * Appliance Alarms
    * Appliance Config Sync
    * VPN ACLs
    * VPN Bridges
    * VPN Client Stats
    * VPN Message Spool
	* VPN Queues
    * VPN Queue Consumers
    * VPN Redundancy
    * VPN Replication
    * VPN Stats
	* VPN Subscriptions
	* VPN Topic Endpoints
	* VPN Topic Endpoint Consumers
    
The maintenance release provides a fix for stability issues found for some 
monitors in SCB environment.


Features Introduced
-------------------
The following features has been introduced by v1.0.1 from the previous release:

None

Supported Environments
----------------------
The SolGeneos Monitors v1.1.0 is compatible with:

    * Java 1.6 (Java 1.7 and above is recommended)
    * SolOS version 6.0 and 7.1
    * SolGeneos Agent v7.1.1.69


Resolved Issues
---------------
The following known issues are resolved by Version 1.0.1:

None.

Changed Functionality
---------------------
The following functionality changed in Version 1.0.1:

None

Known Issues
------------
The following are known issues in Version 1.0.1, and workarounds are provided
when possible:

None.

Installation Instructions
-------------------------
Installation of SolGeneos Agent is out-of-scope of this README as it is covered
in the Solace_SolGeneos_Guide_R<Version>_Iss01.pdf provided with the SolGeneos
package.

The steps to install the monitors provided in this release are outline below:

    1. Copy and extract the release binary package 
        (cs-solgeneos-monitors_v<Version>.tar.gz) to the SolGeneos Agent 
        server.
    
    2. Stop any running instance of SolGeneos Agent:
    
        service solgeneos stop
    
    3. Copy the monitor property files (ex. ApplianceAlarms.properties) to the
       SolGeneos config directory (ex. /usr/sw/solgeneos/config):
       
       cp cs-solgeneos-monitors_v<Version>/config/*.properties <solgeneos_intall_dir>/config/
       
    4. Copy the monitor jar files (ex. appliance-alarm-monitor.jar) to the
       SolGeneos monitors directory (ex. /usr/sw/solgeneos/monitors):
       
       cp scb-solgeneos-monitors_v<Version>/lib/*.jar <solgeneos_intall_dir>/monitors/
       
    5. Update the user sample configuration file
        <solgeneos_intall_dir>/config/_user_sample.properties:
    
        Update the following properties to the suggested values:
        
            # Each VPN has its own managed entity, the VPN name will be appended to the managedEntityPrefix
			managedEntityPrefix=APPLIANCE_NAME-
    
    6. Start Netprobe & SolGeneos Agent:
    
        service netprobe stop
        service netprobe start
        service solgeneos start
        
    You have completed the installation procedure.

Copyright & License
-------------------

Copyright 2015 Solace Systems, Inc. All rights reserved.

http://www.solacesystems.com

This source is distributed under the terms and conditions of any contract or 
contracts between Solace Systems, Inc. ("Solace") and you or your company. If
there are no contracts in place use of this source is not authorized. No 
support is provided and no distribution, sharing with others or re-use of this
source is authorized unless specifically stated in the contracts referred to
above.

This software is custom built to specifications provided by you, and is 
provided under a paid service engagement or statement of work signed between 
you and Solace. This product is provided as is and is not supported by Solace 
unless such support is provided for under an agreement signed between you and
Solace.


How to Contact Solace Systems
-----------------------------
Contact Solace Systems at:

Solace Systems, Inc.
535 Legget Drive
Third Floor
Ottawa, Ontario
Canada
K2K 3B8

Voice: +1 613 271 1010
Fax: +1 613 271 2844
E-mail: info@solacesystems.com
Web site: www.solacesystems.com

For customer support, contact:

North America Toll Free: +1 866 SOLACE1 (+1 866 765 2231)
International: +1 613 270 8404
E-mail: support_request@solacesystems.com
