/**
 * 
 */
package com.solace.psg.ikea.metricscollector.task;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.solace.psg.ikea.metricscollector.splunk.SplunkEventData;

/**
 * Class to represent a Metrics Collector task
 * @author VictorTsonkov
 *
 */
public abstract class McTask implements Runnable
{
	protected SplunkEventData eventData;
	
	protected Gson gson;
	
	protected boolean enrichedFormat = false; 
	
	/**
	 * Task failed state.
	 */
	public final static int FAILED = -1;
	
	/**
	 * Task not started state.
	 */
	public final static int NOT_STARTED = 0;
	
	/**
	 * Task running state.
	 */
	public final static int RUNNING = 1;
	
	/**
	 * Tasks finished state.
	 */
	public final static int FINISHED = 2;
	
	
	protected int executionState = NOT_STARTED;
	
	
	/**
	 * Gets the execution state of the task.
	 * @return the executionState
	 */
	public int getExecutionState()
	{
		return executionState;
	}

	/**
	 * Gets enriched format value.
	 * @return the enrichedFormat
	 */
	public boolean isEnrichedFormat()
	{
		return enrichedFormat;
	}

	/**
	 * Sets enriched format value.
	 * @param enrichedFormat the enrichedFormat to set
	 */
	public void setEnrichedFormat(boolean enrichedFormat)
	{
		this.enrichedFormat = enrichedFormat;
	}

	/**
	 * Initialises a new instance of the class.
	 */
	public McTask()
	{
		gson = new GsonBuilder().disableHtmlEscaping().create();
	}
	
	/**
	 * Returns result to JSON.
	 * @return
	 */
	public String toJson()
	{
		String result = null;
		
		if (eventData != null)
		{
			result = gson.toJson(eventData);
		}
		
		return result;
	}
}
