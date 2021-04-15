package com.solace.psg.enterprisestats.statspump.config;
import java.io.IOException;
import java.io.InputStream;

public interface StatsConfigStreams {
	InputStream getApplianceConfig() throws IOException;
	InputStream getPollerConfig() throws IOException;
	InputStream getPollerGroupConfig() throws IOException;
}
