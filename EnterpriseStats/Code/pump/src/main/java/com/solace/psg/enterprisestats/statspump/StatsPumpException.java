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

/**
 * Top level exception class for StatsPump.
 */
public class StatsPumpException extends Exception {
	private static final long serialVersionUID = 6297572429677191059L;

	public StatsPumpException(String message) {
		super(message);
	}

	public StatsPumpException(Throwable cause) {
		super(cause);
	}

	public StatsPumpException(String message, Throwable cause) {
		super(message, cause);
	}
}
