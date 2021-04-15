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

package com.solace.psg.enterprisestats.statspump.semp;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

import com.solace.psg.enterprisestats.statspump.PhysicalAppliance;
import com.solace.psg.enterprisestats.statspump.containers.ContainerFactory;
import com.solace.psg.enterprisestats.statspump.containers.GroupedContainerSet;
import com.solace.psg.enterprisestats.statspump.containers.SingleContainerSet;
import com.solace.psg.enterprisestats.statspump.pollers.Poller;
import com.solace.psg.enterprisestats.statspump.profiler.NoopSempProcessorListener;
import com.solace.psg.enterprisestats.statspump.tools.PumpConstants;
import com.solace.psg.enterprisestats.statspump.tools.semp.SempReplySchemaLoader;
import com.solace.psg.enterprisestats.statspump.tools.semp.XsdDataType;

public class GenericSempSaxHandler extends DefaultHandler implements LexicalHandler, ErrorHandler {

    protected final Poller poller;
    protected final PhysicalAppliance physical;
    protected final Set<ContainerFactory> factories;
    protected SingleContainerSet containerSet;
    protected GroupedContainerSet groupedContainerSet;
    //protected final Set<GroupedContainer> groupedContainers = new HashSet<GroupedContainer>();
    protected final SempSaxProcessingListener processingListener;
    private int elementCount = 0;
    private int objectCount = 0;
    private int messageCount = 0;

    private boolean hasStarted = false;        // needed for processing... have we seen the basetag yet
    private boolean isAfterEndTag = false;     // needed for processing... are we after a closing XML tag (ignore proceeding chars then)
    private String sempVersion = "";           // should extract something like "soltr/6_2" from rpc-reply tag
    private StringBuilder characterData = new StringBuilder();   // need StringBuilder for the 'characters' method since SAX can call it multiple times
    private StringBuilder currentFullTag;      // will look like /rpc-reply/rpc/show/message-vpn/vpn
    private StringBuilder currentRelativeTag;  // everything after the base tag: /name or /virtual-routers/primary/status/activity
    private StringBuilder commentSB;           // in case there's an error, look for the comment in the XML

    protected final String baseTag;            // e.g. /rpc-reply/rpc/show/message-vpn/vpn  or
    private final boolean isBaseTagRegex;
    protected final Pattern baseTagPattern;    // e.g. /rpc-reply/rpc/show/client/*/client ==> /rpc-reply/rpc/show/client/[^/]+/client
    protected final boolean isGrouped;         // if is grouped, then will emit a new message each time at least one object tag is different than previous

    protected final Map<String,String> objectTagsMap;    // e.g. "VPN_NAME_TAG"->"/name"
    protected int numTagsToFind = 0;
    protected final Map<String,String> tagsValuesMap;    // to find: e.g. "/name" -> vpn_risk_uk_prod
    protected Map<String,String> objectValuesMap;        // to return on onStatMessage(), will generate map: VPN_NAME_TAG -> vpn_risk_uk_prod
    protected Map<String,String> prevObjectValuesMap;    // for grouped things

    private static final Logger logger = LoggerFactory.getLogger(GenericSempSaxHandler.class);
    
    private static final String RPC_REPLY_TAG = "rpc-reply";
    private static final String EXECUTE_RESULT_TAG = "execute-result";
    private static final String EXECUTE_RESULT_CODE_ATTRIBUTE = "code";
    private static final String SEMP_VERSION_ATTRIBUTE_TAG = "semp-version";
    private static final String MORE_COOKIE_TAG = "more-cookie";
    
    private long messageTimestamp = 0;

    private String foundErrorTag = null;
    private boolean hasStartedMoreCookie = false;
    private StringBuilder moreCookieSB = null;
    
    public GenericSempSaxHandler(Poller poller, PhysicalAppliance physical, SempSaxProcessingListener listener, Map<String,String> objectTags) {
        this.poller = poller;
        this.physical = physical;
        this.factories = physical.getLogical().getContainerFactories(poller.getDestination());
        if (listener == null) {
            processingListener = new NoopSempProcessorListener();
            logger.debug("SempSaxProcessingListner passed to SaxParser is null... using NoopProcessorListner");
        } else {
            processingListener = listener;
        }
        assert poller.getBaseTag() != null;  // this should all be checked at the building poller level
        baseTag = poller.getBaseTag();
        if (baseTag.contains("*")) {
            isBaseTagRegex = true;
            baseTagPattern = Pattern.compile(baseTag.replaceAll("\\*","[^/]+"));
        } else {
            isBaseTagRegex = false;
            baseTagPattern = null;
        }
        // initialize containerSet later
        groupedContainerSet = new GroupedContainerSet(factories);
        this.isGrouped = physical.getLogical().hasGroupedBuses();
        if (objectTags == null) {
            assert !isGrouped;
            objectTagsMap = Collections.unmodifiableMap(Collections.<String,String>emptyMap());
            prevObjectValuesMap = null;
        } else {
            objectTagsMap = Collections.unmodifiableMap(new HashMap<String,String>(objectTags));
            prevObjectValuesMap = new HashMap<String,String>();
        }
        tagsValuesMap = new HashMap<String,String>();
        numTagsToFind = tagsValuesMap.keySet().size();
    }


    @Override
    public void startDocument() throws SAXException {
        // reset all the vars, as this Handler object can get reused
        elementCount = 0;
        objectCount = 0;
        messageCount = 0;
        hasStarted = false;
        hasStartedMoreCookie = false;
        moreCookieSB = null;
        isAfterEndTag = false;
        foundErrorTag = null;
        currentFullTag = new StringBuilder();
        currentRelativeTag = new StringBuilder();
        commentSB = new StringBuilder();
    }
    
    @Override
    public void endDocument() throws SAXException {
        // first, if we're grouped, and we're totally done (no more more-cookies)
        //TODO put the grouped stuff back in
//        if (isGrouped && getMoreCookie()==null && !groupedContainer.isEmpty()) {
//            // then if there's still a GroupedContainer being built, need to send now
//            processingListener.onStatMessage(groupedContainer,prevObjectValuesMap);
//            messageCount++;
//            // don't need to clear the maps at this point b/c we know we won't be running again
//        }
        processingListener.onEndSempReply();
    }

    /** Returns the toString() of the current full tag */
    private final String getCurrentFullTag() {
        return currentFullTag.toString();
    }
    
    private final String getCurrentRelativeTag() {
        return currentRelativeTag.toString();
    }

    @Override
    public final void characters(char[] ch, int start, int length) {
        // first, check if we're currently located past a closing XML tag... if so, duck out (the chars will just be carriage return + whitespace)
        if (isAfterEndTag) return;
        // no need to trim it anymore since we set length=0 after each XML tag, no fear of extra chars in data values! should save a lot on processing since this is a tight loop
        // the 'after end tag' boolean not really needed if setting the length to 0 each time, but probably faster to do boolean check than appending whitespace String array data for no reason
        characterData.append(String.valueOf(ch,start,length));
    }

    @Override
    public final void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        isAfterEndTag = false;
        currentFullTag.append('/').append(qName);
        characterData.setLength(0);  // start capturing a new bunch of characters after this start... re-use to prevent object creation
        if (hasStarted) {
            currentRelativeTag.append('/').append(qName);
            if (!SempReplySchemaLoader.isElement(sempVersion,getCurrentFullTag())) {
                containerSet.startNestedElement(qName,SempReplySchemaLoader.isMultiple(sempVersion,getCurrentFullTag()));
            }
        } else if ((!isBaseTagRegex && getCurrentFullTag().equals(baseTag)) || (isBaseTagRegex && baseTagPattern.matcher(getCurrentFullTag()).matches())) {
            // oh boy, we are starting! found the base tag!
            hasStarted = true;
            // initialize stuff
            containerSet = new SingleContainerSet(factories,sempVersion,getCurrentFullTag());
            for (String key : this.objectTagsMap.values()) {  // blank the values
                tagsValuesMap.put(key,PumpConstants.UNINITIALIZED_VALUE);
            }
            numTagsToFind = tagsValuesMap.keySet().size();
            objectValuesMap = new HashMap<String,String>();  // define a new objectValuesMap to use... need a new one as the previous one gets passed to the message handler
        } else if (hasStartedMoreCookie) {
            moreCookieSB.append('<').append(localName);
            for (int i=0;i<atts.getLength();i++) {
                moreCookieSB.append(' ').append(atts.getLocalName(i)).append("=\"").append(atts.getValue(i)).append("\"");
            }
            moreCookieSB.append('>');
        } else if (localName.equals(RPC_REPLY_TAG)) {
            sempVersion = atts.getValue(SEMP_VERSION_ATTRIBUTE_TAG);
            processingListener.onStartSempReply(sempVersion,messageTimestamp);
        } else if (localName.equals(MORE_COOKIE_TAG)) {
            hasStartedMoreCookie = true;
            moreCookieSB = new StringBuilder();
        } else if (localName.equals(EXECUTE_RESULT_TAG)) {
            if (!"ok".equals(atts.getValue(EXECUTE_RESULT_CODE_ATTRIBUTE))) {  // reversed in case getValue() returns null
                StringBuilder badExecuteCodeSb = new StringBuilder(String.format("Execute result code '%s' for %s on %s. [",atts.getValue(EXECUTE_RESULT_CODE_ATTRIBUTE),poller,physical));
                for (int i=0;i<atts.getLength();i++) {
                    if (i>0) {  // add a space preceeding for every one except 1st guy
                        badExecuteCodeSb.append(' ');
                    }
                    badExecuteCodeSb.append(String.format("%s=\"%s\"",atts.getLocalName(i),atts.getValue(i)));
                }
                badExecuteCodeSb.append(']');
                if (!getComment().isEmpty()) {
                    badExecuteCodeSb.append(String.format(" ** Comment: '%s'",commentSB.toString().trim()));
                }
                //TODO I think this should be our own exception
                throw new SAXException(badExecuteCodeSb.toString());
            }
        } else if (localName.endsWith("-error")) {
            foundErrorTag = localName;  // this is a flag for endElement() so that it knows something is wrong
        }
    }

    @Override
    public final void endElement(String namespaceURI, String localName, String qName) throws SAXException, SempSchemaValidationException {
        isAfterEndTag = true;
        if (hasStarted) {  // if we're not 'started' yet, then we can safely ignore
            if (currentRelativeTag.toString().isEmpty()) {  // done... closing the baseTag
                assert baseTag.endsWith(localName);  // might not be true if wildcard on last level
                assert getCurrentFullTag().endsWith(qName);   // e.g. /rpc-reply/rpc/show/message-vpn/vpn/stats ends with 'stats'
                hasStarted = false;
                objectCount++;
                // collapse the 2 maps so that it's in the useful form (e.g.) VPN_NAME_TAG -> vpn_risk_uk_prod
                for (String key : objectTagsMap.keySet()) {
                    objectValuesMap.put(key,tagsValuesMap.get(objectTagsMap.get(key)));
                }
                // now we have to check if grouped
                if (isGrouped) {
                    //TODO finish! ********************************************************************************************
                    // if so, see if any of the values are different
					//                    if (objectValuesMap.equals(prevObjectValuesMap) || prevObjectValuesMap.isEmpty()) {
					//                        groupedContainer.addContainer(container);
					//                    } else {
					//                        processingListener.onStatMessage(groupedContainer,prevObjectValuesMap);
					//                        messageCount++;
					//                        groupedContainer = factory.newGroupedContainer(currentFullTag.toString());
					//                        groupedContainer.addContainer(container);
					//                    }
					//                    prevObjectValuesMap.putAll(objectValuesMap);  // overwrite the previous values with the new ones
                } else {
                    processingListener.onStatMessage(containerSet,objectValuesMap);
                    messageCount++;
                }
                
            } else {  // not at the base tag yet, still processing...
                if (SempReplySchemaLoader.isElement(sempVersion,getCurrentFullTag())) {
                    // check if this tag is one of our tags we're trying to find... if so, add it to the map:
                    if (numTagsToFind > 0 && tagsValuesMap.keySet().contains(getCurrentRelativeTag())) {
                        tagsValuesMap.put(getCurrentRelativeTag(),characterData.toString());
                        numTagsToFind--;
                    }
                    //logger.debug(String.format("%s:'%s' (%s)",qName,getCharacterData(),SempSchemaHandler.getType(getSempVersion(),getCurrentFullTag())));  // key:value pair
                    if ("should we do type checking?".equals("no_type_checking")) {
                        // insert everything as a string
                        containerSet.putString(XsdDataType.STRING,qName,characterData.toString(),SempReplySchemaLoader.isMultiple(sempVersion,getCurrentFullTag()));
                    } else {
                        // else look at the actual data type
                        containerSet.putString(SempReplySchemaLoader.getType(sempVersion,getCurrentFullTag()),qName,characterData.toString(),SempReplySchemaLoader.isMultiple(sempVersion,getCurrentFullTag()));
                    }
                    elementCount++;
                } else {
                    // we're closing a non-element tag (i.e. internal) tag like the attributes or event thresholds or something
                    containerSet.closeNestedElement(qName);
                }
                currentRelativeTag.setLength(currentRelativeTag.length()-(qName.length()+1));  // pop off the last level of the relative tag
            }
        } else if (hasStartedMoreCookie) {  // if we're inside the more-cookie
            moreCookieSB.append(characterData);
        	if (localName.equals(MORE_COOKIE_TAG)) {  // and this is the closing tag
        		hasStartedMoreCookie = false;
        	} else {  // otherwise, add the contents to the string builder
            	moreCookieSB.append("</").append(localName).append(">\r\n");
        	}
        } else if (foundErrorTag != null) {
            if (characterData.toString().contains("schema validation")) {
                throw new SempSchemaValidationException(String.format("SEMP tag <%s> received during %s on %s. Error message: \"%s\"%s",
                        foundErrorTag,poller,physical,characterData.toString(),
                        commentSB!=null?String.format(" ** Comment: '%s'",commentSB.toString().trim()):""));
            } else {
                throw new SAXException(String.format("SEMP tag <%s> received during %s on %s. Error message: \"%s\"%s",
                        foundErrorTag,poller,physical,characterData.toString(),
                        commentSB!=null?String.format(" ** Comment: '%s'",commentSB.toString().trim()):""));
            }
        }
        characterData.setLength(0);  // re-use to prevent object creation
        currentFullTag.setLength(currentFullTag.length()-(qName.length()+1));  // remove last element and slash
    }

    public String getSempVersion() {
        return sempVersion;
    }
    
    public String getMoreCookie() {
    	if (moreCookieSB == null) {
    		return null;
    	} else {
            //logger.debug("RPC more-cookie is "+moreCookieSB.length()+" chars long!");
    		return moreCookieSB.toString();
    	}
    }
    
    /**
     * An element is a single datum.  E.g. <code>&lt;connections&gt;100&lt;/connections&gt;</code>
     */
    public int getElementCount() {
        return elementCount;
    }

    /**
     * An object is a queue, a VPN, a client
     */
    public int getObjectCount() {
    	return objectCount;
    }
    
    /**
     * Unless using grouped results (not currently implemented), this should be the same as object count
     */
    public int getMessageCount() {
        return messageCount;
    }
    
    public long getMessageTimestamp() {
        return messageTimestamp;
    }

    public void setMessageTimestamp(long timestamp) {
        messageTimestamp = timestamp;
    }
    
    public String getComment() {
        return commentSB.toString();
    }

    /* The below methods are from the Lexical Handler... don't care about any of them except comments */
    
    @Override
    public void comment(char[] arg0, int arg1, int arg2) throws SAXException {
        commentSB.append(new String(arg0,arg1,arg2).replaceAll("\\.\n",". ").replaceAll("\n",""));  // regex replace is slow, but doesn't matter for an error
    }

    @Override
    public void endCDATA() throws SAXException { }

    @Override
    public void endDTD() throws SAXException { }
    
    @Override
    public void endEntity(String name) throws SAXException { }

    @Override
    public void startCDATA() throws SAXException { }

    @Override
    public void startDTD(String name, String publicId, String systemId) throws SAXException { }

    @Override
    public void startEntity(String name) throws SAXException { }

    
    // The code below can be used for informal unit testing
    
//    public static void main2(String... args) throws IOException {
//        String filename = "show_queue_detail.txt";
//        SempReplySchemaLoader.loadSchemas();
//        InputStream inputStream = SempReplySchemaLoader.class.getClassLoader().getResourceAsStream(filename);
//        if (inputStream == null) {
//            System.err.println("couldn't find the file");
//            System.exit(1);  // couldn't find the file
//        }
//        try {
//            SAXParserFactory saxFactory = SAXParserFactory.newInstance();
//            saxFactory.setNamespaceAware(true);
//            saxFactory.setValidating(true);
//            //SAXParser saxParser = 
//                    saxFactory.newSAXParser();
////            saxParser.parse(inputStream,QueueDetailPoller.getInstance().buildSaxHandler(null, new NoopSempProcessorListener()));
//        } catch (ParserConfigurationException e) {
//            logger.info("Caught this",e);
//        } catch (SAXException e) {
//            logger.info("Caught this",e);
//        } catch (RuntimeException e) {
//            logger.info("Caught this",e);
//        }
//    }

}
