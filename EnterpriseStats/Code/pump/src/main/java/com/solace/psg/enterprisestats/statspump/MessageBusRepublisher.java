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
package com.solace.psg.enterprisestats.statspump;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solace.psg.enterprisestats.statspump.VpnConnectionManager.VpnConnection;
import com.solace.psg.enterprisestats.statspump.config.xml.DestinationType;
import com.solace.psg.enterprisestats.statspump.containers.ContainerFactory;
import com.solace.psg.enterprisestats.statspump.util.SingleSolaceMessageFactory;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.JCSMPErrorResponseException;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.Topic;
import com.solacesystems.jcsmp.XMLMessage;

public class MessageBusRepublisher implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(MessageBusRepublisher.class);

    private final MessageBus messageBus;
    private VpnConnectionManager connManager = null;
    private static final AtomicInteger totalStatMsgProcCount = new AtomicInteger();
    private static final AtomicInteger totalJcsmpMsgSentCount = new AtomicInteger();

    public static boolean PublishDmqEligible = false;
    public static long PublishTtl = 0;

    public MessageBusRepublisher(MessageBus msgBus) {
        this.messageBus = msgBus;
        connManager = new VpnConnectionManager(this.messageBus);
    }

    private void sendMessage(VpnConnection connection, StatsPumpMessage statsMsg) {
        if (connection == null) {
            // this is what it used to mean:
            // either can't connect, or haven't yet, or don't want to (i.e. VPN
            // shut down or MNR N/A or something)
            // but now, this shouldn't really happen..?! -- it can for now, but
            // shouldn't in future
            logger.error("in sendMessage() and VpnConnection is null");
            return;
        }
        if (!connection.isConnected()) {
            if (statsMsg.getVpn() != null) { // i.e. not an appliance-wide
                                             // message for every VPN
                connection.setRequestedDuringReconnection();
            }
            return;
        }
        final Topic topic = statsMsg.getJcsmpTopic();
        // create the message variable
        XMLMessage msg = null;
        try {
            // build the message according to this message bus' container
            // factory
            msg = statsMsg.buildJcsmpMessage(messageBus.getContainerFactory());
            if (connection.getProducer() != null) { // probably haven't finished
                                                    // connecting yet
                // this next line is commented out b/c we have the
                // connection.isConnected up there
                // connection.getProducer().send(msg,topic);
            } else {
                logger.error("Should be impossible: Inside sendMessage() for " + connection
                        + "... connection.isConnected == true, but connection.getProducer == null " + msg.dump());
            }

            msg.setTimeToLive(PublishTtl);
            msg.setDMQEligible(PublishDmqEligible);
            logger.trace("Publishing with ttl=" + PublishTtl + ", DmqEligible=" + PublishDmqEligible);

            connection.getProducer().send(msg, topic);
            totalJcsmpMsgSentCount.incrementAndGet();
        } catch (IllegalArgumentException e) {
            // most likely due to an invalid topic
            logger.info(String.format("Caught %s while trying to publish a message %s on VPN '%s' on %s", e.toString(),
                    statsMsg.toString(), connection.getVpn(), statsMsg.getLogicalAppliance()));
        } catch (JCSMPErrorResponseException e) {
            // TODO if a SYSTEM message, vpn will be null and would look like a
            // bad exception
            logger.info(String.format("Caught exception while trying to publish a message %s on VPN '%s' on %s",
                    statsMsg.toString(), connection.getVpn(), statsMsg.getLogicalAppliance()), e);
        } catch (JCSMPException e) {
            logger.info(String.format("Caught exception while trying to publish a message %s on VPN '%s' on %s",
                    statsMsg.toString(), connection.getVpn(), statsMsg.getLogicalAppliance()), e);
        } finally {
            // in a finally in case there are any exceptions thrown out after
            // this message has been taken from the pool
            if (msg != null) {
                SingleSolaceMessageFactory.doneWithMessage(msg); // put it back
                                                                 // in the pool
            }
        }
    }

    private void mgmtMsgBusRunLoop() throws InterruptedException {
        StatsPumpMessage statsMsg = null;
        for (;;) {
            try {
                statsMsg = messageBus.takeMessage();
                totalStatMsgProcCount.incrementAndGet();
                // first, make sure this is not a 'self' message... why would
                // that be getting published in the mgmt message bus?
                assert (statsMsg.getDestination() == DestinationType.MGMT
                        || statsMsg.getDestination() == DestinationType.BOTH);
                // then, if it's a management message, double-check this message
                // didn't come from a particular VPN
                // if (statsMsg.getDestination() == DestinationType.MGMT) assert
                // statsMsg.getVpn() == null; // actually! who cares if it did!!
                VpnConnection connection = connManager.getSpecifiedVpnConnection();
                sendMessage(connection, statsMsg);
            } catch (InterruptedException e) { // so the next catch block
                                               // doesn't catch it
                throw e;
            } catch (Throwable e) {
                logger.warn("Caught this while trying to publish " + statsMsg + " on " + messageBus
                        + "!!! argh1!! Still, we continue.", e);
            }
        }
    }

    private void selfMsgBusRunLoop() throws InterruptedException {
        StatsPumpMessage statsMsg = null;
        for (;;) {
            try {
                statsMsg = messageBus.takeMessage();
                totalStatMsgProcCount.incrementAndGet();
                assert statsMsg.getDestination() != DestinationType.MGMT;
                if (statsMsg.getDestination() == DestinationType.SELF) {
                    assert statsMsg.getVpn() != null; // that is: this shouldn't
                                                      // be an appliance-level
                                                      // message sent just to
                                                      // 'self'??!? mgmt would
                                                      // always want this kind
                                                      // of msg too (should be
                                                      // BOTH)
                    // if the message's VPN starts with #, let's assume no
                    // 'SELF' people will need to see it (maybe if it was 'BOTH'
                    // for mgmt..?)
                    if (statsMsg.getVpn().startsWith("#"))
                        continue; // e.g. #config-sync... just a SELF msg, so
                                  // obviously not something MGMT wants
                    if (messageBus.getVpn() == null) { // means we're publishing
                                                       // back onto own VPN
                        // if this message's VPN is in the exception list, and
                        // XOR with our default action
                        if (!messageBus.isAllowedToPublish(statsMsg))
                            continue;
                        // also, don't try to connect to VPNs that are shutdown
                        // or non-existent
                        // if the VPN isn't enabled, operational, and
                        // locally-configured
                        // TODO should this for sure be physical appliance???
                        // ***********************************************************************************
                        // what about VPNs on the backup appliance that are
                        // up..? weird things like that
                        if (!statsMsg.getPhysicalAppliance().isActiveVpn(statsMsg.getVpn()))
                            continue;
                        // these wouldn't matter too much b/c we probably can't
                        // connect to them anyway, so VpnConnection returned
                        // would never connect
                    } else {
                        // else (messageBus.vpn has been set in the config XML)
                        // this means that we have configured a specific VPN to
                        // receive all VPN messages
                        // (therefore this VPN will still receive stats and
                        // thing for shutdown VPNs, ignored ones, but not # ones
                    }
                    VpnConnection connection = connManager.getVpnConnection(statsMsg.getVpn());
                    sendMessage(connection, statsMsg);
                } // end: statsMsg.getDestination() == DestinationType.SELF
                else { // must be BOTH!
                       // be careful not to double post to the management
                       // VPN(s)!
                       // if they're the exact same (host, vpn, container) then
                       // break
                       // TODO this can't happen anymore
                       // if
                       // (statsMsg.getLogicalAppliance().getMgmtMsgBus().equals(statsMsg.getLogicalAppliance().getSelfMsgBus()))
                       // continue;

                    // next, is this an appliance level or broadcast msg? might
                    // have to goto multiple destinations
                    if (statsMsg.getVpn() == null) {
                        // VPN == null, so broadcast/appliance message.
                        // Ok, now does this particular BOTH message have to
                        // goto one specified VPN?
                        if (messageBus.getVpn() == null) { // means we have to
                                                           // publish to ALL
                                                           // VPNs..! (minus the
                                                           // mgmt one, if
                                                           // applicable)
                            for (VpnConnection connection : connManager.getAllVpnConnections()) {
                                // don't double-post to the mgmt VPN is going to
                                // the same msg-bus host
                                // TODO this needs to be fixed!
                                if (connection.getVpn().equals("solmwm") && // e.g.
                                                                            // solmwm
                                                                            // ==
                                                                            // solmwm
                                        statsMsg.getLogicalAppliance().hasMatchingManagementMsgBus(messageBus)) {
                                    continue;
                                } // else, we're all good... post to each one!
                                  // if we're not connected, it'll get skipped
                                  // TODO: verify these statements
                                  // and the List of VpnConnections will only
                                  // have
                                  // connections we're supposed to connect to
                                  // b/c the logic for connecting lie behind all
                                  // the checks (above and below) for exceptions
                                  // /
                                  // shutdown / etc.
                                sendMessage(connection, statsMsg);
                            }
                        } else { // this this is a BROADCAST/SYSTEM message,
                                 // going to a specific chosen VPN, we don't
                                 // have to filter as above
                            VpnConnection connection = connManager.getVpnConnection(messageBus.getVpn());
                            sendMessage(connection, statsMsg);
                        }
                    } else { // statsMsg.getVpn() != null... just a regular VPN
                             // message (non-SYSTEM or BROADCAST)
                        // is this message meant for the mgmt VPN? If so, skip
                        // other we'll be doing a double-publish
                        // TODO fix this!!
                        // if
                        // (statsMsg.getVpn().equals(statsMsg.getLogicalAppliance().getMgmtMsgBus().getVpn()))
                        // continue;
                        if (messageBus.getVpn() == null) { // means we're
                                                           // publishing back
                                                           // onto own VPN
                            // if this message's VPN is in the exception list,
                            // and XOR with our default action
                            if (!messageBus.isAllowedToPublish(statsMsg))
                                continue;
                            if (statsMsg.getVpn().startsWith("#"))
                                continue; // e.g. #config-sync... don't try to
                                          // connect there
                            // also, don't try to connect to VPNs that are
                            // shutdown or non-existent
                            // if the VPN isn't enabled, operational, and
                            // locally-configured
                            // TODO should this for sure be physical
                            // appliance???
                            // ***********************************************************************************
                            // what about VPNs on the backup appliance that are
                            // up..? weird things like that
                            if (!statsMsg.getPhysicalAppliance().isActiveVpn(statsMsg.getVpn()))
                                continue;
                            // these wouldn't matter too much b/c we probably
                            // can't connect to them anyway, so VpnConnection
                            // returned would never connect
                        } else {
                            // else (messageBus.vpn has been set in the XML)
                            // this means that we have configured a specific VPN
                            // to receive all VPN messages
                            // (therefore this VPN will still receive stats and
                            // things for all VPNs: shutdown ones, ignored ones,
                            // even # ones

                        }
                        VpnConnection connection = connManager.getVpnConnection(statsMsg.getVpn());
                        sendMessage(connection, statsMsg);
                    }
                }
            } catch (InterruptedException e) { // so the next catch block
                                               // doesn't catch it
                throw e;
            } catch (Throwable e) {
                logger.warn("Caught this while trying to publish " + statsMsg + " on " + messageBus
                        + "!!! argh1!! Still, we continue.", e);
            }
        }
    }

	private void localMgmtMsgBusRunLoop() throws InterruptedException {
		StatsPumpMessage statsMsg = null;
		for (;;) {
			try {
				statsMsg = messageBus.takeMessage();
				totalStatMsgProcCount.incrementAndGet();
				
				// build the message according to this message bus' container
				// factory
				BytesXMLMessage msg = null;

				try {
					// Convert to an XMLMessage using the container factory
					ContainerFactory f = messageBus.getContainerFactory();
					if (f == null) {
						throw new AssertionError("message bus returned a null container factory.");
					}
					msg = statsMsg.buildJcsmpMessage(f);
					
					// Send the message to the local mgmt bus listener
					messageBus.getLocalMgmtBusListener().onMgmtBusStats(msg);
					totalJcsmpMsgSentCount.incrementAndGet();
				} catch (JCSMPException e) {
					logger.warn(String.format("Caught while trying to publish a message %s to local listener '%s'",
							statsMsg.toString(), messageBus.getLocalMgmtBusListener().getClass().getName()), e);
				} finally {
					// in a finally in case there are any exceptions thrown out
					// after this message has been taken from the pool
					if (msg != null) {
						// put it back in the pool
						SingleSolaceMessageFactory.doneWithMessage(msg);
					}
				}

			} catch (InterruptedException e) { // so the next catch block
												// doesn't catch it
				throw e;
			} catch (Throwable e) {
				logger.warn("Caught this while trying to publish " + statsMsg + " on " + messageBus, e);
			}
		}
	}

    @Override
    public void run() {
        try {
            if (messageBus.isMgmtMsgBus()) { // is this a management msg bus? If
                                             // so, easy... just one destination
                mgmtMsgBusRunLoop();
            } else if (messageBus.isLocalMgmtMsgBus()) {
            	localMgmtMsgBusRunLoop();
            } else { // msg bus = 'self' ... OR? or, VPN level messages
                selfMsgBusRunLoop();
            }
        } catch (InterruptedException e) {
            logger.info("Pump for " + messageBus.toString()
                    + " has been interrupted, so probably shutting down. Stopping this Message Bus Pump");
        } catch (RuntimeException e) {
            logger.warn("Caught this on " + messageBus.toString() + ".  Actually, this shouldn't make it here.", e);
        }
    }
}
