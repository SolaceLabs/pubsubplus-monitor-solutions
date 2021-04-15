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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solace.psg.enterprisestats.statspump.StatsPumpMessage;
import com.solace.psg.enterprisestats.statspump.containers.ContainerFactory;
import com.solace.psg.enterprisestats.statspump.containers.ContainerUtils;
import com.solace.psg.enterprisestats.statspump.containers.GroupedContainer;
import com.solace.psg.enterprisestats.statspump.containers.SingleContainer;
import com.solace.psg.enterprisestats.statspump.util.SingleSolaceMessageFactory;
import com.solace.psg.enterprisestats.statspump.utils.PayloadUtils;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.MapMessage;
import com.solacesystems.jcsmp.Message;
import com.solacesystems.jcsmp.SDTException;
import com.solacesystems.jcsmp.SDTMap;
import com.solacesystems.jcsmp.SDTStream;

/**
 */
public enum SdtMapFactory implements ContainerFactory {

    INSTANCE;
    private static final Logger logger = LoggerFactory.getLogger(SdtMapFactory.class);

    @Override
    public SingleSdtMapContainer newSingleContainer() {
        return new SingleSdtMapContainer();
    }

    @Override
    public SingleSdtMapContainer newSingleContainer(String sempVersion_ignore, String baseTag_ignore) {
        return new SingleSdtMapContainer();
    }

    @Override
    public GroupedSdtMapContainer newGroupedContainer(String sempVersion_ignore, String baseTag) {
        return new GroupedSdtMapContainer(baseTag);
    }
    

    protected static class SingleSdtMapContainer extends AbstractSingleContainer {
        
        protected Deque<SDTMap> nestedMaps = new ArrayDeque<SDTMap>();      // an ordered stack of nested SDTMaps, possibly containing SDTStreams
        
        protected SingleSdtMapContainer() {
            nestedMaps.push(JCSMPFactory.onlyInstance().createMap());
        }
        
        @Override
        public void startNestedElement(String element, boolean isMultiple) {
            SDTMap map = JCSMPFactory.onlyInstance().createMap();
            if (isMultiple) {
                startMultiple(element);  // this will insert a stream into the current (last) map
            }
            nestedMaps.push(map);
        }

        @Override
        public void closeNestedElement(String element) {
            SDTMap map = nestedMaps.pop();
            try {
                boolean isMultiple = nestedMaps.peek().containsKey(element);
                if (isMultiple) {
                    logger.trace("Adding a map of size "+map.size()+" into the parent stream with element "+element);
                    nestedMaps.peek().getStream(element).writeMap(map);
                } else {
                    logger.trace("Adding a map of size "+map.size()+" into the parent map with element "+element);
                    nestedMaps.peek().putMap(element,map);
                }
            } catch (SDTException e) {
                logger.warn(String.format("Encountered when trying to add '%s' to the parent map",element),e);
            }
        }
        
        @Override
        protected void startMultiple(String element) {
            if (!nestedMaps.peek().containsKey(element)) {
                try {
                    nestedMaps.peek().putStream(element, JCSMPFactory.onlyInstance().createStream());
                } catch (SDTException e) {
                    logger.warn(String.format("Encountered when trying to add stream element '%s' to the parent map",element),e);
                }
            }
        }

        protected void putLong(String element, Long value, boolean isMultiple) throws SDTException {
            if (isMultiple) nestedMaps.peek().getStream(element).writeLong(value);
            else nestedMaps.peek().putLong(element,value);
        }

        protected void putInt(String element, Integer value, boolean isMultiple) throws SDTException {
            if (isMultiple) nestedMaps.peek().getStream(element).writeInteger(value);
            else nestedMaps.peek().putInteger(element,value);
        }

        protected void putShort(String element, Short value, boolean isMultiple) throws SDTException {
            if (isMultiple) nestedMaps.peek().getStream(element).writeShort(value);
            else nestedMaps.peek().putShort(element,value);
        }

        protected void putByte(String element, Byte value, boolean isMultiple) throws SDTException {
            if (isMultiple) nestedMaps.peek().getStream(element).writeByte(value);
            else nestedMaps.peek().putByte(element,value);
        }
        
        protected void putBoolean(String element, Boolean value, boolean isMultiple) throws SDTException {
            if (isMultiple) nestedMaps.peek().getStream(element).writeBoolean(value);
            else nestedMaps.peek().putBoolean(element,value);
        }

        protected void putFloat(String element, Float value, boolean isMultiple) throws SDTException {
            if (isMultiple) nestedMaps.peek().getStream(element).writeFloat(value);
            else nestedMaps.peek().putFloat(element,value);
        }

        protected void putDouble(String element, Double value, boolean isMultiple) throws SDTException {
            if (isMultiple) nestedMaps.peek().getStream(element).writeDouble(value);
            else nestedMaps.peek().putDouble(element,value);
        }

        protected void putString(String element, String value, boolean isMultiple) throws SDTException {
            if (isMultiple) nestedMaps.peek().getStream(element).writeString(value);
            else nestedMaps.peek().putString(element,value);
        }
        
        protected SDTMap getRoot() {
            assert nestedMaps.size() == 1;
            return nestedMaps.getFirst();
        }
        
        @Override
        public MapMessage buildJcsmpMessage(StatsPumpMessage statsMessage) throws SDTException {
            MapMessage msg = SingleSolaceMessageFactory.getMessage(MapMessage.class);
            msg.setMap(getRoot());
            // what headers do we want?
            ContainerUtils.prepMessage(msg,statsMessage);
            ContainerUtils.addSdtStreamTopicLevels(msg,statsMessage);
            return msg;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (SDTMap map : nestedMaps) {
                sb.append(PayloadUtils.prettyPrint(map,4));
            }
            return String.format("%s: %d elements",this.getClass().getSimpleName(),nestedMaps.getFirst().size());
        }
    }
    
    protected static class GroupedSdtMapContainer implements GroupedContainer {

        private final List<SingleSdtMapContainer> singles = new ArrayList<SingleSdtMapContainer>();
        private final String baseElement;
        
        protected GroupedSdtMapContainer(String baseTag) {
            this.baseElement = baseTag.substring(baseTag.lastIndexOf('/')+1);
        }
        
        @Override
        public Message buildJcsmpMessage(StatsPumpMessage statsMessage) throws SDTException {
            SDTStream stream = JCSMPFactory.onlyInstance().createStream();
            for (SingleSdtMapContainer single : singles) {
                stream.writeMap(single.getRoot());
            }
            MapMessage msg = SingleSolaceMessageFactory.getMessage(MapMessage.class);
            SDTMap map = JCSMPFactory.onlyInstance().createMap();
            map.putStream(baseElement,stream); // Should be impossible to throw exception... only way would be if stream wasn't a SDTStream..!
            msg.setMap(map);
            ContainerUtils.prepMessage(msg,statsMessage);
            return msg;
        }

        @Override
        public void addContainer(SingleContainer singleContainer) {
            assert singleContainer instanceof SingleSdtMapContainer;
            singles.add((SingleSdtMapContainer)singleContainer);
        }
        
        @Override 
        public boolean isEmpty() {
            return singles.isEmpty();
        }
    }
}
