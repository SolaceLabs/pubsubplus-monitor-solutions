/**
 * Copyright 2016-2017 Solace Corporation. All rights reserved.
 *
 * http://www.solace.com
 *
 * This source is distributed under the terms and conditions of any contract or
 * contracts between Solace Corporation ("Solace") and you or your company. If
 * there are no contracts in place use of this source is not authorized. No
 * support is provided and no distribution, sharing with others or re-use of 
 * this source is authorized unless specifically stated in the contracts 
 * referred to above.
 *
 * This software is custom built to specifications provided by you, and is 
 * provided under a paid service engagement or statement of work signed between
 * you and Solace. This product is provided as is and is not supported by 
 * Solace unless such support is provided for under an agreement signed between
 * you and Solace.
 */
package com.solace.psg.enterprisestats.receiver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solace.psg.enterprisestats.receiver.stats.InternalStats;
import com.solace.psg.enterprisestats.receiver.transport.SolaceTransport;
import com.solace.psg.enterprisestats.receiver.transport.SolaceTransportListener;
import com.solace.psg.enterprisestats.receiver.transport.SolaceTransportToken;
import com.solace.psg.enterprisestats.receiver.utils.ReceiverUtils;
import com.solace.psg.enterprisestats.statspump.LocalMgmtBusListener;
import com.solace.psg.enterprisestats.statspump.StatsPumpException;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.MapMessage;
import com.solacesystems.jcsmp.SDTMap;

/**
 * This class is the main program for the Enterprise stats receiver side
 * framework This class handles the connection to the Solace router, and all
 * subscription logic. It loads an IStatsSpigot implementation by name from the
 * properties file, and calls that implementation for each message received.
 * 
 * This class implements Thread Pooling, so that the spigot only needs to worry
 * about actual writing to the destination.
 */
public class StatsReceiver implements SolaceTransportListener, LocalMgmtBusListener {
	private static final Logger logger = LoggerFactory.getLogger(StatsReceiver.class);

	private static final String SOLACE_TOPICS_PROP_NAME = "SOLACE_TOPICS";
	private static final String SOLACE_QUEUE_PROP_NAME = "SOLACE_QUEUE";
	private static final String THREAD_WORK_QUEUE_SIZE_PROP_NAME = "THREAD_WORK_QUEUE_SIZE";
	private static final String THREAD_POOL_MIN_SIZE_PROP_NAME = "THREAD_POOL_MIN_SIZE";
	private static final String THREAD_POOL_MAX_SIZE_PROP_NAME = "THREAD_POOL_MAX_SIZE";
	private static final String DB_FIELD_SUBSCRIPTIONS_PROP_NAME = "DB_FIELD_SUBSCRIPTIONS";
	private static final String DB_FIELD_FORCE_TO_NUMERIC_PROP_NAME = "DB_FIELD_FORCE_TO_NUMERIC";
	private static final String TAP_PLUGIN_CLASS_PROP_NAME = "TAP_PLUGIN_CLASS";
	private static final String ENABLE_POLLER_STATS_PROP_NAME = "ENABLE_POLLER_STATS";
	private static final String POLLER_BROADCAST_TOPIC_PROP_NAME = "POLLER_BROADCAST_TOPIC";
	private static final String POLLER_STATS_TOPIC_PROP_NAME = "POLLER_STATS_TOPIC";

	private static final String SOLACE_TOPICS_DELIMITER = ";";

	// Solace transport
	private SolaceTransport m_transport = null;

	// Configuration properties
	private StatsReceiverProperties m_properties = null;

	// Internal stats
	private InternalStats m_internalStats = null;;

	private List<String> m_topics = new ArrayList<String>();
	private String m_queueName = "";
	private int m_workQueueSize = 1000;
	private int m_workQueueAlarmThreshold;
	private int m_workQueueClearAlarmThreshold;
	
	private int m_threadMinSize = 3;
	private int m_threadMaxSize = 10;
	private boolean m_enablePollerStats = false;
	private String m_pollerBroadcastTopic = "#STATS/BROADCAST/*/POLLERS";
	private String m_pollerStatsTopic = "#STATS/PUMP/*/POLLER_STAT/>";
	private boolean m_persistentMode = false;
	private DbFilter m_dbFilter = new DbFilter();
	private DbFieldListNonHierarchial m_forcedNumerics = new DbFieldListNonHierarchial();

	// Shutdown flag
	private final static Semaphore _shutdownFlag = new Semaphore(0, true);
	private volatile boolean _receiverStarted = false;

	// The StatsTap implementation
	private StatsTap m_statsTap = null;

	// A worker queue for the executor to enqueue work items (stats msgs) for
	// processing by a thread poll
	private BlockingQueue<Runnable> m_workQueue = null;
	private ExecutorService m_executor = null;

	// For periodic status reporting
	private ScheduledThreadPoolExecutor m_scheduledThreadPoolExecutor;
	private ScheduledFuture<?> m_handle;

	// map from SYSTEM_BLAH to #STATS/~ROUTER_NAME~/TYPE/~BLAH~
	private final ConcurrentMap<String, List<String>> m_pollerInfoMap;

	// map from 'show hostname' to SYSTEM_HOSTNAME
	private final ConcurrentMap<String, String> m_pollerNameMap;

	private AtomicBoolean m_pollerMetadataProcessed = new AtomicBoolean(false);

	public DbFilter getDbFilter() {
		return m_dbFilter;
	}

	public DbFieldListNonHierarchial getForcedNumerics() {
		return m_forcedNumerics;
	}
	public StatsTap getStatsTap() {
		return m_statsTap;
	}

	public boolean isPollerStatsEnabled() {
		return m_enablePollerStats;
	}

	public boolean isPollerMetadataProcessed() {
		return m_pollerMetadataProcessed.get();
	}

	public String getPollerTopicName(String name) {
		return m_pollerNameMap.get(name);
	}

	public List<String> getPollerInfo(String measurementName) {
		return m_pollerInfoMap.get(measurementName);
	}

	public boolean isPersistentMode() {
		return m_persistentMode;
	}

	public boolean hasStarted() {
		return _receiverStarted;
	}

	public StatsReceiver(Map<String, Object> properties) {
		this.m_properties = new StatsReceiverProperties(properties);
		this.m_pollerInfoMap = new ConcurrentHashMap<String, List<String>>();
		this.m_pollerNameMap = new ConcurrentHashMap<String, String>();
		// Internal statistics
		this.m_internalStats = new InternalStats();

		// Register Shutdown hook
		Runnable shutdownHookRunnable = new ReceiverShutdownHookRunnable();
		Runtime.getRuntime().addShutdownHook(new Thread(shutdownHookRunnable, "StatsReciever Shutdown Thread"));
	}

	/**
	 * <p>
	 * Builds and validates configuration properties, initializes the StatsTap
	 * implementation, connects to Solace, and finally subscribes to the
	 * topic/queue destinations to receive stats.
	 * </p>
	 * <p>
	 * This method MAY perform several blocking calls, and returns when the
	 * receiver has successfully started.
	 * </p>
	 * 
	 * @throws ReceiverException
	 */
	public void start(boolean isLocalListener) throws ReceiverException {
		logger.info("Starting Solace Enterprise StatsReceiver...");

		// Build and validate configuration properties
		try {
			buildConfiguration();
		} catch (Exception e) {
			logger.error("Invalid configuration - ensure the given configuration is "
					+ "correct and has all mandatory properties - " + e.getMessage());
			throw new ReceiverException(e);
		}

		// Allow the StatsTap implementation to initialize itself
		try {
			m_statsTap.initialize(m_properties);
		} catch (Exception e) {
			logger.error("Failed to initialize the StatsTap implementation class: " + m_statsTap.getClass().getName());
			throw new ReceiverException(e);
		}

		if (!isLocalListener) {
			// Connect to Solace transport
			m_transport = new SolaceTransport(m_properties, this);
			m_transport.connect();

			// Subscribe to our destination to start consuming stats
			try {
				// Subscribe to topic/queue destination
				if (m_persistentMode) {
					m_transport.subscribe(m_queueName, true);
				} else {
					// Subscribe to broadcast topic
					m_transport.subscribe(m_pollerBroadcastTopic);
					// Subscribe to poller stats if enabled
					if (m_enablePollerStats) {
						m_transport.subscribe(m_pollerStatsTopic);
					}

					for (String topic : m_topics) {
						m_transport.subscribe(topic);
					}
				}
			} catch (ReceiverNotConnectedException e) {
				logger.error("The receiver is not connect to Solace and needs to be restarted. Consider "
						+ "setting reconnect retry properties in configuration to automatically retry the connection.");
				throw e;
			} catch (ReceiverException e) {
				logger.error("Failed to subscribe to required topic/queue destination - check if the "
						+ "receiver has permissions to subscribe to the destination given in the configuration");
				throw e;
			}
		}

		// Schedule status reporting for receiver plugins
		m_scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
		m_handle = m_scheduledThreadPoolExecutor.scheduleAtFixedRate(new Runnable() {
			public void run() {
				try {
					onPeriodicStatusReport();
					m_statsTap.onPeriodicStatusReport();
				} catch (Throwable cause) {
					logger.error("Error calling onPeriodicStatusReport on StatsTap implementation", cause);
				}
			}
		}, 0, 10, TimeUnit.SECONDS);

		// Mark the receiver started
		_receiverStarted = true;
		logger.info("Solace Enterprise StatsReceiver has started and waiting for broadcast messages from Pump.");
	}

	private void onPeriodicStatusReport() {
		int nItemsInQueue = m_workQueue.size();
        logger.info("Internal processing queue currently has  " + nItemsInQueue + " tasks waiting to be processed.");
	}
	
	/**
	 * <p>
	 * Attempts to shutdown the receiver gracefully by disconnecting from Solace
	 * first, then terminating all executing thread tasks, and finally allowing
	 * the receiver plugin to cleanup. Internal statistics are logged before
	 * returning from this call.
	 * </p>
	 */
	public void stop() {
		logger.info("Stopping Solace Enterprise StatsReceiver...");

		// Log internal statistics
		logger.info("# ------------------------------------------------------------");
		logger.info("  StatsReceiver internal statistics summary:");
		logger.info(m_internalStats.toString());
		if (m_statsTap != null) {
			InternalStats pluginStats = m_statsTap.getInternalStats();
			if (pluginStats != null) {
				logger.info("  StatsTap plugin internal statistics summary:");
				logger.info(pluginStats.toString());
			}
		}
		logger.info("# ------------------------------------------------------------");

		try {
			// Disconnect the Solace transport
			if (m_transport != null) {
				logger.info("Disconnecting Solace transport.");
				m_transport.disconnect();
				m_transport = null;
			}

			// Shutdown the executor
			if (m_executor != null) {
				logger.debug("Shutting down thread pool executor");
				try {
					m_executor.shutdown();
					m_executor.awaitTermination(5, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					logger.debug("Interrupted while attempting to orderly shutdown executing task");
				} finally {
					if (!m_executor.isTerminated()) {
						logger.debug("Attempting again to stop all actively executing tasks");
						m_executor.shutdownNow();
					}
					logger.debug("All thread pool executing task have been terminated");
				}
			}

			// Stop the periodic status report
			if (m_handle != null) {
				m_handle.cancel(true);
			}

			// Allow the receiver plugins to perform any cleanup tasks
			if (m_statsTap != null) {
				m_statsTap.cleanup();
			}
		} catch (Throwable cause) {
			logger.info("Unexpected throwable", cause);
		} finally {

		}
		// Release semaphore for main thread to unblock.
		_shutdownFlag.release();
	}

	/**
	 * Updates poller information received from the pump.
	 * 
	 * @param topicName
	 * @param pollerLevels
	 * @param hostname
	 */
	public void processPollerBroadcastMessage(String topicName, List<String> pollerLevels, String hostname) {
		m_pollerInfoMap.put(topicName, pollerLevels);
		// 'show hostname' -> SYSTEM_HOSTNAME
		m_pollerNameMap.put(hostname, topicName);

		if (!isPollerMetadataProcessed()) {
			m_pollerMetadataProcessed.compareAndSet(false, true);
			logger.info("Received poller broadcast messages - starting to process statistics.");
		}
	}

	/**
	 * Increments stats for the provided type.
	 * 
	 * @param statName
	 */
	public void incrementInternalStats(String statName) {
		m_internalStats.incrament(statName);
	}

	private void buildConfiguration() throws ReceiverException {
		// Topics list
		String topics = m_properties.getOptionalStringProperty(SOLACE_TOPICS_PROP_NAME);
		if ((topics != null) && (topics.length() > 0)) {
			m_topics.addAll(Arrays.asList(topics.split(SOLACE_TOPICS_DELIMITER)));
		}
		
		// Poller broadcast topics and stats topic
		String pollerBroadcastTopic = m_properties.getOptionalStringProperty(POLLER_BROADCAST_TOPIC_PROP_NAME);
		if (pollerBroadcastTopic != null && !pollerBroadcastTopic.isEmpty()) {
			m_pollerBroadcastTopic = pollerBroadcastTopic;
		}
		// StatsPump poller stats
		m_enablePollerStats = Boolean
				.parseBoolean((String) m_properties.getOptionalStringProperty(ENABLE_POLLER_STATS_PROP_NAME));
		boolean bUsingDefaultPollerStatsTopic = true;
		if (m_enablePollerStats) {
			// Poller stats are enabled so add override the stats topic if
			// specified or use the default
			String pollerStatsTopic = m_properties.getOptionalStringProperty(POLLER_STATS_TOPIC_PROP_NAME);
			if (pollerStatsTopic != null && !pollerStatsTopic.isEmpty()) {
				m_pollerStatsTopic = pollerStatsTopic;
				bUsingDefaultPollerStatsTopic = false;
			}
			logger.info("Poller stats are enabled - using topic: " + m_pollerStatsTopic);
		}
		// Solace queue to bind
		m_queueName = m_properties.getOptionalStringProperty(SOLACE_QUEUE_PROP_NAME);
		if (m_queueName != null && !m_queueName.isEmpty()) {
			m_persistentMode = true;
			
			logger.info("NOTE: Ignoring configuration property " + SOLACE_TOPICS_PROP_NAME
					+ ". Ensure the Solace queue endpoint '" + m_queueName + "' is mapped with the correct topics, "
					 + "including '" + m_pollerBroadcastTopic + "'.");
			
			if (m_enablePollerStats) {
				String msg = "NOTE: this configuration indicates poller stats are desired. However, since this configuration "
						+ "uses guaranteed messaging, ";
				
				if (bUsingDefaultPollerStatsTopic) {
					msg += "ensure that your have also mapped the " + m_pollerStatsTopic + " onto the " +  m_queueName + " queue.";
				}
				else {
					msg += "and there is a specific value configured in the config file for the " + POLLER_STATS_TOPIC_PROP_NAME
							+ ", ensure you have also mapped the " + m_pollerStatsTopic + " onto the " +  m_queueName + " queue.";
				}
				logger.info(msg);
			}
		}
		else {
			// no queue, a topic better have been specified
			if ((topics == null) || (topics.length() == 0)) {
				String err = "You must have a value specified for either the " + SOLACE_TOPICS_PROP_NAME + " property, or the " +
						SOLACE_QUEUE_PROP_NAME + ". The application cannot start with both settings missing.";
				throw new IllegalArgumentException(err); 
			}
		}
		// Worker thread properties
		String workQueueSize = (String) m_properties.getProperty(THREAD_WORK_QUEUE_SIZE_PROP_NAME);
		if (workQueueSize != null && !workQueueSize.isEmpty()) {
			m_workQueueSize = Integer.parseInt(workQueueSize);
		}
		m_workQueueAlarmThreshold = percentage(m_workQueueSize, 0.8); 
		m_workQueueClearAlarmThreshold = percentage(m_workQueueSize, 0.5); 
		logger.info("Setting internal queue alarm thresholds to " + m_workQueueAlarmThreshold + " (on) and " + m_workQueueClearAlarmThreshold + " (off).");

		String threadMinSize = (String) m_properties.getProperty(THREAD_POOL_MIN_SIZE_PROP_NAME);
		if (threadMinSize != null && !threadMinSize.isEmpty()) {
			m_threadMinSize = Integer.parseInt(threadMinSize);
		}
		else {
			// no value supplied, so stuff teh default value into the app props so that the 
			// tap can have access to it
			m_properties.forcePropertyValue(THREAD_POOL_MIN_SIZE_PROP_NAME, Integer.toString(m_threadMinSize));
		}
		
		String threadMaxSize = (String) m_properties.getProperty(THREAD_POOL_MAX_SIZE_PROP_NAME);
		if (threadMaxSize != null && !threadMaxSize.isEmpty()) {
			m_threadMaxSize = Integer.parseInt(threadMaxSize);
		}
		else {
			// no value supplied, so stuff the default value into the app props so that the 
			// tap can have access to it
			m_properties.forcePropertyValue(THREAD_POOL_MAX_SIZE_PROP_NAME, Integer.toString(m_threadMaxSize));
		}
		
		
		// Initialize the executor engine for multi-threading stats message processing.
		m_workQueue = new ArrayBlockingQueue<Runnable>(m_workQueueSize);
		m_executor = new ThreadPoolExecutor(m_threadMinSize, m_threadMaxSize, 0L, TimeUnit.MILLISECONDS, m_workQueue);

		// Database filter subscriptions
		String dbSubStr = m_properties.getMandatoryStringProperty(DB_FIELD_SUBSCRIPTIONS_PROP_NAME);
		String[] dbSubscriptions = dbSubStr.split(";");
		for (int i = 0; i < dbSubscriptions.length; i++) {
			m_dbFilter.addSubscription(dbSubscriptions[i].trim());
		}
		
		// forced numeric fields - there is some cases in the SempV1 Schema where percentages are returned as strings 
		// with things like %, n/a, etc as values. This list of fields will be stripped and forced to be presented as 
		// numerics to the plug in layer
		String forcedNumericsStr = m_properties.getMandatoryStringProperty(DB_FIELD_FORCE_TO_NUMERIC_PROP_NAME);
		String[] numericsArray = forcedNumericsStr.split(";");
		for (int i = 0; i < numericsArray.length; i++) {
			String field = numericsArray[i].trim();
			
			if (field.length() > 0) {
				this.m_forcedNumerics.add(field);
				logger.debug("Adding '" + field + "' to the list of fields to be forced to numeric.");
			}
		}

		// Load the StatsTap implementation class by name
		String tapClassName = m_properties.getMandatoryStringProperty(TAP_PLUGIN_CLASS_PROP_NAME);
		m_statsTap = ReceiverUtils.statsTapLoader(tapClassName);
		logger.info("StatsReceiver using StatTap implementation class: '" + tapClassName + "'");
	}
	
	private static int percentage(int fullValue, double percent) {
		double temp = (double) fullValue * percent;
		temp = temp + 0.5;
		return (int) Math.floor(temp);
	}

	// //////////////////////////////////////////////////////////////////////
	// Callback Implementations
	//

	@Override
	public void onMgmtBusStats(BytesXMLMessage message) {
		if (!(message instanceof MapMessage))
			return;
		MapMessage rxMessage = (MapMessage) message;
		// Duplicate the message first as the StatsPump will reset this message
		// when we return from this call
		MapMessage newMessage = JCSMPFactory.onlyInstance().createMessage(MapMessage.class);
		newMessage.setProperties(rxMessage.getProperties());
		newMessage.setMap(rxMessage.getMap());
		
		onMessageReceived(newMessage);
	}

	@Override
	public void onPumpStartup() throws StatsPumpException {
		// We are being used as a local mgmt bus listener
		try {
			// Activate the receiver to start collection of stats
			start(true);
		} catch (Exception e) {
			throw new StatsPumpException("Unable to initialize and start StatsReceiver", e);
		}

	}

	@Override
	public void onPumpShutdown() {
		// Attempt to shut down cleanly
		if (hasStarted()) {
			stop();
		}
	}

	@Override
	public void onReconnected() {
		logger.info("Connection to Solace has been re-established.");
	}

	@Override
	public boolean onReconnecting() {
		logger.info("Receiver is attempting to reconnect to Solace.");
		// Return true to continue re-attempting
		return true;
	}

	@Override
	public void onDisconnected() {
		logger.trace("Receiver has been disconnected from Solace");
	}

	@Override
	public void onConnectionLost() {
		logger.warn("Connection to Solace has been lost and is unrecoverable. Requesting receiver to shutdown.");
		requestShutdown();
	}

	@Override
	public void onMessageReceived(BytesXMLMessage message) {
		// if (!message.getDestination().getName().startsWith("#STATS/"))
		// return;
		boolean bSubmitted = false;
		String destination = "?";
		if (message.getDestination() != null) {
			destination = message.getDestination().getName();
		}

		if (!(message instanceof MapMessage)) {
			logger.debug("message recieved on " + destination + " is not a map message and hence will not be processed.");
		}
		else if (destination.contains("~UNINITIALIZED~")) {
			logger.debug("message recieved on " + destination + " contains ~UNINITIALIZED~ data, and hence will not be processed.");
		}
		else {
			MapMessage mapMsg = (MapMessage) message;
			
			boolean bRedelivered = mapMsg.getRedelivered();
			logger.debug("Stats message received on destination (redelivered=" + bRedelivered + ") " + destination);
	
			SDTMap map = mapMsg.getMap();
			if (map.size() == 0) {
				logger.debug("Map message on " + destination + " is of 0 length. It will be ignored.");
			} else {
				int nItemsInQueue = m_workQueue.size();
				logger.debug("There are " + nItemsInQueue + " stats message tasks waiting in the internal queue.");
				
				// Create a new callable task for calling the StatsTap plugin
				TapWorkerThreadedTask tapTask = new TapWorkerThreadedTask(this, mapMsg);
	
				int nRetries = 0;
				int nQuickRetries = 10;
				int nSleepBetweenTries = 100;
				int nOngoingErrorReportingRetries = 100;
				while (!bSubmitted) {
					// Submit the task for execution
					try {
						m_executor.submit(tapTask);
						bSubmitted = true;
						logger.debug("submitted task " + tapTask.getTaskNumber());
					} catch (RejectedExecutionException reject) {
						// Maybe StatsTap implementation is not processing messages
						// fast enough. Put some back pressure on the router since
						// we can't accept any more at the moment
						try {
							// If we have exceeded max retries then shutdown
							// receiver
							if (nRetries == 0) {
								logger.debug("The internal work queue is full, a retry will occur every " + nSleepBetweenTries + 
										"ms to enqueue this task.");
							}
							else if (nRetries == (nQuickRetries + 1)) {
								nSleepBetweenTries = 1000;
								logger.debug("Unable to enqueue stats message in work queue after " + nQuickRetries + 
										" retries. A retry will occur every " + nSleepBetweenTries + "ms to enqueue this task");
							}
							else if ((nRetries % nOngoingErrorReportingRetries) == 0) {
								logger.debug("Unable to enqueue stats message in work queue after " + nRetries + 
										" retries. A retry will occur every " + nSleepBetweenTries + "ms to enqueue this task.");
							}
							Thread.sleep(nSleepBetweenTries);
							nRetries += 1;
						} catch (InterruptedException e) {
							logger.error(e.getMessage());
						}
					}
				}
			}
		}
		if (!bSubmitted) {
            // Acknowledge the message if received from a queue, even though it was not processed. This fixes RT#28629 
			// https://rt.solace.com/rt/Ticket/Display.html?id=28629
            if (isPersistentMode()) {
				logger.debug("This message on " + destination + ", which is ignored, is being immediately ACKed since it is not internally queued.");
            	message.ackMessage();
            }
		}
	}

	@Override
	public void onMessageConfirmed(SolaceTransportToken token) {
		// Nothing to do here as we are not publishing and persistent messages.
	}

	// //////////////////////////////////////////////////////////////////////
	// Static and Main methods
	//

	private static void printUsage() {
		final String appName = System.getProperty("com.solace.psg.enterprisestats.receiver.appname", "stats-receiver");
		System.err.printf("Usage: %s --config <CONFIGFILENAME>%n", appName);
		System.err.println("    <CONFIGFILENAME> is the config file to read\n");
	}

	private static void requestShutdown() {
		_shutdownFlag.release();
	}

	/**
	 * This Runnable will get executed when the JVM shuts down, try to close
	 * things gracefully.
	 */
	public class ReceiverShutdownHookRunnable implements Runnable {

		@Override
		public void run() {
			logger.info("ShutdownHook called... begin stop()");
			requestShutdown();
		}
	}

	/**
	 * Main program launch point.
	 * 
	 * @param args
	 */
	public static void main(String... args) {
		String configFileName = null;
		Map<String, Object> props = null;

		// Check parameters
		if (args == null || args.length == 0) {
			printUsage();
			return;
		}

		for (int i = 0; i < args.length; i++) {
			if ("--config".equals(args[i])) {
				i++;
				configFileName = args[i];
			}
		}

		if (configFileName == null) {
			printUsage();
			return;
		} else {
			try {
				// Load properties from file
				props = ReceiverUtils.loadPropertiesFrom(configFileName);
			} catch (Exception e) {
				logger.error("StatsReceiver initialization error", e);
			}
		}

		if (props != null) {
			StatsReceiver receiver = null;

			try {
				// Create a stats receiver
				receiver = new StatsReceiver(props);

				// Activate the receiver to start collection of stats
				receiver.start(false);

				// Wait until terminated
				try {
					_shutdownFlag.acquire();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} catch (Exception e) {
				logger.error("StatsReceiver got an exception and is shutting down...", e);
			} finally {
				// Attempt to shut down cleanly
				if (receiver != null && receiver.hasStarted()) {
					receiver.stop();
				}
			}
		}

		logger.info("StatsReceiver has stopped");
	}
}
