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
package com.solace.psg.enterprisestats.receiver.transport;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solace.psg.enterprisestats.receiver.ReceiverException;
import com.solace.psg.enterprisestats.receiver.ReceiverNotConnectedException;
import com.solace.psg.enterprisestats.receiver.StatsReceiverProperties;
import com.solace.psg.enterprisestats.receiver.utils.ReceiverUtils;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.ConsumerFlowProperties;
import com.solacesystems.jcsmp.FlowReceiver;
import com.solacesystems.jcsmp.JCSMPChannelProperties;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPReconnectEventHandler;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.JCSMPStreamingPublishCorrelatingEventHandler;
import com.solacesystems.jcsmp.Queue;
import com.solacesystems.jcsmp.Topic;
import com.solacesystems.jcsmp.XMLMessageConsumer;
import com.solacesystems.jcsmp.XMLMessageListener;
import com.solacesystems.jcsmp.XMLMessageProducer;

/**
 * This class provides the ability of connecting to a Solace Message Router,
 * publish messages, subscribe to topics and queues, receive messages on the
 * subscribed destinations, and disconnect from Solace.
 */
public class SolaceTransport
        implements XMLMessageListener, JCSMPStreamingPublishCorrelatingEventHandler, JCSMPReconnectEventHandler {
    private static final Logger s_logger = LoggerFactory.getLogger(SolaceTransport.class);

    private static final String SOLACE_HOST_PROP_NAME = "SOLACE_HOST";
    private static final String SOLACE_VPN_PROP_NAME = "SOLACE_VPN";
    private static final String SOLACE_USERNAME_PROP_NAME = "SOLACE_USERNAME";
    private static final String SOLACE_PASSWORD_PROP_NAME = "SOLACE_PASSWORD";
    private static final String SOLACE_CLIENT_NAME_PROP_NAME = "SOLACE_CLIENT_NAME";
    private static final String SOLACE_APP_DESCRIPTION_PROP_NAME = "SOLACE_APP_DESCRIPTION";
    private static final String SOLACE_RECONNECT_RETRIES_PROP_NAME = "SOLACE_RECONNECT_RETRIES";
    private static final String SOLACE_CONNECT_RETRIES_PROP_NAME = "SOLACE_CONNECT_RETRIES";
    private static final String SOLACE_CONNECT_RETRIES_PER_HOST_PROP_NAME = "SOLACE_CONNECT_RETRIES_PER_HOST";
    private static final String SOLACE_RECONNECT_RETRY_WAIT_MS_PROP_NAME = "SOLACE_RECONNECT_RETRY_WAIT_MS";
    private static final String SOLACE_OVERRIDE_SESSION_PROP_LIST_PROP_NAME = "SOLACE_OVERRIDE_SESSION_PROP_LIST";

    private static final int DEFAULT_CONNECT_RETRIES = 1;
    private static final int DEFAULT_CONNECT_RETRIES_PER_HOST = 20;
    private static final int DEFAULT_RECONNECT_RETRIES = 5;

    private final StatsReceiverProperties m_properties;
    private final SolaceTransportListener m_transportListener;
    private String m_host = "";
    private String m_vpn = "";
    private volatile boolean m_isConnected = false;

    private JCSMPSession m_session;
    private XMLMessageProducer m_producer;
    private XMLMessageConsumer m_consumer;

    // List of flow endpoints currently bound to
    private final ConcurrentMap<String, FlowReceiver> m_flowReceivers = new ConcurrentHashMap<String, FlowReceiver>();

    public SolaceTransport(StatsReceiverProperties properties, SolaceTransportListener listener) {
        this.m_properties = properties;
        this.m_transportListener = listener;
    }

    /**
     * Connects to Solace. This method will block until the connection is
     * established or a timeout occurs.
     * 
     * @throws ReceiverException
     */
    public void connect() throws ReceiverException {
        // Check if we are already connected
        if (isConnected()) {
            s_logger.info("Already connected to Solace on host: " + m_host + " on message-vpn: " + m_vpn);
            return;
        }

        s_logger.info("Connecting receiver to Solace...");
        JCSMPProperties sessionProps;

        //
        // Build the configuration
        try {
            sessionProps = buildConfiguration();
            // Create the session (NOTE: this does not connect the session yet)
            m_session = JCSMPFactory.onlyInstance().createSession(sessionProps);
        } catch (Exception e) {
            s_logger.error("Invalid Solace connection configuration", e);
            throw new ReceiverException(e);
        }

        s_logger.info("# ------------------------------------------------------------");
        s_logger.info("#  Solace Connection Properties");
        s_logger.info("#  host: " + sessionProps.getStringProperty(JCSMPProperties.HOST));
        s_logger.info("#  vpn: " + sessionProps.getStringProperty(JCSMPProperties.VPN_NAME));
        s_logger.info("#  client-username: " + sessionProps.getStringProperty(JCSMPProperties.USERNAME));
        s_logger.info("#  client-name: " + sessionProps.getStringProperty(JCSMPProperties.CLIENT_NAME));
        JCSMPChannelProperties cp = (JCSMPChannelProperties) sessionProps
                .getProperty(JCSMPProperties.CLIENT_CHANNEL_PROPERTIES);
        s_logger.info("#  connect retries: " + cp.getConnectRetries());
        s_logger.info("#  connect retries per host: " + cp.getConnectRetriesPerHost());
        s_logger.info("#  reconnect retries: " + cp.getReconnectRetries());
        s_logger.info("#  reconnect retries wait: " + cp.getReconnectRetryWaitInMillis() + " (ms)");
        s_logger.info("#");
        s_logger.info("#  Connecting...");

        //
        // Connect the session
        try {
            // Obtain a consumer & producer - NOTE this operation automatically
            // causes the session to connect
            m_consumer = m_session.getMessageConsumer(this, this);
            m_producer = m_session.getMessageProducer(this);
            // Start the consumer
            m_consumer.start();
            // Flag transport is connected
            m_isConnected = true;
            s_logger.info("#  Connected to Solace!");
            s_logger.info("# ------------------------------------------------------------");
        } catch (JCSMPException e) {
            s_logger.warn("*****  Connect failed ***** {}", e);
            s_logger.info("# ------------------------------------------------------------");
            throw new ReceiverException(e);
        }
    }

    /**
     * Disconnects from Solace.
     */
    public void disconnect() {
        s_logger.info("Disconnecting receiver from Solace...");

        // Close the producer
        if (m_producer != null) {
            m_producer.close();
            m_producer = null;
        	s_logger.info("producer closed");
        }
        // Close the consumer
        if (m_consumer != null) {
            m_consumer.close();
            m_consumer = null;
        	s_logger.info("consumer closed");
        }
        // Close the session
        if (m_session != null) {
            m_session.closeSession();
            m_session = null;
        	s_logger.info("session closed");
        }

        m_isConnected = false;
        s_logger.info("Disconnected fron Solace broker: " + m_host);
    }

    /**
     * Returns true if the {@link SolaceTransport} is currently connected to
     * Solace.
     */
    public boolean isConnected() {
        return m_isConnected;
    }

    /**
     * Subscribes to a topic on Solace. This method will block until the
     * underlying subscription is acknowledged by Solace or a timeout occurs.
     * This call is idempotent so the caller may safely retry subscribing. The
     * timeout interval used by the service is configurable through the
     * configuration files.
     * 
     * @param destination
     * @throws ReceiverException
     * @throws ReceiverNotConnectedException
     */
    public void subscribe(String destination) throws ReceiverException, ReceiverNotConnectedException {
        this.subscribe(destination, false);
    }

    /**
     * Subscribes to a topic or queue on Solace. This method will block until
     * the underlying subscription or bind is acknowledged by Solace or a
     * timeout occurs. This message is idempotent so the caller may safely retry
     * subscribing. The timeout interval used by the service is configurable
     * through the configuration files.
     * 
     * @param destination
     * @param isQueue
     * @throws ReceiverException
     * @throws ReceiverNotConnectedException
     */
    public void subscribe(String destination, boolean isQueue) throws ReceiverException, ReceiverNotConnectedException {
        if (m_session == null || !isConnected()) {
            throw new ReceiverNotConnectedException("Receiver not connected to Solace");
        }

        try {
            if (isQueue) {
                // First check if we are already bound to this destination
                FlowReceiver flowReceiver = m_flowReceivers.get(destination);
                if (flowReceiver != null) {
                    // Flow already exists - simply return
                    s_logger.debug("Ignoring subscribe - already consuming from queue destination: {}", destination);
                    return;
                }
                // Does not exist so create a new flow
                s_logger.info("Consuming from queue destination: {}", destination);
                Queue queue = JCSMPFactory.onlyInstance().createQueue(destination);
                ConsumerFlowProperties flowProps = new ConsumerFlowProperties();
                flowProps.setAckMode(JCSMPProperties.SUPPORTED_MESSAGE_ACK_CLIENT);
                flowProps.setEndpoint(queue);
                flowProps.setStartState(true);
                // Create the Flow and add to our list
                FlowReceiver newFlowReceiver = m_session.createFlow(this, flowProps);
                flowReceiver = m_flowReceivers.putIfAbsent(destination, newFlowReceiver);
                if (flowReceiver == null)
                    flowReceiver = newFlowReceiver;

            } else {
                // Subscribe to topic
                s_logger.info("Subscribing to topic destination: {}", destination);
                Topic topic = JCSMPFactory.onlyInstance().createTopic(destination);
                m_session.addSubscription(topic, true);
            }
        } catch (JCSMPException e) {
            s_logger.error("Cannot subscribe to destination: {} cause: {}", new Object[] { destination, e.getCause() });
            throw new ReceiverException(e);
        }
    }

    /**
     * Unsubscribes to a topic or queue on Solace. This method will block until
     * the underlying unsubscribe or unbind is acknowledged by Solace or a
     * timeout occurs. The timeout interval used by the service is configurable
     * through configuration file.
     * 
     * @param destination
     * @throws ReceiverException
     * @throws ReceiverNotConnectedException
     */
    public void unsubscribe(String destination, boolean isQueue)
            throws ReceiverException, ReceiverNotConnectedException {
        if (m_session == null || !isConnected()) {
            throw new ReceiverNotConnectedException("Receiver not connected to Solace");
        }

        try {
            if (isQueue) {
                // First check if we are already bound to this destination
                FlowReceiver flowReceiver = m_flowReceivers.remove(destination);
                if (flowReceiver == null) {
                    // Flow does not exist - simply return
                    s_logger.debug("Ignoring unsubscribe - queue destination: {} was not subscribed to previously",
                            destination);
                    return;
                }
                // Close the flow
                s_logger.info("Unsubscribing from queue destination: {}", destination);
                flowReceiver.close();
            } else {
                // Unsubscribe to topic
                s_logger.info("Unsubscribing to topic destination: {}", destination);
                Topic topic = JCSMPFactory.onlyInstance().createTopic(destination);
                m_session.removeSubscription(topic, true);
            }
        } catch (JCSMPException e) {
            s_logger.error("Cannot unsubscribe to destination: {} cause: {}",
                    new Object[] { destination, e.getCause() });
            throw new ReceiverException(e);
        }
    }

    /**
     * Currently not supported. In future it could enqueue a message for
     * publishing to Solace. An active connection to Solace is required.
     * 
     * @param destination
     * @param payload
     * @param isPersistent
     * @return
     * @throws ReceiverException
     * @throws ReceiverNotConnectedException
     */
    public SolaceTransportToken publish(String destination, byte[] payload, boolean isPersistent)
            throws ReceiverException, ReceiverNotConnectedException {
        throw new UnsupportedOperationException();
    }

    private JCSMPProperties buildConfiguration() throws IllegalArgumentException, ReceiverException {
        JCSMPProperties sessionProps = new JCSMPProperties();
        // Host
        m_host = m_properties.getMandatoryStringProperty(SOLACE_HOST_PROP_NAME);
        sessionProps.setProperty(JCSMPProperties.HOST, m_host);
        // VPN
        m_vpn = m_properties.getMandatoryStringProperty(SOLACE_VPN_PROP_NAME);
        sessionProps.setProperty(JCSMPProperties.VPN_NAME, m_vpn);
        // Username
        sessionProps.setProperty(JCSMPProperties.USERNAME,
                m_properties.getMandatoryStringProperty(SOLACE_USERNAME_PROP_NAME));
        // Password
        String encryptedPwd = m_properties.getOptionalStringProperty(SOLACE_PASSWORD_PROP_NAME);
        if (encryptedPwd != null && !encryptedPwd.isEmpty()) {
            String password = ReceiverUtils.decryptPassword(encryptedPwd, SOLACE_PASSWORD_PROP_NAME);
            sessionProps.setProperty(JCSMPProperties.PASSWORD, password);
        }
        sessionProps.setProperty(JCSMPProperties.CLIENT_NAME,
                generateUniqueClientName(m_properties.getOptionalStringProperty(SOLACE_CLIENT_NAME_PROP_NAME)));
        String appDescription = m_properties.getOptionalStringProperty(SOLACE_APP_DESCRIPTION_PROP_NAME);
        if (appDescription == null || appDescription.isEmpty()) {
            appDescription = "Solace Enterprise Stats Receiver";
        }
        sessionProps.setProperty(JCSMPProperties.APPLICATION_DESCRIPTION, appDescription);

        // Channel Properties
        JCSMPChannelProperties cp = (JCSMPChannelProperties) sessionProps
                .getProperty(JCSMPProperties.CLIENT_CHANNEL_PROPERTIES);
        int connectRetries = DEFAULT_CONNECT_RETRIES;
        if (m_properties.getProperty(SOLACE_CONNECT_RETRIES_PROP_NAME) != null) {
            connectRetries = Integer.parseInt((String) m_properties.getProperty(SOLACE_CONNECT_RETRIES_PROP_NAME));
        }
        cp.setConnectRetries(connectRetries);
        int connectRetriesPerHost = DEFAULT_CONNECT_RETRIES_PER_HOST;
        if (m_properties.getProperty(SOLACE_CONNECT_RETRIES_PER_HOST_PROP_NAME) != null) {
            connectRetriesPerHost = Integer
                    .parseInt((String) m_properties.getProperty(SOLACE_CONNECT_RETRIES_PER_HOST_PROP_NAME));
        }
        cp.setConnectRetriesPerHost(connectRetriesPerHost);
        int reconnectRetries = DEFAULT_RECONNECT_RETRIES;
        if (m_properties.getProperty(SOLACE_RECONNECT_RETRIES_PROP_NAME) != null) {
            reconnectRetries = Integer.parseInt((String) m_properties.getProperty(SOLACE_RECONNECT_RETRIES_PROP_NAME));
        }
        cp.setReconnectRetries(reconnectRetries);
        if (m_properties.getProperty(SOLACE_RECONNECT_RETRY_WAIT_MS_PROP_NAME) != null) {
            cp.setReconnectRetryWaitInMillis(
                    Integer.parseInt((String) m_properties.getProperty(SOLACE_RECONNECT_RETRY_WAIT_MS_PROP_NAME)));
        }

        // Reapply subscription - always true
        sessionProps.setProperty(JCSMPProperties.REAPPLY_SUBSCRIPTIONS, true);

        // Override session properties if given
        List<String> overrideProps = stringToArrayList(
                (String) m_properties.getProperty(SOLACE_OVERRIDE_SESSION_PROP_LIST_PROP_NAME), ",");
        if (overrideProps != null) {
            Properties tempProps = sessionProps.toProperties();
            for (int i = 0; i < (overrideProps.size() - 1); i += 2) {
                tempProps.setProperty(overrideProps.get(i), overrideProps.get(i + 1));
            }
            sessionProps = JCSMPProperties.fromProperties(tempProps);
        }

        return sessionProps;
    }

    private static List<String> stringToArrayList(String property, String delimiter) {
        if (property == null || property.isEmpty()) {
            return null;
        }

        String[] tempList = property.split(delimiter);
        return Arrays.asList(tempList);
    }

    private static String generateUniqueClientName(String clientNamePrefix) {
        if (clientNamePrefix == null || clientNamePrefix.isEmpty()) {
            clientNamePrefix = "Stats_Receiver";
        }
        return clientNamePrefix + "_" + System.currentTimeMillis();
    }

    //
    // XMLMessageListener Implementation

    @Override
    public void onException(JCSMPException exception) {
        // In general, the error is unrecoverable.
        s_logger.error("Unrecoverable Solace session error", exception);
        // Notify the listener connection is lost and unrecoverable
        if (m_transportListener != null) {
            try {
                m_transportListener.onConnectionLost();
            } catch (Throwable t) {
                s_logger.error("Unexpected Throwable - onConnectionLost", t);
            }
        } else {
            s_logger.warn("No registered transport listener. Missing onConnectionLost()");
        }
    }

    @Override
    public void onReceive(BytesXMLMessage message) {
        if (m_transportListener != null) {
            try {
                m_transportListener.onMessageReceived(message);
            } catch (Throwable t) {
                s_logger.error("Unexpected Throwable - onMessageReceived", t);
            }
        } else {
            s_logger.warn("No registered transport listener. Missing onMessageReceived()");
        }
    }

    //
    // JCSMPStreamingPublishCorrelatingEventHandler Implementation

    @Override
    public void handleError(String messageId, JCSMPException exception, long timestamp) {
        s_logger.error(
                String.format("Received negative acknowledgement for published message[messageId: %s timestamp: %s]",
                        messageId, timestamp),
                exception);
    }

    @Override
    public void responseReceived(String messageId) {
        s_logger.debug("Received acknowledgement for published message[messageId: " + messageId + "]");
    }

    @Override
    public void handleErrorEx(Object key, JCSMPException exception, long timestamp) {
        if (key instanceof SolaceTransportToken) {
            SolaceTransportToken token = (SolaceTransportToken) key;
            s_logger.error(String.format(
                    "Received negative acknowledgement for published message[messageId: %s topic: %s timestamp: %s]",
                    token.getMessageId(), token.getTopic(), timestamp), exception);
        } else {
            s_logger.error("Received negative acknowledgement for published message[timestamp: " + timestamp + "]",
                    exception);
        }
    }

    @Override
    public void responseReceivedEx(Object key) {
        if (key instanceof SolaceTransportToken) {
            SolaceTransportToken token = (SolaceTransportToken) key;
            String messageId = token.getMessageId();
            String topic = token.getTopic();
            s_logger.debug(String.format("Received acknowledgement for published message[messageId: %s topic: %s]",
                    messageId, topic));
            if (m_transportListener != null) {
                try {
                    // Notify the transport listener
                    m_transportListener.onMessageConfirmed(token);
                } catch (Throwable t) {
                    s_logger.error("Unexpected Throwable - onMessageConfirmed", t);
                }
            } else {
                s_logger.warn("No registered transport listener. Missing onMessageConfirmed()");
            }
        } else {
            s_logger.debug("Received acknowledgement for a published with no SolaceTransportToken");
        }
    }

    //
    // JCSMPReconnectEventHandler Implementation

    @Override
    public void postReconnect() throws JCSMPException {
        if (m_transportListener != null) {
            try {
            	s_logger.info("postReconnect()");
                m_transportListener.onReconnected();
            } catch (Throwable t) {
                s_logger.error("Unexpected Throwable - onReconnected", t);
            }
        } else {
            s_logger.warn("No registered transport listener. Missing onReconnected()");
        }
    }

    @Override
    public boolean preReconnect() throws JCSMPException {
        boolean continueReconnect = true;
        if (m_transportListener != null) {
            try {
            	s_logger.info("preReconnect()");
                continueReconnect = m_transportListener.onReconnecting();
            } catch (Throwable t) {
                s_logger.error("Unexpected Throwable - preReconnect", t);
            }
        } else {
            s_logger.warn("No registered transport listener. Missing preReconnect() - continuing to reconnect");
        }
        return continueReconnect;
    }
}
