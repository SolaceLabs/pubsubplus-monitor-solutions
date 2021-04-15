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
package com.solace.psg.enterprisestats.receiver.elasticsearch;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solace.psg.enterprisestats.receiver.AbstractStatsTap;
import com.solace.psg.enterprisestats.receiver.ReceiverException;
import com.solace.psg.enterprisestats.receiver.StatsMessage;
import com.solace.psg.enterprisestats.receiver.StatsReceiverProperties;
import com.solace.psg.enterprisestats.receiver.elasticsearch.utils.Apache45Client;
import com.solace.psg.enterprisestats.receiver.elasticsearch.utils.HttpException;
import com.solace.psg.enterprisestats.receiver.stats.InternalStats;
import com.solace.psg.enterprisestats.receiver.utils.ReceiverUtils;

/**
 * Implements the Tap interface, in terms of an Elasticsearch writer. Elasticsearch is
 * written to using an Http interface.
 */
public class ElasticsearchStatsTap extends AbstractStatsTap {
    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchStatsTap.class);

    private static final String ELASTICSEARCH_HOST_PROP_NAME = "ELASTICSEARCH_HOST";
    private static final String ELASTICSEARCH_INDEX_PROP_NAME = "ELASTICSEARCH_INDEX";
    private static final String ELASTICSEARCH_USER_PROP_NAME = "ELASTICSEARCH_USER";
    private static final String ELASTICSEARCH_PASSWORD_PROP_NAME = "ELASTICSEARCH_PASSWORD";
    private static final String ELASTICSEARCH_STUBBED_PROP_NAME = "ELASTICSEARCH_STUBBED";
    private static final String ELASTICSEARCH_POLLER_STATS_INDEX_PROP_NAME = "ELASTICSEARCH_POLLER_STATS_INDEX";
    private static final String ROUTER_NAME_KEY = "ROUTER_NAME";

    private static final String ELASTICSEARCH_HTTP_CONNECT_TIMEOUT = "ELASTICSEARCH_HTTP_CONNECT_TIMEOUT";
    private static final String ELASTICSEARCH_HTTP_READ_TIMEOUT = "ELASTICSEARCH_HTTP_READ_TIMEOUT";
    private static final String ELASTICSEARCH_HTTP_READ_RETRIES = "ELASTICSEARCH_HTTP_READ_RETRIES";

    private static final String THREAD_POOL_MAX_SIZE = "THREAD_POOL_MAX_SIZE";
    
    private static final String FIELD_NAME_PUMP_TIMESTAMP = "TIME_STAMP";
    
    private static final String METRIC_INDEX_TEMPLATE = 	"" + 
														    "{" +
														    "\"mappings\": {" +
														        "\"metric\": {" +
														          "\"properties\": {" +
														            "\"" + FIELD_NAME_PUMP_TIMESTAMP + "\": {" +
														              "\"type\": \"date\"" + 
														            "}" +
														          "}" +
														        "}" +
														      "}" +
														    "}";

    private String m_host;
    private String m_user;
    private String m_password;
    private String m_db;
    private String m_pollerStatsDB = "statspump_poller_stats"; // default
    private boolean stubbedOut = false;
    private long measurementsSent = 0;
    private ElasticsearchLogger httpLog = null;
    private InternalStats m_internalStats;
    private int httpRetries = 1;
    private HashSet<String> m_indexSet = new HashSet<String>();

    /**
     * Simple constructor loaded reflectively by the Framework core
     */
    public ElasticsearchStatsTap() {
        m_internalStats = new InternalStats();
    }

    /**
     * Removes the \n char from the 'in' string, replacing it with 0xfffd, which is a universal
     * newline char, which passes into Elasticsearch successfully. Standard \n was creating new measurents
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
     * Check if this is the first time we are attempting to update an index, 
     * if so then determine if the index exists in elasticsearch and if not
     * then create the index explicity.  Otherwise, just return.
     *
     * @param tags
     * @param map
     * @param timestamp
     * @return
     */
    private void registerIndex(String indexName, String indexType) throws HttpException {
    	if(m_indexSet.contains(indexName)) return;
    	
    	String url = String.format("http://%s/%s", m_host, indexName);
    	if (!stubbedOut) {
    		logger.debug("HEAD: " +  url);
            int statusCode = Apache45Client.head(url, m_user, m_password);
        	logger.debug("Status code: " + statusCode + " for url: " + url);
        	if(statusCode/100 * 100 == 200) {
        		// Index already exists in elasticsearch
        	} else if(statusCode == 404) {
        		// Index not found in elastic, so create it in elasticsearch
        		url = String.format("http://%s/%s/", m_host, indexName);
        		logger.debug("PUT: " + url + " Requst: " + METRIC_INDEX_TEMPLATE);
        		String response = Apache45Client.put(url, m_user, m_password, METRIC_INDEX_TEMPLATE);
        		logger.debug("Response: " + response);
        	} else {
        		// throw exception
        		throw new HttpException("Unexpected status code: " + statusCode + " when perform index lookup for: " + indexName);
        	}
        	// Add index to map
        	m_indexSet.add(indexName);
        } else {
            // for debugging without an Influx installation, just write to
            // file
            httpLog.writeLog("HEAD URL: " + url + "\n");
        }
    }
    
    /**
     * Builds the body of the content to sent to Elasticsearch for insertion
     *
     * @param tags
     * @param map
     * @param timestamp
     * @return
     */
    private String buildElasticsearchBody(Map<String, String> tags, Map<String, Object> map, long timestamp) {
        StringBuilder sb = new StringBuilder();
        sb.append("{ ");
        
        for (String tag : tags.keySet()) {
           sb.append("\"").append(tag).append("\"").append(" : \"").append(tags.get(tag)).append("\",");
        }
        
        Date date = new Date(timestamp);
        String dateStr = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ").format(date);
        sb.append("\"").append(FIELD_NAME_PUMP_TIMESTAMP).append("\"").append(" : \"").append(dateStr).append("\",");        
        
        for (String key : map.keySet()) {
            //sb.append(key.replaceAll("([ ,=])", "\\\\$1"));
        	sb.append("\"").append(key).append("\"").append(" : ");
            if (map.get(key) instanceof String) {
				String value = (String) map.get(key);
				sb.append("\"").append(value).append("\",");
            } else if (map.get(key) instanceof Long || map.get(key) instanceof Integer || map.get(key) instanceof Byte
                    || map.get(key) instanceof Short) {
                sb.append(map.get(key)).append(",");
            } else {
                // float, double, or boolean
                sb.append(map.get(key));
                sb.append(",");
            }
        }
        
        // Remove last comma
        sb.setLength(sb.length() - 1);
        
        sb.append(" }");
        
        
        return sb.toString();
    }

    /**
     * Sends the data to insert to Elasticsearch.
     *
     * @param db
     * @param measurements
     */
    private void postToElasticsearch(String db, Measurement measurement) throws ReceiverException {        
        String json = buildElasticsearchBody(measurement.getTags(), measurement.getMap(), measurement.getTimestamp());
        
        boolean writeSucceeded = false;
        int nAttempts = 1;
        String indexName = String.format("%s-%s-metric", db, measurement.getName().toLowerCase());
        String indexType = "metric";
        while (!writeSucceeded) {
	        try {
	        	//registerIndex(indexName, indexType);
	            String url = String.format("http://%s/%s/%s", m_host, indexName, indexType);
	            //String url = String.format("http://%s/%s/%s", m_host, db, measurement.getName());
	            if (logger.isDebugEnabled()) {
	                logger.debug("POSTing write request to '" + db + "/" + measurement.getName() + "' Elasticsearch =" + json + "Measurement: " + measurement);
	            }

	            if (!stubbedOut) {
	                // the elasticsearch server returns no body upon success, just a 200
	                // http code. If it fails, and IOException will be thrown
	                //HttpUtils.httpRequestString(url, m_user, m_password, RequestMethod.POST, sb.toString());
	            	logger.debug("Request: " + json);
	            	String result = Apache45Client.post(url, m_user, m_password, json);
	            	
	            	logger.debug("Response: " + result);
	            } else {
	                // for debugging without an Influx installation, just write to
	                // file
	                httpLog.writeLog("URL: " + url + " TYPE: " + measurement.getTags().get("TYPE") + "doc: "  + json + "\n");
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
        this.m_host = applicationProps.getMandatoryStringProperty(ELASTICSEARCH_HOST_PROP_NAME);
        this.m_user = applicationProps.getMandatoryStringProperty(ELASTICSEARCH_USER_PROP_NAME);
        // this.db_pw =
        // applicationProps.getMandatoryStringProperty("ELASTICSEARCH_PW");
        String encryptedPw = applicationProps.getOptionalStringProperty(ELASTICSEARCH_PASSWORD_PROP_NAME);
        this.m_password = ReceiverUtils.decryptPassword(encryptedPw, ELASTICSEARCH_PASSWORD_PROP_NAME);
        this.m_db = applicationProps.getMandatoryStringProperty(ELASTICSEARCH_INDEX_PROP_NAME);
        String pollerStatsDB = applicationProps.getOptionalStringProperty(ELASTICSEARCH_POLLER_STATS_INDEX_PROP_NAME);
        if (pollerStatsDB != null && !pollerStatsDB.isEmpty()) {
            this.m_pollerStatsDB = pollerStatsDB;
        }

        String stubbed = applicationProps.getOptionalStringProperty(ELASTICSEARCH_STUBBED_PROP_NAME);
        if (stubbed != null && !stubbed.isEmpty()) {
            stubbedOut = stubbed.equalsIgnoreCase("true");
            if (stubbedOut) {
                httpLog = new ElasticsearchLogger();
                logger.info("Elasticsearch writing has been stubbed out. Http requests will be logged to '"
                        + httpLog.getFileNameUsed() + "' instead.");
            }
        }

		String temp = applicationProps.getOptionalStringProperty(ELASTICSEARCH_HTTP_CONNECT_TIMEOUT);
		int nConnectTimeout = -1;
		int nReadTimeout = -1;
		if (temp != null) {
			Integer i = Integer.valueOf(temp);
			nConnectTimeout = i.intValue();
		}

		temp = applicationProps.getOptionalStringProperty(ELASTICSEARCH_HTTP_READ_TIMEOUT);
		if (temp != null) {
			Integer i = Integer.valueOf(temp);
			nReadTimeout = i.intValue();
		}

		temp = applicationProps.getOptionalStringProperty(ELASTICSEARCH_HTTP_READ_RETRIES);
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
        logger.info("Measurements sent to Elasticsearch since launch: " + measurementsSent);
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
        postToElasticsearch(m_db, thisMeasurement);
        this.m_internalStats.incrament("router stat-" + measurementName);
    }

    @Override
    public void onPumpStats(StatsMessage msg) throws ReceiverException {
        String measurementName = msg.getTopicName();
        Map<String, String> levels = msg.getTags();
        String routerName = levels.get(ROUTER_NAME_KEY);
        Measurement thisMeasurement = new Measurement(measurementName,
                Collections.singletonMap(ROUTER_NAME_KEY, routerName), msg.getValues(), msg.getTimestamp());
        postToElasticsearch(m_pollerStatsDB, thisMeasurement);
        this.m_internalStats.incrament("pump stat-" + measurementName);
    }
}
