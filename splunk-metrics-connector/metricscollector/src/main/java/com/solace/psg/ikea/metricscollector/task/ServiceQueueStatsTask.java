/**
 * 
 */
package com.solace.psg.ikea.metricscollector.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.solace.psg.ikea.metricscollector.EventMonitor;
import com.solace.psg.ikea.metricscollector.domain.EventData;
import com.solace.psg.ikea.metricscollector.domain.PubSubBroker;
import com.solace.psg.ikea.metricscollector.splunk.SplunkEventData;
import com.solace.psg.sempv1.HttpSempSession;
import com.solace.psg.sempv1.ShowCommands;
import com.solace.psg.sempv1.solacesempreply.RpcReply.Rpc.Show.Queue.Queues;

/**
 * Class to retrieve all queue stats from a service. 
 * @author VictorTsonkov
 *
 */
public class ServiceQueueStatsTask extends McTask
{
	private HttpSempSession session;
	private ShowCommands show;
	private PubSubBroker broker;
	
	private String splunkIndex;
	private String splunkSourceType;
	
	private EventMonitor eventMonitor;
	
	private static final Logger logger = LoggerFactory.getLogger(ServiceQueueStatsTask.class);
	
	/**
	 * Initialises a new instance of the class.
	 * @throws SAXException 
	 */
	public ServiceQueueStatsTask(PubSubBroker broker, EventMonitor eventMonitor) throws SAXException
	{
		if (broker == null)
			throw new NullPointerException("Argument broker cannot be null.");

		if (eventMonitor == null)
			throw new NullPointerException("Argument eventMonitor cannot be null.");
		
		this.eventMonitor = eventMonitor;
		
    	session = new HttpSempSession(broker.getSempUsername(), broker.getSempPassword(), broker.getSempUrl());
    	
    	this.broker = broker;
    	
    	show = new ShowCommands(session);
	}

	/**
	 * Runs the task execution.
	 */
	public void run()
	{
		logger.info("Running queue stats for service: {}", broker.getServiceName());
		
		try
		{
			logger.info("Obtaining queue details using SEMP for service: {}", broker.getServiceName());
			Queues queues = show.getVpnQueueStats(broker.getVpnName());
			if (enrichedFormat)
				eventData = new SplunkEventData(splunkSourceType, splunkIndex, new EventData(broker.getServiceName(), broker.getVpnName(), "QueueStat",  queues));
			else
				eventData = new SplunkEventData(splunkSourceType, splunkIndex, queues);
			
			logger.info("Sending data to event monitor for service: {}", broker.getServiceName());
			eventMonitor.sendEvent(toJson());
			
			//System.out.println("Payload sent to service: " + broker.getServiceName() + ", payload: " + toJson());
		}
		catch (Exception e)
		{
			logger.error("Error occured while obtaining queue stats for service: {}, exception: {}, stack: {}", broker.getServiceName(), e.getMessage(), e.getStackTrace());
		}
	}
}
