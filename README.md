# Solace Custom Crafted Monitoring Solutions

## IMPORTANT: This branch is for development works in progress. Unless specifically stated there is no expectation that code is executable or usable.

This repository is a collection of useful solution and solution templates that can be a starting point for custom monitoring needs. It is organized by technology stack.

Over time, Solace will streamline these offerings to include Solace's monitoring best practices and standardized documentation making it easy to navigate and find what you are looking for.

Are you creating a new solution that is not here? Please consider contributing your solution to help us centralize and share knowledge.

## Monitoring Solutions

Below is a list of our customer crafted monitoring solutions:

### Datadog
This project is a starting point for a custom Datadog implementation. It features the ability to control which statsitics are pulled from the brokers, the frequency of the pulls and which Datadog account to push to.

This is for the client who is already using Datadog for other enterprise systems, and wants everything in the same pane of glass.

### Splunk Metrics Collector
This project polls queue stats from Solace brokers and pushes those stats into Splunk.

### Syslog
This tool grabs syslog events off of the network pushed out by the Solace brokers and stuffs them into InfluxDb, Grafana or Chronograf.

### AppDynamics
This tool pulls Solace stats from brokers and pushes them into AppDynamics.

### ELK Syslogs
This sample project shows how to capture syslogs and visualize using ElasticSearch, Logstash and Kibana.

### ELK knowledge
We have a series of articles and talks that illustrates how to use Elk.

### Prometheus
This one is pull based. The 'Solace Prometheus Exporter' exposes https endpoints to Prometheus. Written in Go, it also includes grafana visualization.

### AWS Cloudwatch
This project captures syslog events and pushes them into AWS Cloud watch.

### ServiceNow
This is a custom plug-in for PS+ Monitor (aka RTview). It provides alerts.

### SolGeneos Custom Plugins
We have a variety of custom plug ins that have been made for clients over the years. This is a popular appraoch with our capital markets customers.

We have an both an older and newer iteration of Custom SolGeneos Monitors. In a future iteration of this repo, these will likley be merged into a single project.

## EnterpriseStats (StatsPump + recievers)
This java based solution consists of a core process (the Pump) that pulls stats from the broker and puts them on the message bus in a digestable, non-XML format. On the reciever side, there is a framework allowing devs to implement a single class with just a few methods for pushing data anywhere. Current reciever implementaions include pushing to InfluxDb where most users visualize with Grafana, and Elastic Search with Kibana.

It can also run in 'local mode" where the reciver and the Pump are in the same process, avoiding the republishing of stats onto the message bus.

## Other known implementations
- Apple has built a Splunk solution for both Metrics and Events, which they own.
- Voicebase has a sophisticated custom solution built in Elk.
Menards uses Zabbix.
