package com.solacesystems.semp2es;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

/**
 * Created by koverton on 11/26/2014.
 */
class Semp2ESConfig {
    public static String SEMP_IP           = "semp_ip";
    public static String SEMP_PORT         = "semp_port";
    public static String SEMP_USERNAME     = "semp_username";
    public static String SEMP_PASSWORD     = "semp_password";
    public static String SEMP_REQUEST      = "semp_request";
    public static String SEMP_XPATH        = "semp_xpath";
    public static String ES_IP             = "es_ip";
    public static String ES_PORT           = "es_port";
    public static String ES_SOURCE_TYPE    = "es_source_type";
    public static String SEMP2ES_POLL_CYCLE= "semp2es_poll_cycle";

    private static String getIndexName() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy.MM.dd");
        String name = "semp-" + df.format(new Date());
        return name;
    }

    static void loadDefaultConfig(Properties config) {
        config.setProperty(Semp2ESConfig.SEMP_IP, "69.20.234.126");
        config.setProperty(Semp2ESConfig.SEMP_PORT, "8034");
        config.setProperty(Semp2ESConfig.SEMP_USERNAME, "esdemo");
        config.setProperty(Semp2ESConfig.SEMP_PASSWORD, "esdemoadmin");
        config.setProperty(Semp2ESConfig.SEMP_REQUEST, "<rpc semp-version=\"soltr/6_0\"><show><stats><client/></stats></show></rpc>");
        config.setProperty(Semp2ESConfig.SEMP_XPATH, "/rpc-reply/rpc/show/stats/client/global/stats");

        config.setProperty(Semp2ESConfig.ES_IP, "127.0.0.1");
        config.setProperty(Semp2ESConfig.ES_PORT, "9200");
        config.setProperty(Semp2ESConfig.ES_SOURCE_TYPE, "stats");

        config.setProperty(Semp2ESConfig.SEMP2ES_POLL_CYCLE, "60");
    }

    static Properties loadConfig(String configFileName) {
        Properties config = new Properties();
        loadDefaultConfig(config);
        try {
            FileInputStream in = new FileInputStream(configFileName);
            config.load(in);
            in.close();
        }
        catch(FileNotFoundException fnfex) {
            System.out.println("File {"+configFileName+"} not found; using all default configurations.");
        }
        catch(IOException ioex) {
            ioex.printStackTrace();
        }
        return config;
    }

}
