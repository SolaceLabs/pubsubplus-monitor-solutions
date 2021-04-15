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

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.NotImplementedException;

import com.solace.psg.enterprisestats.statspump.StatsPumpMessage;
import com.solacesystems.jcsmp.Message;

public class GroupedContainerSet extends HashSet<GroupedContainer> implements GroupedContainer {

    private static final long serialVersionUID = 1L;

    public GroupedContainerSet(Set<ContainerFactory> factories) {
    }
    
    /**
     * Not Implemented!!
     */
    @Override
    public Message buildJcsmpMessage(StatsPumpMessage message) {
        throw new NotImplementedException();
    }

    @Override
    public void addContainer(SingleContainer singleContainer) {
        for (GroupedContainer container : this) {
            container.addContainer(singleContainer);
        }
    }

    /**
     * Not Implemented!!
     */
    @Override
    public boolean isEmpty() {
        throw new NotImplementedException();
    }
}
