package com.solacesystems.solgeneos.monitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.solacesystems.solgeneos.sample.util.GenericMonitorConfig;
import com.solacesystems.solgeneos.sample.util.MetricFilter;
import com.solacesystems.solgeneos.sample.util.SampleSEMPParser;;

public class GenericObjectListSEMPParser extends SampleSEMPParser implements GenericSEMPParser{
	
	//Strings to represent the names of the elements in the SEMP response 
	private static final String MORE_COOKIE = "more-cookie";
	
	//Booleans to mark if currently in a particular element 
	private boolean m_inDelimiter = false;
	private boolean m_inObjectId = false;
	private boolean m_inMetric = false;
	private boolean m_inConditional = false;
	private boolean m_inParentMetric = false;
	private boolean m_inMoreCookie = false;
	
	private boolean m_objectIdFound = false;
	private boolean m_conditionalTestPassed = true;
	
	private GenericMonitorConfig m_monitorConfig;
	
	// Data structures used to construct the Geneos table
	private Vector<Object> m_tableContent;
	
	// Maintain a map of all metrics where key is the metric name,
	// value is the metric value from the SEMP resonse.
	private HashMap<String,String> m_metricsMap;
	
	// The calling class may or may not wish for this class to generate
	// column names for the table.
	private boolean m_addColumnNamesToTable = true;
	
	// String builders which accumulate characters as provided by event parser
	private StringBuilder m_moreCookie;
	private StringBuilder m_metric;
	private StringBuilder m_objectId;
	private StringBuilder m_conditional;
	private StringBuilder m_parentMetric;
	
	// Match list contains the column names (if any) that have been 
	// defined for the metric topics.
	private MetricFilter.MatchList<String> m_metricMatchList;
	private HashMap<String,MetricFilter.MatchList<String>> m_matchListMap;
	
	// Current xml tag
	private String m_currentLevel;
	
	// Current list of xml tags from root to current location
	private ArrayList<String> m_currentLocation;
	
	// boolean to track one shot warning logs
	private boolean m_ojectIdNotFoundWarningRaised = false;
	private boolean m_duplicateColumnNameWarningRaised = false;
	private boolean m_ranOutOfColumnNameWarningRaised = false;
	private boolean m_unableToExecuteConditionalWarningRaised = false;
	
	private Log m_logger;
	
	public GenericObjectListSEMPParser(GenericMonitorConfig monitorConfig) throws ParserConfigurationException, SAXException {
    	super();
    	
    	m_monitorConfig = monitorConfig;
    	m_logger = LogFactory.getLog(getClass());
    }
    
    public void parse(String respBodyString) throws Exception {
    	super.parse(respBodyString);
    }
    
	@Override
	protected void initializeParser(String str) {
		super.initializeParser(str);

		//Initialize all boolean values to false
		clearElementBooleans();
    	
		//Initialize vector to hold table content and add the column names
    	m_tableContent = new Vector<Object>();
    	
    	m_metricsMap = new HashMap<String,String>();
    	
    	m_moreCookie = new StringBuilder();
    	m_metric = new StringBuilder();
    	m_objectId = new StringBuilder();
    	m_conditional = new StringBuilder();
    	m_parentMetric = new StringBuilder();
    	
    	m_metricMatchList = new MetricFilter.MatchList<String>();
    	m_matchListMap = new HashMap<String,MetricFilter.MatchList<String>>();
    	
    	m_currentLevel = "";
    	m_currentLocation = new ArrayList<String>();  
    	
    	if(m_addColumnNamesToTable) {
    		generateColumnNames();
    	}
	}
	
    @Override
    public void startElement(String s, String s1, String s2, Attributes attributes1) throws SAXException {
    	super.startElement(s, s1, s2, attributes1);
    	
    	// New level
    	m_currentLevel = s2;
    	m_currentLocation.add(s2);
    	
    	String currentLocationTopic = locationToTopic();
    	
    	getLogger().debug(m_monitorConfig.m_name + " Parser start:" + s2 + " location: " + currentLocationTopic);
   	
    	if(m_inMoreCookie) {
    		m_moreCookie.append(startElementToString(s, s1, s2, attributes1));
    	} 
    	
    	if(currentLocationTopic.equals(m_monitorConfig.m_delimiterTopic)) {
    		getLogger().info(m_monitorConfig.m_name + " Parser found delimiter start: " + currentLocationTopic);
    		m_inDelimiter = true;
    		
    		// Reset state as we are starting a new row/object
    		m_objectIdFound = false;
    		m_conditionalTestPassed = true;
    	} else if(currentLocationTopic.equals(m_monitorConfig.m_objectIdTopic)) {
    		getLogger().info(m_monitorConfig.m_name + " Parser found object id start: " + currentLocationTopic);
    		m_inObjectId = true;
    	} else {
    		MetricFilter.MatchList<String> matchList = m_monitorConfig.m_filter.lookup(currentLocationTopic);
    		if(matchList.isMatch()) {
	    		getLogger().info(m_monitorConfig.m_name + " Parser: metric match start: " + currentLocationTopic);
	    		m_inMetric = true;
	    		m_metricMatchList = matchList;
	    	} else if(s2.equalsIgnoreCase(MORE_COOKIE)) {
	    		m_inMoreCookie = true;
	    		m_moreCookie.delete(0, m_moreCookie.length());
	    	}
    	}
    	
    	if(m_monitorConfig.m_parentTopic != null) {
    		if(currentLocationTopic.equals(m_monitorConfig.m_parentTopic)) {
    			m_inParentMetric = true;
    			// Reset the parentMetric on entry
    			m_parentMetric.delete(0, m_parentMetric.length());
    		}
    	}
    	
    	if(m_monitorConfig.m_conditionalTopic != null) {
    		if(currentLocationTopic.equals(m_monitorConfig.m_conditionalTopic.m_topic)) {
    			m_inConditional = true;
    		}
    	}
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
    	super.endElement(uri, localName, qName);
    	
    	String currentLocationTopic = locationToTopic();
    	
    	getLogger().debug(m_monitorConfig.m_name + " Parser end:" + qName + " location: " + currentLocationTopic);
    	
    	//
		// End of Delimiter tag
		//
    	if(currentLocationTopic.equals(m_monitorConfig.m_delimiterTopic)) {
    		getLogger().info(m_monitorConfig.m_name + " Parser found delimiter end: " + currentLocationTopic);
    		m_inDelimiter = false;
    		
    		// Check if the object id has been found, if not raise a warning and don't
    		// update the table.  If the object id was not defined in the
    		// configuration file, update the table and don't raise a warning
    		if(m_objectIdFound || m_monitorConfig.m_objectIdTopic == null) {
    			// If conditional test did not pass, then skip this object
    			if(m_conditionalTestPassed) {
    				generateRow();
    			}
    		} else {
    			if(!m_ojectIdNotFoundWarningRaised) {
	    			getLogger().warn(m_monitorConfig.m_name + 
	    					"Object id not found, discarding data.  Object id topic:" +
	    					m_monitorConfig.m_objectIdTopic);
	    			m_ojectIdNotFoundWarningRaised = true;
    			}
    		}
    		m_metricsMap.clear();
    		m_matchListMap.clear();
    	} 
    	
    	//
		// End of ObjectId tag
		//
    	if(currentLocationTopic.equals(m_monitorConfig.m_objectIdTopic)) {
    		getLogger().info(m_monitorConfig.m_name + " Parser found object id end: " + currentLocationTopic);
    		m_inObjectId = false;
    		m_objectIdFound = true;
    		
    		if(m_metricsMap.containsKey(m_monitorConfig.m_objectIdTopicName)) {
				if(!m_duplicateColumnNameWarningRaised) {
					getLogger().warn(m_monitorConfig.m_name + " Duplicate column names found: " +
							m_monitorConfig.m_objectIdTopicName + " for object id topic: " + 
							currentLocationTopic + 
	    					" please review SEMP schema and configuration file.");
					m_duplicateColumnNameWarningRaised = true;
				}
			} else {
				m_metricsMap.put(m_monitorConfig.m_objectIdTopicName, m_objectId.toString());
			}
    		
    		m_objectId.delete(0, m_objectId.length());
    	} 
    	
    	//
		// End of a matching metric tag
		//
    	if(m_inMetric) {
    		getLogger().info(m_monitorConfig.m_name + " Parser in metric end, metric: " + m_metric.toString() +
    				" column name: " + m_currentLevel + " location: " + currentLocationTopic);
    		m_inMetric = false;
    		
    		// There will be a match list with at least one entry for the
    		// topic representing our current location.  Maintain the match list
    		// in a map using the current location as the key.  Should this location
    		// repeat in the SEMP response, then we use this version of the match
    		// list as it will contain only the remaining column names.
    		MetricFilter.MatchList<String> matchList = 
    				m_matchListMap.get(currentLocationTopic);
    		if(matchList == null) {
    			// add the new match list to the table
    			matchList = m_metricMatchList;
    			m_matchListMap.put(currentLocationTopic, matchList);
    		}
    		
    		if(matchList.size() == 0) {
    			// if match list is empty, we unexpectedly ran out of
    			// column names, drop the metric
    			if(!m_ranOutOfColumnNameWarningRaised) {
    				getLogger().warn(m_monitorConfig.m_name + " Ran out of column names for topic: " +
	    					currentLocationTopic + 
	    					" please review SEMP schema and update topics section configuration file.");
    				m_ranOutOfColumnNameWarningRaised = true;
    			}
    		} else {
    			String columnName = matchList.remove();
    			if(m_metricsMap.containsKey(columnName)) {
    				if(!m_duplicateColumnNameWarningRaised) {
    					getLogger().warn(m_monitorConfig.m_name + " Duplicate column names found: " +
    							columnName + " for topic: " + currentLocationTopic + 
    	    					" please review SEMP schema and configuration file.");
    					m_duplicateColumnNameWarningRaised = true;
    				}
    			} else {
    				m_metricsMap.put(columnName, m_metric.toString());
    			}
    		}
    		
    		m_metric.delete(0, m_metric.length());
    		m_metricMatchList = new MetricFilter.MatchList<String>();
    	}
    	
    	//
		// End of Parent tag
		//
    	if(m_inParentMetric) {
    		m_inParentMetric = false;
    		m_metricsMap.put(m_monitorConfig.m_parentTopicName, m_parentMetric.toString());
    	}
    	
    	//
		// End of Conditional tag
		//
    	if(m_inConditional) {
    		m_inConditional = false;
    		try {
    			boolean conditionalResult = true;
    			if(m_monitorConfig.m_conditionalTopic.m_operator.isIntegerOperand()) {
    				conditionalResult = m_monitorConfig.m_conditionalTopic.execute(
    						Integer.parseInt(m_conditional.toString()));
    			} else {
    				conditionalResult = m_monitorConfig.m_conditionalTopic.execute(
    						m_conditional.toString());
    			}
    			if(!conditionalResult) {
    				this.m_conditionalTestPassed = false;
    			}
    		} catch(Exception e) {
    			if(!m_unableToExecuteConditionalWarningRaised) {
    				getLogger().warn("Unable to execute conditional topic: " +
    						m_monitorConfig.m_conditionalTopicStr + 
    						", exception raised: " + e.getMessage());
    				m_unableToExecuteConditionalWarningRaised = true;
    			}
    		}
    		m_conditional.delete(0, m_conditional.length());
    	}
    	
    	//
    	// Update current location/level
    	//
    	m_currentLocation.remove(m_currentLocation.size() - 1);
    	if(m_currentLocation.isEmpty()) {
    		m_currentLevel = null;
    	}
    	else {
    		m_currentLevel = m_currentLocation.get(m_currentLocation.size() - 1);
    	}
    	
    	//
    	// Handle more cookie
    	//
    	if(qName.equalsIgnoreCase(MORE_COOKIE)) {
    		m_inMoreCookie = false;
    	}
    	if(m_inMoreCookie) {
    		m_moreCookie.append("</" + qName + ">");
    	}
    }


    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
    	super.characters(ch, start, length);    	
    	
    	if(m_inObjectId) {
    		m_objectId.append(ch, start, length);
    	} else if(m_inMetric) {
    		m_metric.append(ch, start, length);
    	}
    	
    	if(m_inConditional) {
    		m_conditional.append(ch, start, length);
    	}
    	
    	if(m_inParentMetric) {
    		m_parentMetric.append(ch, start, length);
    	}
    	
    	if(m_inMoreCookie) {
    		m_moreCookie.append(ch, start, length);
    	}
    }
       
	public Vector<Object> getTableContent() {
		return m_tableContent;
	}
	
	public String getMoreCookie() {
		return m_moreCookie.toString();
	}
	
	public void setAddColumnNamesToTable(boolean addColumnNames) {
		m_addColumnNamesToTable = addColumnNames;
	}
	
	public int getNumWarnings() {
		int numWarnings = 0;
		
		if(m_ojectIdNotFoundWarningRaised) numWarnings++;
		if(m_ranOutOfColumnNameWarningRaised) numWarnings++;
		if(m_unableToExecuteConditionalWarningRaised) numWarnings++;
		if(m_duplicateColumnNameWarningRaised) numWarnings++;
		
		return numWarnings;
	}
	
	private void clearElementBooleans() {
		
		//Set all variables that mark element visitation to false
		m_inDelimiter = false;
		m_inObjectId = false;
		m_inMetric = false;
		m_inMoreCookie = false;
		m_inConditional = false;
		m_inParentMetric = false;
		
		m_objectIdFound = false;
		m_conditionalTestPassed = true;
	}
	
	private String startElementToString(String s, String s1, String s2, Attributes attributes1) {
		StringBuilder strBuilder = new StringBuilder();
		strBuilder.append("<" + s2);
		for(int i=0; i<attributes1.getLength(); i++) {
			strBuilder.append(" " + attributes1.getQName(i) + "=\"" + attributes1.getValue(i) + "\"");
		}
		strBuilder.append(">");
		return strBuilder.toString();
	}
	
	private String locationToTopic() {
		StringBuilder strBuilder = new StringBuilder();
		
		for(int i=0; i<this.m_currentLocation.size(); i++) {
			if(i>0) {
				strBuilder.append("/");
			}
			strBuilder.append(m_currentLocation.get(i));
		}
		return strBuilder.toString();
	}
	
	
	private void generateColumnNames() {
		Iterator<String> it = m_monitorConfig.m_colOrRowNames.iterator();
		LinkedList<String> row = new LinkedList<String>();
		
		// Make object id first column. if present
		if(m_monitorConfig.m_objectIdTopic != null) {
			row.add(m_monitorConfig.m_objectIdTopicName);
			// Make parent 2nd column, if present
			if(m_monitorConfig.m_parentTopic != null) {
				row.add(m_monitorConfig.m_parentTopicName);
			}
		}
		
		while(it.hasNext()) {
    		row.add(it.next());
    	}
		
		if(m_monitorConfig.m_objectIdTopic == null) {
			// Make parent last column, if present
			if(m_monitorConfig.m_parentTopic != null) {
				row.add(m_monitorConfig.m_parentTopicName);
			}
		}

		m_tableContent.add(row);
	}
	
	private void generateRow() {
		Iterator<String> it = m_monitorConfig.m_colOrRowNames.iterator();
		LinkedList<String> row = new LinkedList<String>();
		
		// If an object id was provided, put it in first column
		if(m_monitorConfig.m_objectIdTopic != null) {
			String objectId = m_metricsMap.get(m_monitorConfig.m_objectIdTopicName);
			if(objectId == null) {
				objectId = "";
			}
			row.add(objectId);
			// Make parent 2nd column, if present
			if(m_monitorConfig.m_parentTopic != null) {
				String parentId = m_metricsMap.get(m_monitorConfig.m_parentTopicName);
				if(parentId == null) {
					parentId = "";
				}
				row.add(parentId);
			}
		}
		
		
		while(it.hasNext()) {
			String colName = it.next();
			String value = m_metricsMap.get(colName);
			if(value == null) value = "";
			row.add(value);
		}
		
		if(m_monitorConfig.m_objectIdTopic == null) {
			// Make parent last column, if present
			if(m_monitorConfig.m_parentTopic != null) {
				String parentId = m_metricsMap.get(m_monitorConfig.m_parentTopicName);
				if(parentId == null) {
					parentId = "";
				}
				row.add(parentId);
			}
		}
		
		m_tableContent.add(row);
	}
	
	private Log getLogger() {
		return m_logger;
	}
	
}
