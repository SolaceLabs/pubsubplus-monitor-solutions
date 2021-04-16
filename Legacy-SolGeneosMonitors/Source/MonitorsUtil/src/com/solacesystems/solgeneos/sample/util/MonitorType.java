package com.solacesystems.solgeneos.sample.util;

public enum MonitorType {
	VPN_OBJECT_LIST,
	VPN_METRIC_LIST,
	SYSTEM_OBJECT_LIST,
	SYSTEM_METRIC_LIST,
	UNKNOWN;
	
	public static MonitorType getType(String typeStr) {
		if(typeStr == null)
			return UNKNOWN;
		else if(typeStr.equals("VPN_OBJECT_LIST"))
			return VPN_OBJECT_LIST;
		else if(typeStr.equals("VPN_METRIC_LIST"))
			return VPN_METRIC_LIST;
		else if(typeStr.equals("SYSTEM_OBJECT_LIST"))
			return SYSTEM_OBJECT_LIST;
		else if(typeStr.equals("SYSTEM_METRIC_LIST"))
			return SYSTEM_METRIC_LIST;
		else {
			System.out.println("unexpected type: " + typeStr);
			return UNKNOWN;
		}
	}
	
	public String toString() {
		switch (this) {
		case VPN_OBJECT_LIST:
			return "VPN_OBJECT_LIST";
		case VPN_METRIC_LIST:
			return "VPN_METRIC_LIST";
		case SYSTEM_OBJECT_LIST:
			return "SYSTEM_OBJECT_LIST";
		case SYSTEM_METRIC_LIST:
			return "SYSTEM_METRIC_LIST";
		default:
			return "UNKNOWN";
		}
	}
};