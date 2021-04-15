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

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NamedThreadFactory implements java.util.concurrent.ThreadFactory  {
    private static final Logger logger = LoggerFactory.getLogger(NamedThreadFactory.class);
    
    private static final AtomicInteger count = new AtomicInteger();
    private final String threadName;
    private static Map<String,Set<Integer>> activeThreads = new HashMap<String,Set<Integer>>();
    
    public NamedThreadFactory(String threadName) {
        this.threadName = threadName;
        if (!activeThreads.containsKey(this.threadName)) {
            activeThreads.put(this.threadName,new HashSet<Integer>());
        }
    }
    
    @Override
    public Thread newThread(Runnable runnable) {
        final Thread t = new LoggedThread(runnable);
        t.setName(String.format("%s_%05x",threadName.replaceAll(" ","_"),count.getAndIncrement()));
        t.setDaemon(true);
        t.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable t) {
                logger.error(String.format("Thread '%s' just threw this and wasn't caught",thread.getName()),t);
            }
        });
        return t;
    }
}
