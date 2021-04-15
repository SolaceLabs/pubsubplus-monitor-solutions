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

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PollerStat {
    
    public enum Stat {
        
        REQUEST_COUNT("SEMP Request Count"),  // number of semp requests made (equivalent to number of "pages")
        CHAR_COUNT("Char Count"),  // total size of each SEMP response, # of chars
        ELEMENT_COUNT("Element Count"),  // number of elements, e.g. <connections-smf>234</connections-smf>
        OJBECT_COUNT("Object Count"),  // objects are queues, VPNs, clients, bridges, etc.
        MESSAGE_COUNT("Message Count"),  // number of messages generated... should be same as object count unless using grouped pollers
        TOTAL_TIME_IN_MS("Total time in ms"),
        //PER_PAGE_TIME_IN_MS("Per-page time in ms"),
        FETCH_TIME_IN_MS("Fetch time in ms"),
        PARSE_TIME_IN_MS("Parse time in ms"),
        REPLY_START_TIME_MS("Reply Start time"),
        POLLER_RUN("Poller Runs"),  // 1 if the PollerRunnable was kicked off.  Otherwise you get a miss
        POLLER_MISS("Poller Misses"),  // 1 if a Poller is scheduled to fire, but the previous one is still going
        POLLER_ERROR("Poller Errors"),  // 1 if any type of error occurs during Poller Run (e.g. parse error, exception thrown)
        POLLER_ABORT("Poller Aborts"),  // 1 if the Poller was cancelled for some reason (e.g. shutting down, taking too long)
        ;
        
        final String name;
        
        Stat(String name) {
            this.name = name;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
    
    public static final EnumSet<Stat> aggregateStats = EnumSet.of(
            Stat.REQUEST_COUNT,Stat.CHAR_COUNT,Stat.ELEMENT_COUNT,Stat.OJBECT_COUNT,Stat.MESSAGE_COUNT,
            Stat.POLLER_RUN,Stat.POLLER_MISS,Stat.POLLER_ERROR,Stat.POLLER_ABORT);
            
    public static class Builder {
        
        private final Map<Stat,Number> stats = new LinkedHashMap<Stat,Number>();
        
        public void addStat(Stat stat, Number number) {
            stats.put(stat,number);
        }
        
        public PollerStat build() {
            return new PollerStat(this);
        }
    }
    
    private final Map<Stat,Number> stats;
    private static final Logger logger = LoggerFactory.getLogger(PollerStat.class);
    
    private PollerStat(Builder builder) {
        this.stats = builder.stats;
    }
    
    public static PollerStat buildZeroAggregateStat() {
        PollerStat.Builder builder = new PollerStat.Builder();  // this will be the totals of all Pollers on this box
        for (PollerStat.Stat s : PollerStat.aggregateStats) {
            builder.addStat(s,0);
        }
        return builder.build();
    }

    public Map<Stat, Number> getStats() {
        return stats;
    }
    
    @Override
    public String toString() {
        return stats.toString();
    }

    synchronized public void addStat(final PollerStat pollerStat) {
        for (Stat aggregateStat : aggregateStats) {
            if (pollerStat.stats.containsKey(aggregateStat)) {
                if (pollerStat.stats.get(aggregateStat) instanceof Integer) {
                    this.stats.put(aggregateStat,this.stats.get(aggregateStat).intValue()+pollerStat.stats.get(aggregateStat).intValue());
                } else if (pollerStat.stats.get(aggregateStat) instanceof Long) {
                    this.stats.put(aggregateStat,this.stats.get(aggregateStat).longValue()+pollerStat.stats.get(aggregateStat).longValue());
                } else {
                    logger.error(String.format("Have found a non-Integer or Float stat: %s",aggregateStat.name));
                }
            }
        }
    }
    

    
    
}
