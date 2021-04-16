package com.solacesystems.solgeneos.sample.util;

import java.io.StringReader;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.solacesystems.common.semp.SEMPSAXParser;

public abstract class SampleSEMPParser extends SEMPSAXParser {

	public SampleSEMPParser() throws ParserConfigurationException, SAXException {
		super();
	}

	public void parse(String respBodyString) throws Exception {
        initializeParser(respBodyString);
        StringBuffer respBodyBuffer = new StringBuffer(respBodyString);
        StringReader strreader = new StringReader(respBodyBuffer.toString());
        mSAXParser.parse(new InputSource(strreader), this);
        processResponse();              
    }
}
