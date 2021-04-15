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
package com.solace.psg.enterprisestats.statspump.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class DumpInputStream extends FilterInputStream {

    public DumpInputStream(InputStream is) {
        super(is);
    }

    @Override
    public int read() throws IOException {
        int blah = super.read();
        System.out.print((char)blah);
        //System.out.printf("'%c'|0x%x, ",(char)blah,blah);
        return blah;
    }

    @Override
    public int read(byte[] arg0, int arg1, int arg2) throws IOException {
        int blah = super.read(arg0,arg1,arg2);
        for (int i=arg1;i<arg1+blah;i++) {
            System.out.print((char)arg0[i]);
        }
        return blah;
    }
}

