/**
 * 
 */
package com.solace.psg.sempv1;

import java.io.IOException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.apache.http.auth.AuthenticationException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solace.psg.sempv1.solacesempreply.ObjectFactory;

/**
 * Abstract SEMP Session class. 
 * 
 * @author VictorTsonkov
 *
 */
public abstract class SempSession
{
	private static final Logger logger = LoggerFactory.getLogger(SempSession.class);
	
	protected String solaceSEMPURI = "http://localhost:8080/SEMP";

	protected String solaceSEMPUsername = "admin";;

	protected String solaceSEMPPassword = "admin";

	protected String solaceSEMPRPCVersion = "<rpc>";

	protected int lastHTTPResponseCode;
	
	protected JAXBContext rpcReplyContext = null;

	//protected String messageVpn = "default";
	
	public abstract CloseableHttpResponse execute(String commandXML) throws IOException, AuthenticationException;

	public abstract CloseableHttpResponse executeRequest(String commandXML) throws IOException, AuthenticationException;
	
	public abstract CloseableHttpResponse executeMore(String commandXML) throws IOException, AuthenticationException;

	public abstract void open();
	
	public abstract void reopen();

	public abstract void close();

	/**
	 * Gets SEMP URI.
	 * @return
	 */
	public String getSolaceSEMPURI()
	{
		return solaceSEMPURI;
	}

	/**
	 * Sets SEMP URI.
	 * @param solaceSEMPURI
	 */
	public void setSolaceSEMPURI(String solaceSEMPURI)
	{
		this.solaceSEMPURI = solaceSEMPURI;
	}

	/**
	 * Gets username.
	 * @return
	 */
	public String getSolaceSEMPUsername()
	{
		return solaceSEMPUsername;
	}

	/**
	 * Sets username.
	 * @param solaceSEMPUsername
	 */
	public void setSolaceSEMPUsername(String solaceSEMPUsername)
	{
		this.solaceSEMPUsername = solaceSEMPUsername;
	}

	/**
	 * Gets password.
	 * @return
	 */
	public String getSolaceSEMPPassword()
	{
		return solaceSEMPPassword;
	}

	/**
	 * Sets password.
	 * @param solaceSEMPPassword
	 */
	public void setSolaceSEMPPassword(String solaceSEMPPassword)
	{
		this.solaceSEMPPassword = solaceSEMPPassword;
	}

	/**
	 * Gets RPC Version.
	 * @return
	 */
	public String getSolaceSEMPRPCVersion()
	{
		return solaceSEMPRPCVersion;
	}

	/**
	 * Sets RPC Version.
	 * @param solaceSEMPRPCVersion
	 */
	public void setSolaceSEMPRPCVersion(String solaceSEMPRPCVersion)
	{
		this.solaceSEMPRPCVersion = solaceSEMPRPCVersion;
	}

	/**
	 * Initialises a new instance of the class.
	 */
	public SempSession()
	{
	}

	/**
	 * Gets last HTTP response code.
	 * @return
	 */
	public int getLastHTTPResponseCode()
	{
		return lastHTTPResponseCode;
	}
	
	/**
	 * Get RPC Context.
	 * @return
	 */
	public JAXBContext getRpcReplyContext()
	{
		if (rpcReplyContext == null)
		{
			try
			{
				rpcReplyContext = JAXBContext.newInstance(ObjectFactory.class);
			}
			catch (JAXBException e)
			{
				logger.error("Could not create RPCReply JAXBContext");
			}
		}

		return rpcReplyContext;
	}

	/**
	 * Sets Rpc Reply Context
	 * @param rpcReplyContext
	 */	
	public void setRpcReplyContext(JAXBContext rpcReplyContext)
	{
		this.rpcReplyContext = rpcReplyContext;
	}
	/**
	 * Gets Message VPN.
	 * @return
	 *
	public String getMessageVpn()
	{
		return messageVpn;
	}*/

	/**
	 * Sets Message VPN.
	 * @param messageVpn
	 *
	public void setMessageVpn(String messageVpn)
	{
		this.messageVpn = messageVpn;
	}*/
}
