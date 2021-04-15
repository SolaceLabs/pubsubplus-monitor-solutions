package com.solace.psg.enterprisestats.statspump.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class LocalFileConfigStreamsImpl implements StatsConfigStreams {

	String pollerFilename;
    String pollerGroupFilename; 
    String applianceFilename;
    		
	public LocalFileConfigStreamsImpl(String pollerFilename, String pollerGroupFilename, String applianceFilename) {
    	this.pollerFilename = pollerFilename;
    	this.pollerGroupFilename = pollerGroupFilename;
    	this.applianceFilename = applianceFilename;
    }
	
	@Override
	public InputStream getApplianceConfig() throws IOException {
		FileInputStream is = new FileInputStream(applianceFilename);
		return is;
	}
	@Override
	public InputStream getPollerConfig() throws IOException {
		FileInputStream is = new FileInputStream(pollerFilename);
		return is;
	}

	@Override
	public InputStream getPollerGroupConfig() throws IOException {
		FileInputStream is = new FileInputStream(pollerGroupFilename);
		return is;
	}
}
