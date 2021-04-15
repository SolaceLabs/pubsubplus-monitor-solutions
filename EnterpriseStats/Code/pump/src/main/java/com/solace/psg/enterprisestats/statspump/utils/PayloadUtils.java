/*
 * Copyright 2014-2016 Solace Systems, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to use and
 * copy the Software, and to permit persons to whom the Software is furnished to
 * do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * UNLESS STATED ELSEWHERE BETWEEN YOU AND SOLACE SYSTEMS, INC., THE SOFTWARE IS
 * PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * http://www.SolaceSystems.com
 */

package com.solace.psg.enterprisestats.statspump.utils;

import java.util.Iterator;

import javax.json.JsonArray;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solacesystems.common.util.StringUtil;
import com.solacesystems.jcsmp.SDTException;
import com.solacesystems.jcsmp.SDTMap;
import com.solacesystems.jcsmp.SDTStream;

/**
 * This class has some helpful utilities for printing the contents of various StatsPump messages, but can
 * be used to print any SDT Map, SDT Stream, JSON object, or well-formed XML file.
 * 
 * @author Aaron Lee
 */
public final class PayloadUtils {
    private static final Logger logger = LoggerFactory.getLogger(PayloadUtils.class);

    /**
     * Use this to generate a nice String representation of a Solace SDT Map.
     * @param map
     * @param indent
     * @return
     */
    public static String printSdtMap(SDTMap map, final int indent) {
        if (map == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        String strIndent = StringUtil.padRight("", indent);
        Iterator<String> it = map.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            Object value;
            try {
                value = map.get(key);
            } catch (SDTException e) {
                logger.warn("This happened trying to print an SDTMap..?",e);
                continue;
            }
            String strValue = String.valueOf(value);
            String type = "NULL";
            if (value != null) {
                Class<?> valuClass = value.getClass();
                type = valuClass.getSimpleName();
                if (value instanceof SDTMap) {
                    strValue = "\n";
                    strValue += printSdtMap((SDTMap) value, indent + 2);
                } else if (value instanceof SDTStream) {
                    strValue = "\n";
                    strValue += printSdtStream((SDTStream) value, indent + 2);
                }
            }
            sb.append(strIndent);
            sb.append(String.format("Key '%s' (%s): %s", key, type, strValue));
            if (it.hasNext()) sb.append("\n");
        }
        return sb.toString();
    }
    
    /**
     * Returns a String representation of a Solace SDT Stream object.
     * @param stream
     * @param indent
     * @return
     */
    public static String printSdtStream(SDTStream stream, final int indent) {
        if (stream == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        String strIndent = StringUtil.padRight("", indent);

        while (stream.hasRemaining()) {
            Object value;
            try {
                value = stream.read();
            } catch (SDTException e) {
                logger.warn("This happened trying to print an SDTStream..?",e);
                continue;
            }
            String strValue = String.valueOf(value);
            String type = "NULL";
            if (value != null) {
                Class<?> valuClass = value.getClass();
                type = valuClass.getSimpleName();
                if (value instanceof SDTMap) {
                    strValue = "\n";
                    strValue += printSdtMap((SDTMap) value, indent + 2);
                } else if (value instanceof SDTStream) {
                    strValue = "\n";
                    strValue += printSdtStream((SDTStream) value, indent + 2);
                }
            }
            sb.append(strIndent);
            sb.append(String.format("(%s): %s", type, strValue));
            if (stream.hasRemaining()) sb.append("\n");
        }
        stream.rewind();
        return sb.toString();
    }
    
    /**
     * Helper method for generating a nicely formatted String of XML content.
     * @param xml
     * @return
     */
    public static String prettyPrintXml(String xml) {
        final int INDENT = 2;
        xml = xml.replaceAll("><",">\n<");  // put a carriage return between consecutive tags
        int stack = 0;
        StringBuilder pretty = new StringBuilder();
        //String[] rows = xml.trim().replaceAll(">", ">\n").replaceAll("<", "\n<").split("\n");
        String[] rows = xml.split("\n");
        for (int i = 0; i < rows.length; i++) {
            String row = rows[i];
            if (row.lastIndexOf("<")==0 && row.startsWith("</")) {  // closing tag
                stack--;
            }
            if (stack==0) pretty.append(String.format("%s%n",row));
            else pretty.append(String.format("%"+(INDENT*stack)+"s%s%n"," ",row));
            if (row.lastIndexOf("<")==0 && !(row.startsWith("</") || row.endsWith("/>"))) {  // only one tag on this line, and must be an open
                stack++;
            }
        }
        return pretty.toString();
    }
    
    /**
     * This method will take an Object which is a typical payload of a message
     * and attempt to generate a String formatting it correctly.  Supported formats
     * are XML (as a String), JSON (as a String), SDTMaps and SDTStreams.
     * @param o
     * @param indent
     * @return
     */
    public static String prettyPrint(Object o, int indent) {
        if (o instanceof SDTMap) {
            return printSdtMap((SDTMap)o,indent);
        } else if (o instanceof SDTStream) {
            return printSdtStream((SDTStream)o,indent);
        } else if (o instanceof JSONObject) {
            return ((JSONObject)o).toString(indent);
        } else if (o instanceof JSONArray) {
            return ((JSONArray)o).toString(indent);
        } else if (o instanceof JsonArray) {
            return ((JsonArray)o).toString();
        } else {
            return o.toString();
        }
    }

    private PayloadUtils() {
        throw new AssertionError("Cannot instantiate this utility class");
    }
    
    public static void main(String... args) throws SDTException {
        
        String longString = "<name>solmwm6</name><is-management-message-vpn>false</is-management-message-vpn><enabled>true</enabled><operational>true</operational><locally-configured>true</locally-configured><local-status>Up</local-status><distributed-cache-management-enabled>true</distributed-cache-management-enabled><unique-subscriptions>6</unique-subscriptions><total-local-unique-subscriptions>6</total-local-unique-subscriptions><total-remote-unique-subscriptions>0</total-remote-unique-subscriptions><total-unique-subscriptions>6</total-unique-subscriptions><max-subscriptions>5000000</max-subscriptions><export-subscriptions>false</export-subscriptions><export-subscriptions-percent-complete>100</export-subscriptions-percent-complete><connections>2</connections><connections-service-smf>2</connections-service-smf><connections-service-web>0</connections-service-web><connections-service-rest-incoming>0</connections-service-rest-incoming><connections-service-rest-outgoing>0</connections-service-rest-outgoing><max-connections>9000</max-connections><max-connections-service-smf>9000</max-connections-service-smf><max-connections-service-web>9000</max-connections-service-web><max-connections-service-rest-incoming>9000</max-connections-service-rest-incoming><max-connections-service-rest-outgoing>6000</max-connections-service-rest-outgoing><auth-type>noauthentication</auth-type><auth-profile></auth-profile><radius-domain></radius-domain><authentication><basic-auth><enabled>true</enabled><auth-type>noauthentication</auth-type><auth-profile></auth-profile><radius-domain></radius-domain></basic-auth><client-cert-auth><enabled>false</enabled><max-chain-depth>3</max-chain-depth><validate-cert-date>true</validate-cert-date><allow-api-provided-username>false</allow-api-provided-username></client-cert-auth><kerberos-auth><enabled>false</enabled><allow-api-provided-username>false</allow-api-provided-username></kerberos-auth></authentication><maximum-spool-usage-mb>0</maximum-spool-usage-mb><maximum-transacted-sessions>16000</maximum-transacted-sessions><maximum-transactions>50000</maximum-transactions><services><service><enabled>true</enabled><operating-state>true</operating-state><ssl>false</ssl><compressed>false</compressed><service-name>SMF</service-name><vrf>MsgBB</vrf><port>55555</port></service><service><enabled>true</enabled><operating-state>true</operating-state><ssl>false</ssl><compressed>true</compressed><service-name>SMF</service-name><vrf>MsgBB</vrf><port>55003</port></service><service><enabled>true</enabled><operating-state>true</operating-state><ssl>true</ssl><compressed>false</compressed><service-name>SMF</service-name><vrf>MsgBB</vrf><port>55443</port></service><service><enabled>false</enabled><operating-state>false</operating-state><ssl>false</ssl><service-name>REST</service-name><vrf>MsgBB</vrf><failed-reason>Portnotassigned</failed-reason></service><service><enabled>false</enabled><operating-state>false</operating-state><ssl>true</ssl><service-name>REST</service-name><vrf>MsgBB</vrf><failed-reason>Portnotassigned</failed-reason></service></services>";
        System.out.println(prettyPrintXml(longString));
        System.exit(0);
    }
}
