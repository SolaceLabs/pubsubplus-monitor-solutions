package com.solace.psg.enterprisestats.statspump.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.Message;

public class SolaceArrayMessageFactory {
    private static final int MAX_NUM_MSGS_VECTOR = 50;
    
    private static Map<Class<? extends Message>,Class<? extends Message>> requestedToActualMessageTypeMap = 
            Collections.synchronizedMap(new HashMap<Class<? extends Message>,Class<? extends Message>>());
    
    private static final ThreadLocal<Map<Class<? extends Message>,LinkedList<Message>>> threadedMessagePool = 
            new ThreadLocal<Map<Class<? extends Message>,LinkedList<Message>>>() {
        @Override
        protected Map<Class<? extends Message>,LinkedList<Message>> initialValue() {
            return new HashMap<Class<? extends Message>,LinkedList<Message>>();
        }
    };
    private static final Logger logger = LoggerFactory.getLogger(SolaceArrayMessageFactory.class);
    
    private SolaceArrayMessageFactory() {
    }
    
    private static void buildMessagesArray(Class<? extends Message> requestedMessageClass) {
        threadedMessagePool.get().put(requestedToActualMessageTypeMap.get(requestedMessageClass),new LinkedList<Message>());
        LinkedList<Message> linkedList = threadedMessagePool.get().get(requestedToActualMessageTypeMap.get(requestedMessageClass));
        for (int i=0;i<MAX_NUM_MSGS_VECTOR;i++) {
            linkedList.add(JCSMPFactory.onlyInstance().createMessage(requestedMessageClass));
        }
    }
    
    public static <T extends Message> T getMessage(Class<T> requestedMessageClass) {
        // this next section is not using ThreadLocal, so slim chance could be a race condition in this block, but we're using synchronized map, so all good
        if (!requestedToActualMessageTypeMap.containsKey(requestedMessageClass)) {
            logger.debug(String.format("First time asking for a message of class %s in thread %s",requestedMessageClass.getSimpleName(),Thread.currentThread().getName()));
            Message messageImpl = JCSMPFactory.onlyInstance().createMessage(requestedMessageClass);
            logger.info(String.format("Adding a mapping from requested %s to actual Message class %s in thread %s",requestedMessageClass.getSimpleName(),messageImpl.getClass().getSimpleName(),Thread.currentThread().getName()));
            requestedToActualMessageTypeMap.put(requestedMessageClass, messageImpl.getClass());
        }
        if (!threadedMessagePool.get().containsKey(requestedToActualMessageTypeMap.get(requestedMessageClass))) {
            // first time seeing this Message Class
            buildMessagesArray(requestedMessageClass);
        }
        @SuppressWarnings("unchecked")
        T typedMessage = (T) threadedMessagePool.get().get(requestedToActualMessageTypeMap.get(requestedMessageClass)).pollFirst();
        if (typedMessage == null) {
            logger.info(String.format("Not enough message of type %s in pool for thread %s",requestedToActualMessageTypeMap.get(requestedMessageClass).getSimpleName(),Thread.currentThread().getName()));
            throw new IllegalStateException("Somehow asking for more messages than are allowed..?");
        } else {
            return typedMessage;
        }
    }
    
    public static <T extends Message> void doneWithMessage(T message) {
        message.reset();
        //logger.debug(String.format("Done with message of class %s in thread %s",message.getClass().getSimpleName(),Thread.currentThread().getName()));
        threadedMessagePool.get().get(message.getClass()).addLast(message);
    }
}
