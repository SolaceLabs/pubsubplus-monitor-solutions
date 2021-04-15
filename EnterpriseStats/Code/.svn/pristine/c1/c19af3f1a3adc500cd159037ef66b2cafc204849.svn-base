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

import com.solace.psg.enterprisestats.statspump.StatsPumpMessage;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.JCSMPException;

/**
 * This parent interface for both Single and Grouped Containers is used
 * when a message is about to be published onto the Solace message-bus.
 * @author Aaron Lee
 */
public interface Container {

    /**
     * This method is called when sending a message. Use whatever methods
     * to construct a JSCMP Message object.<p>
     * See util.SolaceMessageFactory as it provides a way to fetch empty
     * messages from a pool that can be reused after having been sent
     * @return Message - a JCSMP Message class
     */
    public BytesXMLMessage buildJcsmpMessage(StatsPumpMessage message) throws JCSMPException;
}
