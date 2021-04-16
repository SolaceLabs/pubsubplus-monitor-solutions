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
import com.solacesystems.solgeneos.sample.util.SampleSEMPParser;

public class GenericMetricListSEMPParser extends SampleSEMPParser implements GenericSEMPParser{
	
	//Strings to represent the names of the elements in the SEMP response 
	private static final String MORE_COOKIE = "more-cookie";
	
	//Booleans to mark if currently in a particular element
	private boolean m_inMetric = false;
	private boolean m_inMoreCookie = false;
	
	private GenericMonitorConfig m_monitorConfig;
	
	// Data structures used to construct the Geneos table
	private Vector<Object> m_tableContent;
	
	// Maintain a map of all metrics where key is the metric name,
	// value is the metric value from the SEMP resonse.
	private HashMap<String,String> m_metricsMap;
	
	// Since we preserve row order, in order to extract metrics from the m_metricsMap
	// we will iterate over the user provided row names, if there were unexpected 
	// rows then we will iterate over this list next
	private LinkedHashSet<String> m_unexpectedRows;
	
	// The calling class may or may not wish for this class to generate
	// column names for the table.
	private boolean m_addColumnNamesToTable = true;
	
	// String builders which accumulate characters as provided by event parser
	private StringBuilder m_moreCookie;
	private StringBuilder m_metric;
	
	// Match list contains the row names (if any) that have been 
	// defined for the metric topics.
	private MetricFilter.MatchList<String> m_metricMatchList;
	private HashMap<String,MetricFilter.MatchList<String>> m_matchListMap;
	
	// Current xml tag
	private String m_currentLevel;
	
	// Track the root level tag name, so we can determine
	// when the we receive the final end of tag callback
	private String m_rootLevel;
	
	// Current list of xml tags from root to current location
	private ArrayList<String> m_currentLocation;
	
	// boolean to track one shot warning logs
	private boolean m_unexpectedRowWarningRaised = false;
	
	private Log m_logger;
	
    public GenericMetricListSEMPParser(GenericMonitorConfig monitorConfig) throws ParserConfigurationException, SAXException {
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
    	
    	m_moreCookie = new StringBuilder();
    	m_metric = new StringBuilder();
    	
    	m_metricMatchList = new MetricFilter.MatchList<String>();
    	m_matchListMap = new HashMap<String,MetricFilter.MatchList<String>>();
    	
    	m_unexpectedRows = new LinkedHashSet<String>();
    	
    	m_metricsMap = new HashMap<String,String> ();
    	
    	m_rootLevel = "";
    	m_currentLevel = "";
    	m_currentLocation = new ArrayList<String>(); 
    	
	}
	
    @Override
    public void startElement(String s, String s1, String s2, Attributes attributes1) throws SAXException {
    	super.startElement(s, s1, s2, attributes1);
    	
    	// New level
    	if(m_rootLevel.isEmpty()) {
    		m_rootLevel = s2;
    	}
    	m_currentLevel = s2;
    	m_currentLocation.add(s2);
    	
    	String currentLocationTopic = locationToTopic();
    	
    	getLogger().debug(m_monitorConfig.m_name + " Parser start:" + s2 + " location: " + currentLocationTopic);
   	
    	if(m_inMoreCookie) {
    		m_moreCookie.append(startElementToString(s, s1, s2, attributes1));
    	} 
    	
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

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
    	super.endElement(uri, localName, qName);
    	
    	String currentLocationTopic = locationToTopic();
    	
    	getLogger().debug(m_monitorConfig.m_name + " Parser end:" + qName + " location: " + currentLocationTopic);
    	
    	if(m_inMetric) {
    		getLogger().info(m_monitorConfig.m_name + " Parser in metric end, metric: " + m_metric.toString() +
    				" row name: " + m_currentLevel + " location: " + currentLocationTopic);
    		m_inMetric = false;
    		
    		// There will be a match list with at least one entry for the
    		// topic representing our current location.  Maintain the match list
    		// in a map using the current location as the key.  Should this location
    		// repeat in the SEMP response, then we use this version of the match
    		// list as it will contain only the remaining row names.
    		MetricFilter.MatchList<String> matchList = 
    				m_matchListMap.get(currentLocationTopic);
    		if(matchList == null) {
    			// add the new match list to the table
    			matchList = m_metricMatchList;
    			m_matchListMap.put(currentLocationTopic, matchList);
    		}
    		
    		if(matchList.size() == 0) {
    			// if match list is empty, we unexpectedly ran out of
    			// row names, use the current level and set isExpected
    			// flag to false
    			addRow("?" + m_currentLevel, m_metric.toString(), false);
    		} else {
    			addRow(matchList.remove(), m_metric.toString(), true);
    		}
    		
    		m_metric.delete(0, m_metric.length());
    		m_metricMatchList = new MetricFilter.MatchList<String>();
    	}
    	
    	m_currentLocation.remove(m_currentLocation.size() - 1);
    	if(m_currentLocation.isEmpty()) {
    		m_currentLevel = null;
    	}
    	else {
    		m_currentLevel = m_currentLocation.get(m_currentLocation.size() - 1);
    	}
    	
    	if(qName.equalsIgnoreCase(MORE_COOKIE)) {
    		m_inMoreCookie = false;
    	}
    	if(m_inMoreCookie) {
    		m_moreCookie.append("</" + qName + ">");
    	}
    	
    	if(qName.equals(m_rootLevel)) {
    		generateTable();
    	}
    }


    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
    	super.characters(ch, start, length);    	
    	
    	if(m_inMetric) {
    		m_metric.append(ch, start, length);
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
		
		if(m_unexpectedRowWarningRaised) numWarnings++;
		
		return numWarnings;
	}
	
	private void clearElementBooleans() {
		
		//Set all variables that mark element visitation to false
		m_inMetric = false;
		m_inMoreCookie = false;
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
	
	private void addRow(String rowName, String value, boolean isExpected) {
		String curRowName = rowName;
		int dupCount = 0;
		while(m_metricsMap.containsKey(curRowName)) {
			curRowName = "(" + (dupCount+2) + ")" + rowName;
		}
		m_metricsMap.put(curRowName, value);
		
		// Add to list of unexpected rows.
		if((dupCount > 0) || !isExpected) {
			m_unexpectedRows.add(curRowName);
			if(!m_unexpectedRowWarningRaised) {
				m_unexpectedRowWarningRaised = true;
				getLogger().warn(m_monitorConfig.m_name + 
						" received unexpected/duplicate row: " + curRowName);
			}
		}
	}
	
	private void generateTable() {
		Iterator<String> expectedRowsIt = m_monitorConfig.m_colOrRowNames.iterator();
		Iterator<String> unexpectedRowsIt = m_unexpectedRows.iterator();
		
		if(m_addColumnNamesToTable) {
			LinkedList<String> row = new LinkedList<String>();
    		row.add("Item");
    		row.add("Value");
    		m_tableContent.add(row);
    	}
		
		generateTable(expectedRowsIt);
		generateTable(unexpectedRowsIt);
	}
	
	private void generateTable(Iterator<String> it) {
		while(it.hasNext()) {
			String rowName = it.next();
			String value = m_metricsMap.get(rowName);
			if(value == null) value = "";
			LinkedList<String> row = new LinkedList<String>();
			row.add(rowName);
			row.add(value);
			m_tableContent.add(row);
		}
	}
	
	private Log getLogger() {
		return m_logger;
	}
	
}
