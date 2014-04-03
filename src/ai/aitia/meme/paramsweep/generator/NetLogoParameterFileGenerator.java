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
package ai.aitia.meme.paramsweep.generator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.util.Enumeration;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ai.aitia.meme.paramsweep.gui.info.ChooserParameterInfo;
import ai.aitia.meme.paramsweep.gui.info.ParameterInfo;
import ai.aitia.meme.paramsweep.utils.ParameterEnumeration;

public class NetLogoParameterFileGenerator {

	//====================================================================================================
	// members
	
	public static final String SWEEP 			= "sweep";
	public static final String RUNS	 			= "runs";
	public static final String PARAMETER 		= "parameter";
	public static final String NAME				= "name";
	public static final String TYPE				= "type";
	public static final String DEFINITION_TYPE	= "definition_type";
	public static final String VALUES			= "values";
	public static final String VALUE			= "value";
	public static final String START			= "start";
	public static final String END				= "end";
	public static final String STEP				= "step";
	public static final String VALID_VALUES		= "valid_values";
	
	private File dest = null;

	//====================================================================================================
	// methods
	
	public NetLogoParameterFileGenerator(File dest) {
		if (dest == null)
			throw new IllegalArgumentException();
		this.dest = dest;
	}
	
	//----------------------------------------------------------------------------------------------------
	public void generateFile(DefaultMutableTreeNode root) throws IOException {
		try {
			Document document = createXmlDocumentFromTree(root);
		
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			transformerFactory.setAttribute("indent-number",4);
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT,"yes");
			Source source = new DOMSource(document);
			FileOutputStream os = new FileOutputStream(dest);
			Result result = new StreamResult(new OutputStreamWriter(os,"utf-8"));
			transformer.transform(source,result);
		} catch (TransformerConfigurationException e) {
			throw new IOException(e);
		} catch (TransformerException e) {
			throw new IOException(e);
		} catch (ParserConfigurationException e) {
			throw new IOException(e);
		}
	}

	//----------------------------------------------------------------------------------------------------
	public static String generateStringRepresentation(DefaultMutableTreeNode root) throws ParserConfigurationException, TransformerException {
		Document document = createXmlDocumentFromTree(root);
		
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		transformerFactory.setAttribute("indent-number",4);
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT,"yes");
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,"yes");
		
		Source source = new DOMSource(document);
		StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        transformer.transform(source,result);
        
        return sw.toString();
	}
	
	//--------------------------------------------------------------------------------
	/** Creates a new (parameter) file object. */
	public static String generateEmptyFilePath(String modelFileName) {
		File f = new File(modelFileName);
		String path = f.getParentFile().getAbsolutePath();
		String name = f.getName() + "_parameters";
		File res = new File(path + File.separator + name + ".xml");
		int number = 0;
		while (res.exists()) 
			res = new File(path + File.separator + name + String.valueOf(number++) + ".xml");
		return res.getAbsolutePath();
	}
	
	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	private static Document createXmlDocumentFromTree(DefaultMutableTreeNode root) throws ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder parser = factory.newDocumentBuilder();
		Document document = parser.newDocument();
		
		Element sweepElement = document.createElement(SWEEP);
		
		Enumeration<DefaultMutableTreeNode> e = new ParameterEnumeration(root);
		DefaultMutableTreeNode prev = null;
		Element prevElement = null;
		boolean first = true;
		while (e.hasMoreElements()) {
			DefaultMutableTreeNode node = e.nextElement();
			if (node.equals(root)) continue;
			ParameterInfo info = (ParameterInfo) node.getUserObject();
			if (first) {
				sweepElement.setAttribute(RUNS,String.valueOf(info.getRuns()));
				first = false;
			}
			Element generatedElement = generateElement(document,info);
			if (node.getParent().equals(root)) {
				sweepElement.appendChild(generatedElement);
			} else {
				if (!prev.equals(node.getParent())) {
					// currently we don't use this branch
					throw new IllegalStateException();
				}
				prevElement.appendChild(generatedElement);
			}
			prev = node;
			prevElement = generatedElement;
		}
		document.appendChild(sweepElement);
		return document;
	}
	
	//----------------------------------------------------------------------------------------------------
	private static Element generateElement(Document document, ParameterInfo info) {
		Element element = document.createElement(PARAMETER);
		element.setAttribute(NAME,info.getName());
		element.setAttribute(TYPE,info.getType());
		element.setAttribute(DEFINITION_TYPE,ParameterInfo.defTypeToString(info.getDefinitionType()));
		
		if (info instanceof ChooserParameterInfo) {
			ChooserParameterInfo ci = (ChooserParameterInfo) info;
			element.setAttribute(VALID_VALUES,ci.validValuesToString());
		}
		
		if (info.getDefinitionType() == ParameterInfo.CONST_DEF)
			element.setAttribute(VALUE,info.valuesToString()); // there is only one value in the list
		else if (info.getDefinitionType() == ParameterInfo.LIST_DEF)
			element.setAttribute(VALUES,info.valuesToString());
		else {
			element.setAttribute(START,info.startToString());
			element.setAttribute(END,info.endToString());
			element.setAttribute(STEP,info.stepToString());
		}
		return element;
	}
}
