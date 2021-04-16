package com.solacesystems.semp2es;

import com.solacesystems.semp2es.XML2JSON;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by koverton on 11/26/2014.
 */
public class XML2JSONTest {

    private static Document file2doc(File xmlfile) throws IOException {
            System.out.println("Working Directory: "+System.getProperty("user.dir"));
            FileInputStream fis = new FileInputStream(xmlfile);
            byte[] xmlbytes = new byte[(int)xmlfile.length()];
            fis.read(xmlbytes);
            fis.close();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document xmldoc = null;
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            xmldoc = builder.parse(new ByteArrayInputStream(xmlbytes));
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return xmldoc;
    }

    @Test
    public void extractTopLevelObjectTest() throws Exception {
        XML2JSON converter = new XML2JSON("/rpc-reply/rpc/show/stats/client/global/stats");
        Document xmldoc = file2doc(new File("resources/test/show.stats.client.detail.xml"));
        JSONObject jsonObject = converter.convert(xmldoc);

        // Validate top-level stats object
        Assert.assertNotNull("stats node was null; should be JSONObject", jsonObject);
    }

    @Test
    public void integerFieldTest() throws Exception {
        XML2JSON converter = new XML2JSON("/rpc-reply/rpc/show/stats/client/global/stats");
        Document xmldoc = file2doc(new File("resources/test/show.stats.client.detail.xml"));
        JSONObject jsonObject = converter.convert(xmldoc);
        // Validate integer field
        Assert.assertEquals("client-control-messages-received is a top-level integer field", 3, jsonObject.getInt("client-control-messages-received"));
    }

    @Test
    public void nestedObjectTest() throws Exception {
        XML2JSON converter = new XML2JSON("/rpc-reply/rpc/show/stats/client/global/stats");
        Document xmldoc = file2doc(new File("resources/test/show.stats.client.detail.xml"));
        JSONObject jsonObject = converter.convert(xmldoc);
        // Validate nested object
        Assert.assertNotNull("ingress-discards should be another JSONObject", (JSONObject) jsonObject.get("ingress-discards"));
    }

    @Test
    public void xpathElidingTest() throws Exception {
        XML2JSON converter = new XML2JSON("//global/stats");
        Document xmldoc = file2doc(new File("resources/test/show.stats.client.detail.xml"));
        JSONObject jsonObject = converter.convert(xmldoc);
        // Validate nested object
        Assert.assertNotNull("ingress-discards should be another JSONObject", (JSONObject) jsonObject.get("egress-discards"));
    }

    @Test
    public void doubleFieldTest() throws Exception {
        XML2JSON converter = new XML2JSON("/rpc-reply/rpc/show/stats/client/global/stats/zip-stats");
        Document xmldoc = file2doc(new File("resources/test/show.stats.client.detail.xml"));
        JSONObject jsonObject = converter.convert(xmldoc);
        Assert.assertEquals("Zip-stats floating-point field", 1.14754, jsonObject.getDouble("ingress-compression-ratio"), 0.000001);
    }
}
