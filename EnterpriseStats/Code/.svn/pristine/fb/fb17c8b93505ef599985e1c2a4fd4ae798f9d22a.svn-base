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
package com.solace.psg.enterprisestats.receiver.influxdb;

import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solace.psg.enterprisestats.receiver.AbstractStatsTap;
import com.solace.psg.enterprisestats.receiver.ReceiverException;
import com.solace.psg.enterprisestats.receiver.StatsMessage;
import com.solace.psg.enterprisestats.receiver.StatsReceiverProperties;
import com.solace.psg.enterprisestats.receiver.influxdb.utils.Apache45Client;
import com.solace.psg.enterprisestats.receiver.influxdb.utils.HttpException;
import com.solace.psg.enterprisestats.receiver.stats.InternalStats;
import com.solace.psg.enterprisestats.receiver.utils.ReceiverUtils;

/**
 * Implements the Tap interface, in terms of an InfluxDB writer. InfluxDB is
 * written to using an Http interface.
 */
public class InfluxDBStatsTap extends AbstractStatsTap {
    private static final Logger logger = LoggerFactory.getLogger(InfluxDBStatsTap.class);

    private static final String INFLUX_HOST_PROP_NAME = "INFLUXDB_HOST";
    private static final String INFLUX_DB_PROP_NAME = "INFLUXDB_DB";
    private static final String INFLUX_USER_PROP_NAME = "INFLUXDB_USER";
    private static final String INFLUX_PASSWORD_PROP_NAME = "INFLUXDB_PASSWORD";
    private static final String INFLUX_STUBBED_PROP_NAME = "INFLUX_STUBBED";
    private static final String INFLUX_POLLER_STATS_DB_PROP_NAME = "INFLUXDB_POLLER_STATS_DB";
    private static final String ROUTER_NAME_KEY = "ROUTER_NAME";

    private static final String INFLUX_HTTP_CONNECT_TIMEOUT = "INFLUXDB_HTTP_CONNECT_TIMEOUT";
    private static final String INFLUX_HTTP_READ_TIMEOUT = "INFLUXDB_HTTP_READ_TIMEOUT";
    private static final String INFLUX_HTTP_READ_RETRIES = "INFLUXDB_HTTP_READ_RETRIES";

    private static final String THREAD_POOL_MAX_SIZE = "THREAD_POOL_MAX_SIZE";
    
    private String m_host;
    private String m_user;
    private String m_password;
    private String m_db;
    private String m_pollerStatsDB = "statspump_poller_stats"; // default
    private boolean stubbedOut = false;
    private long measurementsSent = 0;
    private InfluxLogger httpLog = null;
    private InternalStats m_internalStats;
    private int httpRetries = 1;

    /**
     * Simple constructor loaded reflectively by the Framework core
     */
    public InfluxDBStatsTap() {
        m_internalStats = new InternalStats();
    }

    /**
     * Removes the \n char from the 'in' string, replacing it with 0xfffd, which is a universal
     * newline char, which passes into InfluxDb successfully. Standard \n was creating new measurents
     * 
     * @param in
     * @return
     */
    private String sanitizeNewlines(String in) {
    	String value = in;
    	if (value.contains("\n") ) {
			value = value.replace("\n", "\ufffd");
			logger.debug("replacing newline char, value now='" + value + "'");
		}
    	return value;
    }
    /**
     * Builds the body of the content to sent to influxDB for insertion
     * 
     * @param tags
     * @param map
     * @param timestamp
     * @return
     */
    private String buildInfluxDbBody(Map<String, String> tags, Map<String, Object> map, long timestamp) {
        StringBuilder sb = new StringBuilder();
        /*
         * https://influxdb.com/docs/v0.9/write_protocols/write_syntax.html If a
         * tag key, tag value, or field key contains a space , comma ,, or an
         * equals sign = it must be escaped using the backslash character \.
         * Backslash characters do not need to be escaped. Commas , and spaces
         * will also need to be escaped for measurements, though equals signs =
         * do not.
         */
        for (String tag : tags.keySet()) {
            // escape the escape character
            String value = tags.get(tag).replaceAll("([ ,=])", "\\\\$1");
            value = sanitizeNewlines(value);
            sb.append(",").append(tag).append("=").append(value);
        }
        sb.append(" ");
        for (String key : map.keySet()) {
        	
            sb.append(key.replaceAll("([ ,=])", "\\\\$1"));
            if (map.get(key) instanceof String) {
            	String strValue = (String) map.get(key);
    			if (strValue.length() == 0) {
    				logger.debug("Since the value for " + key + " is now blank, the field and value are eliminated from the results set.");
    			}
    			else {
    				String value = (String) map.get(key);
                	value = sanitizeNewlines(value);
    				sb.append("=\"").append(value);
    				sb.append("\",");
    			}
            } else if (map.get(key) instanceof Long || map.get(key) instanceof Integer || map.get(key) instanceof Byte
                    || map.get(key) instanceof Short) {
                sb.append("=").append(map.get(key));
                // put an i after it for integer (I guess?)
                sb.append("i,");
            } else {
                // float, double, or boolean
                sb.append("=").append(map.get(key));
                sb.append(",");
            }
        }
        sb.setLength(sb.length() - 1);
        sb.append(" ");
        sb.append(timestamp);
        return sb.toString();
    }

    /**
     * Sends the data to insert to InfluxDB.
     * 
     * @param db
     * @param measurements
     */
    private void postToInfluxDb(String db, Measurement measurement) throws ReceiverException {
        StringBuilder sb = new StringBuilder();
        sb.append(measurement.getName()).append(buildInfluxDbBody(measurement.getTags(), measurement.getMap(), measurement.getTimestamp())).append("\n");
        
        boolean writeSucceeded = false;
        int nAttempts = 1;
        while (!writeSucceeded) {
	        try {
	            String url = String.format("http://%s/write?db=%s&precision=ms", m_host, db);
	            if (logger.isDebugEnabled()) {
	                logger.debug("POSTing write request to '" + db + "' influxDB =" + sb.toString());
	            }
	
	            if (!stubbedOut) {
	                // the influx server returns no body upon success, just a 200
	                // http code. If it fails, and IOException will be thrown
	                //HttpUtils.httpRequestString(url, m_user, m_password, RequestMethod.POST, sb.toString());
	            	Apache45Client.post(url, m_user, m_password, sb.toString());
	            } else {
	                // for debugging without an Influx installation, just write to
	                // file
	                httpLog.writeLog(sb.toString());
	            }
	            writeSucceeded = true;
	            measurementsSent += 1;
	        } 
	        catch (HttpException e) {
	            Throwable tInner = e.getCause();
	            String err = "FAILED to write update for measurement: " + measurement + ". '" + e.getMessage() + "'.";

		        if (nAttempts == this.httpRetries) {
		        	// that it, rethrow and move on
		        	err += "\nThis statistic will be discarded.";
		            throw new ReceiverException(err, tInner);
		        }
		        else {
		        	// log this and retry
		        	err += "\nThe HTTP request will be retried.";
		        	if (nAttempts == 1) {
		        		// first time this error has occurred, log at warn level
		        		logger.warn(err);
		        	}
		        	else {
		        		// for subsequent retries, just log at debug level - a final error level log will 
		        		// be issued if the max retries have been exhausted
		        		logger.debug(err);
		        	}
		        	nAttempts++;
		        }
	        }
        }
    }

    @Override
    public void initialize(StatsReceiverProperties applicationProps) throws ReceiverException {
        this.m_host = applicationProps.getMandatoryStringProperty(INFLUX_HOST_PROP_NAME);
        this.m_user = applicationProps.getMandatoryStringProperty(INFLUX_USER_PROP_NAME);
        // this.db_pw =
        // applicationProps.getMandatoryStringProperty("INFLUXDB_PW");
        String encryptedPw = applicationProps.getOptionalStringProperty(INFLUX_PASSWORD_PROP_NAME);
        this.m_password = ReceiverUtils.decryptPassword(encryptedPw, INFLUX_PASSWORD_PROP_NAME);
        this.m_db = applicationProps.getMandatoryStringProperty(INFLUX_DB_PROP_NAME);
        String pollerStatsDB = applicationProps.getOptionalStringProperty(INFLUX_POLLER_STATS_DB_PROP_NAME);
        if (pollerStatsDB != null && !pollerStatsDB.isEmpty()) {
            this.m_pollerStatsDB = pollerStatsDB;
        }

        String stubbed = applicationProps.getOptionalStringProperty(INFLUX_STUBBED_PROP_NAME);
        if (stubbed != null && !stubbed.isEmpty()) {
            stubbedOut = stubbed.equalsIgnoreCase("true");
            if (stubbedOut) {
                httpLog = new InfluxLogger();
                logger.info("InfluxDB writing has been stubbed out. Http requests will be logged to '"
                        + httpLog.getFileNameUsed() + "' instead.");
            }
        }
		
		String temp = applicationProps.getOptionalStringProperty(INFLUX_HTTP_CONNECT_TIMEOUT);
		int nConnectTimeout = -1;
		int nReadTimeout = -1;
		if (temp != null) {
			Integer i = Integer.valueOf(temp);
			nConnectTimeout = i.intValue();
		}

		temp = applicationProps.getOptionalStringProperty(INFLUX_HTTP_READ_TIMEOUT);
		if (temp != null) {
			Integer i = Integer.valueOf(temp);
			nReadTimeout = i.intValue();
		}
		
		temp = applicationProps.getOptionalStringProperty(INFLUX_HTTP_READ_RETRIES);
		if (temp != null) {
			Integer i = Integer.valueOf(temp);
			httpRetries = i.intValue();
		}
		
		String strMaxThreads = applicationProps.getOptionalStringProperty(THREAD_POOL_MAX_SIZE);
		int nThreads = 0; 
		if (strMaxThreads.length() > 0) {
			nThreads = Integer.parseInt(strMaxThreads);
		}
		Apache45Client.initializePool(nThreads, nConnectTimeout, nReadTimeout);
    }

    @Override
    public void onPeriodicStatusReport() {
        logger.info("Measurements sent to influxDB since launch: " + measurementsSent);
    }

    @Override
    public InternalStats getInternalStats() {
        return m_internalStats;
    }

    @Override
    public void onRouterStats(StatsMessage msg) throws ReceiverException {
        String measurementName = msg.getTopicName();
        Measurement thisMeasurement = new Measurement(measurementName, msg.getTags(), msg.getValues(),
                msg.getTimestamp());
        postToInfluxDb(m_db, thisMeasurement);
        this.m_internalStats.incrament("router stat-" + measurementName);
    }

    @Override
    public void onPumpStats(StatsMessage msg) throws ReceiverException {
        String measurementName = msg.getTopicName();
        Map<String, String> levels = msg.getTags();
        String routerName = levels.get(ROUTER_NAME_KEY);
        Measurement thisMeasurement = new Measurement(measurementName,
                Collections.singletonMap(ROUTER_NAME_KEY, routerName), msg.getValues(), msg.getTimestamp());
        postToInfluxDb(m_pollerStatsDB, thisMeasurement);
        this.m_internalStats.incrament("pump stat-" + measurementName);
    }
}
