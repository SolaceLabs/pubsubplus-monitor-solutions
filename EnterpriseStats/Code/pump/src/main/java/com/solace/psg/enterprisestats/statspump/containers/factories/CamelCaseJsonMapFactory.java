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

import org.json.JSONObject;

import com.solace.psg.enterprisestats.statspump.containers.ContainerFactory;
import com.solace.psg.enterprisestats.statspump.containers.ContainerUtils;
import com.solace.psg.enterprisestats.statspump.containers.factories.JsonMapFactory.GroupedJsonMapContainer;
import com.solace.psg.enterprisestats.statspump.containers.factories.JsonMapFactory.SingleJsonMapContainer;
import com.solace.psg.enterprisestats.statspump.tools.semp.XsdDataType;

/**
 * This class will produce a JSON object based on the provided data.<p>
 * For the grouped container, it will format the data as an array at the
 * base layer.
 */
public enum CamelCaseJsonMapFactory implements ContainerFactory {
    
    INSTANCE;

    @Override
    public SingleCamelCaseJsonMapContainer newSingleContainer() {
        return new SingleCamelCaseJsonMapContainer();
    }

    @Override
    public SingleCamelCaseJsonMapContainer newSingleContainer(String sempVersion_ignore, String baseTag_ignore) {
        return new SingleCamelCaseJsonMapContainer();
    }

    @Override
    public GroupedCamelCaseJsonMapContainer newGroupedContainer(String sempVersion_ignore, String baseTag) {
        return new GroupedCamelCaseJsonMapContainer(baseTag);
    }
    
    protected static class SingleCamelCaseJsonMapContainer extends SingleJsonMapContainer {

        protected SingleCamelCaseJsonMapContainer() {
            super();
        }
        
        @Override
        public void startNestedElement(String element, boolean isMultiple) {
            super.startNestedElement(ContainerUtils.getCamelCase(element),isMultiple);
        }
            
        /**
         * This method makes sure that there is a JSON array keyed to this element.
         * If not, it inserts one in the last (leaf) parent object
         * @param element
         */
        @Override
        protected void startMultiple(String element) {
            super.startMultiple(ContainerUtils.getCamelCase(element));
        }
        
        /**
         * Used by the Grouped JSON Container below
         * @param element
         * @param value
         */
        protected void putJsonObject(String element, JSONObject value) {
            super.putJsonObject(ContainerUtils.getCamelCase(element),value);
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
    
    protected static class GroupedCamelCaseJsonMapContainer extends GroupedJsonMapContainer {

        protected GroupedCamelCaseJsonMapContainer(String baseTag) {
            super(ContainerUtils.getCamelCase(baseTag));
        }
    }
}
