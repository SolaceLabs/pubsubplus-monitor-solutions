/**
 * 
 */
package com.solace.psg.ikea.metricscollector.splunk;

/**
 * Class to encapsulate a Splunk HEC Event line. 
 * @author VictorTsonkov
 *
 */
public class SplunkEventData
{
	private String sourceType;
	private String index;
	private Object event;

	
	/**
	 * Gets the source type.
	 * @return the sourceType
	 */
	public String getSourceType()
	{
		return sourceType;
	}

	/**
	 * Sets the source type.
	 * @param sourceType the sourceType to set
	 */
	public void setSourceType(String sourceType)
	{
		this.sourceType = sourceType;
	}

	/**
	 * Gets the index.
	 * @return the index
	 */
	public String getIndex()
	{
		return index;
	}

	/**
	 * Sets the index.
	 * @param index the index to set
	 */
	public void setIndex(String index)
	{
		this.index = index;
	}

	/**
	 * Gets the event.
	 * @return the event
	 */
	public Object getEvent()
	{
		return event;
	}

	/**
	 * Sets the event.
	 * @param event the event to set
	 */
	public void setEvent(Object event)
	{
		this.event = event;
	}

	/**
	 * Initialises a new instance of the class.
	 */
	public SplunkEventData(String sourceType, String index, Object event)
	{
		this.sourceType = sourceType;
		this.index = index;
		this.event = event;
	}
}
