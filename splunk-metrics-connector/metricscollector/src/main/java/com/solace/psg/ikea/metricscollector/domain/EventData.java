/**
 * 
 */
package com.solace.psg.ikea.metricscollector.domain;


/**
 * Main class to encapsulate event data.
 * @author VictorTsonkov
 *
 */
public class EventData
{
	private String serviceName;
	private String vpnName;
	private String taskType;
	private long timestamp;
	private Object data;
	
	
	
	/**
	 * Gets task type.
	 * @return the taskType
	 */
	public String getTaskType()
	{
		return taskType;
	}

	/**
	 * Sets task type.
	 * @param taskType the taskType to set
	 */
	public void setTaskType(String taskType)
	{
		this.taskType = taskType;
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
	 * Gets the VPN name.
	 * @return the vpnName
	 */
	public String getVpnName()
	{
		return vpnName;
	}

	/**
	 * Sets the VPN name.
	 * @param vpnName the vpnName to set
	 */
	public void setVpnName(String vpnName)
	{
		this.vpnName = vpnName;
	}
	
	/**
	 * Gets the event data object.
	 * @return the data
	 */
	public Object getData()
	{
		return data;
	}

	public long getTimestamp()
	{
		return this.timestamp;
	}

	/**
	 * Sets the event data object.
	 * @param data the data to set
	 */
	public void setData(Object data)
	{
		this.data = data;
	}

	/**
	 * Initialises a new instance of the class.
	 */
	public EventData(String serviceName, String vpnName, String taskType, Object data)
	{
		this.serviceName = serviceName;
		this.vpnName = vpnName;
		this.taskType = taskType;
		
		this.data = data;
		this.timestamp = System.currentTimeMillis();
	}
}
