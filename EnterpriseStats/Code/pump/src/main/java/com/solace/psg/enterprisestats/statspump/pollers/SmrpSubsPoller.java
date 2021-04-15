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

package com.solace.psg.enterprisestats.statspump.pollers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solace.psg.enterprisestats.statspump.PhysicalAppliance;
import com.solace.psg.enterprisestats.statspump.config.xml.DestinationType;
import com.solace.psg.enterprisestats.statspump.config.xml.RunCondition;
import com.solace.psg.enterprisestats.statspump.containers.SingleContainerSet;
import com.solace.psg.enterprisestats.statspump.semp.GenericSempSaxHandler;
import com.solace.psg.enterprisestats.statspump.semp.SempSaxProcessingListener;

public class SmrpSubsPoller extends GenericPoller {

    private static final String DESTINATION_NAME = "DESTINATION_NAME";  // the client name who is subscribed to this topic
    private static final String TOPIC = "TOPIC";

    private static final SmrpSubsPoller INSTANCE;
    static {
        GenericPoller.Builder b = new Builder();
        b.setName("show smrp subscriptions topic #STATS/*");
        b.setScope(Scope.VPN);
        b.setDescription("Looks to see how many clients are subscribed to StatsPump #STATS/... topics");
        b.setSempRequest("<rpc semp-version='%s'><show><smrp><subscriptions><vpn-name>*</vpn-name><primary></primary><topic></topic><topic-str>#STATS/*</topic-str></subscriptions></smrp></show></rpc>");
        b.setRunConditionOnPrimaryWhenAS(RunCondition.PRIMARY_LOCAL_ACTIVE);
        b.setRunConditionOnBackupWhenAS(RunCondition.BACKUP_LOCAL_ACTIVE);
        b.setRunConditionOnPrimaryWhenAA(RunCondition.PRIMARY_LOCAL_ACTIVE);
        b.setRunConditionOnBackupWhenAA(RunCondition.PRIMARY_LOCAL_ACTIVE);
        b.setDestination(DestinationType.MGMT);  // actually, won't broadcast this... doesn't matter
        b.setTopicStringSuffix("SMRP_SUBS_STATS");
        b.setBaseTag("/rpc-reply/rpc/show/smrp/subscriptions/subscription");
        b.setVpnNameTag("/vpn-name");
        b.addObjectTag(DESTINATION_NAME,"/destination-name");
        b.addObjectTag(TOPIC,"/topic");
        INSTANCE = new SmrpSubsPoller(b);
    }

    private static final Logger logger = LoggerFactory.getLogger(SmrpSubsPoller.class);

    public static final Poller getInstance() {
        return INSTANCE;
    }

    private SmrpSubsPoller(Builder b) {
        super(b);
    }

    @Override
    public GenericSempSaxHandler buildSaxHandler(final PhysicalAppliance appliance) {
        return buildSaxHandler(appliance,new SmrpSubsPollerListener(appliance));
    }
    
    @Override
    public GenericSempSaxHandler buildSaxHandler(final PhysicalAppliance appliance, final SempSaxProcessingListener listener) {
        return new GenericSempSaxHandler(this,appliance,listener,getObjectTags());
    }
    
    private class SmrpSubsPollerListener extends GenericSempReplyParserListener {

        private Map<String,Set<String>> vpnClientSet = new HashMap<String,Set<String>>();
        private Map<String,Set<String>> vpnTopicSet = new HashMap<String,Set<String>>();
        
        protected SmrpSubsPollerListener(final PhysicalAppliance appliance) {
            super(appliance);
        }
        
        @Override
        public void onStatMessage(final SingleContainerSet containerSet, Map<String,String> objectValuesMap) {
            // maybe grab the stats, and that's about it
            String vpnName = objectValuesMap.get(VPN_NAME);
            if (!vpnClientSet.containsKey(vpnName)) {
                vpnClientSet.put(vpnName,new HashSet<String>());
                vpnTopicSet.put(vpnName,new HashSet<String>());
            }
            vpnClientSet.get(vpnName).add(objectValuesMap.get(DESTINATION_NAME));
            vpnTopicSet.get(vpnName).add(objectValuesMap.get(TOPIC));
            // don't call super.onStatMessage()... this is a noop for message publishing
        }
        
        private String formatStatMsg() {
            final boolean DETAIL = false;
            StringBuilder sb = new StringBuilder();
            int totalClientCount = 0;
            int totalTopicCount = 0;
            for (String vpn : vpnClientSet.keySet()) {
                sb.append(String.format("VPN '%s': %d clients",vpn,vpnClientSet.get(vpn).size()));
                if (DETAIL) {
                    sb.append(" (");
                    for (String client : vpnClientSet.get(vpn)) {
                        sb.append(String.format("%s, ",client));
                    }
                    sb.setLength(sb.length()-2);
                    sb.append(")");
                }
                sb.append(String.format("; %d unique topics",vpnTopicSet.get(vpn).size()));
                if (DETAIL) {
                    sb.append(" (");
                    for (String topic : vpnTopicSet.get(vpn)) {
                        sb.append(String.format("%s, ",topic));
                    }
                    sb.setLength(sb.length()-2);
                    sb.append(")");
                }
                sb.append(String.format("%n"));
                totalClientCount += vpnClientSet.get(vpn).size();
                totalTopicCount += vpnTopicSet.get(vpn).size();
            }
            sb.insert(0,String.format("%d VPNs with %d unique clients and %d unique topics:%n",vpnClientSet.size(),totalClientCount,totalTopicCount));
            return sb.toString();
        }
        
        @Override
        public void onEndSempReply() {
            super.onEndSempReply();
            logger.debug(String.format("Appliance '%s' - %s",physical.toString(),formatStatMsg()));
        }
    }
}
