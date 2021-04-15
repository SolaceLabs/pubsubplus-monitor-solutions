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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

import org.apache.commons.codec.binary.Base64;

public class SempRequestHttpStream {

    public static final String SOLACE_SEMP_URL = "http://%s/SEMP";

    public static InputStream xxperformStreamRequest(String host, String request, String username, String password) throws SempRequestException {
        HttpURLConnection httpConnection = null;
        try {
            URL urlObj = new URL(String.format(SOLACE_SEMP_URL,host));
            httpConnection = (HttpURLConnection)urlObj.openConnection(); 
            httpConnection.setRequestMethod("POST");
            httpConnection.setDoOutput(true);
            httpConnection.setConnectTimeout(10000);
            httpConnection.setReadTimeout(300000);  // 5 minutes
            if (username != null) {
                String userAndPw = username+":"+password;
                String encoded = new Base64().encodeToString(userAndPw.getBytes());
                httpConnection.setRequestProperty("Authorization","Basic "+encoded);
            }
            httpConnection.connect();
            OutputStreamWriter osWriter = null;
            try {
                if (request != null && !request.isEmpty()) {
                    osWriter = new OutputStreamWriter(httpConnection.getOutputStream(),Charset.forName("UTF-8"));
                    osWriter.write(request);
                    osWriter.flush();
                    osWriter.close();
                }
                return httpConnection.getInputStream();
            } finally {
                try {
                    if (osWriter != null) osWriter.close();
                } catch (IOException e) {}
            }
        } catch (IOException e) {
            httpConnection.getErrorStream();
            throw new SempRequestException("Caught while trying to fetch SEMP via stream",e);
        }
    }
}
