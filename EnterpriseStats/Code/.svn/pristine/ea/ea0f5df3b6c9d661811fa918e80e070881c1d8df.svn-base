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

package com.solace.psg.enterprisestats.statspump.profiler;

import java.io.InputStream;
import java.io.Reader;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.solace.psg.enterprisestats.statspump.tools.parsers.SaxParser;
import com.solace.psg.enterprisestats.statspump.tools.parsers.SaxParserException;

public class SempRequests {
    
    static class NewHandler extends DefaultHandler {

        StringBuffer sb = new StringBuffer();
        
        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            sb.append(String.valueOf(ch,start,length));
        }

        @Override
        public void startElement(String arg0, String arg1, String arg2, Attributes arg3) throws SAXException {
            sb.setLength(0);
        }

        @Override
        public void endElement(String arg0, String arg1, String arg2) throws SAXException {
            StringBuilder b = new StringBuilder();
            for (char c : sb.toString().toCharArray()) {
                b.append(String.format("'%c':0x%x, ",c,(int)c));
            }
            System.out.printf("Closing '%s': %s - %s%n",arg1,sb.toString(),b.toString());
        }
    }

    static boolean testXmlString(String xml) {
        try {
            SaxParser.parseString(xml,new DefaultHandler());
            return true;
        } catch (SaxParserException e) {
            System.err.println(e.toString());
            return false;
        }
    }
    
    static boolean testXmlStream(InputStream xml) {
        try {
            SaxParser.parseStream(xml,new DefaultHandler());
            return true;
        } catch (SaxParserException e) {
            System.err.println(e.toString());
            return false;
        }
    }
    
    static boolean testXmlReader(Reader xml) {
        try {
            SaxParser.parseReader(xml,new NewHandler());
            return true;
        } catch (SaxParserException e) {
            System.err.println(e.toString());
            return false;
        }
    }

    // The code below this point can be useful as a informal unit test
    
//    public static void main(String... args) throws SempRequestException, InterruptedException, ConfigLoaderException, IOException {
//        
////        SempRequestHttp conn = new SempRequestHttp("151.237.234.149:8080","aaron_ro","aaron_ro");
////        SempRequestHttp conn = new SempRequestHttp("192.168.2.30:8080","admin","admin");
//        String a;
////      a = "<rpc semp-version='soltr/6_2'><show><jndi><object><name>*</name><vpn-name>HSBCPoC</vpn-name></object></jndi></show></rpc>";
////      a = "<rpc semp-version='soltr/6_2'><show><ip><vrf><name>management</name><route></route></vrf></ip></show></rpc>";
////      a = "<rpc semp-version='soltr/6_2'><show><ip><vrf></vrf></ip></show></rpc>";
////      a = "<rpc semp-version='soltr/6_2'><show><acl-profile><name>aaron</name><detail></detail></acl-profile></show></rpc>";
//      a = "<rpc semp-version='soltr/6_2'><show><client><name>*</name><vpn-name>*</vpn-name>   <sorted-stats></sorted-stats>            <stats-to-show>total-client-messages-received</stats-to-show></client></show></rpc>";
////        a = "<rpc semp-version='soltr/6_2'><show><client><name>aaron*</name><vpn-name>*</vpn-name></client></show></rpc>";
////      a = "<rpc semp-version='soltr/6_2'><show><message-vpn><vpn-name>a*</vpn-name><stats/></message-vpn></show></rpc>";
//      a = "<rpc semp-version='soltr/6_2'><show><message-spool><detail/></message-spool></show></rpc>";
////      a = "<rpc semp-version='soltr/6_0'><show><message-spool><vpn-name>*</vpn-name><detail/></message-spool></show></rpc>";
////      a = "<rpc semp-version='soltr/7_0'><show><message-spool><vpn-name>*</vpn-name><rates/><rates-count>1000</rates-count></message-spool></show></rpc>";
////     a = "<rpc semp-version='soltr/6_1'><show><queue><name>*</name><vpn-name>*</vpn-name><subscriptions/><count/><num-elements>500</num-elements></queue></show></rpc>";
////     a = "<rpc semp-version='soltr/7_0'><show><queue><name>*</name><vpn-name>*</vpn-name><rates/><count/><num-elements>50</num-elements></queue></show></rpc>";
////       a = "<rpc semp-version='soltr/6_2'><show><message-spool><rates/><rates-count>10</rates-count></message-spool></show></rpc>";
//        a = "<rpc semp-version='soltr/7_1_1'><show><client><name>*</name><vpn-name>*</vpn-name><stats/><queues/><count/><num-elements>40</num-elements><primary/></client></show></rpc>";
////      a = "<rpc semp-version='soltr/6_2'><show><redundancy><detail/></redundancy></show></rpc>";
////    a = "<rpc semp-version='soltr/7_1'><show><message-vpn><vpn-name>*</vpn-name><detail/><count/><num-elements>10</num-elements></message-vpn></show></rpc>";
////      a = "<rpc semp-version='soltr/6_2'><show><stats><client><detail/></client></stats></show></rpc>";
////      a = "<rpc semp-version='soltr/6_0'><show><client><name>*</name><vpn-name>asdf</vpn-name><sorted-stats/><stats-to-show>total-egress-discards</stats-to-show><primary/></client></show></rpc>";
////      a = "<rpc semp-version='soltr/7_1'><show><redundancy><detail/></redundancy></show></rpc>";
////      a = "<rpc semp-version='soltr/6_0'><show><cache-instance><name>*</name><vpn-name>*</vpn-name><detail/></cache-instance></show></rpc>";
////      a = "<rpc semp-version='soltr/6_0'><show><distributed-cache><name>*</name><vpn-name>*</vpn-name><detail/></distributed-cache></show></rpc>";
////      a = "<rpc semp-version='soltr/6_0'><show><cache-cluster><name>*</name><vpn-name>*</vpn-name><topics/></cache-cluster></show></rpc>";
////      a = "<rpc semp-version='soltr/6_0'><show><cache-instance><name>*</name><vpn-name>*</vpn-name><remote/><status/></cache-instance></show></rpc>";
////      a = "<rpc semp-version='soltr/6_0'><show><bridge><bridge-name-pattern>*</bridge-name-pattern><subscriptions/></bridge></show></rpc>";
////      a = "<rpc semp-version='soltr/6_0'><show><service/></show></rpc>";
////      a = "<rpc semp-version='soltr/6_0'><show><replicated-topic><topic>*</topic></replicated-topic></show></rpc>";
////   a = "<rpc semp-version='soltr/6_2'><show><client><name>*</name><vpn-name>*</vpn-name><subscriptions/><count/><num-elements>40</num-elements></client></show></rpc>";
////      a = "<rpc semp-version='soltr/7_1_1'><show><client><name>*</name><vpn-name>*</vpn-name><connections/><count/><num-elements>10</num-elements></client></show></rpc>";
////      a = "<rpc semp-version='soltr/6_2'><message-spool><vpn-name>aaron_queue_test</vpn-name><queue><name>q0010</name><shutdown><full/></shutdown></queue></message-spool></rpc>";
////        a = "<rpc semp-version='soltr/6_0'><show><smrp><subscriptions><vpn-name>*</vpn-name><primary/><topic></topic><topic-str>PB*</topic-str></subscriptions></smrp></show></rpc>";
////        a = "<rpc semp-version='soltr/6_0'><show><client><name>*</name><vpn-name>*</vpn-name><count/><num-elements>3</num-elements><detail/></client></show></rpc>";
////        a = "<rpc semp-version='soltr/6_0'><show><queue><name>qqq*</name><vpn-name>aaron</vpn-name><count/><num-elements>100</num-elements></queue></show></rpc>";
////        a = "<rpc semp-version='soltr/7_0'><show><config-sync/></show></rpc>";
////    a = "<rpc semp-version='soltr/7_0'><show><version></version></show></rpc>";
////      a = "<rpc semp-version='soltr/7_0'><show><config-sync><database/></config-sync></show></rpc>";
////      a = "<rpc semp-version='soltr/7_0'><show><interface/></show></rpc>";
////      a = "<rpc semp-version='soltr/7_0'><show><ip><vrf/></ip></show></rpc>";
////      a = "<rpc semp-version='soltr/7_0'><show><ip><vrf><name>msg-backbone</name></vrf></ip></show></rpc>";
////    a = "<rpc semp-version='soltr/7_1_1'><show><client><name>*</name><vpn-name>*</vpn-name><connections/><wide/><count/><num-elements>10</num-elements></client></show></rpc>";
////        a = "<rpc semp-version='soltr/7_1'><show><interface><phy-interface>1/1/1</phy-interface><detail/></interface></show></rpc>";
////      a = "<rpc semp-version='soltr/6_2'><show><smrp><database><detail/></database></smrp></show></rpc>";
////      a = "<rpc semp-version='soltr/6_2'><show><cspf><stats/></cspf></show></rpc>";
////      a = "<rpc semp-version='soltr/6_2'><show><cspf><neighbor><physical-router-name>*</physical-router-name></neighbor></cspf></show></rpc>";
////        a = "<rpc semp-version='soltr/7_0'><show><message-spool><vpn-name>aaron*</vpn-name><rates/></message-spool></show></rpc>";
//      a = "<rpc semp-version='soltr/6_2'><show><cspf><neighbor><physical-router-name>*</physical-router-name><stats/><queues/></neighbor></cspf></show></rpc>";
////      a = "<rpc semp-version='soltr/6_2'><show><stats><neighbor><detail/></neighbor></stats></show></rpc>";
////      a = "<rpc semp-version='soltr/6_2'><show><log><no-subscription-match><wide/></no-subscription-match></log></show></rpc>";
////      a = "<rpc semp-version='soltr/6_2'><show><acl-profile><name>*</name><vpn-name>*</vpn-name><detail/><count/><num-elements>10</num-elements></acl-profile></show></rpc>";
//        
////      SempReplySchemaParser.loadSchemas();
////      ConfigLoader.loadConfig("poller_deployed.xml","appliance_config_emea_local.xml");
////      System.out.println(ConfigLoader.pollersMap.keySet());
//
////      a = ConfigLoader.pollersMap.get("Appliance Redundancy").getSempRequest();
////      System.out.println(a);
//
////      a = ConfigLoader.pollersMap.get("VPN Primary Client Detail w/ Egress Queues").getSempRequestCompressed();
//
//      String resp = HttpUtils.performSempStringRequest("151.237.234.149:8050","aaron_ro","aaron_ro",a);
////      String resp = HttpUtils.performSempStringRequest("192.168.2.30:8080","admin","admin",a);
//      System.out.println();
//      System.out.println();
//      System.out.println("Length: "+resp.length());
//      System.out.println(resp);
//      System.exit(0);
//      for (int i=0;i<resp.length();i++) {
//          if (resp.charAt(i) == ' ') continue;
//          System.out.printf("'%c'|0x%x, ",resp.charAt(i),(int)resp.charAt(i));
//      }
//      System.out.println(testXmlString(resp));
//      System.out.println("------------------------");
//      System.out.println(testXmlStream(new DumpInputStream(HttpUtils.performSempStreamRequest("151.237.234.149:8050","aaron_ro","aaron_ro",a))));
//
//      System.exit(0);
//      
//      CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
//      decoder.onMalformedInput(CodingErrorAction.REPLACE);
//      decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
//      
//      System.out.println(decoder.getClass().getName());
//      for (Method f : decoder.getClass().getDeclaredMethods()) {
//          System.out.println(f);
//          
//      }
//      
//      
//      System.exit(0);
//      System.out.println(testXmlReader(new NonXmlBufferedReader(new InputStreamReader(new BufferedInputStream(
//              HttpUtils.performSempStreamRequest("151.237.234.149:8050","aaron_ro","aaron_ro",a)),Charset.forName("UTF-8")),null)));
//
//      
//
//
//      
//      
//      System.exit(-1);
//
////      Charset charset = Charset.forName("UTF-8");
////
////      String testString = "<test><xml><what>123456</what><blah/></xml>\n</test>";
////      testString = "<client>\n<client-address>30.208.100.203:4104</client-address>\n<name>XXXX/_ld\nndwm424956_8096intranet_syscommodexoticarc_riskviewer__mepdn_1402934252</name>\n<type>Primary</type>\n<profile>me_glbl_prd</profile>\n<acl-profile>default</acl-profile>\n<num-subscriptions>7</num-subscriptions>\n</client>";
////      //System.out.println(Utf8ReplaceInputStream.isValidUtf8(testString.getBytes(charset)));
////      System.out.printf("Original: '%s' (with %s charset)%n",testString,charset.name());
////      System.out.println();
////      byte[] testStringBytes = testString.getBytes(charset);
//      //testStringBytes[7] = 0x04;
//      //System.out.println("Modified 8th char to be 0x04");
//      //System.out.println("is valid utf8? "+Utf8ReplaceInputStream.isValidUtf8(testStringBytes));
////      testStringBytes[18] = (byte)0x01;
////      testStringBytes[19] = (byte)0x01;
////      testStringBytes[20] = (byte)0x01;
////      System.out.println("have now replaced byes 19-21 with 0x01");
////Bytes: 0xF4 0xE5 0xE2 0x20  --> 1111 0100  1110 0101  1110 0010    -- definitely note UTF-8
////      testStringBytes[18] = (byte)0xF4;
////      testStringBytes[19] = (byte)0xE5;
////      testStringBytes[20] = (byte)0xE2;
////      System.out.println("have now replaced byes 19-21 with junk");
////      testStringBytes[18] = (byte)0xE0;
////      testStringBytes[19] = (byte)0x37;
////      testStringBytes[20] = (byte)0x4D;
////      testStringBytes[21] = (byte)0x0a;
////      System.out.println("have now replaced byes 19-22 with junk");
////      testStringBytes[68] = (byte)0xE0;
////      testStringBytes[69] = (byte)0xf3;
////      testStringBytes[70] = (byte)0xD1;
////      testStringBytes[71] = (byte)0xF7;
///*      testStringBytes[68] = (byte)0xE0;
//      testStringBytes[69] = (byte)0x37;
//      testStringBytes[70] = (byte)0x4D;
//      testStringBytes[71] = (byte)0x0a;
//      System.out.println("have now replaced byes 69-72 with junk");
//      System.out.println(new String(testStringBytes));
//      System.out.println();
//      
//
//      
//      CharsetDecoder decoder = charset.newDecoder();
//      decoder.onMalformedInput(CodingErrorAction.REPLACE);
//      decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
//      //decoder.replaceWith("\u251a");
//      
//      Reader isr = new NonXmlBufferedReader(new InputStreamReader(new BufferedInputStream(new ByteArrayInputStream(testStringBytes)),decoder));
////      InputStreamReader isr = new InputStreamReader(SempRequestHttpStream.performStreamRequest("192.168.30.50",a,"aaron_ro","aaron_ro"));
//      DefaultHandler dh2 = new NewHandler();
//      try {
//          SaxParser.parseReader(isr,dh2);
//          System.out.println("all good");
//        } catch (SaxParserException e) {
//            e.printStackTrace();
//        }
//      System.exit(0);
//      
//      
//      InputStream is = SempRequestHttpStream.performStreamRequest("192.168.30.50",a,"aaron_ro","aaron_ro");
//      DefaultHandler dh = new DefaultHandler();
//      try {
//          SaxParser.parseStream(is,dh);
//          System.out.println("all good");
//        } catch (SaxParserException e) {
//            e.printStackTrace();
//        }
//      System.exit(0);
//      int bytes = 0;
//      byte[] data = new byte[10000];
//      while ((bytes = is.read(data)) != -1) {
//          System.out.printf("'%s' (%d bytes read): ",new String(data,StandardCharsets.UTF_8),bytes);
//          for (int i=0;i<bytes;i++) {
//              System.out.printf("%c(%d),",(char)data[i],data[i]);
//          }
//          System.out.println();
//          System.out.println();
//          Thread.sleep(200);
//      }
//      is.close();
//*/
//        
//        
//        
//        
////        String resp = "asdf";
////      System.out.println(resp);
//        System.out.println(resp.substring(0, Math.min(10000,resp.length())));
////      //System.out.println(resp.substring(Math.max(0,resp.length()-10000)));
////        System.out.println("Total size:     "+resp.length());
////
////        final int LOOPS = 50;
////        long total = 0;
////        for (int i=0;i<LOOPS;i++) {
////            long start = System.nanoTime();
////            conn.performSempRequest(a);
////            total += System.nanoTime() - start;
////            Thread.sleep(500);
////        }
////        System.out.println("Average millis: "+((total/1000000)/(float)LOOPS));
// 
//        /*
//        187 VPNs, 5500 queues
//        
//        show message-vpn * stats --> 550ms
//        show message-vpn * --> 44ms
//        show stats client detail --> 12ms
//        show redundancy detail --> 3.5ms
//        show message-spool --> 2.2ms
//        show message-spool detail --> 568ms !!   with new computer, around 210ms
//        show message-spool stats --> 7ms
//        show message-spool message-vpn * --> 18ms
//        show message-spool message-vpn * detail --> 75ms
//        show queue * (1000 queues) --> 257ms
//        show queue * detail (1000) --> 990ms
//        
//        
//        */
//        
//    }
}
