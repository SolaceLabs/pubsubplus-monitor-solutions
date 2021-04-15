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

package com.solace.psg.enterprisestats.statspump;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solace.psg.enterprisestats.statspump.config.ConfigLoaderException;
import com.solace.psg.enterprisestats.statspump.config.LocalFileConfigStreamsImpl;


public class StatsPump {
	private static final Logger logger = LoggerFactory.getLogger(StatsPump.class);
	
	public static void main(String... args) {
	    if (args.length < 3) {
	        System.err.println();
	        System.err.println("Usage: StatsPump <poller_config.xml> <poller_group_config.xml> <appliance_config.xml>");
	        System.exit(-1);
	    }
        String pollerFilename = args[0];
        String pollerGroupFilename = args[1];
        String applianceFilename = args[2];

        LocalFileConfigStreamsImpl streams = new LocalFileConfigStreamsImpl(pollerFilename, pollerGroupFilename, applianceFilename);
        PumpManager pump = new PumpManager();
        
        try {
        	pump.configure(streams);
        } catch (ConfigLoaderException e) {
            logger.error("There was an issue loading the configuration files",e);
            logger.error("The system will now exit");
            System.exit(-1);
        }
        
        pump.start();
		try {
			while (true) {
				Thread.sleep(120000);
			}
		} catch (InterruptedException e) {
		    logger.warn("StatsPump main() got interrupted.");
		}
		pump.stop();
	}
}