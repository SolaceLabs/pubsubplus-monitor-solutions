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

package com.solace.psg.enterprisestats.statspump.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solace.psg.enterprisestats.statspump.PollerRunnable;

public class NonXmlBufferedReader extends BufferedReader {

    private int charCount = 0;
    private long nanoStart = 0;
    private final PollerRunnable poller;
    public static final char REPLACEMENT_CHAR = (char)0xfffc;
    private static final Logger logger = LoggerFactory.getLogger(NonXmlBufferedReader.class);
    

    public NonXmlBufferedReader(Reader in, PollerRunnable pr) {
        super(in);
        poller = pr;
    }
    
    public NonXmlBufferedReader(Reader in, int sz, PollerRunnable pr) {
        super(in,sz);
        poller = pr;
    }

    @Override
    public int read() throws IOException {
        logger.warn("*********************************************** SINGLE READ() during stream from "+poller.toString());
        int c = super.read();
        if (nanoStart == 0) nanoStart = System.nanoTime();  // first char received
        //      if ($semp =~ /[^\x09\x0A\x0D\x20-\x{D7FF}\x{E000}-\x{FFFD}\x{10000}-\x{10FFFF}]/) {
        // changed the order, did usual alpha chars first, followed by LF, then CR
        if ((c>=0x20 && c<=0xdf77) || c==0xa || c==0xd || c==0x9 || (c>=0xe000 && c<=0xfffd) || (c>=0x10000 && c<=0x10ffff)) {
            charCount++;
            return c;
        } else if (c < 0) {  // i.e. -1
            return c;
        } else {
            logger.info(String.format("Single read!!  Encountered a non-valid charcter: 0x%x",c));
            charCount++;
            return REPLACEMENT_CHAR;
        }
    }
    
    enum DumpFormat {
        HEX,
        TEXT,
    }
    
    private String dump2(char[] cbuf, int off, int len, int loc, DumpFormat df, int range) {
        StringBuilder sb = new StringBuilder();
        for (int i=Math.max(loc-range,off);i<Math.min(loc+range,off+len);i++) {
            if (df == DumpFormat.TEXT) {
                if (i == loc) {  // i.e. the problem char... highlight!
                    sb.append(String.format("[[%c]]",cbuf[i]));
                } else {
                    sb.append(cbuf[i]);
                }
            } else if (df == DumpFormat.HEX) {
                if (cbuf[i] == ' ') continue;
                if (cbuf[i] == 0x000a) {
                    sb.append("\n");
                    continue;
                }
                if (i == loc) {  // i.e. the problem char... highlight!
                    sb.append(String.format("[[%c|0x%x]] ",cbuf[i],(int)cbuf[i]));
                } else {
                    sb.append(String.format("%c|0x%x, ",cbuf[i],(int)cbuf[i]));
                }
            }
        }
        return sb.append(String.format("%n")).toString();
    }


    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        // seems to only be this one that ever gets read
        int charsRead = super.read(cbuf, off, len);
        if (charsRead > 0) {
            //System.out.println(charsRead);
            if (nanoStart == 0) nanoStart = System.nanoTime();  // first char received
            charCount += charsRead;
            for (int i=off;i<off+charsRead;i++) {
                if (!((cbuf[i]>=0x20 && cbuf[i]<=0xdf77) || cbuf[i]==0xa || cbuf[i]==0xd || cbuf[i]==0x9 || (cbuf[i]>=0xe000 && cbuf[i]<=0xfffc) || (cbuf[i]>=0x10000 && cbuf[i]<=0x10ffff))) {
                    StringBuilder sb = new StringBuilder();
                    if (cbuf[i] == 0xfffd) {
                        // Probably replaced a non-Utf8 char before
                        sb.append(String.format("Found a non-UTF8 char at position %d during %s, surrounded by [[ ]]. (Has already been replaced).%n",i,poller));
                        sb.append("\"... ").append(dump2(cbuf,off,charsRead,i,DumpFormat.TEXT,100)).append(" ...\"");
                        sb.append(dump2(cbuf,off,charsRead,i,DumpFormat.HEX,20));
                        logger.info(sb.toString());
                        // then do nothing as we've already replaced it and it's technically a valid XML UTF-8 char
                    } else {
                        sb.append(String.format("Found an out-of-range (i.e. non-XML, but vaild UTF-8) char at position %d during %s, surrounded by [[ ]].%n",i,poller));
                        sb.append("\"... ").append(dump2(cbuf,off,charsRead,i,DumpFormat.TEXT,100)).append(" ...\"");
                        sb.append(dump2(cbuf,off,charsRead,i,DumpFormat.HEX,20));
                        logger.info(sb.toString());
                        cbuf[i] = REPLACEMENT_CHAR;
                    }
                }
            }
        }
        return charsRead;
    }

    @Override
    public String readLine() throws IOException {
        System.out.println("*********************** READLINE() *******************************");
        return super.readLine();
    }
    
    public int getCharCount() {
        return charCount;
    }
    
    public long getNanoStart() {
        return nanoStart;
    }
}
