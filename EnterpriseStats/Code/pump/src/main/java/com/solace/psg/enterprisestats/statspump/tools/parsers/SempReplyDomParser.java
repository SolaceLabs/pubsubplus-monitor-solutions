/*
 * Copyright 2014 Solace Systems, Inc. All rights reserved.
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

package com.solace.psg.enterprisestats.statspump.tools.parsers;

/**
 * This is old code that I haven't used in a while.
 * DOM parser for SEMP.  Slow, but can do xPath
 * 
 * @author Aaron Lee
 */
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class SempReplyDomParser {

    protected final String semp;

    public static final Pattern SEMP_MORE_COOKIE = Pattern.compile("<more-cookie>\\s(.*)\\s</more-cookie>",Pattern.DOTALL);
    public static final String SEMP_OK_CODE = "execute-result code=\"ok\"";

    private static final Pattern PATTERN_REASON = Pattern.compile("reason=\"(.*?)\"");
    private static final Pattern PATTERN_ERROR = Pattern.compile("ERROR: (.*?)-->");

    DocumentBuilderFactory dbFactory;
    XPathFactory xpathFactory;
    DocumentBuilder db;
    XPath xpath;
    Document doc = null;

    public SempReplyDomParser(String sempXml) throws SempParserException {
        if (sempXml == null) throw new NullPointerException("SEMP XML String passed to parse() is null");
        this.semp = sempXml;
        dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(true);
        xpathFactory = XPathFactory.newInstance();
        try {
            db = dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new Error("Internal problem initiazling XML DOM components",e);
        }
        xpath = xpathFactory.newXPath();
        try {
            doc = db.parse(new ByteArrayInputStream(sempXml.getBytes("UTF-8")));
            doc.normalizeDocument();
        } catch (IOException e) {
            throw new SempParserException("Internal I/O exception caught while reading in SEMP XML",e);
        } catch (SAXException e) {
            System.err.println(sempXml);
            throw new SempParserException("Internal SAX exception caught while parsing SEMP XML",e);
        }
    }
    
    
    public static String parseSempErrorReason(String sempResponse) {
        Matcher matchReason = PATTERN_REASON.matcher(sempResponse);
        Matcher matchError = PATTERN_ERROR.matcher(sempResponse);
        if (matchReason.find()) {
            return matchReason.group(1);
        } else if (matchError.find()) {
            return matchError.group(1);
        }
        else return sempResponse;
    }

    public String getSemp() {
        return semp;
    }

    public static String getMoreCookie(String semp) {
        Matcher m = SEMP_MORE_COOKIE.matcher(semp.substring(Math.max(0, semp.length()-5000)));
        if (m.find()) return m.group(1);
        else return null;
    }

    public String evaluateSingularXpath(String xpathExpressionString) throws SempParserException {
        return evaluateSingularXpath(xpathExpressionString,null);
    }

    public String evaluateSingularXpath(String xpathExpressionString, Object startingNode) throws SempParserException {
        if (startingNode == null) startingNode = doc;
        XPathExpression xpathExpr;
        try {
            synchronized(xpath) {
                xpathExpr = xpath.compile(xpathExpressionString);
            }
            return (String)xpathExpr.evaluate(startingNode, XPathConstants.STRING);
        } catch (XPathExpressionException e) {
            //logger.error("Internal error configuring XPath parser for SEMP XML",e);
            throw new SempParserException("something bad",e);
        }
    }

    public NodeList evaluateXpath(String xpathExpressionString, Object startingNode) throws SempParserException {
        if (startingNode == null) throw new NullPointerException("Uninitialized starting node");
        XPathExpression xpathExpr;
        NodeList nodes = null;
        try {
            synchronized(xpath) {
                xpathExpr = xpath.compile(xpathExpressionString);
            }
            nodes = (NodeList)xpathExpr.evaluate(startingNode, XPathConstants.NODESET);
            //logger.debug(String.format("Number of DOM nodes matching XPath '%s' == %d",xpathExpressionString,nodes.getLength()));
            return nodes;
        } catch (XPathExpressionException e) {
            //logger.error("Internal error configuring XPath parser for SEMP XML",e);
            throw new SempParserException("something bad",e);
        }
    }

    public NodeList evaluateXpath(String xpathExpressionString) throws SempParserException {
        if (doc == null) throw new UnsupportedOperationException("Have not properly parsed a document");
        return evaluateXpath(xpathExpressionString,doc);
    }

    public Document getDocument() {
        return doc;
    }
}