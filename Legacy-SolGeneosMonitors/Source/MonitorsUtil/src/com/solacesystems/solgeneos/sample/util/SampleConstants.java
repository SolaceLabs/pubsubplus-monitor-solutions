package com.solacesystems.solgeneos.sample.util;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

public interface SampleConstants {

	/**
     * Management IP Address Property
     */
    static final public String MGMT_IP_ADDRESS_PROPERTY_NAME                = "ipaddress";

    /**
     * Management Port Property
     */
    static final public String MGMT_PORT_PROPERTY_NAME                      = "port";

    /**
     * Management Username Property
     */
    static final public String MGMT_USERNAME_PROPERTY_NAME                  = "username";

    /**
     * Management Password Property
     */
    static final public String MGMT_PASSWORD_PROPERTY_NAME                  = "password";
    static final public String MGMT_ENCRYPTED_PASSWORD_PROPERTY_NAME        = "encrypted_password";
    
    static final public String SAMPLER_PROPERTY_NAME                  		= "sampler";
    
    static final public String NETPROBE_PROPERTY_NAME						= "netprobe.np0.endpoint";
    
    /**
     * HTTP Post Request Header
     */
	public final static Header HEADER_CONTENT_TYPE_UTF8 = new BasicHeader("Content-type","text/xml; charset=utf-8");	
	
	/**
	 * HTTP Post Request Uri
	 */
	public static final String HTTP_REQUEST_URI = "/SEMP";

	/**
	 * Appliance SEMP Version
	 * 
	 * The appliance's SEMP show commands are backward compatible to a limited number of versions. For this
	 * example, we choose soltr/5_4 SEMP version so that the sample will work with appliance running SOL-TR 5.4 and later.
	 * However, if your monitor is using a SEMP command that exists in a version later than SOL-TR 5.4, then
	 * you should change the value of this variable to the correct SEMP version.
	 */
	//public static final String SEMP_VERSION = "soltr/7_1";
	
    /**
     * User Properties File Name
     */
	public static final String USER_PROPERTIES_FILE_NAME = "_user_sample.properties";	
	
	/**
	 * Property Name in User Properties File
	 */
	public static final String VIEW_CREATOR = "viewCreator";
	
	
	

	
	
	public static final String ATTRIBUTE_NETPROBE_ALIAS = "netprobeAlias";
	public static final String ATTRIBUTE_MANAGED_ENTITY = "managedEntity";
	public static final String ATTRIBUTE_MANAGED_ENTITY_PREFIX = "managedEntityPrefix";
	public static final String ATTRIBUTE_PAGE_SIZE = "pageSize";
	public static final String ATTRIBUTE_MAX_OBJECTS = "maxObjects";
	public static final String ATTRIBUTE_SEMP_REQUEST = "sempRequest";
	public static final String ATTRIBUTE_SEMP_VERSION = "sempVersion";
	public static final String ATTRIBUTE_TOPICS = "topics";
	public static final String ATTRIBUTE_MONITOR_TYPE = "monitorType";
	public static final String ATTRIBUTE_DELIMITER_TOPIC = "delimiterTopic";
	public static final String ATTRIBUTE_OBJECT_ID_TOPIC = "objectIdTopic";
	public static final String ATTRIBUTE_VIEW_NAME = "view.v0.viewName";
	public static final String ATTRIBUTE_GROUP_HEADER = "groupHeader";
	public static final String ATTRIBUTE_CONDITIONAL_TOPIC = "conditionalTopic";
	public static final String ATTRIBUTE_PARENT_TOPIC = "parentTopic";
	public static final String ATTRIBUTE_VPN_SAMPLER = "vpnSampler";
	
	public static final String ATTRIBUTE_NETPROBE_ALIAS_DEFAULT = "np0";
	public static final String ATTRIBUTE_MANAGED_ENTITY_PREFIX_DEFAULT = "manEnt";
	public static final int ATTRIBUTE_PAGE_SIZE_DEFAULT = 500;
	public static final int ATTRIBUTE_MAX_OBJECTS_DEFAULT = 500;
	public static final String ATTRIBUTE_GROUP_HEADER_DEFAULT = "SolOS";
	
	
	// Request placeholders, these strings in the SEMP Requests will be replaced with proper monitor config
	public static final String PLACEHOLDER_SEMP_VERSION = "%SEMP_VERSION%";
	public static final String PLACEHOLDER_VPN_NAME = "%VPN_NAME%";
	public static final String PLACEHOLDER_PAGE_SIZE = "%PAGE_SIZE%";
	
	
}
