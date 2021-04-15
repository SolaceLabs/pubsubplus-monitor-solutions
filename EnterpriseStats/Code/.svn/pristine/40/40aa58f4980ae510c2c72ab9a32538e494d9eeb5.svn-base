///*
// * Copyright 2014-2016 Solace Systems, Inc. All rights reserved.
// *
// * http://www.solacesystems.com
// *
// * This source is distributed under the terms and conditions
// * of any contract or contracts between Solace Systems, Inc.
// * ("Solace") and you or your company. If there are no 
// * contracts in place use of this source is not authorized.
// * No support is provided and no distribution, sharing with
// * others or re-use of this source is authorized unless 
// * specifically stated in the contracts referred to above.
// *
// * This product is provided as is and is not supported by Solace 
// * unless such support is provided for under an agreement 
// * signed between you and Solace.
// */
//
//package com.solace.psg.enterprisestats.statspump.profiler;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.util.ArrayList;
//import java.util.LinkedHashMap;
//import java.util.List;
//import java.util.Map;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import com.solace.psg.enterprisestats.statspump.LogicalAppliance;
//import com.solace.psg.enterprisestats.statspump.PhysicalAppliance;
//import com.solace.psg.enterprisestats.statspump.PumpManager;
//import com.solace.psg.enterprisestats.statspump.config.ConfigLoader;
//import com.solace.psg.enterprisestats.statspump.config.ConfigLoaderException;
//import com.solace.psg.enterprisestats.statspump.config.LocalFileConfigStreamsImpl;
//import com.solace.psg.enterprisestats.statspump.pollers.HostnamePoller;
//import com.solace.psg.enterprisestats.statspump.pollers.Poller;
//import com.solace.psg.enterprisestats.statspump.pollers.RedundancyPoller;
//import com.solace.psg.enterprisestats.statspump.pollers.VpnDetailPoller;
//import com.solace.psg.enterprisestats.statspump.semp.GenericSempSaxHandler;
//import com.solace.psg.enterprisestats.statspump.semp.SempSaxProcessingListener;
//import com.solace.psg.enterprisestats.statspump.tools.comms.HttpUtils;
//import com.solace.psg.enterprisestats.statspump.tools.comms.SempRequestException;
//import com.solace.psg.enterprisestats.statspump.tools.parsers.SaxParser;
//import com.solace.psg.enterprisestats.statspump.tools.parsers.SaxParserException;
//import com.solace.psg.enterprisestats.statspump.tools.parsers.SempReplyDomParser;
//import com.solace.psg.enterprisestats.statspump.tools.semp.SempReplySchemaLoader;
//
// // This class is a development tool, not part of runtime deployment
//public class Profiler {
//
//    public enum StatType {
//
//        HTTP_FETCH_uS("Fetch Only"),
//        PARSE_uS("Parse Only"),
//        HTTP_FETCH_PARSE_uS("Fetch then Parse"),
//        HTTP_STREAM_PARSE_uS("Stream + Parse"),
//        BYTES("Byes"),
//        OBJECT_COUNT("Object Count"),
//        ;
//
//        private String name;
//
//        StatType(String name) {
//            this.name = name;
//        }
//
//        @Override
//        public String toString() {
//            return name;
//        }
//    };
//
//
//    private static final Logger logger = LoggerFactory.getLogger(Profiler.class);
//    private static final int LOOPS = 40;
//    private static final int WARMUP_LOOPS = 3;
//    private static final int DELAY_INTER_POLL_MS = 200;
//    private static final int DELAY_INTER_CLUSTER_MS = 500;
//    private static final int DELAY_INTER_LOOP_MS = 2000;
//    private static boolean FOR_THE_RECORD = false;
//
//    // poller name -> appliance -> stat -> list of times in microseconds for high accuracy!
//    private static Map<String,Map<PhysicalAppliance,Map<StatType,List<Long>>>> stats = new LinkedHashMap<String,Map<PhysicalAppliance,Map<StatType,List<Long>>>>();
//    
//    private static void timeHttpStreamAndParse(PhysicalAppliance appliance, Poller poller) {
//        String request = poller.getSempRequest();
//        int objectCount = 0;
//        long start = System.nanoTime();
//        InputStream is = null;
//        try {
//            while (request != null) {
//                is = HttpUtils.performSempStreamRequest(appliance.getSempHostnameOrIp(),appliance.getSempUsername(),appliance.getSempPassword(),request);
//                SempSaxProcessingListener listener = new NoopSempProcessorListener();
//                GenericSempSaxHandler handler = poller.buildSaxHandler(appliance,listener);
//                SaxParser.parseStream(is, handler);
//                request = handler.getMoreCookie();
//                objectCount += handler.getObjectCount();
//            }
//            System.err.print(".");
//        } catch (SempRequestException e) {
//            logger.warn(String.format("Caught on %s on %s",poller,appliance),e);
//            System.err.print("X");
//            if (is != null) {
//                try {
//                    is.close();
//                } catch (IOException e1) { }
//            }
//        } catch (SaxParserException e) {
//            logger.warn(String.format("Caught on %s on %s",poller,appliance),e);
//            System.err.print("X");
//            if (is != null) {
//                try {
//                    is.close();
//                } catch (IOException e1) { }
//            }
//        }
//        if (FOR_THE_RECORD) {
//            stats.get(poller.getName()).get(appliance).get(StatType.HTTP_STREAM_PARSE_uS).add((System.nanoTime()-start)/1000);
//            stats.get(poller.getName()).get(appliance).get(StatType.OBJECT_COUNT).add((long)objectCount);
//        }
//    }
//
//    private static void timeHttpFetchAndParse(PhysicalAppliance appliance, Poller poller) {
//        String request = poller.getSempRequest();
//        String response;
//        long fetch = 0;
//        long parse = 0;
//        long start = System.nanoTime();
//        long totalBytes = 0;
//        try {
//            while (request != null) {
//                response = HttpUtils.performSempStringRequest(appliance.getSempHostnameOrIp(),appliance.getSempUsername(),appliance.getSempPassword(),request);
//                totalBytes += response.length();
//                fetch += System.nanoTime()-start;
//                start = System.nanoTime();
//                request = SempReplyDomParser.getMoreCookie(response);
//                SempSaxProcessingListener listener = new NoopSempProcessorListener();
//                GenericSempSaxHandler handler = poller.buildSaxHandler(appliance,listener);
//                SaxParser.parseString(response, handler);
//                parse += System.nanoTime()-start;
//                start = System.nanoTime();
//            }
//        } catch (SempRequestException e) {
//            logger.warn(String.format("Caught on %s on %s",poller,appliance),e);
//            System.err.print("X");
//        } catch (SaxParserException e) {
//            logger.warn(String.format("Caught on %s on %s",poller,appliance),e);
//            System.err.print("X");
//        }
//        if (FOR_THE_RECORD) {
//            stats.get(poller.getName()).get(appliance).get(StatType.HTTP_FETCH_uS).add(fetch/1000);
//            stats.get(poller.getName()).get(appliance).get(StatType.PARSE_uS).add(parse/1000);
//            stats.get(poller.getName()).get(appliance).get(StatType.HTTP_FETCH_PARSE_uS).add((fetch+parse)/1000);
//            stats.get(poller.getName()).get(appliance).get(StatType.BYTES).add(totalBytes);
//        }
//        System.err.print(".");
//    }
//
//    private static void runPoller(Poller poller, LogicalAppliance cluster) throws InterruptedException {
//        timeHttpFetchAndParse(cluster.getPrimary(),poller);
//        Thread.sleep(DELAY_INTER_POLL_MS);
//        timeHttpStreamAndParse(cluster.getPrimary(),poller);
//        Thread.sleep(DELAY_INTER_POLL_MS);
//    }	
//
//
//    public static void main(String... args) throws InterruptedException, IOException, ConfigLoaderException {
//        if (args.length < 3) {
//            System.err.println("Specify 2 arguments: poller_config.xml poller_group_config.xml appliance_config.xml");
//            System.exit(-1);
//        }
//        SempReplySchemaLoader.loadSchemas();
//        
//        LocalFileConfigStreamsImpl streams = new LocalFileConfigStreamsImpl(args[0],args[1],args[2]);
//        ConfigLoader.loadConfig(streams);
//        
//        PumpManager mgr = new PumpManager();
//        mgr.performInitialQueries(ConfigLoader.appliancesList);
//        
//        System.err.println("Ready to go... hit Enter");
//        System.in.read();
//        List<String> pollerList = new ArrayList<String>();//ConfigLoader.pollersMap.keySet());
//        pollerList.add("VPN Queue Basic");
//        pollerList.add("VPN Queue Rates");
//        System.err.println("Polling order will be:");
//        for (int i=0;i<pollerList.size();i++) {
//            System.err.printf("  %2d %s%n",i,pollerList.get(i));
//        }
//        System.err.println();
//        System.err.println("Appliance order will be:");
//        for (int i=0;i<ConfigLoader.appliancesList.size();i++) {
//            System.err.printf("  %2d %s%n",i,ConfigLoader.appliancesList.get(i));
//        }
//        System.err.println();
//        if (WARMUP_LOOPS > 0) System.err.printf("Warming up... %d loops...",WARMUP_LOOPS);
//        for (int i=(-WARMUP_LOOPS);i<LOOPS;i++) {
//            if (i == 0) {
//                FOR_THE_RECORD = true;
//                if (WARMUP_LOOPS > 0) System.err.printf("%n%nWarmup over... for the record now! ");
//                System.err.printf("%d loops...",LOOPS);
//            }
//            System.err.printf("%nL%2d>",i);
//            for (int j=0;j<pollerList.size();j++) {
//                Poller poller = ConfigLoader.pollersMap.get(pollerList.get(j));
//                if (poller == null) {
//                    if (pollerList.get(j).equals("VPN Service")) poller = VpnDetailPoller.getInstance();
//                    if (pollerList.get(j).equals("Router Hostname")) poller = HostnamePoller.getInstance();
//                    if (pollerList.get(j).equals("Message Spool Info")) poller = RedundancyPoller.getInstance();
//                    if (pollerList.get(j).equals("Redundancy")) poller = RedundancyPoller.getInstance();
//                }
//                String pollerName = poller.getName();
//                //if (!pollerName.contains("Global")) continue;
//                if (!stats.containsKey(pollerName)) {
//                    stats.put(pollerName, new LinkedHashMap<PhysicalAppliance,Map<StatType,List<Long>>>());
//                }
//                System.err.printf("p%d",j);
//                for (int k=0;k<ConfigLoader.appliancesList.size();k++) {
//                    LogicalAppliance cluster = ConfigLoader.appliancesList.get(k);
//                    if (!stats.get(pollerName).containsKey(cluster.getPrimary())) {
//                        stats.get(pollerName).put(cluster.getPrimary(),new LinkedHashMap<StatType,List<Long>>());
//                        for (StatType statType : StatType.values()) {
//                            stats.get(pollerName).get(cluster.getPrimary()).put(statType, new ArrayList<Long>());
//                        }
//                        if (!cluster.isStandalone()) {
//                            stats.get(pollerName).put(cluster.getBackup(),new LinkedHashMap<StatType,List<Long>>());
//                            for (StatType statType : StatType.values()) {
//                                stats.get(pollerName).get(cluster.getBackup()).put(statType, new ArrayList<Long>());
//                            }
//                        }
//                    }
//                    System.err.printf("a%d",k);
//                    runPoller(poller,cluster);
//                    Thread.sleep(DELAY_INTER_CLUSTER_MS);
//                }
//            }
//            Thread.sleep(DELAY_INTER_LOOP_MS);
//        }
//        // Now, to print out the output in a CSV format
//        System.err.println();
//        System.out.printf("%s,%s,",StatType.OBJECT_COUNT,StatType.BYTES);
//        for (String pollerName : stats.keySet()) {
//            for (PhysicalAppliance appliance : stats.get(pollerName).keySet()) {
//                for (StatType statType : StatType.values()) {
//                    if (statType == StatType.OBJECT_COUNT || statType == StatType.BYTES) continue;
//                    System.out.printf("%s - %s - %s,",pollerName,appliance.getHostname(),statType);
//                }
//            }
//        }
//        System.out.println();
//        int pollerCount = 0;
//        for (String pollerName : stats.keySet()) {
//            int applianceCount = 0;
//            for (PhysicalAppliance appliance : stats.get(pollerName).keySet()) {
//                Map<StatType,List<Long>> statMap = stats.get(pollerName).get(appliance);
//                for (int i=0;i<LOOPS;i++) {
//                    System.out.printf("%s,%s,",statMap.get(StatType.OBJECT_COUNT).get(i),statMap.get(StatType.BYTES).get(i));
//                    // add extra commas based on some logic
//                    for (int j=0;j<(pollerCount*stats.get(pollerName).keySet().size())+applianceCount;j++) {
//                        for (int k=0;k<StatType.values().length-2;k++) {  // loop for the number of stat types, less 2 (for bytes and object count)
//                            System.out.print(",");
//                        }
//                    }
//                    for (StatType statType : StatType.values()) {
//                        if (statType == StatType.OBJECT_COUNT || statType == StatType.BYTES) continue;
//                        System.out.printf("%.3f,",(double)statMap.get(statType).get(i)/1000);
//                    }
//                    System.out.println();
//                }
//                applianceCount++;
//            }
//            pollerCount++;
//        }
//    }
//}
//
