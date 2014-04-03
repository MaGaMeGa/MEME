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
package ai.aitia.meme.paramsweep.internal.platform;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Arrays;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import uchicago.src.sim.parameter.Parameter;
import ai.aitia.meme.gui.Wizard.Button;
import ai.aitia.meme.paramsweep.ParameterSweepWizard;
import ai.aitia.meme.paramsweep.generator.EMILXmlParameterFileGenerator;
import ai.aitia.meme.paramsweep.generator.NetLogoParameterFileGenerator;
import ai.aitia.meme.paramsweep.generator.RepastJPlainTextParameterFileGenerator;
import ai.aitia.meme.paramsweep.generator.RepastSXMLParameterFileGenerator;
import ai.aitia.meme.paramsweep.gui.Page_Recorders;
import ai.aitia.meme.paramsweep.gui.info.ChooserParameterInfo;
import ai.aitia.meme.paramsweep.gui.info.MasonChooserParameterInfo;
import ai.aitia.meme.paramsweep.gui.info.MasonIntervalParameterInfo;
import ai.aitia.meme.paramsweep.gui.info.ParameterInfo;
import ai.aitia.meme.paramsweep.parser.RepastJParameterFileParser;
import ai.aitia.meme.paramsweep.platform.PlatformManager;
import ai.aitia.meme.paramsweep.platform.PlatformManager.PlatformType;
import ai.aitia.meme.paramsweep.utils.ParameterEnumeration;
import ai.aitia.meme.paramsweep.utils.ParameterParserException;
import ai.aitia.meme.paramsweep.utils.Util;
import ai.aitia.meme.paramsweep.utils.Utilities;
import ai.aitia.meme.utils.OSUtils;
import ai.aitia.meme.utils.OSUtils.OSType;
import ai.aitia.meme.utils.Utils;

public class PlatformSettings {
	private static PlatformType platformType = PlatformType.REPAST;
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public static void setSelectedPlatform( PlatformType platform ){ platformType = platform; }
	public static PlatformType getPlatformType() { return platformType; }

	//----------------------------------------------------------------------------------------------------
	public static IGUIController getGUIControllerForPlatform() { // TODO: EXTEND IT FOR A NEW PLATFORM
		switch (platformType) {
		case REPAST 	: return new RepastGUIController();
		case SIMPHONY	: return new SimphonyGUIController();
		case EMIL		: return new EMILGUIController();
		case TRASS		: return new TrassGUIController();
		case SIMPHONY2	:
		case CUSTOM		: return new CustomJavaGUIController();
		case NETLOGO5	:
		case NETLOGO	: return new NetLogoGUIController();
		case MASON		: return new MasonGUIController();
		default 		: throw new UnsupportedPlatformException(platformType.toString());
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	// pre-condition: getGUIControllerForPlatform().isScriptingSupport() is true
	public static IScriptSupport getScriptSupport(ParameterSweepWizard wizard, Page_Recorders recordersPage) { // TODO: EXTEND IT FOR A NEW PLATFORM
		switch (platformType) {
		case REPAST		:
		case MASON		:
		case SIMPHONY2	:
		case CUSTOM		: return new RepastScriptSupport(wizard,recordersPage);
		case NETLOGO5	:
		case NETLOGO	: return new NetLogoScriptSupport(wizard,recordersPage);
		default			: throw new UnsupportedPlatformException(platformType.toString());
		}
		
	}
	
	//----------------------------------------------------------------------------------------------------
	public static boolean isEnabledForPageLoadModel(ParameterSweepWizard wizard, Button b) { // TODO: EXTEND IT FOR A NEW PLATFORM
		switch (platformType) {
		case REPAST  	:
		case SIMPHONY 	: return _RepastIsEnabledForPageLoadModel(wizard,b);
		case TRASS		:
		case EMIL		: return _EMILIsEnabledForPageLoadModel(wizard,b);
		case MASON		:
		case SIMPHONY2	:
		case CUSTOM		: return _CustomJavaIsEnabledForPageLoadModel(wizard,b);
		case NETLOGO5	:
		case NETLOGO	: return _NetLogoIsEnabledForPageLoadModel(wizard,b);
		default			: throw new UnsupportedPlatformException(platformType.toString());
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public static boolean isEnabledForPageParameters(ParameterSweepWizard wizard, Button b) { // TODO: EXTEND IT FOR A NEW PLATFORM
		switch (platformType) {
		case REPAST 	: return _RepastIsEnabledForPageParameters(wizard,b);
		case SIMPHONY	: return _SimphonyIsEnabledForPageParameters(wizard,b);
		case TRASS		:
		case EMIL		: return _EMILIsEnabledForPageParameters(wizard,b);
		case MASON		:
		case SIMPHONY2	:
		case CUSTOM		: return _CustomJavaIsEnabledForPageParameters(wizard,b);
		case NETLOGO5	:
		case NETLOGO	: return _NetLogoIsEnabledForPageParameters();
		default		: throw new UnsupportedPlatformException(platformType.toString());
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public static boolean isEnabledForPageRecorders(ParameterSweepWizard wizard, Button b) { // TODO: EXTEND IT FOR A NEW PLATFORM
		switch (platformType) {
		case REPAST 	: return _RepastIsEnabledForPageRecorders(wizard,b);
		case SIMPHONY	: return _SimphonyIsEnabledForPageRecorders(wizard,b);
		case TRASS		:
		case EMIL		: return _EMILIsEnabledForPageRecorders(wizard,b);
		case MASON		:
		case SIMPHONY2	:
		case CUSTOM		: return _CustomJavaIsEnabledForPageRecorders(wizard,b);
		case NETLOGO5	:
		case NETLOGO	: return _NetLogoIsEnabledForPageRecorders(wizard,b);
		default			: throw new UnsupportedPlatformException(platformType.toString());
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	// false means check is failed
	public static boolean additionalModelCheck(ParameterSweepWizard wizard) { // TODO: EXTEND IT FOR A NEW PLATFORM
		switch (platformType) {
		case REPAST 	: return _RepastAdditionalCheck(wizard);
		case SIMPHONY 	: return _SimphonyAdditionalCheck(wizard);
		case TRASS		:
		case EMIL		: return _EMILAdditionalCheck(wizard);
		case MASON		:
		case SIMPHONY2	:
		case CUSTOM		: return _CustomJavaAdditionalCheck(wizard);
		case NETLOGO5	:
		case NETLOGO	: return _NetLogoAdditionalCheck();
		default : throw new UnsupportedPlatformException(platformType.toString());
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public static List<String> additionalParameterCheck(ParameterInfo info, String[] newValues, int defType) { // TODO: EXTEND IT FOR A NEW PLATFORM
		switch (platformType) {
		case REPAST		:
		case SIMPHONY	:
		case TRASS		:
		case EMIL		:
		case SIMPHONY2	:
		case CUSTOM		: return new ArrayList<String>();
		case MASON		: return _MasonAdditionalParameterCheck(info, newValues, defType);
		case NETLOGO5	:
		case NETLOGO	: return _NetLogoAdditionalParameterCheck(info,newValues,defType);
		default			: throw new UnsupportedOperationException(platformType.toString());
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public static void generateParameterFile(File file, DefaultMutableTreeNode root) throws IOException { // TODO: EXTEND IT FOR A NEW PLATFORM
		switch (platformType) {
		case REPAST : {
						RepastJPlainTextParameterFileGenerator generator = new RepastJPlainTextParameterFileGenerator(file);
						generator.generateFile(root);
				 	    break;
					  }
		case SIMPHONY : {
						 RepastSXMLParameterFileGenerator generator = new RepastSXMLParameterFileGenerator(file);
						 generator.generateFile(root);
		  				 break;
						}
		case TRASS	:
		case EMIL	: {
						EMILXmlParameterFileGenerator generator = new EMILXmlParameterFileGenerator(file);
						generator.generateFile(root);
						break;
					  }
		case MASON	:
		case SIMPHONY2	:
		case CUSTOM	: {
						RepastJPlainTextParameterFileGenerator generator = new RepastJPlainTextParameterFileGenerator(file);
						generator.generateFile(root);
						break;
		  			  }
		case NETLOGO5:
		case NETLOGO :{
						NetLogoParameterFileGenerator	generator = new NetLogoParameterFileGenerator(file);
						generator.generateFile(root);
						break;
					  }
		default 	: throw new UnsupportedPlatformException(platformType.toString());
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public static String generateParameterTreeOutput(final DefaultMutableTreeNode root, final PlatformType type) throws ParserConfigurationException,
																														TransformerException {
		// TODO: EXTEND IT FOR A NEW PLATFORM
		switch (type) {
		case REPAST 	: return RepastJPlainTextParameterFileGenerator.generate(root);
		case SIMPHONY 	: return RepastSXMLParameterFileGenerator.generate(root);
		case TRASS		:
		case EMIL		: return EMILXmlParameterFileGenerator.generateStringRepresentation(root);
		case MASON		:
		case SIMPHONY2	:
		case CUSTOM		: return RepastJPlainTextParameterFileGenerator.generate(root);
		case NETLOGO5	:
		case NETLOGO	: return NetLogoParameterFileGenerator.generateStringRepresentation(root);
		default 	: throw new UnsupportedPlatformException(platformType.toString());
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public static String generateParameterTreeOutput(final DefaultMutableTreeNode root) throws ParserConfigurationException, TransformerException {
		return generateParameterTreeOutput(root,platformType);
	}
	
	//----------------------------------------------------------------------------------------------------
	public static DefaultMutableTreeNode parseParameterFile(final List<ParameterInfo> parameters, final String paramFileContent, 
															final PlatformType platform) throws ParameterParserException { 
		// Create temporary file to use file parser
		File tempFile = null; 
		try {
			tempFile = File.createTempFile("param",null);
			PrintWriter pw = new PrintWriter(new FileWriter(tempFile));
			pw.print(paramFileContent);
			pw.flush();
			pw.close();
		} catch (IOException e) {
			throw new ParameterParserException("initialization error");
		}
		try {
			final DefaultMutableTreeNode root = parseParameterFile(parameters,tempFile,platform);
			return root;
		} finally {
			tempFile.delete();
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public static DefaultMutableTreeNode parseParameterFile(final List<ParameterInfo> parameters, final String paramFileContent)
																												throws ParameterParserException {
		return parseParameterFile(parameters,paramFileContent,platformType);
	}

	
	//----------------------------------------------------------------------------------------------------
	public static DefaultMutableTreeNode parseParameterFile(final List<ParameterInfo> parameters, final File paramFile, final PlatformType platform)
																			throws ParameterParserException { // TODO: EXTEND IT FOR A NEW PLATFORM
		switch (platform) {
		case REPAST		: return _RepastParseParameterFile(parameters,paramFile);
		case SIMPHONY 	: return _RepastSParseParameterFile(parameters, paramFile);
		case TRASS		:
		case EMIL		: return _EMILParseParameterFile(parameters,paramFile);
		case MASON		:
		case SIMPHONY2	:
		case CUSTOM		: return _CustomJavaParseParameterFile(parameters,paramFile);
		case NETLOGO5	:
		case NETLOGO	: return _NetLogoParseParameterFile(parameters,paramFile);
		default			: throw new UnsupportedPlatformException(platformType.toString());
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public static DefaultMutableTreeNode parseParameterFile(final List<ParameterInfo> parameters, final File paramFile)
																			throws ParameterParserException { // TODO: EXTEND IT FOR A NEW PLATFORM
		return parseParameterFile(parameters,paramFile,platformType);
	}
	
	//----------------------------------------------------------------------------------------------------
	public static String generateParameterFilePath(ParameterSweepWizard wizard) { // TODO: EXTEND IT FOR A NEW PLATFORM
		switch (platformType) {
		case REPAST 	: return RepastJPlainTextParameterFileGenerator.generateEmptyFilePath(wizard.getClassPool(),wizard.getModelFileName());
		case SIMPHONY 	: return RepastSXMLParameterFileGenerator.generateEmptyXMLFilePath(wizard.getModelFileName());
		case TRASS		:
		case EMIL		: return EMILXmlParameterFileGenerator.generateEmptyFilePath(wizard.getModelFileName());
		case MASON 		:
		case SIMPHONY2	:
		case CUSTOM 	: return RepastJPlainTextParameterFileGenerator.generateEmptyFilePath(wizard.getClassPool(),wizard.getModelFileName());
		case NETLOGO5	:
		case NETLOGO	: return NetLogoParameterFileGenerator.generateEmptyFilePath(wizard.getModelFileName());
		default		: throw new UnsupportedPlatformException(platformType.toString());
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	// first value : true means check is successful 
	// second value : true means further checks needed
	public static boolean[] finishCheck(ParameterSweepWizard wizard, Page_Recorders pageRecorders) { // TODO: EXTEND IT FOR A NEW PLATFORM
		switch (platformType) {
		case REPAST   : return _RepastFinishCheck(wizard,pageRecorders);
		case SIMPHONY : return _SimphomyFinishCheck(pageRecorders);
		case TRASS	  :
		case EMIL	  : return _EMILFinishCheck(pageRecorders);
		case MASON	  : return _MasonFinishCheck(wizard, pageRecorders);
		case SIMPHONY2	:
		case CUSTOM	  : return _CustomJavaFinishCheck(wizard,pageRecorders);
		case NETLOGO5 :
		case NETLOGO  : return _NetLogoFinishCheck(wizard,pageRecorders);
		default 	  : throw new UnsupportedPlatformException(platformType.toString());
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public static boolean isUseGeneratedModel() { // TODO: EXTEND IT FOR A NEW PLATFORM
		switch (platformType) {
		case REPAST		: return true;
		case SIMPHONY 	: return false; //TODO: ez m�g v�ltozhat
		case TRASS		: return false;
		case EMIL		: return false;
		case MASON		: return true;
		case SIMPHONY2	: return true;
		case CUSTOM 	: return true;
		case NETLOGO	: return true;
		case NETLOGO5	: return true;
		default 		: return false;
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public static List<String> getDefaultClassPathEntries(ParameterSweepWizard wizard) { // TODO: EXTEND IT FOR A NEW PLATFORM
		switch (platformType) {
		case REPAST		:
		case CUSTOM 	:
		case MASON		:
		case TRASS		:
		case EMIL		: return new ArrayList<String>();
		case NETLOGO5	:
		case NETLOGO	: return _getNetlogoCpEntries(wizard);
		case SIMPHONY2	:
		case SIMPHONY 	: return _getSimphonyCpEntries(wizard);
		default : throw new UnsupportedPlatformException(platformType.toString());
		}
	}
	
	//====================================================================================================
	// private methods
	
	//====================================================================================================
	// RepastJ Section
	
	//----------------------------------------------------------------------------------------------------
	private static boolean _RepastIsEnabledForPageLoadModel(ParameterSweepWizard wizard, Button b) {
		switch (b) {
		case CANCEL : 
		case BACK 	: return true;
		case CUSTOM :
		case FINISH : 
		case NEXT 	: return (wizard != null && wizard.getModelFileName() != null);
		default : return false;
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private static boolean _RepastIsEnabledForPageParameters(ParameterSweepWizard wizard, Button b) { return true; }
	
	//----------------------------------------------------------------------------------------------------
	private static boolean _RepastIsEnabledForPageRecorders(ParameterSweepWizard wizard, Button b) {
		if (wizard.getSweepingMethodID() == 0)
			return b != Button.NEXT; 
		else return true;
	}
	
	//----------------------------------------------------------------------------------------------------
	private static boolean _RepastAdditionalCheck(ParameterSweepWizard wizard) {
		String name = wizard.getModelFileName().substring(0,wizard.getModelFileName().lastIndexOf(".class")); 
		if (name.toUpperCase().endsWith("GUI")) {
			int result = Utilities.askUser(ParameterSweepWizard.getFrame(),false,"Warning",name + " is seemed to be a GUI version of a model.",
										   "Running a GUI model in batch mode may cause crash (especially a distributed mode).",
										   "Do you want to continue?");
			return result > 0; 
		}
		return true;
	}
	
	//----------------------------------------------------------------------------------------------------
	private static DefaultMutableTreeNode _RepastParseParameterFile(List<ParameterInfo> parameters, File paramFile)
																	throws ParameterParserException {
		String[] error = new String[1];
		RepastJParameterFileParser parser = _RepastScanParamFile(paramFile,parameters,error); 
		if (error[0] != null)
			throw new ParameterParserException(error[0]);
		return _RepastCreateTreeFromFile(parser,parameters);
	}
	
	//------------------------------------------------------------------------------
	/** Checks and parses the parameter file <code>file</code> and returns the parser. 
	 * @param file the parameter file (null or non-existing file is not permitted)
	 * @param error output parameter for error messages
	 * @return the parser object
	 */
	private static RepastJParameterFileParser _RepastScanParamFile(File file, List<ParameterInfo> parameters, String[] error) { 
		RepastJParameterFileParser result = null;
		try {
			result = RepastJParameterFileParser.createParser(file);
			
			String[] refNames = result.getCapitalizedParameterNames();
			for (String name : refNames) {
				ParameterInfo pi = new ParameterInfo(name,"",null);
				if (!parameters.contains(pi)) {
					error[0] = "Unknown referenced parameter: " + name;
					return null;
				}
			}

			Vector<Parameter> params = result.getParameters();
			for (Parameter param : params) {
				if (param.getChildren().size() > 1) {
					error[0] = String.format("Unsupported parameter file. %s has two or more children.",Util.capitalize(param.getName()));
					return null;
				}
			}
		} catch (IOException e) {
			error[0] = Util.getLocalizedMessage(e);
		}
		return result;
	}
	
	//------------------------------------------------------------------------------
	/** Builds the parameter tree from file. The file is parsed by <code>parser</code>. */
	@SuppressWarnings("unchecked")
	private static DefaultMutableTreeNode _RepastCreateTreeFromFile(RepastJParameterFileParser parser, List<ParameterInfo> parameters) {
		DefaultMutableTreeNode result = new DefaultMutableTreeNode("Parameter file"); 
		Vector<Parameter> v = parser.getParameters();
		Vector<Integer> indices = new Vector<Integer>(v.size());
		List<ParameterInfo> placed = new ArrayList<ParameterInfo>();
		// I. update data
		for (Parameter param : v) {
			int index = -1;
			for (int i = 0; i < parameters.size();++i) {
				if (parameters.get(i).equals(new ParameterInfo(Util.capitalize(param.getName()),"",null))) {
					index = i;
					break;
				}
			}
			indices.add(new Integer(index));
			ParameterInfo info = parameters.get(index);
			long run = param.getNumRuns();
			if (param.isConstant()) {
			    Object value = convert(param.getValue(),info.getType());
			    if (value == null) 
			    	ParameterSweepWizard.logError("Invalid parameter value: %s at %s",param.getValue().toString(),param.getName());
			    else {
			    	info.clear();
			    	info.setDefinitionType(ParameterInfo.CONST_DEF);
			    	info.setRuns(run);
			    	info.setValue(value);
			    }
			} else if (param.isList()) {
				Vector<Object> list = param.getList();
				List<Object> newList = convert(list,info.getType());
				if (newList == null) 
					ParameterSweepWizard.logError("Invalid parameter value(s) at %s",param.getName());    
				else {
					info.clear();
				    info.setDefinitionType(ParameterInfo.LIST_DEF);
				    info.setRuns(run);
				    info.setValues(newList);
				}
			} else {
				Object start = convert(param.getStart(),info.getType());
				if (start == null)
					ParameterSweepWizard.logError("Invalid parameter value: %s at %s",param.getStart().toString(),param.getName());
				Object end = convert(param.getEnd(),info.getType());
				if (end == null) 
					ParameterSweepWizard.logError("Invalid parameter value: %s at %s",param.getEnd().toString(),param.getName());
				Object incr = convert(param.getIncr(),info.getType());
				if (incr == null)
					ParameterSweepWizard.logError("Invalid parameter value: %s at %s",param.getIncr().toString(),param.getName());
				if (start != null && end != null && incr != null) {
					info.clear();
					info.setDefinitionType(ParameterInfo.INCR_DEF);
					info.setRuns(run);
					info.setStartValue((Number)start);
					info.setEndValue((Number)end);
					info.setStep((Number)incr);
				}
			}
		}
		// II. add to the tree
		for (int i = 0; i < v.size();++i) {
			ParameterInfo info = parameters.get(indices.get(i).intValue());
			if (v.get(i).getParent() == null)
				result.add(new DefaultMutableTreeNode(info));
			else {
				DefaultMutableTreeNode node = findNode(result,parameters,v.get(i).getParent());
				node.add(new DefaultMutableTreeNode(info));
			}
			placed.add(info);
		}
		for (ParameterInfo info : parameters) {
			if (!placed.contains(info))
				result.add(new DefaultMutableTreeNode(info));
		}
		return result;
	}
	
	//------------------------------------------------------------------------------
	/** Converts <code>value</code> to type <code>type</code>.
	 * @param type the name of the new type (if this is a primitive type we use 
	 *             its wrapper type instead.
	 */
	private static Object convert(Object value, String type) {
		try {
			if (type.equals("String"))
				return value.toString();
			if (type.equals("boolean") || type.equals("Boolean"))
				return new Boolean(value.toString());
			Double d = (Double) value;
			if (type.equals("double") || type.equals("Double"))
				return d;
			if (type.equals("byte") || type.equals("Byte"))
				return new Byte(d.byteValue());
			if (type.equals("short") || type.equals("Short"))
				return new Short(d.shortValue());
			if (type.equals("int") || type.equals("Integer"))
				return new Integer(d.intValue());
			if (type.equals("long") || type.equals("Long"))
				return new Long(d.longValue());
			if (type.equals("float") || type.equals("Float"))
				return new Float(d.floatValue());
		} catch (ClassCastException e) {}
		return null;
	}
	
	//------------------------------------------------------------------------------
	/** Converts the element of <code>list</code> to type <code>type</code>.
	 * @param type the name of the new type (if this is a primitive type we use 
	 *             its wrapper type instead.
	 */
	private static List<Object> convert(Vector<Object> list, String type) {
		List<Object> result = new ArrayList<Object>(list.size());
		for (Object o : list) {
			Object oo = convert(o,type);
			if (oo == null) return null;
			result.add(oo);
		}
		return result;
	} 
	 
	//------------------------------------------------------------------------------
	/** Returns the node object that belongs to the parameter <code>param</code>. The
	 *  uchicago.src.sim.parameter. Parameter class is the Repast representation of a 
	 *  parameter. 
	 * @return null if it doesn't find the node
	 */
	@SuppressWarnings("unchecked")
	private static DefaultMutableTreeNode findNode(DefaultMutableTreeNode root, List<ParameterInfo> parameters, Parameter param) {
		int index = -1;
		for (int i = 0; i < parameters.size();++i) {
			if (parameters.get(i).equals(new ParameterInfo(Util.capitalize(param.getName()),"",null))) {
				index = i;
				break;
			}
		}
		if (index == -1) return null;
		ParameterInfo info = parameters.get(index);
		Enumeration<DefaultMutableTreeNode> e = new ParameterEnumeration(root);
		e.nextElement();
		while (e.hasMoreElements()) {
			DefaultMutableTreeNode node = e.nextElement();
			ParameterInfo nodeInfo = (ParameterInfo) node.getUserObject();
			if (info.equals(nodeInfo)) 
				return node;
		}
		return null;
	}
	
	//----------------------------------------------------------------------------------------------------
	private static boolean[] _RepastFinishCheck(ParameterSweepWizard wizard, Page_Recorders pageRecorders) {
		boolean needModelGeneration = (wizard.getNewParameters_internal() != null && wizard.getNewParameters_internal().size() > 0) ||
		  							   pageRecorders.isTreeValid();
		if (needModelGeneration) {
			// generation
			if (pageRecorders.isTreeValid()) return new boolean[] { true, true };
			else {
				// new parameters only
				pageRecorders.setStopFieldText("");
				int result = Utilities.askUserOK(ParameterSweepWizard.getFrame(),"Warning",
						 						 "There were no data recorders added. If there are data recorders in your original model,",
						 						 "click 'OK'. If you wish to add data recorders now, click 'Cancel'.");
				return new boolean [] { (result == 1), false };
			}
		} else {
			// no generation
			int result = Utilities.askUserOK(ParameterSweepWizard.getFrame(),"Warning",
					 						 "There were no data recorders or new parameters added. If there are data recorders",
					 						 "in your original model and you do not wish to add any additional parameters,",
					 						 "it isn't necessary to create a new model. Click 'OK' to continue anyways.",
					 						 "If you would like to add data recorders or parameters to your model click 'Cancel'.");
			return new boolean[] { (result == 1), false };
		}
	}
	
	//====================================================================================================
	// End of RepastJ Section
	
	//====================================================================================================
	// Custom Java Section
	
	//----------------------------------------------------------------------------------------------------
	private static boolean _CustomJavaIsEnabledForPageLoadModel(ParameterSweepWizard wizard, Button b) {
		return _RepastIsEnabledForPageLoadModel(wizard,b);
	}
	
	//----------------------------------------------------------------------------------------------------
	private static boolean _CustomJavaIsEnabledForPageParameters(ParameterSweepWizard wizard, Button b) { 
		return _RepastIsEnabledForPageParameters(wizard,b);
	}
	
	//----------------------------------------------------------------------------------------------------
	private static boolean _CustomJavaIsEnabledForPageRecorders(ParameterSweepWizard wizard, Button b) {
		return _RepastIsEnabledForPageRecorders(wizard,b);
	}
	
	//----------------------------------------------------------------------------------------------------
	private static boolean _CustomJavaAdditionalCheck(ParameterSweepWizard wizard) { return _RepastAdditionalCheck(wizard); }
	
	//----------------------------------------------------------------------------------------------------
	private static DefaultMutableTreeNode _CustomJavaParseParameterFile(List<ParameterInfo> parameters, File paramFile)
																	    throws ParameterParserException {
		return _RepastParseParameterFile(parameters,paramFile);
	}
	
	//----------------------------------------------------------------------------------------------------
	private static boolean[] _CustomJavaFinishCheck(ParameterSweepWizard wizard, Page_Recorders pageRecorders) {
		if (pageRecorders.isTreeValid()) return new boolean[] { true, true };
		return new boolean[] { false, false };
	}
	
	//====================================================================================================
	// End of Custom Java Section
	
	//----------------------------------------------------------------------------------------------------
	//====================================================================================================
	// MASON section
	
	//----------------------------------------------------------------------------------------------------
	private static List<String> _MasonAdditionalParameterCheck(ParameterInfo info, String[] newValues, int defType) {
		ArrayList<String> result = new ArrayList<String>();
		if (info instanceof MasonIntervalParameterInfo) {
			MasonIntervalParameterInfo mipi = (MasonIntervalParameterInfo) info;
			if (defType == ParameterInfo.CONST_DEF) {
				if (!mipi.isValidValue(newValues[0])) 
					result.add(newValues[0] + " is not a valid value. Valid values are between "+mipi.getIntervalMin()+" and "+mipi.getIntervalMax());
			} else if (defType == ParameterInfo.LIST_DEF) {
				if (!mipi.isValidValues(newValues))
					result.add("There are invalid values in the list. Valid values are between "+mipi.getIntervalMin()+" and "+mipi.getIntervalMax());
			} else {
				if (!mipi.isValidValues(newValues[0],newValues[1],newValues[2]))
					result.add("The defined parameter series contains invalid values. Valid values are between "+mipi.getIntervalMin()+" and "+mipi.getIntervalMax());
			}
		} else if (info instanceof MasonChooserParameterInfo) {
			MasonChooserParameterInfo mcpi = (MasonChooserParameterInfo) info;
			if (defType == ParameterInfo.CONST_DEF) {
				if (!mcpi.isValidValue(newValues[0])) 
					result.add(newValues[0] + " is not a valid value. Valid values are: "+mcpi.getValidValuesString());
			} else if (defType == ParameterInfo.LIST_DEF) {
				if (!mcpi.isValidValues(newValues))
					result.add("There are invalid values in the list. Valid values are: "+mcpi.getValidValuesString());
			} else {
				if (!mcpi.isValidValues(newValues[0],newValues[1],newValues[2]))
					result.add("The defined parameter series contains invalid values. Valid values are: "+mcpi.getValidValuesString());
			}
		}
		return result;
	}

	//----------------------------------------------------------------------------------------------------
	private static boolean[] _MasonFinishCheck(ParameterSweepWizard wizard, Page_Recorders pageRecorders) {
		if (pageRecorders.isTreeValid()) {
			ArrayList<String> result = new ArrayList<String>();
			List<ParameterInfo> originalParameters = wizard.getOriginalParameterInfos();
			DefaultMutableTreeNode paramTreeRoot = wizard.getParameterTreeRoot();
			Enumeration<DefaultMutableTreeNode> treeNodes = new ParameterEnumeration(paramTreeRoot);
			treeNodes.nextElement(); //skipping root element, which does not contain any parameter
			while (treeNodes.hasMoreElements()) {
				DefaultMutableTreeNode node = treeNodes.nextElement();
				ParameterInfo actualParameter = (ParameterInfo) node.getUserObject();
				ParameterInfo originalParameter = findInfo(originalParameters, actualParameter.getName());
				if (originalParameter instanceof MasonIntervalParameterInfo) {
					MasonIntervalParameterInfo mipi = (MasonIntervalParameterInfo) originalParameter;
					if (actualParameter.getDefinitionType() == ParameterInfo.CONST_DEF) {
						if (!mipi.isValidValue(actualParameter.getValue().toString())) { 
							result.add(actualParameter.getValue().toString() + " is not a valid value for "+mipi.getName()+". Valid values are between "+mipi.getIntervalMin()+" and "+mipi.getIntervalMax());
						}
					} else if (actualParameter.getDefinitionType() == ParameterInfo.LIST_DEF) {
						String[] actualValues = new String[actualParameter.getValues().size()];
						for (int i = 0; i < actualValues.length; i++) {
							actualValues[i] = actualParameter.getValues().get(i).toString();
						}
						if (!mipi.isValidValues(actualValues)) {
							result.add("There are invalid values in the list for "+mipi.getName()+". Valid values are between "+mipi.getIntervalMin()+" and "+mipi.getIntervalMax());
						}
					} else {
						if (!mipi.isValidValues(actualParameter.getStartValue().toString(),actualParameter.getEndValue().toString(),actualParameter.getStep().toString())) {
							result.add("The defined parameter series contains invalid values for "+mipi.getName()+". Valid values are between "+mipi.getIntervalMin()+" and "+mipi.getIntervalMax());
						}
					}
				} else if (originalParameter instanceof MasonChooserParameterInfo) {
					MasonChooserParameterInfo mcpi = (MasonChooserParameterInfo) originalParameter;
					if (actualParameter.getDefinitionType() == ParameterInfo.CONST_DEF) {
						if (!mcpi.isValidValue(actualParameter.getValue().toString())) { 
							result.add(actualParameter.getValue().toString() + " is not a valid value for "+mcpi.getName()+". Valid values are: "+mcpi.getValidValuesString());
						}
					} else if (actualParameter.getDefinitionType() == ParameterInfo.LIST_DEF) {
						String[] actualValues = new String[actualParameter.getValues().size()];
						for (int i = 0; i < actualValues.length; i++) {
							actualValues[i] = actualParameter.getValues().get(i).toString();
						}
						if (!mcpi.isValidValues(actualValues)) {
							result.add("There are invalid values in the list for "+mcpi.getName()+". Valid values are: "+mcpi.getValidValuesString());
						}
					} else {
						if (!mcpi.isValidValues(actualParameter.getStartValue().toString(),actualParameter.getEndValue().toString(),actualParameter.getStep().toString())) {
							result.add("The defined parameter series contains invalid values for "+mcpi.getName()+". Valid values are: "+mcpi.getValidValuesString());
						}
					}
				}

			}
			if (result.size() > 0) {
				Utilities.userAlert(ParameterSweepWizard.getFrame(), result.toArray());
				return new boolean[] { false, false };
			}
			else 
				return new boolean[] { true, true };
		} else {
			return new boolean[] { false, false };
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private static ParameterInfo findInfo(List<ParameterInfo> params, String name) {
		for (int i = 0; i < params.size(); i++) {
			if (params.get(i).getName().equals(name)) return params.get(i);
		}
		return null;
	}

	//====================================================================================================
	// Repast Simphony section
	
	//----------------------------------------------------------------------------------------------------
	private static boolean _SimphonyIsEnabledForPageParameters(ParameterSweepWizard wizard, Button b) {
		switch (b) {
		case CANCEL : 
		case CUSTOM :
		case FINISH : 
		case BACK 	: return true;
		case NEXT 	: return (wizard != null && wizard.getModelFileName() != null);
		default : return false;
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private static boolean _SimphonyIsEnabledForPageRecorders(ParameterSweepWizard wizard, Button b) {
		if (wizard.getSweepingMethodID() == 0)
			return b != Button.NEXT; 
		else return true;
	}
	
	//----------------------------------------------------------------------------------------------------
	private static boolean _SimphonyAdditionalCheck(ParameterSweepWizard wizard) {
		return true;
	}

	//----------------------------------------------------------------------------------------------------
	private static boolean[] _SimphomyFinishCheck(Page_Recorders pageRecorders) { //TODO: ez biztos?
		if (pageRecorders.isTreeValid()) return new boolean[] { true, true };
		return new boolean[] { false, false };
	}

	//----------------------------------------------------------------------------------------------------
	private static DefaultMutableTreeNode _RepastSParseParameterFile(List<ParameterInfo> parameters, File paramFile) throws ParameterParserException {
		DefaultMutableTreeNode result = new DefaultMutableTreeNode("Parameter file"); 
		List<ParameterInfo> placed = new ArrayList<ParameterInfo>(parameters.size());
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder parser = factory.newDocumentBuilder();
			Document document = parser.parse(paramFile);
			
			Element sweepElement = document.getDocumentElement();
			NodeList nl = sweepElement.getElementsByTagName(RepastSXMLParameterFileGenerator.PARAMETER); // all parameters
			DefaultMutableTreeNode prev = null;
			if (nl != null && nl.getLength() > 0) {
				for (int i = 0;i < nl.getLength();++i) {
					Element paramElement = (Element) nl.item(i);
					ParameterInfo info = convertRepastSXMLTagToParamInfo(paramElement,parameters);
					DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(info);
					if (sweepElement.equals(paramElement.getParentNode()))
						result.add(treeNode);
					else
						prev.add(treeNode);
					placed.add(info);
					prev = treeNode;
				}
			}
			for (ParameterInfo info : parameters) {
				if (!placed.contains(info))
					result.add(new DefaultMutableTreeNode(info));
			}
			return result;
		} catch (ParserConfigurationException e) {
			throw new ParameterParserException(e);
		} catch (SAXException e) {
			throw new ParameterParserException(e);
		} catch (IOException e) {
			throw new ParameterParserException(e);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	protected static ParameterInfo convertRepastSXMLTagToParamInfo(Element paramElement, List<ParameterInfo> parameters) 
																   throws ParameterParserException {
		String name = paramElement.getAttribute(RepastSXMLParameterFileGenerator.NAME);
		if (name == null || name.length() == 0) 
			throw new ParameterParserException("missing or invalid 'name' attribute");
		name = Util.capitalize(name);
		ParameterInfo dummy = new ParameterInfo(name,"",null);
		ParameterInfo cDummy = new ParameterInfo(Util.uncapitalize(name),"",null);
		if (!parameters.contains(dummy) && !parameters.contains(cDummy)) 
			throw new ParameterParserException("Unknown referenced parameter: " + dummy.getName());
		int idx = parameters.indexOf(dummy);
		if (idx == -1)
			idx = parameters.indexOf(cDummy);
		ParameterInfo controlInfo = parameters.get(idx);
		ParameterInfo info = new ParameterInfo(name,controlInfo.getType(),controlInfo.getJavaType());		
		
		// set runs
		Element sweepElement = paramElement.getOwnerDocument().getDocumentElement(); 
		String runStr = sweepElement.getAttribute(RepastSXMLParameterFileGenerator.RUNS);
		if (runStr == null || runStr.length() == 0)
			throw new ParameterParserException("missing 'runs' attribute in parameter file");
		try {
			long runs = Long.parseLong(runStr);
			info.setRuns(runs);
		} catch (NumberFormatException e) {
			throw new ParameterParserException("invalid 'runs' attribute at parameter " + name,e);
		}
		
		// get definition type
		String defTypeStr = paramElement.getAttribute(RepastSXMLParameterFileGenerator.TYPE);
		if (defTypeStr == null || defTypeStr.length() == 0)
			throw new ParameterParserException("missing or invalid 'type' attribute at parameter " + name);
		
		if (RepastSXMLParameterFileGenerator.CONSTANT.equals(defTypeStr)) {
			info.setDefinitionType(ParameterInfo.CONST_DEF);	
			String typeStr = paramElement.getAttribute(RepastSXMLParameterFileGenerator.CONSTANT_TYPE);
			if (typeStr == null || defTypeStr.length() == 0)
				throw new ParameterParserException("missing or invalid 'constant_type' attribute at parameter " + name + ": '" + typeStr + "'");
			
			String valueStr = paramElement.getAttribute(RepastSXMLParameterFileGenerator.VALUE);
			if (valueStr == null || valueStr.length() == 0)
				throw new ParameterParserException("missing or invalid 'value' attribute at parameter " + name + ": '" + valueStr + "'");
			info.setType(calculateTypeFromValue(valueStr));
			Object value = convert(valueStr,info.getType());
			info.setValue(value);
		} else if (RepastSXMLParameterFileGenerator.LIST.equals(defTypeStr)) {
			info.setDefinitionType(ParameterInfo.LIST_DEF);	
			String typeStr = paramElement.getAttribute(RepastSXMLParameterFileGenerator.VALUE_TYPE);
			if (typeStr == null || defTypeStr.length() == 0)
				throw new ParameterParserException("missing or invalid 'value_type' attribute at parameter " + name + ": '" + typeStr + "'");
			info.setType(typeStr);			
			
			String valuesStr = paramElement.getAttribute(RepastSXMLParameterFileGenerator.VALUES);
			// TODO: the following thing doesn't handle Strings containing spaces. Needs revision.
			// TODO: it is possible that the attribute contains sg like this: "'foo bar' 'foo' 'bar'"
			ArrayList<String> valueStrs = new ArrayList<String>();
			StringTokenizer tokenizer = new StringTokenizer(valuesStr," ");
			while (tokenizer.hasMoreTokens()) 
				valueStrs.add(tokenizer.nextToken());

			List<Object> values = new ArrayList<Object>(valueStrs.size());
			for (int i = 0;i < valueStrs.size();++i) {
				String text = valueStrs.get(i);
				if (text == null || text.length() == 0)
					throw new ParameterParserException("invalid 'values' tag at parameter " + name + ": '" + text + "'");
				Object value = convert(text,info.getType());
				if (value == null)
					throw new ParameterParserException("invalid 'values' tag at parameter " + name + ": '" + text + "'");
				values.add(value);
			}
			info.setValues(values);
		} else if (RepastSXMLParameterFileGenerator.NUMBER.equals(defTypeStr)) {
			info.setDefinitionType(ParameterInfo.INCR_DEF);
			
			String startStr = paramElement.getAttribute(RepastSXMLParameterFileGenerator.START);
			if (startStr == null || startStr.length() == 0)
				throw new ParameterParserException("missing or invalid 'start' attribute at parameter " + name + ": '" + startStr + "'");

			String endStr = paramElement.getAttribute(RepastSXMLParameterFileGenerator.END);
			if (endStr == null || endStr.length() == 0)
				throw new ParameterParserException("missing or invalid 'end' attribute at parameter " + name);

			String stepStr = paramElement.getAttribute(RepastSXMLParameterFileGenerator.STEP);
			if (stepStr == null || stepStr.length() == 0)
				throw new ParameterParserException("missing or invalid 'step' attribute at parameter " + name);
			
			info.setType(calculateTypeFromValues(new String [] { startStr, endStr, startStr }));
			
			try {
				Number start = (Number) convert(startStr,info.getType());
				if (start == null)
					throw new NumberFormatException();
				info.setStartValue(start);
			} catch (ClassCastException e) {
				throw new ParameterParserException("invalid 'start' attribute at parameter " + name,e);
			} catch (NumberFormatException e) {
				throw new ParameterParserException("invalid 'start' attribute at parameter " + name,e);
			}

			try {
				Number end = (Number) convert(endStr,info.getType());
				if (end == null)
					throw new NumberFormatException();
				info.setEndValue(end);
			} catch (ClassCastException e) {
				throw new ParameterParserException("invalid 'end' attribute at parameter " + name,e);
			} catch (NumberFormatException e) {
				throw new ParameterParserException("invalid 'end' attribute at parameter " + name,e);
			}

			try {
				Number step = (Number) convert(stepStr,info.getType());
				if (step == null || step.doubleValue() == step.intValue() && step.intValue() == 0)
					throw new NumberFormatException();
				info.setStep(step);
			} catch (ClassCastException e) {
				throw new ParameterParserException("invalid 'step' attribute at parameter " + name,e);
			} catch (NumberFormatException e) {
				throw new ParameterParserException("invalid 'step' attribute at parameter " + name,e);
			}
		} else 
			throw new ParameterParserException("missing or invalid 'type' attribute at parameter " + name + ": '" + defTypeStr + "'");
		return info;
	}
	
	//----------------------------------------------------------------------------------------------------
	private static String calculateTypeFromValue(String value) {
		class Helper {
			static final int INT = 0, LONG = 1, FLOAT = 2, DOUBLE = 3;
			private int type = 0;

			private Helper(String value) {
				type = getType(value);
			}
			
			private String getTypeString() {
				if (type == FLOAT)
					return "Float";
				if (type == DOUBLE)
					return "Double";
				if (type == LONG)
					return "Long";
				return "Integer";
			}
			
			private int getType(String s) {
				if (s.toLowerCase().endsWith("f"))
					return FLOAT;
				if (s.contains(".") && !s.endsWith("."))
					return DOUBLE;
				if (s.toLowerCase().endsWith("l"))
					return LONG;
				return INT;
			}
		}
		
		return new Helper(value).getTypeString();
	}
	
	//----------------------------------------------------------------------------------------------------
	private static String calculateTypeFromValues(String[] values) {
		class Helper {
			static final int INT = 0, LONG = 1, FLOAT = 2, DOUBLE = 3;
			List<Integer> types = null;

			private Helper(String[] values) {
				types = new ArrayList<Integer>(values.length);
				for (String s : values) 
					types.add(getType(s));
			}
			
			private String getTypeString() {
				if (types.contains(FLOAT))
					return "Float";
				if (types.contains(DOUBLE))
					return "Double";
				if (types.contains(LONG))
					return "Long";
				return "Integer";
			}
			
			private int getType(String s) {
				if (s.toLowerCase().endsWith("f"))
					return FLOAT;
				if (s.contains(".") && !s.endsWith("."))
					return DOUBLE;
				if (s.toLowerCase().endsWith("l"))
					return LONG;
				return INT;
			}
		}
		
		return new Helper(values).getTypeString();
	}
	
	//-----------------------------------------------------------------------------------
	private static ArrayList<String> _getSimphonyCpEntries(ParameterSweepWizard wizard) {
		ArrayList<String> entries = new ArrayList<String>();
		
		String dirStr = PlatformManager.getInstallationDirectory(PlatformSettings.getPlatformType());
		File platformDir = new File(dirStr);
		
		File pluginsDir;
		
		if(Arrays.asList(platformDir.list()).contains("eclipse"))pluginsDir = new File(platformDir,"eclipse/plugins");
		else pluginsDir = new File(platformDir,"plugins");
			collectAllSimphonyJARs(pluginsDir,entries);
		/*if (OSUtils.getActual().getOSType() != OSType.UNIX){
			pluginsDir  = new File(platformDir,"eclipse/plugins");
			collectAllSimphonyJARs(pluginsDir,entries);
		} else {
			// on linux, Repast Simphony 2 does not have an eclipse folder
			pluginsDir = new File(platformDir,"plugins");
			collectAllSimphonyJARs(pluginsDir,entries);
		}*/
		for (int i = 0 ; i < entries.size() ; i++) {
			String jarName = entries.get(i);
			if (jarName.toLowerCase().contains("xmlpull")){
				entries.remove(i);
				entries.add(jarName);
				//break;
			}
		}
		
		//add bin and bin-groovy
		String modelDirStr = wizard.getModelRoot();
		if (modelDirStr != null){
			File modelDir = new File(modelDirStr);

			File cpEntry = new File(modelDir,"bin");
			entries.add(cpEntry.getAbsolutePath());	
			cpEntry = new File(modelDir,"bin-groovy");
			entries.add(cpEntry.getAbsolutePath());
		}
						
		return entries;
	}

	//----------------------------------------------------------------------------------------------------
	private static void collectAllSimphonyJARs(File f, List<String> entries) {
		if (f.exists()) {
			if (f.isDirectory()) {
				File[] files = f.listFiles();
				for (File _ : files)
					collectAllSimphonyJARs(_,entries);
			} else {
				if (f.getName().toLowerCase().endsWith(".jar"))
					entries.add(f.getAbsolutePath());
			}
		}
	}
	
	//====================================================================================================
	// End of Repast Simphony section
	
	//====================================================================================================
	// EMIL-S section
	
	//----------------------------------------------------------------------------------------------------
	private static boolean _EMILIsEnabledForPageLoadModel(ParameterSweepWizard wizard, Button b) {
		switch (b) {
		case CANCEL :
		case BACK	: return true;
		case CUSTOM :
		case FINISH :
		case NEXT	: return (wizard != null && wizard.getModelFileName() != null);
		default		: return false;
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private static boolean _EMILIsEnabledForPageParameters(ParameterSweepWizard wizard, Button b) { return true; }
	
	//----------------------------------------------------------------------------------------------------
	private static boolean _EMILIsEnabledForPageRecorders(ParameterSweepWizard wizard, Button b) {
		if (wizard.getSweepingMethodID() == 0)
			return b != Button.NEXT; 
		else return true;
	}
	
	//----------------------------------------------------------------------------------------------------
	private static boolean _EMILAdditionalCheck(ParameterSweepWizard wizard) { return true; }
	
	//----------------------------------------------------------------------------------------------------
	private static DefaultMutableTreeNode _EMILParseParameterFile(List<ParameterInfo> parameters, File paramFile) throws ParameterParserException {
		DefaultMutableTreeNode result = new DefaultMutableTreeNode("Parameter file"); 
		List<ParameterInfo> placed = new ArrayList<ParameterInfo>(parameters.size());
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder parser = factory.newDocumentBuilder();
			Document document = parser.parse(paramFile);
			
			Element sweepElement = document.getDocumentElement();
			NodeList nl = sweepElement.getElementsByTagName(EMILXmlParameterFileGenerator.PARAMETER); // all parameters
			DefaultMutableTreeNode prev = null;
			if (nl != null && nl.getLength() > 0) {
				for (int i = 0;i < nl.getLength();++i) {
					Element paramElement = (Element) nl.item(i);
					ParameterInfo info = convertToParameterInfo(paramElement,parameters);
					DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(info);
					if (sweepElement.equals(paramElement.getParentNode()))
						result.add(treeNode);
					else
						prev.add(treeNode);
					placed.add(info);
					prev = treeNode;
				}
			}
			for (ParameterInfo info : parameters) {
				if (!placed.contains(info))
					result.add(new DefaultMutableTreeNode(info));
			}
			return result;
		} catch (ParserConfigurationException e) {
			throw new ParameterParserException(e);
		} catch (SAXException e) {
			throw new ParameterParserException(e);
		} catch (IOException e) {
			throw new ParameterParserException(e);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private static ParameterInfo convertToParameterInfo(Element paramElement,List<ParameterInfo> parameters) throws ParameterParserException {
		String name = paramElement.getAttribute(EMILXmlParameterFileGenerator.NAME);
		if (name == null || name.length() == 0) 
			throw new ParameterParserException("missing or invalid 'name' attribute");
		name = Util.capitalize(name);
		ParameterInfo dummy = new ParameterInfo(name,"",null);
		ParameterInfo cDummy = new ParameterInfo(Util.uncapitalize(name),"",null);
		if (!parameters.contains(dummy) && !parameters.contains(cDummy)) 
			throw new ParameterParserException("Unknown referenced parameter: " + dummy.getName());
		int idx = parameters.indexOf(dummy);
		if (idx == -1)
			idx = parameters.indexOf(cDummy);
		ParameterInfo controlInfo = parameters.get(idx);
		ParameterInfo info = new ParameterInfo(name,controlInfo.getType(),controlInfo.getJavaType());
		
		String typeStr = paramElement.getAttribute(EMILXmlParameterFileGenerator.TYPE);
		if (typeStr == null || typeStr.length() == 0)
			throw new ParameterParserException("missing or invalid 'type' attribute at parameter " + name);
		if (!typeStr.equals(info.getType()))
			throw new ParameterParserException("invalid 'type' attribute at parameter " + name);
		
		String defTypeStr = paramElement.getAttribute(EMILXmlParameterFileGenerator.DEFINITION_TYPE);
		if (defTypeStr == null || defTypeStr.length() == 0)
			throw new ParameterParserException("missing or invalid 'definition_type' attribute at parameter " + name);
		int defType = ParameterInfo.defTypeFromString(defTypeStr);
		if (defType < 0)
			throw new ParameterParserException("invalid 'definition_type' attribute at parameter " + name);
		info.setDefinitionType(defType);
		
		String runStr = paramElement.getAttribute(EMILXmlParameterFileGenerator.RUNS);
		if (runStr == null || runStr.length() == 0)
			throw new ParameterParserException("missing 'runs' attribute at parameter " + name);
		try {
			long runs = Long.parseLong(runStr);
			info.setRuns(runs);
		} catch (NumberFormatException e) {
			throw new ParameterParserException("invalid 'runs' attribute at parameter " + name,e);
		}
		
		NodeList nl = paramElement.getElementsByTagName(EMILXmlParameterFileGenerator.VALUES);
		if (nl == null || nl.getLength() == 0)
			throw new ParameterParserException("missing 'values' tag at parameter " + name);
		Element valuesElement = (Element) nl.item(0);
		
		if (defType == ParameterInfo.INCR_DEF) {
			String startStr = valuesElement.getAttribute(EMILXmlParameterFileGenerator.START);
			if (startStr == null || startStr.length() == 0)
				throw new ParameterParserException("missing or invalid 'start' attribute at parameter " + name);
			try {
				Number start = (Number) convert(startStr,info.getType());
				if (start == null)
					throw new NumberFormatException();
				info.setStartValue(start);
			} catch (ClassCastException e) {
				throw new ParameterParserException("invalid 'start' attribute at parameter " + name,e);
			} catch (NumberFormatException e) {
				throw new ParameterParserException("invalid 'start' attribute at parameter " + name,e);
			}

			String endStr = valuesElement.getAttribute(EMILXmlParameterFileGenerator.END);
			if (endStr == null || endStr.length() == 0)
				throw new ParameterParserException("missing or invalid 'end' attribute at parameter " + name);
			try {
				Number end = (Number) convert(endStr,info.getType());
				if (end == null)
					throw new NumberFormatException();
				info.setEndValue(end);
			} catch (ClassCastException e) {
				throw new ParameterParserException("invalid 'end' attribute at parameter " + name,e);
			} catch (NumberFormatException e) {
				throw new ParameterParserException("invalid 'end' attribute at parameter " + name,e);
			}

			String stepStr = valuesElement.getAttribute(EMILXmlParameterFileGenerator.STEP);
			if (stepStr == null || stepStr.length() == 0)
				throw new ParameterParserException("missing or invalid 'step' attribute at parameter " + name);
			try {
				Number step = (Number) convert(stepStr,info.getType());
				if (step == null || step.doubleValue() == step.intValue() && step.intValue() == 0)
					throw new NumberFormatException();
				info.setStep(step);
			} catch (ClassCastException e) {
				throw new ParameterParserException("invalid 'step' attribute at parameter " + name,e);
			} catch (NumberFormatException e) {
				throw new ParameterParserException("invalid 'step' attribute at parameter " + name,e);
			}
		} else { // constant or list
			nl = valuesElement.getElementsByTagName(EMILXmlParameterFileGenerator.VALUE);
			if (nl == null || nl.getLength() == 0)
				throw new ParameterParserException("missing 'value' tag at parameter " + name);
			List<Object> values = new ArrayList<Object>(nl.getLength());
			for (int i = 0; i < nl.getLength();++i) {
				Element valueElement = (Element) nl.item(i);
				Text text = (Text) valueElement.getChildNodes().item(0);
				if (text == null || text.getNodeValue().length() == 0)
					throw new ParameterParserException("invalid 'value' tag at parameter " + name);
				Object value = convert(text.getNodeValue(),info.getType());
				if (value == null)
					throw new ParameterParserException("invalid 'value' tag at parameter " + name);
				values.add(value);
			}
			info.setValues(values);
		}
		return info;
	}
	
	//------------------------------------------------------------------------------
	/** Converts <code>value</code> to type <code>type</code>.
	 * @param type the name of the new type (if this is a primitive type we use 
	 *             its wrapper type instead.
	 */
	private static Object convert(String value, String type) throws NumberFormatException {
		if (type.equals("String"))
			return value;
		if (type.equals("boolean") || type.equals("Boolean"))
			return new Boolean(value);
		if (type.equals("double") || type.equals("Double") || type.equals("number") || type.equals("Number"))
			return new Double(value);
		if (type.equals("byte") || type.equals("Byte"))
			return new Byte(value);
		if (type.equals("short") || type.equals("Short"))
			return new Short(value);
		if (type.equals("int") || type.equals("Integer"))
			return new Integer(value);
		if (type.equals("long") || type.equals("Long"))
			return new Long(value.replaceAll("l|L",""));
		if (type.equals("float") || type.equals("Float"))
			return new Float(value);
		return null;
	}
	
	//----------------------------------------------------------------------------------------------------
	private static boolean[] _EMILFinishCheck(Page_Recorders pageRecorders) {
		if (pageRecorders.isTreeValid()) return new boolean[] { true, true };
		return new boolean[] { false, false };
	}
	
	//====================================================================================================
	// End of EMIL-S section
	
	//----------------------------------------------------------------------------------------------------
	// NetLogo section
	
	//----------------------------------------------------------------------------------------------------
	private static boolean _NetLogoIsEnabledForPageLoadModel(ParameterSweepWizard wizard, Button b) {
		return _RepastIsEnabledForPageLoadModel(wizard,b);
	}
	
	//----------------------------------------------------------------------------------------------------
	private static boolean _NetLogoIsEnabledForPageParameters() { return true; }
	
	//----------------------------------------------------------------------------------------------------
	private static boolean _NetLogoIsEnabledForPageRecorders(ParameterSweepWizard wizard, Button b) {
		boolean result = (b == Button.FINISH ? wizard.isRecordersTreeValid() : true);
		if (wizard.getSweepingMethodID() == 0)
			return b != Button.NEXT && result; 
		else 
			return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	private static boolean _NetLogoAdditionalCheck() { return true; }
	
	//----------------------------------------------------------------------------------------------------
	private static List<String> _NetLogoAdditionalParameterCheck(ParameterInfo info, String[] newValues, int defType) {
		List<String> result = new ArrayList<String>();
		if (info instanceof ChooserParameterInfo) {
			ChooserParameterInfo cpi = (ChooserParameterInfo) info;
			if (defType == ParameterInfo.CONST_DEF) {
				if (!cpi.isValidValue(newValues[0])) 
					result.add(newValues[0] + " is not a valid value. Valid values are: " + cpi.getValidValues());
			} else if (defType == ParameterInfo.LIST_DEF) {
				String values = Utils.join(" ",(Object[])newValues);
				if (!cpi.isValidValues(values))
					result.add("There are invalid values in the list. Valid values are: " + cpi.getValidValues());
			} else {
				if (!cpi.isValidValues(newValues[0],newValues[1],newValues[2]))
					result.add("The defined parameter series contains invalid values. Valid values are: " + cpi.getValidValues());
			}
		}
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	private static DefaultMutableTreeNode _NetLogoParseParameterFile(List<ParameterInfo> parameters, File paramFile)
																	throws ParameterParserException {
		DefaultMutableTreeNode result = new DefaultMutableTreeNode("Parameter file"); 
		List<ParameterInfo> placed = new ArrayList<ParameterInfo>(parameters.size());
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder parser = factory.newDocumentBuilder();
			Document document = parser.parse(paramFile);
			
			Element sweepElement = document.getDocumentElement();
			String runStr = sweepElement.getAttribute(NetLogoParameterFileGenerator.RUNS);
			long runs = 1;
			if (runStr == null || runStr.length() == 0)
				throw new ParameterParserException("missing 'runs' attribute");
			try {
				runs = Long.parseLong(runStr);
			} catch (NumberFormatException e) {
				throw new ParameterParserException("invalid 'runs' attribute",e);
			}
			
			NodeList nl = sweepElement.getElementsByTagName(NetLogoParameterFileGenerator.PARAMETER); // all parameters
			DefaultMutableTreeNode prev = null;
			if (nl != null && nl.getLength() > 0) {
				for (int i = 0;i < nl.getLength();++i) {
					Element paramElement = (Element) nl.item(i);
					ParameterInfo info = NLConvertToParameterInfo(paramElement,parameters,runs);
					DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(info);
					if (sweepElement.equals(paramElement.getParentNode()))
						result.add(treeNode);
					else
						prev.add(treeNode);
					placed.add(info);
					prev = treeNode;
				}
			}
			for (ParameterInfo info : parameters) {
				if (!placed.contains(info))
					result.add(new DefaultMutableTreeNode(info));
			}
			return result;
		} catch (ParserConfigurationException e) {
			throw new ParameterParserException(e);
		} catch (SAXException e) {
			throw new ParameterParserException(e);
		} catch (IOException e) {
			throw new ParameterParserException(e);
		}

	}
	
	//----------------------------------------------------------------------------------------------------
	private static ParameterInfo NLConvertToParameterInfo(Element paramElement,List<ParameterInfo> parameters, long runs) 
														  throws ParameterParserException {
		String name = paramElement.getAttribute(NetLogoParameterFileGenerator.NAME);
		if (name == null || name.length() == 0) 
			throw new ParameterParserException("missing or invalid 'name' attribute");
		name = Util.capitalize(name);
		ParameterInfo dummy = new ParameterInfo(name,"",null);
		ParameterInfo cDummy = new ParameterInfo(Util.uncapitalize(name),"",null);
		if (!parameters.contains(dummy) && !parameters.contains(cDummy)) 
			throw new ParameterParserException("Unknown referenced parameter: " + dummy.getName());
		int idx = parameters.indexOf(dummy);
		if (idx == -1)
			idx = parameters.indexOf(cDummy);
		ParameterInfo controlInfo = parameters.get(idx);
		ParameterInfo info = null;
		if (controlInfo instanceof ChooserParameterInfo) {
			ChooserParameterInfo cci = (ChooserParameterInfo) controlInfo;
			info = new ChooserParameterInfo(name,cci.getType(),cci.getJavaType(),cci.getValidValues());
		} else
			info = new ParameterInfo(name,controlInfo.getType(),controlInfo.getJavaType());
		
		String typeStr = paramElement.getAttribute(NetLogoParameterFileGenerator.TYPE);
		if (typeStr == null || typeStr.length() == 0)
			throw new ParameterParserException("missing or invalid 'type' attribute at parameter " + name);
		if (!typeStr.equals(info.getType()))
			throw new ParameterParserException("invalid 'type' attribute at parameter " + name);
		
		String defTypeStr = paramElement.getAttribute(NetLogoParameterFileGenerator.DEFINITION_TYPE);
		if (defTypeStr == null || defTypeStr.length() == 0)
			throw new ParameterParserException("missing or invalid 'definition_type' attribute at parameter " + name);
		int defType = ParameterInfo.defTypeFromString(defTypeStr);
		if (defType < 0)
			throw new ParameterParserException("invalid 'definition_type' attribute at parameter " + name);
		info.setDefinitionType(defType);
		
		info.setRuns(runs);
		
		if (info instanceof ChooserParameterInfo) {
			ChooserParameterInfo cci = (ChooserParameterInfo) info;
			String validValuesStr = paramElement.getAttribute(NetLogoParameterFileGenerator.VALID_VALUES);
			if (validValuesStr == null || validValuesStr.length() == 0)
				throw new ParameterParserException("missing or invalid 'valid_values' attribute at parameter " + name);
			if (!validValuesStr.endsWith(cci.validValuesToString()))
				throw new ParameterParserException("invalid 'valid_values' attribute at parameter " + name);
		}

		if (defType == ParameterInfo.INCR_DEF) {
			Number start = null, end = null, step = null;
			String startStr = paramElement.getAttribute(NetLogoParameterFileGenerator.START);
			if (startStr == null || startStr.length() == 0)
				throw new ParameterParserException("missing or invalid 'start' attribute at parameter " + name);
			try {
				start = (Number) convert(startStr,info.getType());
				if (start == null)
					throw new NumberFormatException();
			} catch (ClassCastException e) {
				throw new ParameterParserException("invalid 'start' attribute at parameter " + name,e);
			} catch (NumberFormatException e) {
				throw new ParameterParserException("invalid 'start' attribute at parameter " + name,e);
			}

			String endStr = paramElement.getAttribute(NetLogoParameterFileGenerator.END);
			if (endStr == null || endStr.length() == 0)
				throw new ParameterParserException("missing or invalid 'end' attribute at parameter " + name);
			try {
				end = (Number) convert(endStr,info.getType());
				if (end == null)
					throw new NumberFormatException();
			} catch (ClassCastException e) {
				throw new ParameterParserException("invalid 'end' attribute at parameter " + name,e);
			} catch (NumberFormatException e) {
				throw new ParameterParserException("invalid 'end' attribute at parameter " + name,e);
			}

			String stepStr = paramElement.getAttribute(NetLogoParameterFileGenerator.STEP);
			if (stepStr == null || stepStr.length() == 0)
				throw new ParameterParserException("missing or invalid 'step' attribute at parameter " + name);
			try {
				step = (Number) convert(stepStr,info.getType());
				if (step == null || step.doubleValue() == step.intValue() && step.intValue() == 0)
					throw new NumberFormatException();
			} catch (ClassCastException e) {
				throw new ParameterParserException("invalid 'step' attribute at parameter " + name,e);
			} catch (NumberFormatException e) {
				throw new ParameterParserException("invalid 'step' attribute at parameter " + name,e);
			}
			
			if (info instanceof ChooserParameterInfo) {
				ChooserParameterInfo ci = (ChooserParameterInfo) info;
				if (!ci.isValidValues(startStr,endStr,stepStr))
					throw new ParameterParserException("invalid parameter definition at parameter " + name);
			}
			info.setStep(step);
			info.setStartValue(start);
			info.setEndValue(end);
		} else if (defType == ParameterInfo.CONST_DEF) { // constant
			String valueStr = paramElement.getAttribute(NetLogoParameterFileGenerator.VALUE);
			if (valueStr == null || valueStr.length() == 0)
				throw new ParameterParserException("missing or invalid 'value' attribute at parameter " + name);
			Object value = convert(valueStr,info.getType());
			if (value == null)
				throw new ParameterParserException("invalid 'value' tag at parameter " + name);
			if (info instanceof ChooserParameterInfo) {
				ChooserParameterInfo ci = (ChooserParameterInfo) info;
				if (!ci.isValidValue(valueStr))
					throw new ParameterParserException("invalid parameter definition at parameter " + name);
			}
			info.setValue(value);
		} else { // list
			String valuesStr = paramElement.getAttribute(NetLogoParameterFileGenerator.VALUES);
			if (valuesStr == null || valuesStr.length() == 0)
				throw new ParameterParserException("missing or invalid 'values' attribute at parameter " + name);
			if (info instanceof ChooserParameterInfo) {
				ChooserParameterInfo ci = (ChooserParameterInfo) info;
				if (!ci.isValidValue(valuesStr))
					throw new ParameterParserException("invalid parameter definition at parameter " + name);
			}
			String[] vals = valuesStr.trim().split(" ");
			List<Object> values = new ArrayList<Object>(vals.length);
			for (String v : vals) {
				Object value = convert(v,info.getType());
				if (value == null)
					throw new ParameterParserException("invalid 'value' tag at parameter " + name);
				values.add(value);
			}
			info.setValues(values);
		}
		return info;
	}
	
	//----------------------------------------------------------------------------------------------------
	private static boolean[] _NetLogoFinishCheck(ParameterSweepWizard wizard, Page_Recorders pageRecorders) {
		if (pageRecorders.getRecorders().getChildCount() == 1) return new boolean[] { true, true };
		if (!pageRecorders.isTreeValid()) return new boolean[] { false, false };
		
		int result = Utilities.askUserOK(ParameterSweepWizard.getFrame(),"Warning",
										 "NetLogo supports only one recorder per experiment.","" +
										 "The others will be ignored.",
										 "Click 'OK' to continue anyways.",
				 						 "If you would like to change your recorder settings click 'Cancel'.");
		return new boolean [] { (result == 1), true };
	}
	
	//-----------------------------------------------------------------------------------
	private static ArrayList<String> _getNetlogoCpEntries(ParameterSweepWizard wizard) {
		ArrayList<String> entries = new ArrayList<String>();
		
		String dirStr = PlatformManager.getInstallationDirectory(PlatformSettings.getPlatformType());
		File platformDir = new File(dirStr);
		
		File pluginsDir = new File(platformDir,"lib");
		collectAllJARs(pluginsDir,entries);

		pluginsDir = new File(platformDir,"extensions");
		collectAllJARs(pluginsDir,entries);

		return entries;
	}

	private static void collectAllJARs(File f, List<String> entries) {
		if (f.exists()) {
			if (f.isDirectory()) {
				File[] files = f.listFiles();
				for (File _ : files)
					collectAllSimphonyJARs(_,entries);
			} else {
				if (f.getName().toLowerCase().endsWith(".jar"))
					entries.add(f.getAbsolutePath());
			}
		}
	}
	

	//====================================================================================================
	// End of NetLogo section
	
	//====================================================================================================
	// nested classes
	
	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("serial")
	public static class UnsupportedPlatformException extends RuntimeException {
		
		//----------------------------------------------------------------------------------------------------
		public UnsupportedPlatformException() { super(); }
		public UnsupportedPlatformException(String message,Throwable cause) { super(message,cause); }
		public UnsupportedPlatformException(String message) { super(message); }
		public UnsupportedPlatformException(Throwable cause) { super(cause); }
	}
}
