package com.solacesystems.solgeneos.monitor;

import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import com.solacesystems.solgeneos.sample.util.GenericMonitorConfig;
import com.solacesystems.solgeneos.sample.util.MonitorType;
import com.solacesystems.solgeneos.sample.util.MonitorUtils;
import com.solacesystems.solgeneos.sample.util.SampleConstants;
import com.solacesystems.solgeneos.sample.util.SampleHttpSEMPResponse;
import com.solacesystems.solgeneos.sample.util.SampleResponseHandler;
import com.solacesystems.solgeneos.solgeneosagent.NetprobeManagedEntityMapping;
import com.solacesystems.solgeneos.solgeneosagent.monitor.BaseMonitor;
import com.solacesystems.solgeneos.solgeneosagent.monitor.BaseView;
import com.solacesystems.solgeneos.solgeneosagent.monitor.View;

public abstract class GenericMonitor extends BaseMonitor implements SampleConstants {
	
	private DefaultHttpClient m_httpClient;    
    private ResponseHandler<SampleHttpSEMPResponse> m_responseHandler;    
    private GenericSEMPParser m_parser;
    private GenericMonitorConfig m_config;
    
    // maintain my own views as BaseMonitor does not accommodate multiple views with same name, which
    // is required for VPN monitors.
    private HashSet<String> m_localViews;


    @Override
    protected int getSamplingRateFloor() {
    	return 1;
    }
    
	 /**
     * This method is called after initialization but before the monitor is started.
     * 
     * It is a good place to initialize any instance variables.
     */
	@Override
	protected void onPostInitialize() throws Exception {
		// use logger provided by the BaseMonitor class
		if (getLogger().isInfoEnabled()) {
			getLogger().info("Initializing http client");
		}
		
		m_localViews = new HashSet<String>();
		
		m_httpClient = MonitorUtils.createHttpClient();
	    
	    // response handler used http client to process http response and release
	    // associated resources
        m_responseHandler = new SampleResponseHandler();
        
        m_config = new GenericMonitorConfig();
        m_config.loadConfig(this);
		
		getLogger().warn(m_config);
		
		// create SEMP parser
		switch (m_config.m_type) {
		case VPN_OBJECT_LIST:
		case SYSTEM_OBJECT_LIST:
			m_parser = new GenericObjectListSEMPParser(m_config);
			break;
		case VPN_METRIC_LIST:
		case SYSTEM_METRIC_LIST:
			m_parser = new GenericMetricListSEMPParser(m_config);
			break;
		default:
			m_parser = null;
			throw new Exception("Unexpected monitor type: " + m_config.m_type);
		}
		
	}
	
	/**
	 * This method is responsible to collect data required for a view.
	 * @return The next monitor state which should be State.REPORTING_QUEUE.
	 */
	@Override
	protected State onCollect() throws Exception {		
		switch (m_config.m_type) {
		case VPN_OBJECT_LIST:
		case VPN_METRIC_LIST:
			return onCollectVpn();
		case SYSTEM_OBJECT_LIST:
		case SYSTEM_METRIC_LIST:
			return onCollectSystem();
		default:
			return State.REPORTING_QUEUE;
		}
	}	
	
	protected State onCollectVpn() throws Exception {
		

		HashSet<String> vpnsToMonitor = MonitorUtils.getVpnsToMonitor(m_config, m_httpClient);
		
		for (Iterator<String> vpnIt = vpnsToMonitor.iterator(); vpnIt.hasNext();) {
			String vpnName = vpnIt.next();
			String samplerName 		= m_config.m_vpnSamplerName;
			String viewName 		= m_config.m_name;
			String managedEntity 	= m_config.m_managedEntityPrefix + vpnName;
			String viewAlias		= managedEntity + "_" + viewName;
			
			getLogger().info("Monitor: " + m_config.m_name + " VPN: " + vpnName);
			
			//
			// Send SEMP request and invoke parser
			//
			String request = m_config.m_sempRequest;
			request = request.replaceAll(PLACEHOLDER_VPN_NAME, vpnName);
			request = request.replaceAll(PLACEHOLDER_PAGE_SIZE, "" + m_config.m_pageSize);
			Vector<Object> tableContent = processSempRequest(request);
			
			
			
			// Create XmlRpc Client
			String probeUrl = "http://" + m_config.m_netprobeAddress + "/xmlrpc";
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			config.setServerURL(new URL(probeUrl));
			XmlRpcClient c = new XmlRpcClient();
			c.setConfig(config);
			
			//
			// Create new view if not already done.
			//
			boolean isNewView = !m_localViews.contains(viewAlias);
			if(isNewView) {
				getLogger().info("Creating view, alias: " + viewAlias);
				
				try {
					String command = managedEntity + "." + samplerName + ".createView";
					Vector<String> parameters = new Vector<String>();
					parameters.add(viewName);
					parameters.add(m_config.m_groupHeader);
					c.execute(command, parameters);
					
					// Create headlines for object list types only
					if(m_config.m_type == MonitorType.VPN_OBJECT_LIST) {
						command = managedEntity + "." + samplerName + "." + m_config.m_groupHeader + "-" +
								viewName + ".addHeadline";
						parameters.removeElementAt(1);
						parameters.setElementAt("Version:", 0);
						c.execute(command, parameters);

						parameters.setElementAt("Number of objects:", 0);
						c.execute(command, parameters);
						
						parameters.setElementAt("Max objects:", 0);
						c.execute(command, parameters);
						
						parameters.setElementAt("Page size:", 0);
						c.execute(command, parameters);
						
						parameters.setElementAt("Monitor warnings:", 0);
						c.execute(command, parameters);
						
						m_localViews.add(viewAlias);
					} else if(m_config.m_type == MonitorType.VPN_METRIC_LIST) {
						command = managedEntity + "." + samplerName + "." + m_config.m_groupHeader + "-" +
								viewName + ".addHeadline";
						parameters.removeElementAt(1);
						parameters.setElementAt("Version:", 0);
						c.execute(command, parameters);

						parameters.setElementAt("Monitor warnings:", 0);
						c.execute(command, parameters);
					}
				} catch(XmlRpcException e) {
					if(e.code == 301) {// VIEW_EXISTS
						getLogger().info("View already exists in probe: " + viewName);
						m_localViews.add(viewAlias);
					}
					else {
						getLogger().warn("Exception received creating view: " + viewName + e.getMessage());
						//throw e;
					}
				}
				
			}
			
			//
			// Update table
			//
			try {
				String command =  managedEntity + "." + samplerName + "." + m_config.m_groupHeader + "-" +
						viewName + ".updateEntireTable";
				Vector parameters = new Vector();
				parameters.addElement(tableContent);
				c.execute(command, parameters);
				
				// Update headlines for object list types only
				if(m_config.m_type == MonitorType.VPN_OBJECT_LIST) {
				
					int numObjects = 0;
					if(tableContent.size() > 0) {
						numObjects = tableContent.size() - 1;
					}
					
					command =  managedEntity + "." + samplerName + "." + m_config.m_groupHeader + "-" +
							viewName + ".updateHeadline";
					parameters.setElementAt("Version:", 0);
					parameters.addElement(CustomMonitorConstants.VERSION);
					c.execute(command, parameters);

					parameters.setElementAt("Number of objects:", 0);
					parameters.setElementAt(numObjects, 1);
					c.execute(command, parameters);
					
					parameters.setElementAt("Max objects:", 0);
					parameters.setElementAt(m_config.m_maxObjects, 1);
					c.execute(command, parameters);
					
					parameters.setElementAt("Page size:", 0);
					parameters.setElementAt(m_config.m_pageSize, 1);
					c.execute(command, parameters);
					
					parameters.setElementAt("Monitor warnings:", 0);
					parameters.setElementAt(m_parser.getNumWarnings(), 1);
					c.execute(command, parameters);
				} else if(m_config.m_type == MonitorType.VPN_METRIC_LIST) {
					command =  managedEntity + "." + samplerName + "." + m_config.m_groupHeader + "-" +
							viewName + ".updateHeadline";
					parameters.setElementAt("Version:", 0);
					parameters.addElement(CustomMonitorConstants.VERSION);
					c.execute(command, parameters);

					parameters.setElementAt("Monitor warnings:", 0);
					parameters.setElementAt(m_parser.getNumWarnings(), 1);
					c.execute(command, parameters);
				}
			} catch(XmlRpcException e) {
				if(e.code == 302) { // VIEW NOT FOUND
					m_localViews.remove(viewAlias);
					getLogger().warn("View not found during update: " + viewName + e.getMessage());
				} else {
					getLogger().warn("Exception received updating view: " + viewName + e.getMessage());
					//throw e;
				}
			}
		}

		return State.REPORTING_QUEUE;
	}	
	
	protected State onCollectSystem() throws Exception {
		String request = m_config.m_sempRequest;
		request = request.replaceAll(PLACEHOLDER_PAGE_SIZE, "" + m_config.m_pageSize);
		
		//
		// Send SEMP request and invoke parser
		//
		Vector<Object> tableContent = processSempRequest(request);
		
		TreeMap<String, View> viewMap = getViewMap();
		if (viewMap != null && viewMap.size() > 0) {
    		for (Iterator<String> viewIt = viewMap.keySet().iterator(); viewIt.hasNext();) {
    			View view = viewMap.get(viewIt.next());
    			if (view.isActive()) {
    				view.setGroupHeader(m_config.m_groupHeader);
    				view.setTableContent(tableContent);
    				if(m_config.m_type == MonitorType.SYSTEM_OBJECT_LIST) {
    					// Account for column names when computing # objects.
    					int numObjects = 0;
    					if(tableContent.size() > 0) {
    						numObjects = tableContent.size() - 1;
    					}
						LinkedHashMap<String, Object> headlines = new LinkedHashMap<String, Object>();
						headlines.put("Version:", CustomMonitorConstants.VERSION);
						headlines.put("Number of objects:", numObjects);
						headlines.put("Max objects:", m_config.m_maxObjects);
						headlines.put("Page size:", m_config.m_pageSize);
						headlines.put("Monitor warnings:", m_parser.getNumWarnings());
						view.setHeadlines(headlines);
					} else if(m_config.m_type == MonitorType.SYSTEM_METRIC_LIST) {
						LinkedHashMap<String, Object> headlines = new LinkedHashMap<String, Object>();
						headlines.put("Version:", CustomMonitorConstants.VERSION);
						headlines.put("Monitor warnings:", m_parser.getNumWarnings());
						view.setHeadlines(headlines);
					}
    			}
    		}
    	}
		return State.REPORTING_QUEUE;
	}
	
	private Vector<Object> processSempRequest(String request) throws Exception {
		SampleHttpSEMPResponse resp;
		String respBody;
		Vector<Object> tableContent = new Vector<Object>();
		
		getLogger().debug(m_config.m_name + " Request: " + request);
		
		m_parser.setAddColumnNamesToTable(true);
		
		HttpPost post = new HttpPost(HTTP_REQUEST_URI);
		post.setHeader(HEADER_CONTENT_TYPE_UTF8);
		
		while(true) {
			try {
				post.setEntity(new ByteArrayEntity(request.getBytes("UTF-8")));
				
				resp = m_httpClient.execute(post, m_responseHandler);		
		        if (resp.getStatusCode() != 200) {
		        	getLogger().warn("Error with " + m_config.m_name + 
		        			" occurred while sending request, status code: " + 
		        			resp.getStatusCode() + ", reason: " + resp.getReasonPhrase() + 
		        			", request: " + request);
		        	throw new Exception("Error with " + m_config.m_name +
		        			" occurred while sending request, status code: " +
		        			resp.getStatusCode() + ", reason: " + resp.getReasonPhrase() + 
		        			", request: " + request);
		        }	        
		        respBody = resp.getRespBody();
		        getLogger().debug(m_config.m_name + " Response: " + respBody);
		        m_parser.parse(respBody);
		        tableContent.addAll(m_parser.getTableContent());
		        
		        // Make sure we don't exceed maxObjects property
		        // Don't forget the first row in the table are the column
		        // names, don't include them in the total
		        if((m_config.m_type == MonitorType.VPN_OBJECT_LIST) || 
		        	(m_config.m_type == MonitorType.SYSTEM_OBJECT_LIST)) 
		        {
			        if((tableContent.size() - 1) >= m_config.m_maxObjects) {
			        	tableContent.setSize(m_config.m_maxObjects + 1);
			        	break;
			        }
		        }
		        
		        request = m_parser.getMoreCookie();
		        if(request.isEmpty()) {
		        	break;
		        }
		        m_parser.setAddColumnNamesToTable(false);
			} catch(Exception e) {
				e.printStackTrace();
				getLogger().error(e.getMessage());
				throw e;
			}
		}
		return tableContent;
	}
}
