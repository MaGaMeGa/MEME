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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import javassist.CtClass;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import ai.aitia.meme.paramsweep.ParameterSweepWizard;
import ai.aitia.meme.paramsweep.gui.Page_IntelliExtension;
import ai.aitia.meme.paramsweep.gui.Page_LoadModel;
import ai.aitia.meme.paramsweep.gui.Page_Parameters;
import ai.aitia.meme.paramsweep.gui.Page_ParametersV2;
import ai.aitia.meme.paramsweep.gui.Page_Recorders;
import ai.aitia.meme.paramsweep.platform.PlatformManager;
import ai.aitia.meme.paramsweep.platform.PlatformManager.PlatformType;
import ai.aitia.meme.paramsweep.utils.WizardLoadingException;

/** This class handles the save/load of the model settings. It saves the settings into
 *  an XML file. The structure of the XML is changed during the development so we assigned
 *  a version number to it. Further changes is expected in the near future.
 *  The history of the XML structure is the following:<br>
 *  <ul>
 *  <li>1.0  base structure</li>
 *  <li>1.1  writing_time nodes</li>
 *  <li>1.2  description nodes</li>
 *  <li>1.3  resources & r_element nodes</li>
 *  <li>1.4  need_generation node</li>
 *  <li>1.5  script - statistics</li>
 *  <li>1.6  script - basic scripts</li>
 *  <li>1.7  intellisweep support</li>
 *  <li>1.8  parameters, members and scripts contains javaType information (former settings files may cause loading failure)</li>
 *  <li>1.9  script - operators </li> 
 *  <li>1.10 script - inner scripts </li>
 *  <li>1.11 alias names for the recordable elements </li>
 *  <li>1.12 editable scripts and parent for inner scripts </li>
 *  <li>2.0  Multi-platform Parameter Sweep Wizard </li> 
 *  <li>2.1	 automatic resources </li>
 *  <li>2.2	 script - netlogo statistics </li>
 *  <li>2.3  script - operators with assistant methods </li>
 *  <li>2.4	 script - user variables </li>
 *  </ul>
 */
public class WizardSettingsManager {
	
	//=================================================================================
	// constants
	
	/** XML structure constant: root node. */
	public static final String SETTINGS 			= "settings";
	/** XML structure constant: version attribute. */
	public static final String VERSION				= "version";
	/** XML structure constant: node belongs to the page Model Selection. */
	public static final String LOAD_PAGE			= "load_page";
	/** XML structure constant: node belongs to the page Parameters. */
	public static final String PARAMETERS_PAGE		= "parameters_page";
	/** XML structure constant: node belongs to the page Data Collection. */
	public static final String RECORDERS_PAGE		= "recorders_page";
	/** XML structure constant: node belongs to the page IntelliSweep Extension. */
	public static final String INTELLIEXTENSION_PAGE		= "intellisweep_page";
	
	/** XML structure constant: 'path of the original model class file' node. */
	public static final String MODEL_FILE			= "model_file";
	public static final String MODEL_ROOT			= "model_root";
	
	/** XML structure constant: 'path of the parameter file' node. */
	public static final String PARAMETER_FILE		= "parameter_file";
	/** XML structure constant: 'name of the generated model' node. */
	public static final String GENERATED_MODEL		= "generated_model_name";
	/** XML structure constant: 'classpath list' node. */
	public static final String CLASSPATH			= "classpath";
	/** XML structure constant: 'element of the classpath' node. */
	public static final String CLASSPATH_ELEMENT	= "cp_element";
	/** XML structure constant: 'resource list' node. */
	public static final String RESOURCES			= "resources";
	/** XML structure constant: 'path of a resource file' node. */
	public static final String RESOURCE_ELEMENT		= "r_element";
	public static final String AUTO_RESOURCE_ELEMENT = "ar_element";
	
	/** XML structure constant: 'source of a statistic instance or script' node. */
	public static final String SOURCE				= "source";
	/** XML structure constant: 'description of the model' node. */
	public static final String DESCRIPTION			= "description";
	
	/** XML structure constant: 'new parameters list' node. */
	public static final String NEW_PARAMETERS		= "new_parameters";
	/** XML structure constant: 'new parameter' node. */
	public static final String PARAMETER			= "parameter";
	/** XML structure constant: type node or attribute. */
	public static final String TYPE					= "type";
	
	/** XML structure constant: 'simulation stopping condition' node. */
	public static final String STOP_DATA			= "stop_data";
	/** XML structure constant: 'The simulation stopping condition is a logical condition?' attribute. */
	public static final String IS_CONDITION			= "condition";
	/** XML structure constant: 'recorders list' node. */
	public static final String RECORDERS			= "recorders";
	/** XML structure constant: 'recorder' node. */
	public static final String RECORDER				= "recorder";
	/** XML structure constant: name node or attribute. */
	public static final String NAME					= "name";
	/** XML structure constant: 'output file of a recorder' node. */
	public static final String OUTPUT_FILE			= "output";
	/** XML structure constant: 'recording time of a recorder' node. */
	public static final String TIME					= "time";
	/** XML structure constant: 'writing time of a recorder' node. */
	public static final String WRITING_TIME			= "writing_time";
	/** XML structure constant: 'recordable element of a recorder' node. */
	public static final String MEMBER				= "member";
	/** XML structure constant: 'generated statistic instance or script list' node. */
	public static final String SCRIPTS				= "scripts";
	/** XML structure constant: 'generated statistic instance or script' node. */
	public static final String SCRIPT				= "script";
	/** XML structure constant: 'displayable name of a statistic instance' node. */
	public static final String CALL					= "call";
	/** XML structure constant: 'referenced generated statistic instance or script list' node. */
	public static final String REFERENCES			= "references";
	/** XML structure constant: 'import declarations' node. */
	public static final String IMPORTS				= "imports";
	public static final String JAVA_TYPE			= "java_type";
	public static final String DISPLAY_NAME			= "display_name";
	public static final String INNER				= "inner";
	public static final String MULTICOLUMN_LENGTH	= "multicolumn_length";
	public static final String ALIAS				= "alias";
	public static final String AGGREGATE			= "alias"; // WTF???
	
	public static final String SYNTAX_SOURCE		= "syntax_source";
	public static final String REPORT_SOURCE		= "report_source";
	
	public static final String BUILD_BLOCKS			= "build_blocks";
	public static final String BLOCK				= "block";
	public static final String BLOCK_ELEMENT		= "block_element";
	public static final String CLASS				= "class";
	public static final String INT					= "int";
	public static final String CONSTANT				= "constant";
	public static final String CONSTANT_KEY			= "constant_key";
	public static final String STRING				= "string";
	public static final String METHOD				= "method";
	public static final String INNER_TYPE			= "inner_type";
	public static final String PARENT				= "parent";
	public static final String GENERATOR_NAME		= "generator_name";
	public static final String PARAMETER_TYPE		= "parameter_type";
	public static final String SIMULATION_PLATFORM	= "simulation_platform";
	
	public static final String ASSISTANT_METHODS	= "assistant_methods";
	public static final String RETURN_TYPE			= "return_type";
	public static final String SCHEDULE_TIME		= "schedule_time";
	
	public static final String USER_VARIABLES		= "user_variables";
	public static final String VARIABLE				= "variable";
	public static final String DEFAULT_INITIALIZED	= "default_initialized";
	public static final String PARENT_VARIABLE		= "parent_variable";
	
	//=================================================================================
	// members
	
	/** Major version number of the XML structure. */
	private static final int MAJOR_VERSION = 2;
	/** Minor version number of the XML structure. */
	private static final int MINOR_VERSION = 4;
	
	/** Owner of the manager. */
	private ParameterSweepWizard owner = null;
	/** Reference to the page Model selection. */
	private Page_LoadModel loadPage = null;
	/** Reference to the page Parameters. */
	private Page_ParametersV2 parametersPage = null;
	/** Reference to the page Data Collection. */
	private Page_Recorders recordersPage = null;
	private Page_IntelliExtension intelliExtensionPage = null;
	private String settingsFile = null;
	
	//=================================================================================
	// methods
	
	//---------------------------------------------------------------------------------
	/** Constructor.
	 * @param owner owner of the manager
	 * @param loadPage reference to the page Model Selection
	 * @param parameterPage reference to the page Parameters
	 * @param recordersPage reference to the page Data Collection
	 */
	public WizardSettingsManager(ParameterSweepWizard owner, Page_LoadModel loadPage, Page_ParametersV2 parameterPage, Page_Recorders recordersPage,
								 Page_IntelliExtension intelliExtensionPage) {
		this.owner = owner;
		this.loadPage = loadPage;
		this.parametersPage = parameterPage;
		this.recordersPage = recordersPage;
		this.intelliExtensionPage = intelliExtensionPage;
	}
	
	//---------------------------------------------------------------------------------
	/** Saves all model settings to an XML file.
	 * @throws ParserConfigurationException
	 * @throws TransformerException
	 */
	public void save() throws ParserConfigurationException, TransformerException, FileNotFoundException {
		String path = ParameterSweepWizard.getPreferences().getSettingsPath();
		String name = owner.getGeneratedModelName() + ".settings.xml";
		File file = new File(path + File.separator + name);
		settingsFile = file.getAbsolutePath();
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder parser = factory.newDocumentBuilder();
		Document document = parser.newDocument();

		createDocument(document);
		
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		Source source = new DOMSource(document);
		FileOutputStream os = new FileOutputStream(file);
		Result result = new StreamResult(os);
		transformer.transform(source,result);
	}
	
	//--------------------------------------------------------------------------------
	/** Loads model settings from an XML file to the Parameter Sweep Wizard.
	 * @param uri the path of the XML file. URI is a more general type than File. With
	 *            this type the method can loads an XML-file contained by a jar.
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws WizardLoadingException if the XML-file is invalid
	 */
	public void load(URI uri) throws ParserConfigurationException, SAXException, IOException, WizardLoadingException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder parser = factory.newDocumentBuilder();
		Document document = parser.parse(uri.toString());
		
		Element rootElement = document.getDocumentElement();
		String version = rootElement.getAttribute(VERSION);
		if (isOld(version)) {
			version = version.trim();
			if (version.equals("")) 
				throw new WizardLoadingException(false,"missing 'version' attribute");
			String[] parts = version.split("\\.");
			if (parts.length != 2) 
				throw new WizardLoadingException(false,"invalid 'version' attribute");
			try {
				int major = Integer.parseInt(parts[0]);
				int minor = Integer.parseInt(parts[1]);
				patch(new File(uri).getPath(),rootElement,major,minor);
			} catch (NumberFormatException e) {
				throw new WizardLoadingException(false,"invalid 'version' attribute");
			}
		}
		owner.load(rootElement);
		
		NodeList nodes = rootElement.getElementsByTagName(LOAD_PAGE);
		if (nodes == null || nodes.getLength() == 0)
			throw new WizardLoadingException(true,"missing node: " + LOAD_PAGE);
		Element loadElement = (Element) nodes.item(0);
		loadPage.load(loadElement);
		
		nodes = null;
		nodes = rootElement.getElementsByTagName(PARAMETERS_PAGE);
		if (nodes == null || nodes.getLength() == 0)
			throw new WizardLoadingException(true,"missing node: " + PARAMETERS_PAGE);
		Element parametersElement = (Element) nodes.item(0);
		parametersPage.load(parametersElement);
		
		nodes = null;
		nodes = rootElement.getElementsByTagName(RECORDERS_PAGE);
		if (nodes == null || nodes.getLength() == 0)
			throw new WizardLoadingException(true,"missing node: " + RECORDERS_PAGE);
		Element recordersElement = (Element) nodes.item(0);
		recordersPage.load(recordersElement);
		
		nodes = null;
		nodes = rootElement.getElementsByTagName(INTELLIEXTENSION_PAGE);
		if (nodes != null && nodes.getLength() > 0){
			Element intelliExtensionElement = (Element) nodes.item(0);
			intelliExtensionPage.load(intelliExtensionElement);
		}
		
		owner.enableDisableButtons();
	}
	
	//----------------------------------------------------------------------------------------------------
	public String getSettingsFile() { return settingsFile; }

	//=================================================================================
	// private methods
	
	//---------------------------------------------------------------------------------
	/** Saves all model settings to an XML document.
	 * @param document an XML document
	 * @throws TransformerException 
	 * @throws ParserConfigurationException 
	 */
	private void createDocument(Document document) throws ParserConfigurationException, TransformerException {
		Element rootElement = document.createElement(SETTINGS);
		String version = MAJOR_VERSION + "." + MINOR_VERSION;
		rootElement.setAttribute(VERSION,version);
		owner.save(rootElement);
		
		Element loadElement = document.createElement(LOAD_PAGE);
		loadPage.save(loadElement);
		rootElement.appendChild(loadElement);
		
		Element parametersElement = document.createElement(PARAMETERS_PAGE);
		parametersPage.save(parametersElement);
		rootElement.appendChild(parametersElement);
		
		Element recordersElement = document.createElement(RECORDERS_PAGE);
		recordersPage.save(recordersElement);
		rootElement.appendChild(recordersElement);
		
		Element intelliExtensionElement = document.createElement(INTELLIEXTENSION_PAGE);
		intelliExtensionPage.save(intelliExtensionElement);
		rootElement.appendChild(intelliExtensionElement);

		document.appendChild(rootElement);
	}
	
	//--------------------------------------------------------------------------------
	/** Returns true if the version number specified by <code>version</code> is former
	 *  than the recent version
	 * @param version string representation of the version number in format &lt;major version&gt;.&lt;minor version&gt;
	 */
	private boolean isOld(String version) {
		version = version.trim();
		if (version.equals("")) return true;
		String[] parts = version.split("\\.");
		if (parts.length != 2) return true;
		try {
			int major = Integer.parseInt(parts[0]);
			int minor = Integer.parseInt(parts[1]);
			if (major < MAJOR_VERSION || (major == MAJOR_VERSION && minor < MINOR_VERSION))
				return true;
			return false;
		} catch (NumberFormatException e) {
			return true;
		}
	}
	
	//--------------------------------------------------------------------------------
	/** Updates the structure of the XML document specified by <code>rootElement</code>
	 *  to the recent version.
	 * @param fileName the name of the XML file
	 * @param rootElement the root element of the XML document
	 * @param oldMajor the major version number of the XML document
	 * @param oldMinor the minor version number of the XML document
	 * @throws WizardLoadingException if the updating is not possible.
	 */
	private void patch(String fileName, Element rootElement, int oldMajor, int oldMinor) throws WizardLoadingException {
		int versionCode = 1000 * oldMajor + oldMinor;
		switch (versionCode) {
		case 1000 : patchFrom1_0To1_1(rootElement); 			// old version: 1.0
		case 1001 : patchFrom1_1To1_2(rootElement); 			// old version: 1.1
		case 1002 : patchFrom1_2To1_3(rootElement); 			// old version: 1.2
		case 1003 : patchFrom1_3To1_4(rootElement); 			// old version: 1.3
		case 1004 : patchFrom1_4To1_5(rootElement); 			// old version: 1.4
		case 1005 : patchFrom1_5To1_6(); 						// old version: 1.5
		case 1006 : patchFrom1_6To1_7(); 						// old version: 1.6
		case 1007 : patchFrom1_7To1_8(fileName,rootElement); 	// old version: 1.7
		case 1008 : patchFrom1_8To1_9(); 						// old version: 1.8
		case 1009 : patchFrom1_9To1_10(); 						// old version: 1.9
		case 1010 : patchFrom1_10To1_11(); 						// old version: 1.10
		case 1011 : patchFrom1_11To1_12(); 						// old version: 1.11
		case 1012 : patchFrom1_12To2_0(rootElement); 			// old version: 1.12
		case 2000 : patchFrom2_0To2_1();						// old version: 2.0
		case 2001 : patchFrom2_1To2_2();						// old version: 2.1
		case 2002 : patchFrom2_2To2_3();						// old version: 2.2
		case 2003 : patchFrom2_3To2_4(rootElement);				// old version: 2.3
		// another versions here without break
					// the last case branch has only break statement
					break;
		default : throw new WizardLoadingException(false,fileName + " is deprecated.");
		}
	}
	
	//--------------------------------------------------------------------------------
	/** Updates the structure of the XML document specified by <code>rootElement</code>
	 *  from 1.0 to 1.1.<br>
	 *	Task: all &lt;recorder&gt; nodes must have a &lt;writing_time&gt; node with RUN 'type'
	 *	attribute
	 * @param rootElement the root element of the XML document
	 * @throws WizardLoadingException if the updatin is not possible
	 */
	private void patchFrom1_0To1_1(Element rootElement) throws WizardLoadingException {
		NodeList nl = rootElement.getElementsByTagName(RECORDERS_PAGE);
		if (nl == null || nl.getLength() == 0)
			throw new WizardLoadingException(false,"missing node: " + RECORDERS_PAGE);
		Element element = (Element) nl.item(0);
		nl = null;
		nl = element.getElementsByTagName(RECORDERS);
		if (nl == null || nl.getLength() == 0)
			throw new WizardLoadingException(false,"missing node: " + RECORDERS);
		element = (Element) nl.item(0);
		nl = null;
		nl = element.getElementsByTagName(RECORDER);
		if (nl == null || nl.getLength() == 0)
			throw new WizardLoadingException(false,"missing node: " + RECORDER);
		Document document = rootElement.getOwnerDocument();
		for (int i = 0;i < nl.getLength();++i) {
			Element recNode = (Element) nl.item(i);
			Element writingElement = document.createElement(WRITING_TIME);
			writingElement.setAttribute(TYPE,"RUN");
			recNode.appendChild(writingElement);
		}
	}
	
	//--------------------------------------------------------------------------------
	/** Updates the structure of the XML document specified by <code>rootElement</code>
	 *  from 1.1 to 1.2.<br>
	 *	Task: &lt;load_page&gt; node must have a &lt;description&gt; node with empty content.
	 * @param rootElement the root element of the XML document
	 * @throws WizardLoadingException if the updatin is not possible
	 */
	private void patchFrom1_1To1_2(Element rootElement) throws WizardLoadingException {
		NodeList nl = rootElement.getElementsByTagName(LOAD_PAGE);
		if (nl == null || nl.getLength() == 0)
			throw new WizardLoadingException(false,"missing node: " + LOAD_PAGE);
		Element element = (Element) nl.item(0);
		Document document = rootElement.getOwnerDocument();
		Element descElement = document.createElement(DESCRIPTION);
		descElement.appendChild(document.createTextNode(""));
		element.appendChild(descElement);
	}
	
	//--------------------------------------------------------------------------------
	/** Updates the structure of the XML document specified by <code>rootElement</code>
	 *  from 1.2 to 1.3.<br>
	 *  Task: &lt;load_page&gt; node must have a &lt;resources&gt; node with empty content.
	 * @param rootElement the root element of the XML document
	 * @throws WizardLoadingException if the updatin is not possible
	 */
	private void patchFrom1_2To1_3(Element rootElement) throws WizardLoadingException {
		NodeList nl = rootElement.getElementsByTagName(LOAD_PAGE);
		if (nl == null || nl.getLength() == 0)
			throw new WizardLoadingException(false,"missing node: " + LOAD_PAGE);
		Element element = (Element) nl.item(0);
		Document document = rootElement.getOwnerDocument();
		Element resElement = document.createElement(RESOURCES);
		element.appendChild(resElement);
	}
	
	//--------------------------------------------------------------------------------
	/** Updates the structure of the XML document specified by <code>rootElement</code>
	 *  from 1.3 to 1.4.<br>
	 *  Task: &lt;settings&gt; node must have a &lt;need_generation&gt; node with true value.
	 *  	  In 2.0 (and later) &lt;need_generation&gt; node is ceased.
	 * @param rootElement the root element of the XML document
	 * @deprecated
	 */
	private void patchFrom1_3To1_4(Element rootElement) {
//		Document document = rootElement.getOwnerDocument();
//		Element needElement = document.createElement(NEED_GENERATION);
//		needElement.appendChild(document.createTextNode("true"));
//		rootElement.appendChild(needElement);
	}
	
	//---------------------------------------------------------------------------------
	/** Updates the structure of the XML document specified by <code>rootElement</code>
	 *  from 1.4 to 1.5.<br>
	 *  Task: &lt;recorders_page&gt; node must have a &lt;scripts&gt; node with empty content.
	 * @param rootElement the root element of the XML document
	 * @throws WizardLoadingException if the updating is not possible
	 */
	private void patchFrom1_4To1_5(Element rootElement) throws WizardLoadingException {
		NodeList nl = rootElement.getElementsByTagName(RECORDERS_PAGE);
		if (nl == null || nl.getLength() == 0)
			throw new WizardLoadingException(false,"missing node: " + RECORDERS_PAGE);
		Element element = (Element) nl.item(0);
		Document document = rootElement.getOwnerDocument();
		Element scriptsElement = document.createElement(SCRIPTS);
		element.appendChild(scriptsElement);
	}
	
	//---------------------------------------------------------------------------------
	/** Updates the structure of the XML document from 1.5 to 1.6.<br>
	 *  Task: the version 1.6 introduces a new type of scripts. Updating is not necessary.
	 *  This method is exists only because of the patching mechanism.
	 */
	private void patchFrom1_5To1_6() {}

	//---------------------------------------------------------------------------------
	/** Updates the structure of the XML document from 1.6 to 1.7.<br>
	 *  Task: the version 1.7 introduces an IntelliSweep support. Updating is not necessary.
	 *  This method is exists only because of the patching mechanism.
	 */
	private void patchFrom1_6To1_7() {}
	
	//---------------------------------------------------------------------------------
	/** Updates the structure of the XML document specified by <code>rootElement</code>
	 *  from 1.7 to 1.8.<br>
	 *  Task: &lt;script&gt; nodes must contain a child node named &lt;java_type&gt; that
	 *  contains the script return type as a java type. Similar, &lt;member&gt; and &lt;parameter&gt;
	 *  nodes must contain an attribute named 'java_type'. 
	 * @param rootElement the root element of the XML document
	 * @throws WizardLoadingException if the updating is not possible
	 */
	private void patchFrom1_7To1_8(String fileName, Element rootElement) throws WizardLoadingException {
		Document document = rootElement.getOwnerDocument();
		NodeList nl = rootElement.getElementsByTagName(PARAMETERS_PAGE);
		if (nl == null || nl.getLength() == 0)
			throw new WizardLoadingException(false,"missing node: " + PARAMETERS_PAGE);
		Element parametersPageElement = (Element) nl.item(0);
		nl = null; nl = parametersPageElement.getElementsByTagName(NEW_PARAMETERS);
		if (nl != null && nl.getLength() > 0) {
			Element newParametersElement = (Element) nl.item(0);
			nl = null; nl = newParametersElement.getElementsByTagName(PARAMETER);
			if (nl == null || nl.getLength() == 0)
				throw new WizardLoadingException(false,"missing node: " + PARAMETER);
			for (int i = 0;i < nl.getLength();++i) {
				Element param = (Element) nl.item(i);
				String type = param.getAttribute(WizardSettingsManager.TYPE);
				if (type == null || type.equals("")) 
					throw new WizardLoadingException(false,"missing 'type' attribute at node: " + PARAMETER);
				String javaType = toJavaTypeString(type);
				if (javaType == null)
					throw new WizardLoadingException(false,fileName + " is deprecated.");
				param.setAttribute(JAVA_TYPE,javaType);
			}
		}

		nl = null; nl = rootElement.getElementsByTagName(RECORDERS_PAGE);
		if (nl == null || nl.getLength() == 0)
			throw new WizardLoadingException(false,"missing node: " + RECORDERS_PAGE);
		Element recordersPageElement = (Element) nl.item(0);
		nl = null; nl = recordersPageElement.getElementsByTagName(SCRIPTS);
		if (nl == null || nl.getLength() == 0)
			throw new WizardLoadingException(false,"missing node: " + SCRIPTS);
		Element scriptsElement = (Element) nl.item(0);
		nl = null; nl = scriptsElement.getElementsByTagName(SCRIPT);
		if (nl != null) {
			for (int i = 0;i < nl.getLength();++i) {
				Element scriptElement = (Element) nl.item(i);
				NodeList temp = scriptElement.getElementsByTagName(TYPE);
				if (temp == null || temp.getLength() == 0)
					throw new WizardLoadingException(false,"missing node: " + TYPE);
				Element typeElement = (Element) temp.item(0);
				NodeList content = typeElement.getChildNodes();
				if (content == null || content.getLength() == 0)
					throw new WizardLoadingException(false,"missing content at node: " + TYPE);
				String type = ((Text)content.item(0)).getNodeValue();
				String javaType = toJavaTypeString(type);
				if (javaType == null)
					throw new WizardLoadingException(false,fileName + " is deprecated.");
				Element javaTypeElement = document.createElement(JAVA_TYPE);
				javaTypeElement.appendChild(document.createTextNode(javaType));
				scriptElement.appendChild(javaTypeElement);
		
			}
		}
		nl = null; nl = recordersPageElement.getElementsByTagName(RECORDERS);
		if (nl == null || nl.getLength() == 0)
			throw new WizardLoadingException(false,"missing node: " + RECORDERS);
		Element recordersElement = (Element) nl.item(0);
		nl = recordersElement.getElementsByTagName(RECORDER);
		if (nl != null) {
			for (int i = 0;i < nl.getLength();++i) {
				Element recorder = (Element) nl.item(i);
				NodeList nodes = recorder.getElementsByTagName(MEMBER);
				if (nodes == null)
					continue;
				for (int j = 0;j < nodes.getLength();++j) {
					Element memberElement = (Element) nodes.item(j);
					String memberType = memberElement.getAttribute(TYPE);
					if (memberType == null || memberType.equals(""))
						throw new WizardLoadingException(false,"missing 'type' attribute at node: " + MEMBER);
					String javaType = toJavaTypeString(memberType);
					if (javaType == null)
						throw new WizardLoadingException(false,fileName + " is deprecated.");
					memberElement.setAttribute(JAVA_TYPE,javaType);
				}
			}
		}
	}
	
	//---------------------------------------------------------------------------------
	/** Updates the structure of the XML document from 1.8 to 1.9.<br>
	 *  Task: the version 1.9 introduces operator script types. Updating is not necessary.
	 *  This method is exists only because of the patching mechanism.
	 */
	private void patchFrom1_8To1_9() {}
	
	//---------------------------------------------------------------------------------
	/** Updates the structure of the XML document from 1.9 to 1.10.<br>
	 *  Task: the version 1.9 introduces inner scripts. Updating is not necessary
	 *  because every script that has not inner attribute are NOT inner script.
	 *  This method is exists only because of the patching mechanism.
	 */
	private void patchFrom1_9To1_10() {}
	
	//----------------------------------------------------------------------------------------------------
	/** Updates the structure of the XML document from 1.10 to 1.11.<br>
	 *  Task: the version 1.11 introduces alias names for recordable elements. Updating is not necessary.
	 *  This method is exists only because of the patching mechanism.
	 */
	private void patchFrom1_10To1_11() {}
	
	//----------------------------------------------------------------------------------------------------
	/** Updates the structure of the XML document from 1.11 to 1.12.<br>
	 *  Task: the version 1.12 introduces editable scripts. Updating is not necessary.
	 *  This method is exists only because of the patching mechanism.
	 */
	private void patchFrom1_11To1_12() {}
	
	//----------------------------------------------------------------------------------------------------
	/** Updates the structure of the XML document from 1.12 to 2.0.<br>
	 *  Task: the version 2.0 introduces Multi-platform Parameter Sweep Wizard. a, The root element has a new
	 *  attribute that describes the simulation platform. This is RepastJ for the old XML-s. b, There is a new
	 *  mandatory node that contains the root of the modell. For old XML-s this can be calculated from the 
	 *  &lt;model_file&gt; node. 
	 */
	private void patchFrom1_12To2_0(Element rootElement) throws WizardLoadingException {
		Document document = rootElement.getOwnerDocument();
		rootElement.setAttribute(SIMULATION_PLATFORM,PlatformManager.idStringForPlatform(PlatformType.REPAST));
		
		NodeList nl = rootElement.getElementsByTagName(WizardSettingsManager.MODEL_FILE);
		if (nl == null || nl.getLength() == 0)
			throw new WizardLoadingException(true,"missing node: " + WizardSettingsManager.MODEL_FILE);
		Element modelFileNameElement = (Element) nl.item(0);
		NodeList content = modelFileNameElement.getChildNodes();
		if (content == null || content.getLength() == 0)
			throw new WizardLoadingException(true,"missing content at node: " + WizardSettingsManager.MODEL_FILE);
		String modelFileName = ((Text)content.item(0)).getNodeValue();
		String modelRoot = calculateModelRoot(modelFileName);
		
		Element element = document.createElement(WizardSettingsManager.MODEL_ROOT);
		element.appendChild(document.createTextNode(modelRoot));
		rootElement.appendChild(element);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void patchFrom2_0To2_1() {}
	
	//----------------------------------------------------------------------------------------------------
	private void patchFrom2_1To2_2() {}
	
	//----------------------------------------------------------------------------------------------------
	private void patchFrom2_2To2_3() {}
	
	//----------------------------------------------------------------------------------------------------
	private void patchFrom2_3To2_4(final Element rootElement) throws WizardLoadingException {
		final NodeList nl = rootElement.getElementsByTagName(RECORDERS_PAGE);
		if (nl == null || nl.getLength() == 0)
			throw new WizardLoadingException(false,"missing node: " + RECORDERS_PAGE);
		final Element recPageElement = (Element) nl.item(0);
		final Document document = rootElement.getOwnerDocument();
		final Element userVariablesElement = document.createElement(USER_VARIABLES);
		recPageElement.appendChild(userVariablesElement);
	}
	
	//----------------------------------------------------------------------------------------------------
	private String toJavaTypeString(String type) {
		if (type == null) return null;
		if (type.equals("byte") || type.equals("short") || type.equals("int") ||
			type.equals("long") || type.equals("float") || type.equals("double") ||
			type.equals("boolean") || type.equals("char")) 
			return type;
		if (type.equals("Byte") || type.equals("Short") || type.equals("Integer") ||
			type.equals("Long") || type.equals("Float") || type.equals("Double") ||
			type.equals("Boolean") || type.equals("Character") || type.equals("String"))
			return "java.lang." + type;
		return null;
	}
	
	//----------------------------------------------------------------------------------------------------
	private String calculateModelRoot(String modelFileName) throws WizardLoadingException {
		File f = new File(modelFileName);
		CtClass	clazz = null;
		try {
			InputStream ins = new FileInputStream(f);
			clazz = owner.getClassPool().makeClass(ins);
			clazz.stopPruning(true);
			ins.close();
		} catch (IOException e) {
			e.printStackTrace(ParameterSweepWizard.getLogStream());
			throw new WizardLoadingException(true,e);
		} finally {
			if (clazz != null)
				clazz.defrost();
		}
		String packageName = clazz.getName();
		int index = packageName.lastIndexOf('.');
		if (index != -1) 
			packageName = packageName.substring(0,index);
		packageName = packageName.replace('.',File.separatorChar);
		index = modelFileName.lastIndexOf(packageName);
		String returnPath = (index == -1) ? modelFileName : modelFileName.substring(0,index - 1);
		return returnPath;
	}
}
