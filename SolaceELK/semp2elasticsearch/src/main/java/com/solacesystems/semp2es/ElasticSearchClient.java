package com.solacesystems.semp2es;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by koverton on 11/26/2014.
 */
class ElasticSearchClient {
    private final String httpIp;
    private final int httpPort;
    private final String typeName;
    private final SimpleDateFormat tsfmt;
    private final SimpleDateFormat indexfmt;
    private URL addItemURL;
    private URL indexURL;
    private String indexName;
    private Date lastDate;

    ElasticSearchClient(final String httpIp, final int httpPort, final String typeName) throws MalformedURLException {
        this.httpIp = httpIp;
        this.httpPort = httpPort;
        this.typeName = typeName;
        // formatting for timestamp creation
        this.tsfmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        this.tsfmt.setTimeZone(TimeZone.getTimeZone("UTC"));
        this.indexfmt = new SimpleDateFormat("'semp-'yyyy.MM.dd");
        this.indexfmt.setTimeZone(TimeZone.getTimeZone("UTC"));
        checkForDateRoll();
    }

    private void checkForDateRoll() throws MalformedURLException {
        Date now = new Date();
        String newIndexName = getIndexName(now);
        if (indexName == null || !indexName.equals(newIndexName)) {
            this.indexName = getIndexName(new Date());
            this.addItemURL = new URL("http://" + httpIp + ":" + httpPort + "/" + indexName + "/" + typeName + "/");
            this.indexURL   = new URL("http://" + httpIp + ":" + httpPort + "/" + indexName);
        }
        this.lastDate = now;
        if (!checkIndex()) createIndex();
    }

    private String getIndexName(Date dt) {
        return indexfmt.format(dt);
    }

    private boolean checkIndex() throws MalformedURLException {
        final Result<String> result = HttpHelper.doHead(indexURL);
        final boolean isSuccess = HttpHelper.httpSuccess(result.getResponseCode());
        if (!isSuccess) {
            System.out.println("Failed checking index {" + indexURL.toString() + "}: " + result.getResponseMessage());
        }
        return isSuccess;
    }

    private Result<JSONObject> createIndex() throws MalformedURLException {
        JSONObject payload = null;

        final Result<String> putResult = HttpHelper.doPut(indexURL);
        if (HttpHelper.httpSuccess(putResult.getResponseCode())) {
            try {
                payload = new JSONObject(putResult.getPayload());
            }
            catch(JSONException je) {
                je.printStackTrace();
                payload = null;
            }
        }
        return new Result(putResult.getResponseCode(), putResult.getResponseMessage(), payload);
    }

    Result<JSONObject> indexObject(final JSONObject data) {
        JSONObject payload = null;

        // Make sure index for this data exists (on date roll, create a new index)
        try {
            checkForDateRoll();
        }
        catch(MalformedURLException ex) {
            return new Result(400, ex.getMessage(), payload);
        }
        // Add a iso-8601 GMT timestamp field
        data.put("@timestamp", tsfmt.format(this.lastDate));

        final Result<String> postResult = HttpHelper.doPost(addItemURL, data.toString());
        if (HttpHelper.httpSuccess(postResult.getResponseCode())) {
            try {
                payload = new JSONObject(postResult.getPayload());
            }
            catch(JSONException je) {
                je.printStackTrace();
                payload = null;
            }
        }
        return new Result(postResult.getResponseCode(), postResult.getResponseMessage(), payload);
    }
}
