/*
 * Copyright 2014-2016 Solace Systems, Inc. All rights reserved.
 *
 * http://www.solacesystems.com
 *
 * This source is distributed under the terms and conditions
 * of any contract or contracts between Solace Systems, Inc.
 * ("Solace") and you or your company. If there are no 
 * contracts in place use of this source is not authorized.
 * No support is provided and no distribution, sharing with
 * others or re-use of this source is authorized unless 
 * specifically stated in the contracts referred to above.
 *
 * This product is provided as is and is not supported by Solace 
 * unless such support is provided for under an agreement 
 * signed between you and Solace.
 */

package com.solace.psg.enterprisestats.statspump;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solace.psg.enterprisestats.statspump.pollers.HostnamePoller;
import com.solace.psg.enterprisestats.statspump.pollers.MessageSpoolPoller;
import com.solace.psg.enterprisestats.statspump.pollers.RedundancyPoller;
import com.solace.psg.enterprisestats.statspump.util.StatsPumpConstants;

/**
 * The ReachableRunnable is the guy who tries to initially make contact with a
 * PhysicalAppliance. Once the 4 commands have successfully completed, the
 * appliance is deemed "reachable" and the regular SEMP polling can take effect.
 * 
 * @author alee
 *
 */
public class ReachableRunnable implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ReachableRunnable.class);

    private final PhysicalAppliance physical;
    private final PollerRunnable hostnameRunnable;
    private final PollerRunnable redundancyRunnable;
    private final PollerRunnable messageSpoolRunnable;

    public ReachableRunnable(PhysicalAppliance physical) {
        this.physical = physical;
        hostnameRunnable = new PollerRunnable(physical, HostnamePoller.getInstance(),
                StatsPumpConstants.HOSTNAME_POLLER_INTERVAL_SEC * 1000);
        redundancyRunnable = new PollerRunnable(physical, RedundancyPoller.getInstance(),
                StatsPumpConstants.REDUNDANCY_POLLER_INTERVAL_SEC * 1000);
        messageSpoolRunnable = new PollerRunnable(physical, MessageSpoolPoller.getInstance(),
                StatsPumpConstants.MSG_SPOOL_POLLER_INTERVAL_SEC * 1000);
    }

    @Override
    public void run() {
        if (physical.isReachable())
            return;
        if (!ScheduledPollerHandle.oneTimePoller(hostnameRunnable)) {
            logger.debug(String.format("Could not contact %s", physical));
            return;
        }
        if (!ScheduledPollerHandle.oneTimePoller(redundancyRunnable)) {
            logger.info(String.format("%s failed, however 'show hostname' just worked!?", redundancyRunnable));
            return;
        }
        if (!ScheduledPollerHandle.oneTimePoller(messageSpoolRunnable)) {
            logger.info(String.format("%s failed, however 'show hostname' just worked!?", messageSpoolRunnable));
            return;
        }
        physical.declareReachable(); // declare the appliance back up
    }

}
