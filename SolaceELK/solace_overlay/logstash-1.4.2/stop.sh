#!/bin/bash

pid=`cat logstash.pid`

kill $pid

rm -f logstash.pid
