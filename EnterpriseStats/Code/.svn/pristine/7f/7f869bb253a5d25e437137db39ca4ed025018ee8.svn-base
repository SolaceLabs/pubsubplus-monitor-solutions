/**
 * Copyright 2016-2017 Solace Corporation. All rights reserved.
 *
 * http://www.solace.com
 *
 * This source is distributed under the terms and conditions of any contract or
 * contracts between Solace Corporation ("Solace") and you or your company. If
 * there are no contracts in place use of this source is not authorized. No
 * support is provided and no distribution, sharing with others or re-use of 
 * this source is authorized unless specifically stated in the contracts 
 * referred to above.
 *
 * This software is custom built to specifications provided by you, and is 
 * provided under a paid service engagement or statement of work signed between
 * you and Solace. This product is provided as is and is not supported by 
 * Solace unless such support is provided for under an agreement signed between
 * you and Solace.
 */
package com.solace.psg.enterprisestats.receiver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solace.psg.enterprisestats.receiver.stats.InternalStats;

/**
 * This base class satisfies the StatsTap interface and
 *
 */
public class AbstractStatsTap implements StatsTap {
    private static final Logger s_logger = LoggerFactory.getLogger(AbstractStatsTap.class);
    StatsReceiverProperties m_properties = null;
    InternalStats m_internalStats = new InternalStats();

    @Override
    public void initialize(StatsReceiverProperties properties) throws ReceiverException {
        this.m_properties = properties;
    }

    @Override
    public void cleanup() {
        // Nothing to do here for now.
        s_logger.trace("Nothing to cleanup in AbstractStatsTap base class.");
    }

    @Override
    public void onPeriodicStatusReport() {
        // Nothing to do here for now.
        s_logger.trace("No status reporting performed in AbstractStatsTap base class.");
    }

    @Override
    public InternalStats getInternalStats() {
        return m_internalStats;
    }

    @Override
    public void onRouterStats(StatsMessage message) throws ReceiverException {
        // Nothing to do here for now.
        s_logger.trace("No processing of received router stats message done AbstractStatsTap base class.");
    }

    @Override
    public void onPumpStats(StatsMessage message) throws ReceiverException {
        // Nothing to do here for now.
        s_logger.trace("No processing of received pump stats message done AbstractStatsTap base class.");
    }
	@Override
	public void onPollerStart(String host, String shortname, String longname) {
		s_logger.trace("poller start message ignored");
	}

	@Override
	public void onPollerEnd(String host, String shortname, String longname, boolean success) {
		s_logger.trace("poller stop message ignored");
	}
}
