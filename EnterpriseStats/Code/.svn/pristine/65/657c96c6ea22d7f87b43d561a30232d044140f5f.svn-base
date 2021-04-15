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
package com.solace.psg.enterprisestats.statspump;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solace.psg.enterprisestats.statspump.VpnConnectionManager.VpnConnection;
import com.solace.psg.enterprisestats.statspump.config.ConfigLoader;
import com.solace.psg.enterprisestats.statspump.config.ConfigLoaderException;
import com.solace.psg.enterprisestats.statspump.config.StatsConfigStreams;
import com.solace.psg.enterprisestats.statspump.config.xml.DestinationType;
import com.solace.psg.enterprisestats.statspump.pollers.HostnamePoller;
import com.solace.psg.enterprisestats.statspump.pollers.MessageSpoolPoller;
import com.solace.psg.enterprisestats.statspump.pollers.Poller;
import com.solace.psg.enterprisestats.statspump.pollers.RedundancyPoller;
import com.solace.psg.enterprisestats.statspump.pollers.SmrpSubsPoller;
import com.solace.psg.enterprisestats.statspump.pollers.VpnDetailPoller;
import com.solace.psg.enterprisestats.statspump.stats.PollerStats;
import com.solace.psg.enterprisestats.statspump.tools.semp.SempReplySchemaLoader;
import com.solace.psg.enterprisestats.statspump.tools.semp.SempRpcSchemaLoader;
import com.solace.psg.enterprisestats.statspump.util.MessageUtils;
import com.solace.psg.enterprisestats.statspump.util.MonitoredLinkBlockingQueue;
import com.solace.psg.enterprisestats.statspump.util.MonitoredQueueTracker;
import com.solace.psg.enterprisestats.statspump.util.NamedThreadFactory;
import com.solace.psg.enterprisestats.statspump.util.StatsPumpConstants;

/**
 * The class is effectively the main class for the StatsPump application. 
 * 
 *   It is the main program designed to be driven in two ways:
 *   1) Normally, via main(args[]) call from the command line, driven by StatsPump.java
 *   2) Via an external entity such as a J2EE app server. 
 *   
 *   In both of these cases, the you use the following sequence to control the Pump:
 *   - configure()
 *   - start()
 *   - stop()
 *   
 *    Configure passes in Input streams to the application configuration files.
 */
public class PumpManager {

	private static PumpManager instance = null;
	public static PumpManager getInstance() {
		return instance;
	}

    private volatile boolean startedFlag = false;  // doesn't need to be atomic as this only gets changed once
    private final List<LogicalAppliance> logicalAppliances = new ArrayList<LogicalAppliance>();

    final ExecutorService pumpExecutor = Executors.newCachedThreadPool(new NamedThreadFactory("Pump Thread"));
    final ScheduledExecutorService pollerExecutor = Executors.newScheduledThreadPool(256,new NamedThreadFactory("Poll Thread"));
    final ScheduledExecutorService scheduleExecutor = Executors.newScheduledThreadPool(2,new NamedThreadFactory("Scheduler Thread"));
    private final ExecutorService utilityExecutor = Executors.newScheduledThreadPool(4,new NamedThreadFactory("Util Thread"));

	private static final Logger logger = LoggerFactory.getLogger(PumpManager.class);

    public PumpManager()  {
        Runnable shutdownHookRunnable = new PumpShutdownHookRunnable();
        Runtime.getRuntime().addShutdownHook(new Thread(shutdownHookRunnable,"Pump Shutdown Thread"));
        instance = this;
    }

    private void load(StatsConfigStreams loader) throws ConfigLoaderException {
        ConfigLoader.loadConfig(loader);
        this.logicalAppliances.addAll(ConfigLoader.appliancesList);
        performInitialQueries(this.logicalAppliances);
    }
	
    
	private void startPump() {
	    startedFlag = true;
	    logger.debug("Scheduling the jobs");
        for (LogicalAppliance logical : logicalAppliances) {
            // first, every appliance pair gets its own message queue and publisher (order will be maintained!
            for (MessageBus msgBus : logical.getMsgBuses(DestinationType.BOTH)) {
                pumpExecutor.execute(new MessageBusRepublisher(msgBus));
            }
            // add the fixed pollers
            logical.addPoller(SmrpSubsPoller.getInstance(),StatsPumpConstants.SMRP_SUBS_POLLER_INTERVAL_SEC);    // how often do we check who is subscribed to us?  once an hour
            logical.addPoller(HostnamePoller.getInstance(),StatsPumpConstants.HOSTNAME_POLLER_INTERVAL_SEC);
            logical.addPoller(RedundancyPoller.getInstance(),StatsPumpConstants.REDUNDANCY_POLLER_INTERVAL_SEC);    // this is how fast we detect fail-overs
            logical.addPoller(MessageSpoolPoller.getInstance(),StatsPumpConstants.MSG_SPOOL_POLLER_INTERVAL_SEC);  // this is how fast we detect message-spool changes
            logical.addPoller(VpnDetailPoller.getInstance(),StatsPumpConstants.VPN_DETAIL_POLLER_INTERVAL_SEC);   // this is how fast we detect VPNs appearing / changing
            for (Poller poller : logical.getPollers()) {
                schedulePoller(logical,poller,logical.getPollerInterval(poller));
            }
        }
        // some simple logging about the internals of the program
        utilityExecutor.execute(new Runnable() {
            @Override
            public void run() {
                int loop = 0;
                try {
                    while (true) {
                        for (MonitoredLinkBlockingQueue<?> queue : MonitoredQueueTracker.INSTANCE.getQueues()) {
                                if (queue.size() > 50 && loop % 2 == 0) {
                                    logger.debug(String.format("*** %s: %s",queue.getName(),queue.getStats()));
                                } else if (queue.size() > 1000) {
                                    logger.info(String.format("*** %s: %s",queue.getName(),queue.getStats()));
                                } else if (loop == 0) {
                                    logger.debug(String.format("*** %s: %s",queue.getName(),queue.getStats()));
                                }
                        }
                        if (loop == 0) {
                            Runtime runtime = Runtime.getRuntime();
                            int mb = 1024*1024;
                            logger.debug(String.format("*** Used Memory=%dMb; Free Memory=%dMb; Total Memory=%dMb; Active Threads=%d",(runtime.totalMemory()-runtime.freeMemory())/mb,runtime.freeMemory()/mb,runtime.totalMemory()/mb,Thread.activeCount()));
                        }
                        //else if (statsMsgBlockingQueue.size()>50) logger.info(String.format("*** Depth of message blocking queue: %d ***",statsMsgBlockingQueue.size()));
                        loop = (loop+1) % 50;
                        Thread.sleep((int)(Math.random()*10000));
                    }
                } catch (InterruptedException e) {
                    logger.info("My logging loop got interrupted!  Probably shutting down");
                }
            }
        });
        // now for some the broadcast of all the pollers and some helpful hints!
        schedulePeriodic(utilityExecutor,new Callable<Boolean>() {
            @Override
            public Boolean call() {
                for (LogicalAppliance pair : logicalAppliances) {
                    for (Poller poller : pair.getPollers()) {
                        MessageUtils.publishPollerBroadcastMessage(poller, pair);
                    }
                    MessageUtils.publishAnnounceBroadcastMessage(pair);
                }
                return true;
            }
            @Override
            public String toString() {
                return "Pollers and Tips broadcast job";
            }
        },120000,0);  // 2 minutes
        schedulePeriodic(utilityExecutor,new Callable<Boolean>() {
            @Override
            public Boolean call() {
                for (LogicalAppliance pair : logicalAppliances) {
                    MessageUtils.publishApplianceBroadcastMessage(pair);
                }
                return true;
            }
            @Override
            public String toString() {
                return "Router broadcast job";
            }
        },30000,1);  // every 30 seconds
        schedulePeriodic(utilityExecutor,PollerStats.INSTANCE.getHourlyByApplianceRunnable(),60*60*1000,2);
	}

	// need to get rid of this at some point!
    private void schedulePeriodic(final ExecutorService executor, final Callable<Boolean> runnable, final long periodMs, final long initialDelayMs) {
        scheduleExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                executor.submit(runnable);
            }
        },initialDelayMs,periodMs,TimeUnit.MILLISECONDS);
    }
	
	private void schedulePoller(final LogicalAppliance logical, final Poller poller, final float intervalInSec) {
        long startingOffsetMs = (int)(Math.random()*intervalInSec*1000);  // used for both
        long intervalMs = (long)(intervalInSec*1000);
        logger.info(String.format("Scheduling %s: every %.1f sec, offset of %dms on %s",poller,intervalInSec,startingOffsetMs,logical));
        ScheduledPollerHandle handle = ScheduledPollerHandle.schedulePoller(new PollerRunnable(logical.getPrimary(),poller,intervalMs),startingOffsetMs);
        logical.addPollerHandle(poller,handle);
        if (!logical.isStandalone()) {
            ScheduledPollerHandle.schedulePoller(new PollerRunnable(logical.getBackup(),poller,intervalMs),startingOffsetMs);
        }
    }

	private void stopPump() {
	    for (LogicalAppliance logical : logicalAppliances) {
	        for (MessageBus msgBus : logical.getMsgBuses(DestinationType.BOTH)) {
            	LocalMgmtBusListener localBus = msgBus.getLocalMgmtBusListener();
            	if (localBus != null) {
            		logger.info("Shutting down local bus listener");
            		localBus.onPumpShutdown();	
            	}
	            for (VpnConnection connection : msgBus.getConnManager().getAllVpnConnections()) {
	                if (connection != null) connection.disconnect();
	            }
	        }
	    }
	    pumpExecutor.shutdownNow();
	    pollerExecutor.shutdownNow();
	    scheduleExecutor.shutdownNow();
	}

    /**
     * This Runnable will get executed when the JVM shuts down, try to close things gracefully.
     */
    private class PumpShutdownHookRunnable implements Runnable {

        @Override
        public void run() {
            logger.info("ShutdownHook called... begin stopPump()");
            if (startedFlag) stopPump();
        }
    }

	private void performInitialQueries(List<LogicalAppliance> clusters) {
	    for (LogicalAppliance appliance : clusters) {
	        pollerExecutor.scheduleWithFixedDelay(new ReachableRunnable(appliance.getPrimary()),0,StatsPumpConstants.REACHABLE_POLLER_INTERVAL_SEC,TimeUnit.SECONDS);
	        // these are all run once... we'll run them periodically again later
	        if (!appliance.isStandalone()) {
	            pollerExecutor.scheduleWithFixedDelay(new ReachableRunnable(appliance.getBackup()),0,StatsPumpConstants.REACHABLE_POLLER_INTERVAL_SEC,TimeUnit.SECONDS);
	        }
	    }
	}
	
//	public static void main(String... args) {
//	    if (args.length < 3) {
//	        System.err.println();
//	        System.err.println("Usage: StatsPump <poller_config.xml> <poller_group_config.xml> <appliance_config.xml>");
//	        System.exit(-1);
//	    }
//        logger.info("######################################################################");
//        logger.info("      _________ __          __         __________                      ");
//        logger.info("     /   _____//  |______ _/  |_  _____\\______   \\__ __  _____ ______  ");
//        logger.info("     \\_____  \\\\   __\\__  \\\\   __\\/  ___/|     ___/  |  \\/     \\\\____ \\ ");
//        logger.info("     /        \\|  |  / __ \\|  |  \\___ \\ |    |   |  |  /  Y Y  \\  |_> >");
//        logger.info("    /_______  /|__| (____  /__| /____  >|____|   |____/|__|_|  /   __/ ");
//        logger.info("            \\/           \\/          \\/                      \\/|__|    ");
//        logger.info("                                      Solace SEMP Data Republishing Tool");
//        logger.info("");
//        logger.info("                                    - Solace Professional Services Group");
//        logger.info("/*");
//        logger.info(" * Copyright 2014-2016 Solace Systems, Inc. All rights reserved.");
//        logger.info(" *");
//        logger.info(" * http://www.solacesystems.com");
//        logger.info(" *");
//        logger.info(" * This program is distributed under the terms and conditions");
//        logger.info(" * of any contract or contracts between Solace Systems, Inc.");
//        logger.info(" * (\"Solace\") and you or your company. If there are no");
//        logger.info(" * contracts in place use of this program is not authorized.");
//        logger.info(" * No support is provided and no distribution, sharing with");
//        logger.info(" * others or re-use of this program is authorized unless");
//        logger.info(" * specifically stated in the contracts referred to above.");
//        logger.info(" *");
//        logger.info(" * This product is provided as is and is not supported by");
//        logger.info(" * Solace unless such support is provided for under an");
//        logger.info(" * agreement signed between you and Solace.");
//        logger.info(" */");
//        logger.info("");
//        logger.info("##### StatsPump main() starting");
//        String pollerFilename = args[0];
//        String pollerGroupFilename = args[1];
//        String applianceFilename = args[2];
//        try {
//    	    SempReplySchemaLoader.loadSchemas();
//    	    SempRpcSchemaLoader.INSTANCE.loadSchemas();
//    	    try {
//    	    	PumpManager.INSTANCE.load(pollerFilename,pollerGroupFilename,applianceFilename);
//            } catch (ConfigLoaderException e) {
//                logger.error("There was an issue loading the configuration files",e);
//                logger.error("The system will now exit");
//                System.exit(-1);
//            }
//    		INSTANCE.start();
//    		try {
//    			while (true) {
//    				Thread.sleep(120000);
//    			}
//    		} catch (InterruptedException e) {
//    		    logger.warn("StatsPump main() got interrupted.");
//    		}
//    		INSTANCE.stopPump();
//        } catch (RuntimeException e) {
//            logger.error("An uncaught RuntimeException got thrown out to the StatsPump main()",e);
//        }
//	}
    
    
	public void	configure(StatsConfigStreams loader) throws ConfigLoaderException {
		logger.info("######################################################################");
        logger.info("      _________ __          __         __________                      ");
        logger.info("     /   _____//  |______ _/  |_  _____\\______   \\__ __  _____ ______  ");
        logger.info("     \\_____  \\\\   __\\__  \\\\   __\\/  ___/|     ___/  |  \\/     \\\\____ \\ ");
        logger.info("     /        \\|  |  / __ \\|  |  \\___ \\ |    |   |  |  /  Y Y  \\  |_> >");
        logger.info("    /_______  /|__| (____  /__| /____  >|____|   |____/|__|_|  /   __/ ");
        logger.info("            \\/           \\/          \\/                      \\/|__|    ");
        logger.info("                                      Solace SEMP Data Republishing Tool");
        logger.info("");
        logger.info("                                    - Solace Professional Services Group");
        logger.info("/*");
        logger.info(" * Copyright 2014-2017 Solace Systems, Inc. All rights reserved.");
        logger.info(" *");
        logger.info(" * http://www.solace.com");
        logger.info(" *");
        logger.info(" * This program is distributed under the terms and conditions");
        logger.info(" * of any contract or contracts between Solace Systems, Inc.");
        logger.info(" * (\"Solace\") and you or your company. If there are no");
        logger.info(" * contracts in place use of this program is not authorized.");
        logger.info(" * No support is provided and no distribution, sharing with");
        logger.info(" * others or re-use of this program is authorized unless");
        logger.info(" * specifically stated in the contracts referred to above.");
        logger.info(" *");
        logger.info(" * This product is provided as is and is not supported by");
        logger.info(" * Solace unless such support is provided for under an");
        logger.info(" * agreement signed between you and Solace.");
        logger.info(" */");
        logger.info("");
        logger.info("##### StatsPump main() starting");

    	SempReplySchemaLoader.loadSchemas();
	    SempRpcSchemaLoader.INSTANCE.loadSchemas();
	    try {
	    	load(loader);
        } catch (Exception e) {
        	throw new ConfigLoaderException(e);
        }
	}
	
	@SuppressWarnings("unused")
	private StringBuffer getStreamAsBuffer(InputStream stream) throws IOException {
		String str = "";
		StringBuffer buf = new StringBuffer();            
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            if (stream != null) {                            
                while ((str = reader.readLine()) != null) {    
                    buf.append(str + "\n" );
                }                
            }
        } 
        finally {
            try { 
            	stream.close(); 
            } 
            catch (Throwable ignore) {}
        }
        return buf;
	}
	
	
	
	public void	start() {
		startPump();		
	}
	public void	stop() {
		stopPump();
	}
}

