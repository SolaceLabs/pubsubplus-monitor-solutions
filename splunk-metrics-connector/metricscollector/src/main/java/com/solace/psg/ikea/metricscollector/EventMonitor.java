/**
 * 
 */
package com.solace.psg.ikea.metricscollector;

/**
 * An interface to be implemented by different event monitoring systems.
 * @author VictorTsonkov
 *
 */
public interface EventMonitor
{
	/**
	 * Sends an event payload,
	 * @param eventPayload
	 */
	public void sendEvent(String eventPayload);
}
