/*
 * Copyright (c) 2003, 2011, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.k3a.springboot.reloadvalue.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.InvalidPropertiesFormatException;
import java.util.Iterator;
import java.util.Set;

/**
 * A class used to aid in ConcurrentProps load and save in XML. Keeping this
 * code outside of ConcurrentProps helps reduce the number of classes loaded
 * when ConcurrentProps is loaded.
 *
 * @author  Michael McCloskey
 * @since   1.3
 */
class XMLUtils {

    // XML loading and saving methods for ConcurrentProps

    // The required DTD URI for exported properties
    private static final String PROPS_DTD_URI =
    "http://java.sun.com/dtd/properties.dtd";

    private static final String PROPS_DTD =
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
    "<!-- DTD for properties -->"                +
    "<!ELEMENT properties ( comment?, entry* ) >"+
    "<!ATTLIST properties"                       +
        " version CDATA #FIXED \"1.0\">"         +
    "<!ELEMENT comment (#PCDATA) >"              +
    "<!ELEMENT entry (#PCDATA) >"                +
    "<!ATTLIST entry "                           +
        " key CDATA #REQUIRED>";

    /**
     * Version number for the format of exported properties files.
     */
    private static final String EXTERNAL_XML_VERSION = "1.0";

    static void load(ConcurrentProps props, InputStream in)
        throws IOException, InvalidPropertiesFormatException
    {
        Document doc = null;
        try {
            doc = getLoadingDoc(in);
        } catch (SAXException saxe) {
            throw new InvalidPropertiesFormatException(saxe);
        }
        Element propertiesElement = doc.getDocumentElement();
        String xmlVersion = propertiesElement.getAttribute("version");
        if (xmlVersion.compareTo(EXTERNAL_XML_VERSION) > 0)
            throw new InvalidPropertiesFormatException(
                "Exported ConcurrentProps file format version " + xmlVersion +
                " is not supported. This java installation can read" +
                " versions " + EXTERNAL_XML_VERSION + " or older. You" +
                " may need to install a newer version of JDK.");
        importProperties(props, propertiesElement);
    }

    static Document getLoadingDoc(InputStream in)
        throws SAXException, IOException
    {
	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	dbf.setIgnoringElementContentWhitespace(true);
	dbf.setValidating(true);
        dbf.setCoalescing(true);
        dbf.setIgnoringComments(true);
	try {
	    DocumentBuilder db = dbf.newDocumentBuilder();
	    db.setEntityResolver(new Resolver());
	    db.setErrorHandler(new EH());
            InputSource is = new InputSource(in);
	    return db.parse(is);
	} catch (ParserConfigurationException x) {
	    throw new Error(x);
	}
    }

    static void importProperties(ConcurrentProps props, Element propertiesElement) {
        NodeList entries = propertiesElement.getChildNodes();
        int numEntries = entries.getLength();
        int start = numEntries > 0 && 
            entries.item(0).getNodeName().equals("comment") ? 1 : 0;
        for (int i=start; i<numEntries; i++) {
            Element entry = (Element)entries.item(i);
            if (entry.hasAttribute("key")) {
                Node n = entry.getFirstChild();
                String val = (n == null) ? "" : n.getNodeValue();
                props.setProperty(entry.getAttribute("key"), val);
            }
        }
    }

    static void save(ConcurrentProps props, OutputStream os, String comment,
                     String encoding) 
        throws IOException
    {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        try {
            db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException pce) {
            assert(false);
        }
        Document doc = db.newDocument();
        Element properties =  (Element)
            doc.appendChild(doc.createElement("properties"));

        if (comment != null) {
            Element comments = (Element)properties.appendChild(
                doc.createElement("comment"));
            comments.appendChild(doc.createTextNode(comment));
        }

        Set keys = props.keySet();
        Iterator i = keys.iterator();
        while(i.hasNext()) {
            String key = (String)i.next();
            Element entry = (Element)properties.appendChild(
                doc.createElement("entry"));
            entry.setAttribute("key", key);
            entry.appendChild(doc.createTextNode(props.getProperty(key)));
        }
        emitDocument(doc, os, encoding);
    }

    static void emitDocument(Document doc, OutputStream os, String encoding)
        throws IOException
    {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer t = null;
        try {
            t = tf.newTransformer();
            t.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, PROPS_DTD_URI);
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.setOutputProperty(OutputKeys.METHOD, "xml");
            t.setOutputProperty(OutputKeys.ENCODING, encoding);
        } catch (TransformerConfigurationException tce) {
            assert(false);
        }
        DOMSource doms = new DOMSource(doc);
        StreamResult sr = new StreamResult(os);
        try {
            t.transform(doms, sr);
        } catch (TransformerException te) {
            IOException ioe = new IOException();
            ioe.initCause(te);
            throw ioe;
        }
    }

    private static class Resolver implements EntityResolver {
        public InputSource resolveEntity(String pid, String sid)
            throws SAXException
        {
            if (sid.equals(PROPS_DTD_URI)) {
                InputSource is;
                is = new InputSource(new StringReader(PROPS_DTD));
                is.setSystemId(PROPS_DTD_URI);
                return is;
            }
            throw new SAXException("Invalid system identifier: " + sid);
        }
    }

    private static class EH implements ErrorHandler {
        public void error(SAXParseException x) throws SAXException {
            throw x;
        }
        public void fatalError(SAXParseException x) throws SAXException {
            throw x;
        }
        public void warning(SAXParseException x) throws SAXException {
            throw x;
        }
    }

}
