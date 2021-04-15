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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solace.psg.enterprisestats.statspump.util.NamedThreadFactory;
import com.solacesystems.jcsmp.InvalidPropertiesException;
import com.solacesystems.jcsmp.JCSMPChannelProperties;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPReconnectEventHandler;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.JCSMPStreamingPublishEventHandler;
import com.solacesystems.jcsmp.JCSMPTransportException;
import com.solacesystems.jcsmp.SessionEvent;
import com.solacesystems.jcsmp.SessionEventArgs;
import com.solacesystems.jcsmp.SessionEventHandler;
import com.solacesystems.jcsmp.XMLMessageProducer;

/**
 * The VPN Connection manager will belong to a specific message bus. But each
 * message bus will have its own JCSMP Context
 * 
 * @author alee
 *
 */
public class VpnConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(VpnConnectionManager.class);

    public static final int INTER_RECONNECT_ATTEMPT_DELAY_SEC = 15;
    public static final int MAX_RECONNECT_ATTEMPT_DELAY_SEC = 60 * 15; // 15
                                                                       // minutes

    private static final ScheduledExecutorService connectionExecutor = Executors.newScheduledThreadPool(8,
            new NamedThreadFactory("Connect Thread"));
    private final MessageBus msgBus;
    // VPN name --> VpnConnection
    private final Map<String, VpnConnection> vpnNameVpnConnectionMap = new HashMap<String, VpnConnection>();

    public VpnConnectionManager(final MessageBus msgBus) {
        this.msgBus = msgBus;
    }

    List<VpnConnection> getAllVpnConnections() {
        // will never be null b/c first map is initialized in construtor, but
        // might be empty at startup. too bad
        // when building a new collection using another one, the iterator gets
        // used, so could possibly end up in ConcurrentModification
        // *** need to chck into that concurrent modification bit need some way
        // to step through
        return new ArrayList<VpnConnection>(vpnNameVpnConnectionMap.values());
    }

    /**
     * This method returns the VPN name that has been configured as the
     * 'default', or explicitly specified in the XML config.
     */
    VpnConnection getSpecifiedVpnConnection() {
        assert msgBus.getVpn() != null;
        return getVpnConnection(msgBus.getVpn());
    }

    /**
     * Note that this method will overwrite the destination vpnName if a VPN
     * name is configured in the msg-bus.
     * 
     * @param msgBus
     * @param vpnName
     * @return
     */
    VpnConnection getVpnConnection(String vpnName) {
        if (msgBus.getVpn() != null) { // means in the configuration we want to
                                       // publish all 'self' VPN stats to a
                                       // specific VPN
            // so override the VPN name where the message thinks it wants to go
            vpnName = msgBus.getVpn();
        }
        // else just leave it as the passed-in VPN name (which should be the VPN
        // owning the SEMP
        // carry on
        // TODO TEMPORARY: means the msgBus has vpnName == null (pub back to
        // self VPN) and no vpnName for this message
        // that could only mean the message was an appliance-wide message...
        // that should be impossible... it should ask for the list of all
        // connections on a host
        if (vpnName == null) {
            throw new AssertionError(
                    "I don't think I should be here... in getVpnConnection() and passed vpnName is null, and msgBus.vpnName is also null");
            // return null;
        }
        // if the Map contains this VPN name, then we've tried to connect
        // already. Return it
        if (vpnNameVpnConnectionMap.containsKey(vpnName)) {
            return vpnNameVpnConnectionMap.get(vpnName);
        } else {
            synchronized (vpnNameVpnConnectionMap) {
                // TODO I'd prefer not to have synchronized, but race condition
                // if 2+ pumps try to connect to same VPN
                if (vpnNameVpnConnectionMap.containsKey(vpnName)) {
                    return vpnNameVpnConnectionMap.get(vpnName);
                } else {
                    // put an unconnected placeholder instead to say we've asked
                    // for it, start the connection process, and return it
                    // anyway
                    VpnConnection connection = new VpnConnection(msgBus.getHostnameOrIp(), vpnName,
                            msgBus.getUsername(), msgBus.getPassword());
                    vpnNameVpnConnectionMap.put(vpnName, connection);
                    spawnConnectionAttempt(connection, 0);// ,INTER_RECONNECT_ATTEMPT_DELAY_SEC);
                    return connection;
                }
            }
        }
    }

    void spawnConnectionAttempt(final VpnConnection connection, final int initialDelayInSec) {
        Runnable connectRunnable = new Runnable() {
            public void run() {
                try {
                    // this may return right away
                    connection.connect();
                } catch (JCSMPException e) {
                    connection.state = ConnectionState.DISCONNECTED;
                    // reset the flag... between next connection attempt, let's
                    // see if this VpnConnection is asked for
                    connection.resetRequestedDuringReconnection();
                    int attemptNumber = connection.getAndIncReconnectAttempts();
                    // 15 seconds, 15, 30, 30, 60, 60, 120, 120, 240, 240, ...
                    int newRetryDelay = Math.min(MAX_RECONNECT_ATTEMPT_DELAY_SEC,
                            INTER_RECONNECT_ATTEMPT_DELAY_SEC * (int) Math.pow(2, attemptNumber / 2));
                    logger.info(String.format(
                            "Couldn't connect to %s (attempt %d) due to '%s'. Blocking connect attempts for %d %s",
                            connection, attemptNumber + 1, e.getMessage(),
                            newRetryDelay > 60 ? newRetryDelay / 60 : newRetryDelay,
                            newRetryDelay > 60 ? "min" : "sec"));
                    connectionExecutor.schedule(new Runnable() {
                        @Override
                        public void run() {
                            // has this VpnConnection been asked for? If so,
                            // connect, otherwise, kill it
                            if (connection.requestedDuringReconnection) {
                                spawnConnectionAttempt(connection, 0);
                            } else {
                                // definitely the BROADCAST messages are causing
                                // the connect attempts
                                logger.info("Terminating connection attempts to " + connection);
                                // TODO may not work with mgmt-msg-bus... check
                                // this logic
                                connection.silentTerminate();
                            }
                        }
                    }, newRetryDelay, TimeUnit.SECONDS);
                }
            }
        };
        logger.debug("Attempting to schedule a connection attempt for " + connection);
        // start the connection process for this 'pump' on a pump thread
        connectionExecutor.schedule(connectRunnable, initialDelayInSec, TimeUnit.SECONDS);
        // connectionExecutor.execute(connectRunnable);
        // scheduleWithDelay(pumpExecutor,connectRunnable,initialDelayInSec*1000);
    }

    // inner class
    // ////////////////////////////////////////////////////////////////////////////////

    enum ConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED,
    }

    public class VpnConnection {

        private final String host;
        private final String vpn;
        private final String username;
        private final String password;
        private final JCSMPSession session;
        private final SessionEventHandler sessionEventHandler;
        private final JCSMPReconnectEventHandler reconnectEventHandler;
        private final JCSMPStreamingPublishEventHandler publishEventHandler;
        private XMLMessageProducer producer = null;
        private final JCSMPProperties properties;
        private ConnectionState state = ConnectionState.DISCONNECTED;
        boolean requestedDuringReconnection = false;
        private int manualReconnectAttempts = 0;
        boolean errorsDuringSupression = false;
        private int errorSuppressionCount = 0;

        // private final MonitoredLinkBlockingQueue<Message> messagesQueue;

        VpnConnection(String host, String vpn, String user, String password) {
            this.host = host;
            this.vpn = vpn;
            if (user == null) {
                username = System.getProperty("user.name");
            } else {
                username = user;
            }
            this.password = password; // could be null
            if (this.host == null || this.host.isEmpty()) {
                throw new IllegalArgumentException("Host name cannot be empty - " + msgBus.getShortName());
            }
            if (this.vpn == null || this.vpn.isEmpty())
                throw new IllegalArgumentException("VPN name cannot be empty");
            properties = buildProperties();
            try {
                sessionEventHandler = new StatsPumpSessionEventHandler();
                reconnectEventHandler = new StatsPumpReconnectEventHandler();
                publishEventHandler = new StatsPumpPublishEventHandler();
                // each MessageBus object has its own Context... use that for
                // the connection
                session = JCSMPFactory.onlyInstance().createSession(properties, msgBus.getContext(),
                        sessionEventHandler);
                // messagesQueue = new
                // MonitoredLinkBlockingQueue<Message>(this.toString());
            } catch (InvalidPropertiesException e) {
                logger.error(String.format("Invalid properties when trying to connect to %s", this), e);
                throw new RuntimeException(e);
            }
        }

        private JCSMPProperties buildProperties() {
            JCSMPProperties properties = new JCSMPProperties();
            properties.setProperty(JCSMPProperties.HOST, host); // msg-backbone
                                                                // ip:port
            properties.setProperty(JCSMPProperties.VPN_NAME, vpn); // message-vpn
            properties.setProperty(JCSMPProperties.USERNAME, username); // client-username
            if (password != null && !password.isEmpty()) {
                properties.setProperty(JCSMPProperties.PASSWORD, password);
            }
            // String randomHashString =
            // Base64.encodeBytes(Double.toString(Math.random()).getBytes()); //
            // Solace utils Base64
            // Apache common utils
            String randomHashString = Base64.encodeBase64String(Double.toString(Math.random()).getBytes());
            // randomHashString = "0123456789";
            properties.setProperty(JCSMPProperties.CLIENT_NAME, "Stats_Publisher__#"
                    + randomHashString.substring(randomHashString.length() / 2, randomHashString.length() / 2 + 8));

            properties.setProperty(JCSMPProperties.APPLICATION_DESCRIPTION,
                    String.format("Solace Stats Publisher  " + "(VPN %s)", vpn));
            // Stats_Publisher__#MTMxNDYw 1 solmwm101 123456789012345678901234
            // 5678901234567890123456
            // 7890123456789012345678
            // 901234567890
            // properties.setProperty(JCSMPProperties.APPLICATION_DESCRIPTION,"12345678901234567890123456789012345678901234567890123456789012345678901234567890");
            properties.setProperty(JCSMPProperties.MESSAGE_CALLBACK_ON_REACTOR, true); // save
                                                                                       // a
                                                                                       // thread
            final JCSMPChannelProperties channelProperties = new JCSMPChannelProperties();
            channelProperties.setConnectTimeoutInMillis(2000);
            channelProperties.setConnectRetriesPerHost(0);
            channelProperties.setConnectRetries(2); // used to be 0
            channelProperties.setReconnectRetries(1); // used to be 2
            channelProperties.setReconnectRetryWaitInMillis(1000);
            // Enable Nagle's Algorithm (RFC 896). Improves throughtput, hurts
            // latency
            channelProperties.setTcpNoDelay(false);
            properties.setProperty(JCSMPProperties.CLIENT_CHANNEL_PROPERTIES, channelProperties);
            return properties;
        }

        private class StatsPumpSessionEventHandler implements SessionEventHandler {
            @Override
            public void handleEvent(SessionEventArgs args) {
                // quite rare that this actually happens... not exposed in my
                // testing... looks like only SUBSCRIPTION_ERROR and
                // VIRTUAL_ROUTER_NAME_CHANGED
                logger.warn(String.format("########################### Received a session event for %s: %s%n",
                        VpnConnection.this, args.toString()));
                logger.info(args.getInfo());
                if (args.getEvent() == SessionEvent.VIRTUAL_ROUTER_NAME_CHANGED) {
                }
            }
        };

        private class StatsPumpReconnectEventHandler implements JCSMPReconnectEventHandler {

            @Override
            public boolean preReconnect() {
                logger.debug(String.format("preReconnect() called for %s", VpnConnection.this));
                // if (isConnected) { // this is the first reconnect attempt...
                // means we just got disconnected
                // isConnected = false; // currently disconnected, but session
                // isn't closed yet... that doesn't happen until reconnect
                // attempts die
                // logger.info(String.format("Loss of connection for %s... %d
                // automatic reconnect attempts
                // starting",VpnConnection.this,((JCSMPChannelProperties)session.getProperty(JCSMPProperties.CLIENT_CHANNEL_PROPERTIES)).getReconnectRetries()));
                // }
                if (state == ConnectionState.CONNECTED) {
                    // this is the first reconnect attempt... means we just got
                    // disconnected
                    // currently disconnected, but session isn't closed yet...
                    // that doesn't happen until reconnect attempts die
                    state = ConnectionState.DISCONNECTED;
                    logger.info(String.format("Loss of connection for %s... %d automatic reconnect attempts starting",
                            VpnConnection.this,
                            ((JCSMPChannelProperties) session.getProperty(JCSMPProperties.CLIENT_CHANNEL_PROPERTIES))
                                    .getReconnectRetries()));
                }
                return true;
            }

            @Override
            public void postReconnect() {
                // success!
                state = ConnectionState.CONNECTED;
                String virtualRouterName = (String) session.getProperty(JCSMPProperties.VIRTUAL_ROUTER_NAME);
                logger.info(String.format("Reconnected to %s (%s)", VpnConnection.this, virtualRouterName));
                // since all VPN connections for this manager belong to the same
                // MessageBus, just set the name
                msgBus.setVirtualRouterName(virtualRouterName);
            }
        };

        private class StatsPumpPublishEventHandler implements JCSMPStreamingPublishEventHandler {

            @Override
            public void responseReceived(String messageID) {
            }

            @Override
            public void handleError(String messageID, JCSMPException e, long timestamp) {
                // currently only using Direct, so hangup... when making a
                // Guaranteed publisher, we could get NACKs.
                // VpnConnection.this.silentTerminate(); // if e instanceof
                // JCSMPTransportException the session will automatically be
                // closed... but this will force it
                // logger.info(String.format("Publisher for %s received an error
                // due to '%s'. Disconnected. Attempting to reconnect in %d
                // seconds.",
                // VpnConnection.this,e.toString(),INTER_RECONNECT_ATTEMPT_DELAY_SEC));
                if (e instanceof JCSMPTransportException) {
                    // in %d seconds.",
                    logger.info(String.format(
                            "Publisher for %s received an error due to '%s'. Disconnected. Attempting to reconnect.",
                            VpnConnection.this, e.toString(), INTER_RECONNECT_ATTEMPT_DELAY_SEC));
                    // Fixing an issue where if the appliance/VMR message spool gets in a bad state,
                    // the publisher flow will throw an exception but the connection remains
                    silentTerminate();  // clean up the connection in case we're not fully disconnected. Reconnection will auto happen
                } else { // most likely due to ACL on publisher, so not
                         // disconnected
                    // need to have a supressed flag on/off too!
                    int attemptNumber = ++errorSuppressionCount;
                    // 15 seconds, 15, 30, 30, 60, 60, 120, 120, 240, 240, ...
                    int newRetryDelay = Math.min(MAX_RECONNECT_ATTEMPT_DELAY_SEC,
                            INTER_RECONNECT_ATTEMPT_DELAY_SEC * (int) Math.pow(2, attemptNumber / 2));
                    logger.info(String.format(
                            "Publisher for %s received an error due to '%s'. Supressing further errors logs for %d %s.",
                            VpnConnection.this, e.toString(), newRetryDelay > 60 ? newRetryDelay / 60 : newRetryDelay,
                            newRetryDelay > 60 ? "min" : "sec"));
                    // logger.debug(e, e);

                }
            }
        };

        public boolean isConnected() {
            return state == ConnectionState.CONNECTED;
        }

        void setRequestedDuringReconnection() {
            requestedDuringReconnection = true;
        }

        private void resetRequestedDuringReconnection() {
            requestedDuringReconnection = false;
        }

        private int getAndIncReconnectAttempts() {
            return manualReconnectAttempts++;
        }

        void connect() throws JCSMPException {
            synchronized (state) {
                // only allow this particular thread to kick off a connection
                // attempt if we're not CONNECTED or CONNECTING
                if (state != ConnectionState.DISCONNECTED) {
                    return;
                } else {
                    state = ConnectionState.CONNECTING;
                }
            }
            logger.debug(String.format("Connecting to VPN '%s' on %s", vpn, host));
            session.connect(); // this will throw an exception if we can't
                               // connect
            // otherwise, hooray!
            producer = session.getMessageProducer(publishEventHandler);
            // session.getMessageConsumer(reconnectEventHandler,new
            // StatsPumpMessageListener()).start();
            // for the consumer, just give it the reconnect handler... don't
            // need a listener, or even need to start it
            session.getMessageConsumer(reconnectEventHandler, null);
            state = ConnectionState.CONNECTED;
            manualReconnectAttempts = 0; // reset the reconnect attempt counter
                                         // in case we were attempting to
                                         // reconnect
            String virtualRouterName = (String) session.getProperty(JCSMPProperties.VIRTUAL_ROUTER_NAME);
            // since all VPN connections for this manager belong to the same
            // MessageBus, just set the name
            msgBus.setVirtualRouterName(virtualRouterName);
            logger.info(String.format("Connected to %s", this));
        }

        void disconnect() {
            logger.info(String.format("disconnect() called on %s", this));
            silentTerminate();
        }

        private void silentTerminate() {
            state = ConnectionState.DISCONNECTED;
            if (session != null) {
                // will also automatically close the producer
                session.closeSession();
            }
            // TODO do I really need a synchronized block around this since I'm
            // using synchronized maps? should test using debug steps
            synchronized (vpnNameVpnConnectionMap) {
                vpnNameVpnConnectionMap.remove(vpn);
            }
            // this will most likely cause this object to disappear since the
            // map is the only thing that holds a reference to it
        }

        final XMLMessageProducer getProducer() {
            return producer;
        }

        public String getVpn() {
            return vpn;
        }

        public String getVpnNameInUse() {
            return (String) properties.getProperty(JCSMPProperties.VPN_NAME_IN_USE);
        }

        @Override
        public String toString() {
            return String.format("VpnConnection '%s' on '%s'", vpn, msgBus.getShortName());
            // return String.format("VpnConnection '%s' on '%s'",vpn,host);
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || !(o instanceof VpnConnection))
                return false;
            if (host.equals(((VpnConnection) o).host) && vpn.equals(((VpnConnection) o).vpn)
                    && username.equals(((VpnConnection) o).username)) {
                return true;
            } else {
                return false;
            }
        }
    };
    // End inner class VpnConnection
}
