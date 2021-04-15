Solace Enterprise Stats Receiver Framework REAMDE
====================

This README contains instructions on how to install, configure, and use the
Solace stats-receiver client and framework to receive stats published by 
StatsPump and store them in a time-series database. 

Things you'll need:

* [Java JDK 1.7 or above](http://www.oracle.com/technetwork/java/javase/overview/index.html)

## Overview

The receiver framework, consists of a simple Solace client to receives and 
parses stat messages published by StatsPump, and and interface for a 
user-defined plugin to store the parsed metrics in a time-series database. 
The receiver framework uses a plugin based design to allow received metrics 
to be stored in any database, such as InfluxDB time-series database.

A sample built-in plugin is provided, for testing purposes, to output the 
received stat messages on the console.

> NOTE: In the current version of the receiver framework, only message types 
published by StatsPump using the SDTFactory class are parsed. Any other message
type, such as message containing JSON payloads, are ignored and discarded.

## Installation

Simply extract the solace-stats-receiver-bin-<version>.tar or zip package:

    tar zxf solace-stats-receiver-bin-<version>.tar
    
or

    unzip solace-stats-receiver-bin-<version>.zip

Next, copy the user-defined plugin that implements the StatTap interface to
the lib/ directory, and update the 'TAP_PLUGIN_CLASS' config property in the
config/stats-receiver.properties or the configuration file that will supplied
when executing the client.

### Systemd Linux service script

A simple service script file is provide to allow the stats-receiver to be
started and stopped using systemd service. This script file can be installed
Unix and Linux systems that use systemd to handle starting of tasks and 
services during boot, and stopping them during shutdown and supervising them
while the system is running.

The systemd script file for stats-receiver is available in the scripts/ folder.

To install the script file, run the following:

    sudo cp scripts/stats-receiver.service /lib/systemd/system/
    sudo systemctl enable stats-receiver.service 
    
To run:

    sudo systemctl start stats-receiver
    
To check the status of the process:

    sudo systemctl status stats-receiver

> NOTE: when using the systemd script to start/stop the stats-receiver, then
the package should be placed under /opt, and the configuration file should
be placed in the config/ directory of the extracted package and called:
appliances.xml. If named differently or placed in a different location,
then update the systemd script file appropriately

## Usage

To run the Solace stats-receiver execute from the root folder of the extracted
package:

    ./bin/stats-receiver --config <CONFIGURATIONFILE>

OR for Windows based platforms:

    ./bin/stats-receiver.bat --config <CONFIGURATIONFILE>
    
Where <CONFIGURATIONFILE> is, the property file containing the configuration
for the stats-receiver client and the provided plugin. A sample configuration
file called: 'sample.stats-receiver.properties' is provided that can be copied
and used.

If the user-defined plugin requires configuration properties to be read from a
file, then they should be added to the above property file supplied to the
stats-receiver at runtime.

> NOTE: when starting up the stats-receiver, it will wait for up to 2 minutes 
to receive the BROADCAST messages from the StatsPump to know what the topic 
formats look like, and what are the indexable attributes. Upon receiving this
broadcast message the receiver will start delivering messages to the configured
plugin.


## Configuration

A sample configuration file called: 'sample.stats-receiver.properties' is 
provided that can be copied and used. This configuration file contains 
Solace connection properties and other properties required by the 
stats-receiver client. 

If the user-defined plugin requires configuration properties to be read from a
file, then they should be added to the above property file supplied to the
stats-receiver at runtime.

> NOTE: All passwords in the configuration file should be encrypted using the
Solace PSG Password Utility. Refer to the README provided with the
solace-pwd-utility-bin-<version>.tar or .zip package for instructions on how to
encrypt your plain-text passwords. 

## Logging support

The stats-receiver uses Apache SLF4J logging framework as a simple facade or
abstraction for various logging frameworks. By default the Apache Log4J2 
framework is used as the implementation to output logs to a file or on the
console. All logs by default are generated in the logs/ directory in a file
called: stats-receiver.log

The configuration for Log4J is provided in the config/ directory. Edit the
log4j2.properties file to make any changes to the logging levels or output
file name & location.

>NOTE: If you are using the systemd service script provided, update the
log4j2.properties file to change the log file path to be under /var, such as:

property.filename = /var/log/stats-receiver/stats-receiver

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
