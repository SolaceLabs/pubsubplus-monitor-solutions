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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.solace.psg.enterprisestats.statspump.LogicalAppliance;
import com.solace.psg.enterprisestats.statspump.PhysicalAppliance;
import com.solace.psg.enterprisestats.statspump.config.xml.DestinationType;
import com.solace.psg.enterprisestats.statspump.config.xml.RunCondition;
import com.solace.psg.enterprisestats.statspump.containers.SingleContainerSet;
import com.solace.psg.enterprisestats.statspump.semp.GenericSempSaxHandler;
import com.solace.psg.enterprisestats.statspump.semp.SempSaxProcessingListener;

/**
 */
public class VpnDetailPoller extends GenericPoller {

    // these need to be public as PhysicalAppliance uses these when changing a VPN's status
    public static final String TAG_ENABLED = "ENABLED";
    public static final String TAG_OPERATIONAL = "OPERATIONAL";
    public static final String TAG_LOCALLY_CONFIGURED = "LOCALLY_CONFIGURED";
    public static final String TAG_LOCAL_STATUS = "LOCAL_STATUS";
    public static final String TAG_SMF_TOPIC_FORMAT = "SMF_TOPIC_FORMAT";
    public static final String TAG_MQTT_TOPIC_FORMAT = "MQTT_TOPIC_FORMAT";
    
    private static final VpnDetailPoller INSTANCE;
    static {
        GenericPoller.Builder b = new Builder();
        b.setName("show message-vpn * detail");
        b.setScope(Scope.VPN);
        b.setDescription("Details the configuration and information available within a Message VPN. Doesn't include Guaranteed messaging info.");
        b.setSempRequest("<rpc semp-version='%s'><show><message-vpn><vpn-name>*</vpn-name><detail/><count/><num-elements>1000</num-elements></message-vpn></show></rpc>");  // <count/><num-elements>10</num-elements>
        b.setRunConditionOnPrimaryWhenAS(RunCondition.ALWAYS);  // use always so VPN list is kept up to date
        b.setRunConditionOnBackupWhenAS(RunCondition.ALWAYS);   // but we'll create the PBPP behaviour below in the onStatMessage()
        b.setRunConditionOnPrimaryWhenAA(RunCondition.ALWAYS);
        b.setRunConditionOnBackupWhenAA(RunCondition.ALWAYS);
        b.setDestination(DestinationType.BOTH);
        b.setTopicStringSuffix("DETAIL");
        b.setBaseTag("/rpc-reply/rpc/show/message-vpn/vpn");
        b.setVpnNameTag("/name");
        b.addObjectTag(TAG_ENABLED,"/enabled");
        b.addObjectTag(TAG_OPERATIONAL,"/operational");
        b.addObjectTag(TAG_LOCALLY_CONFIGURED,"/locally-configured");
        b.addObjectTag(TAG_LOCAL_STATUS,"/local-status");
        b.addObjectTag(TAG_SMF_TOPIC_FORMAT,"/event-configuration/publish-topic-format/smf");
        b.addObjectTag(TAG_MQTT_TOPIC_FORMAT,"/event-configuration/publish-topic-format/mqtt");
        INSTANCE = new VpnDetailPoller(b);
    }
    
    public static final Poller getInstance() {
        return INSTANCE;
    }

    private VpnDetailPoller(Builder b) {
        super(b);
    }
        
    // These next methods are a bit of a hack... this poller is run continuously (AAAA) to track the statuses
    // of the VPNs on both appliances, but we want to have PBPP publishing behaviour
    // So fake it for the broadcast message... if he is calling, then return different values
    // below in the onStatMessage() we provide PBPP publishing behaviour
    // TODO always make sure the method name is called correctly!
    @Override
    public RunCondition runOnPrimaryWhenActiveStandby() {
        if (Thread.currentThread().getStackTrace()[2].getMethodName().equals("publishPollerBroadcastMessage")) {
            return RunCondition.PRIMARY_LOCAL_ACTIVE;
        } else return super.runOnPrimaryWhenActiveStandby();
    }

    @Override
    public RunCondition runOnBackupWhenActiveStandby() {
        if (Thread.currentThread().getStackTrace()[2].getMethodName().equals("publishPollerBroadcastMessage")) {
            return RunCondition.BACKUP_LOCAL_ACTIVE;
        } else return super.runOnPrimaryWhenActiveStandby();
    }

    @Override
    public RunCondition runOnPrimaryWhenActiveActive() {
        if (Thread.currentThread().getStackTrace()[2].getMethodName().equals("publishPollerBroadcastMessage")) {
            return RunCondition.PRIMARY_LOCAL_ACTIVE;
        } else return super.runOnPrimaryWhenActiveStandby();
    }

    @Override
    public RunCondition runOnBackupWhenActiveActive() {
        if (Thread.currentThread().getStackTrace()[2].getMethodName().equals("publishPollerBroadcastMessage")) {
            return RunCondition.PRIMARY_LOCAL_ACTIVE;
        } else return super.runOnPrimaryWhenActiveStandby();
    }
    
    @Override
    public GenericSempSaxHandler buildSaxHandler(PhysicalAppliance appliance) {
        return buildSaxHandler(appliance,new VpnDetailPollerListener(appliance));
    }
    
    @Override
    public GenericSempSaxHandler buildSaxHandler(PhysicalAppliance appliance, SempSaxProcessingListener listener) {
        return new GenericSempSaxHandler(this,appliance,listener,getObjectTags());
    }
    
    public class VpnDetailPollerListener extends GenericSempReplyParserListener {

        Set<String> vpns = new HashSet<String>();
        
        protected VpnDetailPollerListener(PhysicalAppliance appliance) {
            super(appliance);
        }
        
        @Override
        public void onStatMessage(final SingleContainerSet containerSet, Map<String,String> objectValuesMap) {
            //ENABLED=true, OPERATIONAL=true, VPN_NAME_TAG=d01_indigo, LOCALLY_CONFIGURED=true
            physical.updateVpnStatus(objectValuesMap.get(VPN_NAME),objectValuesMap);
            // ok, so thing poller is configured to run ALWAYS, but that's just to maintain the list of active VPNs
            // so, must replicate similar behaviour to shouldPollerRun() in PollerRunnable to determine if we should publish a message
            // Want PBPP behaviour, so if Active-Active, then run as long as primary active
            if (physical.getLogical().getType() == LogicalAppliance.Type.ACTIVE_ACTIVE) {
                if (physical.isPrimaryActive()) {
                    super.onStatMessage(containerSet,objectValuesMap);
                }
            } else {  // either A/S, or Stand-alone, so simulate that run behaviour
                if ((physical.isPrimary() && physical.isPrimaryActive()) ||
                        (!physical.isPrimary() && physical.isBackupActive())) {
                    super.onStatMessage(containerSet,objectValuesMap);
                }
            }
        }
    }
}
