package com.solacesystems.semp2es;

import com.solacesystems.semp2es.Result;
import com.solacesystems.semp2es.SempClient;
import com.solacesystems.semp2es.XML2JSON;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Created by koverton on 11/26/2014.
 */
public class SempClientTest {
    private static String sempQuery = "<rpc semp-version=\"soltr/6_0\"><show><stats><client/></stats></show></rpc>";
    @Test
    public void testConnect() throws Exception {
        SempClient client = new SempClient("69.20.234.126", 8034, "esdemo", "esdemoadmin");
        Result<Document> result = client.doRequest(sempQuery);
        Assert.assertNotNull("Tragic null result!", result);
        Assert.assertEquals("Expect HTTP response success", 200, result.getResponseCode());
    }

    @Test
    public void testConvertibleResult() throws Exception {
        SempClient client = new SempClient("69.20.234.126", 8034, "esdemo", "esdemoadmin");
        Result<Document> result = client.doRequest(sempQuery);

        XML2JSON converter = new XML2JSON("/rpc-reply/rpc/show/stats/client/global/stats");
        JSONObject jsonObject = converter.convert(result.getPayload());

        // Validate top-level stats object
        Assert.assertNotNull("stats node was null; should be JSONObject", jsonObject);
        // check for important fields
        for(String fld : new String[] {
                "current-ingress-rate-per-second",
                "current-egress-rate-per-second",
                "current-ingress-byte-rate-per-second",
                "current-egress-byte-rate-per-second"}) {
            Assert.assertTrue(fld + " is a crucial top-level", jsonObject.has(fld));
        }
        // Validate nested object
        Assert.assertNotNull("ingress-discards should be another JSONObject", (JSONObject)jsonObject.get("ingress-discards"));
    }
}
