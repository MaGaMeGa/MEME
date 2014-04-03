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
package ai.aitia.meme.paramsweep.platform.simphony2;

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
import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.paramsweep.batch.IBatchController;
import ai.aitia.meme.paramsweep.batch.IModelInformation;
import ai.aitia.meme.paramsweep.batch.IParameterPartitioner;
import ai.aitia.meme.paramsweep.batch.IParameterSweepResultReader;
import ai.aitia.meme.paramsweep.batch.output.RecorderInfo;
import ai.aitia.meme.paramsweep.platform.DefaultPluginPlatform;
import ai.aitia.meme.paramsweep.platform.IPSWInformationProvider;
import ai.aitia.meme.paramsweep.platform.IResultFileTool;
import ai.aitia.meme.paramsweep.platform.PlatformManager.PlatformType;
import ai.aitia.meme.paramsweep.platform.repast.impl.ConditionParser;
import ai.aitia.meme.paramsweep.platform.repast.impl.IntelliSweepRepastResultParser;
import ai.aitia.meme.paramsweep.platform.repast.impl.RepastResultFileTool;
import ai.aitia.meme.paramsweep.platform.repast.impl.ResultFileMerger;
import ai.aitia.meme.paramsweep.platform.simphony2.impl.Simphony2BatchController;
import ai.aitia.meme.paramsweep.platform.simphony2.impl.Simphony2ModelGenerator;
import ai.aitia.meme.paramsweep.platform.simphony2.impl.Simphony2ModelInformation;
import ai.aitia.meme.paramsweep.util.DefaultParameterPartitioner;
import ai.aitia.meme.paramsweep.utils.ClassPathPair;
import ai.aitia.meme.paramsweep.utils.ParamSweepConstants;
import ai.aitia.meme.paramsweep.utils.Util;

public class Simphony2JavaPlatform extends DefaultPluginPlatform {

	//====================================================================================================
	// members
	
	private static final long serialVersionUID = -6385906203800787445L;

	public static final String simphony2ModelName = "ai.aitia.meme.paramsweep.platform.simphony2.ISimphony2Model";
	public static final String recorderPrefix = "ai.aitia.recorder.path.prefix";

	private transient CtClass modelClass = null;
	private transient ClassLoader customLoader = null;
	private String generatedModelName = null;
	private boolean local = false;

	//====================================================================================================
	// methods

	//====================================================================================================
	// implemented interfaces
	
	//----------------------------------------------------------------------------------------------------
	public PlatformType getPlatfomType() { return PlatformType.SIMPHONY2; }
	public String getDisplayableName() { return "Platform for Simphony 2 models"; }
	public String getVersion() { return "2.0"; }
	
	//----------------------------------------------------------------------------------------------------
	public String getDescription() {
//		return "This is a support platform for custom java models. It enables the user to use the capabilities of the MEME Parameter Sweep" +
//				" Wizard with any models created in Java. To achive this some minimal modifications must be done on the model.\n\n" +
//				"1. All variables of the model must have at least protected visibility.\n" +
//				"2. There has to be a nullary constructor in the model class.\n" +
//				"3. The model class must implement the\n    ai.aitia.meme.paramsweep.platform.simphony2.ISimphony2CustomModel interface.\n    However, " +
//				" the stepEnded() method may be implemented with empty body.\n" + 
//				"4. At the end of every step of the model the stepEnded() method must be\n    called.\n" +
//				"5. You must use the simulationStop() method if your simulation wants to\n    stop itself.";

		return "Repast Simphony 2.0 is a free and open source agent-based modeling toolkit that simplifies model creation and use. \n\n See " +
		   "http://repast.sourceforge.net/ for further details.";
	}
	
	//----------------------------------------------------------------------------------------------------
	public IParameterPartitioner getParameterPartitioner() { return new DefaultParameterPartitioner(); }
	public IParameterSweepResultReader getReader(List<RecorderInfo> recorders) { return new IntelliSweepRepastResultParser(recorders); }
	public IModelInformation getModelInformation(IPSWInformationProvider provider) { return new Simphony2ModelInformation(provider,modelClass); }
	public List<File> prepareResult(List<RecorderInfo> recorders, File workingDir) { return new ResultFileMerger().merge(recorders,workingDir); }
	
	//----------------------------------------------------------------------------------------------------
	public List<File> prepareResult(final List<RecorderInfo> recorders, final List<String> suffixes, final File workingDir) {
		return new ResultFileMerger().merge(recorders,suffixes,workingDir);
	}
	
	//----------------------------------------------------------------------------------------------------
	public IBatchController getBatchController() {
		if (local) {
			try {
				Class<?> bcClass = Class.forName("ai.aitia.meme.paramsweep.platform.simphony2.impl.Simphony2BatchController",true,customLoader);
				Constructor<?> constructor = bcClass.getConstructor(String.class,Boolean.TYPE);
				return (IBatchController) constructor.newInstance(generatedModelName,true);
			} catch (Throwable t) {
				throw new RuntimeException(t);
			}
		}
		return new Simphony2BatchController(generatedModelName,false);
	}
	
	//----------------------------------------------------------------------------------------------------
	public String checkModel(IPSWInformationProvider provider) {
		try {
			if (provider.getClassCache().get(simphony2ModelName) == null) {
				Class modelClass = provider.getCustomClassLoader().loadClass(simphony2ModelName);
				provider.getClassCache().put(simphony2ModelName,modelClass);
			}
		} catch (Exception e) {
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
		Simphony2ModelGenerator generator = new Simphony2ModelGenerator(provider.getClassPool(),modelClass,provider.getRecorders(),
																		  provider.getStoppingCondition(),provider.isNumberStoppingCondition(),
																		  provider.getNewParameters(),provider.getModelRoot(),
																		  provider.getDescription());
		String error = generator.generateModel();
		if (error == null) {
			if (provider.writeSource())
				generator.writeSource();
			generatedModelName = generator.getGeneratedModelName();
			provider.setGeneratedModelName(generatedModelName);
		}
		return error;
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
	 *  IGeneratedModel} interface, but implement (directly or undirectly) the ISimphony2Model
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
			List<String> intfs = Arrays.asList(clazz.getClassFile2().getInterfaces());
			if (intfs.contains(ParamSweepConstants.generatedModel) || intfs.contains(ParamSweepConstants.generatedModel2)) {
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
			Class customModelClass = provider.getClassCache().get(simphony2ModelName);
			if (customModelClass.isAssignableFrom(c)) {
				try {
					c.getConstructor();
				} catch (NoSuchMethodException e) {
					error[0] = "The model has not nullary constructor.";
					MEMEApp.logException(e);
					return null;
				}
				return clazz;
			}
			error[0] = "This model is not supported.";
		} catch (FileNotFoundException e) {
			error[0] = "Not found.";
			MEMEApp.logException(e);
		} catch (IOException e) {
			error[0] = "Loading error.";
			MEMEApp.logException(e);
		} catch (NotFoundException e) {
			// never happens
			throw new IllegalStateException(e);
		} catch (CannotCompileException e) {
			error[0] = "Required classes not found.";
			MEMEApp.logException(e);
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
		String returnPath = (index == -1) ? path : path.substring(0,index - 1);
		return returnPath;
	}
	
	//----------------------------------------------------------------------------------------------------
	public IResultFileTool getResultFileTool() {
		return new RepastResultFileTool();
	}
}
