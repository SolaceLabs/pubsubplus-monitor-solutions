#!/bin/bash
cd logstash-1.4.2;      ./stop.sh
cd ..
cd elasticsearch-1.4.0; ./stop.sh
cd ..

pkill httpd
