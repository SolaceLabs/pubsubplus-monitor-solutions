package com.solacesystems.solgeneos.sample.util;

import java.io.StringReader;
import java.util.HashSet;
import java.util.Properties;

import javax.xml.xpath.XPathConstants;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.w3c.dom.Node;

import com.solacesystems.solgeneos.solgeneosagent.SolGeneosAgent;


public class SEMPFunctions implements SampleConstants {
	
	static final public String SHOW_VPN_REQUEST = 
            "<rpc semp-version=\"" + PLACEHOLDER_SEMP_VERSION + "\">\n" +
            "    <show>\n" +
            "        <message-vpn>\n" +
            "            <vpn-name>*</vpn-name>\n" +
            "            <count/>\n" +
            "            <num-elements>100</num-elements>\n" +
            "        </message-vpn>\n" +
            "    </show>\n" +
            "</rpc>\n";

	public static HashSet<String> getVpns(GenericMonitorConfig monitorConfig, DefaultHttpClient httpClient) throws Exception {
		ResponseHandler<SampleHttpSEMPResponse> m_responseHandler = new SampleResponseHandler();;    
	    VpnListSEMPParser parser = new VpnListSEMPParser();
		HashSet<String> vpnList = new HashSet<String>();
		SampleHttpSEMPResponse resp;
		String respBody;
		
		HttpPost post = new HttpPost(HTTP_REQUEST_URI);
		post.setHeader(HEADER_CONTENT_TYPE_UTF8);
		String request = SHOW_VPN_REQUEST;
		request = request.replaceAll(SampleConstants.PLACEHOLDER_SEMP_VERSION, monitorConfig.m_sempVersion);
		
		while(true) {
			post.setEntity(new ByteArrayEntity(request.getBytes("UTF-8")));
			
			resp = httpClient.execute(post, m_responseHandler);		
	        if (resp.getStatusCode() != 200) {
	        	throw new Exception("Error occurred while sending request: " + resp.getStatusCode() 
	        			+ " - " + resp.getReasonPhrase());
	        }	        
	        respBody = resp.getRespBody();
	        
	        parser.parse(respBody);
	        vpnList.addAll(parser.getVpns());
	        request = parser.getMoreCookie();
	        if(request.isEmpty()) {
	        	break;
	        }
		}		
		
		return vpnList;
	}


}
