#!/usr/bin/python
# ServiceNowPost
#   Script to do receive Solace Monitor alert, do the parsing and
#   HTTP POST to serviceNow endpoint
#
# Ramesh Natarajan (Solace PSG)
# Sep 10, 2019

import sys
import os
import inspect
import json
import argparse
from datetime import datetime
import re

sys.path.append(os.getcwd() + "/lib")
import SNowLogger as slog
import SNowConfig as scfg
import SNowHttp as shttp

me = "ServiceNowPost"
myver = "v1.1"
SNOW_CFG_FILE = "config/ServiceNow.json"


# ------------------------------------------------------------------------------
# normalize_data
# Cleanup input data from Solace monitor
def normalize_data(argv):
    log.enter("%s::%s : %s", __name__, inspect.stack()[0][3], argv)

    d = {}
    log.trace('parse-args called : %s', argv)
    argv = [str.replace(x, '+', '') for x in argv]
    argv = [str.replace(x, '"', '') for x in argv]
    d['DOMAIN'] = argv[0]
    d['NAME'] = argv[1]
    d['INDEX'] = argv[2]
    d['ID'] = argv[3]
    d['SEVERITY'] = argv[4]
    d['TEXT'] = (' '.join(argv[5:]))
    return d


# ------------------------------------------------------------------------------
# get_post_data
# parse clean data from Solace monitor and create json record
def get_post_data(d):
    log.enter("%s::%s : %s", __name__, inspect.stack()[0][3], d)

    now = datetime.now()
    f = cfg.filter_str(d['NAME'], d['INDEX'])
    if not f:
        log.error("Failed to process filter %s in %s", d['NAME'], d['INDEX'])
        return None

    p = {}
    p['description'] = d['TEXT']
    p['source'] = cfg.source()

    # use connection-name (eg: nram.aws.ec2-13-56-151-95) as node name
    p['node'] = cfg.find('connection')
    # use router name (eg: ip-172-31-17-237) as node if available -
    # this will be confusing - use connection all thru
    # if cfg.find('router') != 'NA':
    #    p['node'] = cfg.find('router')

    # use solmmon alert-name (eg: SolEndpointPendingMsgsHigh)  as type
    p['type'] = d['NAME']
    # for CLI events, use event-name as type
    syslog_event = False
    if cfg.find('event') != 'NA':
        p['type'] = cfg.find('event')
        syslog_event = True
    if p['node'] == 'NA' or p['type'] == 'NA':
        log.error("Invalid Record. Failed to get node / type. node: %s type: %s", p['node'], p['type'])
        return None

    # remove redundant info from resource field
    remap = {
        str(p['source']): '',
        str(p['node']): '',
        str(p['type']): '',
        str(cfg.find('router')): ''
    }
    rex = re.compile("(%s)" % "|".join(map(re.escape, remap.keys())))
    r = rex.sub(lambda mo: remap.get(mo.group(1), mo.group(1)), d['INDEX'])
    r = re.sub(r'^[~_]*', '', r)
    r = re.sub(r'~+', ':', r)
    log.debug("resource (3) %s ", r)
    # shd we strip further?
    # warning - this may remove queue names with _, : or ending with numbers
    if syslog_event:
        r = re.sub(r'_+', ':', r)
        r = re.sub(r':+', ':', r)
        r = re.sub(r'\d+$', '', r)
        r = re.sub(r':$', '', r)
        log.debug("resource (syslog event) %s ", r)
    p['resource'] = r

    log.debug("INDEX: %s resource: %s", d['INDEX'], p['resource'])

    # Get severity from alert and map to servicenow levels
    # use 0 for clear alert (monitor will send original severity)
    p['severity'] = cfg.severity(d['SEVERITY'])
    if clear_alert:
        p['severity'] = 0
        log.info("Received CLEAR event %s with severity %s from %s", p['type'], p['severity'], p['node'])
    else:
        log.info("Received SET event %s with severity %s from %s", p['type'], p['severity'], p['node'])

    if p['node'] in cfg.alert_blacklist('nodes'):
        log.info("Node %s blacklisted in ServiceNow.cfg. Ignoring", p['node'])
        return None
    if p['type'] in cfg.alert_blacklist('events'):
        log.info("Event %s blacklisted in ServiceNow.cfg. Ignoring", p['type'])
        return None

    # add all the dict fields as additional_info to service now
    addl_info = dict(cfg.m_fmd)
    # addl_info['META-CreatedBy'] = '{} {}'.format(me, myver)
    # addl_info['META-TimeStamp'] = now.strftime("%d-%b-%Y (%H:%M:%S.%f)")
    addl_info['alert_name'] = d['NAME']
    addl_info['alert_id'] = d['ID']
    addl_info['alert_index'] = d['INDEX']

    if cfg.env('additional-info', 'NA'):
        p['additional_info'] = "\"{}\"".format(addl_info)
        log.debug("Adding Additional Info: %s", p['additional_info'])
    else:
        log.debug("Skip adding additional_info")

    r = {}
    r['records'] = []
    r['records'].append(p)
    return json.dumps(r)


# ------------------------------------------------------------------------------
# post_http
# http post the json dump to service_now endpoint
def post_http(d):
    log.enter("%s::%s : %s", __name__, inspect.stack()[0][3], d)
    servers = cfg.servers()
    log.debug("Got server list: (%d servets) %s", len(servers), servers)
    #n = 1
    for server in servers:
        if server['enabled']:
            log.info("POST to server : %s/%s", server['url'], server['endpoint'])
            http = shttp.SNowHttp(me,
                                  server['url'],
                                  server['username'],
                                  server['password'],
                                  server['endpoint'])
            http.post(d)
        else:
            log.info("Server %s/%s not enabled in ServiceNow.cfg. Skipping", server['url'], server['endpoint'])
    #n = n + 1


# ---------------------------------------------------------------
# main
# ---------------------------------------------------------------
def main(argv):
    global log
    global cfg
    global clear_alert

    # print 'Starting ...'
    p = argparse.ArgumentParser(prog=me,
                                description='ServiceNowPost: Post PubSub+ Monitor Alert to ServiceNow',
                                formatter_class=argparse.RawDescriptionHelpFormatter)
    pr = p.add_argument_group("Required")
    pr.add_argument('--set', action='store_true', default=False)
    pr.add_argument('--clear', action='store_true', default=False)
    pr.add_argument('--argv', action='store', nargs='+', required=True)

    po = p.add_argument_group("Optional")
    po.add_argument('--dir', action="store", default="./", help='ServiceNow scripts home (default: current dir)')
    po.add_argument('-v', '--verbose', action="count", help='Verbose mode')

    r = p.parse_args()

    if not (r.set or r.clear):
        print
        'Either --set or --clear option must be provided'
        sys.exit(0)
    clear_alert = False
    if r.clear:
        clear_alert = True

    # print 'Received args'
    # print r.argv

    # initialize logging
    log0 = slog.SNowLogger(me, r.verbose)
    log = log0.get_logger()

    if log is None:
        raise Exception("Logger not defined")
    log.debug("=== %s (%s) Starting", me, myver)
    log.debug("args %s", r)

    cfg_file = ("{}/{}".format(r.dir, SNOW_CFG_FILE))
    log.debug("Reading config file : %s", cfg_file)
    cfg = scfg.SNowConfig(me, cfg_file)
    # log0.verbose(cfg.env('verbose', False))
    log.debug("Config JSON: %s", cfg.m_cfg)
    if not cfg.send_alerts():
        log.info("Alert received on standby server. Nothing to do.")
        exit()

    # log.debug("source: %s", cfg.source())

    data = normalize_data(r.argv)
    log.debug("Normalized data: %s", data)

    json_data = get_post_data(data)
    if json_data:
        log.info("POST data: %s", json_data)
        post_http(json_data)
    else:
        log.debug("Nothing to post")


if __name__ == "__main__":
    main(sys.argv[1:])
