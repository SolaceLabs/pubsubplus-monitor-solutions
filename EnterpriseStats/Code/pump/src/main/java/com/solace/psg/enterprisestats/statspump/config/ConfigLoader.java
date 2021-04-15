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
package com.solace.psg.enterprisestats.statspump.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.solace.psg.enterprisestats.statspump.LocalMgmtBusListener;
import com.solace.psg.enterprisestats.statspump.LogicalAppliance;
import com.solace.psg.enterprisestats.statspump.MessageBus;
import com.solace.psg.enterprisestats.statspump.MessageBusRepublisher;
import com.solace.psg.enterprisestats.statspump.PhysicalAppliance;
import com.solace.psg.enterprisestats.statspump.MessageBus.Type;
import com.solace.psg.enterprisestats.statspump.config.xml.Appliances;
import com.solace.psg.enterprisestats.statspump.config.xml.BackupModeType;
import com.solace.psg.enterprisestats.statspump.config.xml.Config;
import com.solace.psg.enterprisestats.statspump.config.xml.DestinationType;
import com.solace.psg.enterprisestats.statspump.config.xml.ExceptionDefaultActionType;
import com.solace.psg.enterprisestats.statspump.config.xml.PollerGroups;
import com.solace.psg.enterprisestats.statspump.config.xml.Appliances.Appliance;
import com.solace.psg.enterprisestats.statspump.config.xml.Appliances.Appliance.LocalMgmtMsgBus;
import com.solace.psg.enterprisestats.statspump.config.xml.Appliances.Appliance.MgmtMsgBus;
import com.solace.psg.enterprisestats.statspump.config.xml.Appliances.Appliance.SelfMsgBus;
import com.solace.psg.enterprisestats.statspump.config.xml.Config.RunConfigurations.RunConfiguration;
import com.solace.psg.enterprisestats.statspump.config.xml.ObjectTagsType.ObjectTag;
import com.solace.psg.enterprisestats.statspump.config.xml.PollerGroups.PollerGroup;
import com.solace.psg.enterprisestats.statspump.config.xml.PollerGroups.PollerGroup.Pollers.Poller;
import com.solace.psg.enterprisestats.statspump.config.xml.SempRequestType.Rpc;
import com.solace.psg.enterprisestats.statspump.containers.ContainerFactory;
import com.solace.psg.enterprisestats.statspump.pollers.GenericPoller;
import com.solace.psg.enterprisestats.statspump.pollers.GenericPoller.Builder;
import com.solace.psg.enterprisestats.statspump.pollers.Poller.Scope;
import com.solace.psg.util.AES;
import com.solace.psg.util.EncryptionException;

public class ConfigLoader {
    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);
    
    public static final String POLLER_XSD_FILENAME = "resources/poller_config.xsd";
    public static final String POLLER_GROUP_XSD_FILENAME = "resources/poller_group_config.xsd";
    public static final String APPLIANCE_XSD_FILENAME = "resources/appliance_config.xsd";
    private static final SchemaFactory schemaFactory;
    static {
        //schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    	schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    }
    
    public static final Map<String,com.solace.psg.enterprisestats.statspump.pollers.Poller> pollersMap = new LinkedHashMap<String,com.solace.psg.enterprisestats.statspump.pollers.Poller>();
    public static final Map<String,Map<String,Float>> pollerGroupsMap = new LinkedHashMap<String,Map<String,Float>>();
    public static final List<LogicalAppliance> appliancesList = new ArrayList<LogicalAppliance>();

    public static void loadConfig(StatsConfigStreams loader) throws ConfigLoaderException {
        logger.info(String.format("Loading StatsPump configuration files via input streams"));
        
        try {
            InputStream pollerConfigFileInputStream = loader.getPollerConfig();
            InputStream pollerGroupConfigFileInputStream = loader.getPollerGroupConfig();
            InputStream applianceConfigFileInputStream = loader.getApplianceConfig();
            
            loadPollerConfig(pollerConfigFileInputStream);
            loadPollerGroupConfig(pollerGroupConfigFileInputStream);
            loadApplianceConfig(applianceConfigFileInputStream);
            logger.info("Configuration loaded");
        }
        catch (IOException io ) {
        	throw new ConfigLoaderException(io);
        }
    }
    
    /**
     * What does this method do??  I think it just validates or provides a method of reading an XML text file?
     * @param jaxbContext
     * @param filename
     * @return
     * @throws ConfigLoaderException
     */
    private static Unmarshaller buildUnmarshaller(final JAXBContext jaxbContext, final String filename) throws ConfigLoaderException {
        try {
            // create JAXB context and initializing Marshaller
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            // apply the schema
            if (ConfigLoader.class.getClassLoader().getResource(filename) == null) {
                throw new ConfigLoaderException("Could not find poller schema file: "+filename);
            }
            Schema configSchema = schemaFactory.newSchema(ConfigLoader.class.getClassLoader().getResource(filename));
            jaxbUnmarshaller.setSchema(configSchema);
            jaxbUnmarshaller.setEventHandler(new StatsPumpValidationEventHandler());
            return jaxbUnmarshaller;
        } catch (JAXBException e) {
            throw new ConfigLoaderException("Failed to unmarshall file: " + filename,e);
        } catch (SAXException e) {
            throw new ConfigLoaderException("Failed to load schema file: " + filename,e);
        }
    }
    
    /**
     * This parses the <rpc> tag inside the poller config... just makes sure it is valid XML... 
     * currently, the code that validates it against a particular SEMP schema is commented out
     * @param jaxbContext
     * @param rpc
     * @return
     * @throws ConfigLoaderException
     */
    private static String parseSempRpc(JAXBContext jaxbContext, Rpc rpc) throws ConfigLoaderException { //JAXBException, SAXException, IOException {
        try {
            // make sure the SEMP RPC SEMP is proper
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            StringWriter sw = new StringWriter();
            marshaller.marshal(new JAXBElement<Rpc>(new QName("rpc"), Rpc.class, rpc),sw);
            // this loads the raw XML text from the config file into sw.  For now, we will just return, and assume it's valid SEMP
            return sw.toString();
        } catch (JAXBException e) {
            throw new ConfigLoaderException("JAXBMarshaller error while parsing SEMP RPC",e);
        }
    }
    
    
    // Sax exception = problem with schema, or validator method; JAXB exception = problem parsing config xml
    private static void loadPollerConfig(InputStream pollerConfigFileInputStream) throws ConfigLoaderException {
        JAXBContext jaxbContext;
        Unmarshaller jaxbUnmarshaller;
        Config pollerConfig;
        try {
            jaxbContext = JAXBContext.newInstance(Config.class);
            jaxbUnmarshaller = buildUnmarshaller(jaxbContext, POLLER_XSD_FILENAME);
            pollerConfig = (Config)jaxbUnmarshaller.unmarshal(pollerConfigFileInputStream);
        } catch (JAXBException e) {
            throw new ConfigLoaderException("JAXB unmarshaller error while loading poller config: ", e);
        }
        
        // this next bit loads all the various run configurations (e.g. primary only, backup only, for primary appliance when active/standby, etc.
        Map<String,RunConfiguration> runConfigurations = new HashMap<String,RunConfiguration>();
        for (RunConfiguration runConfiguration : pollerConfig.getRunConfigurations().getRunConfiguration()) {
            runConfigurations.put(runConfiguration.getName(),runConfiguration);
        }
        com.solace.psg.enterprisestats.statspump.config.xml.Config.Pollers pollers = pollerConfig.getPollers();
        for (int i=0;i<pollers.getSystemPoller().size();i++) {
            com.solace.psg.enterprisestats.statspump.config.xml.Config.Pollers.SystemPoller poller = pollers.getSystemPoller().get(i);
            try {
                Builder builder = new Builder();
                builder.setName(poller.getName());
                builder.setScope(Scope.SYSTEM);
                builder.setDescription(poller.getDescription());
                if (!runConfigurations.containsKey(poller.getRunConfiguration())) {
                    logger.warn(String.format("List of defined RunConditions doesn't include '%s' referenced by Poller '%s'. Skipping",poller.getRunConfiguration(),poller.getName()));
                    continue;
                }
                builder.setSempRequest(parseSempRpc(jaxbContext,poller.getSempRequest().getRpc()));
                builder.setRunConditionOnPrimaryWhenAS(runConfigurations.get(poller.getRunConfiguration()).getRunOnPrimaryWhenActiveStandby());
                builder.setRunConditionOnBackupWhenAS(runConfigurations.get(poller.getRunConfiguration()).getRunOnBackupWhenActiveStandby());
                builder.setRunConditionOnPrimaryWhenAA(runConfigurations.get(poller.getRunConfiguration()).getRunOnPrimaryWhenActiveActive());
                builder.setRunConditionOnBackupWhenAA(runConfigurations.get(poller.getRunConfiguration()).getRunOnBackupWhenActiveActive());
                if (poller.getDestination() == DestinationType.SELF) {
                    throw new IllegalArgumentException("Not allowed to have an appliance poller that goes to SELF - only MGMT or BOTH allowed");
                }
                builder.setDestination(poller.getDestination());
                builder.setTopicStringSuffix(poller.getTopicStringSuffix());
                builder.setBaseTag(poller.getBaseTag());
                if (poller.getObjectTags() != null) {
                    for (int j=0;j<poller.getObjectTags().getObjectTag().size();j++) {
                        ObjectTag ot = poller.getObjectTags().getObjectTag().get(j);
                        builder.addObjectTag(ot.getName(),ot.getTag());
                    }
                }
                pollersMap.put(poller.getName(),new GenericPoller(builder));
            } catch (ConfigLoaderException e) {
                throw new ConfigLoaderException(String.format("Had issue with %s",poller),e);
            }
        }
        for (int i=0;i<pollers.getVpnPoller().size();i++) {
            com.solace.psg.enterprisestats.statspump.config.xml.Config.Pollers.VpnPoller poller = pollers.getVpnPoller().get(i);
            try {
                Builder builder = new Builder();
                builder.setName(poller.getName());
                builder.setScope(Scope.VPN);
                String sempRequest = parseSempRpc(jaxbContext,poller.getSempRequest().getRpc());
                builder.setSempRequest(sempRequest);
                builder.setDescription(poller.getDescription());

                if (!runConfigurations.containsKey(poller.getRunConfiguration())) {
                    logger.warn(String.format("List of defined RunConditions doesn't include '%s' referenced by Poller '%s'. Skipping",poller.getRunConfiguration(),poller.getName()));
                    continue;
                }
                builder.setRunConditionOnPrimaryWhenAS(runConfigurations.get(poller.getRunConfiguration()).getRunOnPrimaryWhenActiveStandby());
                builder.setRunConditionOnBackupWhenAS(runConfigurations.get(poller.getRunConfiguration()).getRunOnBackupWhenActiveStandby());
                builder.setRunConditionOnPrimaryWhenAA(runConfigurations.get(poller.getRunConfiguration()).getRunOnPrimaryWhenActiveActive());
                builder.setRunConditionOnBackupWhenAA(runConfigurations.get(poller.getRunConfiguration()).getRunOnBackupWhenActiveActive());
                builder.setDestination(poller.getDestination());
                builder.setTopicStringSuffix(poller.getTopicStringSuffix());
                builder.setBaseTag(poller.getBaseTag());
                builder.setVpnNameTag(poller.getVpnNameTag());
                if (poller.getObjectTags() != null) {
                    for (int j=0;j<poller.getObjectTags().getObjectTag().size();j++) {
                        ObjectTag ot = poller.getObjectTags().getObjectTag().get(j);
                        builder.addObjectTag(ot.getName(),ot.getTag());
                    }
                }
                pollersMap.put(poller.getName(),new GenericPoller(builder));
            } catch (IllegalArgumentException e) {  // will be thrown when trying to build the Poller if there's an issue
                throw new ConfigLoaderException(String.format("Had issue with %s",poller),e);
            } catch (ConfigLoaderException e) {
                throw new ConfigLoaderException(String.format("Had issue with %s",poller),e);
            }
        }
    }

    
    private static void loadPollerGroupConfig(InputStream pollerGroupConfigFileInputStream) throws ConfigLoaderException {
        JAXBContext jaxbContext;
        Unmarshaller jaxbUnmarshaller;
        PollerGroups pollerGroupConfig;
        try {
            jaxbContext = JAXBContext.newInstance(PollerGroups.class);
            jaxbUnmarshaller = buildUnmarshaller(jaxbContext,POLLER_GROUP_XSD_FILENAME);
            pollerGroupConfig = (PollerGroups)jaxbUnmarshaller.unmarshal(pollerGroupConfigFileInputStream);
        } catch (JAXBException e) {
            throw new ConfigLoaderException("JAXB unmarshaller error while loading poller group config: ",e);
        }
        List<PollerGroup> pollerGroups = pollerGroupConfig.getPollerGroup();
        for (int i=0;i<pollerGroups.size();i++) {
            PollerGroup group = pollerGroups.get(i);
            String name = group.getName();
            Map<String,Float> groupedPollersMap = new LinkedHashMap<String,Float>();
            for (int j=0;j<group.getPollers().getPoller().size();j++) {
                Poller poll = group.getPollers().getPoller().get(j);
                if (!pollersMap.containsKey(poll.getPollerName())) {
                    logger.warn(String.format("List of defined pollers doesn't include '%s' referenced in PollerGroup '%s'. Typo in config XML? Skipping",poll.getPollerName(),name));
                    continue;
                }
                groupedPollersMap.put(poll.getPollerName(),poll.getPollIntervalInSec());
            }
            pollerGroupsMap.put(name,groupedPollersMap);
        }
  }    
    
    
    @SuppressWarnings("unchecked")
    public static ContainerFactory buildNewContainerFactory(String containerFactoryClassName) throws ConfigLoaderException {
        try {
            Class<? extends ContainerFactory> containerClazz = (Class<? extends ContainerFactory>)Class.forName(containerFactoryClassName);
            if (containerClazz.isEnum()) {
        	    @SuppressWarnings({ "rawtypes" })
        	    Class c = containerClazz.asSubclass(Enum.class);
        	    Object val = Enum.valueOf(c, "INSTANCE");
        	    return (ContainerFactory) val;
            } else {  // it's not an enum, so try to instantiate it
                return (containerClazz.newInstance());
            }
        } catch (IllegalArgumentException e) {
            logger.error("Could not find a field named 'INSTANCE' in the Enum class "+containerFactoryClassName,e);
            throw new ConfigLoaderException(e);
        } catch (ClassNotFoundException e) {
            logger.error("Could not find a container factory called "+containerFactoryClassName,e);
            throw new ConfigLoaderException(e);
        } catch (InstantiationException e) {
            logger.error("Found, but could not instantiate a container factory called "+containerFactoryClassName,e);
            throw new ConfigLoaderException(e);
        } catch (IllegalAccessException e) {
            logger.error("Found, but could not instantiate a container factory called "+containerFactoryClassName,e);
            throw new ConfigLoaderException(e);
        }
    }
    
	public static LocalMgmtBusListener buildNewLocalListener(String localListenerClassName,
			Map<String, Object> listenerConfig) throws ConfigLoaderException {
		try {
			Class<?> listenerClazz = Class.forName(localListenerClassName);
			Constructor<?> listenerConstructor = listenerClazz.getConstructor(Map.class);
			return (LocalMgmtBusListener) listenerConstructor.newInstance(listenerConfig);
		} catch (NoSuchMethodException e) {
			logger.error("Could not find a local listener class: " + localListenerClassName
					+ " with constructor that accepts parameter of type Map<String, Object>", e);
			throw new ConfigLoaderException(e);
		} catch (InvocationTargetException e) {
			logger.error("Could not invoke constructor for local listener class: " + localListenerClassName
					+ " with that accepts parameter of type Map<String, Object>", e);
			throw new ConfigLoaderException(e);
		} catch (ClassNotFoundException e) {
			logger.error("Could not find a local listener class called " + localListenerClassName, e);
			throw new ConfigLoaderException(e);
		} catch (InstantiationException e) {
			logger.error("Found, but could not instantiate local listener class called " + localListenerClassName, e);
			throw new ConfigLoaderException(e);
		} catch (IllegalAccessException e) {
			logger.error("Found, but could not instantiate local listener class called " + localListenerClassName, e);
			throw new ConfigLoaderException(e);
		}
	}
    
    private static String decrypt(String pw, String propName) throws ConfigLoaderException {
    	String rc = null;
    	try {
			rc = AES.decrypt(pw);
		} 
    	catch (EncryptionException e) {
			throw new ConfigLoaderException("The " + propName + " password which is stored " +
					" in the appliance config file " +
					"doesn't appear to be an encrypted password. Use the generatePw utility " +
					"create encrypted passwords to store in this config file.");
		}
    	return rc;
    }
    
    private static void loadApplianceConfig(InputStream applianceConfigFileInputStream) throws ConfigLoaderException { 
        JAXBContext appliancesJaxbContext;
        Appliances appliances;
        try {
            appliancesJaxbContext = JAXBContext.newInstance(Appliances.class);
            Unmarshaller appliancesJaxbUnmarshaller = buildUnmarshaller(appliancesJaxbContext,APPLIANCE_XSD_FILENAME);
            appliances = (Appliances)appliancesJaxbUnmarshaller.unmarshal(applianceConfigFileInputStream);
        } catch (JAXBException e) {
            throw new ConfigLoaderException("JAXB unmarshaller error while loading appliance configuration",e);
        }
        MessageBusRepublisher.PublishDmqEligible = appliances.isPublishDmqEligible(); 
        MessageBusRepublisher.PublishTtl = appliances.getPublishTtl();
        logger.debug("Loaded config with ttl=" + appliances.getPublishTtl() + ", DmqEligible=" + appliances.isPublishDmqEligible());
        
        for (int i=0;i<appliances.getAppliance().size();i++) {
            Appliance appliance = appliances.getAppliance().get(i);
            LogicalAppliance logical = new LogicalAppliance(appliance.getName());
            // build the physical appliances first
            
            String pw = appliance.getPrimary().getCliPassword();
            if (pw != null) {
            	if (pw.length() > 0) {
            		pw = decrypt(pw, "primary appliance password");		
            	}
            }
            // check if the primary broker requires using secure sessions for SEMP or not
            boolean isSecurePrimary = false;
            
            if (appliance.getPrimary().getSecure() != null) {
            	logger.info("Using SEMP with secure session for broker: {}",appliance.getPrimary().getSempHost());
            	isSecurePrimary = true;
            }else {
            	logger.info("Using SEMP with plaintext for broker: {}",appliance.getPrimary().getSempHost());
            }
            
            PhysicalAppliance primary = new PhysicalAppliance(
                    logical,
                    appliance.getPrimary().getSempHost(),
                    appliance.getPrimary().getCliUsername(),
                    pw,
                    isSecurePrimary);
            logical.addPrimaryPhysicalAppliance(primary);
            
            if (appliance.getBackup() != null) {
            	pw = appliance.getBackup().getCliPassword();
                if (pw != null) {
                	if (pw.length() > 0) {
                		pw = decrypt(pw, "backup appliance password");	
                	}
                }
                
                // check if the primary broker requires using secure sessions for SEMP or not
                boolean isSecureBackup = false;
                
                if (appliance.getBackup().getSecure() != null) {
                	logger.info("Using SEMP with SSL for broker:{}",appliance.getBackup().getSempHost());
                	isSecureBackup = true;
                }else {
                	logger.info("Using SEMP with plaintext for broker:{}",appliance.getBackup().getSempHost());
                }
                
                PhysicalAppliance backup = new PhysicalAppliance(
                        logical,
                        appliance.getBackup().getSempHost(),
                        appliance.getBackup().getCliUsername(),
                        pw,
                        isSecureBackup);
                
                if (appliance.getBackup().getMode() == BackupModeType.STANDBY) {
                    logical.addBackupPhysicalAppliance(backup,LogicalAppliance.Type.ACTIVE_STANDBY);
                } else {
                    logical.addBackupPhysicalAppliance(backup,LogicalAppliance.Type.ACTIVE_ACTIVE);
                }
            }
            // now build each message-bus destination
            for (MgmtMsgBus mgmt : appliance.getMgmtMsgBus()) {
            	pw = mgmt.getPassword();
                if (pw != null) {
                	if (pw.length() > 0) {
                		pw = decrypt(pw, "message bus password");	
                	}
                }
                MessageBus.Builder b = new MessageBus.Builder(logical,mgmt.getHost());
                b.setType(Type.MGMT);
                b.setVpn(mgmt.getVpn());
                b.setUsername(mgmt.getClientUsername());  // might be null
                b.setPassword(pw);
                ContainerFactory containerFactory;
                containerFactory = buildNewContainerFactory(mgmt.getContainerFactoryClass());
                b.setContainerFactoryClass(containerFactory);
                MessageBus mb = b.build();
                logical.addMessageBus(mb);
                // still need to add it to the main appliance
            }
            for (SelfMsgBus self : appliance.getSelfMsgBus()) {
                MessageBus.Builder b = new MessageBus.Builder(logical,self.getHost());
                b.setType(Type.SELF);
                if (self.getVpn() != null) {  // means that we must publish everything to one VPN
                    b.setVpn(self.getVpn());
                    if (self.getVpnConnectionAclSettings() != null) {
                        throw new ConfigLoaderException("asdlkfj 9 - XSD should block this, but cannot define a particular VPN, and then connection ACLs");
                    }
                }
                b.setUsername(self.getClientUsername());  // might be null
                pw = self.getPassword();
                if (pw != null) {
                	if (pw.length() > 0) {
                		pw = decrypt(pw, "self bus password");	
                	}
                }
                b.setPassword(pw);
                ContainerFactory containerFactory = buildNewContainerFactory(self.getContainerFactoryClass());
                b.setContainerFactoryClass(containerFactory);
                MessageBus selfMsgBus = b.build();
                if (self.getVpnConnectionAclSettings() != null) {
                    selfMsgBus.setExceptionDefaultAction(self.getVpnConnectionAclSettings().getDefaultAction() == ExceptionDefaultActionType.ALLOW);
                    for (String vpn : self.getVpnConnectionAclSettings().getVpnException()) {
                        selfMsgBus.addVpnException(vpn);
                    }
                }
                logical.addMessageBus(selfMsgBus);
            }
			for (LocalMgmtMsgBus mgmt : appliance.getLocalMgmtMsgBus()) {
				MessageBus.Builder b = new MessageBus.Builder(logical, mgmt.getHost());
				b.setType(Type.LOCAL_MGMT);
				ContainerFactory containerFactory;
				containerFactory = buildNewContainerFactory(mgmt.getContainerFactoryClass());
                b.setContainerFactoryClass(containerFactory);
				// Load the configuration for the listener and notify it we are
				// starting so it can initialize
				try {
					Map<String, Object> listenerConfig = loadPropertiesFrom(mgmt.getLocalListenerConfig());
					LocalMgmtBusListener localMgmtBusListener = buildNewLocalListener(mgmt.getLocalListenerClass(), listenerConfig);
					localMgmtBusListener.onPumpStartup();
					b.setLocalListenerConfig(listenerConfig);
					b.setLocalListenerClass(localMgmtBusListener);
				} catch (Exception e) {
					throw new ConfigLoaderException(
							"Unable to initialize the Local Mgmt-Bus Listener: " + mgmt.getLocalListenerClass() + " - check inner exception for details.", e);
				}

				b.setContainerFactoryClass(containerFactory);
				MessageBus mb = b.build();
				logical.addMessageBus(mb);
			}

            for (int j=0;j<appliance.getPollerGroup().size();j++) {
                String appliancePollerGroupName = appliance.getPollerGroup().get(j);
                if (!pollerGroupsMap.containsKey(appliancePollerGroupName)) {
                    throw new ConfigLoaderException("Unknown poller Group: "+appliancePollerGroupName);
                }
                for (String pollerName : pollerGroupsMap.get(appliancePollerGroupName).keySet()) {
                    // add each poller, with the specified interval from the Map
                    logical.addPoller(pollersMap.get(pollerName),pollerGroupsMap.get(appliancePollerGroupName).get(pollerName));
                }
            }
            appliancesList.add(logical);
        }
    }
    
    /**
	 * Load properties from a configuration file.
	 * 
	 * @param fname
	 * @return
	 * @throws IOException
	 */
	public static Map<String, Object> loadPropertiesFrom(String fname) throws IOException {
		FileInputStream fis = new FileInputStream(fname);
		Properties p = new Properties();
		p.load(fis);
		return getPropertiesMap(p);
	}

	/**
	 * Return a map from initialized from a Properties object.
	 * 
	 * @param properties
	 * @return
	 */
	public static Map<String, Object> getPropertiesMap(Properties properties) {
		Map<String, Object> propMap = new HashMap<String, Object>();
		for (Entry<Object, Object> x : properties.entrySet()) {
			propMap.put((String) x.getKey(), x.getValue());
		}

		return propMap;
	}
    
    /* The following main method can be used as an informal unit test
    public static void main(String... args) throws ConfigLoaderException {
        String test = "hi\\there\\hello";
        System.out.println(test.substring(test.lastIndexOf(File.separator)+1));
        System.exit(-1);

        ContainerFactory containerFactory = buildNewContainerFactory("com.solacesystems.psg.enterprisestats.statspump.containers.factories.JsonMapFactory");
        ContainerFactory containerFactory2 = buildNewContainerFactory("com.solacesystems.psg.enterprisestats.statspump.containers.factories.JsonMapFactory");
        System.out.println(containerFactory.equals(containerFactory2));
    }
    */
}
