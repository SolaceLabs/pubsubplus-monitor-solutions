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

package com.solace.psg.enterprisestats.statspump.util;

public class StatsPumpConstants {

    public static final int MAX_TOPIC_LENGTH = 250;
    public static String TOPIC_STRING_PREFIX = "#STATS";  // e.g. #STATS/VPN/solace2/abc_ldn_dev/VPN_STATS 
                                                          //   or #STATS/VPN/solace2/def_ldn_uat/QUEUE/SOLQIN.DEF.WORKFLOW.01
    public static String MQTT_TOPIC_STRING_PREFIX = "$SYS/STATS";  // for MQTT, matches Solace format
    public static String POLLERS_BROADCAST_TOPIC_SUFFIX = "POLLERS";
    public static String ROUTER_BROADCAST_TOPIC_SUFFIX = "ROUTER";
    public static String ANNOUNCE_BROADCAST_TOPIC_SUFFIX = "ANNOUNCE";
    public static String POLLER_START_BROADCAST_TOPIC = "POLLER_START";
    public static String POLLER_END_BROADCAST_TOPIC = "POLLER_END";
    public static String POLLER_STAT_TOPIC = "POLLER_STAT";

    // #STATS/PUMP/emea1/POLLER_STAT/SYSTEM/MEMORY   -- individual stat
    // #STATS/PUMP/emea2/POLLER_STATS/M/SYSTEM/MEMORY  -- aggregate stats per minute
    // #STATS/PUMP/emea2/POLLER_STATS  -- aggregate of all Pollers on emea2
    // #STATS/PUMP/POLLER_STATS/M/SYSTEM/MEMORY  -- aggregate minute stats for SYSTEM_MEMORY Poller across all appliances
    // #STATS/PUMP/MEMORY -- each mgmt destination
    // #STATS/PUMP/QUEUES 
    
    
    public static String SEMP_VERSION_TAG = "version";
    public static String TOPIC_LEVELS_TAG = "topic-levels";

    public static String REDUNDANCY_STANDALONE = "Standalone";
    public static String REDUNDANCY_ACTIVE_STANDBY = "Active/Standby";
    public static String REDUNDANCY_ACTIVE_ACTIVE = "Active/Active";

    public static int MISSED_SEMP_POLL_LIMIT = 5;
    public static double MAX_POLLER_RUN_LIMIT_FACTOR = 3.1;

    public static int HOSTNAME_POLLER_INTERVAL_SEC = 120;
    public static int REDUNDANCY_POLLER_INTERVAL_SEC = 5;
    public static int VPN_DETAIL_POLLER_INTERVAL_SEC = 30;
    public static int MSG_SPOOL_POLLER_INTERVAL_SEC = 5;
    public static int SMRP_SUBS_POLLER_INTERVAL_SEC = 3600;
    public static int REACHABLE_POLLER_INTERVAL_SEC = 20;
    public static boolean STREAMING_HTTP_FETCH = true;

    public static char FLAT_KEY_LEVEL_SEPARATOR = '/';
    public static char FLAT_KEY_NUMERIC_INDEX_SEPARATOR = '|';
    public static String BASETAG_DEFAULT = "/ROOT";  // must include leading / to match other basetags
    
    public static int BLOCKING_QUEUE_TOTAL_CAPACITY = 50000;

    
    
    static {
        for (Object o : System.getProperties().keySet()) {
            String s = (String)o;
            if (s.startsWith("pump.")) {
                Object val = System.getProperty(s);
                String prop = s.substring(5);
                System.err.println(prop+" = "+val);

            }
        }

    }
   
}
