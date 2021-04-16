package com.solacesystems.solgeneos.sample.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Properties;

import com.solacesystems.solgeneos.solgeneosagent.SolGeneosAgent;
import com.solacesystems.solgeneos.solgeneosagent.UserPropertiesConfig;
import com.solacesystems.solgeneos.solgeneosagent.monitor.BaseMonitor;
import com.solacesystems.solgeneos.solgeneosagent.monitor.MonitorConfig;

public class GenericMonitorConfig implements SampleConstants {
	public String m_filename;
	public String m_name;
	public MonitorType m_type = MonitorType.UNKNOWN;
	public String m_netprobe = null;
	public String m_managedEntity = null;
	public String m_managedEntityPrefix = null;
	public String m_sempVersion = null;
	public int m_pageSize = ATTRIBUTE_PAGE_SIZE_DEFAULT;
	public int m_maxObjects = ATTRIBUTE_MAX_OBJECTS_DEFAULT;
	public String m_sempRequest = null;
	public String m_delimiterTopic = null;
	public String m_objectIdTopic = null;
	public String m_objectIdTopicName = null;
	public String m_topics = null;
	public String m_groupHeader = ATTRIBUTE_GROUP_HEADER_DEFAULT;
	public String m_conditionalTopicStr = null;
	public ConditionalTopic m_conditionalTopic = null;
	public String m_parentTopicStr = null;
	public String m_parentTopic = null;
	public String m_parentTopicName = null;
	public String m_vpnSamplerName = null;
	public String m_applianceSamplerName = null;
	public String m_netprobeAddress = null;
	public LinkedHashSet<String> m_colOrRowNames = new LinkedHashSet<String>();
	public MetricFilter<String> m_filter = null;
	
	public GenericMonitorConfig() {
	}
	
	public void loadConfig(BaseMonitor monitor) throws Exception {
		loadGlobalConfig();
		loadMonitorConfig(monitor);
	}
	
	private void loadGlobalConfig() throws Exception {
		Properties props = SolGeneosAgent.onlyInstance.getGlobalProperties();
		m_applianceSamplerName = props.getProperty(SAMPLER_PROPERTY_NAME);
		if(m_applianceSamplerName == null) {
			throw new Exception("Appliance sampler name not specified in global configuration");
		}
		
		m_netprobeAddress = props.getProperty(NETPROBE_PROPERTY_NAME);
		if(m_netprobeAddress == null) {
			throw new Exception("netprobe.np0.endpoint not specified in global configuration");
		}
        
		UserPropertiesConfig userPropsConfig = SolGeneosAgent.onlyInstance.
				getUserPropertiesConfig(USER_PROPERTIES_FILE_NAME);
		if (userPropsConfig != null && userPropsConfig.getProperties() != null) {
			m_netprobe = userPropsConfig.getProperties().getProperty(ATTRIBUTE_NETPROBE_ALIAS);
			if(m_netprobe == null) {
				m_netprobe = ATTRIBUTE_NETPROBE_ALIAS_DEFAULT;
			}
			m_managedEntity = userPropsConfig.getProperties().getProperty(ATTRIBUTE_MANAGED_ENTITY);
			if(m_managedEntity == null) {
				throw new Exception("Managed entity not specified in global configuration");
			}
			m_managedEntityPrefix = userPropsConfig.getProperties().getProperty(ATTRIBUTE_MANAGED_ENTITY_PREFIX);
			if(m_managedEntityPrefix == null) {
				throw new Exception("Managed entity prefix not specified in global configuration");
			}
			m_sempVersion = userPropsConfig.getProperties().getProperty(ATTRIBUTE_SEMP_VERSION);
			if(m_sempVersion == null) {
				throw new Exception("Semp version not specified in global configuration");
			}
			m_vpnSamplerName = userPropsConfig.getProperties().getProperty(ATTRIBUTE_VPN_SAMPLER);
			if(m_vpnSamplerName == null) {
				throw new Exception("VPN sampler name not specified in global configuration");
			}
			if(m_applianceSamplerName.equals(m_vpnSamplerName)) {
				throw new Exception("VPN sampler name cannot be equal to appliance sampler: " + m_vpnSamplerName);
			}
		}
	}
	
	private void loadMonitorConfig(BaseMonitor monitor) throws Exception {
		MonitorConfig monitorConfig = SolGeneosAgent.onlyInstance.getMonitorConfig(monitor);
		
		m_filename = monitorConfig.getFile().getName();
		m_name = m_filename.substring(0, m_filename.lastIndexOf('.'));
		
		String typeStr = monitorConfig.getProperties().getProperty(SampleConstants.ATTRIBUTE_MONITOR_TYPE);
		m_type = MonitorType.getType(typeStr);
		if(m_type == MonitorType.UNKNOWN) {
			//throw new Exception("Invalid monitor type: " +  typeStr + " defined in " + m_filename);
			throw new Exception("Invalid monitor type: " +  typeStr + " (" + m_type + ") defined in " + m_filename);
		}
		
		String groupHeaderStr =  monitorConfig.getProperties().getProperty(SampleConstants.ATTRIBUTE_GROUP_HEADER);
		if(groupHeaderStr != null) {
			m_groupHeader = groupHeaderStr;
		}
		
		String pageSizeStr =  monitorConfig.getProperties().getProperty(SampleConstants.ATTRIBUTE_PAGE_SIZE);
		if(pageSizeStr != null) {
			m_pageSize = Integer.parseInt(pageSizeStr);
		}
		
		String maxObjectsStr =  monitorConfig.getProperties().getProperty(SampleConstants.ATTRIBUTE_MAX_OBJECTS);
		if(maxObjectsStr != null) {
			m_maxObjects = Integer.parseInt(maxObjectsStr);
		}
		
		m_sempRequest = monitorConfig.getProperties().getProperty(SampleConstants.ATTRIBUTE_SEMP_REQUEST);
		if(m_sempRequest == null) {
			throw new Exception("Semp request not defined in " + m_filename);
		}
		m_sempRequest = m_sempRequest.replaceAll(PLACEHOLDER_SEMP_VERSION, m_sempVersion);
		
		if((m_type == MonitorType.VPN_OBJECT_LIST) || (m_type == MonitorType.SYSTEM_OBJECT_LIST)) {
			m_delimiterTopic = monitorConfig.getProperties().getProperty(SampleConstants.ATTRIBUTE_DELIMITER_TOPIC);
			if(m_delimiterTopic == null) {
				throw new Exception("Delimiter topic not defined in " + m_filename);
			}
			m_delimiterTopic = m_delimiterTopic.trim();
			validateTopicFormat(m_delimiterTopic);
			MetricFilter.validateSubscription(m_delimiterTopic);
			
			loadObjectIdTopic(monitor);
			
			loadParentTopic(monitor);
			
			loadConditionalTopic(monitor);
		}
		
		if((m_type == MonitorType.SYSTEM_OBJECT_LIST) || (m_type == MonitorType.SYSTEM_METRIC_LIST)) {
			String viewName = monitorConfig.getProperties().getProperty(SampleConstants.ATTRIBUTE_VIEW_NAME);
			if(viewName == null) {
				throw new Exception("View name not defined in " + m_filename);
			}
		}
		
		loadTopics(monitor);
		
		// Make sure objectIdTopicName and parentTopicName are not among the m_colOrRowNames
		// which are taken from the list of topics
		if(m_colOrRowNames.contains(m_objectIdTopicName)) {
			throw new Exception("ObjectId name: " + m_objectIdTopicName + 
					" is duplicated in topics list: " + m_filename);
		}
		if(m_colOrRowNames.contains(m_parentTopicName)) {
			throw new Exception("Parent name: " + m_parentTopicName + 
					" is duplicated in topics list: " + m_filename);
		}
	}
	
	// List of topics that are used to identify the elements from the SEMP response
	// that will be placed in the Geneos table.  The user has the option to either
	// use the tag name from the SEMP response or define their own name that will be used
	// to name the Geneos column for the matching attribute.  Should a topic
	// match multiple elements in the SEMP response, the user MUST provide a unique name
	// for each of the matching elements.  The order of the provided names will be preserved
	// and applied to the matching elements in the order they are parsed.
	//
	// It is imperitive that there are no duplicate column names.
	//
	// topic list format:  "topic1{columnList},topic2{columnList},...,topicN{columnList}"
	// column list format: ":column1:column2:...:columnN"
	//
	//       - Wildcards are not supported
	//       - A COLON is used as a delimiter between topic and column names and
	//         between each subsequent column name.
	//
	// Example:
	// topics=\
	//       rpc-reply/rpc/show/queue/queues/queue/info/quota,\
	//       rpc-reply/rpc/show/queue/queues/queue/info/num-messages-spooled:num-msgs,\
	//       rpc-reply/rpc/show/queue/queues/queue/info/event/set-value:bind-count:spool-usage:reject-low-priority-limit
	//
	private void loadTopics(BaseMonitor monitor) throws Exception {
		m_filter = new MetricFilter<String>();
		MonitorConfig monitorConfig = SolGeneosAgent.onlyInstance.getMonitorConfig(monitor);
		m_topics = monitorConfig.getProperties().getProperty(SampleConstants.ATTRIBUTE_TOPICS);
		if((m_topics == null) || m_topics.isEmpty()) {
			throw new Exception("Topics not defined in " + m_filename);
		}
		
		String topicsAndNames[] = m_topics.split("\\s*,\\s*");
		for(int i=0; i<topicsAndNames.length; i++) {
			String singleTopicAndOptionalNames[] = topicsAndNames[i].split("\\s*:\\s*");
			String topic = singleTopicAndOptionalNames[0].trim();
			
			// Validate format of topic
			validateTopicFormat(topic);
			
			LinkedList<String> columnOrRowNames = new LinkedList<String>();
			for(int j=1; j<singleTopicAndOptionalNames.length; j++) {
				String name = singleTopicAndOptionalNames[j].trim();
				if(m_colOrRowNames.contains(name)) {
					throw new Exception("Duplicate column/row names not permitted in: " +
							m_filename + " name: " + name + " topic: " + topic);
				}
				columnOrRowNames.add(name);
			}
			
			// If name(s) haven't been provided then use last level of the topic
			if(columnOrRowNames.isEmpty()) {
				String levels[] = topic.split("\\s*/\\s*");
				String name = levels[levels.length - 1];
				if(m_colOrRowNames.contains(name)) {
					throw new Exception("Duplicate column/row names not permitted: " +
							name + " topic: " + topic);
				}
				columnOrRowNames.add(name);
			}
			
			//System.out.println("Adding topic: " + topic + " with columns: " + columnNames);
			m_filter.addSubscription(topic, columnOrRowNames);
			m_colOrRowNames.addAll(columnOrRowNames);
			
		}
	}
	
	private void loadConditionalTopic(BaseMonitor monitor) throws Exception {
		MonitorConfig monitorConfig = SolGeneosAgent.onlyInstance.getMonitorConfig(monitor);
		String conditionalTopicStr = monitorConfig.getProperties().getProperty(SampleConstants.ATTRIBUTE_CONDITIONAL_TOPIC);
		if((conditionalTopicStr == null) || conditionalTopicStr.isEmpty()) {
			return;
		}
		String fields[] = conditionalTopicStr.split("\\s*:\\s*");
		if(fields.length != 3) {
			throw new Exception("Malformed conditional topic: " + conditionalTopicStr);
		}
		validateTopicFormat(fields[0]);
		ConditionalTopic.Operator operator = ConditionalTopic.Operator.getOperator(fields[1]);
		m_conditionalTopic = ConditionalTopic.createConditionalTopic(
				fields[0], operator, fields[2]);
		m_conditionalTopicStr = conditionalTopicStr;
	}
	
	private void loadObjectIdTopic(BaseMonitor monitor) throws Exception {
		MonitorConfig monitorConfig = SolGeneosAgent.onlyInstance.getMonitorConfig(monitor);
		String objectIdTopicStr = monitorConfig.getProperties().getProperty(SampleConstants.ATTRIBUTE_OBJECT_ID_TOPIC);
		if(objectIdTopicStr == null) {
			return;
		}		
		
		String fields[] = objectIdTopicStr.split("\\s*:\\s*");
		if(fields.length > 2) {
			throw new Exception("Malformed object it topic: " + objectIdTopicStr);
		}
		
		String topic = fields[0].trim();
		validateTopicFormat(topic);
		MetricFilter.validateSubscription(topic);
		
		m_objectIdTopic = topic;
		if(fields.length == 2) {
			// metric name provided
			String objectIdTopicName = fields[1].trim();
			if(!objectIdTopicName.isEmpty()) {
				m_objectIdTopicName = objectIdTopicName;
			} 
		}
		
		// if no object id topic name provided use last level of topic
		if(m_objectIdTopicName == null) {
			String topicLevels[] = topic.split("\\s*/\\s*");
			m_objectIdTopicName = topicLevels[topicLevels.length - 1];
		}
	}
	
	private void loadParentTopic(BaseMonitor monitor) throws Exception {
		MonitorConfig monitorConfig = SolGeneosAgent.onlyInstance.getMonitorConfig(monitor);
		String parentTopicStr = monitorConfig.getProperties().getProperty(SampleConstants.ATTRIBUTE_PARENT_TOPIC);
		if(parentTopicStr == null || parentTopicStr.isEmpty()) return;
		
		String fields[] = parentTopicStr.split("\\s*:\\s*");
		if(fields.length > 2) {
			throw new Exception("Malformed parent topic: " + parentTopicStr);
		}
		validateTopicFormat(fields[0]);
		m_parentTopic = fields[0].trim();
		if(fields.length == 2) {
			// metric name provided
			String parentTopicName = fields[1].trim();
			if(!parentTopicName.isEmpty()) {
				m_parentTopicName = parentTopicName;
			} 
		}
		
		// if no parent topic name provided use last level of topic
		if(m_parentTopicName == null) {
			String topicLevels[] = parentTopicStr.split("\\s*/\\s*");
			m_parentTopicName = topicLevels[topicLevels.length - 1];
		}
	}
	
	// Metric Filter also performs some validation; however, additional rules are being imposed.
	// For example, wild cards are not permitted.
	private void validateTopicFormat(String topic) throws Exception {
		if(topic.isEmpty()) {
			throw new Exception("Malformed topic: '" + topic + "' - topic contains only whitespace");
		}
		if(topic.indexOf("*") >= 0) {
			throw new Exception("Malformed topic: '" + topic + "' - topic contains wildcard");
		}
		if(topic.indexOf(">") >= 0) {
			throw new Exception("Malformed topic: '" + topic + "' - topic contains wildcard");
		}
	}

	public String toString() {
		StringBuilder strBuilder = new StringBuilder();
		strBuilder.append("GenericMonitor:\n");
		strBuilder.append("  name:                   " + m_name + "\n");
		strBuilder.append("  type:                   " + m_type + "\n");
		strBuilder.append("  probe:                  " + m_netprobe + "\n");
		strBuilder.append("  managedEntity:          " + m_managedEntity + "\n");
		strBuilder.append("  mePrefix:               " + m_managedEntityPrefix + "\n");
		strBuilder.append("  pageSize:               " + m_pageSize + "\n");
		strBuilder.append("  maxObjects:             " + m_maxObjects + "\n");
		strBuilder.append("  groupHeader             " + m_groupHeader + "\n");
		strBuilder.append("  sempVersion:            " + m_sempVersion + "\n");
		strBuilder.append("  sempRequest:            " + m_sempRequest + "\n");
		strBuilder.append("  delimTopic:             " + m_delimiterTopic + "\n");
		strBuilder.append("  objectIdTopic:          " + m_objectIdTopic + "\n");
		strBuilder.append("  objectIdTopicName:      " + m_objectIdTopicName + "\n");
		strBuilder.append("  conditionalTopic:       " + m_conditionalTopicStr + "\n");
		strBuilder.append("  parentTopic:            " + m_parentTopicStr + "\n");
		strBuilder.append("  parentTopicName:        " + m_parentTopicName + "\n");
		strBuilder.append("  m_vpnSamplerName:       " + m_vpnSamplerName + "\n");
		strBuilder.append("  m_applianceSamplerName: " + m_applianceSamplerName + "\n");
		strBuilder.append("  m_netprobeAddress:      " + m_netprobeAddress + "\n");
		strBuilder.append("  topics:                 " + m_topics + "\n");
		strBuilder.append("  colOrRowNames:          " + m_colOrRowNames + "\n");
		
		return strBuilder.toString();
	}
}
