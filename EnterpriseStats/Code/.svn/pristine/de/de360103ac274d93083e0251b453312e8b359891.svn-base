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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solace.psg.enterprisestats.statspump.StatsPumpMessage;
import com.solace.psg.enterprisestats.statspump.containers.ContainerFactory;
import com.solace.psg.enterprisestats.statspump.containers.ContainerUtils;
import com.solace.psg.enterprisestats.statspump.containers.GroupedContainer;
import com.solace.psg.enterprisestats.statspump.containers.SingleContainer;
import com.solace.psg.enterprisestats.statspump.tools.semp.XsdDataType;
import com.solace.psg.enterprisestats.statspump.util.SingleSolaceMessageFactory;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.MapMessage;
import com.solacesystems.jcsmp.Message;
import com.solacesystems.jcsmp.SDTException;
import com.solacesystems.jcsmp.SDTMap;

public enum FlatSdtMapFactory implements ContainerFactory {

    INSTANCE;

    private static final Logger logger = LoggerFactory.getLogger(FlatSdtMapFactory.class);

    @Override
    public SingleFlatSdtMapContainer newSingleContainer() {
        return new SingleFlatSdtMapContainer();
    }

    @Override
    public SingleFlatSdtMapContainer newSingleContainer(String sempVersion_ignore, String baseTag_ignore) {
        return new SingleFlatSdtMapContainer();
    }

    @Override
    public GroupedFlatSdtMapContainer newGroupedContainer(String sempVersion_ignore, String baseTag) {
        return new GroupedFlatSdtMapContainer(baseTag);
    }
    
    static class SingleFlatSdtMapContainer extends AbstractSingleContainer {
        
        protected final SDTMap map;  // a single map
        FlatKeyGenerator fkg = new FlatKeyGenerator();
        
        private SingleFlatSdtMapContainer() {
            map = JCSMPFactory.onlyInstance().createMap();
        }
        
        @Override
        public void startNestedElement(String element, boolean isMultiple) {
            fkg.startNestedElement(element,isMultiple);
        }
        
    
        @Override
        public void closeNestedElement(String element) {
            fkg.closeNestedElement();
        }
 
        @Override
        protected void startMultiple(String element) {
            // do nothing... doesn't matter
        }
    
        @Override
        public void put(XsdDataType type, String element, Object value, boolean isMultiple) {
            element = fkg.put(element,isMultiple);  // will increment the element
            super.put(type,element,value,isMultiple);
        }
    
        @Override
        public void putString(XsdDataType type, String element, String value, boolean isMultiple) {
            element = fkg.put(element,isMultiple);
            super.putString(type,element,value,isMultiple);
        }
    
        @Override
        protected void putLong(String element, Long value, boolean isMultiple) throws SDTException {
            map.putLong(element,value);
        }
    
        @Override
        protected void putInt(String element, Integer value, boolean isMultiple) throws SDTException {
            map.putInteger(element,value);
        }
    
        @Override
        protected void putShort(String element, Short value, boolean isMultiple) throws SDTException {
            map.putShort(element,value);
        }
    
        @Override
        protected void putByte(String element, Byte value, boolean isMultiple) throws SDTException {
            map.putByte(element,value);
        }
        
        @Override
        protected void putBoolean(String element, Boolean value, boolean isMultiple) throws SDTException {
            map.putBoolean(element,value);
        }
    
        @Override
        protected void putFloat(String element, Float value, boolean isMultiple) throws SDTException {
            map.putFloat(element,value);
        }
    
        @Override
        protected void putDouble(String element, Double value, boolean isMultiple) throws SDTException {
            map.putDouble(element,value);
        }
    
        @Override
        protected void putString(String element, String value, boolean isMultiple) throws SDTException {
            map.putString(element,value);
        }

        private void putMultipleMap(String element, SingleFlatSdtMapContainer singleFlatSDTMapContainer) {
            element = fkg.put(element,true);
            for (String key : singleFlatSDTMapContainer.map.keySet()) {
                try {
                    map.putObject(new StringBuilder(element).append('/').append(key).toString(),singleFlatSDTMapContainer.map.get(key));
                } catch (SDTException e) {
                    String currentState = String.format("Element='%s', Type='%s', Value='%s'%n%s",element,"SDTMap",singleFlatSDTMapContainer.map,this.toString());
                    logger.error(currentState,e);
                }
            }
        }

        @Override
        public MapMessage buildJcsmpMessage(StatsPumpMessage statsMessage) throws SDTException {
            MapMessage msg = SingleSolaceMessageFactory.getMessage(MapMessage.class);
            msg.setMap(map);
            // what headers do we want?
            ContainerUtils.prepMessage(msg,statsMessage);
            ContainerUtils.addSdtStreamTopicLevels(msg,statsMessage);
            return msg;
        }
    
        @Override
        public String toString() {
            return String.format("%s: %s",this.getClass().getSimpleName(),map.toString());
        }

    }

    // TODO here we use the last level of the basetag as root array, same like JSON, but not Nested..?  That's an anonymous array
    static class GroupedFlatSdtMapContainer implements GroupedContainer {

        final SingleFlatSdtMapContainer parentContainer;
        final String baseElement;

        private GroupedFlatSdtMapContainer(String baseTag) {
            parentContainer = new SingleFlatSdtMapContainer();
            this.baseElement = baseTag.substring(baseTag.lastIndexOf('/')+1);
        }

        @Override
        public Message buildJcsmpMessage(StatsPumpMessage statsMessage) throws SDTException {
            MapMessage msg = SingleSolaceMessageFactory.getMessage(MapMessage.class);
            msg.setMap(parentContainer.map);
            // what headers do we want?
            ContainerUtils.prepMessage(msg,statsMessage);
            ContainerUtils.addSdtStreamTopicLevels(msg,statsMessage);
            return msg;
        }

        @Override
        public void addContainer(SingleContainer singleContainer) {
            parentContainer.putMultipleMap(baseElement,((SingleFlatSdtMapContainer)singleContainer));
        }

        @Override
        public boolean isEmpty() {
            return parentContainer.map.isEmpty();
        }
    }
}
