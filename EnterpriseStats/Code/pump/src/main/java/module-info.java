module StatsPump {
	exports com.solace.psg.enterprisestats.statspump.containers.factories;
	exports com.solace.psg.enterprisestats.statspump.tools.semp;
	exports com.solace.psg.enterprisestats.statspump.util.utf8;
	exports com.solace.psg.enterprisestats.statspump.containers;
	exports com.solace.psg.enterprisestats.receiver.stats;
	exports com.solace.psg.enterprisestats.statspump.config;
	exports com.solace.psg.enterprisestats.receiver.sample;
	exports com.solace.psg.enterprisestats.statspump.utils;
	exports com.solace.psg.enterprisestats.statspump.pollers;
	exports com.solace.psg.enterprisestats.receiver.transport;
	exports com.solace.psg.enterprisestats.receiver.influxdb.utils;
	exports com.solace.psg.enterprisestats.statspump.tools;
	exports com.solace.psg.enterprisestats.receiver.elasticsearch;
	exports com.solace.psg.enterprisestats.statspump.semp;
	exports com.solace.psg.enterprisestats.statspump;
	exports com.solace.psg.enterprisestats.statspump.tools.parsers;
	exports com.solace.psg.enterprisestats.statspump.tools.comms;
	exports com.solace.psg.enterprisestats.statspump.stats;
	exports com.solace.psg.enterprisestats.statspump.tools.util;
	exports com.solace.psg.enterprisestats.statspump.util;
	exports com.solace.psg.util;
	exports com.solace.psg.enterprisestats.receiver.influxdb;
	exports com.solace.psg.enterprisestats.receiver.elasticsearch.utils;
	exports com.solace.psg.enterprisestats.statspump.config.xml;
	exports com.solace.psg.enterprisestats.receiver;
	exports com.solace.psg.enterprisestats.receiver.utils;

	requires commons.codec;
	requires commons.lang;
	requires hamcrest.core;
	requires java.xml;
	requires javax.json.api;
	requires json;
	requires junit;
	requires slf4j.api;
	requires sol.jcsmp;
}