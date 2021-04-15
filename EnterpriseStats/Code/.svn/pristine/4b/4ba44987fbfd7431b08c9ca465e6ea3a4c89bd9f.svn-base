/*
 * Copyright 2014 Solace Systems, Inc. All rights reserved.
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

import com.solace.psg.enterprisestats.statspump.tools.semp.XsdDataType;

/**
 * <h2>Awesome Class!</h2>
 * @author Aaron Lee
 */
public interface SingleContainer extends Container {

    public void startNestedElement(String element, boolean isMultiple);
    public void closeNestedElement(String element);
    public void put(XsdDataType type, String element, Object value, boolean isMultiple);
    public void putString(XsdDataType type, String element, String value, boolean isMultiple);
}
