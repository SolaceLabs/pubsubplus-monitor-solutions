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

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/*
 * This class is used by the SempReplySchemaLoader for parsing the supplied SEMP schema files.
 * It walks through the XSD file and builds a set of key-value pairs, where each key is a nested
 * set of XML elements (e.g. rpc-reply/rpc/show/message-vpn/) and each value is the XSD data type
 * (e.g. String, Float, Boolean, etc.).  Further, it provides a list of all elements that can be
 * repeated multiple times, in a loop, or as a stream.
 * 
 * I had to code this myself as I could find no alternative XML schema parsing library... all the
 * alternatives build marshalling/unmarshalling code to generate Java Ojbects from XML, but the
 * Stats Pump simply needs to know, for each XML attribute, what is the data type and does it repeat.
 */

class SempReplySchemaHandler extends DefaultHandler {
    
    private final static String TAG_ELEMENT = "element";
    private final static String TAG_SEQUENCE = "sequence";        // needed for multiples
    private final static String TAG_CHOICE = "choice";            // needed for multiples
    private final static String TAG_RESTRICTION = "restriction";  // needed in SAX parser
    private final static String TAG_EXTENSION = "extension";      // needed in SAX parser
    private final static String TAG_SCHEMA = "schema";
    private final static String TAG_ALL = "all";
    private final static String TAG_DOCUMENTATION = "documentation";
    private final static String TAG_ANNOTATION = "annotation";
    private final static String TAG_ENUMERATION = "enumeration";
    private final static String TAG_ATTRIBUTE = "attribute";
    private final static Set<String> IGNORABLE_TAGS = new HashSet<String>(
            Arrays.asList(TAG_SCHEMA,TAG_ALL,TAG_DOCUMENTATION,TAG_ANNOTATION,TAG_ENUMERATION,TAG_ATTRIBUTE));

    /**
     * Meant to represent a single XML schema tag, usually one per line.
     * A tag is a member of a tree of schema tags, therefore each tag
     * will have a parent (except for the 'root'), and possibly have children
     * tags as well.
     * 
     * @author Aaron Lee
     */
    private static class SchemaTag {

        private final String tagType;
        private final String name;
        private final String type;
        private final boolean multiples;
        private final SchemaTag parent;
        
        private SchemaTag(String tagType, String name, String type, boolean multiples, SchemaTag parent) {
            this.tagType = tagType;
            this.name = name;
            this.type = type;
            this.multiples = multiples;
            this.parent = parent;
        }

        @Override
        public String toString() {
            if (parent == null) {  // root element, must have a name
                assert name != null;
                return String.format("[%s] %s",tagType.substring(0,Math.min(4,tagType.length())),name);
            } else {
                if (name == null) return String.format("%s/[%s]%s%s",parent.toString(),tagType.substring(0,Math.min(4, tagType.length())),(multiples?"*":""),(type!=null?" ("+type+")":""));
                else return String.format("%s/[%s]%s %s%s",parent.toString(),tagType.substring(0,Math.min(4, tagType.length())),(multiples?"*":""),name,(type!=null?" ("+type+")":""));
            }
        }
        
        /**
         * Recursively builds the full tag String, ignoring parent tags that have no name, until reaching the root.
         * If root is not element (e.g. complexType or simple definition) then root tag name is not included in full tag.
         */
        private String buildFullTag() {
            if (parent == null) {
                if (tagType.equals(TAG_ELEMENT)) return "/"+name;
                else return "";  // if this isn't an element, then it's a definition, so don't include the name in the full tag
            } else {
                if (name == null) return parent.buildFullTag();
                else return new StringBuilder(parent.buildFullTag()).append('/').append(name).toString();
            }
        }
        
        /**
         * Finds the ancestor/root tag of this tag via recursion and returns empty string if it is an actual element (like rpc-reply),
         * or the name if it is not (either an xs:complexType or xs:simpleType definition)
         */
        private String getDefinitionName() {
            if (parent == null) {
                if (tagType.equals(TAG_ELEMENT)) return "";
                else return name;
            } else return parent.getDefinitionName();
        }
        
        /** this tag is an element with multiples, or the parent is a sequence or choice with multiples (i.e. maxOccurs > 1) */
        private boolean isMultiples() {
            return (tagType.equals(TAG_ELEMENT) && multiples) ||
                    (parent != null && (parent.tagType.equals(TAG_SEQUENCE) || parent.tagType.equals(TAG_CHOICE)) && parent.multiples);
        }
        
        /**
         * Returns the type of this tag (e.g. complexType, element, etc.)
         */
        private String getType() {
            return type;
        }
    }
    

    
    // THE MAIN CLASS ////////////////////////////////////////////////////////////////////////
    
    private final static String ATTS_NAME = "name";
    private final static String ATTS_TYPE = "type";
    private final static String ATTS_BASE = "base";
    private final static String ATTS_MAXOCCURS = "maxOccurs";
    
    private String sempVersion = "undefined";

    private Deque<SchemaTag> currentFullTag = new ArrayDeque<SchemaTag>();
    private Map<String,String> elements = new LinkedHashMap<String,String>();  // temp structure used while reading the schema... tracks actual elements (rpc-reply)
    private Map<String,Map<String,String>> definitions = new LinkedHashMap<String,Map<String,String>>();  // temp used while building... tracks complex/simple definitions
    private Map<String,Set<String>> isMultipleMap = new LinkedHashMap<String,Set<String>>();  // which elements are multiples

    private final Map<String,Map<String,XsdDataType>> allTagsTypeMaps;
    private final Map<String,Set<String>> allIsMultipleSets;

    private static final Logger logger = LoggerFactory.getLogger(SempReplySchemaHandler.class);

    SempReplySchemaHandler(Map<String,Map<String,XsdDataType>> allTagsTypeMaps, Map<String,Set<String>> allIsMultipleSets) {
        this.allTagsTypeMaps = allTagsTypeMaps;
        this.allIsMultipleSets = allIsMultipleSets;
    }
    
    @Override
    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        if (IGNORABLE_TAGS.contains(localName)) return;
        currentFullTag.removeLast();
    }

    @Override
    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        // new check added in so that we dynamically find the semp version from the attribute tag at the bottom of rpc-reply
        if (localName.equals("attribute")) {
            if (atts.getValue("name").equals("semp-version") && currentFullTag.peekFirst().name.equals("rpc-reply")) {
                sempVersion = atts.getValue("fixed");
            }
        }
        if (IGNORABLE_TAGS.contains(localName)) return;
        SchemaTag tag = new SchemaTag(
                localName,                   // e.g. xs:element or xs:complexType
                atts.getValue(ATTS_NAME),    // if it has an attributed callled 'name'
                atts.getValue(ATTS_TYPE) != null?atts.getValue(ATTS_TYPE):atts.getValue(ATTS_BASE),  // either the type, or base, or null
                atts.getValue(ATTS_MAXOCCURS) != null,
                currentFullTag.peekLast());  // and the parent for this new tag is the last guy currently
        currentFullTag.addLast(tag);

        // now, if it's an element and type is defined, or is a special modifier (restriction, extension)
        if ((localName.equals(TAG_ELEMENT) && atts.getValue(ATTS_TYPE) != null) ||  // elements that have a type get written down
                localName.equals(TAG_RESTRICTION) ||  // restriction tells you what datatype the parent element is (string, int, etc.)
                localName.equals(TAG_EXTENSION)) {    // only a couple extensions, but essentially they just get copied in
            logger.trace(String.format("%s %s - %s",tag.getDefinitionName(),tag.buildFullTag(),tag.getType()));
            if (tag.getDefinitionName().isEmpty()) {  // this is a proper element, not a definition (probably roots at rpc-reply)
                elements.put(tag.buildFullTag(),tag.getType());
            } else {  // definition (complex, simple) which gets copied into the elements during post-processing
                if (!definitions.containsKey(tag.getDefinitionName())) {
                    definitions.put(tag.getDefinitionName(), new LinkedHashMap<String,String>());
                }
                definitions.get(tag.getDefinitionName()).put(tag.buildFullTag(),tag.getType());
            }
        }
        if (tag.isMultiples()) {
            if (!isMultipleMap.containsKey(tag.getDefinitionName())) {
                isMultipleMap.put(tag.getDefinitionName(), new HashSet<String>());
            }
            isMultipleMap.get(tag.getDefinitionName()).add(tag.buildFullTag());
            logger.trace(String.format("isMultiple: '%s' %s",tag.getDefinitionName(),tag.buildFullTag()));
        }
    }

    @Override
    public void endDocument() throws SAXException {
        Map<String,XsdDataType> tagsTypeMap = new LinkedHashMap<String,XsdDataType>();
        Set<String> isMultipleSet = new HashSet<String>();

        // all done... 
        // first, add all the multiple tags from the rpc-reply element
        isMultipleSet.addAll(isMultipleMap.get(""));
        // then, time to build the final version objects with the Enum datatypes
        for (String element : elements.keySet()) {
            // for each rpc-reply-rooted element, recursively build all elements
            buildFinalTags(element,elements.get(element),tagsTypeMap,isMultipleSet);
        }
        tagsTypeMap = Collections.unmodifiableMap(tagsTypeMap);  // parent class instance
        allTagsTypeMaps.put(sempVersion,tagsTypeMap);
        isMultipleSet = Collections.unmodifiableSet(isMultipleSet);  // parent class instance
        allIsMultipleSets.put(sempVersion,isMultipleSet);
        elements.clear();
        elements = null;
        definitions.clear();
        definitions = null;
        isMultipleMap.clear();
        isMultipleMap = null;
        currentFullTag.clear();
        currentFullTag = null;
    }

    private void buildFinalTags(String prefix, String type, Map<String,XsdDataType> tagsTypeMap, Set<String> isMultipleSet) {
        // dump out the objects
        if (type.startsWith("xs:")) {  // an actual standard/primitive datatype
            // i.e. prefix = the whole tag!
            logger.trace(String.format("%s %s  [%s]",isMultipleMap.get("").contains(prefix)?"$":"-",prefix,type));
            try {
                // trim off the xs: and see what matches: e.g. xs:string -> STRING
                tagsTypeMap.put(prefix, XsdDataType.valueOf(type.substring(3).toUpperCase()));
                logger.trace(String.format("%s - %s",prefix,type));
            } catch (IllegalArgumentException e) {  // i.e. not one we know about apparently!
                logger.info(String.format("The SEMP schema parser encountered a new unknown type: %s... defaulting to 'UNKNOWN'", type));
                tagsTypeMap.put(prefix, XsdDataType.UNKNOWN);
            }
        }
        else {  // means its a complex or simple definition (i.e. not an element with a type)
            if (isMultipleMap.containsKey(type)) {
                for (String definition : isMultipleMap.get(type)) {
                    isMultipleSet.add(prefix+definition);
                }
            }
            if (definitions.containsKey(type)) {
                // now comes the recursive bit...
                for (String definition : definitions.get(type).keySet()) {
                    buildFinalTags(prefix+definition,definitions.get(type).get(definition),tagsTypeMap,isMultipleSet);
                }
            } else {
                // it's ok... it might be an empty tag definition, like  <xs:complexType name="jndi-property-list"/>
                // there is at least one of those in the current schema
                //logger.debug(String.format("FYI: definitions for v%s does not have %s for prefix %s",sempVersion,type,prefix));
            }
        }
    }
}


