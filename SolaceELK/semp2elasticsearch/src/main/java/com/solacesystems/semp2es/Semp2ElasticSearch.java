package com.solacesystems.semp2es;

import org.json.JSONObject;
import org.w3c.dom.Document;

import java.util.Properties;

import static com.solacesystems.semp2es.Semp2ESConfig.*;

/**
 * Created by koverton on 11/26/2014.
 */
public class Semp2ElasticSearch {

    private boolean poll(final SempClient sempClient, final String sempQuery, final ElasticSearchClient searchClient, final XML2JSON converter) {
        boolean result = false;
        try {
            // Retrieve the SEMP stats XML and convert to JSON
            final Result<Document> sempResult = sempClient.doRequest(sempQuery);
            if (HttpHelper.httpSuccess(sempResult.getResponseCode())) {
                // Convert the result to JSON
                final JSONObject jsonObject = converter.convert(sempResult.getPayload());
                if (jsonObject != null) {
                    // Publish JSON to ElasticSearch
                    Result<JSONObject> searchResult = searchClient.indexObject(jsonObject);
                    if (HttpHelper.httpSuccess(searchResult.getResponseCode()))
                        result = true;
                }
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public void run(final String configFile) {
        final Properties config = loadConfig(configFile);
        System.out.println("Running " + this.getClass().getSimpleName() + " with the following properties:");
        config.list(System.out);

        SempClient sempClient = null;
        ElasticSearchClient esClient = null;

        try {
            sempClient = new SempClient(
                    config.getProperty(SEMP_IP),
                    Integer.parseInt(config.getProperty(SEMP_PORT)),
                    config.getProperty(SEMP_USERNAME),
                    config.getProperty(SEMP_PASSWORD));
            esClient = new ElasticSearchClient(
                    config.getProperty(ES_IP),
                    Integer.parseInt(config.getProperty(ES_PORT)),
                    config.getProperty(ES_SOURCE_TYPE));

            final int cycleTimeSeconds = Integer.parseInt(config.getProperty(SEMP2ES_POLL_CYCLE));
            final String sempQuery     = config.getProperty(SEMP_REQUEST);
            final String sempStatsXpath= config.getProperty(SEMP_XPATH);
            final XML2JSON converter   = new XML2JSON(sempStatsXpath);

            final int sleepMillis = 1000 * cycleTimeSeconds;
            long nextPollTime = 0;
            while(true) {
                // Take a starting timestamp
                long pollTime = System.currentTimeMillis();

                // Poll for results, taking a timestamp at the *beginning*
                poll(sempClient, sempQuery, esClient, converter);

                // Sleep until the next poll time
                nextPollTime = pollTime + sleepMillis;
                waitTil(nextPollTime);
            }
        }
        catch(Exception ex) {
            System.out.println("Unexpected exception, exiting ...");
            ex.printStackTrace();
        }
    }

    private static void waitTil(final long nextPollTime)   {
        final long now = System.currentTimeMillis();
        if (now < nextPollTime) {
            try {
                Thread.sleep(nextPollTime - now);
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("\nUSAGE: " + Semp2ElasticSearch.class.getSimpleName() + " <properties-file-name>");
            System.out.println("\t<properties-file-name>: path to a configuration file with overrides to default options");
            System.exit(1);
        }

        new Semp2ElasticSearch().run(args[0]);
    }
}
