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

import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class MonitoredLinkBlockingQueue<E> extends LinkedBlockingQueue<E> implements Monitorable {

    private static final long serialVersionUID = 1L;
    private String name;
    private int currentHwm = 0;
    private int hwm = 0;
    
    public MonitoredLinkBlockingQueue(String name) {
        super(StatsPumpConstants.BLOCKING_QUEUE_TOTAL_CAPACITY);
        this.name = name;
        MonitoredQueueTracker.INSTANCE.monitorQueue(this);
    }
    
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        return super.offer(e, timeout, unit);
    }

    @Override
    public boolean offer(E e) {
        hwm = Math.max(hwm,this.size());
        currentHwm = Math.max(currentHwm,this.size());
        return super.offer(e);
    }

    @Override
    public E take() throws InterruptedException {
        hwm = Math.max(hwm,this.size());
        currentHwm = Math.max(currentHwm,this.size());
        return  super.take();
    }
    
    @Override
    public int drainTo(Collection<? super E> arg0, int arg1) {
        return super.drainTo(arg0, arg1);
    }

    public int getHwm() {
        return hwm;
    }
    
    public int getCurrentHwm() {
        int tmp = currentHwm;
        currentHwm = 0;
        return tmp;
    }
    
    @Override
    public void clearHwm() {
        hwm = 0;
    }
    
    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getStats() {
        return String.format("Current depth=%d, Current HWM=%d, HWM=%d",size(),getCurrentHwm(),getHwm());
    }

    @Override
    public void clearStats() {
        clearHwm();
    }
}
