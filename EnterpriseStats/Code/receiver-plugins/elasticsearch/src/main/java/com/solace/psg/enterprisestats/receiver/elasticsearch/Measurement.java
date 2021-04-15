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
package com.solace.psg.enterprisestats.receiver.elasticsearch;

import java.util.Map;

/**
 * Simple data structure for passing related info around in this class.
 */
public class Measurement {
    private final String name;
    private final Map<String, String> tags;
    private final Map<String, Object> map;
    private final Long timestamp;

    public Measurement(String name, Map<String, String> tags, Map<String, Object> map, Long timestamp) {
        this.name = name;
        this.tags = tags;
        this.map = map;
        this.timestamp = timestamp;
    }

    public String getName() {
        return name;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public Map<String, Object> getMap() {
        return map;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return name + " tags: " + tags + " map: " + map;
    }
}
