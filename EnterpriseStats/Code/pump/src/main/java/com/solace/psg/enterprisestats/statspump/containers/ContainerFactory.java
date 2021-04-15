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


/**
 * An interface that provides a number of 
 * @author Aaron Lee
 */
public interface ContainerFactory {

    /**
     * This is some descriptive text here.
     * @param baseTag - will be of the form /rpc/reply/show/... and indicates
     * where in the XML reply schema the reply comes from.  Not every implementation
     * needs to use this data, it is provide just in case.
     * @return
     */
    public SingleContainer newSingleContainer(String sempVersion,String baseTag);
    public SingleContainer newSingleContainer();//String sempVersion);
    
    /**
     * This is some descriptive text here.
     * @param baseTag - will be of the form /rpc/reply/show/... and indicates
     * where in the XML reply schema the reply comes from.  Not every implementation
     * needs to use this data, it is provide just in case.
     * @return
     */
    public GroupedContainer newGroupedContainer(String baseTag, String sempVersion);
}
