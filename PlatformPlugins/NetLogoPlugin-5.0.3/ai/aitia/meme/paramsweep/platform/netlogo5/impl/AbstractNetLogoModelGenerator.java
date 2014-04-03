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
import static ai.aitia.meme.paramsweep.platform.netlogo5.impl.MEMENetLogoParser.EXTENSIONS_KEYWORD;
import static ai.aitia.meme.paramsweep.platform.netlogo5.impl.MEMENetLogoParser.GLOBALS_KEYWORD;
import static ai.aitia.meme.paramsweep.platform.netlogo5.impl.MEMENetLogoParser.INCLUDES_KEYWORD;
import static ai.aitia.meme.paramsweep.platform.netlogo5.impl.MEMENetLogoParser.MAGIC_STRING;
import static ai.aitia.meme.paramsweep.platform.netlogo5.impl.MEMENetLogoParser.OPEN_BRACKET;
import static ai.aitia.meme.paramsweep.platform.netlogo5.impl.MEMENetLogoParser.U_LINK_BREED_KEYWORD;

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
import ai.aitia.meme.paramsweep.batch.output.RecorderInfo;
import ai.aitia.meme.paramsweep.platform.IPSWInformationProvider;
import ai.aitia.meme.paramsweep.platform.netlogo.info.NLStatisticGeneratedRecordableInfo;
import ai.aitia.meme.paramsweep.platform.netlogo5.impl.MEMENetLogoParser.BreedInfo;
import ai.aitia.meme.paramsweep.platform.repast.info.ExtendedOperatorGeneratedRecordableInfo;
import ai.aitia.meme.paramsweep.platform.repast.info.GeneratedRecordableInfo;
import ai.aitia.meme.paramsweep.platform.repast.info.OperatorGeneratedRecordableInfo;
import ai.aitia.meme.paramsweep.platform.repast.info.ScriptGeneratedRecordableInfo;
import ai.aitia.meme.paramsweep.utils.AssistantMethod;
import ai.aitia.meme.paramsweep.utils.PlatformConstants;
import ai.aitia.meme.paramsweep.utils.UserDefinedVariable;
import ai.aitia.meme.paramsweep.utils.Util;
import ai.aitia.meme.utils.Utils.Pair;

public abstract class AbstractNetLogoModelGenerator {

	//====================================================================================================
	// members
	
	protected static final String DOUBLE_NEWLINE = "\n\n ";
	protected static final String STEP_METHOD = "aitia_generated_step_method";
	protected static final String INIT = "__init-aitia-generated-user-variables";  
	
	private static final String PACKAGE_NAME	= "ai.aitia.script.extension";
	private static final String MANAGER_CLASS_NAME = "ScriptExtensionManager";

	protected final ClassPool pool;
	protected final String modelFile;
	protected final List<RecorderInfo> recorders;
	protected final String timestamp;
	protected final List<String> generatedScripts = new ArrayList<String>();
	protected final List<String> nonStatisticReporters = new ArrayList<String>();
	protected final List<CtClass> generatedClasses = new ArrayList<CtClass>();
	protected String generatedModelName = null;

	protected final CtClass defaultReporter;
	protected final CtClass reporterInterface;
	protected final CtClass defaultClassManager;
	protected final CtClass classManagerInterface;
	
	protected final List<Pair<String,AssistantMethod>> assistantMethods = new ArrayList<Pair<String,AssistantMethod>>();
	protected boolean hasTimeSeries = false;
	protected final String SETUP_COMMAND;
	
	protected MEMENetLogoParser parser = null;
	
	//====================================================================================================
	// methods

	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	protected AbstractNetLogoModelGenerator(final IPSWInformationProvider provider, final String setupCommand) {
		if (provider == null)
			throw new IllegalArgumentException("'provider' is null.");
		this.pool = provider.getClassPool();
		this.modelFile = provider.getModelFile().getAbsolutePath();
		this.recorders = provider.getRecorders();
		this.SETUP_COMMAND = setupCommand;
		
		try {
			this.defaultReporter = this.pool.get("org.nlogo.api.DefaultReporter");
			this.reporterInterface = this.pool.get("org.nlogo.api.Reporter");
			this.defaultClassManager = this.pool.get("org.nlogo.api.DefaultClassManager");
			this.classManagerInterface = this.pool.get("org.nlogo.api.ClassManager");
		} catch (final NotFoundException e) {
			throw new IllegalStateException(e);
		}
		timestamp = Util.getTimeStamp();
	}
	
	//----------------------------------------------------------------------------------------------------
	protected void generateReporterClass(final GeneratedRecordableInfo info) throws CannotCompileException {
		importPackages();
		if (info instanceof NLStatisticGeneratedRecordableInfo) {
			final NLStatisticGeneratedRecordableInfo _info = (NLStatisticGeneratedRecordableInfo) info;
			generateStatisticClass(_info);
		} else if ((info instanceof ScriptGeneratedRecordableInfo) || (info instanceof OperatorGeneratedRecordableInfo)) 
			generateClassOfReferenced(info);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void generateClassOfReferenced(final GeneratedRecordableInfo info) throws CannotCompileException {
		if (generatedScripts.contains(info.getAccessibleName())) return;
		
		final List<GeneratedRecordableInfo> references = info.getReferences();
		for (final GeneratedRecordableInfo gri : references) {
			if (gri instanceof NLStatisticGeneratedRecordableInfo) {
				final NLStatisticGeneratedRecordableInfo _gri = (NLStatisticGeneratedRecordableInfo) gri;
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
	private void generateStatisticClass(final NLStatisticGeneratedRecordableInfo info) throws CannotCompileException {
		if (generatedScripts.contains(info.getAccessibleName())) return;
		
		final List<GeneratedRecordableInfo> references = info.getReferences();
		for (final GeneratedRecordableInfo gri : references) {
			if (gri instanceof NLStatisticGeneratedRecordableInfo) {
				final NLStatisticGeneratedRecordableInfo _gri = (NLStatisticGeneratedRecordableInfo) gri;
				generateStatisticClass(_gri);
			} else if ((gri instanceof ScriptGeneratedRecordableInfo) || (gri instanceof OperatorGeneratedRecordableInfo)) 
				generateClassOfReferenced(gri);
		}
		
		final CtClass clazz = pool.makeClass(PACKAGE_NAME + "." + createClassName(info.getAccessibleName()),defaultReporter);
		clazz.addInterface(reporterInterface);
		
		final StringBuilder syntax = new StringBuilder("public strictfp Syntax getSyntax() {\n ");
		syntax.append(info.getSyntaxBody()).append("\n ");
		syntax.append("}\n");
		final CtMethod syntaxMethod = CtNewMethod.make(syntax.toString(),clazz);
		clazz.addMethod(syntaxMethod);
		
		final StringBuilder report = new StringBuilder("public Object report(Argument[] ");
		report.append(PlatformConstants.ARGUMENT).append(", Context context) throws ExtensionException, LogoException {\n ");
		report.append(info.getReportBody()).append("\n ");
		report.append("}\n ");
		final CtMethod reportMethod = CtNewMethod.make(report.toString(),clazz);
		clazz.addMethod(reportMethod);
		
		generatedClasses.add(clazz);
		generatedScripts.add(info.getAccessibleName());
	}
	
	//----------------------------------------------------------------------------------------------------
	private void importPackages() { pool.importPackage("org.nlogo.api"); }

	//----------------------------------------------------------------------------------------------------
	private String createClassName(final String reporterName) {
		String result = reporterName;
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
	protected void generateManagerClass() throws CannotCompileException {
		final CtClass clazz = pool.makeClass(PACKAGE_NAME + "." + MANAGER_CLASS_NAME,defaultClassManager);
		clazz.addInterface(classManagerInterface);
		
		final StringBuilder load = new StringBuilder("public void load(PrimitiveManager primitivemanager) throws ExtensionException {\n ");
		for (final String name : generatedScripts) {
			if (!nonStatisticReporters.contains(name)) {
				load.append("primitivemanager.addPrimitive(\"").append(name).append("\",new ").append(PACKAGE_NAME).append(".");
				load.append(createClassName(name)).append("());\n ");
			}
		}
		load.append("}\n ");
		final CtMethod loadMethod = CtNewMethod.make(load.toString(),clazz);
		clazz.addMethod(loadMethod);
		generatedClasses.add(clazz);
	}
	
	//----------------------------------------------------------------------------------------------------
	protected void createReporterClassFiles() throws IOException, CannotCompileException, NotFoundException {
		final File tempDir = new File("NetLogoTemp");
		boolean exception = true; 
		try {
			tempDir.mkdir();
			pool.appendClassPath(tempDir.getAbsolutePath());
			final File packageDir = new File(tempDir,PACKAGE_NAME.replace('.','/'));
			packageDir.mkdirs();
			
			for (final CtClass c : generatedClasses) {
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
	protected File createExtension() throws CannotCompileException, IOException { 
		final File tempDir = new File("NetLogoTemp");
		try {
			for (final CtClass c : generatedClasses) {
				c.stopPruning(true);
				c.writeFile(tempDir.getAbsolutePath());
				c.defrost();
			}
			
			final File modelDir = new File(modelFile).getParentFile();
			final File extensionDir = new File(modelDir,PlatformConstants.AITIA_GENERATED_SCRIPTS);
			extensionDir.mkdir();
			
			final File manifest = new File("resources/nlmanifest5.mf");
			jarDirectory(tempDir,new File(extensionDir,PlatformConstants.AITIA_GENERATED_SCRIPTS + ".jar"),manifest);
			copyRequiredJars(extensionDir);
			
			return extensionDir;
		} finally {
			deleteDir(tempDir);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void jarDirectory(final File dir2zip, final File destFile, final File manifest) throws IOException {
		final Manifest man = new Manifest(new FileInputStream(manifest));
		final JarOutputStream zos = new JarOutputStream(new FileOutputStream(destFile),man);
		zos.setLevel(Deflater.BEST_COMPRESSION);
		jarDir(dir2zip,dir2zip,zos);
		zos.finish();
		zos.close();
	}
	
	//----------------------------------------------------------------------------------------------------
	private void jarDir(final File original, final File dir2zip, final JarOutputStream zos) throws IOException {
		// get a listing of the directory content 
	    final File[] dirList = dir2zip.listFiles(); 
	    final byte[] readBuffer = new byte[2156]; 
	    int bytesIn = 0;
	    
	    //loop through dirList, and zip the files
	    final int idx = original.getPath().length();
	    for (int i = 0;i < dirList.length;i++) { 
	        final String substring = dirList[i].getPath().substring(idx + 1).replace('\\','/');
			if (dirList[i].isDirectory()) {
	        	final JarEntry anEntry = new JarEntry(substring + "/");
	        	zos.putNextEntry(anEntry);
	        	zos.closeEntry();
	        	jarDir(original,dirList[i],zos); 
	        } else {
	        	final FileInputStream fis = new FileInputStream(dirList[i]); 
	        	final JarEntry anEntry = new JarEntry(substring); 
	        	zos.putNextEntry(anEntry);
	        	while ((bytesIn = fis.read(readBuffer)) != -1) 
	        		zos.write(readBuffer,0,bytesIn); 
	        	zos.closeEntry();
	        	fis.close();
	        }
	    } 
	}
	
	//----------------------------------------------------------------------------------------------------
	private void deleteDir(final File dir) {
		final File[] files = dir.listFiles();
		for (final File f : files)
			clean(f);
		dir.delete();
	}
	
	//----------------------------------------------------------------------------------------------------
	private void clean(final File file) {
		if (file.isDirectory()) {
			final File[] files = file.listFiles();
			for (final File f : files)
				clean(f);
		}
		file.delete();
	}
	
	//----------------------------------------------------------------------------------------------------
	private void copyRequiredJars(final File dest) throws IOException {
		final File colt = new File("resources/colt.jar");
		final File coltDest = new File(dest,"colt.jar");
		FileInputStream in = new FileInputStream(colt);
		FileOutputStream out = new FileOutputStream(coltDest);
		Util.copyInputStream(in,out);
		
		final File mpFile = new File("meme-paramsweep.jar");
		final File mpDest = new File(dest,"meme-paramsweep.jar");
		in = new FileInputStream(mpFile);
		out = new FileOutputStream(mpDest);
		Util.copyInputStream(in,out);
	}
	
	//----------------------------------------------------------------------------------------------------
	protected MEMENetLogoParser getParser() throws IOException {
		if (parser == null) {
			final File file = new File(modelFile);
			final FileReader reader = new FileReader(file);
			final StringBuffer source = new StringBuffer();
			final char[] buffer = new char[8096];
			int chars = 0;
			while ((chars = reader.read(buffer)) > 0) 
				source.append(String.valueOf(buffer,0,chars));
			try {
				reader.close();
			} catch (final IOException _) {}

			parser = new MEMENetLogoParser(source.toString(),file.getParent(),SETUP_COMMAND);
			parser.parse();
		}
		return parser;
	}
	
	//----------------------------------------------------------------------------------------------------
	protected Pair<PrintWriter,List<UserDefinedVariable>> createDescendantModelHeader(final boolean needExtension) throws IOException {
		final File file = new File(modelFile);
		final String modelFileName = file.getName();
		final int idx = modelFileName.lastIndexOf('.');
		final String name = idx == -1 ? modelFileName : modelFileName.substring(0,idx);
		generatedModelName = name + modelSuffix() + "__" + timestamp;

		final File descendant = new File(file.getParentFile(),generatedModelName + ".nlogo");
		final PrintWriter writer = new PrintWriter(new FileWriter(descendant));
		
		// header section
		StringBuilder line = new StringBuilder(EXTENSIONS_KEYWORD);
		line.append(" ").append(OPEN_BRACKET);
		if (needExtension)
			line.append(" ").append(PlatformConstants.AITIA_GENERATED_SCRIPTS).append(" ");
		if (hasTimeSeries)
			line.append(" table5 ");
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
				final String _line = BREED_KEYWORD + " " + OPEN_BRACKET + " " + bi.getAgentSetName() + 
							   (bi.getAgentName() == null ? "" : " " + bi.getAgentName() + " ") + CLOSE_BRACKET;
				writer.println(_line);
			}
		}
		
		for (final BreedInfo bi : getParser().getDirectedLinkBreeds()) {
			if (!bi.isFromIncludeFile()) {
				final String _line = D_LINK_BREED_KEYWORD + " " + OPEN_BRACKET + " " + bi.getAgentSetName() + " " + bi.getAgentName() + " " + CLOSE_BRACKET;
				writer.println(_line);
			}
		}

		for (final BreedInfo bi : getParser().getUndirectedLinkBreeds()) {
			if (!bi.isFromIncludeFile()) {
				final String _line = U_LINK_BREED_KEYWORD + " " + OPEN_BRACKET + " " + bi.getAgentSetName() + " " + bi.getAgentName() + " " + CLOSE_BRACKET;
				writer.println(_line);
			}
		}
		
		for (String otherHeader : getParser().getOtherHeaders())
			writer.println(otherHeader);
		writer.println();
				
		return new Pair<PrintWriter,List<UserDefinedVariable>>(writer,userVariables);		
	}
	
	//----------------------------------------------------------------------------------------------------
	protected void appendOthers(final String originalFilePath, final PrintWriter writer) throws IOException {
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
	protected void insertReporter(final GeneratedRecordableInfo info, final StringBuilder methods) {
		if (info instanceof NLStatisticGeneratedRecordableInfo) {
			final NLStatisticGeneratedRecordableInfo _info = (NLStatisticGeneratedRecordableInfo) info;
			insertStatistic(_info,methods);
		} else if ((info instanceof ScriptGeneratedRecordableInfo) || (info instanceof OperatorGeneratedRecordableInfo)) 
			insertScriptOrOperator(info,methods);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void insertStatistic(final NLStatisticGeneratedRecordableInfo info, final StringBuilder methods) {
		if (generatedScripts.contains(info.getAccessibleName())) return;
		
		final List<GeneratedRecordableInfo> references = info.getReferences();
		for (final GeneratedRecordableInfo gri : references) {
			if (gri instanceof NLStatisticGeneratedRecordableInfo) {
				final NLStatisticGeneratedRecordableInfo _gri = (NLStatisticGeneratedRecordableInfo) gri;
				insertStatistic(_gri,methods);
			} else if ((gri instanceof ScriptGeneratedRecordableInfo) || (gri instanceof OperatorGeneratedRecordableInfo)) 
				insertScriptOrOperator(gri,methods);
		}
		methods.append(info.getSource()).append("\n ");
		generatedScripts.add(info.getAccessibleName());
	}
	
	//----------------------------------------------------------------------------------------------------
	private void insertScriptOrOperator(final GeneratedRecordableInfo info, final StringBuilder methods) {
		if (generatedScripts.contains(info.getAccessibleName())) return;
		
		final List<GeneratedRecordableInfo> references = info.getReferences();
		for (final GeneratedRecordableInfo gri : references) {
			if (gri instanceof NLStatisticGeneratedRecordableInfo) {
				final NLStatisticGeneratedRecordableInfo _gri = (NLStatisticGeneratedRecordableInfo) gri;
				insertStatistic(_gri,methods);
			} else if ((gri instanceof ScriptGeneratedRecordableInfo) || (gri instanceof OperatorGeneratedRecordableInfo)) 
				insertScriptOrOperator(gri,methods);
		}
		final String source = "to-report " + info.getAccessibleName() + "\n " + info.getSource() + "\n end\n ";
		methods.append(source).append("\n ");
		generatedScripts.add(info.getAccessibleName());
	}
	
	//----------------------------------------------------------------------------------------------------
	protected void insertAssistantMethod(final Pair<String,AssistantMethod> method, final int idx, final StringBuilder methods) {
		final StringBuilder builder = new StringBuilder("to ");
		if (!method.getSecond().returnValue.equals(Void.TYPE))
			builder.append("-report ");
		String _name = method.getFirst();
		if (_name.endsWith("()")) 
			_name = _name.substring(0,_name.length() - 2);
		final String fullName = _name + PlatformConstants.AITIA_GENERATED_INFIX + idx;
		builder.append(fullName).append("\n ");
		builder.append(method.getSecond().body).append("\n ");
		builder.append("end\n ");
		methods.append(builder.toString()).append("\n ");
	}
	
	//----------------------------------------------------------------------------------------------------
	protected void insertAitiaGeneratedStepMethod(final StringBuilder methods) {
		final StringBuilder builder = new StringBuilder("to "); 
		builder.append(STEP_METHOD).append("\n ");
		for (int i = 0;i < assistantMethods.size();++i) {
			final Pair<String,AssistantMethod> method = assistantMethods.get(i);
			String _name = method.getFirst();
			if (_name.endsWith("()")) 
				_name = _name.substring(0,_name.length() - 2);
			final String fullName = _name + PlatformConstants.AITIA_GENERATED_INFIX + i;
			switch (method.getSecond().scheduleTime) {
			case NEVER : continue;
			case TICK  : builder.append(fullName).append("\n ");
						 break;
			case RUN   : builder.append(createStopIfStatement(fullName));
						 break;
			default	   : assert false;
			}
		}
		builder.append("\n end\n ");
		methods.append(builder.toString()).append(DOUBLE_NEWLINE);
	}
	
	//----------------------------------------------------------------------------------------------------
	protected List<UserDefinedVariable> collectVariables(final GeneratedRecordableInfo gmi) {
		final List<UserDefinedVariable> result = new ArrayList<UserDefinedVariable>();
		final List<GeneratedRecordableInfo> references = gmi.getReferences();
		for (final GeneratedRecordableInfo info : references) 
			Util.addAllDistinct(result,collectVariables(info));
		if (gmi instanceof ScriptGeneratedRecordableInfo) {
			final ScriptGeneratedRecordableInfo sgmi = (ScriptGeneratedRecordableInfo) gmi;
			Util.addAllDistinct(result,sgmi.getUserVariables());
		}
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	protected void generateUserVariableInitialization(final List<UserDefinedVariable> variables, final PrintWriter writer) {
		final StringBuilder code = new StringBuilder();
		code.append("to ").append(INIT).append(" \n ");
		for (final UserDefinedVariable variable : variables) 
			code.append(variable.getInitializationCode()).append("\n ");
		code.append("end\n ");
		writer.println(code.toString());
	}
	
	//----------------------------------------------------------------------------------------------------
	protected File copyTableExtension() throws IOException {
		final File modelDir = new File(modelFile).getParentFile();
		final File extensionDir = new File(modelDir,"table5");
		extensionDir.mkdir();
		
		final File table = new File("resources/table5.jar");
		final File tableDest = new File(extensionDir,"table5.jar");
		final FileInputStream in = new FileInputStream(table);
		final FileOutputStream out = new FileOutputStream(tableDest);
		Util.copyInputStream(in,out);
		
		return extensionDir;
	}

	//----------------------------------------------------------------------------------------------------
	protected abstract String modelSuffix();
	protected abstract Object createStopIfStatement(final String fullName);
	protected abstract List<UserDefinedVariable> collectVariables();
}
