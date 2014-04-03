package ai.aitia.meme.paramsweep.platform.netlogo.impl;

import static ai.aitia.meme.paramsweep.platform.netlogo.impl.MEMENetLogoParser.BREED_KEYWORD;
import static ai.aitia.meme.paramsweep.platform.netlogo.impl.MEMENetLogoParser.CLOSE_BRACKET;
import static ai.aitia.meme.paramsweep.platform.netlogo.impl.MEMENetLogoParser.D_LINK_BREED_KEYWORD;
import static ai.aitia.meme.paramsweep.platform.netlogo.impl.MEMENetLogoParser.END_KEYWORD;
import static ai.aitia.meme.paramsweep.platform.netlogo.impl.MEMENetLogoParser.EXTENSIONS_KEYWORD;
import static ai.aitia.meme.paramsweep.platform.netlogo.impl.MEMENetLogoParser.GLOBALS_KEYWORD;
import static ai.aitia.meme.paramsweep.platform.netlogo.impl.MEMENetLogoParser.INCLUDES_KEYWORD;
import static ai.aitia.meme.paramsweep.platform.netlogo.impl.MEMENetLogoParser.MAGIC_STRING;
import static ai.aitia.meme.paramsweep.platform.netlogo.impl.MEMENetLogoParser.OPEN_BRACKET;
import static ai.aitia.meme.paramsweep.platform.netlogo.impl.MEMENetLogoParser.TO_KEYWORD;
import static ai.aitia.meme.paramsweep.platform.netlogo.impl.MEMENetLogoParser.U_LINK_BREED_KEYWORD;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nlogo.headless.HeadlessWorkspace;
import org.nlogo.nvm.LabInterface.ProgressListener;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.NotFoundException;
import _.agentset;
import _.link;
import _.patch;
import _.turtle;
import _.unknown;
import ai.aitia.meme.paramsweep.batch.IModelInformation;
import ai.aitia.meme.paramsweep.batch.output.BreedNonRecordableInfo;
import ai.aitia.meme.paramsweep.batch.output.NonRecordableFunctionInfo;
import ai.aitia.meme.paramsweep.batch.output.NonRecordableInfo;
import ai.aitia.meme.paramsweep.batch.output.RecordableInfo;
import ai.aitia.meme.paramsweep.batch.output.RecorderInfo;
import ai.aitia.meme.paramsweep.batch.param.ParameterInfo;
import ai.aitia.meme.paramsweep.platform.IPSWInformationProvider;
import ai.aitia.meme.paramsweep.platform.PlatformManager;
import ai.aitia.meme.paramsweep.platform.PlatformManager.PlatformType;
import ai.aitia.meme.paramsweep.platform.netlogo.impl.MEMENetLogoParser.BreedInfo;
import ai.aitia.meme.paramsweep.platform.netlogo.impl.MEMENetLogoParser.BreedType;
import ai.aitia.meme.paramsweep.platform.netlogo.impl.MEMENetLogoParser.InterfaceParameter;
import ai.aitia.meme.paramsweep.utils.Util;
import ai.aitia.meme.utils.Utils.Pair;

public class NetLogoModelInformation implements IModelInformation {

	//====================================================================================================
	// members
	
	// constants
	private static final String		SUFFIX = "_aitiaGenerated";
	private static final String 	TYPECHECKER = "typechecker";
	
	private static final String xmlSource = "<experiments>" + 
											"<experiment name=\"experiment\" repetitions=\"1\" runMetricsEveryStep=\"false\">" +
											"<setup>setup" + SUFFIX + "</setup>" +
											"<go>go" + SUFFIX + "</go>" +
											"<timeLimit steps=\"1\"/>" +
											"</experiment>" +
											"</experiments>";
	
	private static Map<String,Class<?>> builtInRecordables = new HashMap<String,Class<?>>();
	static {
		builtInRecordables.put("min-pxcor",Double.class);
		builtInRecordables.put("max-pxcor",Double.class);
		builtInRecordables.put("min-pycor",Double.class);
		builtInRecordables.put("max-pycor",Double.class);
		builtInRecordables.put("world-width",Double.class);
		builtInRecordables.put("world-height",Double.class);
	}
	
	private static Map<BreedType,CtClass> breedAncestors = null;
	
	private IPSWInformationProvider provider = null;
	private String setupCommand = null;
	private Throwable error = null;
	
	private List<ParameterInfo<?>> parameters = null;
	private List<RecordableInfo> recordables = null;
	private List<NonRecordableInfo> nonRecordables = null;

	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public NetLogoModelInformation(IPSWInformationProvider provider, String setupCommand) {
		this.provider = provider;
		this.setupCommand = setupCommand;
		analyzeModel();
	}

	//====================================================================================================
	// implemented interfaces

	//----------------------------------------------------------------------------------------------------
	public String getName() {
		File f = new File(provider.getEntryPoint());
		int idx = f.getName().lastIndexOf('.');
		return f.getName().substring(0,idx);
	}
		
	//----------------------------------------------------------------------------------------------------
	public List<NonRecordableInfo> getNonRecordables() throws ModelInformationException {
		if (error != null)
			throw new ModelInformationException(error);
		return nonRecordables;
	}
	
	//----------------------------------------------------------------------------------------------------
	public List<ParameterInfo<?>> getParameters() throws ModelInformationException {
		if (error != null)
			throw new ModelInformationException(error);
		return parameters;
	}
	
	//----------------------------------------------------------------------------------------------------
	public List<RecordableInfo> getRecordables() throws ModelInformationException {
		if (error != null)
			throw new ModelInformationException(error);
		return recordables;
	}
	
	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	private void analyzeModel() {
		parameters = new ArrayList<ParameterInfo<?>>();
		recordables = new ArrayList<RecordableInfo>();
		nonRecordables = new ArrayList<NonRecordableInfo>();
		
		try {
			FileReader reader = new FileReader(provider.getModelFile());
			
			String fileName = provider.getModelFile().getName();
			int idx = fileName.lastIndexOf('.');
			String name = idx == -1 ? fileName : fileName.substring(0,idx);
			String generatedFileName = name + SUFFIX + "__" + Util.getTimeStamp() + ".nlogo";
			
			if (provider.rngSeedAsParameter())
				parameters.add(new ParameterInfo<Double>("Random-seed","",0.));

			for (Entry<String,Class<?>> entry : builtInRecordables.entrySet()) 
				recordables.add(new RecordableInfo(entry.getKey(),entry.getValue(),entry.getKey()));
			
			String source = new String();
			char[] buffer = new char[8096];
			int chars = 0;
			while ((chars = reader.read(buffer)) > 0) 
				source += String.valueOf(buffer,0,chars);
			try {
				reader.close();
			} catch (IOException ee) {}

			MEMENetLogoParser parser = new MEMENetLogoParser(source,provider.getModelRoot(),setupCommand);
			parser.parse();
			
			processInterfaceElements(parser.getInterfaceParameters());
			
			if (parser.getAllGlobals().size() > 0) {
				File generatedFile = generateDummyModel(provider.getModelFile().getAbsolutePath(),generatedFileName,parser);
				
				if (parser.getExtensions().size() > 0) 
					migrateExtensionsIfNeed(parser.getExtensions(),provider.getModelFile().getParentFile());
				
				String output = runDummyModel(generatedFile);
				generatedFile.delete();
				processGlobals(calculateTypeOfGlobals(parser.getAllGlobals(),output));
			}
			processZeroArgReporters(parser.getNullArgReporterNames());
			processOtherReporters(parser.getOtherReporterNamesAndNoOfParams());
			processCommands(parser.getCommandNamesAndNoOfParams());
			processBreeds(parser.getBreeds(),parser.getDirectedLinkBreeds(),parser.getUndirectedLinkBreeds());
			registerResources(parser.getAllIncludedFiles());
		} catch (Throwable e) {
			error = e;
		} 
	}
	
	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	private void processInterfaceElements(List<InterfaceParameter> elements) {
		for (InterfaceParameter p : elements) {
			String name = Util.capitalize(p.getName());
			if (p.getPossibleValues() != null) 
				parameters.add(new NetLogoChooserParameterInfo(name,"",p.getDefaultValue(),p.getPossibleValues()));
			else 
				parameters.add(new ParameterInfo(name,"",p.getDefaultValue()));
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void processGlobals(Map<String,Class<?>> globals) {
		for (Entry<String,Class<?>> entry : globals.entrySet()) {
			if (entry.getValue().equals(unknown.class))
				nonRecordables.add(new NonRecordableInfo(entry.getKey(),entry.getValue(),entry.getKey()));
			else
				recordables.add(new RecordableInfo(entry.getKey(),entry.getValue(),entry.getKey()));
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void processZeroArgReporters(List<String> reporterNames) {
		for (String name : reporterNames)
			recordables.add(new RecordableInfo(name,unknown.class,name + " "));
	}
	
	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	private void processOtherReporters(List<Pair<String,Integer>> others) {
		for (Pair<String,Integer> p : others) 
			nonRecordables.add(new NonRecordableFunctionInfo(p.getFirst(),unknown.class,p.getFirst() + " ",(List)Collections.nCopies(p.getSecond(),
															 unknown.class)));
	}
	
	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	private void processCommands(List<Pair<String,Integer>> commands) {
		for (Pair<String,Integer> p : commands) 
			nonRecordables.add(new NonRecordableFunctionInfo(p.getFirst(),Void.TYPE,p.getFirst() + " ",(List)Collections.nCopies(p.getSecond(),
															 unknown.class)));
	}
	
	//----------------------------------------------------------------------------------------------------
	private void processBreeds(final List<BreedInfo> breeds, final List<BreedInfo> directedLinkBreeds, final List<BreedInfo> undirectedLinkBreeds) {
		NonRecordableInfo info = new NonRecordableInfo("turtles",agentset.class,"turtles");
		info.setInnerType(turtle.class);
		nonRecordables.add(info);
		
		info = new NonRecordableInfo("patches",agentset.class,"patches");
		info.setInnerType(patch.class);
		nonRecordables.add(info);
		
		info = new NonRecordableInfo("links",agentset.class,"links");
		info.setInnerType(link.class);
		nonRecordables.add(info);
		
		final Map<String,Class<?>> classCache = new HashMap<String,Class<?>>();
		processBreedList(breeds,classCache);
		processBreedList(directedLinkBreeds,classCache);
		processBreedList(undirectedLinkBreeds,classCache);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void processBreedList(final List<BreedInfo> breedList, final Map<String,Class<?>> classCache) {
		final String packageName = "_";
		for (final BreedInfo bi : breedList) {
			final String className = bi.getAgentName() == null ? bi.getAgentSetName() : bi.getAgentName();
			Class<?> breedClass = null;
			if (classCache.containsKey(packageName + "." + className))
				breedClass = classCache.get(packageName + "." + className);
			else {
				try {
					breedClass = Class.forName(packageName + "." + className);
				} catch (ClassNotFoundException e) {
					breedClass = createClassForBreed(bi);
				}
				classCache.put(breedClass.getName(),breedClass);
			}
			final BreedNonRecordableInfo info = new BreedNonRecordableInfo(bi.getAgentSetName(),agentset.class,bi.getAgentSetName());
			info.setInnerType(breedClass);
			nonRecordables.add(info);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private Class<?> createClassForBreed(final BreedInfo info) {
		if (breedAncestors == null) {
			breedAncestors = new HashMap<BreedType,CtClass>();
			try {
				breedAncestors.put(BreedType.TURTLE,provider.getClassPool().get("_.turtle"));
				final CtClass linkClass = provider.getClassPool().get("_.link");
				breedAncestors.put(BreedType.DIRECTED,linkClass);
				breedAncestors.put(BreedType.UNDIRECTED,linkClass);
			} catch (NotFoundException e) {
				throw new IllegalStateException(e);
			}
		}
		final CtClass ancestor = breedAncestors.get(info.getBreedType());
		CtClass newClass = null;
		final String className = info.getAgentName() == null ? info.getAgentSetName() : info.getAgentName();
		try {
			newClass = provider.getClassPool().get(ancestor.getPackageName() + "." + className);
		} catch (NotFoundException e1) {
			newClass = provider.getClassPool().makeClass(ancestor.getPackageName() + "." + className,ancestor);
		}
		newClass.stopPruning(true);
		try {
			return newClass.toClass();
		} catch (CannotCompileException e) {
			throw new IllegalStateException(e);
		} finally {
			newClass.defrost();
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private File generateDummyModel(String originalFilePath, String fileName, MEMENetLogoParser parser) throws IOException {
		File dir = provider.getModelFile().getParentFile();
		File file = new File(dir,fileName);
		PrintWriter writer = new PrintWriter(new FileWriter(file));
		
		// header section
		StringBuilder line = new StringBuilder(EXTENSIONS_KEYWORD);
		line.append(" ").append(OPEN_BRACKET).append(" ").append(TYPECHECKER).append(" ");
		for (String extension : parser.getExtensions())
			line.append(extension).append(" ");
		line.append(CLOSE_BRACKET);
		writer.println(line.toString());
		
		line = new StringBuilder(GLOBALS_KEYWORD);
		line.append(" ").append(OPEN_BRACKET).append(" ");
		for (String global : parser.getGlobals())
			line.append(global).append(" ");
		line.append(CLOSE_BRACKET);
		writer.println(line.toString());
		
		line = new StringBuilder(INCLUDES_KEYWORD);
		line.append(" ").append(OPEN_BRACKET).append(" ");
		for (String include : parser.getIncludes())
			line.append("\"").append(include).append("\" ");
		line.append(CLOSE_BRACKET);
		writer.println(line.toString());
		
		for (final BreedInfo bi : parser.getBreeds()) {
			if (!bi.isFromIncludeFile()) {
				String _line = BREED_KEYWORD + " " + OPEN_BRACKET + " " + bi.getAgentSetName() +
							   (bi.getAgentName() == null ? "" : " " + bi.getAgentName() + " ") + CLOSE_BRACKET;
				writer.println(_line);
			}
		}
		
		for (final BreedInfo bi : parser.getDirectedLinkBreeds()) {
			if (!bi.isFromIncludeFile()) {
				String _line = D_LINK_BREED_KEYWORD + " " + OPEN_BRACKET + " " + bi.getAgentSetName() + " " + bi.getAgentName() + " " + CLOSE_BRACKET;
				writer.println(_line);
			}
		}

		for (final BreedInfo bi : parser.getUndirectedLinkBreeds()) {
			if (!bi.isFromIncludeFile()) {
				String _line = U_LINK_BREED_KEYWORD + " " + OPEN_BRACKET + " " + bi.getAgentSetName() + " " + bi.getAgentName() + " " + CLOSE_BRACKET;
				writer.println(_line);
			}
		}
		
		for (String otherHeader : parser.getOtherHeaders())
			writer.println(otherHeader);
		writer.println();
		
		// procedures section
		writer.println(getModifiedSetup(parser));
		writer.println(getModifiedGo());
		writer.println(parser.getSetupMethod());
		writer.println();
		
		for (String method : parser.getMethods()) {
			writer.println(method);
			writer.println();
		}
		
		writer.println(MAGIC_STRING);
		writer.println();
		
		// other sections
		appendOthers(originalFilePath,writer);
		
		writer.flush();
		writer.close();
		return file;
	}
	
	//----------------------------------------------------------------------------------------------------
	private String getModifiedSetup(MEMENetLogoParser parser) {
		StringBuilder s = new StringBuilder(TO_KEYWORD);
		s.append(" setup").append(SUFFIX).append("\n");
		s.append(setupCommand).append("\n");
		for (String global : parser.getAllGlobals())
			s.append("show word \"[MEME-TYPE-CHECKER]: ").append(global).append(" => \" typechecker:what-kind-of")
			 .append(" ").append(global).append("\n");
		s.append(END_KEYWORD).append("\n");
		return s.toString();
	}
	
	//----------------------------------------------------------------------------------------------------
	private String getModifiedGo() {
		StringBuilder s = new StringBuilder(TO_KEYWORD);
		s.append(" go").append(SUFFIX).append("\n");
		s.append(END_KEYWORD).append("\n");
		return s.toString();
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
	private String runDummyModel(File file) {
		PrintStream oldOut = System.out;
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		System.setOut(new PrintStream(buffer));
		
		File modelDir = file.getParentFile();
		File tcDir = new File(modelDir,TYPECHECKER);
		File tcJar = null;
		
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(provider.getCustomClassLoader());
		try {
			
//			MEMEHeadlessWorkspace ws = new MEMEHeadlessWorkspace();
//			tcDir.mkdir();
//			tcJar = copyTypeChecker(tcDir);
//			ws.open(file.getAbsolutePath());
//			ws.runExperiment(xmlSource,2,null,null,'|',null);
//			ws.dispose();
		
			Class<?> workspaceClass = Class.forName("ai.aitia.meme.paramsweep.platform.netlogo.impl.MEMEHeadlessWorkspace", true, provider.getCustomClassLoader());
			Constructor<?> constructor = workspaceClass.getConstructor();
			
			final HeadlessWorkspace ws = (HeadlessWorkspace) constructor.newInstance();
			
			tcDir.mkdir();
			tcJar = copyTypeChecker(tcDir);
			ws.open(file.getAbsolutePath());
			Method runExperimentMethod = workspaceClass.getMethod("runExperiment", String.class, Integer.TYPE, String.class, PrintWriter.class, Character.TYPE, ProgressListener.class);
			runExperimentMethod.invoke(ws, xmlSource, 2, null, null, '|', null);
			ws.dispose();

			
			byte[] byteArray = buffer.toByteArray();
			int size = buffer.size();
			buffer.close();
			return new String(byteArray,0,size);
		} catch (Exception e) {
			// never happens
			throw new IllegalStateException(e);
		} finally {
			System.setOut(oldOut);
			if (tcJar != null)
				tcJar.delete();
			tcDir.delete();
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private File copyTypeChecker(File destDir) throws IOException {
		File typeChecker = new File("resources/typechecker4.jar");
		if (!typeChecker.exists())
			typeChecker = new File("typechecker4.jar");
		File destFile = new File(destDir,"typechecker.jar");
		Util.copyInputStream(new FileInputStream(typeChecker),new FileOutputStream(destFile));
		return destFile;
	}
	
	//----------------------------------------------------------------------------------------------------
	private Map<String,Class<?>> calculateTypeOfGlobals(List<String> globals, String output) {
		Map<String,Class<?>> result =  new HashMap<String,Class<?>>();
		String identifierPattern = "[\\w\\.=\\*!<>:#\\+/%\\$_\\^'&\\-][\\w\\.=\\*!<>:#\\+/%\\$_\\^'&\\-\\?]*";
		String typePattern = "(?:<boolean>)|(?:<number>)|(?:<string>)|(?:<other>)";
		Pattern pattern = Pattern.compile("observer: \"\\[MEME-TYPE-CHECKER\\]: (" + identifierPattern + ")+ => (" + typePattern + ")\"");
		String[] lines = output.split("\n");
		for (String line : lines) {
			Matcher m = pattern.matcher(line.trim());
			if (m.matches()) {
				String name = m.group(1);
				String type = m.group(2);
				Class<?> clazz = toClass(type);
				if (clazz != null && globals.contains(name)) 
					result.put(name,clazz);
			}
		}
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	private Class<?> toClass(String type) {
		if (type.equals("<boolean>")) return Boolean.class;
		if (type.equals("<number>")) return Double.class;
		if (type.equals("<string>")) return String.class;
		if (type.equals("<other>")) return unknown.class;
		return null;
	}

	//----------------------------------------------------------------------------------------------------
	private void registerResources(List<File> resources) {
		if (resources.size() > 0) {
			List<String> resourceList = new ArrayList<String>();
			List<String> old = provider.getAutomaticResources();
			if (old != null) 
				resourceList.addAll(old);
			for (File f : resources) { 
				String absolutePath = f.getAbsolutePath();
				if (!resourceList.contains(absolutePath))
					resourceList.add(absolutePath);
			}
			provider.setAutomaticResources(resourceList);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void migrateExtensionsIfNeed(final List<String> extensions, final File modelDir) throws IOException {
		final File mainExtensionsDir = new File(PlatformManager.getInstallationDirectory(PlatformType.NETLOGO) + File.separator + "extensions");
		
		for (final String extension : extensions) {
			final File extensionDir = new File(modelDir,extension);
			if (!extensionDir.exists()) {
				final File sourceDir = new File(mainExtensionsDir,extension);
				Util.copyFolder(sourceDir,extensionDir);
			}
		}
		
		final List<String> resourceList = new ArrayList<String>();
		final List<String> old = provider.getAutomaticResources();
		if (old != null)
			resourceList.addAll(old);
		for (final String extension : extensions) {
			final File extensionDir = new File(modelDir,extension);
			migrate(extensionDir,resourceList);
		}
		provider.setAutomaticResources(resourceList);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void migrate(final File file, final List<String> resources) {
		if (file.isDirectory()) {
			final File[] files = file.listFiles();
			for (final File f : files)
				migrate(f,resources);
		} else {
			final String absPath = file.getAbsolutePath();
			if (!resources.contains(absPath))
				resources.add(absPath);
		}
	}

	@Override
	public List<RecorderInfo> getRecorders() throws ModelInformationException {
		return null;
	}

	@Override
	public String getRecordersXML() throws ModelInformationException {
		return null;
	}
}