import requests
import os
from xml.etree import ElementTree
from ddtrace import tracer

tracer.configure(
    hostname='datadog-agent',
    port=8126,
)

# Try-Block copied from: https://docs.datadoghq.com/developers/write_agent_check/?tab=agentv6v7
try:
    from datadog_checks.base import AgentCheck
except ImportError:
    from checks import AgentCheck

__version__ = "0.7.0"

def is_number(s):
    try:
        float(s)
        return True
    except ValueError:
        return False

def tag(key, value):
    return '{}:{}'.format(key, value)

XML_START = '<rpc><show>'
XML_END = '</show></rpc>'

# What prefix for all metrices in datadog ?
# Don't forget trailing '.'
PREFIX='solace.'

class SolaceCheck(AgentCheck):
    NAME_TAGS = ['msg-vpn-name', 'vpn-name', 'name', 'os-physical-interface', 'pool-id', 'type']
    TO_COLLECT = [
        # System health
        PREFIX + 'health.disk-latency-current-value',
        PREFIX + 'health.compute-latency-current-value',
        PREFIX + 'health.network-latency-current-value',
        PREFIX + 'health.mate-link-latency-current-value',
        
        # System memory (physical & subscription memory NOT WORKING)
        PREFIX + 'physical-memory-usage-percent',
        PREFIX + 'subscription-memory-usage-percent',
        PREFIX + 'slot-infos.slot-info.nab-buffer-load-factor',
        
        # Stats client detail
        PREFIX + 'client.global.stats.total-clients',
        PREFIX + 'client.global.stats.total-clients-connected',
        PREFIX + 'client.global.stats.total-clients-connected-with-compression',
        PREFIX + 'client.global.stats.total-clients-connected-with-ssl',
        PREFIX + 'client.global.stats.total-clients-connected-service-smf',
        PREFIX + 'client.global.stats.total-clients-connected-service-web',
        PREFIX + 'client.global.stats.total-clients-connected-service-rest',
        PREFIX + 'client.global.stats.total-clients-connected-service-mqtt',
        PREFIX + 'client.global.stats.current-login-req-received-rate-per-second',
        PREFIX + 'client.global.stats.current-login-rsp-sent-rate-per-second',
        PREFIX + 'client.global.stats.current-ingress-rate-per-second',
        PREFIX + 'client.global.stats.current-egress-rate-per-second',
        PREFIX + 'client.global.stats.current-ingress-byte-rate-per-second',
        PREFIX + 'client.global.stats.current-egress-byte-rate-per-second',
        PREFIX + 'client.global.stats.current-ingress-compressed-rate-per-second',
        PREFIX + 'client.global.stats.current-egress-compressed-rate-per-second',
        PREFIX + 'client.global.stats.current-ingress-uncompressed-rate-per-second',
        PREFIX + 'client.global.stats.current-egress-uncompressed-rate-per-second',
        PREFIX + 'client.global.stats.current-ingress-ssl-rate-per-second',
        PREFIX + 'client.global.stats.current-egress-ssl-rate-per-second',
        PREFIX + 'client.global.stats.ingress-discards.total-ingress-discards',
        PREFIX + 'client.global.stats.egress-discards.total-egress-discards',
        
        # Message spool detail
        PREFIX + 'message-spool-info.message-count-utilization-percentage',
        PREFIX + 'message-spool-info.transaction-resource-utilization-percentage',
        PREFIX + 'message-spool-info.transacted-session-resource-utilization-percentage',
        PREFIX + 'message-spool-info.transacted-seession-count-utilization-percentage',
        PREFIX + 'message-spool-info.delivered-unacked-msgs-utilization-percentage',
        PREFIX + 'message-spool-info.spool-files-utilization-percentage',
        PREFIX + 'message-spool-info.active-disk-partition-usage',
        PREFIX + 'message-spool-info.standby-disk-partition-usage',
        PREFIX + 'message-spool-info.transacted-sessions-used',
        PREFIX + 'message-spool-info.transacted-sessions-local-used',
        PREFIX + 'message-spool-info.transacted-sessions-xa-used',
        PREFIX + 'message-spool-info.transactions-used',
        PREFIX + 'message-spool-info.max-transactions',
        PREFIX + 'message-spool-info.transactions-local-used',
        PREFIX + 'message-spool-info.transactions-xa-used',
        PREFIX + 'message-spool-info.queue-topic-subscriptions-used',
        PREFIX + 'message-spool-info.ingress-flow-count',
        PREFIX + 'message-spool-info.egress-flow-count',
        PREFIX + 'message-spool-info.active-flow-count',
        PREFIX + 'message-spool-info.inactive-flow-count',
        PREFIX + 'message-spool-info.browser-flow-count',
        PREFIX + 'message-spool-info.current-persist-usage',
        PREFIX + 'message-spool-info.total-messages-currently-spooled',
        PREFIX + 'message-spool-info.current-cache-usage',
        
        # Message pool rates
        PREFIX + 'message-spool-rates.message-vpn-spool-rates.qendpt-data-rates.current-ingress-byte-rate-per-second',
        PREFIX + 'message-spool-rates.message-vpn-spool-rates.qendpt-data-rates.current-ingress-rate-per-second',
        PREFIX + 'message-spool-rates.message-vpn-spool-rates.qendpt-data-rates.current-egress-byte-rate-per-second',
        PREFIX + 'message-spool-rates.message-vpn-spool-rates.qendpt-data-rates.current-egress-rate-per-second',
        PREFIX + 'message-spool-rates.total-message-vpn-spool-rates.qendpt-data-rates.current-ingress-byte-rate-per-second',
        PREFIX + 'message-spool-rates.total-message-vpn-spool-rates.qendpt-data-rates.current-ingress-rate-per-second',
        PREFIX + 'message-spool-rates.total-message-vpn-spool-rates.qendpt-data-rates.current-egress-byte-rate-per-second',
        PREFIX + 'message-spool-rates.total-message-vpn-spool-rates.qendpt-data-rates.current-egress-rate-per-second',
        
        # VPN stats detail
        PREFIX + 'vpn.total-unique-subscriptions',
        PREFIX + 'vpn.connections',
        PREFIX + 'vpn.connections-service-smf',
        PREFIX + 'vpn.connections-service-web',
        PREFIX + 'vpn.connections-service-rest-incoming',
        PREFIX + 'vpn.connections-service-mqtt',
        PREFIX + 'vpn.connections-service-rest-outgoing',
        PREFIX + 'vpn.stats.current-login-req-received-rate-per-second',
        PREFIX + 'vpn.stats.current-login-rsp-sent-rate-per-second',
        PREFIX + 'vpn.stats.current-ingress-rate-per-second',
        PREFIX + 'vpn.stats.current-egress-rate-per-second',
        PREFIX + 'vpn.stats.current-ingress-byte-rate-per-second',
        PREFIX + 'vpn.stats.current-egress-byte-rate-per-second',
        PREFIX + 'vpn.stats.current-ingress-compressed-rate-per-second',
        PREFIX + 'vpn.stats.current-egress-compressed-rate-per-second',
        PREFIX + 'vpn.stats.current-ingress-uncompressed-rate-per-second',
        PREFIX + 'vpn.stats.current-egress-uncompressed-rate-per-second',
        PREFIX + 'vpn.stats.current-ingress-ssl-rate-per-second',
        PREFIX + 'vpn.stats.current-egress-ssl-rate-per-second',
        PREFIX + 'vpn.stats.ingress-discards.total-ingress-discards',
        PREFIX + 'vpn.stats.egress-discards.total-egress-discards',

        # VPN Message spool detail
        PREFIX + 'message-vpn.vpn.current-queues-and-topic-endpoints',
        PREFIX + 'message-vpn.vpn.num-queues',
        PREFIX + 'message-vpn.vpn.num-topic-endpoints',
        PREFIX + 'message-vpn.vpn.num-sequenced-topics',
        PREFIX + 'message-vpn.vpn.current-messages-spooled',
        PREFIX + 'message-vpn.vpn.current-spool-usage-mb',
        PREFIX + 'message-vpn.vpn.current-transacted-sessions',
        PREFIX + 'message-vpn.vpn.transacted-sessions-local-used',
        PREFIX + 'message-vpn.vpn.transacted-sessions-xa-used',
        PREFIX + 'message-vpn.vpn.current-transactions',
        PREFIX + 'message-vpn.vpn.transactions-local-used',
        PREFIX + 'message-vpn.vpn.transactions-xa-used',
        PREFIX + 'message-vpn.vpn.current-egress-flows',
        PREFIX + 'message-vpn.vpn.current-ingress-flows',

        # Bridge
        PREFIX + 'bridges.bridge.inbound-operational-state',
        PREFIX + 'bridges.bridge.outbound-operational-state',
        PREFIX + 'bridges.bridge.queue-operational-state',

        # Bridge stats
        PREFIX + 'bridges.bridge.client.stats.current-ingress-rate-per-second',
        PREFIX + 'bridges.bridge.client.stats.current-egress-rate-per-second',
        PREFIX + 'bridges.bridge.client.stats.current-ingress-byte-rate-per-second',
        PREFIX + 'bridges.bridge.client.stats.current-egress-byte-rate-per-second',
        PREFIX + 'bridges.bridge.client.stats.ingress-discards.total-ingress-discards',
        PREFIX + 'bridges.bridge.client.stats.egress-discards.total-egress-discards',

        # Queue details
        PREFIX + 'queues.queue.info.topic-subscription-count',
        PREFIX + 'queues.queue.info.num-messages-spooled',
        PREFIX + 'queues.queue.info.current-spool-usage-in-mb',

        # Queue rates
        PREFIX + 'queues.queue.rates.qendpt-data-rates.current-ingress-byte-rate-per-second',
        PREFIX + 'queues.queue.rates.qendpt-data-rates.current-ingress-rate-per-second',
        PREFIX + 'queues.queue.rates.qendpt-data-rates.current-egress-byte-rate-per-second',
        PREFIX + 'queues.queue.rates.qendpt-data-rates.current-egress-rate-per-second'
    ]

    def warning(self, text):
        print('WARNING: {}'.format(text))

    def debug(self, text):
        text
        
    def read_credentials(self, instance,tag):
        # credentials can be configured in the instance-yaml file or as ENV variables.
        # For docker containers, the ENV options is easier to configure, because you can set ENV variables in the docker run script.
        if not instance.get('use_env'):
            return instance.get(tag)
        else:
            # env is not mandatory - so we need to set defaults
            if 'SOLACE_'+tag not in os.environ:
                os.environ['SOLACE_' + tag] = ''
            return os.environ['SOLACE_' + tag]
        
    def publish_metric(self, parent_tag, element, tags=None):
        # Ignore names and thresholds
        if element.tag not in self.NAME_TAGS and\
           'thresholds' not in element.tag and\
           'clear' not in element.tag and\
           element.text:
            value = element.text
            metric_type = 'GAUGE'
            if not is_number(value):
                # Boolean-like values are converted to 1 and 0 and inferred to be gauges
                metric_type = 'GAUGE'
                if value in ['Yes', 'true', 'True', 'Enabled', 'Up', 'Primary']:
                    value = 1
                elif value in ['No', 'false', 'False', 'Disabled', 'Down', 'Backup']:
                    value = 0

            # Only consider numbers
            if is_number(value):
                metric_name = PREFIX + '{}.{}'.format(parent_tag, element.tag)

                if metric_name in self.TO_COLLECT:
                    if 'percent' in element.tag:
                        metric_type = 'GAUGE'

                    if metric_type == 'COUNT':
                        self.count(metric_name, value, tags)
                    elif metric_type == 'GAUGE':
                        self.gauge(metric_name, value, tags)

    def process_children(self, parent_tag, element, tags=None):
        if tags is None:
            tags = []

        for child in element:
            children = list(child)

            if len(children) > 0:
                # Start looking for a tag that looks like a name
                for name_tag in self.NAME_TAGS:
                    new_tags = []
                    name = child.find(name_tag)

                    if name is not None and name.text:
                        # Avoid mixing message-vpn-name and vpn-name
                        if 'vpn' in name_tag:
                            new_tags.append(tag('message-vpn-name', name.text))
                            # name and type are used too generically so qualify it
                        elif name_tag == 'name' or name_tag == 'type':
                            new_tags.append(tag('{}-{}'.format(child.tag, name_tag), name.text))
                        else:
                            new_tags.append(tag(name_tag, name.text))
                        break

                self.process_children('{}{}'.format('{}.'.format(parent_tag) if parent_tag else '', child.tag), children, tags + new_tags)
            else:
                self.publish_metric(parent_tag, child, tags)

    def process_semp_metrics_response(self, element, tags=None):
        if element is None:
            return
        
        print('================== {} =================='.format(element.tag))
        self.process_children('', element,tags)

    def post_xml(self, check, body):
        return requests.post(self.url, body, auth=self.auth, verify=False)

    def semp_request(self, check, objects, values=None):
        if values is None:
            values = []

        request_body = XML_START

        for o in objects:
            request_body += '<{}>'.format(o)

        for o in values:
            if isinstance(o, dict):
                for key in o:
                    request_body += '<{}>{}</{}>'.format(key, o[key], key)
            else:
                request_body += '<{}></{}>'.format(o, o)

        for o in reversed(objects):
            request_body += '</{}>'.format(o)

        request_body += XML_END

        response=self.post_xml(check, request_body)
        self.debug("Response: {}".format(response.content))

        rpcelement=ElementTree.fromstring(response.content).find('rpc')
        self.debug("rpcelement: {}".format(rpcelement))

        if rpcelement is None:
            self.warning("No rpc element in response found. Response was: {}".format(response.content))
            return

        showelement=rpcelement.find('show')
        self.debug("showelement: {}".format(showelement))
        if showelement is None:
            self.warning("No show element in response found. Response was: {}".format(response.content))
            return
            
        objectelement=showelement.find(objects[0])
        self.debug("objectelement: {}".format(objectelement))
        if  objectelement is None:
            self.warning("No element with '{}' in response found. Response was: {}".format(objects[0],response.content))
            return
        
        return objectelement

    def check(self, instance):
        # get credentials... either from config file or ENV variables
        host = self.read_credentials(instance,'host')
        port = self.read_credentials(instance,'port')
        username = self.read_credentials(instance,'username')
        password = self.read_credentials(instance,'password')

        self.url = 'https://{}:{}/SEMP'.format(host,port)
        self.auth = requests.auth.HTTPBasicAuth(username, password)


        # Add an ENV tag, if configured
        tags = []
        env = self.read_credentials(instance, 'env')
        if env:
            tags.append(tag('ENV', env))

        self.process_semp_metrics_response(self.semp_request(self, ['stats', 'client', 'detail']), tags)
        self.process_semp_metrics_response(self.semp_request(self, ['message-spool', 'detail']), tags)
        self.process_semp_metrics_response(self.semp_request(self, ['message-spool', 'stats']), tags)
        self.process_semp_metrics_response(self.semp_request(self, ['message-spool', 'rates']), tags)
        self.process_semp_metrics_response(self.semp_request(self, ['system', 'health']), tags)
        self.process_semp_metrics_response(self.semp_request(self, ['memory']), tags)
        # #        self.process_semp_metrics_response(client.semp_request(['alarm']), tags)
        self.process_semp_metrics_response(self.semp_request(self, ['service']), tags)
        self.process_semp_metrics_response(self.semp_request(self, ['interface']), tags)
        # #        self.process_semp_metrics_response(client.semp_request(['config-sync'], ['database', 'count', {'num-elements': 5}]), tags)
        self.process_semp_metrics_response(self.semp_request(self, ['redundancy']), tags)

        self.process_semp_metrics_response(self.semp_request(self, ['message-vpn'], [{'vpn-name': '*'}, 'stats', 'detail']), tags)
        self.process_semp_metrics_response(self.semp_request(self, ['message-vpn'], [{'vpn-name': '*'}, 'replication', 'detail']), tags)
        self.process_semp_metrics_response(self.semp_request(self, ['message-spool'], [{'vpn-name': '*'}, 'detail']), tags)

        self.process_semp_metrics_response(self.semp_request(self, ['bridge'], [{'bridge-name-pattern': '*'}, {'vpn-name-pattern': '*'}]), tags)
        self.process_semp_metrics_response(self.semp_request(self, ['bridge'], [{'bridge-name-pattern': '*'}, {'vpn-name-pattern': '*'}, 'stats']), tags)

        #self.process_semp_metrics_response(self.semp_request(self, ['queue'], [{'name': '*'}, {'vpn-name': '*'}, 'detail']), tags)
        #self.process_semp_metrics_response(self.semp_request(self, ['queue'], [{'name': '*'}, {'vpn-name': '*'}, 'rates']), tags)
        list = [ '*'  ]
        for item in list:
            self.process_semp_metrics_response(self.semp_request(self, ['queue'], [{'name': item}, {'vpn-name': '*'}, 'detail']), tags)
            self.process_semp_metrics_response(self.semp_request(self, ['queue'], [{'name': item}, {'vpn-name': '*'}, 'rates']), tags)
        
            #Self.process_semp_metrics_response(self.semp_request(self, ['topic-endpoint'], [{'name': '*'}, {'vpn-name': '*'}, 'detail']), tags)
            #self.process_semp_metrics_response(self.semp_request(self, ['topic-endpoint'], [{'name': '*'}, {'vpn-name': '*'}, 'rates']), tags)
            #self.process_semp_metrics_response(self.semp_request(self, ['client'], [{'name': '*'}, {'vpn-name': '*'}, 'connections', 'wide', 'slow-subscriber']), tags)
