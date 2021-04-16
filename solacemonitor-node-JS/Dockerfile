FROM node:10.15.3-stretch

RUN apt-get update
RUN apt-get -y install jq
RUN apt-get -y install libxml2-utils
RUN apt-get -y install sysstat
RUN apt-get -y install expect
RUN apt-get -y install net-tools
RUN apt-get -y install vim

# download all the packages
COPY package*.json /tmp/
RUN cd /tmp && npm ci --only=production
RUN mkdir -p /app && cp -a /tmp/node_modules /app/

VOLUME ["/app/config", "/app/processing", "/app/log", "/app/store"]

WORKDIR /app
COPY . .

CMD [ "./run.sh" ]
