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
package com.solace.psg.enterprisestats.receiver.stats;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InternalStats {
    private ConcurrentMap<String, Double> internalStatsMap = new ConcurrentHashMap<String, Double>();

    public void setStatValue(String statName, Double value) {
        internalStatsMap.put(statName, value);
    }

    public void incrament(String statName) {
        double nCurrentValue = 0.0;

        if (internalStatsMap.containsKey(statName)) {
            nCurrentValue = internalStatsMap.get(statName);
        }
        nCurrentValue += 1;

        internalStatsMap.put(statName, nCurrentValue);
    }

    /**
     * <p>
     * Returns the string representation of this internal stats. The string
     * consists of the following format: <br/>
     * STAT_NAME_1 = value\n <br/>
     * STAT_NAME_2 = value\n
     * <p>
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Double> entry : internalStatsMap.entrySet()) {
            sb.append(entry.getKey() + " = " + entry.getValue() + "\n");
        }
        return sb.toString();
    }
}
