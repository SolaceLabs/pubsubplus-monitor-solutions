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
import com.solace.psg.enterprisestats.statspump.containers.ContainerUtils;
import com.solace.psg.enterprisestats.statspump.containers.factories.SdtMapFactory.GroupedSdtMapContainer;
import com.solace.psg.enterprisestats.statspump.containers.factories.SdtMapFactory.SingleSdtMapContainer;
import com.solace.psg.enterprisestats.statspump.tools.semp.XsdDataType;

/**
 */
public enum CamelCaseSdtMapFactory implements ContainerFactory {

    INSTANCE;

    @Override
    public SingleCamelCaseSdtMapContainer newSingleContainer() {
        return new SingleCamelCaseSdtMapContainer();
    }

    @Override
    public SingleCamelCaseSdtMapContainer newSingleContainer(String sempVersion_ignore, String baseTag_ignore) {
        return new SingleCamelCaseSdtMapContainer();
    }

    @Override
    public GroupedCamelCaseSdtMapContainer newGroupedContainer(String sempVersion_ignore, String baseTag) {
        return new GroupedCamelCaseSdtMapContainer(baseTag);
    }

    public static class SingleCamelCaseSdtMapContainer extends SingleSdtMapContainer {

        protected SingleCamelCaseSdtMapContainer() {
            super();
        }
        
        @Override
        public void startNestedElement(String element, boolean isMultiple) {
            super.startNestedElement(ContainerUtils.getCamelCase(element),isMultiple);
        }

        @Override
        public void closeNestedElement(String element) {
            super.closeNestedElement(ContainerUtils.getCamelCase(element));
        }
        
        @Override
        protected void startMultiple(String element) {
            super.startMultiple(ContainerUtils.getCamelCase(element));
        }

        @Override
        public void put(XsdDataType type, String element, Object value, boolean isMultiple) {
            super.put(type,ContainerUtils.getCamelCase(element),value,isMultiple);
        }

        @Override
        public void putString(XsdDataType type, String element, String value, boolean isMultiple) {
            super.putString(type,ContainerUtils.getCamelCase(element),value,isMultiple);
        }
    }
    
    static class GroupedCamelCaseSdtMapContainer extends GroupedSdtMapContainer {

        protected GroupedCamelCaseSdtMapContainer(String baseTag) {
            super(ContainerUtils.getCamelCase(baseTag));
        }
    }
}
