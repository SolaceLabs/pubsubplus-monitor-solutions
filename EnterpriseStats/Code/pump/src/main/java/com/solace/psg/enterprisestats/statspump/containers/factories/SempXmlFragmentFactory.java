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

package com.solace.psg.enterprisestats.statspump.containers.factories;

import com.solace.psg.enterprisestats.statspump.containers.ContainerFactory;
import com.solace.psg.enterprisestats.statspump.containers.factories.SempXmlFactory.GroupedSempXmlContainer;
import com.solace.psg.enterprisestats.statspump.containers.factories.SempXmlFactory.SingleSempXmlContainer;

public enum SempXmlFragmentFactory implements ContainerFactory {
    
    INSTANCE;
    
    @Override
    public SingleSempXmlContainer newSingleContainer() {
        return new SingleSempXmlContainer(false);
    }

    @Override
    public SingleSempXmlContainer newSingleContainer(String sempVersion, String baseTag) {
        return new SingleSempXmlContainer(sempVersion,baseTag,false);
    }

    @Override
    public GroupedSempXmlContainer newGroupedContainer(String sempVersion,String baseTag) {
        return new GroupedSempXmlContainer(sempVersion,baseTag,false);
    }
}
