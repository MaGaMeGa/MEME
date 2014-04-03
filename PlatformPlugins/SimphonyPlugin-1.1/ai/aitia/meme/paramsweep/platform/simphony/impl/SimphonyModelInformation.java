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
package ai.aitia.meme.paramsweep.platform.simphony.impl;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javassist.ClassPath;
import javassist.NotFoundException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ai.aitia.meme.paramsweep.batch.IModelInformation;
import ai.aitia.meme.paramsweep.batch.output.NonRecordableInfo;
import ai.aitia.meme.paramsweep.batch.output.RecordableInfo;
import ai.aitia.meme.paramsweep.batch.output.RecorderInfo;
import ai.aitia.meme.paramsweep.batch.param.ParameterInfo;
import ai.aitia.meme.paramsweep.platform.IPSWInformationProvider;
import ai.aitia.meme.paramsweep.utils.ClassPathPair;
import ai.aitia.meme.paramsweep.utils.Util;

public class SimphonyModelInformation implements IModelInformation {

	//=====================================================================================
	// members
	
	private ArrayList<ParameterInfo<?>> modelParameters = null;
	private ArrayList<RecordableInfo> recordables = null;
	private IPSWInformationProvider infoProvider = null;
	
	//=====================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public SimphonyModelInformation(IPSWInformationProvider provider) {
		infoProvider = provider;
	}
	
	//=====================================================================================
	// implemented interfaces
	
	//----------------------------------------------------------------------------------------------------
	public List<ParameterInfo<?>> getParameters() throws ModelInformationException {
		if (modelParameters == null) {
			try {
				modelParameters = createParametersFromScore(infoProvider.getEntryPoint());
			} catch (IOException e) {
				// failed to retrieve parameter information
				throw new ModelInformationException("Cannot retrieve parameter information.");
			} catch (SAXException e) {
				// failed to retrieve parameter information
				throw new ModelInformationException("An error occured when parsing the .score file.");
			} catch (ParserConfigurationException e) {
				// failed to retrieve parameter information
				throw new ModelInformationException("Cannot retrieve parameter information.");
			}
		}
		return modelParameters;
	}
	
	//-------------------------------------------------------------------------------------
	public List<RecordableInfo> getRecordables() throws ModelInformationException {
		if (recordables == null) {
			try {
				recordables = getRecordablesFromScore(infoProvider.getEntryPoint());
			} catch (IOException e) {
				// failed to retrieve recordable information
				throw new ModelInformationException("Cannot retrieve recordable information.");
			} catch (SAXException e) {
				// failed to retrieve recordable information
				throw new ModelInformationException("An error occured when parsing the .score file.");
			} catch (ParserConfigurationException e) {
				// failed to retrieve recordable information
				throw new ModelInformationException("Cannot retrieve recordable information.");
			}
		}
		return recordables;
	}
	
	//-------------------------------------------------------------------------------------
	public String getName(){
		String modelRoot = infoProvider.getModelRoot();
		return modelRoot.substring(modelRoot.lastIndexOf(File.separator));
	}
	
	//-------------------------------------------------------------------------------------
	public List<NonRecordableInfo> getNonRecordables() throws ModelInformationException {
		// TODO: ez majd v�ltozik
		return null;
	}
	
	//=====================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	/** Creates the list of information objects of parameters. */
	@SuppressWarnings("unchecked")
	protected ArrayList<ParameterInfo<?>> createParametersFromScore(String fileName) throws ParserConfigurationException, SAXException,	IOException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder parser = factory.newDocumentBuilder();
		Document document = parser.parse(new File(fileName).toURI().toString());
		
		Element rootElement = document.getDocumentElement();

		// all attributes
		NodeList nl = rootElement.getElementsByTagName("attributes");
		ArrayList <ParameterInfo<?>> createdParameters = new ArrayList<ParameterInfo<?>>(nl.getLength());
		ArrayList<String> initParamResultList = new ArrayList<String>();
		
		for (int i = 0;i < nl.getLength();++i) {
			Element param = (Element) nl.item(i);
			if (!param.getParentNode().equals(rootElement)) continue;
			
			String paramName = param.getAttribute("ID");
			initParamResultList.add(paramName);
			String type = param.getAttribute("sType");
			String defaultValue = param.getAttribute("defaultValue");

			ParameterInfo pi = createParameterInfo(Util.capitalize(paramName),type,defaultValue);
			createdParameters.add(pi);
		}
		
		NodeList projectionList = rootElement.getElementsByTagName("projections");
		
		for (int i = 0;i < projectionList.getLength();++i) {
			Element prElement = (Element) projectionList.item(i);
			String prId = prElement.getAttribute("ID");
			NodeList prAttributes = prElement.getElementsByTagName("attributes");
			
			for (int j = 0;j < prAttributes.getLength();++j) {
				Element param = (Element) prAttributes.item(j);
				String attrId = param.getAttribute("ID");
				if (attrId.equalsIgnoreCase("dimensions")) continue;

				String paramName = prId + attrId;
				initParamResultList.add(paramName);
				String type = param.getAttribute("sType");
				String defaultValue = param.getAttribute("defaultValue");
				ParameterInfo pi = createParameterInfo(Util.capitalize(paramName),type,defaultValue);
				createdParameters.add(pi);
			}
		}
		
		ParameterInfo<Long> seedInfo = new ParameterInfo<Long>("RandomSeed","",0L);
		if (!createdParameters.contains(seedInfo)) 
			createdParameters.add(seedInfo);
		
		return createdParameters;
	}
	
	//-------------------------------------------------------------------------------------
	protected ArrayList<RecordableInfo> getRecordablesFromScore(String scoreFile) throws IOException, ParserConfigurationException, SAXException,
																						 ModelInformationException {
		// get the implementation package of the model
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder parser = factory.newDocumentBuilder();
		Document document = parser.parse(new File(scoreFile).toURI().toString());
		
		Element rootElement = document.getDocumentElement();
		NodeList nl = rootElement.getElementsByTagName("implementation");
		Element impl = (Element) nl.item(0);
		String implementationPackage = impl.getAttribute("package");
		
		//simphonyModelName = Utilities.capitalize( implementationPackage );
		//contextCreatorFQName = implementationPackage + "." + impl.getAttribute( "className" );
		
		// add bin and bin-groovy to the classpath
		String binPath = infoProvider.getModelRoot() + File.separator + "bin";
		String binGroovyPath = infoProvider.getModelRoot() + File.separator + "bin-groovy";
		
		ArrayList<RecordableInfo> recordables = new ArrayList<RecordableInfo>();
		// set classPath
		try {
			ClassPathPair pair = new ClassPathPair(binPath,null);
			if (!infoProvider.getClassPathListModel().contains(pair)) {
				ClassPath cp = infoProvider.getClassPool().insertClassPath(binPath);
				pair.setClassPath(cp);
				infoProvider.getClassPathListModel().add(0,pair);
			}
			
			pair = new ClassPathPair(binGroovyPath,null);
			if (!infoProvider.getClassPathListModel().contains(pair)) {
				ClassPath cp = infoProvider.getClassPool().insertClassPath(binGroovyPath);
				pair.setClassPath(cp);
				infoProvider.getClassPathListModel().add(0,pair);
			}
		} catch (NotFoundException e) {
			throw new ModelInformationException(Util.getLocalizedMessage(e),e);
		}
		
		// dig into the agent
		nl = rootElement.getElementsByTagName("agents");
		ArrayList<String> fullyQualifiedAgentClassNames = new ArrayList<String>();
		for (int i = 0;i < nl.getLength();++i) {
			Element agent = (Element) nl.item(i);
			NodeList implTags = agent.getElementsByTagName("implementation");
			String packageName = ((Element)implTags.item(0)).getAttribute("package");
			String className = packageName.length() == 0 ? implementationPackage : packageName;
			className += "." + ((Element)implTags.item(0)).getAttribute("className");
			fullyQualifiedAgentClassNames.add(className);
		}
		
//		List<String> reserved = new ArrayList<String>( Arrays.asList( owner.getInitParam() ) );
//		if( ParameterSweepWizard.getPreferences().rngSeedAsParameter() )
//			reserved.add( "rngSeed" );
		
		for (int i = 0;i < fullyQualifiedAgentClassNames.size();++i) {
			String agentClassName = fullyQualifiedAgentClassNames.get(i);
			Class agentClass = null;
			try {
				agentClass = Class.forName(agentClassName,true,infoProvider.getCustomClassLoader());
			} catch (ClassNotFoundException e) {
				throw new ModelInformationException("Agent class not found: " + agentClassName,e);
			}
			
			Method[] agentMethods = agentClass.getMethods();
			Field[] agentFields = agentClass.getFields();
			
			for (int j = 0;j < agentMethods.length;++j) {
				Method method = agentMethods[j];
				String methodName = method.getName();
				if (method.getReturnType() == Void.TYPE || method.getParameterTypes().length > 0 ||	methodName.equalsIgnoreCase("getClass") ||
					methodName.equalsIgnoreCase("hashCode") || methodName.equalsIgnoreCase("clone")) continue;
				
				String fieldName = null;
				if (methodName.startsWith("get") && methodName.length() > 3) {
					String nameSuffix = Util.uncapitalize(methodName.substring(3));
					for (int k = 0;k < agentFields.length;++k) {
						if (nameSuffix.equalsIgnoreCase(agentFields[k].getName())) {
							fieldName = nameSuffix;
							break;
						}
					}
				}
				
				// let only agents field be recorded...
				if (fieldName == null) continue;
				
				RecordableInfo ri = new RecordableInfo(fieldName,method.getReturnType(),fullyQualifiedAgentClassNames.get(i) + "." + methodName + "()");
				recordables.add(ri);
			}
		}
		return recordables;
	}
	
	//-------------------------------------------------------------------------------------
	private ParameterInfo<?> createParameterInfo(String name, String type, String value) throws NumberFormatException {
		if (type.equals("FLOAT")) {
			Float floatValue = Float.parseFloat(value);
			return new ParameterInfo<Float>(name,null,floatValue);
		} else if( type.equals("INTEGER")) {
			Integer intValue = Integer.parseInt(value);
			return new ParameterInfo<Integer>(name,null,intValue);
		} else if (type.equals("BOOLEAN")) {
			Boolean boolValue = Boolean.parseBoolean(value);
			return new ParameterInfo<Boolean>(name,null,boolValue);
		} else if (type.equals("Long")) { // TODO ????
			Long longValue = Long.parseLong(value);
			return new ParameterInfo<Long>(name,null,longValue);
		} else if (type.equals("STRING")) {
			return new ParameterInfo<String>(name,null,value);
		} else if (type.equals("UNDEFINED")) {
			//TODO: figure out how 'undefined' behaves
			return new ParameterInfo<String>(name,null,value);
		} else if (type.equals("File")) {
			//TODO: figure out how 'file' behaves
			return new ParameterInfo<String>(name,null,value);
		}
		return null;
	}
	
	//-------------------------------------------------------------------------------------
	@SuppressWarnings("unused")
	private String convertSimphonyTypeName(String sType) {
		if (sType.equalsIgnoreCase("FLOAT"))
			return "float";
		if (sType.equalsIgnoreCase("INTEGER"))
			return "int";
		if (sType.equalsIgnoreCase("BOOLEAN"))
			return "boolean";
		if (sType.equalsIgnoreCase("Long"))
			return "long";
		if (sType.equalsIgnoreCase("STRING"))
			return null; // ????
		if (sType.equalsIgnoreCase("UNDEFINED"))
			return null;
		if (sType.equalsIgnoreCase("File"))
			return null;
		return null;
	}

	@Override
	public List<RecorderInfo> getRecorders() throws ModelInformationException {
		return new ArrayList<RecorderInfo>();
	}

	@Override
	public String getRecordersXML() throws ModelInformationException {
		// TODO Auto-generated method stub
		return null;
	}
}
