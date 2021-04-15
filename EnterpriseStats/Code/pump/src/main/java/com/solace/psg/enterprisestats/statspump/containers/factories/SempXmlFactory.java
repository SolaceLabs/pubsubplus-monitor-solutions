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

package com.solace.psg.enterprisestats.statspump.containers.factories;

import com.solace.psg.enterprisestats.statspump.StatsPumpMessage;
import com.solace.psg.enterprisestats.statspump.containers.ContainerFactory;
import com.solace.psg.enterprisestats.statspump.containers.ContainerUtils;
import com.solace.psg.enterprisestats.statspump.containers.GroupedContainer;
import com.solace.psg.enterprisestats.statspump.containers.SingleContainer;
import com.solace.psg.enterprisestats.statspump.tools.semp.XsdDataType;
import com.solace.psg.enterprisestats.statspump.util.SingleSolaceMessageFactory;
import com.solace.psg.enterprisestats.statspump.util.StatsPumpConstants;
import com.solacesystems.jcsmp.SDTException;
import com.solacesystems.jcsmp.XMLContentMessage;

public enum SempXmlFactory implements ContainerFactory {
    
    INSTANCE;
    
    @Override
    public SingleSempXmlContainer newSingleContainer(){//String sempVersion) {
        return new SingleSempXmlContainer(true);
    }

    @Override
    public SingleSempXmlContainer newSingleContainer(String sempVersion, String baseTag) {
        return new SingleSempXmlContainer(sempVersion,baseTag,true);
    }

    @Override
    public GroupedSempXmlContainer newGroupedContainer(String sempVersion, String baseTag) {
        return new GroupedSempXmlContainer(sempVersion,baseTag,true);
    }

    
    private static XMLContentMessage buildXmlMessage(String xmlContent) {
        XMLContentMessage msg = SingleSolaceMessageFactory.getMessage(XMLContentMessage.class);
        msg.setXMLContent(xmlContent);
        return msg;
    }
    
    static class SingleSempXmlContainer implements SingleContainer {

        //private final String sempVersion;
        private final String baseElement;
        private final boolean includePrePostAmble;
        private final String preAmble;
        private final String postAmble;
        private final StringBuilder xmlFragmentContentSb = new StringBuilder(512);
        //private final ArrayDeque<String> nestedLevels = new ArrayDeque<String>();  // an ordered stack of nested keys / levels
        
        SingleSempXmlContainer(String sempVersion, String baseTag, boolean includePrePostAmble) {
            //this.sempVersion = sempVersion;
            this.includePrePostAmble = includePrePostAmble;
            if (this.includePrePostAmble) {
                baseElement = "";
                final StringBuilder preSb = new StringBuilder(64);
                final StringBuilder postSb = new StringBuilder(64);
                String[] baseTagLevels = baseTag.split("/");
                for (int i=1;i<baseTagLevels.length;i++) {  // from i=1 b/c first string will be empty due to leading '/'
                    preSb.append('<').append(baseTagLevels[i]).append('>');
                    postSb.insert(0,'>').insert(0,baseTagLevels[i]).insert(0,"</");
                }
                preAmble = preSb.toString().replaceFirst("<rpc-reply>",String.format("<rpc-reply semp-version=\"%s\">",sempVersion));
                postAmble = postSb.toString().replaceFirst("</rpc></rpc-reply>","</rpc><execute-result code=\"ok\"/></rpc-reply>");
            } else {
                baseElement = baseTag.substring(baseTag.lastIndexOf('/')+1);  // if single level (no /) then will return -1, so +1 == 0 so full string
                preAmble = "";
                postAmble = "";
            }
        }
        
        public SingleSempXmlContainer(boolean includePrePostAmble) {
            this("soltr/0_0",StatsPumpConstants.BASETAG_DEFAULT,includePrePostAmble);
        }
        
        @Override
        public XMLContentMessage buildJcsmpMessage(StatsPumpMessage message) throws SDTException {
            XMLContentMessage msg = SempXmlFactory.buildXmlMessage(getXmlContent());
            ContainerUtils.prepMessage(msg,message);
            ContainerUtils.addXmlTopicLevels(msg,message);
            return msg;
        }
        
        String getXmlContent() {
            if (includePrePostAmble) {
                return new StringBuilder(preAmble)
                        .append(xmlFragmentContentSb)
                        .append(postAmble).toString();
            } else {
                return new StringBuilder("<").append(baseElement).append('>')
                        .append(xmlFragmentContentSb)
                        .append("</").append(baseElement).append('>').toString();
            }
        }
    
        @Override
        public void startNestedElement(String element, boolean isMultiple) {
            //nestedLevels.addLast(element);
            xmlFragmentContentSb.append('<').append(element).append('>');
        }
    
        @Override
        public void closeNestedElement(String element) {
            xmlFragmentContentSb.append("</").append(element).append(">");
        }
    
        @Override
        public void put(XsdDataType type, String element, Object value, boolean isMultiple) {
            xmlFragmentContentSb.append('<').append(element).append('>');
            xmlFragmentContentSb.append(value.toString());
            xmlFragmentContentSb.append("</").append(element).append('>');
        }
        
        @Override
        public void putString(XsdDataType type, String element, String value, boolean isMultiple) {
            xmlFragmentContentSb.append('<').append(element).append('>');
            xmlFragmentContentSb.append(value);
            xmlFragmentContentSb.append("</").append(element).append('>');
        }
        
        @Override
        public String toString() {
            return String.format("%s: %s",this.getClass().getSimpleName(),xmlFragmentContentSb.insert(0,preAmble).append(postAmble).toString());
        }
    }
    
    static class GroupedSempXmlContainer implements GroupedContainer {

        // Sneaky, just using a single xml container here to hold all the grouped XML, as all the pre/post amble stuff the same
        final SingleSempXmlContainer groupedXmlTextContainer;  // ... just need to trim baseTag
        private final String preBaseElement;
        private final String postBaseElement;

        GroupedSempXmlContainer(final String sempVersion, final String baseTag, boolean includeFullSempXml) {
            String baseElement = baseTag.substring(baseTag.lastIndexOf('/')+1);
            preBaseElement = new StringBuilder("<").append(baseElement).append('>').toString();
            postBaseElement = new StringBuilder("</").append(baseElement).append('>').toString();
            // so now, since we'll wrap each SingleContainer with the last tag, pass all the rest of the tags to the whole Single
            groupedXmlTextContainer = new SingleSempXmlContainer(sempVersion,baseTag.substring(0,baseTag.lastIndexOf('/')),includeFullSempXml);
        }
        
        @Override
        public XMLContentMessage buildJcsmpMessage(StatsPumpMessage message) throws SDTException {
            XMLContentMessage msg = groupedXmlTextContainer.buildJcsmpMessage(message);
            ContainerUtils.prepMessage(msg,message);
            ContainerUtils.addJsonTopicLevels(msg,message);
            return msg;
        }

        @Override
        public void addContainer(SingleContainer singleContainer) {
            assert singleContainer instanceof SingleSempXmlContainer;
            // wrap the raw XML in the basetags <stats> and </stats>
            // this ensures that we don't lump in the <rpc-reply><rpc><etc.> tags in with each individual single, just grabs the Fragment string directly
            groupedXmlTextContainer.xmlFragmentContentSb.append(preBaseElement).append(((SingleSempXmlContainer)singleContainer).xmlFragmentContentSb).append(postBaseElement);
        }
        
        @Override 
        public boolean isEmpty() {
            return groupedXmlTextContainer.xmlFragmentContentSb.length() == 0;
        }
    }
}
