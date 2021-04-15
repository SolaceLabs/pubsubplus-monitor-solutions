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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggedThread extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(LoggedThread.class);
    
    public LoggedThread(Runnable runnable) {
        super(runnable);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        StackTraceElement[] stacks = getStackTrace();
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement el : stacks) {
            sb.append(String.format("%s - %s, line %d%n",el.getClassName(),el.getMethodName(),el.getLineNumber()));
        }
        logger.debug("finalize() called for thread "+this.getName());
        logger.debug(sb.toString());
    }
}
