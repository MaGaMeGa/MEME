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
package ai.aitia.meme.paramsweep.platform.simphony;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.List;

import javassist.NotFoundException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ai.aitia.meme.paramsweep.batch.IBatchController;
import ai.aitia.meme.paramsweep.batch.IModelInformation;
import ai.aitia.meme.paramsweep.batch.IParameterPartitioner;
import ai.aitia.meme.paramsweep.batch.IParameterSweepResultReader;
import ai.aitia.meme.paramsweep.batch.output.RecorderInfo;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings;
import ai.aitia.meme.paramsweep.platform.DefaultPluginPlatform;
import ai.aitia.meme.paramsweep.platform.IPSWInformationProvider;
import ai.aitia.meme.paramsweep.platform.IResultFileTool;
import ai.aitia.meme.paramsweep.platform.PlatformManager.PlatformType;
import ai.aitia.meme.paramsweep.platform.simphony.impl.IntelliSweepSimphonyResultParser;
import ai.aitia.meme.paramsweep.platform.simphony.impl.RepastSOutputterGenerator;
import ai.aitia.meme.paramsweep.platform.simphony.impl.SimphonyBatchController;
import ai.aitia.meme.paramsweep.platform.simphony.impl.SimphonyModelInformation;
import ai.aitia.meme.paramsweep.platform.simphony.impl.SimphonyParameterPartitioner;
import ai.aitia.meme.paramsweep.platform.simphony.impl.SimphonyResultFileMerger;
import ai.aitia.meme.paramsweep.utils.Util;

public class SimphonyPlatform extends DefaultPluginPlatform {
	
	//=====================================================================================
	// members

	private static final long serialVersionUID = -3930821827036318583L;
	public static final String recorderPrefix = "ai.aitia.recorder.path.prefix";
	
	protected String generatedContextCreator = null;
	protected boolean local = false;
	protected transient ClassLoader customLoader = null;
	
	//====================================================================================================
	// methods
	
	//=====================================================================================
	//implemented interfaces
	
	//----------------------------------------------------------------------------------------------------
	public String getDisplayableName() { return "Repast Simphony 1.1"; }
	public PlatformType getPlatfomType() { return PlatformType.SIMPHONY; }
	public String getVersion() { return "1.1"; }
	public IModelInformation getModelInformation(IPSWInformationProvider provider) { return new SimphonyModelInformation(provider); }
	public IParameterPartitioner getParameterPartitioner() { return new SimphonyParameterPartitioner(); }
	public String checkCondition(String condition, IPSWInformationProvider provider) { return null; }
	public IParameterSweepResultReader getReader(List<RecorderInfo> recorders) { return new IntelliSweepSimphonyResultParser(recorders); }
	public List<File> prepareResult(List<RecorderInfo> recorders, File workingDir) { return new SimphonyResultFileMerger().merge(recorders,workingDir); }
	
	//----------------------------------------------------------------------------------------------------
	public List<File> prepareResult(List<RecorderInfo> recorders, List<String> suffixes, File workingDir) {
		// TODO: megval�s�tani
		return null;
	}
	
	//----------------------------------------------------------------------------------------------------
	public String getDescription() {
		return "Repast Simphony is a free and open source agent-based modeling toolkit that simplifies model creation and use. \n\n See " +
			   "http://repast.sourceforge.net/ for further details.";
	}
	
	//----------------------------------------------------------------------------------------------------
	public IBatchController getBatchController() {
		if (local) {
			try {
				Class<?> bcClass = Class.forName("ai.aitia.meme.paramsweep.platform.simphony.impl.SimphonyBatchController",true,customLoader);
				Constructor<?> constructor = bcClass.getConstructor(String.class);
				return (IBatchController) constructor.newInstance(generatedContextCreator);
			} catch (Throwable t) {
				throw new RuntimeException(t);
			}
		}
		return new SimphonyBatchController(generatedContextCreator);
	}
	
	//----------------------------------------------------------------------------------------------------
	public String prepareModel(IPSWInformationProvider provider) {
		local = provider.isLocalRun();
		String entryPoint = provider.getEntryPoint();
		
		// get the implementation package of the model
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder parser = null;
		Document document = null;
		try {
			parser = factory.newDocumentBuilder();
			document = parser.parse(new File(entryPoint).toURI().toString());
		} catch (ParserConfigurationException e) {
			return "Cannot parse model score file due parser configuration problems!";
		} catch (SAXException e) {
			return "Cannot parse model score file!";
		} catch (IOException e) {
			return "Cannot read model score file!";
		}
		
		Element rootElement = document.getDocumentElement();
		NodeList nl = rootElement.getElementsByTagName("implementation");
		Element impl = (Element) nl.item(0);
		String implementationPackage = impl.getAttribute("package");
		String simphonyModelName = Util.capitalize(implementationPackage);
		String contextCreatorFQName = implementationPackage + "." + impl.getAttribute("className");
		
		// add bin and bin-groovy to the classpath
		String binPath = provider.getModelRoot();
		binPath += File.separator + "bin";
		try {
			provider.getClassPool().appendClassPath(binPath);
		} catch (NotFoundException e) {
			return "Class path entry is not found: " + binPath;
		}
		
		String binGroovyPath = provider.getModelRoot();
		binGroovyPath += File.separator + "bin-groovy";
		try {
			provider.getClassPool().appendClassPath(binGroovyPath);
		} catch (NotFoundException e) {
			return "Class path entry is not found: " + binGroovyPath;
		}
		
		RepastSOutputterGenerator generator = new RepastSOutputterGenerator(simphonyModelName,contextCreatorFQName,provider.getClassPool(),
										   									provider.getRecorders(),provider.getStoppingCondition(),true,
										   									binPath);
		
		String error = generator.generateOutputters();
		String error2 = null;
		if (provider.writeSource())
			error2 = generator.writeSources();
		if (error != null || error2 != null) 
			return (error == null ? "" : error) + (error2 == null ? "" : "\n" + error2);
		
		generatedContextCreator = generator.getGeneratedModelName();
		// TODO: meg kell majd v�ltoztatni, ha m�r nem model.score lesz a model neve
		int idx1 = generatedContextCreator.lastIndexOf("__");
		String timestamp = generatedContextCreator.substring(idx1);
		provider.setGeneratedModelName("model.score" + timestamp);
		
		return null;
	}
														 		
	//----------------------------------------------------------------------------------------------------
	public String checkModel(IPSWInformationProvider provider) {
		customLoader = provider.getCustomClassLoader();
		
		if (!provider.getEntryPoint().endsWith(".score"))
			return "The model file is not a .score file!";
		
		String binPath = provider.getModelRoot();
		binPath += File.separator + "bin";
		if (!new File(binPath).exists())
			return "There is no bin directory in the model directory.";
		try {
			if (!isScoreFile(new File(provider.getEntryPoint())))
				return "The model file is not a .score file!";
		} catch (Exception e) {
			return "The model file is not a .score file!";
		}
		
		return null;
	}
	
	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	private boolean isScoreFile(File file) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder parser = factory.newDocumentBuilder();
		Document document = parser.parse(file.toURI().toString());
		
		Element rootElement = document.getDocumentElement();
		return "score:SContext".equals(rootElement.getNodeName());
	}
	
	//----------------------------------------------------------------------------------------------------
	public IResultFileTool getResultFileTool() {
		//TODO: create one! (just joking, for _Simphony_???)
		throw new PlatformSettings.UnsupportedPlatformException("No result file tool yet for Simphony platform.");
	}
}
