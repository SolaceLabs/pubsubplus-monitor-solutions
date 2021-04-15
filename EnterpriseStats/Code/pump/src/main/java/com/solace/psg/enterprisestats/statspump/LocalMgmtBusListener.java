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

import com.solacesystems.jcsmp.BytesXMLMessage;

/**
 * An simple interface to receive management bus statistics messages directly
 * from StatsPump, and by-passing the Solace Message Router.
 */
public interface LocalMgmtBusListener {

	/**
	 * Called when the StatsPump is starting so the listener can start and
	 * initialize.
	 * 
	 * @throws StatsPumpException
	 */
	void onPumpStartup() throws StatsPumpException;

	/**
	 * Called when the StatsPump is shutting down so the listener can stop and
	 * cleanup/release resources.
	 */
	void onPumpShutdown();

	/**
	 * Called when the StatsPump publishes a stats message for management
	 * message bus.
	 * 
	 * @param message
	 *            The Solace BytesXMLMessage as if it was received from a Solace
	 *            Message Router.
	 */
	void onMgmtBusStats(BytesXMLMessage message);
}
