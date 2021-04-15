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

package com.solace.psg.enterprisestats.statspump.tools.semp;

public enum XsdDataType {
    /** A string, just a regular string */
    STRING,
    LONG,
    UNSIGNEDLONG,
    /** A regular 4-byte integer. See XsType.Integer for massively large integer. */
    INT,
    UNSIGNEDINT,
    SHORT,
    UNSIGNEDSHORT,
    BYTE,
    UNSIGNEDBYTE,
    BOOLEAN,
    FLOAT,
    DOUBLE,
    TIME,
    DATE,
    DATETIME,
    /** XML schema type 'integer' has no defined maximum size. Assuming it will be passed as a String, not BigInteger */
    INTEGER,
    /** XML schema type 'decimal' has no defined maximum size. Assuming it will be passed as a String, not BigDecimal */
    DECIMAL,
    UNKNOWN,
    NONNEGATIVEINTEGER,  // new in 7.1
    ;
}


