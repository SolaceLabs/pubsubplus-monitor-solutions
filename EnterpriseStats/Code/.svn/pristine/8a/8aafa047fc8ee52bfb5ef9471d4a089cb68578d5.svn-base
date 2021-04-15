/**
 * Copyright 2016-2017 Solace Corporation. All rights reserved.
 *
 * http://www.solace.com
 *
 * This source is distributed under the terms and conditions of any contract or
 * contracts between Solace Corporation ("Solace") and you or your company. If
 * there are no contracts in place use of this source is not authorized. No
 * support is provided and no distribution, sharing with others or re-use of 
 * this source is authorized unless specifically stated in the contracts 
 * referred to above.
 *
 * This software is custom built to specifications provided by you, and is 
 * provided under a paid service engagement or statement of work signed between
 * you and Solace. This product is provided as is and is not supported by 
 * Solace unless such support is provided for under an agreement signed between
 * you and Solace.
 */
package com.solace.psg.enterprisestats.receiver.sample;

import com.solace.psg.enterprisestats.receiver.AbstractStatsTap;
import com.solace.psg.enterprisestats.receiver.ReceiverException;
import com.solace.psg.enterprisestats.receiver.StatsMessage;

/**
 * Implements the Tap interface, as a sample that just prints to screen.
 */
public class SampleReciever extends AbstractStatsTap {

    @Override
    public void onRouterStats(StatsMessage message) throws ReceiverException {
        System.out.println(message.dump());
    }

    @Override
    public void onPumpStats(StatsMessage message) throws ReceiverException {
        System.out.println(message.dump());
    }
}
