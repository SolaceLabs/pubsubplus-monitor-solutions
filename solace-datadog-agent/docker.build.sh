#!/bin/bash

# Script to build the dd-solace-agent docker container

# Version / Tag:
# Inside the source code solace.py: It's this line:

## __version__ = "0.6.3"

TAG=$(grep __version__ solace.py | sed -e 's/.*= *"\(.*\)"/\1/')


docker build -t dd-solace-agent:$TAG .

docker save dd-solace-agent:$TAG > dd-solace-agent-$TAG.tar.gz
