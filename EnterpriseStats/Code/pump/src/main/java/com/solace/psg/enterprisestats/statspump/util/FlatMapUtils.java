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
package com.solace.psg.enterprisestats.statspump.util;

import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.SDTException;
import com.solacesystems.jcsmp.SDTMap;

public class FlatMapUtils {
    public String getFlatKey(String key, int index) {
        return new StringBuilder(key).append(StatsPumpConstants.FLAT_KEY_NUMERIC_INDEX_SEPARATOR).append(index).toString();
    }
    
    public static SDTMap get(SDTMap map, String key) throws SDTException {
        // any key match exactly
        SDTMap returnMap = JCSMPFactory.onlyInstance().createMap();
        if (map.containsKey(key)) {
            returnMap.putObject("",map.get(key));
            return returnMap;
        }
        // else, maybe one or some map keys match by some of the key
        for (String k : map.keySet()) {
            // if k starts with the specified key, it must be shorter than k, so the chatAt() will always work
            if (k.startsWith(key)) {
                if (k.charAt(key.length())==StatsPumpConstants.FLAT_KEY_NUMERIC_INDEX_SEPARATOR) {
                    //System.out.printf("%s = %s%n",k,map.get(k));
                    String index = k.substring(key.length()+1);
                    returnMap.putObject(index,map.get(k));
                } else if (k.charAt(key.length())==StatsPumpConstants.FLAT_KEY_LEVEL_SEPARATOR) {
                    //System.out.printf("%s = %s%n",k,map.get(k));
                    String index = k.substring(key.length()+1);
                    returnMap.putObject(index,map.get(k));
                }
            }
        }
        return returnMap;
    }
    
    // The code below this point can be used for informal unit testing
    
//    public static void main(String... args) throws SDTException {
//        
//        SDTMap map = JCSMPFactory.onlyInstance().createMap();
//        map.putString("object-tags/VPN_NAME","/name");
//        map.putString("object-tags/LOCAL_STATUS","/local-status");
//        map.putString("routers","emea1 & emea2");
//        map.putString("object-tags/ENABLED","/enabled");
//        map.putString("semp-base-tag","/rpc-reply/rpc/show/message-vpn/vpn");
//        map.putString("runs-on-backup-appliance","Only when Backup Virtual Router 'Local Active' (i.e. other appliance has failed over to this one)");
//        map.putFloat("frequency_sec",10f);
//        map.putString("description","Reports back a number of aggregate statistics for a Message VPN");
//        map.putString("topic-levels|4","STATS");
//        map.putString("topic-levels|3","~VPN_NAME~");
//        map.putString("runs-on-primary-appliance","When Primary Virtual Router 'Local Active' (i.e. When this appliance is active, normal operating)");
//        map.putString("topic-levels|0","#STATS");
//        map.putString("object-tags/LOCALLY_CONFIGURED","/locally-configured");
//        map.putString("topic-levels|2","~ROUTER_NAME~");
//        map.putString("poller-name","show message-vpn * stats detail");
//        map.putString("topic-levels|1","VPN");
//        map.putString("test|0/key","aaron");
//        map.putString("test|0/value","man");
//        map.putString("test|1/key","hollie");
//        map.putString("test|1/value","woman");
//        
//        System.out.println(map);
//        System.out.println(get(map,"frequency_sec"));
//        System.out.println(get(map,"object-tags"));
//        System.out.println(get(map,"topic-levels"));
//        SDTMap m = get(map,"test");
//        System.out.println(m);
//        for (int i=0;i<2;i++) {
//            System.out.println(get(m,Integer.toString(i)));
//        }
//        
//    }
    
}
