#!/bin/bash

pid=`cat elasticsearch.pid`

kill $pid

rm -f elasticsearch.pid
