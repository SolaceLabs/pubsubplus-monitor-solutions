package com.solace.psg.enterprisestats.receiver.influxdb.utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.xml.bind.DatatypeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

/**
 * This class implements a thin wrapper around the Apache Client, V4.5. 
 *
 */
public class Apache45Client {
    private static final Logger logger = LoggerFactory.getLogger(Apache45Client.class);
	private static CloseableHttpClient httpClient = null;
	private static RequestConfig requestConfig = null;
	/**
	 * Initializes static elements, including the connection pooling.
	 * @param nConnections
	 */
	public static void initializePool(int nConnections, int nConnectTimeout, int nReadTimeout) {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(nConnections);
        httpClient = HttpClients.custom().setConnectionManager(cm).build();
        
        logger.debug("Apache Http Client V4.5 initialized with connection pool size of " + nConnections);
        
        requestConfig = RequestConfig.custom()
        		.setConnectTimeout(nConnectTimeout)
                .setSocketTimeout(nConnectTimeout)
                .setConnectionRequestTimeout(nReadTimeout)
                .build();
	}

	/** 
	 * Graceful shutdown of the client and connection pool
	 * @throws HttpException
	 */
	public static void shutdown() throws HttpException {
		try {
			if (httpClient != null) {
				httpClient.close();
			}
		} 
		catch (IOException e) {
			throw new HttpException(e);
		}
	}

	/**
	 * Performs Http GET against the specified URL. Credentials are passed in the request header as 
	 * basic authentication.
	 * @param uri
	 * @param username
	 * @param password
	 * @return
	 * @throws HttpException
	 */
	public static String get(String uri, String username, String password) throws HttpException{
		HttpGet httpget = new HttpGet(uri);
		return execute(httpget, username, password);
	}
	
	/**
	 * Performs Http POST against the specified URL, passing the specified body text in the post. Credentials are passed 
	 * in the request header as basic authentication.
	 * @param uri
	 * @param username
	 * @param password
	 * @param body
	 * @return
	 * @throws HttpException
	 */
	public static String post(String uri, String username, String password, String body) throws HttpException {
		HttpPost httpPost = new HttpPost(uri);
	    StringEntity entity;
		try {
			entity = new StringEntity(body);
		} catch (UnsupportedEncodingException e) {
			throw new HttpException(e);
		}
	    httpPost.setEntity(entity);
	    return execute(httpPost, username, password);
	}
	
	/**
	 * Inserts user/pw creds into the request header. Works for any http method.
	 * @param http
	 * @param username
	 * @param password
	 * @throws HttpException
	 */
	private static void addAuthenticationHeader(HttpRequestBase http, String username, String password) throws HttpException {
        String encoded;
		try {
			encoded = DatatypeConverter.printBase64Binary((username + ":" + password).getBytes("UTF-8"));
	        http.addHeader("AUTHORIZATION", "Basic " + encoded);
		} 
		catch (UnsupportedEncodingException e) {
			throw new HttpException(e);
		}
    }
	
	/**
	 * Executes the specified http method, parses the response and returns any response body as a string.
	 * @param method
	 * @param username
	 * @param password
	 * @return
	 * @throws HttpException
	 */
	private static String execute(HttpRequestBase method, String username, String password) throws HttpException {
		if (httpClient == null) {
			throw new HttpException("Temporal coupling exception: call initialiize() before calling post() or get()");
		}
		// set the timeouts that were specified at initialization time
		method.setConfig(requestConfig);
		
		HttpResponse response = null;
        String bodyRc = "";
        int statusCode = 0;
		try {
			addAuthenticationHeader(method, username, password);
		    
			response = httpClient.execute(method);
			statusCode = response.getStatusLine().getStatusCode();

			// round to nearest 100, so that 204s, 205s, etc are all "good"
			statusCode = ((statusCode + 50)/100)*100;
	
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity != null) {
                byte[] bytes = EntityUtils.toByteArray(responseEntity);
                bodyRc = new String (bytes);
            }
        } 
		catch (IOException e ) {
			throw new HttpException(e);
		}
        if (statusCode != 200) {
        	throw new HttpException(bodyRc);
        }
		return bodyRc;
	}
}
