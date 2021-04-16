package com.solacesystems.semp2es;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.*;

/**
 * Created by koverton on 11/26/2014.
 */
class SempClient extends Authenticator {
    private final String username;
    private final String password;
    private final URL mgmtUrl;
    private final DocumentBuilder builder;

    SempClient(final String mgmtIp, final int httpPort, final String username, final String password) throws Exception {
        this.username = username;
        this.password = password;
        this.mgmtUrl = new URL("http://" + mgmtIp + ":" + httpPort + "/SEMP");
        this.builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Authenticator.setDefault(this);
    }

    @Override
    /// Overrides Authenticator.getPasswordAuthentication
    protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication (username, password.toCharArray());
    }

    Result<Document> doRequest(final String xmlRpcRequest) {
        Document payload = null;
        String responseMsg = null;
        final Result<String> postResult = HttpHelper.doPost(mgmtUrl, xmlRpcRequest);
        try {
            if (HttpHelper.httpSuccess(postResult.getResponseCode())) {
                payload = parseResponse(postResult.getPayload());
                if (payload == null) {
                    responseMsg = postResult.getPayload();
                    System.out.println("Error: " + postResult.getResponseCode() + " " + responseMsg);
                }
            } else {
                responseMsg = postResult.getResponseMessage();
                System.out.println("Error: " + postResult.getResponseCode() + " " + responseMsg + ": " + postResult.getPayload());
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return new Result(postResult.getResponseCode(), responseMsg, payload);
    }

    private Document parseResponse(final String response) {
        try {
            final InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(response));
            Document document = builder.parse(is);
            builder.reset();
            return document;
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
