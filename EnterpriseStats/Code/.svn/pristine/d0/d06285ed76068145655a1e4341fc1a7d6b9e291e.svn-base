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
import java.util.Deque;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

import com.solace.psg.enterprisestats.statspump.StatsPumpMessage;
import com.solace.psg.enterprisestats.statspump.containers.ContainerFactory;
import com.solace.psg.enterprisestats.statspump.containers.ContainerUtils;
import com.solace.psg.enterprisestats.statspump.containers.GroupedContainer;
import com.solace.psg.enterprisestats.statspump.containers.SingleContainer;
import com.solace.psg.enterprisestats.statspump.util.SingleSolaceMessageFactory;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.Message;
import com.solacesystems.jcsmp.SDTException;
import com.solacesystems.jcsmp.TextMessage;

/**
 * This class will produce a JSON object based on the provided data.<p>
 * For the grouped container, it will format the data as an array at the
 * base layer.
 */
public enum JsonMapFactory implements ContainerFactory {
    
    INSTANCE;

    @Override
    public SingleJsonMapContainer newSingleContainer() {
        return new SingleJsonMapContainer();
    }

    @Override
    public SingleJsonMapContainer newSingleContainer(String sempVersion_ignore, String baseTag_ignore) {
        return new SingleJsonMapContainer();
    }

    @Override
    public GroupedJsonMapContainer newGroupedContainer(String sempVersion_ignore, String baseTag) {
        return new GroupedJsonMapContainer(baseTag);
    }
    
    protected static class SingleJsonMapContainer extends AbstractSingleContainer {

        //protected Deque<String> nestedElements = new ArrayDeque<String>();             // an ordered stack of nested elements / levels
        protected Deque<JSONObject> nestedJsonObjects = new ArrayDeque<JSONObject>();  // an ordered stack of nested JSONObjets
        // as you go "in" one level in the SEMP XML, you add a new JSON object, which then gets added to its parent when closing that tag

        protected SingleJsonMapContainer() {
            nestedJsonObjects.add(new JSONObject());  // the root map
        }
        
        @Override
        public void startNestedElement(String element, boolean isMultiple) {
            JSONObject map = new JSONObject();
            if (isMultiple) {
                startMultiple(element);  // this will insert an array into the current (last) map
            }
            //nestedElements.addLast(element);
            nestedJsonObjects.addLast(map);
        }
    
        @Override
        public void closeNestedElement(String element) {
            //String element = nestedElements.removeLast();
            JSONObject map = nestedJsonObjects.removeLast();
            boolean isMultiple = false;
            Iterator<?> i = nestedJsonObjects.getLast().keys();
            while (i.hasNext()) {
                if (element.equals(i.next())) {
                    isMultiple = true;
                }
            }
            if (isMultiple) {
//                logger.debug("Adding a map of size "+map.size()+" into the parent stream with element "+element);
                nestedJsonObjects.getLast().getJSONArray(element).put(map);
            } else {
//                logger.debug("Adding a map of size "+map.size()+" into the parent map with element "+element);
                nestedJsonObjects.getLast().put(element,map);
            }
        }
        
        /**
         * This method makes sure that there is a JSON array keyed to this element.
         * If not, it inserts one in the last (leaf) parent object
         * @param element
         */
        @Override
        protected void startMultiple(String element) {
            Iterator<?> i = nestedJsonObjects.getLast().keys();
            while (i.hasNext()) {
                if (element.equals(i.next())) {
                    return;
                }
            }
            // else, couldn't find this element name, so create a new array
            nestedJsonObjects.getLast().put(element,new JSONArray());
        }
        
        /**
         * Used by the Grouped JSON Container below
         * @param element
         * @param value
         */
        protected void putJsonObject(String element, JSONObject value) {
            startMultiple(element);
            nestedJsonObjects.getLast().getJSONArray(element).put(value);
        }

        @Override
        protected void putLong(String element, Long value, boolean isMultiple) {
            if (isMultiple) nestedJsonObjects.getLast().getJSONArray(element).put(value);
            else nestedJsonObjects.getLast().put(element,value);
        }
    
        @Override
        protected void putInt(String element, Integer value, boolean isMultiple) {
            if (isMultiple) nestedJsonObjects.getLast().getJSONArray(element).put(value);
            else nestedJsonObjects.getLast().put(element,value);
        }
    
        @Override
        protected void putShort(String element, Short value, boolean isMultiple) {
            if (isMultiple) nestedJsonObjects.getLast().getJSONArray(element).put(value);
            else nestedJsonObjects.getLast().put(element,value);
        }
    
        @Override
        protected void putByte(String element, Byte value, boolean isMultiple) {
            if (isMultiple) nestedJsonObjects.getLast().getJSONArray(element).put(value);
            else nestedJsonObjects.getLast().put(element,value);
        }
        
        @Override
        protected void putBoolean(String element, Boolean value, boolean isMultiple) {
            if (isMultiple) nestedJsonObjects.getLast().getJSONArray(element).put(value);
            else nestedJsonObjects.getLast().put(element,value);
        }
    
        @Override
        protected void putFloat(String element, Float value, boolean isMultiple) {
            if (isMultiple) nestedJsonObjects.getLast().getJSONArray(element).put(value);
            else nestedJsonObjects.getLast().put(element,value);
        }
    
        @Override
        protected void putDouble(String element, Double value, boolean isMultiple) {
            if (isMultiple) nestedJsonObjects.getLast().getJSONArray(element).put(value);
            else nestedJsonObjects.getLast().put(element,value);
        }
    
        @Override
        protected void putString(String element, String value, boolean isMultiple) {
            if (isMultiple) nestedJsonObjects.getLast().getJSONArray(element).put(value);
            else nestedJsonObjects.getLast().put(element,value);
        }
    
        protected JSONObject getRoot() {
            assert nestedJsonObjects.size() == 1;
            return nestedJsonObjects.getFirst();
        }
        
        @Override
        public BytesXMLMessage buildJcsmpMessage(StatsPumpMessage statsMessage) throws SDTException {
            TextMessage msg = SingleSolaceMessageFactory.getMessage(TextMessage.class);
            msg.setText(getRoot().toString());
			//            BytesXMLMessage msg = SingleSolaceMessageFactory.getMessage(BytesXMLMessage.class);
			//            msg.writeAttachment(getRoot().toString().getBytes(Charset.forName("UTF-8")));
            // what headers do we want?
            ContainerUtils.prepMessage(msg,statsMessage);
            ContainerUtils.addJsonTopicLevels(msg,statsMessage);
            return msg;
        }
    
        @Override
        public String toString() {
            return String.format("%s: %s",this.getClass().getSimpleName(),nestedJsonObjects.toString());
        }
    }
    
    protected static class GroupedJsonMapContainer implements GroupedContainer {

        protected final SingleJsonMapContainer parentContainer = new SingleJsonMapContainer();
        protected final String baseElement;
        
        protected GroupedJsonMapContainer(String baseTag) {
            this.baseElement = baseTag.substring(baseTag.lastIndexOf('/')+1);
        }
        
        @Override
        public Message buildJcsmpMessage(StatsPumpMessage statsMessage) throws SDTException {
            TextMessage msg = SingleSolaceMessageFactory.getMessage(TextMessage.class);
            msg.setText(parentContainer.nestedJsonObjects.getFirst().toString());
            // what headers do we want?
            ContainerUtils.prepMessage(msg,statsMessage);
            ContainerUtils.addSdtStreamTopicLevels(msg,statsMessage);
            return msg;
        }

        @Override
        public void addContainer(SingleContainer singleContainer) {
            assert singleContainer instanceof SingleJsonMapContainer;
            parentContainer.putJsonObject(this.baseElement,((SingleJsonMapContainer)singleContainer).nestedJsonObjects.getFirst());
        }

        @Override
        public boolean isEmpty() {
            return parentContainer.nestedJsonObjects.isEmpty();
        }
    }
}
