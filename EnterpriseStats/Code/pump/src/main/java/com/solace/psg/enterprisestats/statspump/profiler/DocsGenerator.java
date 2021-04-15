/**
 * Copyright 2016 Solace Systems, Inc. All rights reserved.
 *
 * http://www.solacesystems.com
 *
 * This source is distributed under the terms and conditions
 * of any contract or contracts between Solace Systems, Inc.
 * ("Solace") and you or your company.
 * If there are no contracts in place use of this source
 * is not authorized.
 * No support is provided and no distribution, sharing with
 * others or re-use of this source is authorized unless
 * specifically stated in the contracts referred to above.
 *
 * This product is provided as is and is not supported
 * by Solace unless such support is provided for under 
 * an agreement signed between you and Solace.
 */
package com.solace.psg.enterprisestats.statspump.profiler;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.solace.psg.enterprisestats.statspump.PhysicalAppliance;
import com.solace.psg.enterprisestats.statspump.StatsPumpMessage;
import com.solace.psg.enterprisestats.statspump.StatsPumpMessage.MessageType;
import com.solace.psg.enterprisestats.statspump.config.ConfigLoader;
import com.solace.psg.enterprisestats.statspump.config.ConfigLoaderException;
import com.solace.psg.enterprisestats.statspump.config.LocalFileConfigStreamsImpl;
import com.solace.psg.enterprisestats.statspump.containers.ContainerFactory;
import com.solace.psg.enterprisestats.statspump.containers.SingleContainerSet;
import com.solace.psg.enterprisestats.statspump.pollers.GenericPoller;
import com.solace.psg.enterprisestats.statspump.pollers.HostnamePoller;
import com.solace.psg.enterprisestats.statspump.pollers.MessageSpoolPoller;
import com.solace.psg.enterprisestats.statspump.pollers.Poller;
import com.solace.psg.enterprisestats.statspump.pollers.RedundancyPoller;
import com.solace.psg.enterprisestats.statspump.pollers.VpnDetailPoller;
import com.solace.psg.enterprisestats.statspump.semp.GenericSempSaxHandler;
import com.solace.psg.enterprisestats.statspump.semp.SempSaxProcessingListener;
import com.solace.psg.enterprisestats.statspump.semp.SempSchemaValidationException;
import com.solace.psg.enterprisestats.statspump.tools.comms.HttpUtils;
import com.solace.psg.enterprisestats.statspump.tools.comms.SempRequestException;
import com.solace.psg.enterprisestats.statspump.tools.parsers.SaxParser;
import com.solace.psg.enterprisestats.statspump.tools.parsers.SaxParserException;
import com.solace.psg.enterprisestats.statspump.tools.semp.SempReplySchemaLoader;
import com.solace.psg.enterprisestats.statspump.tools.semp.SempRpcSchemaLoader;
import com.solace.psg.enterprisestats.statspump.util.MessageUtils;
import com.solace.psg.enterprisestats.statspump.util.NonXmlBufferedReader;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.XMLContentMessage;

public class DocsGenerator {
    
    static PhysicalAppliance physical;
    static PrintStream psMain;
    static PrintStream psList;
    static String[] colors = new String[] {"#444444","#773322","#339933","#992222","#aa8811","#991199","#999999"};
    
    static String escapeXml(String xml) {
        return xml.replaceAll("<","&lt;").replaceAll(">","&gt;");
    }
    
    static class Blah implements SempSaxProcessingListener {
        
        boolean seenOne = false;
        final GenericPoller poller;
        
        public Blah(Poller poller) {
            this.poller = (GenericPoller)poller;
        }

        @Override
        public void onStartSempReply(String sempVersion, long timestamp) { }

        protected List<String> buildPartialTopicLevels(Map<String,String> objectValuesMap) {
            List<String> list = new ArrayList<String>();
            for (int i=0;i<poller.getTopicLevels().length;i++) {
                if (poller.getTopicLevelReplacements()[i]) list.add(objectValuesMap.get(poller.getTopicLevels()[i]));
                else list.add(poller.getTopicLevels()[i]);
            }
            return list;
        }
        
        static String pollerRow(String label,String content) {
            return String.format("<tr><td class=\"bold\"><nobr>%s</nobr></td><td class=\"text\">%s</td></tr>",label,content);
        }
        
        @Override
        public void onStatMessage(SingleContainerSet containerSet, Map<String, String> objectValuesMap) {
            if (seenOne) return;
            List<String> topicList = buildPartialTopicLevels(objectValuesMap);
            StatsPumpMessage msg = null;
            try {
                switch (poller.getScope()) {
                    case SYSTEM:
                        msg = new StatsPumpMessage(MessageType.SYSTEM,physical,poller.getDestination(),containerSet,topicList,System.currentTimeMillis());
                        break;
                    case VPN:
                        msg = new StatsPumpMessage(MessageType.VPN,physical,objectValuesMap.get(GenericPoller.VPN_NAME),poller.getDestination(),containerSet,topicList,System.currentTimeMillis());
                        break;
                    default:
                }
                psList.printf("<nobr><a href=\"main.html#%s\"  target=\"mainFrame\">%s</a></nobr><br>%n",poller.getName(),poller.getName());
                psMain.printf("<a name=\"%s\">%n<!--   -->%n</a>%n",poller.getName());
                psMain.printf("<h1>Poller '%s'</h1>%n%n",poller.getName());
                psMain.printf("<p>%s</p>%n",poller.getDescription());
                psMain.println("<table>");
                psMain.println(pollerRow("Name",poller.getName()));
                StringBuilder sb = new StringBuilder();
                for (int i=0;i<poller.getTopicLevelsForBroadcast().size();) {
                    String level = poller.getTopicLevelsForBroadcast().get(i);
                    if (level.startsWith("~")) {
                        sb.append(String.format("<span style=\"color:"+colors[i]+";font-weight:bold\">%s</span>",level));
                    } else {
                        sb.append(level);
                    }
                    if (++i < poller.getTopicLevelsForBroadcast().size()) sb.append("/");
                }
                psMain.println(pollerRow("Topic Description",sb.toString()));
                psMain.println(pollerRow("Scope",poller.getScope().name()));
                psMain.println(pollerRow("Min SEMP version",poller.getMinSempVersion().toString()));
                psMain.println(pollerRow("Max SEMP version",poller.getMaxSempVersion() == null ? "undefined" : poller.getMaxSempVersion().toString()));
                psMain.println(pollerRow("SEMP command",escapeXml(poller.getSempRequestCompressed())));
                psMain.println(pollerRow("SEMP base tag in reply",poller.getBaseTag()));
                psMain.println(pollerRow("Broadcast destination",poller.getDestination().toString()+" message buses"));
                psMain.println("</table>");
                psMain.printf("<h2>Example</h2>%n%n");
                psMain.println("<p><b>Topic:</b> <code>");
                for (int i=0;i<poller.getTopicLevelsForBroadcast().size();) {
                    String level = poller.getTopicLevelsForBroadcast().get(i);
                    String actual = msg.getTopicLevels().get(i);
                    assert poller.getTopicLevelsForBroadcast().size() == msg.getTopicLevels().size();
                    if (level.startsWith("~")) {
                        psMain.printf("<span style=\"color:"+colors[i]+";font-weight:bold\">%s</span>",actual);
                    } else {
                        psMain.print(actual);
                    }
                    if (++i < poller.getTopicLevelsForBroadcast().size()) psMain.print("/");
                }
                psMain.println("</code></p>");
                psMain.println("<ul>");
                for (ContainerFactory con : containerSet.keySet()) {
                    BytesXMLMessage jcsmpMessage = msg.buildJcsmpMessage(con);
                    int length;
                    if (jcsmpMessage instanceof XMLContentMessage) {
                        length = ((XMLContentMessage)jcsmpMessage).getXMLContent().length();
                    } else {
                        length = jcsmpMessage.getAttachmentContentLength();
                    }
                    psMain.printf("<li><a href=\"#%s+%s\">%s</a>: %d byte payload</li>%n",poller.getName(),con.getClass().getSimpleName(),con.getClass().getSimpleName(),length);
                }
                psMain.println("</ul>");
                for (ContainerFactory con : containerSet.keySet()) {
                    BytesXMLMessage jcsmpMessage = msg.buildJcsmpMessage(con);
                    psMain.printf("<a name=\"%s+%s\">%n<!--   -->%n</a>%n",poller.getName(),con.getClass().getSimpleName());
                    psMain.printf("<h3>%s Message for '%s'</h3>%n%n",con.getClass().getSimpleName(),poller.getName());
                    psMain.println("<table class=\"spacey\">");
                    psMain.println(pollerRow("JCSMP Message Type",jcsmpMessage.getClass().getSimpleName().replaceAll("Impl","")));
                    String properties = escapeXml(MessageUtils.prettyPrintUserProperies(jcsmpMessage));
                    psMain.println(pollerRow("User Properties Map","<pre>"+properties+"</pre>"));
                    String payload = escapeXml(MessageUtils.prettyPrintPayload(jcsmpMessage));
                    // SDTMap: find the end of they key and grab the next work and wrap in a value tag
                    payload = payload.replaceAll("\\): (.+?)((?:\\n|\\z))","\\): <span class=\"value\">$1</span>$2");  // sdtmap value bold
                    payload = payload.replaceAll("Key '(.+?)' ","Key '<span class=\"key\">$1</span>' ");  // sdtmap key green bold
                    payload = payload.replaceAll(" \\((.*?)\\): "," <i>\\($1\\):</i> ");       // sdtmap datatype italic
                    payload = payload.replaceAll("&gt;(.*?)&lt;/","&gt;<span class=\"value\">$1</span>&lt;/");       // xml value bold
                    payload = payload.replaceAll("&lt;(/?)([^ ].*?)((?: .+?)?)&gt;","&lt;$1<span class=\"key\">$2</span>$3&gt;");       // xml key bold
                    payload = payload.replaceAll("\": ([^\\{\\[]+?)((?:,\\n|\\n|\\z|\\},\\n|\\}\\n|\\}\\z))","\": <span class=\"value\">$1</span>$2");     // json values bold - close quote, colon, space, value, possible trailing comma
                    payload = payload.replaceAll("([ \\{\\[])\"(.+?)\": ","$1\"<span class=\"key\">$2</span>\": ");     // json key bold (could have a space, { or [ before the key start ")
                    psMain.println(pollerRow("Payload (Pretty Print)","<pre>"+payload+"</pre>"));
                    psMain.println("</table><br>");
                }
                seenOne = true;
            } catch (JCSMPException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onEndSempReply() { }

    }

    
    
    private static void doIt(Poller poller) {

        String sempRequest = String.format(poller.getSempRequest(),physical.getSempVersion());
        GenericSempSaxHandler handler = poller.buildSaxHandler(physical,new Blah(poller));
        BufferedReader reader = null;
        while (sempRequest != null) {
            try {
                handler.setMessageTimestamp(System.currentTimeMillis());
                reader = new NonXmlBufferedReader(new InputStreamReader(new BufferedInputStream(HttpUtils.performSempStreamRequest(
                        physical.getSempHostnameOrIp(),physical.getSempUsername(),physical.getSempPassword(),sempRequest))),null);
                SaxParser.parseReader(reader,handler);
                sempRequest = handler.getMoreCookie();
                //                        System.err.println(sempRequest);
            } catch (SempRequestException e) {
                e.printStackTrace();
            } catch (SempSchemaValidationException e) {
                e.printStackTrace();
            } catch (SaxParserException e) {
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException ignore) { }
                }
            }
        }        
    }

    
    public static void main(String... args) throws ConfigLoaderException, InterruptedException, FileNotFoundException {
        
        
        SempReplySchemaLoader.loadSchemas();
        SempRpcSchemaLoader.INSTANCE.loadSchemas();
        LocalFileConfigStreamsImpl streams = new LocalFileConfigStreamsImpl(args[0],args[1],args[2]);
        ConfigLoader.loadConfig(streams);
        
        physical = ConfigLoader.appliancesList.get(0).getPrimary();
        physical.setSempVersion("soltr/7_1_1");
        physical.setHostname("emea1");
        physical.setAdActivity("AD-Active");
        physical.setPrimaryLocalActivity("Local Active");
        physical.setBackupLocalActivity("Mate Active");
        HostnamePoller.getInstance();
        RedundancyPoller.getInstance();
        MessageSpoolPoller.getInstance();
        VpnDetailPoller.getInstance();
        Thread.sleep(1000);
        psMain = new PrintStream(new File("main.html"));
        psMain.println("<html><head><title>Poller Output</title><LINK REL =\"stylesheet\" TYPE=\"text/css\" HREF=\"stylesheet.css\" TITLE=\"Style\"></head><body>");
        psList = new PrintStream(new File("list.html"));
        psList.println("<html><head><title>Poller Index</title><LINK REL =\"stylesheet\" TYPE=\"text/css\" HREF=\"stylesheet.css\" TITLE=\"Style\"></head><body>");
        psList.println("<h2>StatsPump PollerDocs</h2>");
        
        doIt(HostnamePoller.getInstance());
        doIt(RedundancyPoller.getInstance());
        doIt(MessageSpoolPoller.getInstance());
        doIt(VpnDetailPoller.getInstance());
        for (Poller poller : physical.getLogical().getPollers()) {
            doIt(poller);
            System.out.print(".");
        }
        psMain.println("</body></html>");
        psMain.close();
        psList.println("<hr><p><span style=\"font-size:0.65em\">&copy;2014-2016 Solace Systems Professional Servcices Group<br>solacesystems.com</span></p></body></html>");
        psList.close();
        System.out.println("DONE!");
    }
    
    
}
