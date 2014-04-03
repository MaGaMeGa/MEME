package ai.aitia.meme.paramsweep.platform.netlogo.impl;

import static ai.aitia.meme.paramsweep.platform.netlogo.impl.MEMENetLogoParser.BREED_KEYWORD;
import static ai.aitia.meme.paramsweep.platform.netlogo.impl.MEMENetLogoParser.CLOSE_BRACKET;
import static ai.aitia.meme.paramsweep.platform.netlogo.impl.MEMENetLogoParser.D_LINK_BREED_KEYWORD;
import static ai.aitia.meme.paramsweep.platform.netlogo.impl.MEMENetLogoParser.EXTENSIONS_KEYWORD;
import static ai.aitia.meme.paramsweep.platform.netlogo.impl.MEMENetLogoParser.GLOBALS_KEYWORD;
import static ai.aitia.meme.paramsweep.platform.netlogo.impl.MEMENetLogoParser.INCLUDES_KEYWORD;
import static ai.aitia.meme.paramsweep.platform.netlogo.impl.MEMENetLogoParser.MAGIC_STRING;
import static ai.aitia.meme.paramsweep.platform.netlogo.impl.MEMENetLogoParser.OPEN_BRACKET;
import static ai.aitia.meme.paramsweep.platform.netlogo.impl.MEMENetLogoParser.U_LINK_BREED_KEYWORD;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.Deflater;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;
import ai.aitia.meme.paramsweep.batch.output.RecordableInfo;
import ai.aitia.meme.paramsweep.batch.output.RecorderInfo;
import ai.aitia.meme.paramsweep.platform.IPSWInformationProvider;
import ai.aitia.meme.paramsweep.platform.netlogo.impl.MEMENetLogoParser.BreedInfo;
import ai.aitia.meme.paramsweep.platform.netlogo.info.NLStatisticGeneratedRecordableInfo;
import ai.aitia.meme.paramsweep.platform.repast.info.ExtendedOperatorGeneratedRecordableInfo;
import ai.aitia.meme.paramsweep.platform.repast.info.GeneratedRecordableInfo;
import ai.aitia.meme.paramsweep.platform.repast.info.OperatorGeneratedRecordableInfo;
import ai.aitia.meme.paramsweep.platform.repast.info.ScriptGeneratedRecordableInfo;
import ai.aitia.meme.paramsweep.utils.AssistantMethod;
import ai.aitia.meme.paramsweep.utils.PlatformConstants;
import ai.aitia.meme.paramsweep.utils.UserDefinedVariable;
import ai.aitia.meme.paramsweep.utils.Util;
import ai.aitia.meme.utils.Utils.Pair;

public class NetLogoModelGenerator {

	//====================================================================================================
	// members
	
	public static final String PACKAGE_NAME	= "ai.aitia.script.extension";
	public static final String MANAGER_CLASS_NAME = "ScriptExtensionManager";
	
	private static final String	SUFFIX = "_aitiaGenerated";
	
	public static final String STEP_METHOD = "aitia_generated_step_method"; 
	public static final String SD_SETUP_METHOD = "aitia_generated_system_dynamics_setup_method";
	private static final String INIT = "__init-aitia-generated-user-variables";  
	static final String modelSuffix = "BatchGenerated";
	
	private ClassPool pool = null;
	private String modelFile = null;
	private List<RecorderInfo> recorders = null;
	private String timestamp = null;
	private List<String> generatedScripts = new ArrayList<String>();
	private List<String> nonStatisticReporters = new ArrayList<String>();
	private List<CtClass> generatedClasses = new ArrayList<CtClass>();
	private String generatedModelName = null;
	private IPSWInformationProvider provider = null;
	
	private CtClass defaultReporter = null;
	private CtClass reporterInterface = null;
	private CtClass defaultClassManager = null;
	private CtClass classManagerInterface = null;
	
	private List<Pair<String,AssistantMethod>> assistantMethods = new ArrayList<Pair<String,AssistantMethod>>();
	private boolean hasTimeSeries = false;
	private final String SETUP_COMMAND;
	private MEMENetLogoParser parser = null;
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public NetLogoModelGenerator(final IPSWInformationProvider provider, final String setupCommand) {
		if (provider == null)
			throw new IllegalArgumentException("'provider' is null.");
		this.provider = provider;
		this.pool = provider.getClassPool();
		this.modelFile = provider.getModelFile().getAbsolutePath();
		this.recorders = provider.getRecorders();
		this.SETUP_COMMAND = setupCommand;
		
		try {
			this.defaultReporter = this.pool.get("org.nlogo.api.DefaultReporter");
			this.reporterInterface = this.pool.get("org.nlogo.api.Reporter");
			this.defaultClassManager = this.pool.get("org.nlogo.api.DefaultClassManager");
			this.classManagerInterface = this.pool.get("org.nlogo.api.ClassManager");
		} catch (NotFoundException e) {
			throw new IllegalStateException(e);
		}
		timestamp = Util.getTimeStamp();
	}
	
	//----------------------------------------------------------------------------------------------------
	public String generateModel() { 
		String error = null;
		try {
			for (RecorderInfo ri : recorders) {
				List<RecordableInfo> recordables = ri.getRecordables();
				for (RecordableInfo info : recordables) {
					if (info instanceof GeneratedRecordableInfo) 
						generateReporterClass((GeneratedRecordableInfo)info);
				}
			}
			boolean needExtension = generatedClasses.size() > 0;
			if (needExtension) {
				createReporterClassFiles();
				generateManagerClass();
				createExtension();
				generatedClasses.clear();
			}
			if (needExtension || nonStatisticReporters.size() > 0 || getParser().isSystemDynamicsModel()) {
				generatedScripts.clear();
				createDescendantModel(needExtension);
			}
		} catch (CannotCompileException e) {
			error = "Compile error while generating script extension.";
			if (e.getReason() != null && e.getReason().trim().length() != 0)
				error += "\nReason: " + e.getReason();
		} catch (Exception e) {
			error = "Error while generating script extension or descendant model.";
			if (e.getLocalizedMessage() != null && Util.getLocalizedMessage(e).trim().length() > 0)
				error += "\nReason: " + Util.getLocalizedMessage(e);
		}
		return error;
	}
	
	//----------------------------------------------------------------------------------------------------
	public String getGeneratedModelName() { return generatedModelName; }

	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	private void generateReporterClass(GeneratedRecordableInfo info) throws CannotCompileException {
		importPackages();
		if (info instanceof NLStatisticGeneratedRecordableInfo) {
			NLStatisticGeneratedRecordableInfo _info = (NLStatisticGeneratedRecordableInfo) info;
			generateStatisticClass(_info);
		} else if ((info instanceof ScriptGeneratedRecordableInfo) || (info instanceof OperatorGeneratedRecordableInfo)) 
			generateClassOfReferenced(info);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void generateClassOfReferenced(GeneratedRecordableInfo info) throws CannotCompileException {
		if (generatedScripts.contains(info.getAccessibleName())) return;
		List<GeneratedRecordableInfo> references = info.getReferences();
		for (GeneratedRecordableInfo gri : references) {
			if (gri instanceof NLStatisticGeneratedRecordableInfo) {
				NLStatisticGeneratedRecordableInfo _gri = (NLStatisticGeneratedRecordableInfo) gri;
				generateStatisticClass(_gri);
			} else if ((gri instanceof ScriptGeneratedRecordableInfo) || (info instanceof OperatorGeneratedRecordableInfo)) 
				generateClassOfReferenced(gri);
		}
		if (info instanceof ExtendedOperatorGeneratedRecordableInfo) {
			final ExtendedOperatorGeneratedRecordableInfo _info = (ExtendedOperatorGeneratedRecordableInfo) info;
			for (final AssistantMethod method : _info.getAssistantMethods()) 
				assistantMethods.add(new Pair<String,AssistantMethod>(_info.getAccessibleName(),method));
			hasTimeSeries |= "ai.aitia.meme.paramsweep.operatorPlugin.Operator_TimeSeries".equalsIgnoreCase(((OperatorGeneratedRecordableInfo)info).getGeneratorName());
		}
		generatedScripts.add(info.getAccessibleName());
		nonStatisticReporters.add(info.getAccessibleName());
	}
	
	//----------------------------------------------------------------------------------------------------
	private void generateStatisticClass(NLStatisticGeneratedRecordableInfo info) throws CannotCompileException {
		if (generatedScripts.contains(info.getAccessibleName())) return;
		List<GeneratedRecordableInfo> references = info.getReferences();
		for (GeneratedRecordableInfo gri : references) {
			if (gri instanceof NLStatisticGeneratedRecordableInfo) {
				NLStatisticGeneratedRecordableInfo _gri = (NLStatisticGeneratedRecordableInfo) gri;
				generateStatisticClass(_gri);
			} else if ((gri instanceof ScriptGeneratedRecordableInfo) || (gri instanceof OperatorGeneratedRecordableInfo)) 
				generateClassOfReferenced(gri);
		}
		
		CtClass clazz = pool.makeClass(PACKAGE_NAME + "." + createClassName(info.getAccessibleName()),defaultReporter);
		clazz.addInterface(reporterInterface);
		
		StringBuilder syntax = new StringBuilder("public strictfp Syntax getSyntax() {\n");
		syntax.append(info.getSyntaxBody()).append("\n");
		syntax.append("}\n");
		CtMethod syntaxMethod = CtNewMethod.make(syntax.toString(),clazz);
		clazz.addMethod(syntaxMethod);
		
		StringBuilder report = new StringBuilder("public Object report(Argument[] ");
		report.append(PlatformConstants.ARGUMENT).append(", Context context) throws ExtensionException, LogoException {\n");
		report.append(info.getReportBody()).append("\n");
		report.append("}\n");
		CtMethod reportMethod = CtNewMethod.make(report.toString(),clazz);
		clazz.addMethod(reportMethod);
		
		generatedClasses.add(clazz);
		generatedScripts.add(info.getAccessibleName());
	}

	//----------------------------------------------------------------------------------------------------
	private void importPackages() {	pool.importPackage("org.nlogo.api"); }
	
	//----------------------------------------------------------------------------------------------------
	private String createClassName(String reporterName) {
		String result = new String(reporterName);
		result = result.replaceAll("\\.","\\$D\\$");
		result = result.replaceAll("\\?","\\$Q\\$");
		result = result.replaceAll("=","\\$E\\$");
		result = result.replaceAll("\\*","\\$S\\$");
		result = result.replaceAll("!","\\$e\\$");
		result = result.replaceAll(":","\\$C\\$");
		result = result.replaceAll("#","\\$H\\$");
		result = result.replaceAll("\\+","\\$P\\$");
		result = result.replaceAll("/","\\$s\\$");
		result = result.replaceAll("<","\\$L\\$");
		result = result.replaceAll(">","\\$G\\$");
		result = result.replaceAll("%","\\$p\\$");
		result = result.replaceAll("\\^","\\$h\\$");
		result = result.replaceAll("'","\\$q\\$");
		result = result.replaceAll("\\&","\\$A\\$");
		result = result.replaceAll("-","\\$d\\$");
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	private void generateManagerClass() throws CannotCompileException {
		CtClass clazz = pool.makeClass(PACKAGE_NAME + "." + MANAGER_CLASS_NAME,defaultClassManager);
		clazz.addInterface(classManagerInterface);
		
		StringBuilder load = new StringBuilder("public void load(PrimitiveManager primitivemanager) throws ExtensionException {\n");
		for (String name : generatedScripts) {
			if (!nonStatisticReporters.contains(name)) {
				load.append("primitivemanager.addPrimitive(\"").append(name).append("\",new ").append(PACKAGE_NAME).append(".");
				load.append(createClassName(name)).append("());\n");
			}
		}
		load.append("}\n");
		CtMethod loadMethod = CtNewMethod.make(load.toString(),clazz);
		clazz.addMethod(loadMethod);
		generatedClasses.add(clazz);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void createReporterClassFiles() throws IOException, CannotCompileException, NotFoundException {
		File tempDir = new File("NetLogoTemp");
		boolean exception = true; 
		try {
			tempDir.mkdir();
			pool.appendClassPath(tempDir.getAbsolutePath());
			File packageDir = new File(tempDir,PACKAGE_NAME.replace('.','/'));
			packageDir.mkdirs();
			
			for (CtClass c : generatedClasses) {
				c.stopPruning(true);
				c.writeFile(tempDir.getAbsolutePath());
				c.defrost();
			}
			generatedClasses.clear();
			exception = false;
		} finally {
			if (exception)
				deleteDir(tempDir);
		}
		
	}
	
	//----------------------------------------------------------------------------------------------------
	private void createExtension() throws CannotCompileException, IOException {
		File tempDir = new File("NetLogoTemp");
		try {
			for (CtClass c : generatedClasses) {
				c.stopPruning(true);
				c.writeFile(tempDir.getAbsolutePath());
				c.defrost();
			}
			
			File modelDir = new File(modelFile).getParentFile();
			File extensionDir = new File(modelDir,PlatformConstants.AITIA_GENERATED_SCRIPTS);
			extensionDir.mkdir();
			
			File manifest = new File("resources/nlmanifest.mf");
			jarDirectory(tempDir,new File(extensionDir,PlatformConstants.AITIA_GENERATED_SCRIPTS + ".jar"),manifest);
			copyRequiredJars(extensionDir);
			
			List<String> resources = new ArrayList<String>();
			List<String> old = provider.getAutomaticResources();
			if (old != null)
				resources.addAll(old);
			
			String absolutePath = new File(extensionDir,"colt.jar").getAbsolutePath();
			if (!resources.contains(absolutePath))
				resources.add(absolutePath);
			absolutePath = new File(extensionDir,"meme-paramsweep.jar").getAbsolutePath();
			if (!resources.contains(absolutePath))
				resources.add(absolutePath);
			absolutePath = new File(extensionDir,PlatformConstants.AITIA_GENERATED_SCRIPTS + ".jar").getAbsolutePath();
			if (!resources.contains(absolutePath))
				resources.add(absolutePath);
			provider.setAutomaticResources(resources);
		} finally {
			deleteDir(tempDir);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void jarDirectory(File dir2zip, File destFile, File manifest) throws IOException {
		Manifest man = new Manifest(new FileInputStream(manifest));
		JarOutputStream zos = new JarOutputStream(new FileOutputStream(destFile),man);
		zos.setLevel(Deflater.BEST_COMPRESSION);
		jarDir(dir2zip,dir2zip,zos);
		zos.finish();
		zos.close();
	}
	
	//----------------------------------------------------------------------------------------------------
	private void jarDir(File original, File dir2zip, JarOutputStream zos) throws IOException {
		//get a listing of the directory content 
	    File[] dirList = dir2zip.listFiles(); 
	    byte[] readBuffer = new byte[2156]; 
	    int bytesIn = 0; 
	    //loop through dirList, and zip the files
	    int idx = original.getPath().length();
	    for (int i = 0;i < dirList.length;i++) { 
	        String substring = dirList[i].getPath().substring(idx + 1).replace('\\','/');
			if (dirList[i].isDirectory()) {
	        	JarEntry anEntry = new JarEntry(substring + "/");
	        	zos.putNextEntry(anEntry);
	        	zos.closeEntry();
	        	jarDir(original,dirList[i],zos); 
	        } else {
	        	FileInputStream fis = new FileInputStream(dirList[i]); 
	        	JarEntry anEntry = new JarEntry(substring); 
	        	zos.putNextEntry(anEntry);
	        	while ((bytesIn = fis.read(readBuffer)) != -1) 
	        		zos.write(readBuffer,0,bytesIn); 
	        	zos.closeEntry();
	        	fis.close();
	        }
	    } 
	}
	
	//----------------------------------------------------------------------------------------------------
	private void deleteDir(File dir) {
		File[] files = dir.listFiles();
		for (File f : files)
			_clean(f);
		dir.delete();
	}
	
	//----------------------------------------------------------------------------------------------------
	private void _clean(File f) {
		if (f.isDirectory()) {
			File[] files = f.listFiles();
			for (File _f : files)
				_clean(_f);
		}
		f.delete();
	}
	
	//----------------------------------------------------------------------------------------------------
	private void copyRequiredJars(File dest) throws IOException {
		File colt = new File("resources/colt.jar");
		File coltDest = new File(dest,"colt.jar");
		FileInputStream in = new FileInputStream(colt);
		FileOutputStream out = new FileOutputStream(coltDest);
		Util.copyInputStream(in,out);
		
		File mp = new File("meme-paramsweep.jar");
		File mpDest = new File(dest,"meme-paramsweep.jar");
		in = new FileInputStream(mp);
		out = new FileOutputStream(mpDest);
		Util.copyInputStream(in,out);
	}
	
	//----------------------------------------------------------------------------------------------------
	private MEMENetLogoParser getParser() throws IOException {
		if (parser == null) {
			final File file = new File(modelFile);
			FileReader reader = new FileReader(file);
			String source = new String();
			char[] buffer = new char[8096];
			int chars = 0;
			while ((chars = reader.read(buffer)) > 0) 
				source += String.valueOf(buffer,0,chars);
			try {
				reader.close();
			} catch (IOException ee) {}

			parser = new MEMENetLogoParser(source,file.getParent(),SETUP_COMMAND);
			parser.parse();
		}
		return parser;
	}
	
	//----------------------------------------------------------------------------------------------------
	private void createDescendantModel(boolean needExtension) throws IOException {
		final File file = new File(modelFile);
		String modelFileName = file.getName();
		int idx = modelFileName.lastIndexOf('.');
		String name = idx == -1 ? modelFileName : modelFileName.substring(0,idx);
		generatedModelName = name + SUFFIX + "__" + timestamp;

		File descendant = new File(file.getParentFile(),generatedModelName + ".nlogo");
		PrintWriter writer = new PrintWriter(new FileWriter(descendant));
		
		// header section
		StringBuilder line = new StringBuilder(EXTENSIONS_KEYWORD);
		line.append(" ").append(OPEN_BRACKET);
		if (needExtension)
			line.append(" ").append(PlatformConstants.AITIA_GENERATED_SCRIPTS).append(" ");
		if (hasTimeSeries)
			line.append(" table ");
		for (String extension : getParser().getExtensions())
			line.append(extension).append(" ");
		line.append(CLOSE_BRACKET);
		writer.println(line.toString());

		final List<UserDefinedVariable> userVariables = collectVariables();
		
		line = new StringBuilder(GLOBALS_KEYWORD);
		line.append(" ").append(OPEN_BRACKET).append(" ");
		if (hasTimeSeries)
			line.append(PlatformConstants.AITIA_GENERATED_VARIABLES).append(" ");
		for (String global : getParser().getGlobals())
			line.append(global).append(" ");
		for (final UserDefinedVariable variable : userVariables)
			line.append(variable.getName()).append(" ");
		line.append(CLOSE_BRACKET);
		writer.println(line.toString());
		
		line = new StringBuilder(INCLUDES_KEYWORD);
		line.append(" ").append(OPEN_BRACKET).append(" ");
		for (String include : getParser().getIncludes())
			line.append("\"").append(include).append("\" ");
		line.append(CLOSE_BRACKET);
		writer.println(line.toString());
		
		for (final BreedInfo bi : getParser().getBreeds()) {
			if (!bi.isFromIncludeFile()) {
				String _line = BREED_KEYWORD + " " + OPEN_BRACKET + " " + bi.getAgentSetName() + 
							   (bi.getAgentName() == null ? "" : " " + bi.getAgentName() + " ") + CLOSE_BRACKET;
				writer.println(_line);
			}
		}
		
		for (final BreedInfo bi : getParser().getDirectedLinkBreeds()) {
			if (!bi.isFromIncludeFile()) {
				String _line = D_LINK_BREED_KEYWORD + " " + OPEN_BRACKET + " " + bi.getAgentSetName() + " " + bi.getAgentName() + " " + CLOSE_BRACKET;
				writer.println(_line);
			}
		}

		for (final BreedInfo bi : getParser().getUndirectedLinkBreeds()) {
			if (!bi.isFromIncludeFile()) {
				String _line = U_LINK_BREED_KEYWORD + " " + OPEN_BRACKET + " " + bi.getAgentSetName() + " " + bi.getAgentName() + " " + CLOSE_BRACKET;
				writer.println(_line);
			}
		}
		
		for (String otherHeader : getParser().getOtherHeaders())
			writer.println(otherHeader);
		writer.println();
		
		if (userVariables.size() > 0)
			generateUserVariableInitialization(userVariables,writer);
		
		final StringBuilder methodPart = new StringBuilder();
		
		String actualSetup = getParser().getSetupMethod();
		if (getParser().isSystemDynamicsModel()) {
			actualSetup = actualSetup.replaceAll("(?i)\\s" + SETUP_COMMAND + "\\s"," " + SD_SETUP_METHOD + " reset-ticks ");
			actualSetup = actualSetup.replaceAll("(?i)\\sca\\s"," ");
			actualSetup = actualSetup.replaceAll("(?i)\\sclear-all\\s"," ");
			actualSetup = actualSetup.replaceAll("(?i)\\ssystem-dynamics-setup\\s"," ");
			
			// append original setup method
			methodPart.append(getParser().getSetupMethod()).append("\n\n");
		}
		
		if (hasTimeSeries || userVariables.size() > 0) 
			extendSetupMethod(actualSetup,methodPart,userVariables.size() > 0);
		else 
			methodPart.append(actualSetup).append("\n\n");
			
		// procedures section
		for (String method : getParser().getMethods()) 
			methodPart.append(method).append("\n\n");
		
		for (RecorderInfo ri : recorders) {
			List<RecordableInfo> recordables = ri.getRecordables();
			for (RecordableInfo info : recordables) {
				if (info instanceof GeneratedRecordableInfo) 
					insertReporter((GeneratedRecordableInfo)info,methodPart);
			}
		}
		
		for (int i = 0;i < assistantMethods.size();++i)
			insertAssistantMethod(assistantMethods.get(i),i,methodPart);
		
		if (assistantMethods.size() > 0)
			insertAitiaGeneratedStepMethod(methodPart);
		
		String methodsStr = methodPart.toString();
		if (assistantMethods.size() > 0)
			methodsStr = methodsStr.replaceAll("(?i)\\stick\\s", " " + STEP_METHOD + "\ntick\n"); 

		writer.println(methodsStr);
		writer.println();
		
		writer.println(MAGIC_STRING);
		writer.println();
		
		// other sections
		appendOthers(modelFile,writer);
		
		writer.flush();
		writer.close();
		
		if (hasTimeSeries && !provider.isLocalRun())
			copyTableExtension();
	}
	
	//----------------------------------------------------------------------------------------------------
	private void appendOthers(String originalFilePath, PrintWriter writer) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(originalFilePath));
		String line = null;
		boolean afterMagicLine = false;
		while ((line = reader.readLine()) != null) {
			line = line.trim();
			if (!afterMagicLine) {
				if (MAGIC_STRING.equals(line))
					afterMagicLine = true;
			} else 
				writer.println(line);
			
		}
		reader.close();
	}
	
	//----------------------------------------------------------------------------------------------------
	private void insertReporter(final GeneratedRecordableInfo info, final StringBuilder methods) {
		if (info instanceof NLStatisticGeneratedRecordableInfo) {
			NLStatisticGeneratedRecordableInfo _info = (NLStatisticGeneratedRecordableInfo) info;
			insertStatistic(_info,methods);
		} else if ((info instanceof ScriptGeneratedRecordableInfo) || (info instanceof OperatorGeneratedRecordableInfo)) 
			insertScriptOrOperator(info,methods);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void insertStatistic(final NLStatisticGeneratedRecordableInfo info, final StringBuilder methods) {
		if (generatedScripts.contains(info.getAccessibleName())) return;
		List<GeneratedRecordableInfo> references = info.getReferences();
		for (GeneratedRecordableInfo gri : references) {
			if (gri instanceof NLStatisticGeneratedRecordableInfo) {
				NLStatisticGeneratedRecordableInfo _gri = (NLStatisticGeneratedRecordableInfo) gri;
				insertStatistic(_gri,methods);
			} else if ((gri instanceof ScriptGeneratedRecordableInfo) || (gri instanceof OperatorGeneratedRecordableInfo)) 
				insertScriptOrOperator(gri,methods);
		}
		methods.append(info.getSource()).append("\n");
		generatedScripts.add(info.getAccessibleName());
	}
	
	//----------------------------------------------------------------------------------------------------
	private void insertScriptOrOperator(final GeneratedRecordableInfo info, final StringBuilder methods) {
		if (generatedScripts.contains(info.getAccessibleName())) return;
		List<GeneratedRecordableInfo> references = info.getReferences();
		for (GeneratedRecordableInfo gri : references) {
			if (gri instanceof NLStatisticGeneratedRecordableInfo) {
				NLStatisticGeneratedRecordableInfo _gri = (NLStatisticGeneratedRecordableInfo) gri;
				insertStatistic(_gri,methods);
			} else if ((gri instanceof ScriptGeneratedRecordableInfo) || (gri instanceof OperatorGeneratedRecordableInfo)) 
				insertScriptOrOperator(gri,methods);
		}
		String source = "to-report " + info.getAccessibleName() + "\n" + info.getSource() + "\nend\n";
		methods.append(source).append("\n");
		generatedScripts.add(info.getAccessibleName());
	}
	
	//----------------------------------------------------------------------------------------------------
	private void insertAssistantMethod(final Pair<String,AssistantMethod> method, final int idx, final StringBuilder methods) {
		final StringBuilder sb = new StringBuilder("to ");
		if (!method.getSecond().returnValue.equals(Void.TYPE))
			sb.append("-report ");
		String _name = method.getFirst();
		if (_name.endsWith("()")) 
			_name = _name.substring(0,_name.length() - 2);
		final String fullName = _name + PlatformConstants.AITIA_GENERATED_INFIX + idx;
		sb.append(fullName).append("\n");
		sb.append(method.getSecond().body).append("\n");
		sb.append("end\n");
		methods.append(sb.toString()).append("\n");
	}
	
	//----------------------------------------------------------------------------------------------------
	private void extendSetupMethod(final String origSetup, final StringBuilder methods, final boolean userVariables) {
		String newLine = "";
		if (hasTimeSeries)
			newLine += " set " + PlatformConstants.AITIA_GENERATED_VARIABLES + " table:make\n";
		if (userVariables)
			newLine += " " + INIT + "\n";
		final String setup = origSetup.replaceAll("(?i)\\send",newLine + "end\n");
		methods.append(setup).append("\n\n");
	}
	
	//----------------------------------------------------------------------------------------------------
	private void insertAitiaGeneratedStepMethod(final StringBuilder methods) {
		final StringBuilder sb = new StringBuilder("to ");
		sb.append(STEP_METHOD).append("\n");
		for (int i = 0;i < assistantMethods.size();++i) {
			final Pair<String,AssistantMethod> method = assistantMethods.get(i);
			String _name = method.getFirst();
			if (_name.endsWith("()")) 
				_name = _name.substring(0,_name.length() - 2);
			final String fullName = _name + PlatformConstants.AITIA_GENERATED_INFIX + i;
			switch (method.getSecond().scheduleTime) {
			case NEVER : continue;
			case TICK  : sb.append(fullName).append("\n");
						 break;
			case RUN   : sb.append(createStopIfStatement(fullName));
						 break;
			default	   : assert false;
			}
		}
		sb.append("\nend\n");
		methods.append(sb.toString()).append("\n\n");
	}
	
	//----------------------------------------------------------------------------------------------------
	private String createStopIfStatement(final String methodName) {
		final StringBuilder sb = new StringBuilder();
		sb.append("if ").append(exitCondition()).append(" [\n");
		sb.append(methodName).append("\n");
		sb.append("]\n");
		return sb.toString();
	}
	
	//----------------------------------------------------------------------------------------------------
	private String exitCondition() {
		final String condition = provider.getStoppingCondition().trim();
		if (condition.startsWith("{") && condition.endsWith("}")) {
			String result = condition.substring(1,condition.length() - 1);
			return result.trim();
		} else {
			return "ticks >= " + condition;   
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void copyTableExtension() throws IOException {
		final File modelDir = new File(modelFile).getParentFile();
		final File extensionDir = new File(modelDir,"table");
		extensionDir.mkdir();
		
		final File table = new File("resources/table.jar");
		final File tableDest = new File(extensionDir,"table.jar");
		final FileInputStream in = new FileInputStream(table);
		final FileOutputStream out = new FileOutputStream(tableDest);
		Util.copyInputStream(in,out);
		
		final List<String> resources = new ArrayList<String>();
		final List<String> old = provider.getAutomaticResources();
		if (old != null)
			resources.addAll(old);
		
		final String absolutePath = new File(extensionDir,"table.jar").getAbsolutePath();
		if (!resources.contains(absolutePath))
			resources.add(absolutePath);
		provider.setAutomaticResources(resources);
	}
	
	//----------------------------------------------------------------------------------------------------
	private List<UserDefinedVariable> collectVariables() {
		final List<UserDefinedVariable> result = new ArrayList<UserDefinedVariable>();
		for (final RecorderInfo ri : recorders) {
			final List<RecordableInfo> recordables = ri.getRecordables();
			for (final RecordableInfo info : recordables) {
				if (info instanceof GeneratedRecordableInfo) 
					Util.addAllDistinct(result,collectVariables((GeneratedRecordableInfo)info));
			}
		}
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	private List<UserDefinedVariable> collectVariables(final GeneratedRecordableInfo gmi) {
		final List<UserDefinedVariable> result = new ArrayList<UserDefinedVariable>();
		final List<GeneratedRecordableInfo> references = gmi.getReferences();
		for (final GeneratedRecordableInfo info : references) 
			Util.addAllDistinct(result,collectVariables(info));
		if (gmi instanceof ScriptGeneratedRecordableInfo) {
			ScriptGeneratedRecordableInfo sgmi = (ScriptGeneratedRecordableInfo) gmi;
			Util.addAllDistinct(result,sgmi.getUserVariables());
		}
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	private void generateUserVariableInitialization(final List<UserDefinedVariable> variables, final PrintWriter writer) {
		final StringBuilder code = new StringBuilder();
		code.append("to ").append(INIT).append(" \n");
		for (final UserDefinedVariable variable : variables) 
			code.append(variable.getInitializationCode()).append("\n");
		code.append("end\n");
		writer.println(code.toString());
	}
}