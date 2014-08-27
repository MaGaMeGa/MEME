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
import ai.aitia.meme.Logger;
import ai.aitia.meme.paramsweep.batch.output.RecordableInfo;
import ai.aitia.meme.paramsweep.batch.output.RecorderInfo;
import ai.aitia.meme.paramsweep.platform.IPSWInformationProvider;
import ai.aitia.meme.paramsweep.platform.repast.info.GeneratedRecordableInfo;
import ai.aitia.meme.paramsweep.utils.PlatformConstants;
import ai.aitia.meme.paramsweep.utils.UserDefinedVariable;
import ai.aitia.meme.paramsweep.utils.Util;
import ai.aitia.meme.utils.Utils.Pair;

public class NetLogoModelGenerator extends AbstractNetLogoModelGenerator {

	//====================================================================================================
	// members
	
	public static final String SD_SETUP_METHOD = "aitia_generated_system_dynamics_setup_method";

	private static final String	SUFFIX = "_aitiaGenerated";
	
	private final IPSWInformationProvider provider;
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public NetLogoModelGenerator(final IPSWInformationProvider provider, final String setupCommand) {
		super(provider,setupCommand);
		this.provider = provider;
	}
	
	//----------------------------------------------------------------------------------------------------
	public String generateModel() { 
		String error = null;
		try {
			for (final RecorderInfo ri : recorders) {
				final List<RecordableInfo> recordables = ri.getRecordables();
				for (final RecordableInfo info : recordables) {
					if (info instanceof GeneratedRecordableInfo) 
						generateReporterClass((GeneratedRecordableInfo)info);
				}
			}
			final boolean needExtension = !generatedClasses.isEmpty();
			if (needExtension) {
				createReporterClassFiles();
				generateManagerClass();
				createExtension();
				generatedClasses.clear();
			}
			if (needExtension || !nonStatisticReporters.isEmpty() || getParser().isSystemDynamicsModel()) {
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
			Logger.logExceptionCallStack(e);
		}
		return error;
	}
	
	//----------------------------------------------------------------------------------------------------
	public String getGeneratedModelName() { return generatedModelName; }

	//====================================================================================================
	// assistant methods

	//----------------------------------------------------------------------------------------------------
	@Override
	protected File createExtension() throws CannotCompileException, IOException {
		final File extensionDir = super.createExtension();
		
		final List<String> resources = new ArrayList<String>();
		final List<String> old = provider.getAutomaticResources();
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
		
		return extensionDir;
	}
	
	//----------------------------------------------------------------------------------------------------
	private void createDescendantModel(final boolean needExtension) throws IOException {
		final Pair<PrintWriter,List<UserDefinedVariable>> result = createDescendantModelHeader(needExtension);
		final PrintWriter writer = result.getFirst();
		final List<UserDefinedVariable> userVariables = result.getSecond();
		
		if (!userVariables.isEmpty())
			generateUserVariableInitialization(userVariables,writer);
		
		final StringBuilder methodPart = new StringBuilder();
		
		String actualSetup = getParser().getSetupMethod();
		if (getParser().isSystemDynamicsModel()) {
			actualSetup = actualSetup.replaceAll("(?i)\\s" + SETUP_COMMAND + "\\s"," " + SD_SETUP_METHOD + " reset-ticks ");
			actualSetup = actualSetup.replaceAll("(?i)\\sca\\s"," ");
			actualSetup = actualSetup.replaceAll("(?i)\\sclear-all\\s"," ");
			actualSetup = actualSetup.replaceAll("(?i)\\s__clear-all-and-reset-ticks\\s"," ");
			actualSetup = actualSetup.replaceAll("(?i)\\ssystem-dynamics-setup\\s"," ");
			
			// append original setup method
			methodPart.append(getParser().getSetupMethod()).append(DOUBLE_NEWLINE);
		}
		
		if (hasTimeSeries || !userVariables.isEmpty()) 
			extendSetupMethod(actualSetup,methodPart,!userVariables.isEmpty());
		else 
			methodPart.append(actualSetup).append(DOUBLE_NEWLINE);
			
		// procedures section
		for (final String method : getParser().getMethods()) 
			methodPart.append(method).append(DOUBLE_NEWLINE);
		
		for (final RecorderInfo ri : recorders) {
			final List<RecordableInfo> recordables = ri.getRecordables();
			for (final RecordableInfo info : recordables) {
				if (info instanceof GeneratedRecordableInfo) 
					insertReporter((GeneratedRecordableInfo)info,methodPart);
			}
		}
		
		for (int i = 0;i < assistantMethods.size();++i)
			insertAssistantMethod(assistantMethods.get(i),i,methodPart);
		
		if (!assistantMethods.isEmpty())
			insertAitiaGeneratedStepMethod(methodPart);
		
		String methodsStr = methodPart.toString();
		if (!assistantMethods.isEmpty())
			methodsStr = methodsStr.replaceAll("(?i)\\stick\\s", " " + AbstractNetLogoModelGenerator.STEP_METHOD + "\n tick\n "); 

		writer.println(methodsStr);
		writer.println();
		
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
	private void extendSetupMethod(final String origSetup, final StringBuilder methods, final boolean userVariables) {
		String newLine = "";
		if (hasTimeSeries)
			newLine += " set " + PlatformConstants.AITIA_GENERATED_VARIABLES + " table5:make\n ";
		if (userVariables)
			newLine += " " + INIT + "\n ";
		final String setup = origSetup.replaceAll("(?i)\\send",newLine + "end\n ");
		methods.append(setup).append(DOUBLE_NEWLINE);
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override
	protected String createStopIfStatement(final String methodName) {
		final StringBuilder builder = new StringBuilder();
		builder.append("if ").append(exitCondition()).append(" [\n ");
		builder.append(methodName).append("\n ");
		builder.append("]\n ");
		return builder.toString();
	}
	
	//----------------------------------------------------------------------------------------------------
	private String exitCondition() {
		final String condition = provider.getStoppingCondition().trim();
		if (condition.charAt(0) == '{' && condition.endsWith("}")) {
			final String result = condition.substring(1,condition.length() - 1);
			return result.trim();
		} else {
			return "ticks >= " + condition;   
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override protected File copyTableExtension() throws IOException {
		final File extensionDir = super.copyTableExtension();
		
		final List<String> resources = new ArrayList<String>();
		final List<String> old = provider.getAutomaticResources();
		if (old != null)
			resources.addAll(old);
		
		final String absolutePath = new File(extensionDir,"table5.jar").getAbsolutePath();
		if (!resources.contains(absolutePath))
			resources.add(absolutePath);
		provider.setAutomaticResources(resources);
		
		return extensionDir;
	};
	
	//----------------------------------------------------------------------------------------------------
	@Override
	protected List<UserDefinedVariable> collectVariables() {
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
	@Override
	protected String modelSuffix() { return SUFFIX; }
}
