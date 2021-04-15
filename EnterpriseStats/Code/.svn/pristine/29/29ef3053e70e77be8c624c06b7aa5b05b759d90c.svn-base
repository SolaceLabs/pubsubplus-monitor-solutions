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
package com.solace.psg.enterprisestats.receiver.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.solace.psg.enterprisestats.receiver.ReceiverException;
import com.solace.psg.enterprisestats.receiver.StatsTap;
import com.solace.psg.util.AES;
import com.solacesystems.jcsmp.SDTException;
import com.solacesystems.jcsmp.SDTMap;
import com.solacesystems.jcsmp.SDTStream;

import java.util.Properties;

/**
 * A helper utility class for StatsPump Receivers.
 */
public final class ReceiverUtils {

    private ReceiverUtils() {
        throw new AssertionError();
    }

    /**
     * Load properties from a configuration file.
     * 
     * @param fname
     * @return
     * @throws IOException
     */
    public static Map<String, Object> loadPropertiesFrom(String fname) throws IOException {
        FileInputStream fis = new FileInputStream(fname);
        Properties p = new Properties();
        p.load(fis);
        return getPropertiesMap(p);
    }

    /**
     * Return a map from initialized from a Properties object.
     * 
     * @param properties
     * @return
     */
    public static Map<String, Object> getPropertiesMap(Properties properties) {
        Map<String, Object> propMap = new HashMap<String, Object>();
        for (Entry<Object, Object> x : properties.entrySet()) {
            propMap.put((String) x.getKey(), x.getValue());
        }

        return propMap;
    }

    /**
     * The system-dependent path-separator character, represented as a string
     * for convenience.
     * 
     * @return
     */
    public static String getFileSeparator() {
        return File.pathSeparator;
    }

    public static String decryptPassword(String password, String propName) throws ReceiverException {
        String rc = null;
        try {
            rc = AES.decrypt(password);
        } catch (Exception e) {
            throw new ReceiverException("The password for the configuration properties '" + propName
                    + "' does not appear to be encrypted. Use the Solace Password Utility to encrypt the "
                    + "passwords in reciever configuration file.");
        }
        return rc;
    }
    
    /**
     * <p>
     * Uses reflection to generate an instance of a <code>StatsTap</code> based
     * on given className.
     * </p>
     * 
     * @param className
     *            The full class name of the concrete implementation of the
     *            <code>StatsTap</code> interface.
     * @return The instantiated <code>StatsTap</code> concrete class.
     * @throws ReceiverException
     *             if the className could not be instantiated.
     */
    public static StatsTap statsTapLoader(String className) throws ReceiverException {
        if (className != null) {
            try {
                Class<?> clazz = Class.forName(className);
                // Get a new instance of this class
                StatsTap statsTap = (StatsTap) clazz.newInstance();
                return statsTap;
            } catch (ClassNotFoundException e) {
                throw new ReceiverException("The StatsTap implementation '" + className + "' class was not found", e);
            } catch (InstantiationException e) {
                throw new ReceiverException("Please ensure the StatsTap implementation '" + className
                        + "' class implements the StatsTap interface.", e);
            } catch (IllegalAccessException e) {
                throw new ReceiverException("Please ensure the StatsTap implementation '" + className
                        + "' class implements the StatsTap interface.", e);
            }
        }
        return null;
    }

    public static void parseSdtMap(final SDTMap sdtMap, final Map<String, Object> flatMap) throws SDTException {
        parseSdtMap(sdtMap, flatMap, "");
    }

    public static void parseSdtMap(final SDTMap sdtMap, final Map<String, Object> flatMap, final String flatKeyPrefix)
            throws SDTException {
        for (String key : sdtMap.keySet()) {
            Object o = sdtMap.get(key);
            if (o instanceof SDTMap) {
                parseSdtMap((SDTMap) o, flatMap, flatKeyPrefix + key + "/");
            } else if (o instanceof SDTStream) {
                parseSdtStream((SDTStream) o, flatMap, flatKeyPrefix + key);
            } else {
                flatMap.put(flatKeyPrefix + key, o);
            }
        }
    }

    public static void parseSdtStream(final SDTStream stdStream, final Map<String, Object> flatMap,
            final String flatKeyPrefix) throws SDTException {
        int count = 0;
        while (stdStream.hasRemaining()) {
            // so would look like stats|0
            String key = flatKeyPrefix + "|" + count;
            Object o = stdStream.read();
            if (o instanceof SDTMap) {
                // so, stats|0/key, stats|0/val
                parseSdtMap((SDTMap) o, flatMap, key + "/");
            } else if (o instanceof SDTStream) {
                // TODO: Log proper error to logging framework
                System.err.println("ERROR - streams of streams found\n" + key + "\n" + flatMap);
                parseSdtStream((SDTStream) o, flatMap, key + "|");
            } else {
                flatMap.put(key, o);
            }
            count++;
        }
        stdStream.rewind();
    }

    /**
     * This would look like SYSTEM_INTERFACE, or VPN_QUEUE based of levels[1]
     * and [3]
     * 
     * @param topicLevels
     * @return
     */
    public static String getShortName(List<String> topicLevels) {
        return topicLevels.get(1) + "_"
                + (topicLevels.get(1).equals("SYSTEM") ? topicLevels.get(3) : topicLevels.get(4));
    }

    /**
     * Goes into the message header and gets the levels
     * 
     * @param stream
     * @return
     * @throws ReceiverException
     */
    public static List<String> getLevels(SDTStream stream) throws ReceiverException {
        List<String> levels = new ArrayList<String>();
        try {
            while (stream.hasRemaining()) {
                levels.add(stream.readString());
            }
        } catch (SDTException e) {
            throw new ReceiverException(e);
        }
        stream.rewind();
        return levels;
    }
}
