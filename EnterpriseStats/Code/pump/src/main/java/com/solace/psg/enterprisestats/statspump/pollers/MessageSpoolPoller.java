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

public class MessageSpoolPoller extends GenericPoller {

    private static final String AD_STATUS = "AD_STATUS";

    private static final MessageSpoolPoller INSTANCE;
    static {
        GenericPoller.Builder b = new Builder();
        b.setName("show message-spool");
        b.setScope(Scope.SYSTEM);
        b.setDescription("Reports basic information about this router's message spool");
        b.setSempRequest("<rpc semp-version='%s'><show><message-spool/></show></rpc>");
        b.setRunConditionOnPrimaryWhenAS(RunCondition.ALWAYS);
        b.setRunConditionOnBackupWhenAS(RunCondition.ALWAYS);
        b.setRunConditionOnPrimaryWhenAA(RunCondition.ALWAYS);
        b.setRunConditionOnBackupWhenAA(RunCondition.ALWAYS);
        b.setDestination(DestinationType.MGMT);
        b.setTopicStringSuffix("MSG-SPOOL");
        b.setBaseTag("/rpc-reply/rpc/show/message-spool/message-spool-info");
        b.addObjectTag(AD_STATUS,"/operational-status");
        INSTANCE = new MessageSpoolPoller(b);
    }

    public static final Poller getInstance() {
        return INSTANCE;
    }

    private MessageSpoolPoller(Builder b) {
        super(b);
    }

    @Override
    public GenericSempSaxHandler buildSaxHandler(final PhysicalAppliance appliance) {
        return buildSaxHandler(appliance,new MessageSpoolPollerListener(appliance));
    }
    
    @Override
    public GenericSempSaxHandler buildSaxHandler(final PhysicalAppliance appliance, final SempSaxProcessingListener listener) {
        return new GenericSempSaxHandler(this,appliance,listener,getObjectTags());
    }
    
    private class MessageSpoolPollerListener extends GenericSempReplyParserListener {
                
        protected MessageSpoolPollerListener(final PhysicalAppliance appliance) {
            super(appliance);
        }
        
        @Override
        public void onStartSempReply(String sempVersion, long timestamp) {
            super.onStartSempReply(sempVersion,timestamp);
        }
        
        @Override
        public void onStatMessage(final SingleContainerSet containerSet, Map<String,String> objectValuesMap) {
            physical.setAdActivity(objectValuesMap.get(AD_STATUS));
            super.onStatMessage(containerSet,objectValuesMap);
        }
    }
}
