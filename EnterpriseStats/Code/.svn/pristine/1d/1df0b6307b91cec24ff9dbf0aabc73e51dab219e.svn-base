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

import java.util.Map;

import com.solace.psg.enterprisestats.statspump.PhysicalAppliance;
import com.solace.psg.enterprisestats.statspump.config.xml.DestinationType;
import com.solace.psg.enterprisestats.statspump.config.xml.RunCondition;
import com.solace.psg.enterprisestats.statspump.containers.SingleContainerSet;
import com.solace.psg.enterprisestats.statspump.semp.GenericSempSaxHandler;
import com.solace.psg.enterprisestats.statspump.semp.SempSaxProcessingListener;

public class HostnamePoller extends GenericPoller {

    private static final String HOSTNAME = "HOSTNAME";

    private static final HostnamePoller INSTANCE;  // have to use old school static singleton due to inheritance
    static {
        GenericPoller.Builder b = new Builder();
        b.setName("show hostname");
        b.setScope(Scope.SYSTEM);
        b.setDescription("Reports the actual hostname for the appliance. Should match the 2nd level in the topic string after #STATS.");
        b.setSempRequest("<rpc semp-version='%s'><show><hostname/></show></rpc>");
        b.setRunConditionOnPrimaryWhenAS(RunCondition.ALWAYS);
        b.setRunConditionOnBackupWhenAS(RunCondition.ALWAYS);
        b.setRunConditionOnPrimaryWhenAA(RunCondition.ALWAYS);
        b.setRunConditionOnBackupWhenAA(RunCondition.ALWAYS);
        b.setDestination(DestinationType.MGMT);
        b.setTopicStringSuffix("HOSTNAME");
        //  b.setBaseTag("/rpc-reply/rpc/show/client");
        b.setBaseTag("/rpc-reply/rpc/show/hostname");
        b.addObjectTag(HOSTNAME,"/hostname");
        INSTANCE = new HostnamePoller(b);
    }

    public static final Poller getInstance() {
        return INSTANCE;
    }

    private HostnamePoller(Builder b) {
        super(b);
    }

    @Override
    public GenericSempSaxHandler buildSaxHandler(final PhysicalAppliance appliance) {
        return buildSaxHandler(appliance,new HostnamePollerListener(appliance));
    }
    
    @Override
    public GenericSempSaxHandler buildSaxHandler(final PhysicalAppliance appliance, final SempSaxProcessingListener listener) {
        return new GenericSempSaxHandler(this,appliance,listener,getObjectTags());
    }
    
    private class HostnamePollerListener extends GenericSempReplyParserListener {
        protected HostnamePollerListener(final PhysicalAppliance appliance) {
            super(appliance);
        }
        
        @Override
        public void onStartSempReply(String sempVersion, long timestamp) {
            super.onStartSempReply(sempVersion,timestamp);
        }
        
        @Override
        public void onStatMessage(final SingleContainerSet containerSet, Map<String,String> objectValuesMap) {
            physical.setHostname(objectValuesMap.get(HOSTNAME));
            // this is being done by the GenericPoller now
            super.onStatMessage(containerSet, objectValuesMap);
        }
    }
}
