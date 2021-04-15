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

package com.solace.psg.enterprisestats.statspump.containers;

import org.json.JSONArray;

import com.solace.psg.enterprisestats.statspump.StatsPumpMessage;
import com.solace.psg.enterprisestats.statspump.util.StatsPumpConstants;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.SDTException;
import com.solacesystems.jcsmp.SDTStream;
import com.solacesystems.jcsmp.XMLMessage;

public final class ContainerUtils {
    /**
     * Converts "connections-total-smf" to "connectionsTotalSmf"
     */
    public static String getCamelCase(String element) {
        String[] parts = element.split("-");
        StringBuilder sb = new StringBuilder(parts[0]);
        for (int i=1;i<parts.length;i++) {
            sb.append(Character.toUpperCase(parts[i].charAt(0)));
            if (parts[i].length() > 1) sb.append(parts[i].substring(1));
        }
        return sb.toString();
    }

    /**
     * Build a User Property Map, then adds in the SEMP version,
     * <i>doesn't</i> add the topic levels as they might be in different
     * formats, and then timestamps the message as appropriate.
     */
    public static void prepMessage(XMLMessage message, StatsPumpMessage statsMessage) throws SDTException {
        addUserPropertyMap(message);
        if (statsMessage.getType().isSempStatMessage()) addSempVersion(message,statsMessage);
        message.setSenderTimestamp(statsMessage.getTimestamp());
    }
    
    private static void addUserPropertyMap(XMLMessage message) {
        message.setProperties(JCSMPFactory.onlyInstance().createMap());
    }
    
    private static void addSempVersion(XMLMessage jcsmpMessage, StatsPumpMessage statsMessage) throws SDTException {
        jcsmpMessage.getProperties().putString(StatsPumpConstants.SEMP_VERSION_TAG,statsMessage.getPhysicalAppliance().getSempVersion().toString());
    }
    
    public static void addSdtStreamTopicLevels(XMLMessage jcsmpMessage, StatsPumpMessage statsMessage) throws SDTException {
        SDTStream stream = JCSMPFactory.onlyInstance().createStream();
        for (String level : statsMessage.getTopicLevels()) {
            stream.writeString(level);
        }
        jcsmpMessage.getProperties().putStream(StatsPumpConstants.TOPIC_LEVELS_TAG,stream);
    }
    
    public static void addJsonTopicLevels(XMLMessage jcsmpMessage, StatsPumpMessage statsMessage) throws SDTException {
        jcsmpMessage.getProperties().putString(StatsPumpConstants.TOPIC_LEVELS_TAG,new JSONArray(statsMessage.getTopicLevels()).toString());
    }

    public static void addXmlTopicLevels(XMLMessage jcsmpMessage, StatsPumpMessage statsMessage) throws SDTException {
        StringBuilder sb = new StringBuilder("<topic-levels>");
        for (String level : statsMessage.getTopicLevels()) {
            sb.append("<level>").append(level).append("</level>");
        }
        sb.append("</topic-levels>");
        jcsmpMessage.getProperties().putString(StatsPumpConstants.TOPIC_LEVELS_TAG,sb.toString());
    }
    
    private ContainerUtils() {
        throw new AssertionError("Cannot instantiate this utility class");
    }
}
