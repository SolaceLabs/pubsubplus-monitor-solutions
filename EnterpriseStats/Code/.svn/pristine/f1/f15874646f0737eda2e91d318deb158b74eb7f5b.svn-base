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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solace.psg.enterprisestats.statspump.PollerRunnable.PollerReturnCode;
import com.solace.psg.enterprisestats.statspump.StatsPumpMessage.MessageType;
import com.solace.psg.enterprisestats.statspump.config.xml.DestinationType;
import com.solace.psg.enterprisestats.statspump.config.xml.RunCondition;
import com.solace.psg.enterprisestats.statspump.pollers.Poller;
import com.solace.psg.enterprisestats.statspump.pollers.Poller.Scope;
import com.solace.psg.enterprisestats.statspump.semp.GenericSempSaxHandler;
import com.solace.psg.enterprisestats.statspump.semp.SempSchemaValidationException;
import com.solace.psg.enterprisestats.statspump.stats.PollerStat;
import com.solace.psg.enterprisestats.statspump.stats.PollerStats;
import com.solace.psg.enterprisestats.statspump.stats.PollerStat.Stat;
import com.solace.psg.enterprisestats.statspump.tools.comms.HttpUtils;
import com.solace.psg.enterprisestats.statspump.tools.comms.SempRequestException;
import com.solace.psg.enterprisestats.statspump.tools.parsers.SaxParser;
import com.solace.psg.enterprisestats.statspump.tools.parsers.SaxParserException;
import com.solace.psg.enterprisestats.statspump.util.MessageUtils;
import com.solace.psg.enterprisestats.statspump.util.NonXmlBufferedReader;
import com.solace.psg.enterprisestats.statspump.util.StatsPumpConstants;

/**
 * This class takes in a poller, keeps track of how often it is supposed to run,
 * and provides a Runnable interface to kick off the scheduling of the SEMP
 * request and XML parsing.
 */
public class PollerRunnable implements Callable<PollerReturnCode> {
    private static final Logger logger = LoggerFactory.getLogger(PollerRunnable.class);

    public enum PollerReturnCode {
        SUCCESS, FAILURE, NOT_RUNNING,;
    }

    private final static Charset CHARSET = Charset.forName("UTF-8");

    private final PhysicalAppliance physical;
    private final Poller poller;
    private final long desiredIntervalMs;

    /**
     * 
     * @param physical
     *            the actual physical appliance to run this poller against
     * @param poller
     *            the poller to run
     * @param desiredIntervalMs
     *            how often to run, in ms
     */
    public PollerRunnable(PhysicalAppliance physical, Poller poller, long desiredIntervalMs) {
        this.physical = physical;
        this.poller = poller;
        this.desiredIntervalMs = desiredIntervalMs; // should this should be
                                                    // kept at the
                                                    // LogicalAppliance? so both
                                                    // sides are kept in sync?
    }

    public PhysicalAppliance getAppliance() {
        return physical;
    }

    public Poller getPoller() {
        return poller;
    }

    public long getIntervalMs() {
        return desiredIntervalMs;
    }

    @Override
    public String toString() {
        return String.format("%s on %s", poller, physical);
    }

    private boolean shouldRun(RunCondition runCondition) {
        switch (runCondition) {
        case ALWAYS:
            return true;
        case NEVER: {
            logger.debug(String.format("Not running %s: configured to never run on %s (%s) when %s", this, physical,
                    physical.isPrimary() ? "Primary" : "Backup", physical.getLogical().getType()));
            return false;
        }
        case PRIMARY_LOCAL_ACTIVE:
            if (physical.isPrimaryActive()) {
                return true;
            } else {
                logger.debug(String.format(
                        "Not running %s: only run if Primary Virtual Router 'Local-Active' and %s is '%s'", this,
                        physical, physical.getPrimaryActivity()));
                return false;
            }
        case BACKUP_LOCAL_ACTIVE:
            if (physical.isBackupActive()) {
                return true;
            } else {
                logger.debug(
                        String.format("Not running %s: only run if Backup Virtual Router 'Local-Active' and %s is '%s'",
                                this, physical, physical.getBackupActivity()));
                return false;
            }
        case AD_ACTIVE:
            if (physical.isAdActive()) {
                return true;
            } else {
                logger.debug(String.format("Not running %s: only run if AD-Active and %s is '%s'", this, physical,
                        physical.getAdActivity()));
                return false;
            }
        default:
            logger.error("I'm in the default block of this..!");
            return false;
        }
    }

    private boolean shouldPollerRun() {
        // do a SEMP version check!
        if (!physical.getSempVersion().isValid(poller.getMinSempVersion(), poller.getMaxSempVersion())) {
            logger.debug(String.format("Not running %s: SolTR version %s not in range [%s,%s]", this,
                    physical.getSempVersion(), poller.getMinSempVersion(), poller.getMaxSempVersion()));
            return false;
        }
        // if this poller is only for SELF, do we have SELF message buses
        // configured?
        if (physical.getLogical().getMsgBuses(poller.getDestination()).isEmpty()) {
            logger.debug(String.format("Not running %s: no Message Buses configured for Poller destination %s", this,
                    poller.getDestination()));
            return false;
        }
        if (physical.isPrimary()) {
            switch (physical.getLogical().getType()) {
            case ACTIVE_ACTIVE:
                return shouldRun(poller.runOnPrimaryWhenActiveActive());
            case ACTIVE_STANDBY:
            case STANDALONE:
                return shouldRun(poller.runOnPrimaryWhenActiveStandby());
            default:
                logger.error("I'm in the default block of shouldPollerRun primary..!");
                return false;
            }
        } else { // this is the backup router
            switch (physical.getLogical().getType()) {
            case ACTIVE_ACTIVE:
                return shouldRun(poller.runOnBackupWhenActiveActive());
            case ACTIVE_STANDBY:
            case STANDALONE:
                return shouldRun(poller.runOnBackupWhenActiveStandby());
            default:
                logger.error("I'm in the default block of shouldPollerRun backup..!");
                return false;
            }
        }
    }

    @Override
    public PollerReturnCode call() {
        if (!shouldPollerRun())
        	// don't send poller start/end messages if not running
            return PollerReturnCode.NOT_RUNNING;
        String sempRequest = String.format(poller.getSempRequest(), physical.getSempVersion());
        publishPollerStartMessage();
        int requestCount = 0;
        long totalElementCount = 0;
        int totalObjectCount = 0;
        int totalMessageCount = 0;
        float replyStartTime = 0;
        long totalCharCount = 0;
        int fetchTime = 0; // only for non-streaming
        int parseTime = 0; // only for non-streaming

        Map<String, Object> statsMap = new HashMap<String, Object>();
        // statsMap.put(Stat.PER_PAGE_TIME_IN_MS.getName(),new
        // ArrayList<Float>());
        List<Float> perPageFetchTime = new ArrayList<Float>();
        long start = System.nanoTime();
        long perPageStart;
        GenericSempSaxHandler handler = poller.buildSaxHandler(physical);
        BufferedReader reader = null;
        Scanner scanner = null;
        while (sempRequest != null) {
            try {
                perPageStart = System.nanoTime();
                handler.setMessageTimestamp(System.currentTimeMillis());
                reader = new NonXmlBufferedReader(
                        new InputStreamReader(
                                new BufferedInputStream(
                                        HttpUtils.performSempStreamRequest(physical.getSempHostnameOrIp(),physical.isSecureSession(),
                                                physical.getSempUsername(), physical.getSempPassword(), sempRequest)),
                                CHARSET),
                        this);
                try {
                    if (StatsPumpConstants.STREAMING_HTTP_FETCH) {
                        // means that we open a stream and pipe it directly into
                        // the SAX parser (rather than reading the whole HTTP
                        // response into a String, and then parsing)
                        SaxParser.parseReader(reader, handler);
                    } else {
                        // saw this method for efficiently reading a Stream to
                        // String from StackOverflow... time delta between
                        // streaming vs. not is very small now
                        scanner = new Scanner(reader);
                        scanner.useDelimiter("\\A"); // scan until "beginning of
                                                     // input boundary"
                        String reply = scanner.hasNext() ? scanner.next() : "";
                        fetchTime += System.nanoTime() - perPageStart;
                        long parseStart = System.nanoTime();
                        SaxParser.parseString(reply, handler);
                        parseTime += System.nanoTime() - parseStart;
                    }
                } catch (SempSchemaValidationException e) {
                    // perhaps the schema version on the appliance just changed?
                    // check by generating a new SEMP query
                    String newSempRequest = String.format(poller.getSempRequest(), physical.getSempVersion());
                    if (newSempRequest.equals(sempRequest)) {
                        throw e; // same, which means SEMP version didn't
                                 // change, which means actual SEMP validation
                                 // error!
                    } else {
                        // sempVersion has changed, so resubmit SEMP request
                        sempRequest = newSempRequest;
                        continue;
                    }
                }
                // logger.debug(String.format("Difference between request and
                // 1st byte for %s on %s:
                // %.1fms",poller,physical,((reader.getNanoStart()-perPageStart)/1000000.0f)));
                if (requestCount == 0)
                    replyStartTime = (((NonXmlBufferedReader) reader).getNanoStart() - perPageStart) / 1000000.0f; // only
                                                                                                                   // get
                                                                                                                   // the
                                                                                                                   // time
                                                                                                                   // when
                                                                                                                   // the
                                                                                                                   // reply
                                                                                                                   // started
                                                                                                                   // arriving
                                                                                                                   // on
                                                                                                                   // the
                                                                                                                   // first
                                                                                                                   // page
                perPageFetchTime.add((System.nanoTime() - perPageStart) / 1000000.0f);
                requestCount++;
                totalElementCount += handler.getElementCount();
                totalObjectCount += handler.getObjectCount();
                totalMessageCount += handler.getMessageCount();
                totalCharCount += ((NonXmlBufferedReader) reader).getCharCount();
                // totalBytes += inputStream.getByteCount();
                sempRequest = handler.getMoreCookie();
            } catch (SempRequestException e) {
                logger.info(String.format("Caught on %s in %sstreaming-mode while fetching due to: %s", this,
                        StatsPumpConstants.STREAMING_HTTP_FETCH ? "" : "non-", e.getCause()));
                publishPollerEndMessage(PollerReturnCode.FAILURE);
                return PollerReturnCode.FAILURE;
            } catch (SempSchemaValidationException e) {
                logger.debug(String.format("Caught on %s in %sstreaming-mode while parsing due to: %s", this,
                        StatsPumpConstants.STREAMING_HTTP_FETCH ? "" : "non-", e));
                publishPollerEndMessage(PollerReturnCode.FAILURE);
                return PollerReturnCode.FAILURE;
            } catch (SaxParserException e) {
                logger.info(String.format("Caught on %s in %sstreaming-mode while parsing due to: %s", this,
                        StatsPumpConstants.STREAMING_HTTP_FETCH ? "" : "non-", e));
                publishPollerEndMessage(PollerReturnCode.FAILURE);
                return PollerReturnCode.FAILURE;
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException ignore) {
                    }
                }
                if (scanner != null) {
                    scanner.close();
                }
            }
        }
        publishPollerEndMessage(PollerReturnCode.SUCCESS);
        // publish some stats on how long that took
        if (totalElementCount > 0) {
            PollerStat.Builder builder = new PollerStat.Builder();
            statsMap.put(Stat.POLLER_RUN.toString(), 1);
            builder.addStat(Stat.POLLER_RUN, 1);
            statsMap.put(Stat.TOTAL_TIME_IN_MS.toString(), (System.nanoTime() - start) / 1000000.0f);
            builder.addStat(Stat.TOTAL_TIME_IN_MS, (System.nanoTime() - start) / 1000000.0f);
            if (!StatsPumpConstants.STREAMING_HTTP_FETCH) {
                statsMap.put(Stat.FETCH_TIME_IN_MS.toString(), fetchTime / 1000000.0f);
                builder.addStat(Stat.FETCH_TIME_IN_MS, fetchTime / 1000000.0f);
                statsMap.put(Stat.PARSE_TIME_IN_MS.toString(), parseTime / 1000000.0f);
                builder.addStat(Stat.PARSE_TIME_IN_MS, parseTime / 1000000.0f);
            }
            // statsMap.put(Stat.PER_PAGE_TIME_IN_MS.getName(),perPageFetchTime);
            statsMap.put(Stat.REQUEST_COUNT.toString(), requestCount);
            statsMap.put(Stat.CHAR_COUNT.toString(), totalCharCount);
            statsMap.put(Stat.ELEMENT_COUNT.toString(), totalElementCount);
            statsMap.put(Stat.OJBECT_COUNT.toString(), totalObjectCount);
            statsMap.put(Stat.MESSAGE_COUNT.toString(), totalMessageCount);
            statsMap.put(Stat.REPLY_START_TIME_MS.toString(), replyStartTime);
            builder.addStat(Stat.REQUEST_COUNT, requestCount);
            builder.addStat(Stat.CHAR_COUNT, totalCharCount);
            builder.addStat(Stat.ELEMENT_COUNT, totalElementCount);
            builder.addStat(Stat.OJBECT_COUNT, totalObjectCount);
            builder.addStat(Stat.MESSAGE_COUNT, totalMessageCount);
            builder.addStat(Stat.REPLY_START_TIME_MS, replyStartTime);
            publishPollerStatMessage(statsMap, handler.getMessageTimestamp());
            PollerStats.INSTANCE.addStat(poller, physical, builder.build());
        } else { // probably an error happened
            // TODO do something? send a 'no-op'..?
            logger.debug("An element count of 0 encountered on " + this);
        }
        return PollerReturnCode.SUCCESS;
    }

    // @Override
    // public Boolean call() {
    // if (runningFlag.compareAndSet(false,true)) {
    // } else {
    // logger.info(String.format("%s STILL RUNNING! It has been %d ms since it
    // started, but is supposed to run every %d ms",
    // this,System.currentTimeMillis()-pollStartTime,desiredIntervalMsec));
    // //TODO if it has taken more than 5x the desired interval, assume it dead
    // (or try to kill it) and start it anyway
    // return false;
    // }
    // pollStartTime = System.currentTimeMillis();
    // try {
    // return runPerAppliance(); // will return false if something went wrong
    // } catch (RuntimeException e) {
    // logger.error(String.format("Unexpected RuntimeException caught when
    // running %s after %d
    // ms",this,System.currentTimeMillis()-pollStartTime),e);
    // return false;
    // } catch (Throwable e) {
    // logger.fatal("Caught a Throwable in my PollerRunnable! "+this,e);
    // return false;
    // } finally {
    // runningFlag.set(false);
    // if (System.currentTimeMillis()-pollStartTime > desiredIntervalMsec) {
    // logger.info(String.format("*Warning* %s took %.1f sec to run, but is
    // supposed to run every %.1f sec",
    // this,(System.currentTimeMillis()-pollStartTime)/1000f,desiredIntervalMsec/1000f));
    // }
    // }
    // }

    private void publishPollerStatMessage(Map<String, Object> statsMap, long timestamp) {
        Map<String, Object> msgPayload = new HashMap<String, Object>();
        msgPayload.put("hostname", physical.getHostname());
        msgPayload.put("name", poller.getName());
        // map.put("grouped",poller.isGrouped());
        msgPayload.put("streaming", StatsPumpConstants.STREAMING_HTTP_FETCH);
        msgPayload.put("stats", statsMap);
        String topicSuffix = poller.getScope().name() + "/";
        if (poller.getScope().equals(Scope.SYSTEM)) {
            topicSuffix += poller.getTopicLevelsForBroadcast().get(3);
        } else {
            topicSuffix += poller.getTopicLevelsForBroadcast().get(4);
        }
        MessageUtils.publishMapMessage(MessageType.PUMP, physical, DestinationType.MGMT,
        		                       msgPayload, new String[] {StatsPumpConstants.POLLER_STAT_TOPIC,topicSuffix});
        logger.debug(String.format("Stats for %s on %s: %s", poller, physical, statsMap));

        // the following dumps out InfluxDB insert statements
        // statsMap.remove(PollerStat.Stat.PER_PAGE_TIME_IN_MS.getName());
        // String stats =
        // statsMap.toString().substring(1,statsMap.toString().length()-1).replaceAll(",
        // ",",").replaceAll(" ","_");
        // System.out.println(String.format("insert %s,host=%s %s
        // %d",pollerName,physical.getHostname(),stats,(System.currentTimeMillis()*1000000)));
    }
    
    private void publishPollerStartMessage() {
        Map<String, Object> msgPayload = new HashMap<String, Object>();
        msgPayload.put("hostname", physical.getHostname());
        msgPayload.put("name", poller.getName());
        // so: #STATS/BROADCAST/emea1/POLLER_START/SYSTEM/HARDWARE
        String topicSuffix = poller.getScope().name() + "/";
        if (poller.getScope().equals(Scope.SYSTEM)) {
            topicSuffix += poller.getTopicLevelsForBroadcast().get(3); // the 'name' of the topic
        } else {
            topicSuffix += poller.getTopicLevelsForBroadcast().get(4);
        }
        MessageUtils.publishMapMessage(MessageType.BROADCAST, physical, DestinationType.MGMT,
        		                       msgPayload, new String[] {StatsPumpConstants.POLLER_START_BROADCAST_TOPIC,topicSuffix});
    }
    
    private void publishPollerEndMessage(PollerReturnCode returnCode) {
        Map<String, Object> msgPayload = new HashMap<String, Object>();
        msgPayload.put("hostname", physical.getHostname());
        msgPayload.put("name", poller.getName());
        msgPayload.put("return-code",returnCode);
        // so: #STATS/BROADCAST/emea1/POLLER_END/SYSTEM/HARDWARE
        String topicSuffix = poller.getScope().name() + "/";
        if (poller.getScope().equals(Scope.SYSTEM)) {
            topicSuffix += poller.getTopicLevelsForBroadcast().get(3); // the 'name' of the topic
        } else {
            topicSuffix += poller.getTopicLevelsForBroadcast().get(4);
        }
        MessageUtils.publishMapMessage(MessageType.BROADCAST, physical, DestinationType.MGMT,
                                       msgPayload, new String[] {StatsPumpConstants.POLLER_END_BROADCAST_TOPIC,topicSuffix});
    }
}
