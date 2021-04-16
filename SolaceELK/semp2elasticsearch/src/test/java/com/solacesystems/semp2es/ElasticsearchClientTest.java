package com.solacesystems.semp2es;

import com.solacesystems.semp2es.ElasticSearchClient;
import com.solacesystems.semp2es.Result;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Created by koverton on 11/26/2014.
 */
public class ElasticsearchClientTest {
    static final String statsString = "{"
            + "\"current-ingress-rate-per-second\": 10,"
            + "\"client-non-persistent-bytes-received\": 9,"
            + "\"current-ingress-byte-rate-per-second\": 8"
            + "}";

    @Ignore
    public void endToEndTest() throws Exception {
        ElasticSearchClient client = new ElasticSearchClient("127.0.0.1", 9200, "stats");
        Result<JSONObject> result = client.indexObject(new JSONObject(statsString));
        Assert.assertEquals("HTTP result must be 201 Created", 201, result.getResponseCode());
        System.out.println("RESULT:" + result.getPayload().toString());
    }
}
