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

package com.solace.psg.enterprisestats.statspump.tools.comms;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Scanner;

import org.apache.commons.codec.binary.Base64;

/**
 * This class contains some useful methods for performing SEMP HTTP requests,
 * as well as other types of requests (GET, POST).
 * 
 * @author Aaron Lee
 */
public final class HttpUtils {
    
    public enum RequestMethod {
        GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS, TRACE;
    }
    
    /**
     * A Singleton reference to the UTF-8 charset.
     */
    public static final Charset UTF8 = Charset.forName("UTF-8");
    
    /**
     * The SEMP (version 1) URL for performing a SEMP HTTP POST request
     */
    public static final String SOLACE_SEMP_URL = "http://%s/SEMP";
    public static final String SOLACE_SECURE_SEMP_URL = "https://%s/SEMP";
    
    public static String performSempStringRequest(String host, String username, String password, String sempRequest) throws SempRequestException {
        return streamToString(performSempStreamRequest(host,username,password,sempRequest));
    }
    
    public static InputStream performSempStreamRequest(String host, String username, String password, String sempRequest) throws SempRequestException {
        return httpRequestStream(String.format(SOLACE_SEMP_URL,host),username,password,RequestMethod.POST,sempRequest);
    }
    
    public static InputStream performSempStreamRequest(String host, boolean useSecureSession, String username, String password, String sempRequest) throws SempRequestException {
        return httpRequestStream(String.format(useSecureSession ? SOLACE_SECURE_SEMP_URL : SOLACE_SEMP_URL,host),username,password,RequestMethod.POST,sempRequest);
    }
    
    public static String httpRequestString(String url, String username, String password, RequestMethod requestMethod, String body) throws SempRequestException {
        return streamToString(httpRequestStream(url,username,password,requestMethod,body));
    }
    
    /**
     * 
     * @param url
     * @param username
     * @param password
     * @param requestMethod - must be GET, POST, or other RequestMethod
     * @param body - could be null
     * @return
     * @throws SempRequestException
     */
    public static InputStream httpRequestStream(String url, String username, String password, RequestMethod requestMethod, String body) throws SempRequestException {
        HttpURLConnection httpConnection = null;
        try {
            URL urlObj = new URL(url);
            httpConnection = (HttpURLConnection)urlObj.openConnection(); 
            httpConnection.setRequestMethod(requestMethod.name());
            httpConnection.setDoOutput(true);
            httpConnection.setConnectTimeout(10000);
            httpConnection.setReadTimeout(300000);  // 5 minutes
            if (username != null && !"authType".equals("basic")) {  // obv. haven't implemented the other type yet
                String userAndPw = username+":"+password;
                String encoded = new Base64().encodeToString(userAndPw.getBytes());
                httpConnection.setRequestProperty("Authorization","Basic "+encoded);
            }
            httpConnection.connect();
            OutputStreamWriter osWriter = null;
            try {
                if (body != null && !body.isEmpty()) {
                    osWriter = new OutputStreamWriter(httpConnection.getOutputStream(),UTF8);
                    osWriter.write(body);
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
    
    private static String streamToString(InputStream stream) {
        Reader reader = new InputStreamReader(new BufferedInputStream(stream),UTF8);
        Scanner scanner = null;
        // saw this method for efficiently reading a Stream to String from StackOverflow... 
        // time delta between streaming vs. not is very small now
        try {
            scanner = new Scanner(reader);
            scanner.useDelimiter("\\A");  // scan until "beginning of input boundary"
            String reply = scanner.hasNext() ? scanner.next() : "";
            return reply;
        } finally {
            scanner.close();
        }
    }
    

    private HttpUtils() {
        // can't instantiate
        throw new AssertionError("Not allowed to instantiate this class");
    }
}
