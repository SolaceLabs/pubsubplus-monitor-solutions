#!/usr/bin/python
# SNowHttp
#   Common HTTP functions used by Solace Tempale python scripts
#
# Ramesh Natarajan (Solace PSG)

from __future__ import absolute_import, division, print_function, unicode_literals
import sys
import os
import base64
import string
import re
# import xml.etree.ElementTree as ET
import logging
import inspect

if sys.version_info[0] == 3:
    import http.client as http
else:
    import httplib as http

sys.path.append(os.getcwd() + "/lib")


# import SNowXml as posxml


class SNowHttp:
    """SNow HTTP connection implementation"""

    # --------------------------------------------------------------
    # Constructor
    # --------------------------------------------------------------
    def __init__(self, me, host, user, passwd, url=""):
        self.m_logger = logging.getLogger(me)
        log = self.m_logger
        log.enter("%s::%s : %s/%s %s", __name__, inspect.stack()[0][3], host, url, user)

        self.m_me = me
        # strip http:// or https:// from url
        self.m_usehttps = False
        if host.find("https:") > -1:
            host = string.replace(host, "https://", "")
            self.m_usehttps = True
            log.debug('Stripping https:// from url')  # httplib assumes url:port format
        if host.find('http:') > -1:
            host = string.replace(host, 'http://', '')
            log.debug('Stripping http:// from url')  # httplib assumes url:port format
        log.debug('host = %s', host)
        self.m_host = host
        self.m_user = user
        self.m_passwd = passwd
        self.m_url = url
        self.open_http_connection()

    # -------------------------------------------------------
    # Connection related functions
    #
    def open_http_connection(self):
        log = self.m_logger
        log.enter("%s::%s :", __name__, inspect.stack()[0][3])
        if sys.version_info[0] == 3:
            base64_str = base64.encodestring(('%s:%s' % (self.m_user, self.m_passwd)).encode()).decode().replace('\n',
                                                                                                                 '')
            auth = base64_str.strip()
        else:
            auth = string.strip(base64.encodestring(self.m_user + ":" + self.m_passwd))

        log.debug("auth: %s", auth)
        self.m_hdrs = {"Content-type": "application/json",
                       "Accept": "application/json"}
        self.m_hdrs["Authorization"] = "Basic %s" % auth
        log.debug("Headers: %s", list(self.m_hdrs.items()))
        try:
            if self.m_usehttps:
                log.debug("HTTPS connection to %s", self.m_host)
                self.m_conn = http.HTTPSConnection(self.m_host)
            else:
                log.debug("HTTP connection to %s", self.m_host)
                self.m_conn = http.HTTPConnection(self.m_host)

        except http.InvalidURL as e:
            log.exception(e)
            raise
        except:
            log.exception("Unexpected exception: %s", sys.exc_info()[0])
            raise
        log.debug("%s::%s : HTTP/S Connection open", __name__, inspect.stack()[0][3])
        return self.m_conn

    # -------------------------------------------------------
    # Post a req
    def post(self, req):
        log = self.m_logger
        log.enter("%s::%s :", __name__, inspect.stack()[0][3])
        log.debug("headers: %s", self.m_hdrs)
        log.debug("request: %s", req)

        log.debug("HTTP POST to URL/endpoint : %s/%s", self.m_host, self.m_url)
        self.m_conn.request("POST", self.m_url, req, self.m_hdrs)
        self.m_res = self.m_conn.getresponse()
        log.info("HTTP Response Status: %s Reason: %s", self.m_res.status, self.m_res.reason)
        if not self.m_res:
            raise Exception("No response")
        self.m_resp = self.m_res.read().decode(encoding="utf-8")
        log.debug("response data: %s", self.m_resp)
        if self.m_resp is None:
            raise Exception("Null response")
            return None
        return self.m_resp
