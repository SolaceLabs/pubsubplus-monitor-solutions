#!/bin/bash
cd logstash-1.4.2;      ./start.sh
cd ..
cd elasticsearch-1.4.0; ./start.sh
cd ..

nohup httpd > httpd.log 2>&1&
