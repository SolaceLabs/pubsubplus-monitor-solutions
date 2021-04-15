Solace Enterprise Stats README
====================

This README contains developer instructions on how to develop, build, test, and
generate distribution archives for Enterprise Stats projects. 

Things you'll need:

* [Java JDK 1.7 or above](http://www.oracle.com/technetwork/java/javase/overview/index.html)
* [Eclipse IDE](http://www.eclipse.org/)

> **NOTE:** Gradle wrapper and scripts for both UNIX & Windows based platforms 
are included with the source, so you do not need a local installation of 
Gradle. It is recommended and a best practice to always use the Gradle
wrapper to build and assemble this project.

## Overview

The StatsPump Utility provides a way for client applications using Solace 
messaging technology to asynchronously receive state and statistical 
information over the message-bus from their connected Solace router without
having to poll the Solace router using SEMP (Solace Element Management 
Protocol), and without having to parse the resulting XML.

This StatsPump project consists of several subprojects. The following outlines
the structure and description of each project.

```
Root project 'StatsPump'
+--- Project ':pump' - StatsPump core application
+--- Project ':pwd-utility' - StatsPump utility to encrypt/decrypt passwords
+--- Project ':receiver-plugins'
|    \--- Project ':receiver-plugins:influxdb' - Influxdb plugin for stats-receiver
\--- Project ':stats-receiver' - StatsPump listener framework
```

Each subproject will contain it's own Gradle build script file. Those build
scripts file are named after the subproject name and use the '.gradle' 
extension. The root build.gradle file declares all dependencies for all 
projects as well as the inter-project dependencies. Always run Gradle tasks
at the root project level, rather than from the subprojects.

## Building
 
To build the complete project, run unit tests, and generate binary 
distribution archive files, run the below from the StatsPump root directory:

```
./gradlew clean build
```

To compile only Java code:

```
./gradlew compileJava
```

You can also compile individual projects as follows, which will automatically
compile any dependent projects:

```
./gradlew :<PROJECT_NAME>:compileJava
./gradlew :pump:compileJava
./gradlew :receiver-plugins:influxdb:compileJava
```

## Testing
 
To run unit tests for all projects:

```
./gradlew test
```

HTML test execution reports can be viewed from the project's respective build
directory. For ex., HTML test reports for the 'stats-receiver' project will be
under:

> stats-receiver/build/reports/tests/test/

To run unit tests for individual projects, prefix the project name in front of
the test Gradle task, for ex.:

```
./gradlew :stats-receiver:test
```

## Generating distribution packages

Both binary and source distribution packages can be generated. Both the build
task and the assemble task will generate both ZIP and Tar distribution 
packages.

```
./gradlew assemble
```

To generate distributions for individual projects, prefix the project name in
front of the test Gradle task, for ex.:

```
./gradlew :pump:assemble
```

To generate source distributions for the core pump project or individual stats
listener projects:

```
./gradlew dist<Classifier>Src
```
Where <Classifier> is ['Pump', 'Stats-Receiver', 'Influxdb'].

> **NOTE:** the source distribution will include and dependent projects as well
and generated in the "_dist" folder in the root directory of the project.


## How to develop new Receiver Plug-ins

> TODO

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
