package ai.aitia.meme.paramsweep.platform.netlogo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

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
import ai.aitia.meme.paramsweep.platform.netlogo.impl.DataSourceTestRunner;
import ai.aitia.meme.paramsweep.platform.netlogo.impl.IntelliSweepNetLogoResultParser;
import ai.aitia.meme.paramsweep.platform.netlogo.impl.MEMEHeadlessWorkspace;
import ai.aitia.meme.paramsweep.platform.netlogo.impl.MEMENetLogoParser;
import ai.aitia.meme.paramsweep.platform.netlogo.impl.NetLogoBatchController;
import ai.aitia.meme.paramsweep.platform.netlogo.impl.NetLogoModelGenerator;
import ai.aitia.meme.paramsweep.platform.netlogo.impl.NetLogoModelInformation;
import ai.aitia.meme.paramsweep.platform.netlogo.impl.NetLogoParameterPartitioner;
import ai.aitia.meme.paramsweep.platform.netlogo.impl.NetLogoResultFileMerger;
import ai.aitia.meme.paramsweep.platform.netlogo.impl.NetLogoScriptParser;
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
	
	public static final String NETLOGO_41x_VERSION = "NETLOGO 4.1";
	public static final String NETLOGO_41x_3D_VERSION = "NETLOGO 3D 4.1";
	
	private String generatedModelName = null;
	
	//====================================================================================================
	// implemented interfaces

	//----------------------------------------------------------------------------------------------------
	public String getDisplayableName() { return "NetLogo 4.1.2"; }
	public PlatformType getPlatfomType() { return PlatformType.NETLOGO; }
	public String getVersion() { return "4.1.x"; }
	
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
	public IParameterSweepResultReader getReader(List<RecorderInfo> recorders) { return new IntelliSweepNetLogoResultParser(recorders); }
	public IModelInformation getModelInformation(IPSWInformationProvider provider) { return new NetLogoModelInformation(provider,SETUP_COMMAND); }

	//----------------------------------------------------------------------------------------------------
	public List<File> prepareResult(List<RecorderInfo> recorders, File workingDir) { 
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
					line = reader.readLine().trim(); // This is the version line
					if (!line.toUpperCase(Locale.US).startsWith(NETLOGO_41x_VERSION) &&
							!line.toUpperCase(Locale.US).startsWith(NETLOGO_41x_3D_VERSION))
						return "Unsupported model version: " + line;
					if (line.toUpperCase(Locale.US).startsWith(NETLOGO_41x_3D_VERSION)){
						System.setProperty("org.nlogo.is3d", "true");
					}

				}
				return null;
			} catch (final FileNotFoundException _) {
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
	public String checkCondition(String condition, IPSWInformationProvider provider) {
		String err = null;
		try {
			MEMEHeadlessWorkspace workspace = new MEMEHeadlessWorkspace();
			workspace.open(provider.getModelFile().getAbsolutePath());
			Compiler.checkReporterSyntax(condition,workspace.world.program(),workspace.getProcedures(),workspace.getExtensionManager(),true);
			workspace.dispose();
		} catch (Exception e) {
			err = Utils.getLocalizedMessage(e);
		}  
		return err;
	}
	
	//----------------------------------------------------------------------------------------------------
	public String prepareModel(IPSWInformationProvider provider) {
		NetLogoModelGenerator generator = new NetLogoModelGenerator(provider,SETUP_COMMAND);
		String error = generator.generateModel();
		if (error == null) {
			generatedModelName = generator.getGeneratedModelName();
			if (generatedModelName != null)
				provider.setGeneratedModelName(generatedModelName);
		}
		return error;
	}
	
	//----------------------------------------------------------------------------------------------------
	public List<String> checkScript(ScriptGeneratedRecordableInfo info, List<GeneratedRecordableInfo> others, IPSWInformationProvider provider) {
		final NetLogoScriptParser parser = new NetLogoScriptParser(info,others,provider,SETUP_COMMAND);
		return parser.check();
	}
	
	//----------------------------------------------------------------------------------------------------
	public List<String> checkVariable(final UserDefinedVariable variable, final IPSWInformationProvider provider) {
		final NetLogoScriptParser parser = new NetLogoScriptParser(null,null,provider,SETUP_COMMAND);
		return parser.checkVariable(variable);
	}
	
	//----------------------------------------------------------------------------------------------------
	public Pair<String,Throwable> testRun(IPSWInformationProvider provider, List<GeneratedRecordableInfo> dataSources) throws IOException {
		DataSourceTestRunner runner = new DataSourceTestRunner(provider,dataSources,SETUP_COMMAND);
		return runner.testRun();
	}

	//----------------------------------------------------------------------------------------------------
	public IResultFileTool getResultFileTool() {
		//TODO: create one!
		throw new PlatformSettings.UnsupportedPlatformException("No result file tool yet for NetLogo platform.");
	}
}