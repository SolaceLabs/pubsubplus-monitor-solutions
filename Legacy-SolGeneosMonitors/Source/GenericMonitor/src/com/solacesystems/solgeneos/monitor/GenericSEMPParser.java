package com.solacesystems.solgeneos.monitor;

import java.util.ArrayList;
import java.util.HashMap;
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

public interface GenericSEMPParser {
	
	public void parse(String respBodyString) throws Exception;
       
	public Vector<Object> getTableContent();
	
	public String getMoreCookie();
	
	public void setAddColumnNamesToTable(boolean addColumnNames);
	
	public int getNumWarnings();
	
	
}
