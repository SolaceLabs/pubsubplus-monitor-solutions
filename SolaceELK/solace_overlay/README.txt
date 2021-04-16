Overlay files for current versions of ElasticSearch, Logstash and Kibana. Most of these are just convenience start/stop scripts, only the following files are really mandatory (and their differences against the basic distro):

Solace-specific files added to logstash-1.4.2:
logstash-1.4.2/patterns/solace-syslog
logstash-1.4.2/config/syslog-grok.config


elasticsearch-1.4.0/config/elasticsearch.yml
385a386,389
>
> http.cors.allow-origin: "/.*/"
> http.cors.enabled: true


kibana-3.1.2/config.js
32c32
<     elasticsearch: "http://"+window.location.hostname+":9200",
---
>     elasticsearch: "http://<HOSTNAME-OR-IP>:9200",

