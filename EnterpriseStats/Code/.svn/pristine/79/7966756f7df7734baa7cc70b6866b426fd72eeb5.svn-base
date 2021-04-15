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

package com.solace.psg.enterprisestats.statspump.tools.semp;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.solace.psg.enterprisestats.statspump.tools.util.ResourceList;

public enum SempRpcSchemaLoader {

    INSTANCE;

    private static final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    private static final XPathFactory xpathFactory = XPathFactory.newInstance();

    private final SortedMap<SempVersion,Schema> schemas = new TreeMap<SempVersion,Schema>();
    private static final Logger logger = LoggerFactory.getLogger(SempRpcSchemaLoader.class);
        
    public void loadSchemas() {
        Pattern pattern = Pattern.compile(".*semp-rpc-soltr.xsd");
        final Set<URL> list = ResourceList.getResources(pattern);
        logger.info("SempRpcSchemaLoader found " + list.size() + "schemas... loading them");
        for (final URL url : list) {
            logger.info("Loading SEMP RPC schema: "+url);
            loadSchema(url);
        }
    }
    
    private NodeList getNodes(Schema sempRequestSchema, URL schemaUrl, String xpathExpressionString) throws XPathExpressionException, IOException {
        final XPath xpath = xpathFactory.newXPath();
        XPathExpression xpathExpr = xpath.compile(xpathExpressionString);
        NodeList nodes = (NodeList)xpathExpr.evaluate(new InputSource(schemaUrl.openStream()),XPathConstants.NODESET);
        assert nodes.getLength() == 1;
        return nodes;
    }
    
    private void logBadSchemaMessage(URL schemaUrl) {
		logger.warn("This XSD file ('" + schemaUrl.toString() + "' doesn't have the schema version in the expected location. This file is unusable by this version of StatsPump.");
    }
    
    private void loadSchema(URL schemaUrl) {
        try {
            if (schemaUrl == null) {
                throw new IllegalArgumentException("Could not find SEMP RPC schema file: "+schemaUrl);
            }
            final Schema sempRequestSchema = schemaFactory.newSchema(schemaUrl);
            final String xpathSempVersionPre_8dot3 = "//*[@name='semp-version'][@fixed]";  // look for the attribute tag that defines the SEMP version
            final String xpathExpressionStringForVersions_8dot3_orLater = "//*[@name='semp-version'][@default]";  // look for the attribute tag that defines the SEMP version
            String attrtoUse = "fixed";
            
            boolean bGoodSchema = true;
            
            NodeList nodes = getNodes(sempRequestSchema, schemaUrl, xpathSempVersionPre_8dot3);
            if (nodes.item(0) == null) {
        		logger.info("No nodes were loaded from this schema. Perhaps this schema is an 8.3 or later one. Checking...");
                nodes = getNodes(sempRequestSchema, schemaUrl, xpathExpressionStringForVersions_8dot3_orLater);
                if (nodes.item(0) == null) {
                	bGoodSchema = false;
                	logBadSchemaMessage(schemaUrl);
                }
                else {
                	// good to go with an 8.3 or later schema 
                	attrtoUse = "default";
                }
        	} 
            if (bGoodSchema) {
                // now to find out what version it is!
            	String solOSVersion = nodes.item(0).getAttributes().getNamedItem(attrtoUse).getNodeValue();
            	if (solOSVersion.length() > 0) { 
            		logger.info(String.format("Success loading '%s', detected SEMP version '%s'",URLDecoder.decode(schemaUrl.toString(),"UTF-8"),solOSVersion));
            		schemas.put(new SempVersion(solOSVersion),sempRequestSchema);
            	}
            	else {
                	logBadSchemaMessage(schemaUrl);
            	}
            }
        }
        catch (XPathExpressionException e) {
            logger.error("Somehow managed to throw this? Should not happen unless someone changed my XPath",e);
        } catch (IOException e) {
            logger.error("Internal I/O exception caught while reading in SEMP schema "+schemaUrl,e);
        } catch (SAXException e) {
            logger.error("Could not process SEMP schema "+schemaUrl,e);
        }
    }
    
    /**
     * Will return null if the method cannot find any valid schema to validate
     * this particular SEMP command.
     * @param sempRequest
     * @return
     */
    public SempVersion findMinSempVersion(final String sempRequest) {
        for (SempVersion ver : schemas.keySet()) {
            Validator validator = schemas.get(ver).newValidator();
            try {
                String versionedSempCommand = String.format(sempRequest,ver.toString());  // change the %s in the SEMP oommand to this version
                //System.out.println(versionedSempCommand);
                validator.validate(new StreamSource(new StringReader(versionedSempCommand)));
                return ver;
            } catch (SAXException e) {
                continue;
            } catch (IOException e) {
                continue;
            }
        }
        return null;
    }
    
    
    // The code below this point can be used for informal unit testing
    
//    public static void mainTest(String... args) {
//        SempRpcSchemaLoader.INSTANCE.loadSchemas();
//        String semp = "<rpc semp-version='%s'><show><cache-cluster><name>*</name><vpn-name>*</vpn-name><topics/></cache-cluster></show></rpc>";
//        System.out.printf("Min SEMP version '%s' for %s%n",SempRpcSchemaLoader.INSTANCE.findMinSempVersion(semp),semp);
//        semp = "<rpc semp-version='%s'><show><client><name>*</name><vpn-name>*</vpn-name><stats/><queues/><count/><num-elements>1000</num-elements></client></show></rpc>";
//        System.out.printf("Min SEMP version '%s' for %s%n",SempRpcSchemaLoader.INSTANCE.findMinSempVersion(semp),semp);
//        semp = "<rpc semp-version='%s'><show><bridge><bridge-name-pattern>*</bridge-name-pattern><subscriptions/></bridge></show></rpc>";
//        System.out.printf("Min SEMP version '%s' for %s%n",SempRpcSchemaLoader.INSTANCE.findMinSempVersion(semp),semp);
//        semp = "<rpc semp-version='%s'><show><config-sync/></show></rpc>";
//        System.out.printf("Min SEMP version '%s' for %s%n",SempRpcSchemaLoader.INSTANCE.findMinSempVersion(semp),semp);
//        semp = "<rpc semp-version='%s'><show><queue><name>*</name><vpn-name>*</vpn-name><rates/><count/><num-elements>1000</num-elements></queue></show></rpc>";
//        System.out.printf("Min SEMP version '%s' for %s%n",SempRpcSchemaLoader.INSTANCE.findMinSempVersion(semp),semp);
//    }
    
}
