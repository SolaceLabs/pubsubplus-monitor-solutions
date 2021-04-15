package com.solace.psg.ikea.metricscollector;


import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.solace.psg.ikea.metricscollector.config.ConfigurationManager;
import com.solace.psg.ikea.metricscollector.domain.PubSubBroker;
import com.solace.psg.ikea.metricscollector.splunk.SplunkHec;
import com.solace.psg.ikea.metricscollector.task.ServiceQueueDetailsTask;
import com.solace.psg.ikea.metricscollector.task.ServiceQueueStatsTask;
import com.solace.psg.ikea.metricscollector.utils.FileUtils;


/**
 * The main entry class for the application.
 * @author VictorTsonkov
 *
 */
public class MetricsCollectorApp 
{
	private static final Logger logger = LoggerFactory.getLogger(MetricsCollectorApp.class);
	
	private ConfigurationManager config;
	
	private ArrayList<PubSubBroker> brokers = new ArrayList<PubSubBroker>();
	
	private ScheduledExecutorService executorService = null;
	
	// The number of tasks scheduled for execution.
	private int taskNumber = 2;
	
	// The initial scheduler delay.
	private int initialDelay = 1;
	
	private boolean running = true;

	public static void main( String[] args )
    {
		
		MetricsCollectorApp app = new MetricsCollectorApp();
    	try
    	{
        	app.init();

    		app.run();
    	}
    	catch(Exception ex)
    	{
			logger.error("Exception occured while running app:  {}, stackTrace: {}",  ex.getMessage(), ex.getStackTrace());
			System.exit(-1);
    	}
    	finally
    	{
    		app.exit();
    	}
    }
	
	/**
	 * Stop the main app.
	 */
	public void stop()
	{
		logger.info( "Main app stop invoked." );
		running = false;
	}
	
	/**
	 * Exits the app.
	 */
	private void exit()
	{
		logger.info( "Main app exitting ..." );
		executorService.shutdown();
	}

    /**
     * Runs the end to end copy process.
     * @throws Exception 
     */
    public void run() throws Exception
    {
    	logger.info( "Main app running ..." );
    	for (PubSubBroker broker : brokers)
    	{
    		scheduleBrokerTask(broker);
    	}
    	
    	while(running)
    	{
    		Thread.sleep(1000);
    	}
    }
    
    /**
     * Schedules a single task.
     * @param broker
     * @throws SAXException
     * @throws IOException 
     */
    private void scheduleBrokerTask(PubSubBroker broker) throws SAXException, IOException
    {
    	SplunkHec hec = null;
    	if (config.isSplunkSecured())
    	{
    		String content = FileUtils.readFileToString(config.getSplunkCertificateFile());
    		hec = new SplunkHec(config.getSplunkUrl(), config.getSplunkToken(), config.getSplunkCertificateAlias(),content);
    	}
    	else
    		hec = new SplunkHec(config.getSplunkUrl(), config.getSplunkToken());
    	
    	ServiceQueueStatsTask task1 = new ServiceQueueStatsTask(broker, hec);
    	task1.setEnrichedFormat(config.getEnrichedFormat());

    	ServiceQueueDetailsTask task2 = new ServiceQueueDetailsTask(broker, hec);
    	task2.setEnrichedFormat(config.getEnrichedFormat());
    	
    	executorService.scheduleAtFixedRate(task1, initialDelay, config.getPollingInterval(), TimeUnit.SECONDS);
    	executorService.scheduleAtFixedRate(task2, initialDelay, config.getPollingInterval(), TimeUnit.SECONDS);
    }
        
    /**
     * Init app settings.
     */
    private void init()
    {
       	logger.info( "Main app initializing ..." );
    	
		config = ConfigurationManager.getInstance(); 	
				
		for (int i = 1 ; i <= config.getBrokerCount(); i++)
		{
			PubSubBroker broker = new PubSubBroker(config.getBrokerServiceName(i), config.getBrokerVpnName(i), config.getBrokerUrl(i), config.getBrokerUsername(i), config.getBrokerPassword(i));
			brokers.add(broker);
		}
		
		executorService = Executors.newScheduledThreadPool(config.getBrokerCount()* taskNumber);
    }
}
