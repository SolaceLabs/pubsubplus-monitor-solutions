       _________ __          __         __________
      /   _____//  |______ _/  |_  _____\______   \__ __  _____ ______
      \_____  \\   __\__  \\   __\/  ___/|     ___/  |  \/     \\____ \
      /        \|  |  / __ \|  |  \___ \ |    |   |  |  /  Y Y  \  |_> >
     /_______  /|__| (____  /__| /____  >|____|   |____/|__|_|  /   __/
             \/           \/          \/                      \/|__|
                                     Solace SEMP Data Republishing Tool
                                     Solace Professional Services Group

Solace StatsPump README
====================

## Overview

The StatsPump Utility provides a way for client applications using Solace 
messaging technology to asynchronously receive state and statistical 
information over the message-bus from their connected Solace router without
having to poll the Solace router using SEMP (Solace Element Management 
Protocol), and without having to parse the resulting XML.

StatsPump is a software application. It is not part of the official Solace
product line, is provided as-is and is not supported by Solace unless such
support is provided for under an agreement signed between you and Solace.

A detailed description of the installation, configuration and operation of
the StatsPump can be found in the StatsPump RunBook document. A quick start
guide is provided below in this README.

## Installation

Simply extract the solace-statspump-bin-<version>.tar or zip package:

    tar zxf solace-statspump-bin-<version>.tar
    
or

    unzip solace-statspump-bin-<version>.zip

### Systemd Linux service script

A simple service script file is provide to allow the statspump to be started
and stopped using systemd service. This script file can be installed Unix and
Linux systems that use systemd to handle starting of tasks and services during
boot, and stopping them during shutdown and supervising them while the system
is running.

The systemd script file for statspump is available in the scripts/ folder.

To install the script file, run the following:

    sudo cp scripts/statspump.service /lib/systemd/system/
    sudo systemctl enable statspump.service 
    
To run:

    sudo systemctl start statspump
    
To check the status of the process:

    sudo systemctl status statspump

> NOTE: when using the systemd script to start/stop the statspump, then
the package should be placed under /opt, and the configuration file should
be placed in the config/ directory of the extracted package and called:
appliances.xml. If named differently or placed in a different location,
then update the systemd script file appropriately.

## Usage

To run the Solace StatsPump execute from the root folder of the extracted
package:

    ./bin/statspump <POLLER_CONFIG> <POLLER_GROUPS> <APPLIANCE_CONFIG>

OR for Windows based platforms:

    ./bin/statspump.bat <POLLER_CONFIG> <POLLER_GROUPS> <APPLIANCE_CONFIG>
    
Where,

    <POLLER_CONFIG> is, the xml file containing the poller configuration
    for the StatsPump. A default pollers_default.xml file is provided in the
    config/ folder which should be used as a starting point
    
    <POLLER_GROUPS> is, the xml file containing the poller groups configuration
    for the StatsPump. A default groups_default.xml file is provided in the
    config/ folder which should be used as a starting point.
    
    <APPLIANCE_CONFIG> is, the xml file containing the appliances to monitor. 
    Some sample appliance_*.xml files are provided in the config/ folder which
    can be copies and modified appropriately to monitor your Solace appliances.
    
> NOTE: You can alternatively use the systemd service to start/stop StatsPump.
refer to the "Systemd Linux service script" section for more details. 

## Configuration

StatsPump requires three different configuration files in XML format, as listed
out in the Usage section of this README. For a more detailed explanation of
each of these configuration files refer to the StatsPump RunBook document. 

## Logging support

The StatsPump uses Apache SLF4J logging framework as a simple facade or
abstraction for various logging frameworks. By default the Apache Log4J2 
framework is used as the implementation to output logs to a file or on the
console. All logs by default are generated in the logs/ directory in a file
called: statspump.log

The configuration for Log4J is provided in the config/ directory. Edit the
log4j2.properties file to make any changes to the logging levels or output
file name & location. 

>NOTE: If you are using the systemd service script provided, update the
log4j2.properties file to change the log file path to be under /var, such as:

property.filename = /var/log/statspump/statspump

Refer to the [Log4J website](https://logging.apache.org/log4j/2.x/manual/configuration.html)
for details on how to modify and update the logging configuration file.

## License

Copyright 2016-2017 Solace Corporation. All rights reserved.

http://www.solace.com

This source is distributed under the terms and conditions of any contract or
contracts between Solace Corporation ("Solace") and you or your company. If
there are no contracts in place use of this source is not authorized. No
support is provided and no distribution, sharing with others or re-use of 
this source is authorized unless specifically stated in the contracts 
referred to above.

This software is custom built to specifications provided by you, and is 
provided under a paid service engagement or statement of work signed between
you and Solace. This product is provided as is and is not supported by 
Solace unless such support is provided for under an agreement signed between
you and Solace.
