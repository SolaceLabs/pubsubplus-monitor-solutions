package com.solace.psg.sempv1;

import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solace.psg.sempv1.solacesempreply.ObjectFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.IOException;

public class Session 
{
    private static final Logger logger = LoggerFactory.getLogger(Session.class);

    private String solaceSEMPURI = "http://localhost:8080/SEMP";

    private String solaceSEMPUsername = "admin";;

    private String solaceSEMPPassword = "admin";

    private String solaceSEMPRPCVersion = "<rpc>";

    private int lastHTTPResponseCode;

    private String messageVpn = "default";

    private CloseableHttpClient client;

    private JAXBContext rpcReplyContext = null;

    public Session() 
    {
    }

    public String getSolaceSEMPURI() {
        return solaceSEMPURI;
    }

    public void setSolaceSEMPURI(String solaceSEMPURI) {
        this.solaceSEMPURI = solaceSEMPURI;
    }

    public String getSolaceSEMPUsername() {
        return solaceSEMPUsername;
    }

    public void setSolaceSEMPUsername(String solaceSEMPUsername) {
        this.solaceSEMPUsername = solaceSEMPUsername;
    }

    public String getSolaceSEMPPassword() {
        return solaceSEMPPassword;
    }

    public void setSolaceSEMPPassword(String solaceSEMPPassword) {
        this.solaceSEMPPassword = solaceSEMPPassword;
    }

    public String getSolaceSEMPRPCVersion() {
        return solaceSEMPRPCVersion;
    }

    public void setSolaceSEMPRPCVersion(String solaceSEMPRPCVersion) {
        this.solaceSEMPRPCVersion = solaceSEMPRPCVersion;
    }

    public CloseableHttpResponse execute(String commandXML) throws IOException, AuthenticationException {
        HttpPost httpPost = new HttpPost(solaceSEMPURI);

        httpPost.setEntity(new StringEntity(
                solaceSEMPRPCVersion + commandXML));

        UsernamePasswordCredentials creds
                = new UsernamePasswordCredentials(solaceSEMPUsername, solaceSEMPPassword);
        httpPost.addHeader(new BasicScheme().authenticate(creds, httpPost, null));

        CloseableHttpResponse response = client.execute(httpPost);

        lastHTTPResponseCode = response.getStatusLine().getStatusCode();

        return client.execute(httpPost);
    }

    public void open() {
        client = HttpClients.createDefault();
    }

    public void close() {
        if (client != null) {
            try {
                client.close();
            } catch (Exception ex) {
                logger.error("Error closing client.");
            }
        }
    }

    public JAXBContext getRpcReplyContext() {
        if (rpcReplyContext == null) {
            try {
                rpcReplyContext = JAXBContext.newInstance(ObjectFactory.class);
            } catch (JAXBException e) {
                logger.error("Could not create RPCReply JAXBContext");
            }
        }

        return rpcReplyContext;
    }

    public void setRpcReplyContext(JAXBContext rpcReplyContext)
    {
        this.rpcReplyContext = rpcReplyContext;
    }

    public int getLastHTTPResponseCode() {
        return lastHTTPResponseCode;
    }

    public String getMessageVpn() {
        return messageVpn;
    }

    public void setMessageVpn(String messageVpn) {
        this.messageVpn = messageVpn;
    }
}
