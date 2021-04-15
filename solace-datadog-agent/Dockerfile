# Build solace-datadog-agent container
# Command to build:
# docker build -t dd-solace-agent:0.7.0 .

FROM datadog/agent:latest
ADD  solace.yaml /etc/datadog-agent/conf.d/solace.yaml
ADD  solace.py   /etc/datadog-agent/checks.d/solace.py
