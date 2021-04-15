/**
 * Copyright 2016 Solace Systems, Inc. All rights reserved.
 *
 * http://www.solacesystems.com
 *
 * This source is distributed under the terms and conditions
 * of any contract or contracts between Solace Systems, Inc.
 * ("Solace") and you or your company.
 * If there are no contracts in place use of this source
 * is not authorized.
 * No support is provided and no distribution, sharing with
 * others or re-use of this source is authorized unless
 * specifically stated in the contracts referred to above.
 *
 * This product is provided as is and is not supported
 * by Solace unless such support is provided for under 
 * an agreement signed between you and Solace.
 */
package com.solace.psg.enterprisestats.statspump.tools.comms;

import com.solace.psg.enterprisestats.statspump.tools.SempToolsException;

public class HttpUtilsException extends SempToolsException {

    private static final long serialVersionUID = 1L;

    public HttpUtilsException(String message) {
        super(message);
    }

    public HttpUtilsException(String message,Throwable cause) {
        super(message,cause);
    }

    public static HttpUtilsException factory(String message,Throwable cause) {
        if (cause.getMessage().contains("HTTP response code: 401")) {
            message = "Unauthorized! "+message;
        }
        return new HttpUtilsException(message,cause);
    }
}
