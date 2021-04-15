/**
 * 
 */
package com.solace.psg.sempv1;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to provide common methods for the different command implementations. 
 * @author VictorTsonkov
 *
 */
public abstract class SempCommand
{
	private static final Logger logger = LoggerFactory.getLogger(SempCommand.class);

	protected SempSession session;
	
	protected JAXBContext jaxbContext;
	
	/**
	 * Initialises a new instance of the class.
	 */
	protected SempCommand(SempSession session)
	{
		this.session = session;
	}
	
	/**
	 * Executes a configuration command.
	 * @param command
	 * @return
	 * @throws IOException 
	 * @throws ParseException 
	 * @throws AuthenticationException 
	 * @throws JAXBException 
	 */
	protected boolean executeCommand(String command) throws ParseException, IOException, AuthenticationException, JAXBException
	{
		boolean success = false;

		session.open();		

		logger.info("Running configure command: {}", command);
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

			com.solace.psg.sempv1.solacesempreply.RpcReply.ExecuteResult result = reply.getExecuteResult();

			if (result.getCode().equals("ok"))
			{
				success = true;
			}
			else
			{
				success = false;
				logger.info("Response from SEMP API {}", apiOutput);
			}

			logger.info("Result code {}", result.getCode());

		}
		else
		{
			logger.warn("Received unexpected ({}) response from SEMP API", response.getStatusLine().getStatusCode());

			success = false;
		}

		session.close();

		logger.info("Configure command completed ({})", command);

		return success;				
	}
}
