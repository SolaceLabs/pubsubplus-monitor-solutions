/**
 * 
 */
package com.solace.psg.ikea.metricscollector.splunk;

import com.solace.psg.ikea.metricscollector.EventMonitor;

/**
 * Class to capture the details of a Splunk HEC instance.
 * @author VictorTsonkov
 *
 */
public class SplunkHec implements EventMonitor
{
	private String url;
	private String token;

	
	private SplunkHecHttpClient hecClient;
	
	/**
	 * Initialises a new instance of the class.
	 */
	public SplunkHec(String url, String token, String certificateAlias, String certificateContent)
	{
		this.url = url;
		this.token = token;
				
		hecClient = new SplunkHecHttpClient(url, token, certificateAlias, certificateContent);	
	}
	
	/**
	 * Initialises a new instance of the class.
	 */
	public SplunkHec(String url, String token)
	{
		this.url = url;
		this.token = token;
				
		hecClient = new SplunkHecHttpClient(url, token);	
	}

	/**
	 * Gets the Splunk HEC URL.
	 * @return the url
	 */
	public String getUrl()
	{
		return url;
	}

	/**
	 * Sets the Splunk HEC URL.
	 * @param url the url to set
	 */
	public void setUrl(String url)
	{
		this.url = url;
	}

	/**
	 * Gets the HEC token.
	 * @return the token
	 */
	public String getToken()
	{
		return token;
	}

	/**
	 * Sets the HEC token.
	 * @param token the token to set
	 */
	public void setToken(String token)
	{
		this.token = token;
	}
	
	/**
	 * Sends a Splunk Event payload.
	 * @param eventPayload
	 */
	public void sendEvent(String eventPayload)
	{
		hecClient.sendEvent(eventPayload);
	}
}
