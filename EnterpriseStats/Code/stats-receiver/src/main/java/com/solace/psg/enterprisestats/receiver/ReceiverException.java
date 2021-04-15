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

/**
 * Simple exception, typed for this package
 */
public class ReceiverException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Construct with a string
     */
    public ReceiverException(String message) {
        super(message);
    }

    /**
     * Wraps another exception
     */
    public ReceiverException(Exception exception) {
        super(exception);
    }

    public ReceiverException(String message, Throwable cause) {
        super(message, cause);
    }
}