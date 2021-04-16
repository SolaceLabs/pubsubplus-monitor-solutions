package com.solacesystems.solgeneos.sample.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class VpnListSEMPParser extends SampleSEMPParser {
	
	//Strings to represent the names of the elements in the SEMP response 
	private static final String MESSAGE_VPN = "message-vpn";
	private static final String VPN = "vpn";
	private static final String NAME = "name";
	private static final String MORE_COOKIE = "more-cookie";
	
	//Booleans to mark if element has been visited 
	private boolean m_inMessageVpn = false;
	private boolean m_inVpn = false;
	private boolean m_inName = false;
	
	private boolean m_inMoreCookie = false;
	
	//String builders to hold content for each row
	private StringBuilder m_name = new StringBuilder();
	    
	private HashSet<String> m_vpns;
	private StringBuilder m_moreCookie;
    
    public VpnListSEMPParser() throws ParserConfigurationException, SAXException {
    	super();
    }
    
	@Override
	protected void initializeParser(String str) {
		super.initializeParser(str);

		//Initialize all boolean values to false
		clearElementBooleans();
		
		//Initialize all string buffers to empty
		emptyStringBuffers();
    	
		//Initialize vector to hold table content and add the column names
    	m_vpns = new HashSet<String>();
    	
    	//Initialize string buffer for the more cookie
    	m_moreCookie = new StringBuilder();
	}
	
    @Override
    public void startElement(String s, String s1, String s2, Attributes attributes1) throws SAXException {
    	super.startElement(s, s1, s2, attributes1);
   	
    	if(m_inMoreCookie) {
    		m_moreCookie.append(startElementToString(s, s1, s2, attributes1));
    	}
    	
    	if(s2.equalsIgnoreCase(MESSAGE_VPN) && !m_inMoreCookie){
    		m_inMessageVpn = true;
    	}else if(m_inMessageVpn && s2.equalsIgnoreCase(VPN)){
    		m_inVpn = true;
    	}else if(m_inVpn && s2.equalsIgnoreCase(NAME)){
    		m_inName = true;
    	}else if(s2.equalsIgnoreCase(MORE_COOKIE)) {
    		m_inMoreCookie = true;
    	}
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
    	super.endElement(uri, localName, qName);
    	
    	if(m_inMessageVpn && qName.equalsIgnoreCase(MESSAGE_VPN) && !m_inMoreCookie)
    	{
    		m_inMessageVpn = false;
    	}else if(m_inVpn && qName.equalsIgnoreCase(VPN) && !m_inMoreCookie)
    	{
    		// Add the vpn to the set
    		m_vpns.add(m_name.toString());
    		
    		//Make sure all string builders are empty
    		emptyStringBuffers();
    		
    		m_inVpn = false;    		
    	}else if (m_inVpn && qName.equalsIgnoreCase(NAME)) {
			m_inName = false;
	    }else if(qName.equalsIgnoreCase(MORE_COOKIE)) {
    		m_inMoreCookie = false;
    	}
    	
    	if(m_inMoreCookie) {
    		m_moreCookie.append("</" + qName + ">");
    	}
    }


    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
    	super.characters(ch, start, length);    	
    	
    	if (m_inName) {
    		m_name.append(ch, start, length);
    	}
    	
    	if(m_inMoreCookie) {
    		m_moreCookie.append(ch, start, length);
    	}
    }
       
	public HashSet<String> getVpns() {
		return m_vpns;
	}
	
	public String getMoreCookie() {
		return m_moreCookie.toString();
	}
	
	private void clearElementBooleans(){
		
		//Set all variables that mark element visitation to false
		m_inMessageVpn = false;
		m_inVpn = false;
		m_inName = false;
		m_inMoreCookie = false;
	}
	
	private void emptyStringBuffers() {
		
		//Make sure all string builders are empty
		m_name.delete(0, m_name.length());
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
	
}
