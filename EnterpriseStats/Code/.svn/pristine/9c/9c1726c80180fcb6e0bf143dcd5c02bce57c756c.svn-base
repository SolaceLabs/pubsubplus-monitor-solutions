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

package com.solace.psg.enterprisestats.statspump.containers;

import java.util.LinkedHashMap;
import java.util.Set;

import org.apache.commons.lang.NotImplementedException;

import com.solace.psg.enterprisestats.statspump.StatsPumpMessage;
import com.solace.psg.enterprisestats.statspump.tools.semp.XsdDataType;
import com.solacesystems.jcsmp.Message;

/**
 * This class is a sneaky way of having a set of containers behave like a SingleContainer.  It will contain
 * a number of containers determined by the ContainerFactories it is initialized with.
 * @author alee
 *
 */
public class SingleContainerSet extends LinkedHashMap<ContainerFactory,SingleContainer> implements SingleContainer {
        
    private static final long serialVersionUID = 1L;
    
    public SingleContainerSet(Set<ContainerFactory> factories, String sempVersion,String baseTag) {
        for (ContainerFactory factory : factories) {
            SingleContainer container = factory.newSingleContainer(sempVersion,baseTag);
            this.put(factory,container);
        }
    }
    
    public SingleContainerSet(Set<ContainerFactory> factories) {
        for (ContainerFactory factory : factories) {
            SingleContainer container = factory.newSingleContainer();
            this.put(factory,container);
        }
    }
    
    public Container getContainer(ContainerFactory factory) {
        return this.get(factory);
    }
    
    /**
     * Not Implemented!!
     */
    @Override
    public Message buildJcsmpMessage(StatsPumpMessage message) {
        throw new NotImplementedException();
    }

    @Override
    public void startNestedElement(String element, boolean isMultiple) {
        for (SingleContainer container : this.values()) {
            container.startNestedElement(element,isMultiple);
        }
    }

    @Override
    public void closeNestedElement(String element) {
        for (SingleContainer container : this.values()) {
            container.closeNestedElement(element);
        }
    }

    @Override
    public void put(XsdDataType type, String element, Object value, boolean isMultiple) {
        for (SingleContainer container : this.values()) {
            container.put(type, element, value, isMultiple);
        }
    }

    @Override
    public void putString(XsdDataType type, String element, String value, boolean isMultiple) {
        for (SingleContainer container : this.values()) {
            container.putString(type, element, value, isMultiple);
        }
    }
}
