package com.solacesystems.solgeneos.sample.util;

import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import com.solacesystems.solgeneos.solgeneosagent.SolGeneosAgent;

public class MonitorUtils implements SampleConstants {
	
	
	public static HashSet<String> getVpnsToMonitor(GenericMonitorConfig monitorConfig, DefaultHttpClient httpClient) throws Exception {
		HashSet<String> vpns = new HashSet<String>();
		
		vpns = SEMPFunctions.getVpns(monitorConfig, httpClient);
		
		HashSet<String> vpnsCopy = new HashSet<String>(vpns);
		for(Iterator<String> iter = vpnsCopy.iterator(); iter.hasNext();) {
			String vpn = iter.next();
			
			if(!validateVpnConfigurationOnGateway(vpn, monitorConfig)) {
				vpns.remove(vpn);
				//System.out.println("Removing VPN:  " + vpn + " (managed entity: " + managedEntity + " not found)");
				continue;
			}
		}
		
		return vpns;
	}
	
	public static DefaultHttpClient createHttpClient() throws GeneralSecurityException {
		
		// retrieve SEMP over HTTP properties from global properties
		Properties props = SolGeneosAgent.onlyInstance.getGlobalProperties();
        String host = props.getProperty(MGMT_IP_ADDRESS_PROPERTY_NAME);
        int port = 80;
        try {
        	port = Integer.parseInt(props.getProperty(MGMT_PORT_PROPERTY_NAME));
        } catch (NumberFormatException e) {
    		System.out.println("createHttpClient:  Invalid port number, using default 80");
        }
        String username = props.getProperty(MGMT_USERNAME_PROPERTY_NAME);
//        String password = props.getProperty(MGMT_PASSWORD_PROPERTY_NAME);
        String password = SolGeneosAgent.onlyInstance.getEncryptedProperty(MGMT_ENCRYPTED_PASSWORD_PROPERTY_NAME, 
        		MGMT_PASSWORD_PROPERTY_NAME);
        
        // create a http client
        DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpParams httpParams = httpClient.getParams();
		
		// set connection properties
	    httpParams.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 15000);
	    httpParams.setParameter(CoreConnectionPNames.TCP_NODELAY, true);
	    httpParams.setParameter(CoreConnectionPNames.SO_TIMEOUT, 180000);
	    httpParams.setParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false);
		
		// set connection target
	    HttpHost target = new HttpHost(host, port);
	    httpParams.setParameter(ClientPNames.DEFAULT_HOST, target);
        
        // set connection credential
	    httpClient.getCredentialsProvider().setCredentials(
	    		new AuthScope(host, port),
                new UsernamePasswordCredentials(username, password));
	    
	    return httpClient;		
	}
	
	public static boolean validateVpnConfigurationOnGateway(String vpn, GenericMonitorConfig monitorConfig) {
		try {
			String managedEntity = monitorConfig.m_managedEntityPrefix + vpn;
			String probeUrl = "http://" + monitorConfig.m_netprobeAddress + "/xmlrpc";
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			config.setServerURL(new URL(probeUrl));
			XmlRpcClient c = new XmlRpcClient();
			c.setConfig(config);
		
			Vector<String> parameters = new Vector<String>();
			parameters.add(managedEntity);
			Boolean result = (Boolean) c.execute("_netprobe.managedEntityExists", parameters);
			return result.booleanValue();
			
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
		
	}

}
