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
package com.solace.psg.enterprisestats.statspump.util;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solace.psg.enterprisestats.statspump.LogicalAppliance;
import com.solace.psg.enterprisestats.statspump.LogicalAppliance.Type;
import com.solace.psg.enterprisestats.statspump.MessageBus;
import com.solace.psg.enterprisestats.statspump.PhysicalAppliance;
import com.solace.psg.enterprisestats.statspump.StatsPumpMessage;
import com.solace.psg.enterprisestats.statspump.StatsPumpMessage.MessageType;
import com.solace.psg.enterprisestats.statspump.config.xml.DestinationType;
import com.solace.psg.enterprisestats.statspump.config.xml.RunCondition;
import com.solace.psg.enterprisestats.statspump.containers.ContainerFactory;
import com.solace.psg.enterprisestats.statspump.containers.SingleContainer;
import com.solace.psg.enterprisestats.statspump.containers.SingleContainerSet;
import com.solace.psg.enterprisestats.statspump.pollers.Poller;
import com.solace.psg.enterprisestats.statspump.tools.semp.XsdDataType;
import com.solace.psg.enterprisestats.statspump.utils.PayloadUtils;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.MapMessage;
import com.solacesystems.jcsmp.Message;
import com.solacesystems.jcsmp.SDTMap;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.XMLContentMessage;

public final class MessageUtils {
    private static final Logger logger = LoggerFactory.getLogger(MessageUtils.class);

    @SuppressWarnings("unchecked")
    public static void containerAddList(SingleContainer singleContainer, String key, List<? extends Object> list) {
        int size = list.size();
        Object firstElement = list.get(0);
        if (firstElement instanceof Long) {
            for (int i=0;i<size;i++) { singleContainer.put(XsdDataType.LONG,    key, list.get(i), true); }
        } else if (firstElement instanceof Integer) {
            for (int i=0;i<size;i++) { singleContainer.put(XsdDataType.INT,     key, list.get(i), true); }
        } else if (firstElement instanceof Short) {
            for (int i=0;i<size;i++) { singleContainer.put(XsdDataType.SHORT,   key, list.get(i), true); }
        } else if (firstElement instanceof Byte) {
            for (int i=0;i<size;i++) { singleContainer.put(XsdDataType.BYTE,    key, list.get(i), true); }
        } else if (firstElement instanceof Float) {
            for (int i=0;i<size;i++) { singleContainer.put(XsdDataType.FLOAT,   key, list.get(i), true); }
        } else if (firstElement instanceof Double) {
            for (int i=0;i<size;i++) { singleContainer.put(XsdDataType.DOUBLE,  key, list.get(i), true); }
        } else if (firstElement instanceof Boolean) {
            for (int i=0;i<size;i++) { singleContainer.put(XsdDataType.BOOLEAN, key, list.get(i), true); }
        } else if (firstElement instanceof Map<?,?>) { 
            for (int i=0;i<size;i++) {
                Map<?,?> mapElementI = ((Map<?,?>)(list.get(i)));
                if (!mapElementI.isEmpty()) {
                    singleContainer.startNestedElement(key,true);
                    containerAddMap(singleContainer,(Map<String,?>)mapElementI.get(key));
                    singleContainer.closeNestedElement(key);
                }
            }
        } else if (firstElement instanceof List<?>) {
            for (int i=0;i<size;i++) {
                if (!((List<?>)list.get(i)).isEmpty()) {
                    containerAddList(singleContainer,key,(List<?>)list.get(i));
                }
            }
        } else {
            // everything else!
            for (int i=0;i<size;i++) { singleContainer.put(XsdDataType.STRING,key,list.get(i),true); }
        }
    }
        
    @SuppressWarnings("unchecked")
    public static void containerAddMap(SingleContainer singleContainer, Map<String,? extends Object> map) {
        for (String key : map.keySet()) {
            if (map.get(key) instanceof Long)          singleContainer.put(XsdDataType.LONG,   key, map.get(key),false);
            else if (map.get(key) instanceof Integer)  singleContainer.put(XsdDataType.INT,    key, map.get(key),false);
            else if (map.get(key) instanceof Short)    singleContainer.put(XsdDataType.SHORT,  key, map.get(key),false);
            else if (map.get(key) instanceof Byte)     singleContainer.put(XsdDataType.BYTE,   key, map.get(key),false);
            else if (map.get(key) instanceof Float)    singleContainer.put(XsdDataType.FLOAT,  key, map.get(key),false);
            else if (map.get(key) instanceof Double)   singleContainer.put(XsdDataType.DOUBLE, key, map.get(key),false);
            else if (map.get(key) instanceof Boolean)  singleContainer.put(XsdDataType.BOOLEAN,key, map.get(key),false);
            else if (map.get(key) instanceof Map<?,?> && !((Map<?,?>)map.get(key)).isEmpty()) {
                singleContainer.startNestedElement(key,false);
                containerAddMap(singleContainer,(Map<String,Object>)map.get(key));
                singleContainer.closeNestedElement(key);
            } else if (map.get(key) instanceof List<?> && !((List<?>)map.get(key)).isEmpty()) {
                containerAddList(singleContainer, key, (List<?>)map.get(key));
            }
            // everything else!
            else singleContainer.put(XsdDataType.STRING,key,map.get(key),false);  // if all else fails, put a string
        }
    }

    /**
     * Data types supported in the Map are:<ul>
     * <li>long</li>
     * <li>int</li>
     * <li>short</li>
     * <li>byte</li>
     * <li>float</li>
     * <li>double</li>
     * <li>boolean</li>
     * <li>String</li>
     * <li>Map&lt;String,Object&gt;</li>
     * <li>List&lt;Object&gt;</li>
     * </ul>
     * All other types in the map will be converted to Strings with Object.toString().
     */
    public static void publishMapMessage(MessageType type, PhysicalAppliance physical, DestinationType destination, Map<String,? extends Object> map, String... restOfTopic) {
    	LogicalAppliance logic = physical.getLogical();
        SingleContainerSet containerSet = new SingleContainerSet(logic.getContainerFactories(destination));
        for (ContainerFactory factory : containerSet.keySet()) {
            SingleContainer statsMsgContainer = factory.newSingleContainer();
            containerAddMap(statsMsgContainer,map);
            containerSet.put(factory,statsMsgContainer);
        }
        if (containerSet.size() == 0) {
        	throw new AssertionError("The Containers collection is empty.");
        }
        StatsPumpMessage msg = new StatsPumpMessage(type,physical,destination,containerSet,Arrays.asList(restOfTopic),System.currentTimeMillis());
        for (MessageBus msgBus : physical.getLogical().getMsgBuses(destination)) {
            if (!msgBus.enqueueStatMessage(msg)) {
                logger.warn("Couldn't push message onto broadcast topic: "+msg.dump());
            }
        }
    }

    public static void publishListMessage(MessageType type, PhysicalAppliance physical, DestinationType destination, String key, List<? extends Object> list, String restOfTopic) {
        SingleContainerSet containerSet = new SingleContainerSet(physical.getLogical().getContainerFactories(destination));
        for (ContainerFactory factory : containerSet.keySet()) {
            SingleContainer statsMsgContainer = factory.newSingleContainer();
            containerAddList(statsMsgContainer,key,list);
            containerSet.put(factory,statsMsgContainer);
        }
        StatsPumpMessage msg = new StatsPumpMessage(type,physical,destination,containerSet,Collections.singletonList(restOfTopic),System.currentTimeMillis());
        for (MessageBus msgBus : physical.getLogical().getMsgBuses(destination)) {
            if (!msgBus.enqueueStatMessage(msg)) {
                logger.warn("Couldn't push message onto broadcast topic: "+msg.dump());
            }
        }
    }

    private static String runConditionToString(RunCondition condition) {
        switch (condition) {
            case AD_ACTIVE:            return "When appliance is 'AD-Active' (message spool can only be active on one appliance in an HA-pair)";
            case ALWAYS:               return "Always";
            case BACKUP_LOCAL_ACTIVE:  return "Only when Backup Virtual Router 'Local Active' (i.e. other appliance has failed over to this one)";
            case PRIMARY_LOCAL_ACTIVE: return "When Primary Virtual Router 'Local Active' (i.e. When this appliance is active, normal operating)";
            case NEVER:                return "Never";
            default:
                logger.warn("Inside the default block for RunCondition: "+condition.toString());
                return condition.toString();
        }
    }

    public static void publishAnnounceBroadcastMessage(LogicalAppliance pair) {
        List<String> tips = new ArrayList<String>();
        tips.add("Object names escaped by ~ in topic string will be replaced by actual object names. This can be useful for knowing what to call these variables (e.g. for auto-indexing in a database)");
        tips.add("If configured in an HA-Pair and appliance failover occurs, appliance name in topic will change. Have 2 subscriptions with both appliance names, or use wildcard (e.g. ldnpsol1006*) if applicable.");
        tips.add("Topic names are subject to change during StatsPump development... watch the #STATS/BROADCAST/*/POLLERS topic for changes");
        MessageUtils.publishListMessage(MessageType.BROADCAST,pair.getPrimary(),DestinationType.BOTH,"announcements",tips,StatsPumpConstants.ANNOUNCE_BROADCAST_TOPIC_SUFFIX);
    }

    /**
     * Will publish one for each appliance in the pair
     */
    public static void publishApplianceBroadcastMessage(LogicalAppliance logical) {
        Map<String,String> map = new LinkedHashMap<String,String>();
        PhysicalAppliance physical = logical.getPrimary();
        map.put("Router Hostname",physical.getHostname());
        map.put("Router SEMP Version",physical.getSempVersion().toString());
        map.put("Primary Virtual Router Status",physical.getPrimaryActivity());
        if (logical.getType() == Type.ACTIVE_ACTIVE) {  // otherwise, no need to see the backup router information
            map.put("Backup Virtual Router Status",physical.getBackupActivity());
        }
        map.put("Message Spool Status",physical.getAdActivity());
        map.put("Router Redundancy Role","Primary");
        map.put("Router Pair Configured Name",logical.getName());
        map.put("Router Pair Redundancy Mode",logical.getType().toString());
        if (!logical.isStandalone()) {
            map.put("Mate Router Hostname",logical.getBackup().getHostname());
        }
        MessageUtils.publishMapMessage(MessageType.BROADCAST,physical,DestinationType.BOTH,map,StatsPumpConstants.ROUTER_BROADCAST_TOPIC_SUFFIX);
        if (!logical.isStandalone()) {
            map = new LinkedHashMap<String,String>();
            physical = logical.getBackup();
            map.put("Router Hostname",physical.getHostname());
            map.put("Router SEMP Version",physical.getSempVersion().toString());
            if (logical.getType() == Type.ACTIVE_ACTIVE) {  // otherwise, no need to see the backup router information
                map.put("Primary Virtual Router Status",physical.getPrimaryActivity());
            }
            map.put("Backup Virtual Router Status",physical.getBackupActivity());
            map.put("Message Spool Status",physical.getAdActivity());
            map.put("Router Redundancy Role","Backup");
            map.put("Router Pair Configured Name",logical.getName());
            map.put("Router Pair Redundancy Mode",logical.getType().toString());
            map.put("Mate Router Hostname",logical.getPrimary().getHostname());
            MessageUtils.publishMapMessage(MessageType.BROADCAST,physical,DestinationType.BOTH,map,StatsPumpConstants.ROUTER_BROADCAST_TOPIC_SUFFIX);
        }
    }
    
    /**
     * Note that this method name is looked for in VpnServicePoller!!! DO NOT CHANGE THE NAME OF THIS METHOD!
     * @see VpnServicePoller.runOnPrimaryWhenActiveStandby()
     * @param poller
     * @param logical
     */
    public static void publishPollerBroadcastMessage(Poller poller, LogicalAppliance logical) {
        // TODO update this to print pollers for both routers in the pair
        //logger.debug(String.format(">>> Publishing a broadcast message for %s",this.toString()));
        Map<String,Object> map = new HashMap<String,Object>();
        map.put("name",poller.getName());
        map.put("description",poller.getDescription());
        map.put("topic",poller.getTopicStringForBroadcast());
        map.put("topic-levels",poller.getTopicLevelsForBroadcast());
        if (logical.isStandalone()) map.put("routers",logical.getPrimary().getHostname());
        else map.put("routers",String.format("%s,%s",logical.getPrimary().getHostname(),logical.getBackup().getHostname()));
        //map.put("Scope",poller.getScope());
        map.put("destination",poller.getDestination());
        map.put("frequency_sec",logical.getPollerInterval(poller));
        //map.put("Grouped results",poller.isGrouped());  // pollers don't group, message bus destinations group
        if (logical.getType() == Type.ACTIVE_ACTIVE) {
            map.put("runs-on-primary-appliance",runConditionToString(poller.runOnPrimaryWhenActiveActive()));
            map.put("runs-on-backup-appliance",runConditionToString(poller.runOnBackupWhenActiveActive()));  // backup must exist since A/A
        } else if (logical.getType() == Type.ACTIVE_STANDBY) {  // either standalone or A/S
            map.put("runs-on-primary-appliance",runConditionToString(poller.runOnPrimaryWhenActiveStandby()));
            map.put("runs-on-backup-appliance",runConditionToString(poller.runOnBackupWhenActiveStandby()));
        } else {
            map.put("runs-on-primary-appliance",runConditionToString(poller.runOnPrimaryWhenActiveStandby()));
            map.put("runs-on-backup-appliance","N/A (standalone)");
        }
        map.put("semp-base-tag",poller.getBaseTag());
        if (!poller.getObjectTags().isEmpty()) map.put("object-tags",poller.getObjectTags());
        if (poller.getDestination() == DestinationType.MGMT) {
            MessageUtils.publishMapMessage(MessageType.BROADCAST,logical.getPrimary(),poller.getDestination(),map,StatsPumpConstants.POLLERS_BROADCAST_TOPIC_SUFFIX);
        } else {  // even if it's SELF, publish to all VPNs so the management knows which pollers are running
            MessageUtils.publishMapMessage(MessageType.BROADCAST,logical.getPrimary(),DestinationType.BOTH,map,StatsPumpConstants.POLLERS_BROADCAST_TOPIC_SUFFIX);
        }
        if (!logical.isStandalone()) {
            // TODO why broadcast the same message on both appliance's topics..?
            //MessageUtils.publishMapMessage(MessageType.BROADCAST,logical.getStandby(),poller.getDestination(),map,StatsPumpConstants.POLLERS_BROADCAST_TOPIC_SUFFIX);
        }
    }
    
    public static String prettyPrintHeader(BytesXMLMessage msg) {
        return msg.dump(Message.MSGDUMP_BRIEF);
    }
    
    public static String prettyPrintUserProperies(BytesXMLMessage msg) {
        return PayloadUtils.prettyPrint(msg.getProperties(),0);
    }
    
    public static String prettyPrintPayload(BytesXMLMessage msg) {
        if (msg instanceof TextMessage) {
            // maybe it's JSON?  Otherwise, the JSONObject.getText() will leave as the String, which means prettyPrint won't do anything
            JSONObject o = new JSONObject(((TextMessage)msg).getText());
            return PayloadUtils.prettyPrint(o,2);
        } else if (msg instanceof MapMessage) {
            SDTMap map = ((MapMessage)msg).getMap();
            return PayloadUtils.prettyPrint(map,0);
        } else if (msg instanceof XMLContentMessage) {
            String xmlPayload = ((XMLContentMessage)msg).getXMLContent();
            return PayloadUtils.prettyPrintXml(xmlPayload);
        } else {
            return "Message received of type "+msg.getClass().getSimpleName();
        }
    }

    public static void prettyPrint(BytesXMLMessage msg) {
        StringBuilder sb = new StringBuilder("HEADER:");
        sb.append(prettyPrintHeader(msg));
        if (msg.getProperties() != null) {
            sb.append("\n---------------------------------------------");
            sb.append("\nUSER PROPERTY MAP:\n");
            sb.append(prettyPrintUserProperies(msg));
        }
        sb.append("\n---------------------------------------------");
        sb.append("\nPAYLOAD:\n");
        sb.append(prettyPrintPayload(msg));

        // this next section will never execute due to the XOR
        // however, it was created to help debug the first couple bytes of a TextMessage, as (I believe) they
        // encode the length of the message.  Just leaving this code in here in case I ever need a binary dump again
        ByteBuffer bb = ((BytesXMLMessage)msg).getAttachmentByteBuffer();
        if (bb != null ^ bb != null) {  // adding in the XOR itself means this will never get executed
            sb.append("\n");
            int size = ((BytesXMLMessage)msg).getAttachmentContentLength();
            byte[] ba = new byte[Math.min(size,16*2)];  // 8 rows
            bb.get(ba,0,ba.length);
            for (int i=0;i<ba.length;i++) {
                if (ba[i] >= 0) {  // print in hex
                    if (ba[i] < 32) {  // 0x20 i.e. control chars
                        sb.append("\n");
                        sb.append(String.format("'?'|0x%02x, ",(int)ba[i]));
                    } else {
                        sb.append("\n");
                        sb.append(String.format("'%c'|0x%02x, ",(char)ba[i],(int)ba[i]));
                    }
                } else {  // print in regular decimal with negative sign
                    sb.append("\n");
                    sb.append(String.format("'%c'|% 4d, ",(char)ba[i],(int)ba[i]));
                }
                if (i % 16 == 15) sb.append("\n");;
            }
            sb.append("\n");;
        }
        
        sb.append("\n");
        sb.append("\n===============================================================================\n");
        logger.debug(sb.toString());
    }
}
