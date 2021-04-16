package com.solacesystems.semp2es;

import org.json.JSONObject;

/**
 * Created by koverton on 11/26/2014.
 */
class Result<PayloadType> {
    private final int responseCode;
    private final String responseMessage;
    private final PayloadType payload;

    Result(final int responseCode, final String responseMessage, final PayloadType payload) {
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
        this.payload = payload;
    }

    int getResponseCode() {
        return responseCode;
    }

    String getResponseMessage() {
        return responseMessage;
    }

    PayloadType getPayload() {
        return payload;
    }

}
