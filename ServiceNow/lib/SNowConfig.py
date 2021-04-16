#!/usr/bin/python
# SNowJsonConfig
#   JSON Config file parsing
#
# Ramesh Natarajan (Solace PSG)
# Jun 14, 2019

import sys, os
import re
import logging, inspect
import pprint
import json
import time

# import common function
mypath = os.path.dirname(__file__)
sys.path.append(mypath + "/lib")


class SNowConfig:
    """JSON Parsing implementation"""

    # --------------------------------------------------------------
    # Constructor
    # --------------------------------------------------------------
    def __init__(self, me, cfgfile):
        self.m_me = me
        self.m_logger = logging.getLogger(me)
        self.m_fmd = {}
        log = self.m_logger
        # log = self.m_logger
        log.enter(" %s::%s  config file: %s", __name__, inspect.stack()[0][3], cfgfile)
        if (cfgfile == None):
            log.debug("Nothing to do here.")
            return

        # read config files
        try:
            # load app config
            with open(cfgfile, 'r') as f:
                self.m_cfg = json.load(f)
            log.trace("JSON Config: %s", self.m_cfg)
            f.close()
        except:
            log.exception("unexpected exception", sys.exc_info()[0])
            raise

    def find(self, s, s1=None):
        log = self.m_logger
        # log.enter("%s::%s : field : %s", __name__, inspect.stack()[0][3], s)
        if s1:
            if s1 in self.m_fmd:
                log.debug("found override field %s => %s", s1, self.m_fmd[s1])
                return self.m_fmd[s1]
        if s in self.m_fmd:
            log.debug("found field %s => %s", s, self.m_fmd[s])
            return self.m_fmd[s]
        else:
            log.debug("No match for field %s", s)
            return "NA"

    def severity(self, s):
        log = self.m_logger
        log.enter("%s::%s : severity %s", __name__, inspect.stack()[0][3], s)

        severity_map = self.m_cfg['severities']
        s1 = "1"
        if s in severity_map:
            s1 = severity_map[s]
            log.debug("Mapping monitor severity  %s to service_now severity %s", s, s1)
        else:
            log.error("No mapping exists for monitor severity %s. using default %s", s, s1)
        return s1

    def filter_str(self, s, d):
        log = self.m_logger
        log.enter("%s::%s : type: %s index: %s", __name__, inspect.stack()[0][3], s, d)

        filter_map = self.m_cfg['filters']
        if s in filter_map:
            s1 = filter_map[s]
            log.debug("Got filter for type %s: %s", s, s1)
        else:
            log.debug("No filter found for type %s", s)
            s1 = filter_map['default']
            log.info("Use default filter for type %s", s)
            log.debug("default filter %s", s1)
        b = re.match(s1, d)
        if b:
            log.debug("Got match for index: %s ", d)
            self.m_fmd = b.groupdict()
            log.debug("groupdict: %s", b.groupdict())
        else:
            log.error("No match for index: %s ", d)
        return self.m_fmd

    def source(self):
        log = self.m_logger
        log.enter("%s::%s", __name__, inspect.stack()[0][3])
        host = self.m_cfg['host']
        return host['source']

    def server(self):
        log = self.m_logger
        log.enter("%s::%s", __name__, inspect.stack()[0][3])
        return self.m_cfg['server'][0]
     
    # send array of servers to support multiple servicenow endpoints   
    def servers(self):
        log = self.m_logger
        log.enter("%s::%s", __name__, inspect.stack()[0][3])
        return self.m_cfg['servers']

    # support suppressing alerts by connection, etc
    def alert_blacklist(self, field):
        log = self.m_logger
        log.enter("%s::%s", __name__, inspect.stack()[0][3])
        return self.m_cfg['alert_blacklist'][field]

    def env(self, s, d):
        log = self.m_logger
        log.enter("%s::%s %s", __name__, inspect.stack()[0][3], s)
        env = self.m_cfg['host']
        if s in env:
            log.debug("Return match %s => %s", s, env[s])
            return env[s]
        else:
            log.debug("No match. Return default %s => %s", s, d)
            return d

    def send_alerts(self):
        log = self.m_logger
        log.enter("%s::%s ", __name__, inspect.stack()[0][3])
        env = self.m_cfg['host']
        if "send-alerts" in env:
            return env["send-alerts"]
        else:
            return False

