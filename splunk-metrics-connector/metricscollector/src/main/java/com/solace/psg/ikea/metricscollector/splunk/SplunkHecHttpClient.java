/**
 * 
 */
package com.solace.psg.ikea.metricscollector.splunk;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solace.psg.sempv1.HostnameVerification;
import com.solace.psg.ikea.metricscollector.utils.FileUtils;
import com.solace.psg.sempv1.CustomTrustStrategy;


/**
 * Class to handle the HTTP calls to Splunk HEC via HTTP.
 * @author VictorTsonkov
 *
 */
public class SplunkHecHttpClient
{
	private String urlString;
	private String token;
	
	private String certificateContent = null;
	private String certificateAlias = null;
	
	private boolean secured = false;
	
	private static final Logger logger = LoggerFactory.getLogger(SplunkHecHttpClient.class);
	
	/**
	 * Initialises a new instance of the class.
	 */
	public SplunkHecHttpClient(String url, String token, String certificateAlias, String certificateContent )
	{
		logger.info("Initialising SplunkHecHttpClient instance ...");
		this.urlString = url;
		this.token = token;
		this.certificateAlias = certificateAlias;
		this.certificateContent = certificateContent;
		secured = true;
	}
	
	/**
	 * Initialises a new instance of the class.
	 */
	public SplunkHecHttpClient(String url, String token)
	{
		logger.info("Initialising SplunkHecHttpClient instance ...");
		this.urlString = url;
		this.token = token;
	}

	
	/**
	 * Sending an event to Splunk HEC.
	 * 
	 * @param payload the JSON payload to be sent. 
	 * @throws UnsupportedOperationException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	public void sendEvent(String payload) 
	{
		logger.info("Sending event payload to HEC ...");
		HttpResponse response = null;

		SSLConnectionSocketFactory sslsf = null;
		
		try
		{
			CertificateFactory cf = CertificateFactory.getInstance("X.509");

			KeyStore clientTrustStore = null;
			clientTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());//KeyStore.getInstance("JKS");
			clientTrustStore.load(null, null);
	
			BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(certificateContent.getBytes()));
			
			Certificate certificate = null;
		    while (bis.available() > 0) 
		    {
		    	certificate = cf.generateCertificate(bis);
		    }
		    
			clientTrustStore.setCertificateEntry(certificateAlias, certificate);	
		
			SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(clientTrustStore, new CustomTrustStrategy()).build();
					
			sslsf = new SSLConnectionSocketFactory(sslContext, new String[]
			{ "TLSv1.2" }, null, getHostnameVerifier(HostnameVerification.NONE));
		
		}
		catch (CertificateException | KeyStoreException | NoSuchAlgorithmException | IOException | KeyManagementException e)
		{
			logger.error("Exception was thrown while trying to send event to Splunk HEC: {}, stack: {}", e.getMessage(), e.getStackTrace());		
		}
				
		CloseableHttpClient httpClient = null;
		try 
		{
			if (secured)
				httpClient = HttpClients.custom().setSSLSocketFactory(sslsf).setSSLHostnameVerifier( NoopHostnameVerifier.INSTANCE).build();
			else
				httpClient =  HttpClientBuilder.create().build();
			
			HttpPost httpPost = new HttpPost(urlString);
			httpPost.addHeader("Authorization", " Splunk " + token);
			
			logger.debug("Payload: {}", payload);
			
			httpPost.setEntity(new StringEntity(payload));
			response = httpClient.execute(httpPost);
			StatusLine statusLine = response.getStatusLine();

			String responseString = "";

			try (ByteArrayOutputStream out = new ByteArrayOutputStream())
			{
				response.getEntity().writeTo(out);
				responseString = out.toString();
			}

			if (statusLine.getStatusCode() == HttpStatus.SC_OK)
			{
				logger.debug("Payload: {}", responseString);
			}
			else
			{
				logger.error("Response contained unsuccessful code: {} and response: {}", statusLine.getStatusCode(), responseString);
			}
		} 
		catch (IOException  e) 
		{
			logger.error("Exception was thrown while trying to send event to Splunk HEC: {}, stack: {}", e.getMessage(), e.getStackTrace());		
		}
		finally
		{	
			if (response != null)
			{
				try
				{
					logger.debug("Closing Splunk response object...");
					response.getEntity().getContent().close();
				}
				catch (UnsupportedOperationException | IOException e)
				{
					logger.error("Exception was thrown while trying to close response: {}, stack: {}", e.getMessage(), e.getStackTrace());		
				}
			}
			
			if (httpClient != null)
			{
				try
				{
					httpClient.close();
				}
				catch (IOException e)
				{
					logger.error("Exception was thrown while trying to close httpClient: {}, stack: {}", e.getMessage(), e.getStackTrace());		
				}
			}
		}
	}
	
	private HostnameVerifier getHostnameVerifier(HostnameVerification verification)
	{
		if (verification == null)
		{
			verification = HostnameVerification.STRICT;
		}
		switch (verification)
		{
		case STRICT:
			return SSLConnectionSocketFactory.getDefaultHostnameVerifier();
		case NONE:
			return NoopHostnameVerifier.INSTANCE;
		default:
			throw new IllegalArgumentException("Unhandled HostnameVerification: " + verification);
		}
	}
}
