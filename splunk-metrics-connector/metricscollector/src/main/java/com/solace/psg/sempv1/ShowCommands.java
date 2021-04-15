package com.solace.psg.sempv1;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.auth.AuthenticationException;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;


import com.solace.psg.sempv1.solacesempreply.RpcReply.Rpc.Show.Queue.Queues;

import javax.xml.bind.JAXBContext;

import javax.xml.bind.JAXBException;

import javax.xml.bind.Unmarshaller;



import java.io.IOException;
import java.io.StringReader;


/**
 * Class to execute show commands.
 * 
 * @author VictorTsonkov
 *
 */
public class ShowCommands
{
	private SempSession session;
	
	private String showVpnQueuesStats = "<show><queue><name>*</name><vpn-name>{vpn}</vpn-name><stats></stats></queue></show></rpc>";
	private String showVpnQueuesDetails = "<show><queue><name>*</name><vpn-name>{vpn}</vpn-name><detail></detail></queue></show></rpc>";

	private static final Logger logger = LoggerFactory.getLogger(ShowCommands.class);
	
	private int pageElementCount = 50;

	/**
	 * Gets the page element count for elements retrieved on a single request.
	 * @return the pageElementCount
	 */
	public int getPageElementCount()
	{
		return pageElementCount;
	}

	/**
	 * Sets the page element count for elements retrieved on a single request.
	 * @param pageElementCount the pageElementCount to set
	 */
	public void setPageElementCount(int pageElementCount)
	{
		this.pageElementCount = pageElementCount;
	}

	private JAXBContext jaxbContext;

	/**
	 * Initialises a new instance of the class.
	 * @param session
	 * @throws SAXException 
	 */
	public ShowCommands(SempSession session) throws SAXException
	{
		this.session = session;
	}
	
	/**
	 * Gets queues info.
	 * @param vpnName
	 * @return
	 * @throws JAXBException
	 * @throws AuthenticationException
	 * @throws IOException
	 */
	public Queues getVpnQueueStats(String vpnName) throws JAXBException, HttpException, IOException
	{
		if (vpnName == null)
			throw new IllegalArgumentException("Argument vpnName cannot be null.");
		
		Queues result = null;
		
		session.open();

		String command = showVpnQueuesStats.replace("{vpn}", vpnName);

		logger.info("Running show command: {}", command);
		CloseableHttpResponse response = session.execute(command);

		if (response.getStatusLine().getStatusCode() == 200)
		{
			logger.info("Received 200 response from SEMP API");

			HttpEntity httpEntity = response.getEntity();
			String apiOutput = EntityUtils.toString(httpEntity);

			jaxbContext = session.getRpcReplyContext();
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			
			com.solace.psg.sempv1.solacesempreply.RpcReply reply = (com.solace.psg.sempv1.solacesempreply.RpcReply) jaxbUnmarshaller
					.unmarshal(new StringReader(apiOutput));

			result = reply.getRpc().getShow().getQueue().getQueues();
		}
		else
		{
			logger.warn("Received unexpected ({}) response from SEMP API", response.getStatusLine().getStatusCode());
			throw new HttpException("Request returned unexpected Status code: " + response.getStatusLine().getStatusCode());
		}

		session.close();

		logger.info("Show command completed", showVpnQueuesStats);
	
		return result;
	}

	/**
	 * Gets queues details.
	 * @param vpnName
	 * @return
	 * @throws JAXBException
	 * @throws AuthenticationException
	 * @throws IOException
	 */
	public Queues getVpnQueueDetails(String vpnName) throws JAXBException, HttpException, IOException
	{
		if (vpnName == null)
			throw new IllegalArgumentException("Argument vpnName cannot be null.");
		
		Queues result = null;
		
		session.open();

		String command = showVpnQueuesDetails.replace("{vpn}", vpnName);

		logger.info("Running show command: {}", command);
		CloseableHttpResponse response = session.execute(command);

		if (response.getStatusLine().getStatusCode() == 200)
		{
			logger.info("Received 200 response from SEMP API");

			HttpEntity httpEntity = response.getEntity();
			String apiOutput = EntityUtils.toString(httpEntity);

			jaxbContext = session.getRpcReplyContext();
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			
			com.solace.psg.sempv1.solacesempreply.RpcReply reply = (com.solace.psg.sempv1.solacesempreply.RpcReply) jaxbUnmarshaller
					.unmarshal(new StringReader(apiOutput));

			result = reply.getRpc().getShow().getQueue().getQueues();
		}
		else
		{
			logger.warn("Received unexpected ({}) response from SEMP API", response.getStatusLine().getStatusCode());
			throw new HttpException("Request returned unexpected Status code: " + response.getStatusLine().getStatusCode());
		}

		session.close();

		logger.info("Show command completed", showVpnQueuesDetails);
	
		return result;
	}

	
	/**
	 * Gets the inner XML of the more cookie XML.  
	 * @param response
	 * @return
	 */
	private String getMoreCookieContent(String response)
	{
		//JAXBElement<MoreCookie> jaxbElement = new JAXBElement<MoreCookie>( new QName("", "more-cookie"), MoreCookie.class,  mc);
		//JAXBContext context = JAXBContext.newInstance(MoreCookie.class);
		//Marshaller m = context.createMarshaller();
		//m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);			
		
		//StringWriter sw = new StringWriter();
		//m.marshal(jaxbElement, sw);
		//String commandMore = sw.toString();
		int indexOfCookie = response.indexOf("<more-cookie>");
		int lastIndexOfCookie = response.indexOf("</more-cookie>");
		return response.substring(indexOfCookie+14, lastIndexOfCookie);
	}	
}
