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
import com.solace.psg.enterprisestats.statspump.containers.factories.JsonMapFactory.GroupedJsonMapContainer;
import com.solace.psg.enterprisestats.statspump.containers.factories.JsonMapFactory.SingleJsonMapContainer;
import com.solace.psg.enterprisestats.statspump.containers.factories.SdtMapFactory.GroupedSdtMapContainer;
import com.solace.psg.enterprisestats.statspump.containers.factories.SdtMapFactory.SingleSdtMapContainer;
import com.solace.psg.enterprisestats.statspump.tools.semp.XsdDataType;
import com.solacesystems.jcsmp.Message;
import com.solacesystems.jcsmp.SDTException;

public class SDTMapAndJsonFactory implements ContainerFactory {
    
    private static SdtMapFactory nestedSDTMapFactory = SdtMapFactory.INSTANCE;
    private static JsonMapFactory jsonMapFactory = JsonMapFactory.INSTANCE;

    @Override
    public SingleContainer newSingleContainer() {
        return new SingleSDTMapAndJsonContainer();
    }

    @Override
    public SingleContainer newSingleContainer(String sempVersion_ignore, String baseTag_ignore) {
        return new SingleSDTMapAndJsonContainer();
    }

    @Override
    public GroupedContainer newGroupedContainer(String sempVersion_ignore, String baseTag) {
        return new GroupedSDTMapAndJsonContainer(baseTag);
    }
    
    public static class SingleSDTMapAndJsonContainer implements SingleContainer {
    
        protected SingleJsonMapContainer json;
        protected SingleSdtMapContainer sdtMap;
        
        public SingleSDTMapAndJsonContainer() {
            json = jsonMapFactory.newSingleContainer();
            sdtMap = nestedSDTMapFactory.newSingleContainer();
        }
    
        public SingleSDTMapAndJsonContainer(String sempVersion, String baseTag) {
            json = jsonMapFactory.newSingleContainer(sempVersion,baseTag);
            sdtMap = nestedSDTMapFactory.newSingleContainer(sempVersion,baseTag);
        }
    
        @Override
        public void startNestedElement(String element, boolean isMultiple) {
            json.startNestedElement(element, isMultiple);
            sdtMap.startNestedElement(element, isMultiple);
        }
    
        @Override
        public void closeNestedElement(String element) {
            json.closeNestedElement(element);
            sdtMap.closeNestedElement(element);
        }
        
        @Override
        public void put(XsdDataType type, String element, Object value, boolean isMultiple) {
            json.put(type, element, value, isMultiple);
            sdtMap.put(type, element, value, isMultiple);
        }
    
        @Override
        public void putString(XsdDataType type, String element, String value, boolean isMultiple) {
            json.putString(type, element, value, isMultiple);
            sdtMap.putString(type, element, value, isMultiple);
        }
    
        @Override
        public Message buildJcsmpMessage(StatsPumpMessage message) throws SDTException {
            Message msg = sdtMap.buildJcsmpMessage(message);
            msg.writeBytes(json.nestedJsonObjects.getFirst().toString().getBytes());  // writes this into the "XML" portion of the payload
            return msg;
        }
    
        @Override
        public String toString() {
            return String.format("%s: %s / %s",this.getClass().getSimpleName(),sdtMap.nestedMaps.toString(),json.nestedJsonObjects.toString());
        }
    }
    
    public static class GroupedSDTMapAndJsonContainer implements GroupedContainer {

        protected GroupedJsonMapContainer jsonContainer;
        protected GroupedSdtMapContainer sdtMapContainer;
        
        final String baseTag;
        
        public GroupedSDTMapAndJsonContainer(String baseTag) {
            this.baseTag = baseTag;
            jsonContainer = jsonMapFactory.newGroupedContainer("",baseTag);         // the last level of the baseTag is used to build the root JSON array object
            sdtMapContainer = nestedSDTMapFactory.newGroupedContainer("",baseTag);
        }

        @Override
        public Message buildJcsmpMessage(StatsPumpMessage message) throws SDTException {
            Message msg = sdtMapContainer.buildJcsmpMessage(message);
            msg.writeBytes(jsonContainer.parentContainer.nestedJsonObjects.getFirst().toString().getBytes());
            return msg;
        }

        @Override
        public void addContainer(SingleContainer singleContainer) {
            assert singleContainer instanceof SingleSDTMapAndJsonContainer;
            jsonContainer.addContainer(((SingleSDTMapAndJsonContainer)singleContainer).json);
            sdtMapContainer.addContainer(((SingleSDTMapAndJsonContainer)singleContainer).sdtMap);
        }

        @Override
        public boolean isEmpty() {
            return sdtMapContainer.isEmpty();
        }
    }
}
