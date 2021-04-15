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

package com.solace.psg.enterprisestats.statspump.util;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.XMLMessage;

/**
 * This class keeps one message of each type in a Map to be used by publishing threads.
 * This will have to be modified if starting to do vectored sends or something.<p>
 * This is a much simpler version of this class than originally coded.  So if needing
 * to do multiple messages from the same factory, keep the "static interface" and get the
 * old version of this class.
 */
public class SingleSolaceMessageFactory {

    private static final ThreadLocal<Map<Class<? extends XMLMessage>,XMLMessage>> threadedRequestedToActualMessageMap = 
            new ThreadLocal<Map<Class<? extends XMLMessage>,XMLMessage>>() {
        @Override
        protected Map<Class<? extends XMLMessage>,XMLMessage> initialValue() {
            return new HashMap<Class<? extends XMLMessage>,XMLMessage>();
        }
    };
    private static final Logger logger = LoggerFactory.getLogger(SingleSolaceMessageFactory.class);
    
    private SingleSolaceMessageFactory() {
        throw new AssertionError("This is a utility class. Do not instantiate.");
    }
    
    @SuppressWarnings("unchecked")
    public static <T extends XMLMessage> T getMessage(Class<T> requestedMessageClass) {
        if (!threadedRequestedToActualMessageMap.get().containsKey(requestedMessageClass)) {
            XMLMessage messageImpl = JCSMPFactory.onlyInstance().createMessage(requestedMessageClass);
            logger.info(String.format("Adding a mapping from requested %s to actual Message class %s in thread %s",requestedMessageClass.getSimpleName(),messageImpl.getClass().getSimpleName(),Thread.currentThread().getName()));
            threadedRequestedToActualMessageMap.get().put(requestedMessageClass,messageImpl);
            return (T)messageImpl;
        }
        return (T)threadedRequestedToActualMessageMap.get().get(requestedMessageClass);
    }
    
    public static <T extends XMLMessage> void doneWithMessage(T message) {
        message.reset();
        //logger.debug(String.format("Done with message of class %s in thread %s",message.getClass().getSimpleName(),Thread.currentThread().getName()));
        // don't even need to put it back in the pool (back of linked list) anymore since its a pool of one, and we don't lose a reference to it
        //threadedMessagePool.get().get(message.getClass()).addLast(message);
    }
}
