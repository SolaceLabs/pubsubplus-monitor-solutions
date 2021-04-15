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

import com.solace.psg.enterprisestats.receiver.stats.InternalStats;

/**
 * This interface is contract between the Solace PSG Enterprise stats receiver
 * framework, and the individual implementations. Implement this interface to
 * receive stats messages from the Pump.
 * 
 * Note that the stats receiver framework is multi-threaded, so each call to
 * {@link StatsTap.onRouterStats} or {@link StatsTap.onPumpStats} may be on a
 * different thread. Therefore, implementations can receive messages
 * out-of-order from multiple threads.
 */
public interface StatsTap {
    /**
     * Initializes the stats receiver plugin with the given properties. Plugins
     * implementing this interface may choose to establish connection to
     * databases or perform any other initialize here.
     * 
     * @param properties
     *            The configuration properties read by the {@link StatsReceiver}
     *            from the configuration file.
     * @throws ReceiverException
     *             Should be thrown when any missing required configuration
     *             properties or unable to initialize the plugin, such as
     *             invalid database connection.
     */
    void initialize(StatsReceiverProperties properties) throws ReceiverException;

    /**
     * Called when the StatsReceiver is shutting down.
     */
    void cleanup();

    /**
     * Called when {@link StatsReceiver} client has received a stats messages
     * published by StatsPump.
     * 
     * @param msg
     *            The stats message containing the list of tags and key-value
     *            pairs of metrics for a specific measurement.
     * @throws ReceiverException
     *             Should be thrown if unable to process or store the received
     *             stats messages.
     */
    void onRouterStats(StatsMessage msg) throws ReceiverException;

    /**
     * Called when {@link StatsReceiver} client has received a poller start 
     * message published by StatsPump.
     * 
     * @param host
     *            The name of the message router which the poller is running against.
     * @param shortname
     *            The abbreviated name of the poller such as VPN/CLIENT_STATS.
     * @param longname
     *            The full name of the poller such as "show client * stats".
     * @throws ReceiverException
     *             Should be thrown if unable to process the start message.
     */
    void onPollerStart(String host, String shortname, String longname);

    /**
     * Called when {@link StatsReceiver} client has received a poller end 
     * message published by StatsPump.
     * 
     * @param host
     *            The name of the message router which the poller is running against.
     * @param shortname
     *            The abbreviated name of the poller such as VPN/CLIENT_STATS.
     * @param longname
     *            The full name of the poller such as "show client * stats".
     * @param success
     *            Indicates whether or not the poller finished successfully.
     * @throws ReceiverException
     *             Should be thrown if unable to process the start message.
     */
    void onPollerEnd(String host, String shortname, String longname, boolean success);

    /**
     * Called when the {@link StatsReceiver} client has received a stats message
     * from StatsPump containing metrics about the polling information.
     * StatsPump periodically sends out internal statistics about the pollers
     * collecting information from Solace Message Routers.
     * 
     * @param msg
     *            The stats message containing StatsPump's internal poller
     *            statistics.
     * @throws ReceiverException
     *             Should be thrown if unable to process or store the received
     *             stats messages.
     */
    void onPumpStats(StatsMessage msg) throws ReceiverException;

    /**
     * Called by the {@link StatsReceiver} client at a scheduled interval.
     * Plugins implementing this interface may choose to output simple internal
     * stats to the logging framework.
     */
    void onPeriodicStatusReport();

    /**
     * Called by the {@link StatsReceiver} client during shutdown to log
     * internal plugin statistics.
     * 
     * @return The instance of the internal statistics created by the plugin.
     */
    InternalStats getInternalStats();
}
