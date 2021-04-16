#!/usr/bin/python
# SnowLogger
#   Common logging functions used by Solace Tempale python scripts
#
# Ramesh Natarajan (Solace PSG)
#
# Jun 14, 2019
#   Initial Version
#

import sys, os
import logging


class SNowLogger:
    """Solace Logger wrapper implementation"""

    # ------------------trace
    # init
    #
    def __init__(self, appname, verbose=0):
        self.m_appname = appname
        self.m_logfile = "log/" + appname + ".log"
        self.m_verbose = verbose
        self.m_init = False
        self.setup_logger()

    # ------------------------------------------------------------------------------
    # setup logger
    # logger levels:
    # CRIT - 50
    # ERROR - 40
    # NOTE - 32 		(custom)
    # WARN - 30
    # STATUS - 22 	(custom)
    # INFO - 20
    # ENTER - 12		(custom)
    # DEBUG  - 10
    # TRACE - 8 		(custom)
    # NOTSET - 0
    def setup_logger(self):

        # add additional levels
        logging.TRACE = 8  # track status seperatlely
        logging.addLevelName(logging.TRACE, 'TRACE')

        logging.ENTER = 12  # track status seperatlely
        logging.addLevelName(logging.ENTER, 'ENTER')

        logging.STATUS = 22  # track status seperatlely
        logging.addLevelName(logging.STATUS, 'STATUS')

        logging.NOTE = 32  # positive yet important
        logging.addLevelName(logging.NOTE, 'NOTICE')

        logging.addLevelName(logging.CRITICAL, 'FATAL')  # rename existing

        self.m_logger = logging.getLogger(self.m_appname)
        self.m_logger.trace = lambda msg, *args: self.m_logger._log(logging.TRACE, msg, args)
        self.m_logger.note = lambda msg, *args: self.m_logger._log(logging.NOTE, msg, args)
        self.m_logger.status = lambda msg, *args: self.m_logger._log(logging.STATUS, msg, args)
        self.m_logger.enter = lambda msg, *args: self.m_logger._log(logging.ENTER, msg, args)
        self.m_logger.setLevel(logging.INFO)

        formatter = logging.Formatter('%(asctime)s : %(name)s [%(levelname)s] %(message)s')

        stream_formatter = logging.Formatter('[%(levelname)s] %(message)s')

        # stream_formatter = logging.Formatter('%(message)s')

        # file handler
        fh = logging.FileHandler(self.m_logfile)
        fh.setLevel(logging.INFO)
        if self.m_verbose > 2:
            print "** Setting file log level to TRACE ***"
            fh.setLevel(logging.TRACE)
            self.m_logger.setLevel(logging.TRACE)
        elif self.m_verbose > 0:
            print "** Setting file log level to DEBUG ***"
            fh.setLevel(logging.DEBUG)
            self.m_logger.setLevel(logging.DEBUG)
        fh.setFormatter(formatter)
        self.m_logger.addHandler(fh)

        # stream handler -- log at higher level
        ch = logging.StreamHandler()
        ch.setLevel(logging.NOTE)
        if self.m_verbose > 0:
            print "** Setting stream log level to INFO ***"
            ch.setLevel(logging.INFO)
            # self.m_logger.setLevel(logging.INFO)
        ch.setFormatter(stream_formatter)
        self.m_logger.addHandler(ch)

        self.m_init = True

    # ------------------------------------------------------------------------------
    # return logging.logger to apps
    #
    def get_logger(self, name=None):
        if not self.m_init:
            print "Logging not initialized"
            return None
        if name == None:
            # print "Returning saved logger"
            return self.m_logger
        # print "Returning logger for ", name
        return logging.getLogger(name)

    # TODO: Doesn't work - can't change FH loglevel mid stream
    def verbose(self, v=0):
        self.m_verbose = v
        if self.m_verbose > 0:
            print "Set verbose to {}".format(self.m_verbose)
        return self.m_verbose
