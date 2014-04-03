/*******************************************************************************
 * Copyright (C) 2006-2013 AITIA International, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package ai.aitia.meme.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import ai.aitia.meme.MEMEApp;

/** Collection of XML related utility functions. */
public class XMLUtils {

	public static DocumentBuilder				g_DefaultXMLParser	= null;
	public static Transformer					g_DefaultXMLWriter	= null;
	
	//-------------------------------------------------------------------------
	/** Returns the default parser. */ 
	public static DocumentBuilder getDefaultXMLParser() {
		if (g_DefaultXMLParser == null) {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			try {
				g_DefaultXMLParser = factory.newDocumentBuilder();
			} catch (ParserConfigurationException e) {
				MEMEApp.logExceptionCallStack("getXMLParser()", e);
			}
		}
		return g_DefaultXMLParser;
	}

	//-------------------------------------------------------------------------
	/** Returns the default writer. */
	public static Transformer getDefaultXMLWriter() {
		if (g_DefaultXMLWriter == null) {
			try {
				g_DefaultXMLWriter = TransformerFactory.newInstance().newTransformer();
			} catch (Throwable t) {
				MEMEApp.logExceptionCallStack("getXMLWriter()", t);
			}
		}
		return g_DefaultXMLWriter;
	}

	//-------------------------------------------------------------------------
	/** Parses an XML-document from 'stream'. Returns the root of the document. 
	 *  If 'closeIt' is true, the method closes the stream before returns. 
	 */
	public static Element parse(java.io.Reader stream, boolean closeIt) throws SAXException, IOException {
		try { 
			Document d = getDefaultXMLParser().parse(new org.xml.sax.InputSource(stream));
			return d.getDocumentElement();
		} finally {
			if (closeIt && stream != null) stream.close();
		}
	}

	//-------------------------------------------------------------------------
	/** Parses an XML-document from 's'. Returns the root of the document. 
	 */
	public static Element parse(String s) throws Exception {
		java.io.LineNumberReader stream = new java.io.LineNumberReader(new java.io.StringReader(s));  
		try {
			return parse(stream, true);
		} catch (Exception e) {
			throw new Exception(String.format("in line %d: %s", stream.getLineNumber(), Utils.getLocalizedMessage(e)), e);
		}
	}

	//-------------------------------------------------------------------------
	/** Loads an XML-document specified by 'resource'. Returns the root of the document.  */
	public static Element load(java.net.URI resource) throws Exception {
		Document document = getDefaultXMLParser().parse(resource.toString());
		return document.getDocumentElement();
	
//		java.io.InputStream is = (resource.isAbsolute()) ? resource.toURL().openStream()
//														: MEMEApp.class.getResourceAsStream(resource.toString());
//		if (is == null)
//			return null;
//		java.io.LineNumberReader stream = new java.io.LineNumberReader(new InputStreamReader(is));
//		try {
//			return parse(stream, true);
//		} catch (Exception e) {
//			throw new Exception(String.format("in line %d: %s", stream.getLineNumber(), Utils.getLocalizedMessage(e)), e);
//		}
	}

	//-------------------------------------------------------------------------
	/** Writes 'node' to 'w'. Does not close the stream.
	 * @return w
	 */
	public static java.io.Writer write(java.io.Writer w, Node node) throws TransformerException {
		javax.xml.transform.Source source = new javax.xml.transform.dom.DOMSource( node );
		javax.xml.transform.Result output = new javax.xml.transform.stream.StreamResult( w );
		getDefaultXMLWriter().transform( source, output );
		return w;
	}

	//-------------------------------------------------------------------------
	/** Writes 'node' to file 'file'. */
	public static void write(java.io.File file, Node node) throws TransformerException, IOException {
		javax.xml.transform.Source source = new javax.xml.transform.dom.DOMSource(node);
		java.io.FileOutputStream os = new java.io.FileOutputStream(file);
		javax.xml.transform.Result output = new javax.xml.transform.stream.StreamResult(os);
		getDefaultXMLWriter().transform(source,output);
		os.close();
	}

	//-------------------------------------------------------------------------
	public static String toString(Node node) throws TransformerException {
		java.io.StringWriter w = new java.io.StringWriter();
		write(w, node);
		return w.toString();
	}

	//-------------------------------------------------------------------------
//	public static void writeProperties(Node n, Properties p) {
//		Document document = n.getOwnerDocument();
//		for (java.util.Map.Entry<Object, Object> it : p.entrySet()) {
//			Element e = document.createElement("property");
//			e.setAttribute("key", it.getKey().toString());
//			e.setTextContent(it.getValue().toString());
//			n.appendChild(e);
//		}
//	}

	//-------------------------------------------------------------------------
	/** 
	 * This function reads data from an XML <code>node</code> into a newly created
	 * Properties object.
	 * @throws XMLLoadingException if there is no child nodes of <code>node</code>.
	 */
	public static Properties readProperties(Node node) {
		Properties prop = new Properties();
		NodeList nodes = node.getChildNodes();
		if (nodes != null) {
			for (int i = 0, n = nodes.getLength(); i < n; ++i) {
				if (!(nodes.item(i) instanceof Element)) continue;
				Element element = (Element)nodes.item(i);
				if (!element.getTagName().equals("property")) continue;
				prop.setProperty(element.getAttribute("key"), getText(element));
			}
		}
		return prop;
	}

	//-------------------------------------------------------------------------
	/** 
	 * Returns the concatenated contents of all children of type Text.
	 * Non-Text descendants are ignored. Never returns null.
	 * @see Node.getTextContent()
	 */ 
	public static String getText(Node node) {
		StringBuilder sb = new StringBuilder();
	    NodeList children = (node == null) ? null : node.getChildNodes();
	    if (children != null) {
		    for(int i = 0, n = children.getLength(); i < n; ++i) {
		       Node child = children.item(i);
		       if (child instanceof Text)
		    	   sb.append(((Text)child).getNodeValue());
		    }
	    }
	    return sb.toString();
	}

	//-------------------------------------------------------------------------
	/**
	 * Creates a Text child, or replaces all existing children of type Text.
	 * Non-Text descendants are preserved without change.
	 * @return The Text child which receives the specified text. 
	 * @see Node.setTextContent()
	 */
	public static Text setText(Node node, String text) {
	    NodeList children = node.getChildNodes();
	    if (children != null) {
		    for(int i = 0, n = children.getLength(); i < n; ++i) {
		       Node child = children.item(i);
		       if (child instanceof Text) {
		    	   return ((Text)child).replaceWholeText(text);
		       }
		    }
	    }
		Text ans = node.getOwnerDocument().createTextNode(text);
	    node.appendChild(ans);
	    return ans;
	}

	//-------------------------------------------------------------------------
	/** @param text <code>null</code> removes 'subElement' completely */ 
	public static Text setTextField(Element node, String subElement, String text) {
		NodeList nl = node.getElementsByTagName(subElement);
		if (nl == null || nl.getLength() == 0) {
			if (text == null) return null;
			Document d = node.getOwnerDocument();
			Element e = d.createElement(subElement);
			Text ans = d.createTextNode(text);
			e.appendChild(ans);
			node.appendChild(e);
			return ans;
		} else if (text != null) {
			return setText(nl.item(0), text); 
		} else {
			node.removeChild(nl.item(0));
			return null;
		}
	}

	//-------------------------------------------------------------------------
	/** Returns the text value of 'subElement'. */
	public static String getTextField(Element node, String subElement, boolean mayBeNull) {
		NodeList nl = node.getElementsByTagName(subElement);
		if (nl == null || nl.getLength() == 0)
			return mayBeNull ? null : "";
		return getText(nl.item(0)); 
	}

	//-------------------------------------------------------------------------
	/** Returns the text value of 'subElement'. Never returns null */
	public static String getTextField(Element node, String subElement) {
		return getTextField(node, subElement, false);
	}
	
	//-------------------------------------------------------------------------
	/** Returns the first 'subElement' of 'node'. */
	public static Element findFirst(Element node, String subElement) {
		Element ans = null;
		if (node != null) {
			NodeList nl = node.getElementsByTagName(subElement);
			if (nl != null && nl.getLength() > 0)
				ans = (Element)nl.item(0);
		}
		return ans;
	}

	//-------------------------------------------------------------------------
	/** Returns all 'subElement' of 'node'. */
	public static List<Element> findAll(Element node, String subElement) {
		if (node != null) {
			NodeList nl = node.getElementsByTagName(subElement);
			if (nl != null && nl.getLength() > 0) {
				int n = nl.getLength(); 
				ArrayList<Element> ans = new ArrayList<Element>(n);
				for (int i = 0; i < n; ++i)
					ans.add((Element)nl.item(i));
				return ans;
			}
		}
		return java.util.Collections.<Element>emptyList();
	}
	
	//-------------------------------------------------------------------------
	public static void removeAll(Element node, String subElement) {
		for (Element e : findAll(node, subElement))
			node.removeChild(e);
	}

	//-------------------------------------------------------------------------
	public static Element clear(Element node, String subElement) {
		Element child = findFirst(node, subElement);
		if (child == null) {
			child = node.getOwnerDocument().createElement(subElement);
			node.appendChild(child);
		}
		child.setTextContent(null);					// remove all descendants
		return child;
	}

}
