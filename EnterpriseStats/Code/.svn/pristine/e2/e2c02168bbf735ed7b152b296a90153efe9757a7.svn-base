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
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solace.psg.enterprisestats.statspump.config.xml.DestinationType;
import com.solace.psg.enterprisestats.statspump.containers.Container;
import com.solace.psg.enterprisestats.statspump.containers.ContainerFactory;
import com.solace.psg.enterprisestats.statspump.util.StatsPumpConstants;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.Topic;

public class StatsPumpMessage {
    private static final Logger logger = LoggerFactory.getLogger(StatsPumpMessage.class);

    /**
     * One of: SYSTEM, VPN, BROADCAST, POLLER_STATS.<br>
     * The toSring() method returns one of: /SYSTEM/,/VPN/, etc.<br>
     * The name() method will still return SYSTEM, VPN, etc.
     */
    public enum MessageType {

        SYSTEM, VPN, BROADCAST, PUMP,;

        final String msgTypeName;

        MessageType() {
            msgTypeName = "/" + this.name() + "/";
        }

        @Override
        public String toString() {
            return msgTypeName;
        }

        /**
         * Is this message of type SYSTEM or VPN; that is, is it derived from a
         * SEMP request. It is not a artificially created BROADCAST message or
         * other.
         */
        public boolean isSempStatMessage() {
            return (this == SYSTEM || this == VPN);
        }
    }

    private final MessageType type;
    private final PhysicalAppliance physical;
    private final String vpn;
    private final DestinationType destination;
    private final Map<ContainerFactory, ? extends Container> containers;
    private final List<String> topicLevels;
    private final String topic;
    private final Topic jcsmpTopic;
    private final long timestamp;

    /**
     * Will throw an Illegal argument exception if this message can't be created
     * for whatever reason (e.g. invalid topic name)
     * 
     * @throws IllegalArgumentException
     */
    public StatsPumpMessage(MessageType type, PhysicalAppliance physical, DestinationType destination,
            Map<ContainerFactory, ? extends Container> containers, List<String> topicSuffix, long timestamp)
            throws IllegalArgumentException {
        this(type, physical, null, destination, containers, topicSuffix, timestamp);
    }

    /**
     * Will throw an Illegal argument exception if this message can't be created
     * for whatever reason (e.g. invalid topic name)
     * 
     * @throws IllegalArgumentException
     */
    public StatsPumpMessage(MessageType type, PhysicalAppliance physical, String vpn, DestinationType destination,
            Map<ContainerFactory, ? extends Container> containers, List<String> topicSuffix, long timestamp)
            throws IllegalArgumentException {
        try {
            this.type = type;
            assert this.type != null;
            this.physical = physical;
            assert this.physical != null;
            if (type == MessageType.BROADCAST && destination == DestinationType.SELF) {
                // so, a broadcast message for a SELF destination (poller
                // probably)... send to BOTH, not just SELF
                this.destination = DestinationType.BOTH;
            } else {
                this.destination = destination;
            }
            assert this.destination != null;
            this.vpn = vpn;
            if (this.type == MessageType.VPN && this.vpn == null) {
                // so if it's a VPN message, then vpn can't be null
                throw new IllegalArgumentException("Seem to have a VPN stats message with no VPN defined");
            } else if (this.type != MessageType.VPN && this.vpn != null) {
                // so this is NOT a VPN message, and somehow VPN is defined..?
                throw new IllegalArgumentException("Seem to have a non-VPN stats message with a VPN defined?");
            }
            // can't have that... at best, BOTH with VPN null means will get
            // sent to all VPNs configured for this router mgmt and self
            // msg-buses
            assert !(this.vpn == null && this.destination == DestinationType.SELF);
            this.containers = containers;
            assert this.containers != null;
            assert !this.containers.isEmpty();
        	if (containers.size() == 0) {
        		throw new AssertionError("The Containers collection is empty.");
        	}

            this.timestamp = timestamp;
            assert topicSuffix != null;
            // assert !topicSuffix.isEmpty();
            topicLevels = completeTopic(topicSuffix);
            topic = buildTopic();
            // will throw an exception if a problem
            jcsmpTopic = JCSMPFactory.onlyInstance().createTopic(topic);
        } catch (AssertionError e) {
            logger.error(String.format(
                    "Caught creating a message: Type=%s, Physical Appliance=%s, Destination=%s, VPN=%s, TopicSuffix=%s, Containers=%s",
                    type, physical, destination, vpn, topicSuffix, containers), e);
            throw e;
        }
    }

    public MessageType getType() {
        return type;
    }

    public LogicalAppliance getLogicalAppliance() {
        return physical.getLogical();
    }

    public PhysicalAppliance getPhysicalAppliance() {
        return physical;
    }

    public String getVpn() {
        return vpn;
    }

    public Map<ContainerFactory, ? extends Container> getContainers() {
        return containers;
    }

    public DestinationType getDestination() {
        return destination;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        // boolean isMgmtVpn = vpn == null;
        // return String.format("Message on topic '%s' for %s for
        // '%s'",getTopic(),isMgmtVpn ? "*MGMT*" : String.format("VPN
        // '%s'",vpn),router);
        return String.format("Message on topic '%s'", getTopic());
    }

    public String dump() {
        return String.format("%s %s:%n\tRouter='%s'%n\tVPN='%s'%n\tDestination='%s'%n\tTopic='%s'%n\tContainers='%s'",
                type.name(), this.getClass().getSimpleName(), physical, vpn, destination, topic, containers);
    }

    /**
     * Method to construct the topic used for this message. If the topic is
     * longer than 'MAX_TOPIC_LENGTH' (250), than it will be truncated.
     * 
     * @return a topic string for this message of max-length 250
     */
    private List<String> completeTopic(List<String> topicSuffix) {
        List<String> topicLevels = new ArrayList<String>();
        topicLevels.add(StatsPumpConstants.TOPIC_STRING_PREFIX);
        topicLevels.add(type.name());
        topicLevels.add(physical.getHostname());
        switch (type) {
        case VPN:
            topicLevels.add(vpn);
        default:
            break; // i.e. only add the VPN name if this is a VPN message
        }
        topicLevels.addAll(topicSuffix);
        // logger.debug(topicLevels);
        return topicLevels;
    }

    /**
     */
    private String buildTopic() {
        StringBuilder sb = new StringBuilder(256);
        sb.append(StatsPumpConstants.TOPIC_STRING_PREFIX);
        for (int i = 1; i < topicLevels.size(); i++) {
            sb.append('/').append(topicLevels.get(i));
        }
        return sb.toString().substring(0, Math.min(sb.toString().length(), StatsPumpConstants.MAX_TOPIC_LENGTH));
    }

    public List<String> getTopicLevels() {
        return topicLevels;
    }

    public String getTopic() {
        return topic;
    }

    public Topic getJcsmpTopic() {
        return jcsmpTopic;
    }

    public BytesXMLMessage buildJcsmpMessage(ContainerFactory factory) throws JCSMPException {
        // what kind of message do we need... depends on the container
    	if (containers == null) {
    		throw new AssertionError("The Containers collection is null.");
    	}
    	if (containers.size() == 0) {
    		throw new AssertionError("The Containers collection is empty.");
    	}
    	
    	Container theContainer = containers.get(factory);
    	if (theContainer == null) {
    		throw new AssertionError("theContainer is null.");
    	}
    	
        return theContainer.buildJcsmpMessage(this);
    }
}
