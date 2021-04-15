package com.solace.psg.enterprisestats.receiver;

public class TypeChecker {
	 public static boolean isDouble(String value) {
	    	boolean bRc = false;
	    	try {
	        	@SuppressWarnings("unused")
				Double d = new Double(value);
	        	bRc = true;
	    	} catch (Exception e) {}
	    	return bRc;
	    }
	    public static boolean isLong(String value) {
	    	boolean bRc = false;
	    	try {
	    		@SuppressWarnings("unused")
				Long l = new Long(value);
	        	bRc = true;
	    	} catch (Exception e) {}
	    	return bRc;
	    }
	    public static boolean isInt(String value) {
	    	boolean bRc = false;
	    	try {
	    		@SuppressWarnings("unused")
				Integer i = new Integer(value);
	        	bRc = true;
	    	} catch (Exception e) {}
	    	return bRc;
	    }
	    public static boolean isShort(String value) {
	    	boolean bRc = false;
	    	try {
	    		@SuppressWarnings("unused")
	    		Short i = new Short(value);
	        	bRc = true;
	    	} catch (Exception e) {}
	    	return bRc;
	    }
	    public static boolean isBoolean(String value) {
	    	boolean bRc = false;
	    	if (value.length() == 0) {
	    		// nope, this will not be consider a boolean for this purpose
	    	}
	    	else {
		    	try {
		    		@SuppressWarnings("unused")
					Boolean b = new Boolean(value);
		        	bRc = true;
		    	} catch (Exception e) {}
	    	}
	    	return bRc;
	    }   
}
