/**
 * Copyright 2016-2017 Solace Corporation. All rights reserved.
 *
 * http://www.solace.com
 *
 * This source is distributed under the terms and conditions of any contract or
 * contracts between Solace Corporation ("Solace") and you or your company. If
 * there are no contracts in place use of this source is not authorized. No
 * support is provided and no distribution, sharing with others or re-use of 
 * this source is authorized unless specifically stated in the contracts 
 * referred to above.
 *
 * This software is custom built to specifications provided by you, and is 
 * provided under a paid service engagement or statement of work signed between
 * you and Solace. This product is provided as is and is not supported by 
 * Solace unless such support is provided for under an agreement signed between
 * you and Solace.
 */
package com.solace.psg.enterprisestats.receiver;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solace.psg.enterprisestats.receiver.utils.ReceiverUtils;
import com.solacesystems.jcsmp.Destination;
import com.solacesystems.jcsmp.MapMessage;
import com.solacesystems.jcsmp.SDTException;
import com.solacesystems.jcsmp.SDTMap;

/**
 * This class encapsulates the "callable" requirement for a task to be used in
 * an executor's thread pool. It simply implements the callable interface, and
 * calls the Tap's onStatsAvailable method.
 */
class TapWorkerThreadedTask implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(TapWorkerThreadedTask.class);

    private MapMessage m_mapMsg;
    private StatsTap m_tapPlugIn;
    private final long nThisTaskNumber;
    private final String thisThreadName;
    private static AtomicLong nTaskCount = new AtomicLong(0);
    private StatsReceiver m_statsReceiver;
    private DbFieldListNonHierarchial m_forcedNumerics;
    private DbFilter m_dbFilter;
    private AtomicBoolean hasShownNoDestinationWarning = new AtomicBoolean(false); 

    /**
     * Constructor
     * 
     * @param statsReceiver
     *            The main stats receiver.
     * @param mapMsg
     *            The received stats map message.
     */
    public TapWorkerThreadedTask(StatsReceiver statsReceiver, MapMessage mapMsg) {
        this.m_statsReceiver = statsReceiver;
        this.m_tapPlugIn = statsReceiver.getStatsTap();
        this.m_mapMsg = mapMsg;
        this.m_dbFilter = m_statsReceiver.getDbFilter();
        this.m_forcedNumerics = m_statsReceiver.getForcedNumerics();
        // generate a name for the thread based on the tap class name and
        // a unique integer
        nThisTaskNumber = nTaskCount.addAndGet(1);
        thisThreadName = m_tapPlugIn.getClass().getSimpleName() + "-" + nThisTaskNumber;
    }
    
    public long getTaskNumber() { 
    	return this.nThisTaskNumber; 
    }

    /**
     * Callable interface. This method is called by the Java thread executor
     * when a thread in the pool is available.
     * 
     * @returns 0 for success, -1 for failure
     */
    @Override
    public Integer call() throws Exception {
        Integer rc = 0;
        Thread.currentThread().setName(thisThreadName);
        try {
            // call the tap class to write these stats out somewhere
            processStats(m_mapMsg);
            logger.trace("Tap Thread '" + thisThreadName + "' has finished.");
        } catch (Exception e) {
            logger.error("Tap failure while processing Stats", e);
            rc = -1;
        } finally {
            // Acknowledge the message if received from a queue
            if (m_statsReceiver.isPersistentMode()) {
                m_mapMsg.ackMessage();
				logger.debug("ACKed message for task " + getTaskNumber());
            }
        }
        return rc;
    }

    private void processPollerBroadcastMessage(MapMessage mapMsg) throws ReceiverException {
        List<String> pollerLevels;
        try {
            pollerLevels = ReceiverUtils.getLevels(mapMsg.getMap().getStream("topic-levels"));
        } catch (SDTException e) {
            logger.error("Error while processing boardcast message - " + e.getMessage());
            throw new ReceiverException(e);
        }

        // Get the short topic name like SYSTEM/INTERFACE, or VPN/QUEUE_DETAIL
        String topicName = ReceiverUtils.getShortName(pollerLevels);

        logger.debug("Received a poller broadcast message for topic: " + topicName);

        try {
            m_statsReceiver.processPollerBroadcastMessage(topicName, pollerLevels, mapMsg.getMap().getString("name"));
        } catch (SDTException e) {
            logger.error("Poller boadcast map message does not contain the key 'name'");
            throw new ReceiverException(e);
        }
    }

    private void processPollerStatsMessage(MapMessage mapMsg, List<String> levels)
            throws SDTException, ReceiverException {
        String name = mapMsg.getMap().getString("name");
        logger.debug("Receiver processing a poller stats message for: " + name);

        String topicName = m_statsReceiver.getPollerTopicName(name);
        if (topicName != null && !topicName.isEmpty()) {
            Map<String, Object> keyValMap = new HashMap<String, Object>();
            ReceiverUtils.parseSdtMap(mapMsg.getMap(), keyValMap);
            m_statsReceiver.incrementInternalStats("poller-" + name);

            Long messageTimeStamp = mapMsg.getSenderTimestamp();
            if (messageTimeStamp == null) {
            	// likley we are running inside the pump as a "local bus". Normally the timestamp is put on automatically
            	// by the Solace API layer during sends, but since this message was never sent, it has no timestamp.
            	// Lets just say 'now' and call it close it enough.
            	messageTimeStamp = System.currentTimeMillis();
            }

            Map<String, String> tags = Collections.singletonMap("ROUTER_NAME", levels.get(2));

            StatsMessage msg = new StatsMessage(topicName, tags, keyValMap, messageTimeStamp);
            this.m_tapPlugIn.onPumpStats(msg);
            // updateGroupInfluxDb("POLLER_STATS",Collections.singletonList(new
            // Measurement(topicName,Collections.singletonMap("ROUTER_NAME",levels.get(2)),keyValMap,mapMsg)));
        } else {
            logger.debug(
                    "Receiver discarding poller stat message because the pollerNameMap does not contain this name.");
        }
    }

    private void processRegularStatsMessage(MapMessage mapMsg, List<String> levels) throws ReceiverException {
        // first, lets verify that this is a legitimate stats message
        if (!mapMsg.getProperties().containsKey("topic-levels")) {
            // logger.debug("Unexpected message received: " + mapMsg.dump());
            throw new ReceiverException(
                    "An unexpected message was received. It contains no topic-levels in the message header.");
        }

        // now, lets confirm we have poller info for this stats message type
        String measurementName = ReceiverUtils.getShortName(levels);
        List<String> pollerLevels = m_statsReceiver.getPollerInfo(measurementName);
        if (pollerLevels == null || pollerLevels.isEmpty()) {
            // not sure how that could have them... we should get all the Poller
            // infos all at once
            // logger.debug("Unexpected message received: " + mapMsg.dump());
            // TODO: provide a bit more details about the message in the log
            logger.debug("No poller info has been cached for this message type. The message cannot be processed.");
        } else {
            // We have the poller information for this message so now we need:
            // - database (SYSTEM, or VPN, or VPN+name)
            // - measurement name (DETAIL, or QUEUE)
            // - any replaceable elements as tags
            //
            Map<String, String> tags = new HashMap<String, String>();
            for (int i = 0; i < pollerLevels.size(); i++) {
                String level = pollerLevels.get(i);
                if (level.startsWith("~")) { // this will become an InfluxDB
                                             // "tag" (i.e. indexable attribute)
                    String tag = level.substring(1, level.length() - 1);
                    String tagVal = levels.get(i);
                    tags.put(tag, tagVal);
                }
            }
            Map<String, Object> keyValMap = new HashMap<String, Object>();
            try {
                ReceiverUtils.parseSdtMap(mapMsg.getMap(), keyValMap);
            } catch (SDTException e) {
                throw new ReceiverException(e);
            }

            Map<String, Object> filteredKeyValMap = filterDbKeys(measurementName, keyValMap);
            if (!filteredKeyValMap.isEmpty()) {
                m_statsReceiver.incrementInternalStats("router-" + measurementName);
                Long messageTimeStamp = mapMsg.getSenderTimestamp();
                
                if (messageTimeStamp == null) {
                	// likley we are running inside the pump as a "local bus". Normally the timestamp is put on automatically
                	// by the Solace API layer during sends, but since this message was never sent, it has no timestamp.
                	// Lets just say 'now' and call it close it enough.
                	messageTimeStamp = System.currentTimeMillis();
                }

                StatsMessage msg = new StatsMessage(measurementName, tags, filteredKeyValMap, messageTimeStamp);
                this.m_tapPlugIn.onRouterStats(msg);

                // Measurement thisMeasurement = new
                // Measurement(measurementName,tags,filteredKeyValMap,mapMsg);
                // List<Measurement> theListToInsert =
                // Collections.singletonList(thisMeasurement);
                // updateGroupInfluxDb(db_db,theListToInsert);
            } else {
                logger.debug("Dropping entire measurement - no subscription match: " + measurementName);
            }
        }
    }

    private void processPollerBroadcastStartMessage(MapMessage mapMsg, List<String> levels)
            throws SDTException, ReceiverException {
    	SDTMap map = mapMsg.getMap();
        String longname = map.getString("name");
        String shortname = levels.get(4);
        String host = map.getString("hostname");

        Map<String, Object> keyValMap = new HashMap<String, Object>();
        ReceiverUtils.parseSdtMap(mapMsg.getMap(), keyValMap);
        m_statsReceiver.incrementInternalStats("start-" + longname);

        logger.debug("Receiver processing poller Start message " + shortname + "," + host + "," + longname);
        this.m_tapPlugIn.onPollerStart(host, shortname, longname);
    }
    private void processPollerBroadcastEndMessage(MapMessage mapMsg, List<String> levels)
            throws SDTException, ReceiverException {
    	SDTMap map = mapMsg.getMap();
        String longname = map.getString("name");
        String shortname = levels.get(4);
        String host = map.getString("hostname");
        String retCode = map.getString("return-code");
        boolean bRc = false; 
        
        try {
            bRc = Boolean.parseBoolean(retCode);
        }
        catch (Exception e) {
            logger.debug("unable to convert return-code value for poller end message '" + retCode + "'.");
        }
        
        Map<String, Object> keyValMap = new HashMap<String, Object>();
        ReceiverUtils.parseSdtMap(mapMsg.getMap(), keyValMap);
        m_statsReceiver.incrementInternalStats("end-" + longname);

        logger.debug("Receiver processing a poller End message " + shortname + "," + host + "," + longname + " rc=" + bRc);
        this.m_tapPlugIn.onPollerEnd(host, shortname, longname, bRc);
    }
    
    private void processStats(MapMessage mapMsg) throws ReceiverException {
        try {
        	Destination d = mapMsg.getDestination();
        	if (d == null) {
        		if (hasShownNoDestinationWarning.get() == false) {
        			logger.debug("message has no destination, assuming running as a local Pump process plug in");
        			hasShownNoDestinationWarning.set(true);
        		}
        	}
        	else {
            	logger.debug("received a message on " + mapMsg.getDestination().getName());
        	}
        	
            SDTMap props = mapMsg.getProperties();
            List<String> levels = ReceiverUtils.getLevels(props.getStream("topic-levels"));

            // ready to go
            String type = levels.get(1);
            String level3 = levels.get(3);
            if (type.equals("SYSTEM") || type.equals("VPN")) {
                processRegularStatsMessage(mapMsg, levels);
            }
            else if (type.equals("BROADCAST")) {
            	if (level3.equals("POLLERS")) {
            		processPollerBroadcastMessage(mapMsg);
            	} else if (level3.equals("POLLER_START")) {
            		processPollerBroadcastStartMessage(mapMsg, levels);
            	} else if (level3.equals("POLLER_END")) {
            		processPollerBroadcastEndMessage(mapMsg, levels);
            	}
            }
            /*
             * Important: there is a defect in the pump that is being remedied
             * here.
             * 
             * The pump should have created the levels as : [#STATS, PUMP,
             * lab-128-48, POLLER_STAT, SYSTEM/VERSION]. However, it is
             * currently sending out: [#STATS, PUMP, lab-128-48,
             * POLLER_STAT/SYSTEM/VERSION].
             * 
             * In the interests of lowering risk, a fix is being made here, that
             * ideally would belong in Pump. At this late stage in the delivery
             * of sofware to SCB, it is too risky to change the Pump. Here are
             * Aaron's notes on the proper fix:
             * 
             * Aaron Lee: I think the easiest (maybe not best though) place to
             * make the change is in the helper utility methods inside
             * MessageUtils.java, under util: the publishMapMessage() and
             * publishListMessage() methods. This line:
             * 
             * StatsPumpMessage msg = new
             * StatsPumpMessage(type,physical,destination,containerSet,
             * Collections.singletonList(restOfTopic),System.currentTimeMillis()
             * );
             * 
             * Should check to see if the type MessageType is a PUMP message,
             * and if so, split the restOfTopic String by "/" once, and pass to
             * the StatsPumpMessaage constructor. Something like:
             * 
             * StatsPumpMessage msg; if (type == MessageType.PUMP) { String[]
             * levels = restOfTopic.split("/",2); msg = new
             * StatsPumpMessage(type,physical,destination,containerSet,Arrays.
             * asList(levels),System.currentTimeMillis()); } else { msg = new
             * StatsPumpMessage(type,physical,destination,containerSet,
             * Collections.singletonList(restOfTopic),System.currentTimeMillis()
             * ); }
             *
             * In the meantime, the line below has been changed from
             * levels.get(3).equals("POLLER_STAT") to
             * levels.get(3).contains("POLLER_STAT")
             */
            else if (m_statsReceiver.isPollerStatsEnabled() && levels.get(1).equals("PUMP")
                    && levels.get(3).contains("POLLER_STAT")) {
                // eg #STATS/PUMP/*/POLLER_STAT/>
                processPollerStatsMessage(mapMsg, levels);
            } else {
                // toss it!
                logger.debug("Discarding unknown message.");
            }
        } catch (SDTException ex) {
            throw new ReceiverException(ex);
        }
    }

    private String forceToNumericValue(String value) {
    	String valueRC = value;
    	if (valueRC.contains("%")) {
    		valueRC = valueRC.replace("%", "");
    	}

    	if (valueRC.contains("n/a")) {
    		valueRC = valueRC.replace("n/a", "");
    	}

    	if (valueRC.contains("N/A")) {
    		valueRC = valueRC.replace("N/A", "");
    	}

    	if (valueRC.trim().equals("-")) {
    		valueRC = valueRC.replace("-", "");
    	}
    	valueRC = valueRC.trim();
    	return valueRC;
    }
    
   

	private Map<String, Object> filterDbKeys(String measurementName, Map<String, Object> keyValMap) {
		Map<String, Object> filteredMap = new HashMap<String, Object>();
		for (String key : keyValMap.keySet()) {
			String topic = measurementName + "/" + key;
			if (m_dbFilter.lookup(topic)) {
				Object value = keyValMap.get(key);;

				// ok, the field is in scope based on field filtering. Now, let's see if it needs to be forced
				// to a numeric value
				
				if (this.m_forcedNumerics.contains(topic)) {
					String strValue = value.toString();
					String newValue = forceToNumericValue(strValue);

					// was it actually changed?
					if (newValue.equals(strValue) == false) {
						value = newValue;
						
						// ok, here is where we need to actually convert
						if (TypeChecker.isDouble(newValue)) {
							value = new Double(newValue);
						}
						else if (TypeChecker.isLong(newValue)) {
							value = new Long(newValue);
						}
						else if (TypeChecker.isInt(newValue)) {
							value = new Integer(newValue);
						}
						else if (TypeChecker.isShort(newValue)) {
							value = new Short(newValue);
						}
						else if (TypeChecker.isBoolean(newValue)) {
							value = new Boolean(newValue);
						} 
						
						logger.debug("The field value for " + topic + " was forced from '" + strValue
								+ "' to '" + value.getClass().toString() + "' value '" + value + "'.");
					}
					else {
						logger.debug("The field value for " + topic + " was configured to be forced to a numeric value, but the '" + value
								+ "' does not need to be changed.");
					}
				}
				filteredMap.put(key, value);
			}
		}

		return filteredMap;
	}
}