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
package ai.aitia.meme.paramsweep.platform.repast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;

import javassist.CannotCompileException;
import javassist.ClassPath;
import javassist.CtClass;
import javassist.Modifier;
import javassist.NotFoundException;
import ai.aitia.meme.paramsweep.batch.IBatchController;
import ai.aitia.meme.paramsweep.batch.IModelInformation;
import ai.aitia.meme.paramsweep.batch.IParameterPartitioner;
import ai.aitia.meme.paramsweep.batch.IParameterSweepResultReader;
import ai.aitia.meme.paramsweep.batch.output.RecorderInfo;
import ai.aitia.meme.paramsweep.platform.IPSWInformationProvider;
import ai.aitia.meme.paramsweep.platform.IResultFileTool;
import ai.aitia.meme.paramsweep.platform.Platform;
import ai.aitia.meme.paramsweep.platform.repast.impl.ConditionParser;
import ai.aitia.meme.paramsweep.platform.repast.impl.IntelliSweepRepastResultParser;
import ai.aitia.meme.paramsweep.platform.repast.impl.ModelGenerator;
import ai.aitia.meme.paramsweep.platform.repast.impl.RepastBatchController;
import ai.aitia.meme.paramsweep.platform.repast.impl.RepastModelInformation;
import ai.aitia.meme.paramsweep.platform.repast.impl.RepastResultFileTool;
import ai.aitia.meme.paramsweep.platform.repast.impl.ResultFileMerger;
import ai.aitia.meme.paramsweep.util.DefaultParameterPartitioner;
import ai.aitia.meme.paramsweep.utils.ClassPathPair;
import ai.aitia.meme.paramsweep.utils.ParamSweepConstants;
import ai.aitia.meme.paramsweep.utils.Util;

public class RepastPlatform implements Platform {
	
	//====================================================================================================
	// members
	
	private static final long serialVersionUID = 1441831948982961821L;
	public static final String recorderPrefix = "ai.aitia.recorder.path.prefix";

	static final private Platform REPAST = new RepastPlatform();
	
	private transient CtClass modelClass = null;
	private String generatedModelName = null;
	private transient ClassLoader customLoader = null;
	private boolean local = false;
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	private RepastPlatform() {}
	
	//----------------------------------------------------------------------------------------------------
	public static Platform getPlatform() { return REPAST; }
	
	//====================================================================================================
	// implemented interfaces
	
	//----------------------------------------------------------------------------------------------------
	public String getDisplayableName() { return "RepastJ 3.1"; }
	public String getVersion() { return "3.1"; }
	
	//----------------------------------------------------------------------------------------------------
	public String getDescription(){
		return "The Recursive Porous Agent Simulation Toolkit (Repast) is" +
				" a free open source toolkit. \n\nRepast seeks to support the development " +
				"of extremely flexible models of living social agents, but is not " +
				"limited to modeling living social entities alone. At its heart, " +
				"Repast toolkit version " +
				"3 can be thought of as a specification for agent-based modeling " +
				"services or functions. \n\n See http://repast.sourceforge.net/repast_3 for " +
				"further details.";
	}
	
	public IParameterPartitioner getParameterPartitioner() { return new DefaultParameterPartitioner(); }
	public IParameterSweepResultReader getReader(List<RecorderInfo> recorders) { return new IntelliSweepRepastResultParser(recorders, local); }
	public IModelInformation getModelInformation(IPSWInformationProvider provider) { return new RepastModelInformation(provider,modelClass); }
	public List<File> prepareResult(List<RecorderInfo> recorders, File workingDir) { return new ResultFileMerger().merge(recorders, workingDir); }
	
	//----------------------------------------------------------------------------------------------------
	public List<File> prepareResult(final List<RecorderInfo> recorders, final List<String> suffixes, final File workingDir) {
		return new ResultFileMerger().merge(recorders,suffixes,workingDir);
	}
	
	//----------------------------------------------------------------------------------------------------
	public IBatchController getBatchController() {
		if (local) {
			try {
				Class<?> bcClass = Class.forName("ai.aitia.meme.paramsweep.platform.repast.impl.RepastBatchController",true,customLoader);
				Constructor<?> constructor = bcClass.getConstructor(String.class,Boolean.TYPE);
				return (IBatchController) constructor.newInstance(generatedModelName,true);
			} catch (Throwable t) {
				throw new RuntimeException(t);
			}
		}
		return new RepastBatchController(generatedModelName,false);
	}
	
	//----------------------------------------------------------------------------------------------------
	public String checkModel(IPSWInformationProvider provider) {
		try {
			if (provider.getClassCache().get(ParamSweepConstants.simModelName) == null) {
				Class simModelClass = provider.getCustomClassLoader().loadClass(ParamSweepConstants.simModelName);
				provider.getClassCache().put(ParamSweepConstants.simModelName,simModelClass);
			}
		} catch (Exception e) {
			// missing repast.jar
			throw new IllegalStateException(e);
		}
		customLoader = provider.getCustomClassLoader();
		modelClass = null;
		local = false;
		generatedModelName = null;
		File modelFile = provider.getModelFile();
		if (modelFile == null)
			return "Missing model file.";
		String[] errorCode = new String[1];
		modelClass = checkModelFile(provider,modelFile,errorCode);
		return modelClass == null ? errorCode[0] : null;
	}
	
	//----------------------------------------------------------------------------------------------------
	public String prepareModel(IPSWInformationProvider provider) {
		local = provider.isLocalRun();
		boolean needModelGeneration = (provider.getNewParameters()!= null && provider.getNewParameters().size() > 0) ||
									   provider.getRecorders().size() > 0;
		if (needModelGeneration) {
			ModelGenerator generator = new ModelGenerator(provider.getClassPool(),modelClass,provider.getRecorders(),provider.getStoppingCondition(),
														  provider.isNumberStoppingCondition(),provider.getNewParameters(),provider.getModelRoot(),
														  provider.getDescription());
			String error = generator.generateModel();
			if (error == null) {
				if (provider.writeSource())
					generator.writeSource();
				generatedModelName = generator.getGeneratedModelName();
				provider.setGeneratedModelName(generatedModelName);
			}
			return error;
		} else {
			// no generation, but need generatedModelName for starting simulation
			try {
				generatedModelName = createModelClone(provider);
				provider.setGeneratedModelName(generatedModelName);
			} catch (IOException e) {
				return "Error while prepairing the model for the simulation.\nSee error log for details.";
			}
			return null;
		}
	}

	//----------------------------------------------------------------------------------------------------
	public String checkCondition(String condition, IPSWInformationProvider provider) {
		ConditionParser parser = new ConditionParser(provider.getClassPool(),modelClass,condition);
		if (parser.isValid()) return null;
		return parser.getMessage();
	}
	
	//====================================================================================================
	// assistant methods
	
	//-------------------------------------------------------------------------------
	/** Checks whether the file contains a valid model or not. Valid models are not
	 *  abstract, not implement {@link ai.aitia.meme.paramsweep.platform.repast.impl.IGeneratedModel 
	 *  IGeneratedModel} interface, but implement (directly or undirectly) the SimModel
	 *  interface.
	 */
	@SuppressWarnings("unchecked")
	private CtClass checkModelFile(IPSWInformationProvider provider, File f, String[] error) {
		CtClass clazz = null;
		try {
			InputStream ins = new FileInputStream(f);
			clazz = provider.getClassPool().makeClass(ins);
			Util.all2Public(clazz);
			ins.close();
			if (f.getParent() != null) {
				String pathName = findPath(clazz,f.getParent());
				ClassPath cp = provider.getClassPool().insertClassPath(pathName);
				ClassPathPair pair = new ClassPathPair(pathName, cp);
				if (!provider.getClassPathListModel().contains(pair))
					provider.getClassPathListModel().add(0,pair);
			}			
			if (Modifier.isInterface(clazz.getModifiers())) {
				error[0] = "This is an interface.";
				return null;
			}
			if (Modifier.isAbstract(clazz.getModifiers())) {
				error[0] = "This is an abstract class.";
				return null;
			}
			if (Arrays.asList(clazz.getClassFile2().getInterfaces()).contains(ParamSweepConstants.generatedModel)) {
				error[0] = "This is generated by this wizard.";
				return null;
			}
			Class c = null;
			if ((c = provider.getClassCache().get(clazz.getName())) == null) {
				clazz.stopPruning(true);
				try {
					c = clazz.toClass(provider.getCustomClassLoader(),null);
				} finally {
					clazz.defrost();
				}
				provider.getClassCache().put(clazz.getName(),c);
			}
			Class simModelClass = provider.getClassCache().get(ParamSweepConstants.simModelName);
			if (simModelClass.isAssignableFrom(c)) {
				try {
					c.getConstructor();
				} catch (NoSuchMethodException e) {
					error[0] = "The model has not nullary constructor.";
					return null;
				}
				return clazz;
			}
			error[0] = "This does not seem to be a Repast model.";
		} catch (FileNotFoundException e) {
			error[0] = "Not found.";
		} catch (IOException e) {
			error[0] = "Loading error.";
		} catch (NotFoundException e) {
			// never happens
			throw new IllegalStateException(e);
		} catch (CannotCompileException e) {
			error[0] = "Required classes not found.";
		}
		return null;
	}
	
	//-------------------------------------------------------------------------------
	/** This methods finds the first component of the parameter <code>path</code> that
	 *  not contains the package name of the class specified by <code>clazz</code>.
	 * @param clazz class
	 * @param path a file path
	 * @return the first (see above) component of <code>path</code>.
	 */
	private String findPath(CtClass clazz, String path) {
		String packages = clazz.getName();
		return findPath(packages,path);
	}
	
	//-------------------------------------------------------------------------------
	/** This methods finds the first component of the parameter <code>path</code> that
	 *  not contains the package name of the class specified by <code>className</code>.
	 * @param className the fully qualified name of a class
	 * @param path a file path
	 * @return the first (see above) component of <code>path</code>.
	 */
	private String findPath(String className, String path) {
		int index = className.lastIndexOf('.');
		if (index != -1) 
			className = className.substring(0,index);
		className = className.replace('.',File.separatorChar);
		index = path.lastIndexOf(className);
		String returnPath = (index == -1) ? path : path.substring(0,index-1);
		return returnPath;
	}
	
	//----------------------------------------------------------------------------------------------------
	/** Creates a clone of class file represented by <code>modelClass</code>. The clone
	 *  class is derived from the original, but its name is unique (because it contains
	 *  a timestamp).
	 * @throws IOException if any problem occurs during the writing of the class file
	 */
	private String createModelClone(IPSWInformationProvider provider) throws IOException { 
		String timestamp = Util.getTimeStamp();
		String dir = findPath(modelClass,provider.getEntryPoint());
		String cloneName = modelClass.getName() + "__" + timestamp;
		CtClass clone = null;
		try {
			clone = provider.getClassPool().makeClass(cloneName,modelClass);
			clone.stopPruning(true);
			clone.writeFile(dir);
			return cloneName;
		} catch (CannotCompileException e) {
			// never happens in normal state because we change only the name of the class
			throw new IllegalStateException(e);
		} finally {
			if (clone != null)
				clone.defrost();
		}
	}

	//----------------------------------------------------------------------------------------------------
	public IResultFileTool getResultFileTool() {
		return new RepastResultFileTool();
	}
}
