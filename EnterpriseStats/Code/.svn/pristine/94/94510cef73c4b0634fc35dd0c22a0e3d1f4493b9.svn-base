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
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.helpers.DefaultHandler;

import com.solace.psg.enterprisestats.statspump.tools.parsers.SaxParser;
import com.solace.psg.enterprisestats.statspump.tools.parsers.SaxParserException;
import com.solace.psg.enterprisestats.statspump.tools.util.ResourceList;

/**
 * This crazy class parses the SEMP reply schema and builds a simple list
 * 
 * @author Aaron Lee
 */
public class SempReplySchemaLoader {
    
    private final DefaultHandler handler;
    //private final String sempVersion;

    private static final Logger logger = LoggerFactory.getLogger(SempReplySchemaLoader.class);

    private final static Map<String,Map<String,XsdDataType>> allTagsTypeMaps = new HashMap<String,Map<String,XsdDataType>>();
    private final static Map<String,Set<String>> allIsMultipleSets = new HashMap<String,Set<String>>();
    
    public SempReplySchemaLoader() {
        handler = new SempReplySchemaHandler(allTagsTypeMaps,allIsMultipleSets);
    }

    public static void loadSchemas() {
        //Pattern pattern = Pattern.compile(".*semp-rpc-(?:reply-)?soltr.xsd");  // generally, the schema files are in this form: 
        Pattern pattern = Pattern.compile(".*semp-rpc-reply-soltr.xsd");
        final Set<URL> list = ResourceList.getResources(pattern);
        logger.info("SempReplySchemaLoader found " + list.size() + "schemas... loading them");

        for (final URL url : list){
            logger.info("Loading SEMP Reply schema: "+url);
            loadSchemaName(url);
        }
    }
        
    private static boolean loadSchemaName(URL schemaFilename) {
        try {
            InputStream inputStream = schemaFilename.openStream();
            SaxParser.parseStream(inputStream,new SempReplySchemaLoader().handler);
            return true;
        } catch (SaxParserException e) {
            logger.info("Caught this",e);
        } catch (IOException e) {
            logger.info("Caught this",e);
        } catch (RuntimeException e) {
            logger.info("Caught this",e);
        }
        return false;
    }

    private static Map<String,XsdDataType> getTags(String schemaVersion) {
        return allTagsTypeMaps.get(schemaVersion);
    }

    public static XsdDataType getFlattenedType(String schemaVersion, String prefix, String tag) {
        if (getTags(schemaVersion) != null) {
            tag = tag.replaceAll("\\|[0-9]+","");  // remove any array/nested portions: e.g. vpn/clients/client|3/name
            return allTagsTypeMaps.get(schemaVersion).get(new StringBuilder(prefix).append('/').append(tag).toString());
        }
        else throw new IllegalStateException(String.format("Have not loaded schemas for '%s'. Ensure schema version looks like 'soltr/x_y'. Call loadSchemas() first, or import new schema files.",schemaVersion));
    }
    
    public static XsdDataType getType(String schemaVersion, String tag) {
        if (getTags(schemaVersion) != null) return allTagsTypeMaps.get(schemaVersion).get(tag);
        else throw new IllegalStateException(String.format("Have not loaded schemas for '%s'. Ensure schema version looks like 'soltr/x_y'. Call loadSchemas() first, or import new schema files.",schemaVersion));
    }
    
    public static boolean isElement(String schemaVersion, String tag) {
        if (getTags(schemaVersion) != null) return allTagsTypeMaps.get(schemaVersion).containsKey(tag);
        else throw new IllegalStateException(String.format("Have not loaded schemas for '%s'. Ensure schema version looks like 'soltr/x_y'. Call loadSchemas() first, or import new schema files.",schemaVersion));
    }
    
    public static boolean isMultiple(String schemaVersion, String tag) {
        if (getTags(schemaVersion) != null) return allIsMultipleSets.get(schemaVersion).contains(tag);
        else throw new IllegalStateException(String.format("Have not loaded schemas for '%s'. Ensure schema version looks like 'soltr/x_y'. Call loadSchemas() first, or import new schema files.",schemaVersion));
    }
    
    public static Set<String> getSchemaVersionsForTag(String tagToFind) {
        if (tagToFind.contains("*")) return getSchemaVersionsForTagWildcard(tagToFind);
        Set<String> returnSet = new HashSet<String>();
        for (String ver : allTagsTypeMaps.keySet()) {
            if (allTagsTypeMaps.get(ver).containsKey(tagToFind)) returnSet.add(ver);
        }

        String allSet = "";
        for (String one : returnSet) {
        	if (allSet.length()  > 0 ) {
            	allSet += ", ";
        	}
        	allSet += one;
        }
        logger.info("getSchemaVersionsForTag tagToFind= " + tagToFind + ", rc=" + allSet);

        return returnSet;
    }
    
    private static Set<String> getSchemaVersionsForTagWildcard(String tag) {
        Set<String> returnSet = new HashSet<String>();
        tag = tag.replaceAll("\\*",".*?");
        Pattern p = Pattern.compile(tag);
        for (String ver : allTagsTypeMaps.keySet()) {
            Set<String> keys = allTagsTypeMaps.get(ver).keySet();
            for (String key : keys) {
                if (p.matcher(key).matches()) returnSet.add(ver);
            }
        }
        return returnSet;
    }
    
    // The code below this point can be used for informal unit testing.
//  
//    public static void main(String... args) throws Exception {
//        SempReplySchemaLoader.loadSchemas();
//        
////        for (String key : allTagsTypeMaps.get("soltr/7_1_1").keySet()) {
////            if (key.startsWith("/rpc-reply/rpc/show/client/primary-virtual-router/client/")) {
////                System.out.println(key);
////            }
////        }
////        System.exit(1);
//        
//        System.out.println(SempReplySchemaLoader.getType("soltr/6_2","/rpc-reply/rpc/show/routing/routing-enabled"));
//        System.out.println(SempReplySchemaLoader.getType("soltr/6_2","/rpc-reply/rpc/show/smrp/database/total-block-ids-used/backup/percentage-used"));
//        System.out.println(SempReplySchemaLoader.getType("soltr/6_2","/rpc-reply/rpc/show/stats/ssl/connection-accepted"));
//        
//        System.out.println(SempReplySchemaLoader.isMultiple("soltr/6_2","/rpc-reply/rpc/show/message-spool/message-spool-info/disk-infos/disk-info"));
//        System.out.println(SempReplySchemaLoader.isMultiple("soltr/6_2","/rpc-reply/rpc/show/cache-cluster/cache-clusters/cache-cluster"));
//        System.out.println(SempReplySchemaLoader.isMultiple("soltr/6_2","/rpc-reply/rpc/show/client/backup-virtual-router"));
//        System.out.println(SempReplySchemaLoader.isMultiple("soltr/6_2","/rpc-reply/rpc/show/client/sorted-stats"));
//        System.out.println(SempReplySchemaLoader.isMultiple("soltr/6_2","/rpc-reply/rpc/show/message-spool/message-spool-info/event-configuration/event-thresholds/name"));
//        
////        System.out.println(SempReplySchemaLoader.extractSempSchemaVersion("<rpc semp-version='soltr/6_2'><show><client><name>*</n"));
////        System.out.println(SempReplySchemaLoader.extractSempSchemaVersion("<rpc semp-version=\"soltr/6_2\"><show><client><name>*</n"));
////        System.out.println(SempReplySchemaLoader.extractSempSchemaVersion("<rpc semp-version=soltr/6_2><show><client><name>*</n"));
//    }
}
