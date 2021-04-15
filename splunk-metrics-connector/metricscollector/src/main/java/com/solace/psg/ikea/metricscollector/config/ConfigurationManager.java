package com.solace.psg.ikea.metricscollector.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Class to handle application's configuration.
 * @author VictorTsonkov
 *
 */
public class ConfigurationManager
{
	private static final Logger logger = LogManager.getLogger(ConfigurationManager.class);

	public static final String DEFAULT_NAME = "config.properties"; 
	
	private String filename;
	
	private Properties props = new Properties();
	
	private static ConfigurationManager instance = new ConfigurationManager();
	
	/**
	 * Returns singleton configuration.
	 * @return
	 */
	public static ConfigurationManager getInstance()
	{
		return instance;
	}
	
	/**
	 * Initialises a new instance of the class.
	 */
	private ConfigurationManager() 
	{
		this(DEFAULT_NAME);
	}
	
	/**
	 * Initialises a new instance of the class.
	 */
	private ConfigurationManager(String filename) 
	{
		this.filename = filename;
		
		load();
	}
	
	/**
	 * Gets the filename;
	 * @return the filename.
	 */
	public String getFilename()
	{
		return filename;
	}
	
	/**
	 * Loads the config file properties.
	 */
	private void load() 
	{
		InputStream input = null;
		try
		{
			File propsFile = new File(filename);
			
			if (propsFile.exists())
			{
				input = new FileInputStream(propsFile);
				
				props.load(input);		
			}
		}
		catch(Exception ex)
		{
			logger.error("Error while trying to parse config file: {} ", ex.getMessage());
		}
		finally
		{
			if (input != null)
			try
			{
				input.close();
			}
			catch (IOException e)
			{
				logger.error("Error while trying to close config file: {} ", e.getMessage());
			}
		}
	}
	
	/**
	 * Checks and returns an Environment property if exists since Docker might configure those.
	 * @param key
	 * @return
	 */
	private String getProperty(String key)
	{
		if (System.getenv(key) != null)
			return System.getenv(key);
		else
			return props.getProperty(key);
	}

	/**
	 * Gets broker SEMP user name.
	 * @return user name
	 */
	public String getBrokerUsername(int index)
	{
		return getProperty("broker." + index + ".sempUsername");
	}

	/**
	 * Gets broker SEMP password.
	 * @return password
	 */
	public String getBrokerPassword(int index)
	{
		return getProperty("broker." + index + ".sempPassword");
	}

	/**
	 * Gets broker service name.
	 * @return user name
	 */
	public String getBrokerServiceName(int index)
	{
		return getProperty("broker." + index + ".serviceName");
	}
	
	/**
	 * Gets broker service name.
	 * @return user name
	 */
	public String getBrokerVpnName(int index)
	{
		return getProperty("broker." + index + ".vpnName");
	}	
	
	/**
	 * Gets broker URL.
	 * @return user name
	 */
	public String getBrokerUrl(int index)
	{
		return getProperty("broker." + index + ".url");
	}


	/**
	 * Gets broker 1 SEMP password.
	 * @return password
	 */
	public String getBroker2Password()
	{
		return getProperty("broker.2.sempPassword");
	}	
	
	/**
	 * Gets polling interval.
	 * @return
	 */
	public int getPollingInterval()
	{
		return Integer.valueOf(getProperty("pollingInterval"));
	}

	/**
	 * Gets broker count.
	 * @return
	 */
	public int getBrokerCount()
	{
		return Integer.valueOf(getProperty("brokerCount"));
	}
	
	/**
	 * Getsenriched format property if data will contain additional fields.
	 * @return
	 */
	public boolean  getEnrichedFormat()
	{
		return Boolean.valueOf(getProperty("enrichedFormat"));
	}
	
	/**
	 * Sets Splunk URL.
	 * @param url url
	 */
	public void setSplunkUrl(String url)
	{
		props.setProperty("splunkUrl", url);
	}

	/**
	 * Gets Splunk certificate alias.
	 * @return url
	 */
	public String getSplunkCertificateAlias()
	{
		return getProperty("splunkCertificateAlias");
	}
	
	/**
	 * Returns if Splunk is using secure connection.
	 * @return
	 */
	public boolean  isSplunkSecured()
	{
		return Boolean.valueOf(getProperty("splunkSecured"));
	}
	
	/**
	 * Gets Splunk certificate file.
	 * @return url
	 */
	public String getSplunkCertificateFile()
	{
		return getProperty("splunkCertificateFile");
	}	

	/**
	 * Gets Splunk source type.
	 * @return url
	 */
	public String getSplunkSourceType()
	{
		return getProperty("splunkSourceType");
	}
	
	/**
	 * Gets Splunk index.
	 * @return url
	 */
	public String getSplunkIndex()
	{
		return getProperty("splunkIndex");
	}
	
	/**
	 * Gets Splunk URL.
	 * @return url
	 */
	public String getSplunkUrl()
	{
		return getProperty("splunkUrl");
	}

	/**
	 * Sets Splunk token.
	 * @param url url
	 */
	public void setSplunkToken(String token)
	{
		props.setProperty("splunkToken", token);
	}

	/**
	 * Gets Splunk token.
	 * @return url
	 */
	public String getSplunkToken()
	{
		return getProperty("splunkToken");
	}
}