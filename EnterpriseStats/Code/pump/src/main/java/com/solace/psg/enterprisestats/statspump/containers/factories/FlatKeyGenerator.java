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

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solace.psg.enterprisestats.statspump.util.StatsPumpConstants;

/**
 * This class is used by a number of container factories to create a single text
 * string for a series of hierarchical keys.
 * e.g. 
 * @author alee
 *
 */
public class FlatKeyGenerator {
    private static final Logger logger = LoggerFactory.getLogger(FlatKeyGenerator.class);

    // default level separator is '/', and default numeric index separator is '|'
    
    private final ArrayDeque<String> nestedElements;  // an ordered stack of nested element names / levels
    private final LinkedList<Map<String,Integer>> nestedElementsArrayCounters;

    public FlatKeyGenerator() {
        nestedElements = new ArrayDeque<String>();
        nestedElementsArrayCounters = new LinkedList<Map<String,Integer>>();
        nestedElementsArrayCounters.addLast(new HashMap<String,Integer>());
    }
    
    public void startNestedElement(String element, boolean isMultiple) {
        if (isMultiple) {
            int elementMultipleIndex = incMultipleIndex(element);
            nestedElements.add(new StringBuilder(element).append('|').append(elementMultipleIndex).toString());
        } else {
            nestedElements.addLast(element);
        }
        nestedElementsArrayCounters.addLast(new HashMap<String,Integer>());
    }
    
    private int incMultipleIndex(String element) {
        if (nestedElementsArrayCounters.getLast().containsKey(element)) {
            int index = nestedElementsArrayCounters.getLast().get(element);
            index++;
            nestedElementsArrayCounters.getLast().put(element,index);
            return index;
        } else {
            nestedElementsArrayCounters.getLast().put(element, new Integer(0));
            return 0;
        }
    }
    
    /**
     * Call this before putting anything, and will do the appropriate counters and whatnot
     * @param element
     * @param isMultiple
     * @return returns the flattened element name, including counter(s) if required
     */
    public String put(String element, boolean isMultiple) {
        if (isMultiple) incMultipleIndex(element);
        return buildFlatElement(element);  // element has the 'multiple' stuff already built into it, so no need for 'isMultiple' below
    }
    
    public void closeNestedElement() {
        assert nestedElements.size() > 0;
        nestedElements.removeLast();
        nestedElementsArrayCounters.removeLast();
    }

    /** Looking at the nested elements, this constructs a flattened string representation
     *  with arrays / sequences represented with a "|2" notation
     */
    private String buildFlatElement(String element) {
        StringBuilder sb = new StringBuilder(50);
        try {
            int levelCount = 0;
            for (String level : nestedElements) {
                sb.append(level);
                if (nestedElementsArrayCounters.get(levelCount).containsKey(level)) {
                    sb.append(StatsPumpConstants.FLAT_KEY_NUMERIC_INDEX_SEPARATOR).append(nestedElementsArrayCounters.get(levelCount).get(level));
                }
                levelCount++;
                sb.append(StatsPumpConstants.FLAT_KEY_LEVEL_SEPARATOR);
            }
            sb.append(element);
            if (nestedElementsArrayCounters.getLast().containsKey(element)) {
                sb.append(StatsPumpConstants.FLAT_KEY_NUMERIC_INDEX_SEPARATOR).append(nestedElementsArrayCounters.getLast().get(element));
            }
            return sb.toString();
        } catch (NoSuchElementException e) {  // no way!
            logger.error("This broke! "+sb.toString(),e);
            throw new AssertionError("sdf");
        }
    }
}
