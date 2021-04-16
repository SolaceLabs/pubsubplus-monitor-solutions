#!/bin/bash

nohup bin/logstash -f config/syslog-grok.config > out.log 2>&1&

echo $! > logstash.pid


