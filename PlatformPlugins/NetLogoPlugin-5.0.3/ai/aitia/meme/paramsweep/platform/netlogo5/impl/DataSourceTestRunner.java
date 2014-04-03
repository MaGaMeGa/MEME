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

import static ai.aitia.meme.paramsweep.platform.netlogo5.impl.MEMENetLogoParser.MAGIC_STRING;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javassist.CannotCompileException;

import org.nlogo.api.CompilerException;
import org.nlogo.api.LogoException;
import org.xml.sax.SAXException;

import ai.aitia.meme.paramsweep.platform.IPSWInformationProvider;
import ai.aitia.meme.paramsweep.platform.repast.info.GeneratedRecordableInfo;
import ai.aitia.meme.paramsweep.utils.PlatformConstants;
import ai.aitia.meme.paramsweep.utils.UserDefinedVariable;
import ai.aitia.meme.paramsweep.utils.Util;
import ai.aitia.meme.utils.Utils.Pair;

public class DataSourceTestRunner extends AbstractNetLogoModelGenerator {

	//====================================================================================================
	// members
	
	private static final String SUFFIX = "_aitiaGeneratedDummy";
	private static final String FINAL = "__test-datasources";
	private static final String XML_SOURCE = "<experiments>" + 
											 "<experiment name=\"experiment\" repetitions=\"1\" runMetricsEveryStep=\"false\">" +
											 "<setup>setup</setup>" +
											 "<go>go</go>" +
											 "<final>" + FINAL + "</final>" +
											 "<timeLimit steps=\"10\"/>" +
											 "</experiment>" +
											 "</experiments>";
	
	
	private final List<GeneratedRecordableInfo> dataSources;
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public DataSourceTestRunner(final IPSWInformationProvider provider, final List<GeneratedRecordableInfo> dataSources, final String setupCommand) {
		super(provider,setupCommand);
		this.dataSources = dataSources;
	}
	
	//----------------------------------------------------------------------------------------------------
	public Pair<String,Throwable> testRun() throws IOException, LogoException {
		String error = generateModel();
		if (error != null)
			return new Pair<String,Throwable>(error,null);
		final MEMEHeadlessWorkspace workspace = MEMEHeadlessWorkspace.newWorkspace();
		try {
			final File testModel = new File(new File(modelFile).getParentFile(),generatedModelName + ".nlogo");
	        workspace.open(testModel.getAbsolutePath());
	        workspace.runExperiment(XML_SOURCE,2,null,null,'|',null);
	        if (workspace.lastLogoException() != null) {
	        	if (workspace.getLastContext() != null) 
	        		error = workspace.getLastContext().buildRuntimeErrorMessage(workspace.getLastInstruction(),workspace.lastLogoException());
	        	else
	        		error = Util.getLocalizedMessage(workspace.lastLogoException());
	        	return new Pair<String,Throwable>(error,workspace.lastLogoException());
	        }
	        return null;
		} catch (CompilerException e) {
			return new Pair<String,Throwable>(Util.getLocalizedMessage(e),e);
		} catch (SAXException e) {
			return new Pair<String,Throwable>(Util.getLocalizedMessage(e),e);
//		} catch (LogoException e) {
//			return new Pair<String,Throwable>(Util.getLocalizedMessage(e),e);
		} finally {
			try {
				workspace.dispose();
			} catch (final InterruptedException _) {}
			deleteDummyModel(new File(new File(modelFile).getParentFile(),generatedModelName + ".nlogo"));
		}
	}
	
	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	private String generateModel() { 
		String error = null;
		try {
			for (final GeneratedRecordableInfo info : dataSources) 
				generateReporterClass(info);
			final boolean needExtension = !generatedClasses.isEmpty();
			if (needExtension) {
				createReporterClassFiles();
				generateManagerClass();
				createExtension();
				generatedClasses.clear();
			}
			if (needExtension || !nonStatisticReporters.isEmpty()) {
				generatedScripts.clear();
				createDescendantModel(needExtension);
			}
		} catch (final CannotCompileException e) {
			error = "Compile error while generating test script extension.";
			if (e.getReason() != null && e.getReason().trim().length() != 0)
				error += "\nReason: " + e.getReason();
		} catch (final Exception e) {
			error = "Error while generating test script extension or test model.";
			if (e.getLocalizedMessage() != null && Util.getLocalizedMessage(e).trim().length() > 0)
				error += "\nReason: " + Util.getLocalizedMessage(e);
		}
		
		return error;
	}
	
	//----------------------------------------------------------------------------------------------------
	private void createDescendantModel(final boolean needExtension) throws IOException {
		final Pair<PrintWriter,List<UserDefinedVariable>> result = createDescendantModelHeader(needExtension);
		final PrintWriter writer = result.getFirst();
		final List<UserDefinedVariable> userVariables = result.getSecond();
		
		final StringBuilder methodPart = new StringBuilder();
		if (hasTimeSeries) 
			extendSetupMethod(parser.getSetupMethod(),methodPart);
		else 
			methodPart.append(parser.getSetupMethod()).append(DOUBLE_NEWLINE);
		
		// procedures section
		for (final String method : parser.getMethods()) 
			methodPart.append(method).append(DOUBLE_NEWLINE);
		
		for (final GeneratedRecordableInfo info : dataSources) 
			insertReporter(info,methodPart);
		
		for (int i = 0;i < assistantMethods.size();++i)
			insertAssistantMethod(assistantMethods.get(i),i,methodPart);
		
		if (!assistantMethods.isEmpty())
			insertAitiaGeneratedStepMethod(methodPart);
		
		String methodsStr = methodPart.toString();
		if (!assistantMethods.isEmpty())
			methodsStr = methodsStr.replaceAll("(?i)\\stick\\s", " " + AbstractNetLogoModelGenerator.STEP_METHOD + "\n tick \n"); 

		writer.println(methodsStr);
		writer.println();
		
		if (!userVariables.isEmpty())
			generateUserVariableInitialization(userVariables,writer);
	
		insertTestMethod(writer,!userVariables.isEmpty());
	
		writer.println(MAGIC_STRING);
		writer.println();
		
		// other sections
		appendOthers(modelFile,writer);
		
		writer.flush();
		writer.close();
		
		if (hasTimeSeries)
			copyTableExtension();
	}
	
	//----------------------------------------------------------------------------------------------------
	private void insertTestMethod(final PrintWriter writer, final boolean needInitialization) {
		final StringBuilder builder = new StringBuilder();
		builder.append("\nto ").append(FINAL).append("\n ");
		if (needInitialization)
			builder.append(INIT).append("\n ");
		for (final GeneratedRecordableInfo info : dataSources)
			builder.append("show ").append(info.getAccessibleName()).append("\n ");
		builder.append("end");
		writer.println(builder.toString());
	}
	
	//----------------------------------------------------------------------------------------------------
	private void deleteDummyModel(final File model) {
		model.delete();
		final File extensionDir = new File(new File(modelFile).getParentFile(),PlatformConstants.AITIA_GENERATED_SCRIPTS);
		if (extensionDir.exists()) {
			final File[] files = extensionDir.listFiles();
			for (final File f : files)
				f.delete();
			extensionDir.delete();
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void extendSetupMethod(final String origSetup, final StringBuilder methods) {
		final String newLine = " set " + PlatformConstants.AITIA_GENERATED_VARIABLES + " table5:make\n ";
		final String setup = origSetup.replaceAll("(?i)\\send",newLine + "end\n ");
		methods.append(setup).append(DOUBLE_NEWLINE);
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override
	protected String createStopIfStatement(final String methodName) {
		final StringBuilder builder = new StringBuilder();
		builder.append("if ticks >= 10 [\n ");
		builder.append(methodName).append("\n ");
		builder.append("]\n ");
		return builder.toString();
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override
	protected List<UserDefinedVariable> collectVariables() {
		final List<UserDefinedVariable> result = new ArrayList<UserDefinedVariable>();
		for (final GeneratedRecordableInfo info : dataSources) 
			Util.addAllDistinct(result,collectVariables(info));
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override
	protected String modelSuffix() { return SUFFIX; }
}
