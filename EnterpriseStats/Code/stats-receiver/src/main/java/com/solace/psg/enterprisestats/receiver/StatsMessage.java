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

import java.util.Map;

/**
 * An instance of this class contains the parsed messages received by the
 * {@link StatsReceiver} from the Pump.
 */
public class StatsMessage {
    private final String topicName;
    private final Map<String, String> tags;
    private final Map<String, Object> keyValMap;
    private final Long messageTimeStamp;

    public StatsMessage(String topicName, Map<String, String> tags, Map<String, Object> keyValMap,
            Long messageTimeStamp) {
        this.topicName = topicName;
        this.tags = tags;
        this.keyValMap = keyValMap;
        this.messageTimeStamp = messageTimeStamp;
    }

    public String getTopicName() {
        return this.topicName;
    }

    public Map<String, String> getTags() {
        return this.tags;
    }

    public Map<String, Object> getValues() {
        return this.keyValMap;
    }

    public Long getTimestamp() {
        return this.messageTimeStamp;
    }

    public String dump() {
        StringBuilder sb = new StringBuilder();
        sb.append("--------------------------------------------------------\n");
        sb.append(topicName + " at " + messageTimeStamp + "\n");
        sb.append("tags: " + tags.toString() + "\n");
        sb.append("values: " + keyValMap.toString());
        return sb.toString();
    }
}
