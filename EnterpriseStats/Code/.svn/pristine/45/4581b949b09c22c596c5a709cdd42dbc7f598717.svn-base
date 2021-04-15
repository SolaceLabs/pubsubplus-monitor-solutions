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

package com.solace.psg.enterprisestats.statspump.stats;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solace.psg.enterprisestats.statspump.LogicalAppliance;
import com.solace.psg.enterprisestats.statspump.PhysicalAppliance;
import com.solace.psg.enterprisestats.statspump.pollers.Poller;

public enum PollerStats {

    INSTANCE;
    
    private final Map<PhysicalAppliance,Map<Poller,PollerStat>> hourlyByAppliance = new LinkedHashMap<PhysicalAppliance,Map<Poller,PollerStat>>();
    private final Set<LogicalAppliance> hourlyAppliances = new HashSet<LogicalAppliance>();
            
//    private final Map<PhysicalAppliance,Map<Poller,List<PollerStat>>> minuteByAppliance = new LinkedHashMap<PhysicalAppliance,Map<Poller,List<PollerStat>>>();
//    private final Map<PhysicalAppliance,Map<Poller,List<PollerStat>>> hourlyByAppliance = new LinkedHashMap<PhysicalAppliance,Map<Poller,List<PollerStat>>>();
//    private final Map<PhysicalAppliance,Map<Poller,List<PollerStat>>> dailyByAppliance = new LinkedHashMap<PhysicalAppliance,Map<Poller,List<PollerStat>>>();
//    //private final Map<PhysicalAppliance,Map<Poller,List<PollerStat>>> weeklyByAppliance = new LinkedHashMap<PhysicalAppliance,Map<Poller,List<PollerStat>>>();
//    
//    private final Map<Poller,Map<PhysicalAppliance,List<PollerStat>>> minuteByPoller = new LinkedHashMap<Poller,Map<PhysicalAppliance,List<PollerStat>>>();
//    private final Map<Poller,Map<PhysicalAppliance,List<PollerStat>>> hourlyByPoller = new LinkedHashMap<Poller,Map<PhysicalAppliance,List<PollerStat>>>();
//    private final Map<Poller,Map<PhysicalAppliance,List<PollerStat>>> dailyByPoller = new LinkedHashMap<Poller,Map<PhysicalAppliance,List<PollerStat>>>();
//    //private final Map<Poller,Map<PhysicalAppliance,List<PollerStat>>> weeklyByPoller = new LinkedHashMap<Poller,Map<PhysicalAppliance,List<PollerStat>>>();

    private static final Logger logger = LoggerFactory.getLogger(PollerStats.class);
    
    public void addStat(Poller poller, PhysicalAppliance physical, PollerStat pollerStat) {
        // first, is this the first time seeing this appliance?
        if (!hourlyByAppliance.containsKey(physical)) {
            synchronized(hourlyByAppliance) {
                if (!hourlyByAppliance.containsKey(physical)) {  // for sure?
                    hourlyByAppliance.put(physical,new LinkedHashMap<Poller,PollerStat>());
                    hourlyAppliances.add(physical.getLogical());  // may already be in there if the mate added it
                }
            }
        }
        Map<Poller,PollerStat> physicalPollerMap = hourlyByAppliance.get(physical);
        // now, for this appliance, have we seen this particular Poller?
        if (!physicalPollerMap.containsKey(poller)) {
            synchronized(physicalPollerMap) {
                if (!physicalPollerMap.containsKey(poller)) {
                    physicalPollerMap.put(poller,PollerStat.buildZeroAggregateStat());  // an empty 0-ized PollerStat
                }
            }
        }
        physicalPollerMap.get(poller).addStat(pollerStat);  // this call is already synchronized
    }
    
    public Callable<Boolean> getHourlyByApplianceRunnable() {
        return getWhichByApplianceRunnable();
    }
    
    private Callable<Boolean> getWhichByApplianceRunnable() { //final Map<PhysicalAppliance,Map<Poller,List<PollerStat>>> whichByAppliance) {
        return new Callable<Boolean>() {
            @Override
            public Boolean call() {
                try {
                    PollerStat allAppliances = PollerStat.buildZeroAggregateStat();
                    synchronized (hourlyByAppliance) {
                        for (LogicalAppliance logical : hourlyAppliances) {
                            PollerStat totalPrimary = PollerStat.buildZeroAggregateStat();
                            Map<Poller,PollerStat> pollerStatMap = hourlyByAppliance.get(logical.getPrimary());
                            //logger.debug("There will be "+pollerStatMap.keySet().size()+" pollers here");
                            for (Poller poller : pollerStatMap.keySet()) {
//                                logger.info(String.format("PER-MINUTE for %s on %s: %s",poller,logical.getPrimary(),pollerStatMap.get(poller)));
                                totalPrimary.addStat(pollerStatMap.get(poller));
                            }
                            logger.debug(String.format("Stats Per-Hour for %s: %s",logical.getPrimary(),totalPrimary));
                            allAppliances.addStat(totalPrimary);
                            if (!logical.isStandalone()) {
                                PollerStat totalBackup = PollerStat.buildZeroAggregateStat();
                                pollerStatMap = hourlyByAppliance.get(logical.getBackup());
                                //logger.debug("There will be "+pollerStatMap.keySet().size()+" pollers here");
                                for (Poller poller : pollerStatMap.keySet()) {
//                                    logger.info(String.format("PER-MINUTE for %s on %s: %s",poller,logical.getBackup(),pollerStatMap.get(poller)));
                                    totalBackup.addStat(pollerStatMap.get(poller));
                                }
                                logger.debug(String.format("Stats Per-Hour for %s: %s",logical.getBackup(),totalBackup));
                                allAppliances.addStat(totalBackup);
                                totalPrimary.addStat(totalBackup);  // reuse the "primary" for the Logical HA-Pair totals
                                logger.debug(String.format("Stats Per-Hour for %s: %s",logical,totalPrimary));
                            }
                        }
                        hourlyByAppliance.clear();
                    }
                    logger.debug(String.format("Stats Per-Hour for ALL appliances: %s",allAppliances));
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                return true;
            }
            @Override
            public String toString() {
                return "Runnable for Stats-per-Appliance";
            }
        };
    }
}
