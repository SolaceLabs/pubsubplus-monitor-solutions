package com.solace.psg.sempv1;

import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Class to handle a Solace SEMP Session calls over HTTP.
 * @author VictorTsonkov
 *
 */
public class HttpSempSession extends SempSession
{
	private static final Logger logger = LoggerFactory.getLogger(HttpSempSession.class);

	protected CloseableHttpClient client;

	/**
	 * Initialises a new instance of the class.
	 * @param username
	 * @param password
	 * @param sempUri
	 */
	public HttpSempSession(String username, String password, String sempUri)
	{
		this.solaceSEMPURI = sempUri;
		this.solaceSEMPUsername = username;
		this.solaceSEMPPassword = password;
		//this.messageVpn = vpn;
	}

	/**
	 * Initialises a new instance of the class with default vpn and localhost.
	 */
	public HttpSempSession()
	{
	}

	/**
	 * Execute command.
	 * @param commandXML
	 * @return
	 * @throws IOException
	 * @throws AuthenticationException
	 */
	public CloseableHttpResponse execute(String commandXML) throws IOException, AuthenticationException
	{
		String request = solaceSEMPRPCVersion + commandXML; 
		return executeRequest(request);
	}

	/**
	 * Execute command.
	 * @param commandXML
	 * @return
	 * @throws IOException
	 * @throws AuthenticationException
	 */
	public CloseableHttpResponse executeRequest(String commandXML) throws IOException, AuthenticationException
	{
		HttpPost httpPost = new HttpPost(solaceSEMPURI);

		httpPost.setEntity(new StringEntity(commandXML));

		UsernamePasswordCredentials creds = new UsernamePasswordCredentials(solaceSEMPUsername, solaceSEMPPassword);
		httpPost.addHeader(new BasicScheme().authenticate(creds, httpPost, null));

		CloseableHttpResponse response = client.execute(httpPost);

		lastHTTPResponseCode = response.getStatusLine().getStatusCode();

		return client.execute(httpPost);
	}

	/**
	 * Execute command.
	 * @param commandXML
	 * @return
	 * @throws IOException
	 * @throws AuthenticationException
	 */
	public CloseableHttpResponse executeMore(String commandXML) throws IOException, AuthenticationException
	{
		return executeRequest(commandXML);
	}
	
	/**
	 * Reopen HTTP request.
	 */
	public void reopen()
	{
		close();
		open();
	}
	
	/**
	 * Open HTTP Client.
	 */
	public void open()
	{
		client = HttpClients.createDefault();
	}

	/**
	 * Close HTTP client.
	 */
	public synchronized void close()
	{
		if (client != null)
		{
			try
			{
				client.close();
			}
			catch (Exception ex)
			{
				logger.error("Error closing client.");
			}
		}
	}
}
