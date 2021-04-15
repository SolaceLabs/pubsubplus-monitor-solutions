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
package com.solace.psg.enterprisestats.receiver;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple wrapper for all configuration properties for StatsReceivers and
 * plugins.
 */
public class StatsReceiverProperties {
    private Map<String, Object> m_properties = new HashMap<String, Object>();

    /**
     * Constructor; loads the properties file
     * 
     * @param filename
     * @throws ReceiverException
     */
    public StatsReceiverProperties(Map<String, Object> properties) {
        this.m_properties = properties;
    }

    /**
     * Gets an int property, throwing if the specified key is not present.
     * 
     * @param name
     * @return The property value specified in the props file
     * @throws IllegalArgumentException
     */
    public int getMandatoryIntProperty(String name) throws IllegalArgumentException {
        if (!m_properties.containsKey(name)) {
            throw new IllegalArgumentException("Missing required configuration property '" + name + "'");
        }
        return Integer.parseInt((String) m_properties.get(name));
    }

    /**
     * Gets a String property, throwing if the specified key is not present.
     * 
     * @param name
     * @return The property value specified in the props file
     * @throws IllegalArgumentException
     */
    public String getMandatoryStringProperty(String name) throws IllegalArgumentException {
        if (!m_properties.containsKey(name)) {
            throw new IllegalArgumentException("Missing required configuration property '" + name + "'");
        }
        return (String) m_properties.get(name);
    }

    /**
     * Gets a String property, returning null if the specified key is not
     * present.
     * 
     * @param name
     * @return The property value specified in the props file
     */
    public String getOptionalStringProperty(String name) {
        return (String) m_properties.get(name);
    }

    public Object getProperty(String name) {
        return m_properties.get(name);
    }
    
    public void forcePropertyValue(String name, Object value) {
    	m_properties.put(name, value);
    }
}
