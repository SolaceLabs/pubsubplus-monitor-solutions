# solace-datadog-agent, a Datadog plugin for Solace Message Brokers

## Overview

Install the solace-datadog-agent into an existing datadog agent (either standalone or in a docker container) and configure the credentials to access the brokers. After doing that, you can see Solace metrics in your datadog instance and can arrange dashboards

## Features

FIXME: List of metrics. 
For now the list of metrics monitored can be found in the last third of solace.py code.

## Installation

### Standalone datadog agent
- copy solace.py into /etc/datadog-agent/checks.d/
- copy solace.yaml into /etc/datadog-agent/conf.d/

### Docker Container
We have prepared a Dockerfile to run docker build.
Ensure that in solace.yaml, the tag use_env is set to "yes".

```
docker build -t dd-solace-agent:0.7.0 .
docker save dd-solace-agent:0.7.0 | gzip > dd-solace-agent_0.7.0.tar.gz

docker run -d -name dd-solace-agent \
  -e DD_API_KEY=<your DD-API-KEY>   \\
  -e DD_SITE=<your DD_SITE>         \
  -e DD_LOGS_ENABLED=true           \
  -e DD_LOGS_CONFIG_CONTAINER_COLLECT_ALL=true  \
  -v /var/run/docker.sock:/var/run/docker.sock:ro \
  -v /proc/:/host/proc/:ro          \
  -v /sys/fs/cgroup/:/host/sys/fs/cgroup:ro  \
  -e SOLACE_host=<your broker host> \
  -e SOLACE_port=<SEMP PORT TL>     \
  -e SOLACE_username=monitor        \
  -e SOLACE_password=xxx            \
  dd-solace-agent:0.7.0

```

## Configuration
You can either use a YAML file for config (preferred way for configure in a standalone environment) or you can use environment variables (preferred way for configuration in a docker environment, because you can set these variables during the docker run command)

### YAML file
```yaml
init_config:

instances:
  - min_collection_interval: 60
    host: xxx.messaging.solace.com
    port: 1943
    username: monitor
    password: monitor123
    use_env: no
```

### ENV variables:
If you want to use ENV variables for configuration, you should use a shortened YAML file:
```yaml
init_config:

instances:
  - min_collection_interval: 60
    use_env: no
```

and set environment variables 
- SOLACE_host
- SOLACE_port
- SOLACE_username
- SOLACE_password

## How to debug the solace datadog agent ?
- Inside the docker container: `agent check solace`

## Open Issues
- Auto-generate "__version__" in solace.py/ somehow (and sync it to code examples in this README.md file)
- make prefix in solace.py configurable
- review list of checks. Add missing
- group checks
- make checks (or groups of checks) switch on/off configurable
- Can multiple instances be monitored withe one solace-datadog-agent ?
- add config tag for Solace cloud instances (because of less permissions)
