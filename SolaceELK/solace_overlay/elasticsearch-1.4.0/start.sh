#!/bin/bash

nohup bin/elasticsearch > out.log 2>&1&

echo $! > elasticsearch.pid


