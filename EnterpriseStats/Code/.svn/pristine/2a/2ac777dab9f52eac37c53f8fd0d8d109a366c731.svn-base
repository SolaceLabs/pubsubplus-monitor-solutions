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
package com.solace.psg.enterprisestats.statspump.pollers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solace.psg.enterprisestats.statspump.MessageBus;
import com.solace.psg.enterprisestats.statspump.PhysicalAppliance;
import com.solace.psg.enterprisestats.statspump.StatsPumpMessage;
import com.solace.psg.enterprisestats.statspump.StatsPumpMessage.MessageType;
import com.solace.psg.enterprisestats.statspump.config.xml.DestinationType;
import com.solace.psg.enterprisestats.statspump.config.xml.RunCondition;
import com.solace.psg.enterprisestats.statspump.containers.SingleContainerSet;
import com.solace.psg.enterprisestats.statspump.semp.GenericSempSaxHandler;
import com.solace.psg.enterprisestats.statspump.semp.SempSaxProcessingListener;
import com.solace.psg.enterprisestats.statspump.tools.PumpConstants;
import com.solace.psg.enterprisestats.statspump.tools.semp.SempReplySchemaLoader;
import com.solace.psg.enterprisestats.statspump.tools.semp.SempRpcSchemaLoader;
import com.solace.psg.enterprisestats.statspump.tools.semp.SempVersion;
import com.solace.psg.enterprisestats.statspump.util.StatsPumpConstants;

/**
 * This class is used to construct the various Poller objects loaded from
 * a configuration file.  Due to the number of constructor variables, it uses
 * a Builder object to pass to the constructor.  It also parses the provided
 * topic string for replaceable elements.
 */
public class GenericPoller implements Poller {

    public static final String VPN_NAME = "VPN_NAME";
    public static final String TOPIC_STRING_ROUTER_NAME = "~ROUTER_NAME~";
    public static final String TOPIC_STRING_VPN_NAME = "~VPN_NAME~";

    public static class Builder {
        private String b_name;
        private Scope b_scope = null;
        private String b_description;
        private String b_sempRequest;
        private RunCondition b_runOnPrimaryWhenActiveStandby = null;
        private RunCondition b_runOnBackupWhenActiveStandby = null;
        private RunCondition b_runOnPrimaryWhenActiveActive = null;
        private RunCondition b_runOnBackupWhenActiveActive = null;
        private DestinationType b_destination = null;
        private String b_topicStringSuffix;
        private String b_baseTag;
        private final Map<String,String> b_objectTags = new HashMap<String,String>();
        private String b_minSempVersion = null;
        private String b_maxSempVersion = null;
        
        public Builder() { }
        
        public Builder setName(String name) {                             this.b_name = name; return this; }
        public Builder setScope(Scope scope) {                        this.b_scope = scope; return this; }
        public Builder setDescription(String description) {               this.b_description = description; return this; }
        public Builder setSempRequest(String sempRequest) {               this.b_sempRequest = sempRequest; return this; }
        public Builder setRunConditionOnPrimaryWhenAS(RunCondition run) { this.b_runOnPrimaryWhenActiveStandby = run; return this; }
        public Builder setRunConditionOnBackupWhenAS(RunCondition run) {  this.b_runOnBackupWhenActiveStandby = run; return this; }
        public Builder setRunConditionOnPrimaryWhenAA(RunCondition run) { this.b_runOnPrimaryWhenActiveActive = run; return this; }
        public Builder setRunConditionOnBackupWhenAA(RunCondition run) {  this.b_runOnBackupWhenActiveActive = run; return this; }
        public Builder setDestination(DestinationType destinationType) {  this.b_destination = destinationType; return this; }
        public Builder setTopicStringSuffix(String topicStringSuffix) {   this.b_topicStringSuffix = topicStringSuffix; return this; }
        public Builder setBaseTag(String baseTag) {                       this.b_baseTag = baseTag; return this; }
        public Builder setVpnNameTag(String vpnNameTag) {                 this.b_objectTags.put(VPN_NAME,vpnNameTag); return this; }
        public Builder addObjectTag(String objectName, String tag) {      this.b_objectTags.put(objectName, tag); return this; }
        public Builder setMinSempVersion(String sempVersion) {            this.b_minSempVersion = sempVersion; return this; }
        public Builder setMaxSempVersion(String sempVersion) {            this.b_maxSempVersion = sempVersion; return this; }
        
        /**
         * Will throw if anything in the Poller doesn't jive
         * @throws IllegalArgumentException
         */
        public void verify() throws IllegalArgumentException {
            if (b_name == null || b_name.isEmpty()) {
                throw new IllegalArgumentException("name can't be empty or null");
            } else if (b_scope == null) {
                throw new IllegalArgumentException(String.format("scope in poller builder '%s' can't be null",b_name));
            } else if (b_description == null || b_description.isEmpty()) {
                throw new IllegalArgumentException(String.format("description in poller builder '%s' can't be empty or null",b_name));
            } else if (b_sempRequest == null || b_sempRequest.isEmpty()) {
                throw new IllegalArgumentException(String.format("SEMP request in Poller builder '%s' can't be empty or null",b_name));
            } else if (b_runOnPrimaryWhenActiveStandby == null) {
                throw new IllegalArgumentException(String.format("Need to define RunCondition on primary appliance when using as Active/Standby in poller builder '%s'",b_name));
            } else if (b_runOnBackupWhenActiveStandby == null) {
                throw new IllegalArgumentException(String.format("Need to define RunCondition on backup appliance when using as Active/Standby in poller builder '%s'",b_name));
            } else if (b_runOnPrimaryWhenActiveActive == null) {
                throw new IllegalArgumentException(String.format("Need to define RunCondition on primary appliance when using as Active/Active in poller builder '%s'",b_name));
            } else if (b_runOnBackupWhenActiveActive == null) {
                throw new IllegalArgumentException(String.format("Need to define RunCondition on backup appliance when using as Active/Active in poller builder '%s'",b_name));
            } else if (b_destination == null) {
                throw new IllegalArgumentException(String.format("destination in Poller builder '%s' can't be null",b_name));
            } else if (b_topicStringSuffix == null || b_topicStringSuffix.isEmpty()) {
                throw new IllegalArgumentException(String.format("topic string in Poller builder '%s' can't be null or empty",b_name));
            } else if (b_topicStringSuffix.contains("//")) {
                throw new IllegalArgumentException(String.format("topic string in Poller builder '%s' cannot contain any empty levels: '%s'",b_name,b_topicStringSuffix));
            } else if (b_baseTag == null || b_baseTag.isEmpty()) {
                throw new IllegalArgumentException(String.format("base tag in Poller builder '%s' can't be empty or null",b_name));
            }
            if (!b_objectTags.containsKey(VPN_NAME) || b_objectTags.get(VPN_NAME) == null || b_objectTags.get(VPN_NAME).isEmpty()) {
                if (b_scope == Scope.VPN) {
                    throw new IllegalArgumentException(String.format("VPN name tag in Poller builder '%s' can't be empty or null",b_name));
                }
            } else if (b_scope != Scope.VPN) {
                throw new IllegalArgumentException(String.format("VPN name in Poller builder '%s' not applicable for this type of stat poller",b_name));
            }
            for (String objectName : b_objectTags.keySet()) {
                if (SempReplySchemaLoader.getSchemaVersionsForTag(b_baseTag+b_objectTags.get(objectName)).size() == 0) {
                    //TODO DONE! fix this -- need to account for * in XML tag level, and %s in semp version
                    //throw new IllegalArgumentException(String.format("%s tag '%s' in Poller '%s' does not appear in any SEMP reply schema definitions when requesting: %s",objectName,b_baseTag+b_objectTags.get(objectName),b_name,b_sempRequest));
                    logger.warn(String.format("%s tag '%s' in Poller '%s' does not appear in any SEMP reply schema definitions when requesting: %s",objectName,b_baseTag+b_objectTags.get(objectName),b_name,b_sempRequest));
                }
            }
            if (b_minSempVersion == null) {
                SempVersion minVer = SempRpcSchemaLoader.INSTANCE.findMinSempVersion(b_sempRequest);
                if (minVer == null) {
                    throw new IllegalArgumentException(String.format("SEMP request '%s' does not validate on any known SEMP RPC schema",b_sempRequest));
                }
                b_minSempVersion = minVer.toString();
            }
        }
    }
    
    
    /* Actual Poller class now *******************************/

    private final String name;
    private final Scope scope;
    private final String description;
    private final String sempRequest;
    private final RunCondition runOnPrimaryWhenActiveStandby;
    private final RunCondition runOnBackupWhenActiveStandby;
    private final RunCondition runOnPrimaryWhenActiveActive;
    private final RunCondition runOnBackupWhenActiveActive;
    private final DestinationType destination;
    private final String topicStringSuffix;
    private final String baseTag;
    private final Map<String,String> objectTags;
    private final SempVersion minSempVersion;
    private final SempVersion maxSempVersion;
    private final String[] topicLevels;  // defined in constructor, not passed in by the Builder
    private final boolean[] levelNeedsReplacement;  // defined in constructor, not passed in by the Builder
    
    private static final Logger logger = LoggerFactory.getLogger(GenericPoller.class);
    
    public GenericPoller(Builder b) throws IllegalArgumentException {
        b.verify();
        // we can now trust all these values
        this.name = b.b_name;
        this.description = b.b_description;
        this.scope = b.b_scope;
        this.sempRequest = b.b_sempRequest;
        this.runOnPrimaryWhenActiveStandby = b.b_runOnPrimaryWhenActiveStandby;
        this.runOnBackupWhenActiveStandby = b.b_runOnBackupWhenActiveStandby;
        this.runOnPrimaryWhenActiveActive = b.b_runOnPrimaryWhenActiveActive;
        this.runOnBackupWhenActiveActive = b.b_runOnBackupWhenActiveActive;
        this.destination = b.b_destination;
        this.topicStringSuffix = b.b_topicStringSuffix;
        this.baseTag = b.b_baseTag;
        this.objectTags = Collections.unmodifiableMap(b.b_objectTags);
        this.minSempVersion = b.b_minSempVersion == null ? null : new SempVersion(b.b_minSempVersion);
        this.maxSempVersion = b.b_maxSempVersion == null ? null : new SempVersion(b.b_maxSempVersion);
        // to make sure nothing has changed underneath
        b.verify();
        // now for some processing to aid topic string token replacement...
        // e.g. "MSG-SPOOL/QUEUE/~QUEUE_NAME~" --> ["MSG-SPOOL/QUEUE", "~QUEUE_NAME~"]
        // first, split the supplied topic string on the delimiteres
        String[] temp_splitArray = this.topicStringSuffix.split("/");
        List<String> temp_topicStringLevels = new ArrayList<String>();
        for (int i=0;i<temp_splitArray.length;i++) {            // for each level
            if (temp_splitArray[i].contains("~")) {             // see if contains a tilde
                if (!temp_splitArray[i].matches("~[^~]+~")) {   // make sure it's formatted correctly (start/end with tilde)
                    throw new IllegalArgumentException("Invalid format for topic string... can have at most exactly one replacement token per level");
                } else {
                    temp_topicStringLevels.add(temp_splitArray[i]);  // add this replaceable part to the levels
                }
            } else if (!temp_splitArray[i].isEmpty()) {  // the verify method in the builder should ensure this
                // else no tilde, no replacement token... append it to whatever we have so far (if anything)
                temp_topicStringLevels.add(temp_splitArray[i]);  // add whatever it is to the levels
            }
        }
        // done pre-processing... did some verification, but didn't collapse non-replacing topic levels together (as in comments below)... make final arrays
        topicLevels = new String[temp_topicStringLevels.size()];  // since var is final, had to wait till we knew how big the array was
        levelNeedsReplacement = new boolean[temp_topicStringLevels.size()];  // whether a particular level has a tilde
        for (int i=0;i<temp_topicStringLevels.size();i++) {
            String level = temp_topicStringLevels.get(i);
            if (level.contains("~")) {
                levelNeedsReplacement[i] = true;
                topicLevels[i] = level.replaceAll("~","");  // drop the tilde so we can do search-replace later
            } else {
                levelNeedsReplacement[i] = false;
                topicLevels[i] = level;
            }
        }
        // load in the various object tags (if any)
        for (String tag : objectTags.values()) {
            if (!tag.startsWith("/") || tag.endsWith("/")) {
                throw new IllegalArgumentException(String.format("Invalid object tag format on %s... must start with / and not end with one: %s",this,objectTags));
            }
        }
        // double-check that any replaceable levels actually have a corresponding object tag
        for (int i=0;i<topicLevels.length;i++) {
            if (levelNeedsReplacement[i] && !objectTags.containsKey(topicLevels[i])) {
                throw new IllegalArgumentException("Missing a object tag for topic replacement token "+topicLevels[i]);
            }
        }
        logger.info("Built " + pollerAsString());
    }
    
    private String pollerAsString() {
        StringBuilder sb = new StringBuilder(this.getClass().getSimpleName()).append(":");
        sb.append(String.format("%n\tName: '%s'%n",name));
        sb.append(String.format("\tScope: '%s'%n",scope));
        sb.append(String.format("\tDescription: '%s'%n",description));
        sb.append(String.format("\tSEMP Request: '%s'%n",getSempRequestCompressed()));
        //        sb.append(String.format("\tIs Grouped: '%b'%n",isGrouped));
        sb.append(String.format("\tRun on Primary when A/S: '%s'%n",runOnPrimaryWhenActiveStandby));
        sb.append(String.format("\tRun on Backup when A/S: '%s'%n",runOnBackupWhenActiveStandby));
        sb.append(String.format("\tRun on Primary when A/A: '%s'%n",runOnPrimaryWhenActiveActive));
        sb.append(String.format("\tRun on Backup when A/A: '%s'%n",runOnBackupWhenActiveActive));
        sb.append(String.format("\tDestination: '%s'%n",destination));
        sb.append(String.format("\tTopic String suffix: '%s'%n",topicStringSuffix));
        sb.append(String.format("\tBase Tag: '%s'%n",baseTag));
        sb.append(String.format("\tObject Tags: '%s'%n",objectTags));
        sb.append(String.format("\tMinimum SEMP version: '%s'%n",minSempVersion));
        sb.append(String.format("\tMaximum SEMP version: '%s'%n",maxSempVersion));
        sb.append(String.format("\tTopic Levels Array: '%s'%n",Arrays.toString(topicLevels)));
        sb.append(String.format("\tLevels need replacement Array: '%s'%n",Arrays.toString(levelNeedsReplacement)));
        return sb.toString();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Scope getScope() {
        return scope;
    }
    
    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getSempRequest() {
        return sempRequest;
    }
    
    @Override
    public String getSempRequestCompressed() {
        // remove all whitespace, except the one between immediately
        // before semp-version via a non-capturing negative lookahead
        return sempRequest.replaceAll("\\s(?!semp-version)","");
    }

    @Override
    public RunCondition runOnPrimaryWhenActiveStandby() {
        return runOnPrimaryWhenActiveStandby;
    }
    
    @Override
    public RunCondition runOnBackupWhenActiveStandby() {
        return runOnBackupWhenActiveStandby;
    }

    @Override
    public RunCondition runOnPrimaryWhenActiveActive() {
        return runOnPrimaryWhenActiveActive;
    }

    @Override
    public RunCondition runOnBackupWhenActiveActive() {
        return runOnBackupWhenActiveActive;
    }

    @Override
    public DestinationType getDestination() {
        return destination;
    }

    @Override
    public String getBaseTag() {
        return baseTag;
    }
    
    @Override
    public Map<String, String> getObjectTags() {
        return objectTags;
    }

    @Override
    public SempVersion getMinSempVersion() {
        return minSempVersion;
    }

    @Override
    public SempVersion getMaxSempVersion() {
        return maxSempVersion;
    }

    @Override
    public String getTopicStringForBroadcast() {
        StringBuilder sb = new StringBuilder(StatsPumpConstants.TOPIC_STRING_PREFIX);
        if (scope == Scope.SYSTEM) {
            sb.append(MessageType.SYSTEM).append(TOPIC_STRING_ROUTER_NAME);  // the toString includes leading and trailing /
        } else {
            sb.append(MessageType.VPN).append(TOPIC_STRING_ROUTER_NAME).append('/').append(TOPIC_STRING_VPN_NAME);
        }
        for (int i=0;i<topicLevels.length;i++) {
            sb.append('/');
            if (levelNeedsReplacement[i]) sb.append('~').append(topicLevels[i]).append('~');
            else sb.append(topicLevels[i]);
        }
        return sb.toString();
    }
    
    @Override
    public List<String> getTopicLevelsForBroadcast() {
        List<String> list = new ArrayList<String>();
        list.add(StatsPumpConstants.TOPIC_STRING_PREFIX);
        if (scope == Scope.SYSTEM) {
            list.add(MessageType.SYSTEM.name());
        } else {
            list.add(MessageType.VPN.name());
        }
        list.add(TOPIC_STRING_ROUTER_NAME);
        if (scope == Scope.VPN) {
            list.add(TOPIC_STRING_VPN_NAME);
        }
        for (int i=0;i<topicLevels.length;i++) {
            if (levelNeedsReplacement[i]) list.add(new StringBuilder().append('~').append(topicLevels[i]).append('~').toString());
            else list.add(topicLevels[i]);
        }
        return list;
    }
    
    @Override
    public GenericSempSaxHandler buildSaxHandler(PhysicalAppliance appliance) {
        return buildSaxHandler(appliance,new GenericSempReplyParserListener(appliance));
    }
    
    @Override
    public GenericSempSaxHandler buildSaxHandler(PhysicalAppliance appliance, SempSaxProcessingListener listener) {
        return new GenericSempSaxHandler(this,appliance,listener,objectTags);
    }
    
    /** Not part of the Poller interface... used by other classes though */
    public String[] getTopicLevels() {
        return topicLevels;
    }
    
    /** Not part of the Poller interface... used by other classes though */
    public boolean[] getTopicLevelReplacements() {
        return levelNeedsReplacement;
    }
    
    
    /********************************************************
     * This guy is used to catch callbacks during the parsing of the XML document.
     * Non-generic Pollers should override these methods to perform specefic functions,
     * like keeping a list of various values that it has seen, or looking for things that
     * are toggled on and off.
     */
    public class GenericSempReplyParserListener implements SempSaxProcessingListener {
        
        protected PhysicalAppliance physical;
        protected long timestamp;
        private boolean cannotEnqueueFlag = false;
        private boolean invalidMessageFlag = false;
        
        protected Set<String> vpns = new HashSet<String>();
        protected String curVpn = "";
        
        protected GenericSempReplyParserListener(PhysicalAppliance physical) {
            this.physical = physical;
        }
        
        protected List<String> buildPartialTopicLevels(Map<String,String> objectValuesMap) {
            List<String> list = new ArrayList<String>();
            for (int i=0;i<topicLevels.length;i++) {
                if (levelNeedsReplacement[i]) list.add(objectValuesMap.get(topicLevels[i]));
                else list.add(topicLevels[i]);
            }
            return list;
        }
        
        @Override
        public void onStartSempReply(String sempVersion, long timestamp) {
            this.timestamp = timestamp;
            physical.setSempVersion(sempVersion);
        }

        protected void sendMsgToBus(MessageBus msgBus, StatsPumpMessage msg) {
            if (!msgBus.enqueueStatMessage(msg)) {
                if (!cannotEnqueueFlag) {  // so we only report once per poll interval
                    logger.info("Could not enqueue a message on "+msgBus+": "+msg.toString());
                    cannotEnqueueFlag = true;
                }
            }
        }

        @Override
        public void onStatMessage(SingleContainerSet containerSet, Map<String,String> objectValuesMap) {
            List<String> topicList = buildPartialTopicLevels(objectValuesMap);
            if (topicList.contains(PumpConstants.UNINITIALIZED_VALUE) || topicList.contains("")) {
            	// means that one of the values we needed for search & replace didn't exist
            	// this happens with 'show client * connections wide', SEMP returns empty <client> elements
            	// or it could be an empty level, like with 'show interface' on the VMR, all the non-used interfaces have a blank name
            	// log at debug if really interested, but not super critical as we know this happens
                if (!invalidMessageFlag) {
                    invalidMessageFlag = true;  // log only once per poller thing
                    logger.debug(String.format("Found a stat message without all topic levels replaced: '%s': %s",topicList,objectValuesMap));
                }
                // don't send the message
                return;
            }
            StatsPumpMessage msg = null;
            try {
                switch (scope) {
                    case SYSTEM:
                        msg = new StatsPumpMessage(MessageType.SYSTEM,physical,destination,containerSet,topicList,timestamp);
                        break;
                    case VPN:
                        msg = new StatsPumpMessage(MessageType.VPN,physical,objectValuesMap.get(VPN_NAME),destination,containerSet,topicList,timestamp);
                        if (curVpn.isEmpty()) {
                            vpns.add(objectValuesMap.get(VPN_NAME));
                            curVpn = objectValuesMap.get(VPN_NAME);
                        } else if (!curVpn.equals(objectValuesMap.get(VPN_NAME))) {  // doesn't match, different VPN than the previous object
                            if (vpns.contains(objectValuesMap.get(VPN_NAME))) {  // not good, have already seen this!
                                logger.debug("on <this> poller on <this> host I saw <this VPN before and now again");
                                
                            }
                        }
                        break;
                    default:
                        logger.error("Have encountered an unknown Poller Scope Type: "+scope.name());
                        throw new AssertionError("Have found some new Poller enum type: "+scope.name());
                }
                for (MessageBus msgBus : physical.getLogical().getMsgBuses(destination)) {  // be it SELF, BOTH, or MGMT
                    sendMsgToBus(msgBus, msg);
                }
            } catch (RuntimeException e) {  // had a problem creating the message, or sending it!
                if (!invalidMessageFlag) {
                    invalidMessageFlag = true;  // log only once per poller thing
                    logger.info(String.format("Had a problem creating a message or sending to topic '%s': %s",topicList,objectValuesMap),e);
                }
            }
        }
        
        @Override
        public void onEndSempReply() {
            // no-op - subclasses can override this if they wish
        }
    }
    
    @Override
    public String toString() {
       return String.format("Poller '%s'",getName());
    }
}
