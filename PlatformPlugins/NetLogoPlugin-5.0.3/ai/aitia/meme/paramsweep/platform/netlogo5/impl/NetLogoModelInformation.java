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
package ai.aitia.meme.paramsweep.platform.netlogo5.impl;

import static ai.aitia.meme.paramsweep.platform.netlogo5.impl.MEMENetLogoParser.BREED_KEYWORD;
import static ai.aitia.meme.paramsweep.platform.netlogo5.impl.MEMENetLogoParser.CLOSE_BRACKET;
import static ai.aitia.meme.paramsweep.platform.netlogo5.impl.MEMENetLogoParser.D_LINK_BREED_KEYWORD;
import static ai.aitia.meme.paramsweep.platform.netlogo5.impl.MEMENetLogoParser.END_KEYWORD;
import static ai.aitia.meme.paramsweep.platform.netlogo5.impl.MEMENetLogoParser.EXTENSIONS_KEYWORD;
import static ai.aitia.meme.paramsweep.platform.netlogo5.impl.MEMENetLogoParser.GLOBALS_KEYWORD;
import static ai.aitia.meme.paramsweep.platform.netlogo5.impl.MEMENetLogoParser.INCLUDES_KEYWORD;
import static ai.aitia.meme.paramsweep.platform.netlogo5.impl.MEMENetLogoParser.MAGIC_STRING;
import static ai.aitia.meme.paramsweep.platform.netlogo5.impl.MEMENetLogoParser.OPEN_BRACKET;
import static ai.aitia.meme.paramsweep.platform.netlogo5.impl.MEMENetLogoParser.TO_KEYWORD;
import static ai.aitia.meme.paramsweep.platform.netlogo5.impl.MEMENetLogoParser.U_LINK_BREED_KEYWORD;

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

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.NotFoundException;

import org.nlogo.headless.HeadlessWorkspace;
import org.nlogo.lab.Protocol;
import org.nlogo.lab.Worker;
import org.nlogo.nvm.LabInterface.ProgressListener;

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
import ai.aitia.meme.paramsweep.platform.netlogo.impl.NetLogoChooserParameterInfo;
import ai.aitia.meme.paramsweep.platform.netlogo5.impl.MEMENetLogoParser.BreedInfo;
import ai.aitia.meme.paramsweep.platform.netlogo5.impl.MEMENetLogoParser.BreedType;
import ai.aitia.meme.paramsweep.platform.netlogo5.impl.MEMENetLogoParser.InterfaceParameter;
import ai.aitia.meme.paramsweep.utils.Util;
import ai.aitia.meme.utils.Utils.Pair;

public class NetLogoModelInformation implements IModelInformation {

	//====================================================================================================
	// members
	
	// constants
	private static final String		SUFFIX = "_aitiaGenerated";
	private static final String 	TYPECHECKER = "typechecker";
	
	private static final String XML_SOURCE = "<experiments>" + 
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
	
	private final IPSWInformationProvider provider;
	private final String setupCommand;
	private Throwable error = null;
	
	private List<ParameterInfo<?>> parameters = null;
	private List<RecordableInfo> recordables = null;
	private List<NonRecordableInfo> nonRecordables = null;

	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public NetLogoModelInformation(final IPSWInformationProvider provider, final  String setupCommand) {
		this.provider = provider;
		this.setupCommand = setupCommand;
		analyzeModel();
	}

	//====================================================================================================
	// implemented interfaces

	//----------------------------------------------------------------------------------------------------
	public String getName() {
		final File file = new File(provider.getEntryPoint());
		final int idx = file.getName().lastIndexOf('.');
		return file.getName().substring(0,idx);
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
			final FileReader reader = new FileReader(provider.getModelFile());
			
			final String fileName = provider.getModelFile().getName();
			final int idx = fileName.lastIndexOf('.');
			final String name = idx == -1 ? fileName : fileName.substring(0,idx);
			final String generatedFileName = name + SUFFIX + "__" + Util.getTimeStamp() + ".nlogo";
			
			if (provider.rngSeedAsParameter())
				parameters.add(new ParameterInfo<Double>("Random-seed","",0.));

			for (final Entry<String,Class<?>> entry : builtInRecordables.entrySet()) 
				recordables.add(new RecordableInfo(entry.getKey(),entry.getValue(),entry.getKey()));
			
			final StringBuffer source = new StringBuffer();
			final char[] buffer = new char[8096];
			int chars = 0;
			while ((chars = reader.read(buffer)) > 0) 
				source.append(String.valueOf(buffer,0,chars));
			try {
				reader.close();
			} catch (final IOException _) {}

			final MEMENetLogoParser parser = new MEMENetLogoParser(source.toString(),provider.getModelRoot(),setupCommand);
			parser.parse();
			
			processInterfaceElements(parser.getInterfaceParameters());
			
			if (parser.getAllGlobals().size() > 0) {
				final File generatedFile = generateDummyModel(provider.getModelFile().getAbsolutePath(),generatedFileName,parser);
				
				if (parser.getExtensions().size() > 0) 
					migrateExtensionsIfNeed(parser.getExtensions(),provider.getModelFile().getParentFile());
				
				final String output = runDummyModel(generatedFile);
				generatedFile.delete();
				processGlobals(calculateTypeOfGlobals(parser.getAllGlobals(),output));
			}
			processZeroArgReporters(parser.getNullArgReporterNames());
			processOtherReporters(parser.getOtherReporterNamesAndNoOfParams());
			processCommands(parser.getCommandNamesAndNoOfParams());
			processBreeds(parser.getBreeds(),parser.getDirectedLinkBreeds(),parser.getUndirectedLinkBreeds());
			registerResources(parser.getAllIncludedFiles());
		} catch (final Throwable e) {
			error = e;
		} 
	}
	
	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void processInterfaceElements(final List<InterfaceParameter> elements) {
		for (final InterfaceParameter p : elements) {
			final String name = Util.capitalize(p.getName());
			if (p.getPossibleValues() != null) 
				parameters.add(new NetLogoChooserParameterInfo(name,"",p.getDefaultValue(),p.getPossibleValues()));
			else 
				parameters.add(new ParameterInfo(name,"",p.getDefaultValue()));
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void processGlobals(final Map<String,Class<?>> globals) {
		for (final Entry<String,Class<?>> entry : globals.entrySet()) {
			if (entry.getValue().equals(unknown.class))
				nonRecordables.add(new NonRecordableInfo(entry.getKey(),entry.getValue(),entry.getKey()));
			else
				recordables.add(new RecordableInfo(entry.getKey(),entry.getValue(),entry.getKey()));
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void processZeroArgReporters(final List<String> reporterNames) {
		for (final String name : reporterNames)
			recordables.add(new RecordableInfo(name,unknown.class,name + " "));
	}
	
	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void processOtherReporters(final List<Pair<String,Integer>> others) {
		for (final Pair<String,Integer> p : others) 
			nonRecordables.add(new NonRecordableFunctionInfo(p.getFirst(),unknown.class,p.getFirst() + " ",(List)Collections.nCopies(p.getSecond(),
															 unknown.class)));
	}
	
	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void processCommands(final List<Pair<String,Integer>> commands) {
		for (final Pair<String,Integer> p : commands) 
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
		final Object lock = new Object();
		synchronized (lock) {
			if (breedAncestors == null) {
				breedAncestors = new HashMap<BreedType,CtClass>();
				try {
					breedAncestors.put(BreedType.TURTLE,provider.getClassPool().get("_.turtle"));
					final CtClass linkClass = provider.getClassPool().get("_.link");
					breedAncestors.put(BreedType.DIRECTED,linkClass);
					breedAncestors.put(BreedType.UNDIRECTED,linkClass);
				} catch (final NotFoundException e) {
					throw new IllegalStateException(e);
				}
			}
		}
		final CtClass ancestor = breedAncestors.get(info.getBreedType());
		CtClass newClass = null;
		final String className = info.getAgentName() == null ? info.getAgentSetName() : info.getAgentName();
		try {
			newClass = provider.getClassPool().get(ancestor.getPackageName() + "." + className);
		} catch (final NotFoundException _) {
			newClass = provider.getClassPool().makeClass(ancestor.getPackageName() + "." + className,ancestor);
		}
		newClass.stopPruning(true);
		try {
			return newClass.toClass();
		} catch (final CannotCompileException e) {
			throw new IllegalStateException(e);
		} finally {
			newClass.defrost();
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private File generateDummyModel(final String originalFilePath, final String fileName, final MEMENetLogoParser parser) throws IOException {
		final File dir = provider.getModelFile().getParentFile();
		final File file = new File(dir,fileName);
		final PrintWriter writer = new PrintWriter(new FileWriter(file));
		
		// header section
		StringBuilder line = new StringBuilder(EXTENSIONS_KEYWORD);
		line.append(" ").append(OPEN_BRACKET).append(" ").append(TYPECHECKER).append(" ");
		for (final String extension : parser.getExtensions())
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
	private String getModifiedSetup(final MEMENetLogoParser parser) {
		final StringBuilder builder = new StringBuilder(TO_KEYWORD);
		builder.append(" setup").append(SUFFIX).append("\n ");
		builder.append(setupCommand).append("\n ");
		for (final String global : parser.getAllGlobals())
			builder.append("show word \"[MEME-TYPE-CHECKER]: ").append(global).append(" => \" typechecker:what-kind-of")
			 .append(" ").append(global).append("\n ");
		builder.append(END_KEYWORD).append("\n ");
		return builder.toString();
	}
	
	//----------------------------------------------------------------------------------------------------
	private String getModifiedGo() {
		final StringBuilder builder = new StringBuilder(TO_KEYWORD);
		builder.append(" go").append(SUFFIX).append("\n ");
		builder.append(END_KEYWORD).append("\n ");
		return builder.toString();
	}
	
	//----------------------------------------------------------------------------------------------------
	private void appendOthers(final String originalFilePath, final PrintWriter writer) throws IOException {
		final BufferedReader reader = new BufferedReader(new FileReader(originalFilePath));
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
	private String runDummyModel(final File file) {
		final PrintStream oldOut = System.out;
		final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		System.setOut(new PrintStream(buffer));
		
		final File modelDir = file.getParentFile();
		final File tcDir = new File(modelDir,TYPECHECKER);
		File tcJar = null;
		
		// netlogo uses the contextclassloader to load classes, therefore we
		// should set it to the one that loaded the netlogo plugin
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(provider.getCustomClassLoader());
		try {
//			final MEMEHeadlessWorkspace workspace = MEMEHeadlessWorkspace.newWorkspace();
			Class<?> workspaceClass = Class.forName("ai.aitia.meme.paramsweep.platform.netlogo5.impl.MEMEHeadlessWorkspace", true, provider.getCustomClassLoader());
			Method newInstanceMethod = workspaceClass.getMethod("newWorkspace");
			
			final HeadlessWorkspace workspace = (HeadlessWorkspace) newInstanceMethod.invoke(null);
			
			tcDir.mkdir();
			tcJar = copyTypeChecker(tcDir);
			workspace.open(file.getAbsolutePath());
			Method runExperimentMethod = workspaceClass.getMethod("runExperiment", String.class, Integer.TYPE, String.class, PrintWriter.class, Character.TYPE, ProgressListener.class);
			runExperimentMethod.invoke(workspace, XML_SOURCE, 2, null, null, '|', null);
			//workspace.runExperiment(XML_SOURCE,2,null,null,'|',null);
			workspace.dispose();
			final byte[] byteArray = buffer.toByteArray();
			final int size = buffer.size();
			buffer.close();
			return new String(byteArray,0,size);
		} catch (final Exception e) {
			// never happens
			throw new IllegalStateException(e);
		} finally {
			System.setOut(oldOut);
			if (tcJar != null)
				tcJar.delete();
			tcDir.delete();
			Thread.currentThread().setContextClassLoader(contextClassLoader);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private File copyTypeChecker(final File destDir) throws IOException {
		File typeChecker = new File("resources/typechecker5.jar");
		if (!typeChecker.exists())
			typeChecker = new File("typechecker5.jar");
		final File destFile = new File(destDir,"typechecker.jar");
		Util.copyInputStream(new FileInputStream(typeChecker),new FileOutputStream(destFile));
		return destFile;
	}
	
	//----------------------------------------------------------------------------------------------------
	private Map<String,Class<?>> calculateTypeOfGlobals(final List<String> globals, final String output) {
		final Map<String,Class<?>> result =  new HashMap<String,Class<?>>();
		final String identifierPattern = "[\\w\\.=\\*!<>:#\\+/%\\$_\\^'&\\-][\\w\\.=\\*!<>:#\\+/%\\$_\\^'&\\-\\?]*";
		final String typePattern = "(?:<boolean>)|(?:<number>)|(?:<string>)|(?:<other>)";
		final Pattern pattern = Pattern.compile("observer: \"\\[MEME-TYPE-CHECKER\\]: (" + identifierPattern + ")+ => (" + typePattern + ")\"");
		final String[] lines = output.split("\n");
		for (final String line : lines) {
			final Matcher matcher = pattern.matcher(line.trim());
			if (matcher.matches()) {
				final String name = matcher.group(1);
				final String type = matcher.group(2);
				final Class<?> clazz = toClass(type);
				if (clazz != null && globals.contains(name)) 
					result.put(name,clazz);
			}
		}
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	private Class<?> toClass(final String type) {
		if ("<boolean>".equals(type)) return Boolean.class;
		if ("<number>".equals(type)) return Double.class;
		if ("<string>".equals(type)) return String.class;
		if ("<other>".equals(type)) return unknown.class;
		return null;
	}

	//----------------------------------------------------------------------------------------------------
	private void registerResources(final List<File> resources) {
		if (!resources.isEmpty()) {
			final List<String> resourceList = new ArrayList<String>();
			final List<String> old = provider.getAutomaticResources();
			if (old != null) 
				resourceList.addAll(old);
			for (final File f : resources) { 
				final String absolutePath = f.getAbsolutePath();
				if (!resourceList.contains(absolutePath))
					resourceList.add(absolutePath);
			}
			provider.setAutomaticResources(resourceList);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void migrateExtensionsIfNeed(final List<String> extensions, final File modelDir) throws IOException {
		final File mainExtensionsDir = new File(PlatformManager.getInstallationDirectory(PlatformType.NETLOGO5) + File.separator + "extensions");
		
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
		return new ArrayList<RecorderInfo>();
	}

	@Override
	public String getRecordersXML() throws ModelInformationException {
		return null;
	}
}
