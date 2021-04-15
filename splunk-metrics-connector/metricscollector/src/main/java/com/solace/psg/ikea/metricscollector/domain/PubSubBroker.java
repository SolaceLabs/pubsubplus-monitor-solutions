package com.solace.psg.ikea.metricscollector.domain;


/**
 * Class to encapsulate properties of a Solace PubSub Broker.
 * 
 * @author VictorTsonkov
 *
 */
public class PubSubBroker
{
	private String serviceName;
	private String vpnName;
	private String sempUrl;
	private String sempUsername;
	private String sempPassword;
	
	/**
	 * Initialises a new instance of the class.
	 */
	public PubSubBroker(String serviceName, String vpnName, String sempUrl, String sempUsername, String sempPassword)
	{
		this.sempUsername = sempUsername;
		this.sempPassword = sempPassword;
		this.serviceName = serviceName;
		this.vpnName = vpnName;
		this.sempUrl = sempUrl;		
	}
	
	/**
	 * Gets the VPN name.
	 * @return the serviceName
	 */
	public String getVpnName()
	{
		return vpnName;
	}

	/**
	 * Sets the VPN name.
	 * @param serviceName the serviceName to set
	 */
	public void setVpnName(String vpnName)
	{
		this.vpnName = vpnName;
	}

	/**
	 * Gets the service name.
	 * @return the serviceName
	 */
	public String getServiceName()
	{
		return serviceName;
	}

	/**
	 * Sets the service name.
	 * @param serviceName the serviceName to set
	 */
	public void setServiceName(String serviceName)
	{
		this.serviceName = serviceName;
	}

	/**
	 * Gets the SEMP user name.
	 * @return the sempUsername
	 */
	public String getSempUsername()
	{
		return sempUsername;
	}

	/**
	 * Sets the SEMP user name.
	 * @param sempUsername the sempUsername to set
	 */
	public void setSempUsername(String sempUsername)
	{
		this.sempUsername = sempUsername;
	}

	/**
	 * Gets the SEMP password.
	 * @return the sempPassword
	 */
	public String getSempPassword()
	{
		return sempPassword;
	}

	/**
	 * Sets the SEMP password.
	 * @param sempPassword the sempPassword to set
	 */
	public void setSempPassword(String sempPassword)
	{
		this.sempPassword = sempPassword;
	}

	/**
	 * Gets SEMP URL.
	 * @return the sempUrl
	 */
	public String getSempUrl()
	{
		return sempUrl;
	}

	/**
	 * Sets SEMP Url
	 * @param sempUrl the sempUrl to set
	 */
	public void setSempUrl(String sempUrl)
	{
		this.sempUrl = sempUrl;
	}
}
