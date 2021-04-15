/**
 * Copyright 2016 Solace Systems, Inc. All rights reserved.
 *
 * http://www.solacesystems.com
 *
 * This source is distributed under the terms and conditions
 * of any contract or contracts between Solace Systems, Inc.
 * ("Solace") and you or your company.
 * If there are no contracts in place use of this source
 * is not authorized.
 * No support is provided and no distribution, sharing with
 * others or re-use of this source is authorized unless
 * specifically stated in the contracts referred to above.
 *
 * This product is provided as is and is not supported
 * by Solace unless such support is provided for under 
 * an agreement signed between you and Solace.
 */
package com.solace.psg.enterprisestats.statspump.util;

import java.util.HashSet;
import java.util.Set;

public enum MonitoredQueueTracker {

    INSTANCE;
    
    private Set<MonitoredLinkBlockingQueue<?>> queues = new HashSet<MonitoredLinkBlockingQueue<?>>();
    
    public void monitorQueue(MonitoredLinkBlockingQueue<?> queue) {
        queues.add(queue);
    }
    
    public boolean removeQueue(MonitoredLinkBlockingQueue<?> queue) {
        return queues.remove(queue);
    }
    
    public void clearAllHWMs() {
        for (MonitoredLinkBlockingQueue<?> queue : queues) {
            queue.clearHwm();
        }
    }
    
    public Set<MonitoredLinkBlockingQueue<?>> getQueues() {
        return queues;
    }
    
}
