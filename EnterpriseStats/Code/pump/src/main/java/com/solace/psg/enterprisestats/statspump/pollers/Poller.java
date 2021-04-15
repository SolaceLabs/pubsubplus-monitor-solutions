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

import java.util.List;
import java.util.Map;

import com.solace.psg.enterprisestats.statspump.PhysicalAppliance;
import com.solace.psg.enterprisestats.statspump.config.xml.DestinationType;
import com.solace.psg.enterprisestats.statspump.config.xml.RunCondition;
import com.solace.psg.enterprisestats.statspump.semp.GenericSempSaxHandler;
import com.solace.psg.enterprisestats.statspump.semp.SempSaxProcessingListener;
import com.solace.psg.enterprisestats.statspump.tools.semp.SempVersion;

public interface Poller {

    public enum Scope {
        VPN,
        SYSTEM,
    }
    
    public String getName();
    public String getDescription();
    public Scope getScope();
    public String getSempRequest();
    public String getSempRequestCompressed();
    public RunCondition runOnPrimaryWhenActiveStandby();
    public RunCondition runOnBackupWhenActiveStandby();
    public RunCondition runOnPrimaryWhenActiveActive();
    public RunCondition runOnBackupWhenActiveActive();
    public DestinationType getDestination();
    public String getBaseTag();
    public Map<String, String> getObjectTags();
    public SempVersion getMinSempVersion();
    public SempVersion getMaxSempVersion();
    
    /**
     * Helper method that will send out a 'broadcast' message on a well-known
     * topic just at the start of the polling.  Applications can subscribe to
     * that topic to hear what topics to listen on.
     * @param appliance
     * @return a string that represents the topic an app would subscribe to
     * to receive updates for this poller
     */
    public String getTopicStringForBroadcast();    
    public List<String> getTopicLevelsForBroadcast();    
    public GenericSempSaxHandler buildSaxHandler(PhysicalAppliance appliance);
    public GenericSempSaxHandler buildSaxHandler(PhysicalAppliance appliance, SempSaxProcessingListener listener);

}
