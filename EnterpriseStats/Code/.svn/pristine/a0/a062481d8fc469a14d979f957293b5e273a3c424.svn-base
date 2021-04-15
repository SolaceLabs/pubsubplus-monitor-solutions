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
import com.solace.psg.enterprisestats.statspump.containers.GroupedContainer;
import com.solace.psg.enterprisestats.statspump.containers.SingleContainer;
import com.solace.psg.enterprisestats.statspump.containers.factories.SdtMapFactory.GroupedSdtMapContainer;
import com.solace.psg.enterprisestats.statspump.containers.factories.SdtMapFactory.SingleSdtMapContainer;
import com.solace.psg.enterprisestats.statspump.containers.factories.SempXmlFactory.GroupedSempXmlContainer;
import com.solace.psg.enterprisestats.statspump.containers.factories.SempXmlFactory.SingleSempXmlContainer;
import com.solace.psg.enterprisestats.statspump.tools.semp.XsdDataType;
import com.solace.psg.enterprisestats.statspump.util.SingleSolaceMessageFactory;
import com.solacesystems.jcsmp.MapMessage;
import com.solacesystems.jcsmp.Message;
import com.solacesystems.jcsmp.SDTException;

/**
 * 
xs:unsignedLong   long
xs:long           long
xs:unsignedInt    long
xs:int            int
xs:unsignedShort  int
xs:unsignedByte   short
xs:boolean        boolean
xs:float          float
xs:double         double
xs:time           Calendar
xs:dateTime       Calendar
xs:integer        string
xs:string         string
 * 
 */



public class SDTMapAndXmlFactory implements ContainerFactory {
    
    private static final SempXmlFragmentFactory xmlTextFactory = SempXmlFragmentFactory.INSTANCE;
    private static final SdtMapFactory sdtMapFactory = SdtMapFactory.INSTANCE;

    @Override
    public SingleContainer newSingleContainer() {
        return new SingleSDTMapAndXmlContainer();
    }

    @Override
    public SingleContainer newSingleContainer(String sempVersion, String baseTag) {
        return new SingleSDTMapAndXmlContainer(sempVersion,baseTag);
    }

    @Override
    public GroupedContainer newGroupedContainer(String sempVersion, String baseTag) {
        return new GroupedSDTMapAndXmlContainer(sempVersion,baseTag);
    }
    
    public static class SingleSDTMapAndXmlContainer implements SingleContainer {
    
        protected SingleSempXmlContainer xml;
        protected SingleSdtMapContainer sdtMap;
        
        public SingleSDTMapAndXmlContainer() {
            xml = xmlTextFactory.newSingleContainer();
            sdtMap = sdtMapFactory.newSingleContainer();
        }
    
        public SingleSDTMapAndXmlContainer(String sempVersion, String baseTag) {
            xml = xmlTextFactory.newSingleContainer(sempVersion,baseTag);
            sdtMap = sdtMapFactory.newSingleContainer(sempVersion,baseTag);
        }
    
        @Override
        public void startNestedElement(String element, boolean isMultiple) {
            xml.startNestedElement(element, isMultiple);
            sdtMap.startNestedElement(element, isMultiple);
        }
    
        @Override
        public void closeNestedElement(String element) {
            xml.closeNestedElement(element);
            sdtMap.closeNestedElement(element);
        }
        
        @Override
        public void put(XsdDataType type, String element, Object value, boolean isMultiple) {
            xml.put(type, element, value, isMultiple);
            sdtMap.put(type, element, value, isMultiple);
        }
    
        @Override
        public void putString(XsdDataType type, String element, String value, boolean isMultiple) {
            xml.putString(type, element, value, isMultiple);
            sdtMap.putString(type, element, value, isMultiple);
        }
    
        @Override
        public Message buildJcsmpMessage(StatsPumpMessage message) {
            MapMessage msg = SingleSolaceMessageFactory.getMessage(MapMessage.class);
            msg.setMap(sdtMap.getRoot());
            msg.writeBytes(xml.getXmlContent().getBytes());  // writes this into the "XML" portion of the payload
            return msg;
        }
    
        @Override
        public String toString() {
            return String.format("%s: %s / %s",this.getClass().getSimpleName(),sdtMap.nestedMaps.toString(),xml.getXmlContent());
        }
    }
    
    public static class GroupedSDTMapAndXmlContainer implements GroupedContainer {

        protected GroupedSempXmlContainer xmlContainer;
        protected GroupedSdtMapContainer sdtMapContainer;
        
        final String sempVersion;
        final String baseTag;
        
        public GroupedSDTMapAndXmlContainer(String sempVersion, String baseTag) {
            this.sempVersion = sempVersion;
            this.baseTag = baseTag;
            xmlContainer = xmlTextFactory.newGroupedContainer(sempVersion,baseTag);
            sdtMapContainer = sdtMapFactory.newGroupedContainer(sempVersion,baseTag);
        }

        @Override
        public Message buildJcsmpMessage(StatsPumpMessage message) throws SDTException {
            Message msg = sdtMapContainer.buildJcsmpMessage(message);
            msg.writeBytes(xmlContainer.groupedXmlTextContainer.getXmlContent().getBytes());  // writes this into the "XML" portion of the payload
            return msg;
        }

        @Override
        public void addContainer(SingleContainer singleContainer) {
            assert singleContainer instanceof SingleSDTMapAndXmlContainer;
            xmlContainer.addContainer(((SingleSDTMapAndXmlContainer)singleContainer).xml);
            sdtMapContainer.addContainer(((SingleSDTMapAndXmlContainer)singleContainer).sdtMap);
        }

        @Override
        public boolean isEmpty() {
            return sdtMapContainer.isEmpty();
        }
    }
}
