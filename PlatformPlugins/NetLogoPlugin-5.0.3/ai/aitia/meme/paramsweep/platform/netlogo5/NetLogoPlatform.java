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
package ai.aitia.meme.paramsweep.platform.netlogo5;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

import org.nlogo.api.LogoException;
import org.nlogo.compiler.Compiler;

import ai.aitia.meme.paramsweep.batch.IBatchController;
import ai.aitia.meme.paramsweep.batch.IModelInformation;
import ai.aitia.meme.paramsweep.batch.IParameterPartitioner;
import ai.aitia.meme.paramsweep.batch.IParameterSweepResultReader;
import ai.aitia.meme.paramsweep.batch.output.RecorderInfo;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings;
import ai.aitia.meme.paramsweep.platform.DefaultPluginPlatform;
import ai.aitia.meme.paramsweep.platform.IPSWInformationProvider;
import ai.aitia.meme.paramsweep.platform.IResultFileTool;
import ai.aitia.meme.paramsweep.platform.IScriptChecker;
import ai.aitia.meme.paramsweep.platform.ITestRunner;
import ai.aitia.meme.paramsweep.platform.PlatformManager.PlatformType;
import ai.aitia.meme.paramsweep.platform.netlogo5.impl.DataSourceTestRunner;
import ai.aitia.meme.paramsweep.platform.netlogo5.impl.IntelliSweepNetLogoResultParser;
import ai.aitia.meme.paramsweep.platform.netlogo5.impl.MEMEHeadlessWorkspace;
import ai.aitia.meme.paramsweep.platform.netlogo5.impl.MEMENetLogoParser;
import ai.aitia.meme.paramsweep.platform.netlogo5.impl.NetLogoBatchController;
import ai.aitia.meme.paramsweep.platform.netlogo5.impl.NetLogoParameterPartitioner;
import ai.aitia.meme.paramsweep.platform.netlogo5.impl.NetLogoResultFileMerger;
import ai.aitia.meme.paramsweep.platform.netlogo5.impl.NetLogoScriptParser;
import ai.aitia.meme.paramsweep.platform.repast.info.GeneratedRecordableInfo;
import ai.aitia.meme.paramsweep.platform.repast.info.ScriptGeneratedRecordableInfo;
import ai.aitia.meme.paramsweep.utils.UserDefinedVariable;
import ai.aitia.meme.utils.Utils;
import ai.aitia.meme.utils.Utils.Pair;

public class NetLogoPlatform extends DefaultPluginPlatform implements IScriptChecker,
																	  ITestRunner {
	
	//====================================================================================================
	// members
	
	private static final long serialVersionUID = 1515516992349421619L;
	private static final String SETUP_COMMAND	= "setup";
	private static final String GO_COMMAND		= "go";		
	
	public static final String NETLOGO_50X_VERSION = "NETLOGO 5.0";
	public static final String NETLOGO_50X_3D_VERSION = "NETLOGO 3D 5.0";
	
	private String generatedModelName = null;
	
	//====================================================================================================
	// implemented interfaces

	//----------------------------------------------------------------------------------------------------
	public String getDisplayableName() { return "NetLogo 5.0.3"; }
	public PlatformType getPlatfomType() { return PlatformType.NETLOGO5; }
	public String getVersion() { return "5.0.x"; }
	
	//----------------------------------------------------------------------------------------------------
	public String getDescription() {
		return "NetLogo is a programmable modeling environment for simulating natural and social phenomena. It was authored by Uri Wilensky in " +
			   "1999 and is in continuous development at the Center for Connected Learning and Computer-Based Modeling.\n\nNetLogo is particularly" +
			   " well suited for modeling complex systems developing over time. Modelers can give instructions to hundreds or thousands of " +
			   "\"agents\" all operating independently. This makes it possible to explore the connection between the micro-level behavior of" +
			   " individuals and the macro-level patterns that emerge from the interaction of many individuals.";
	}
	
	//----------------------------------------------------------------------------------------------------
	public IParameterPartitioner getParameterPartitioner() { return new NetLogoParameterPartitioner(); }
	public IBatchController getBatchController() { return new NetLogoBatchController(SETUP_COMMAND,GO_COMMAND,generatedModelName); }
	public IParameterSweepResultReader getReader(final List<RecorderInfo> recorders) { return new IntelliSweepNetLogoResultParser(recorders); }
//	public IModelInformation getModelInformation(final IPSWInformationProvider provider) { return new NetLogoModelInformation(provider,SETUP_COMMAND); }
	public IModelInformation getModelInformation(final IPSWInformationProvider provider) { 
		try {
			@SuppressWarnings("unchecked")
			Class<IModelInformation> modelInformationClass = (Class<IModelInformation>) Class.forName("ai.aitia.meme.paramsweep.platform.netlogo5.impl.NetLogoModelInformation", true, provider.getCustomClassLoader());
			Constructor<IModelInformation> constructor = modelInformationClass.getConstructor(IPSWInformationProvider.class, String.class);
			return constructor.newInstance(provider, SETUP_COMMAND);
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException(e);
		} catch (SecurityException e) {
			throw new IllegalStateException(e);
		} catch (NoSuchMethodException e) {
			throw new IllegalStateException(e);
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException(e);
		} catch (InstantiationException e) {
			throw new IllegalStateException(e);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		} catch (InvocationTargetException e) {
			throw new IllegalStateException(e);
		}
	}

	//----------------------------------------------------------------------------------------------------
	public List<File> prepareResult(final List<RecorderInfo> recorders, final File workingDir) { 
		return new NetLogoResultFileMerger(false).merge(recorders,workingDir);
	}
	
	//----------------------------------------------------------------------------------------------------
	public List<File> prepareResult(final List<RecorderInfo> recorders, final List<String> suffixes, final File workingDir) {
		return new NetLogoResultFileMerger(false).merge(recorders,suffixes,workingDir);
	}

	//----------------------------------------------------------------------------------------------------
	public String checkModel(final IPSWInformationProvider provider) {
		final File modelFile = provider.getModelFile();
		if (modelFile.exists()) {
			BufferedReader reader = null; 
			try {
				reader = new BufferedReader(new FileReader(modelFile));
				int magicStringCount = 0;
				String line = null;
				while ((line = reader.readLine()) != null) {
					if (MEMENetLogoParser.MAGIC_STRING.equals(line.trim())) 
						magicStringCount++;
					if (4 == magicStringCount) break;
				}
				if (magicStringCount == 4) {
					line = reader.readLine(); // This is the version line
					if (line != null)
						line = line.trim();
					else 
						return "Missing model version. Invalid file.";
					if (!line.toUpperCase(Locale.US).startsWith(NETLOGO_50X_VERSION) &&
							!line.toUpperCase(Locale.US).startsWith(NETLOGO_50X_3D_VERSION))
						return "Unsupported model version: " + line;
					if (line.toUpperCase(Locale.US).startsWith(NETLOGO_50X_3D_VERSION)){
						System.setProperty("org.nlogo.is3d", "true");
					}
				}
				return null;
			} catch (final FileNotFoundException _) {
				// never happens
			} catch (final IOException _) {
				// model could be right => permission granted
			} finally {
				try { if (reader != null) reader.close(); } catch (final IOException _) {}
			}
			return null;
		} else 
			return "Model file is not exist.";
	}

	
	//----------------------------------------------------------------------------------------------------
	public String checkCondition(final String condition, final IPSWInformationProvider provider) {
		String err = null;
		try {
			final MEMEHeadlessWorkspace workspace = MEMEHeadlessWorkspace.newWorkspace();
			workspace.open(provider.getModelFile().getAbsolutePath());
			Compiler.checkReporterSyntax(condition,workspace.world.program(),workspace.getProcedures(),workspace.getExtensionManager(),true);
			workspace.dispose();
		} catch (final Exception e) {
			err = Utils.getLocalizedMessage(e);
		}  
		return err;
	}
	
	//----------------------------------------------------------------------------------------------------
	public String prepareModel(final IPSWInformationProvider provider) {
//		final NetLogoModelGenerator generator = new NetLogoModelGenerator(provider,SETUP_COMMAND);
//		final String error = generator.generateModel();
//		
//		if (error == null){
//			generatedModelName = generator.getGeneratedModelName();
//			if (generatedModelName != null){
//				provider.setGeneratedModelName(generatedModelName);
//			}
//		}
//		
//		return error;
//		
		try {
			Class<?> modelGeneratorClass = Class.forName("ai.aitia.meme.paramsweep.platform.netlogo5.impl.NetLogoModelGenerator", true, provider.getCustomClassLoader());
			Constructor<?> constructor = modelGeneratorClass.getConstructor(IPSWInformationProvider.class, String.class);
			Object modelGenerator = constructor.newInstance(provider, SETUP_COMMAND);
			
			Method generateMethod = modelGeneratorClass.getMethod("generateModel");
			Method getModelNameMethod = modelGeneratorClass.getMethod("getGeneratedModelName");
			
			final String error = (String) generateMethod.invoke(modelGenerator);
			if (error == null) {
				generatedModelName = (String) getModelNameMethod.invoke(modelGenerator);
				if (generatedModelName != null)
					provider.setGeneratedModelName(generatedModelName);
			}
			return error;
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException(e);
		} catch (SecurityException e) {
			throw new IllegalStateException(e);
		} catch (NoSuchMethodException e) {
			throw new IllegalStateException(e);
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException(e);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		} catch (InvocationTargetException e) {
			throw new IllegalStateException(e);
		} catch (InstantiationException e) {
			throw new IllegalStateException(e);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public List<String> checkScript(final ScriptGeneratedRecordableInfo info, final List<GeneratedRecordableInfo> others, final IPSWInformationProvider provider) {
		final NetLogoScriptParser parser = new NetLogoScriptParser(info,others,provider,SETUP_COMMAND);
		return parser.check();
	}
	
	//----------------------------------------------------------------------------------------------------
	public List<String> checkVariable(final UserDefinedVariable variable, final IPSWInformationProvider provider) {
		final NetLogoScriptParser parser = new NetLogoScriptParser(null,null,provider,SETUP_COMMAND);
		return parser.checkVariable(variable);
	}
	
	//----------------------------------------------------------------------------------------------------
	public Pair<String,Throwable> testRun(final IPSWInformationProvider provider, final List<GeneratedRecordableInfo> dataSources) throws IOException {
		final DataSourceTestRunner runner = new DataSourceTestRunner(provider,dataSources,SETUP_COMMAND);
		try {
			return runner.testRun();
		} catch (LogoException e) {
			return new Pair<String, Throwable>(e.getMessage(), e);
		}
	}

	//----------------------------------------------------------------------------------------------------
	public IResultFileTool getResultFileTool() {
		//TODO: create one!
		throw new PlatformSettings.UnsupportedPlatformException("No result file tool yet for NetLogo platform.");
	}
}
