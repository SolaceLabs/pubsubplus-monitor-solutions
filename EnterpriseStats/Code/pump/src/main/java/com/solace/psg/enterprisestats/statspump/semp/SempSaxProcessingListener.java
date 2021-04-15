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

package com.solace.psg.enterprisestats.statspump.semp;

import java.util.Map;

import com.solace.psg.enterprisestats.statspump.containers.SingleContainerSet;

public interface SempSaxProcessingListener {

    public void onStartSempReply(String sempVersion, long messageTimestamp);
    public void onStatMessage(SingleContainerSet containerSet, Map<String,String> objectValuesMap);
    public void onEndSempReply();
}