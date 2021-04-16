package com.solacesystems.semp2es;

import org.json.JSONObject;
import org.json.XML;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import java.io.StringWriter;

/**
 * Created by koverton on 11/26/2014.
 */
class XML2JSON {
    final XPathFactory xPathfactory;
    final XPath xpath;
    final XPathExpression xpathExpr;
    final String objectName;

    XML2JSON(final String xpathQuery) throws XPathExpressionException {
        this.xPathfactory = XPathFactory.newInstance();
        this.xpath = xPathfactory.newXPath();
        this.xpathExpr = xpath.compile(xpathQuery);
        this.objectName = xpathQuery.substring(xpathQuery.lastIndexOf('/')+1);
    }

    JSONObject convert(final Document xmldoc) throws Exception {
        final Node subNode = ((NodeList)xpathExpr.evaluate(xmldoc, XPathConstants.NODESET)).item(0);

        final Transformer transformer= TransformerFactory.newInstance().newTransformer();
        final StreamResult streamResult = new StreamResult(new StringWriter());
        transformer.transform(new DOMSource(subNode), streamResult);

        // XML conversion retains the outer node as an additional layer wrapped around the object, ie,
        //  { 'myObject' : { 'field1':0, 'field2':'booberry', etc. } }
        // so extract the object from that by name, and just return it, ie,
        // { 'field1':0, 'field2':'booberry', etc. }
        final JSONObject theObject = XML.toJSONObject(streamResult.getWriter().toString());
        return (JSONObject)theObject.get(this.objectName);
    }
}
