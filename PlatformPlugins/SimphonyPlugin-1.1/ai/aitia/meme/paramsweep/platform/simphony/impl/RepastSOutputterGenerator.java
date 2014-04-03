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
package ai.aitia.meme.paramsweep.platform.simphony.impl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;
import ai.aitia.meme.paramsweep.batch.output.RecordableInfo;
import ai.aitia.meme.paramsweep.batch.output.RecorderInfo;
import ai.aitia.meme.paramsweep.platform.simphony.SimphonyPlatform;
import ai.aitia.meme.paramsweep.platform.simphony.impl.info.RepastSRecordableInfo;
import ai.aitia.meme.paramsweep.utils.PlatformConstants;
import ai.aitia.meme.paramsweep.utils.Util;

public class RepastSOutputterGenerator {

	//===============================================================================
	// members
	
	/** Standard prefix of the names of generated members (except statistics and scripts). */  
	public static final String memberPrefix = "aitiaGenerated";
	/** Standard suffix of the generated model name (it stands before the timestamp). */
	public static final String scenarioSuffix = "BatchScenarioCreatorGenerated";
	public static final String controllerSuffix = "StopControllerActionGenerated";
	
	public static final String packagePrefix = "ai.aitia.generated";
	
	
	/** This object controls bytecode modification with Javassist. */
	private ClassPool pool = null;

	/** The root of the recorders tree. */
	private List<RecorderInfo> recorders = null;
	/** The string representation of the simulation stopping condition. */
	private String stopData = null;
	/** Flag that determines whether the simulation must be stopped in a well-defined
	 *  tick or when a logical condition becomes true.
	 */
	private boolean stopAfterFixInterval = true;
	/** The base directory of the original (and the generated) model. */
	private String directory = null;
	/** The current timestamp in string format. */
	private String timestamp = null;

	/** The object that contains the source code of the generated model. */
	private StringBuilder source = null;
	private StringBuilder source2 = null;

	private int ID = 0;
	
	private final CtClass scenarioInterface;
	private final CtClass stoppingClass;
	
	private String modelName = null;
	private String contextCreatorName = null;
	
	//===============================================================================
	// methods
	
	//-------------------------------------------------------------------------------
	/** Constructor.
	 * @param pool the object that controls bytecode modification with Javassist
	 * @param recorders the root of the recorders tree
	 * @param stopData the string representation of the simulation stopping condition
	 * @param stopAfterFixInterval the information objects of the new parameters in a list
	 * @param directory the base directory of the original (and the generated) model
	 */
	public RepastSOutputterGenerator(String modelName, String contextCreatorName, ClassPool pool, List<RecorderInfo> recorders, String stopData,
						  			 boolean stopAfterFixInterval, String directory) {
		if (pool == null)
			throw new IllegalArgumentException("'pool' is null.");
		if (recorders == null)
			throw new IllegalArgumentException("'recorders' is null.");
		if (directory == null)
			throw new IllegalArgumentException("'directory' is null.");
		if (modelName == null)
			throw new IllegalArgumentException("'modelName' is null.");
		if (contextCreatorName == null)
			throw new IllegalArgumentException("'contextCreatorName' is null.");
		this.modelName = modelName;
		this.contextCreatorName = contextCreatorName;
		this.pool = pool;
		this.recorders = recorders;
		this.stopData = stopData;
		this.stopAfterFixInterval = stopAfterFixInterval;
		this.directory = directory;
		try {
			this.scenarioInterface = this.pool.get("repast.simphony.batch.BatchScenarioCreator");
			this.stoppingClass = this.pool.get("repast.simphony.engine.controller.NullAbstractControllerAction");
		} catch (NotFoundException e) {
			throw new IllegalStateException(e);
		}
		source = new StringBuilder();
		source2 = new StringBuilder();
		timestamp = Util.getTimeStamp();
	}
	
	//--------------------------------------------------------------------------------
	/** Generates the new model.
	 * @return the error message (null if there is no error)
	 */
	public String generateOutputters() {
		if (!stopAfterFixInterval)  
			return "Sorry, the logical stopping condition is not supported at this time.";
		String error = null;
		CtClass scenarioCreator = null;
		CtClass controllerAction = null;
		try {
			File dir = new File(directory + "/ai/aitia/generated");
			dir.mkdirs();
			
			source.append("package " + packagePrefix + ";\n\n");
			source2.append("package " + packagePrefix + ";\n\n");
			importPackages();
			
//			if (stopData != null && stopData.trim().length() != 0) {
//				controllerAction = createStopClass();
//				controllerAction.stopPruning(true);
//				controllerAction.writeFile(directory);
//			}
			scenarioCreator = createScenarioClass(controllerAction);
			scenarioCreator.stopPruning(true);
			scenarioCreator.writeFile(directory);
			return null;
		} catch (CannotCompileException e) { 
			error = "Compile error while generating batch scenario classes.";
			if (e.getReason() != null && e.getReason().trim().length() != 0)
				error += "\nReason: " + e.getReason();
		} catch (Exception e) {
			error = "Error while generating descendant batch scenario classes.";
			if (e.getLocalizedMessage() != null && e.getLocalizedMessage().trim().length() != 0)
				error += "\nReason: " + Util.getLocalizedMessage(e);
		} finally {
			if (controllerAction != null)
				controllerAction.defrost();
			if (scenarioCreator != null)
				scenarioCreator.defrost();
		}
		return error;
	}
	
	//--------------------------------------------------------------------------------
	
	public String getGeneratedModelName() { return packagePrefix + "." + modelName +  scenarioSuffix + "__" + timestamp; }

	/** Returns the source code of the generated class. */
	public String getSource() {	return source.toString(); }
	public String getSource2() { return source2.toString(); }
	/** Returns the name of the generated class. */
	public String getGeneratedBatchScenarioCreatorName() { return modelName +  scenarioSuffix + "__" + timestamp; }
	public String getGeneratedStopControllerAction() { return modelName + controllerSuffix + "__" + timestamp; }
	
	//--------------------------------------------------------------------------------
	/** Writes the source code of the generated class into file.
	 * @return the error message (null if there is no error)
	 */
	public String writeSources() {
		try {
			String dir = directory + File.separator + "ai" + File.separator + "aitia" + File.separator + "generated" + File.separator;
			PrintWriter writer = new PrintWriter(new FileWriter(dir + modelName + scenarioSuffix + "__" + timestamp + ".java"));
			writer.print(prettyPrint(source.toString()));
			writer.flush();
			writer.close();
//			writer = new PrintWriter(new FileWriter(dir + modelName + controllerSuffix + "__" + timestamp + ".java"));
//			writer.print(prettyPrint(source2.toString()));
//			writer.flush();
//			writer.close();
			return null;
		} catch (IOException e) {
			return "Error while writing the outputter source.";
		}
	}
	
	//================================================================================
	// private methods
	
	//--------------------------------------------------------------------------------
	/** Collects the import declarations from the generated scripts and adds
	 *  to the <code>pool</code> (and the source).
	 */
	private void importPackages() {
		List<String> imports = new ArrayList<String>();
		imports.add("repast.score.metadata");
		imports.add("repast.simphony.batch");
		imports.add("repast.simphony.context");
		imports.add("repast.simphony.data.engine");
		imports.add("repast.simphony.data.logging.gather");
		imports.add("repast.simphony.data.logging.gather.aggregate");
		imports.add("repast.simphony.data.logging.outputter.engine");
		imports.add("repast.simphony.dataLoader.engine");
		imports.add("repast.simphony.engine.controller");
		imports.add("repast.simphony.engine.environment");
		imports.add("repast.simphony.engine.schedule");
		imports.add("repast.simphony.parameter");
		imports.add("ai.aitia.generated");
		imports.add("ai.aitia.meme.paramsweep.platform.simphony.impl");
		
		for (String imp : imports) {
			pool.importPackage(imp);
			source.append("import " + imp + ".*;\n");
			source2.append("import " + imp + ".*;\n");
		}
		
		source.append("\n");
		source2.append("\n");
	}
	
	//--------------------------------------------------------------------------------
	/** Creates the method that stops the simulation. 
	 * @param model the generated model
	 * @return the Javassist representation of the method
	 * @throws CannotCompileException if the syntax of the method is wrong
	 */
	@SuppressWarnings("unused")
	private CtClass createStopClass() throws CannotCompileException {
		CtClass stopClass = pool.makeClass(packagePrefix + "." + modelName + controllerSuffix + "__" + timestamp,stoppingClass);
		source2.append("public class " + stopClass.getSimpleName() + " extends " + stoppingClass.getName() + " {\n\n");
		StringBuilder sb = new StringBuilder("public void runInitialize(RunState runState, Context context, Parameters param) {\n");
		String endTick = getFixValue();
		sb.append("RunEnvironment.getInstance().endAt(" + endTick + ");\n");
		sb.append("}\n");
		CtMethod method = CtNewMethod.make(sb.toString(),stopClass);
		stopClass.addMethod(method);
		source2.append(sb.toString());
		source2.append("}");
		return stopClass;
	}
	
	//----------------------------------------------------------------------------------------------------
	private CtClass createScenarioClass(CtClass controllerAction) throws CannotCompileException {
		CtClass scenarioCreator = pool.makeClass(packagePrefix + "." + modelName +  scenarioSuffix + "__" + timestamp);
		scenarioCreator.addInterface(scenarioInterface);
		source.append("public class " + scenarioCreator.getSimpleName() + " implements " + scenarioInterface.getSimpleName() + " {\n\n");
		
		StringBuilder sb = new StringBuilder("public BatchScenario createScenario() {\n");
	    sb.append("try {\n");
	    sb.append("AitiaBatchScenario scenario = new AitiaBatchScenario(\"Context\");\n\n");
//	    sb.append("BatchScenario scenario = new BatchScenario(\"Context\");\n\n");

		for (int i = 0;i < recorders.size();++i) {
			List<RecordableInfo> orig_recordables = recorders.get(i).getRecordables();
			RepastSRecordableInfo[] recordables = new RepastSRecordableInfo[orig_recordables.size()];
			for (int j = 0;j < orig_recordables.size();++j) 
				recordables[j] = (RepastSRecordableInfo) orig_recordables.get(j);
			
			for (int j = 0;j < recordables.length;++j) {
				if (recordables[j].getAccessibleName() != null){ 
					@SuppressWarnings("unused")
					String additionalCP = directory.replaceAll("\\\\","\\\\\\\\") + File.pathSeparator + directory.replaceAll("\\\\","\\\\\\\\") +
										  "-groovy";
					String name = recordables[j].getFieldName();
					if (name == null)
						name = recordables[j].getName();
					if (name == null) {
						String methodName = recordables[j].getAccessMethod();
						if (methodName.endsWith("()"))
							methodName = methodName.substring(0,methodName.length() - 2);
						name = methodName;
					}
					sb.append("scenario.prepareWatchee(\"" + recordables[j].getAgentClass() + "\",\"" + name + 
							  /* "\", \"" + additionalCP + */ "\");\n");
				}
			}
	        sb.append("ScoreUtils.addLoadedAgent(scenario.getContext(), \"" + recordables[0].getAgentPackage() + "\", \"" + 
	        		  recordables[0].getAgentClassSimpleName() + "\");\n");
	        if (i == 0)
	        	sb.append("scenario.addDataLoader(\"Context\", new ClassNameContextBuilder(\"" + contextCreatorName + "\"));\n");
	        
	        sb.append("Class agentClass" + ++ID + " = " + recordables[0].getAgentClass() + ".class;\n\n");
	        sb.append("DataGathererDescriptor descriptor" + ID + " = scenario.addDataGatherer(\"Context\", \"DataSet" + ID + "\", agentClass" + ID + ");\n");
	        sb.append("descriptor" + ID + ".addMapping(RunNumberMapping.RUN_NUMBER_COL_NAME, new RunNumberMapping());\n");
	        sb.append("descriptor" + ID + ".addMapping(TimeDataMapping.TICK_COLUMN, new DefaultTimeDataMapping());\n");
	        for (int j = 0; j < recordables.length;++j)
	        	sb.append(getDataMapping(recordables[j]));
	        
	        sb.append(getGatheringSchedule(recorders.get(i)));
	        
	        sb.append("\nFileOutputterDescriptor fileDescriptor" + ID + " = scenario.addDataLogger(\"Context\",\"DataSet" + ID + "\");\n");
	        sb.append("fileDescriptor" + ID + ".addColumn(RunNumberMapping.RUN_NUMBER_COL_NAME);\n");
	        sb.append("fileDescriptor" + ID + ".addColumn(TimeDataMapping.TICK_COLUMN);\n");
	        for (int j = 0; j < recordables.length;++j)
	        	sb.append(getOutputterMapping(recordables[j]));
	        sb.append("fileDescriptor" + ID + ".setAppendToFile(true);\n");
	        sb.append("fileDescriptor" + ID + ".setInsertTimeToFileName(false);\n");
	        sb.append("fileDescriptor" + ID + ".setIsBatchAction(true);\n");
	        sb.append("fileDescriptor" + ID + ".setWriteHeader(true);\n");
	        sb.append("fileDescriptor" + ID + ".setDelimiter(\"" + recorders.get(i).getDelimiter() + "\");\n");
	        
			sb.append("String recorderPathPrefix = System.getProperty(\"" + SimphonyPlatform.recorderPrefix + "\",\"\");\n");
			sb.append("fileDescriptor" + ID + ".setFileName(recorderPathPrefix + \"" + recorders.get(i).getOutputFile().getPath().replaceAll("\\\\","\\\\\\\\") + "\");\n");
		}
	     
		sb.append("\nscenario.addParameterSetter(new ParameterTreeSweeper());\n");
//	    sb.append("scenario.addMasterControllerAction(new " + controllerAction.getName() + "());\n"); 
	    sb.append("\nreturn scenario;\n");
		sb.append("} catch (IllegalAccessException e) {\n");
	    sb.append("e.printStackTrace();\n");  
	    sb.append("} catch (InstantiationException e) {\n");
	    sb.append("e.printStackTrace();\n");  
	    sb.append("} catch (ClassNotFoundException e) {\n");
		sb.append("e.printStackTrace();\n");  
	    sb.append("} catch (NoSuchMethodException e) {\n");
		sb.append("e.printStackTrace();\n");  
	    sb.append("}\n");
	    sb.append("return null;\n");
	    sb.append("}\n");
	    CtMethod creatorMethod = CtNewMethod.make(sb.toString(),scenarioCreator);
	    scenarioCreator.addMethod(creatorMethod);
	    source.append(sb.toString());
		source.append("}");
		return scenarioCreator;
	}
	
	//----------------------------------------------------------------------------------------------------
	private String getDataMapping(RepastSRecordableInfo ri) throws CannotCompileException {
		StringBuilder code = new StringBuilder("descriptor" + ID);
		String methodName = ri.getAccessMethod();
		if (methodName.endsWith("()"))
			methodName = methodName.substring(0,methodName.length() - 2);
		String name = ri.getName();
		if (name == null) 
			name = methodName;
		String methodMapping = "new MethodMapping(agentClass" + ID + ".getMethod(\"" + methodName + "\",(Class[])null))";
		if (ri.isSimple()) 
			code.append(".addMapping(\"" + name + "\", " + methodMapping + ");\n");
		else {
			code.append(".addPrimaryAggregateMapping(\"" + name + "\", ");
			switch (ri.getAggrType()) {
			case VARIANCE			: code.append("new VarianceMapping(" + methodMapping + "));\n");
								  	  break;
			case STANDARD_DEVIATION : code.append("new StandardDeviationMapping(" + methodMapping + "));\n");
									  break;
			case MAX				: code.append("new MaxMapping(" + methodMapping + "));\n");
									  break;
			case SKEWNESS			: code.append("new SkewnessMapping(" + methodMapping + "));\n");
									  break;
			case MEAN				: code.append("new MeanMapping(" + methodMapping + "));\n");
									  break;
			case KURTOSIS			: code.append("new KurtosisMapping(" + methodMapping + "));\n");
									  break;
			case SUMSQ				: code.append("new SumsqMapping(" + methodMapping + "));\n");
									  break;
			case MIN				: code.append("new MinMapping(" + methodMapping + "));\n");
									  break;
			case COUNT				: code.append("new CountMapping());\n");
									  break;
			case SUM				: code.append("new SumMapping(" + methodMapping + "));\n");
									  break;
			case GEOMETRIC_MEAN		: code.append("new GeometricMeanMapping(" + methodMapping + "));\n");
									  break;
			default					: throw new CannotCompileException("invalid aggregation type");
			}
		}
		return code.toString();
	}
	
	//----------------------------------------------------------------------------------------------------
	private String getGatheringSchedule(RecorderInfo recorder) {
		StringBuilder code = new StringBuilder("descriptor" + ID + ".setScheduleParameters(ScheduleParameters.");
		String[] arg = new String[1];
		String recorderType = parseRecorderType(recorder.getRecordType(),arg);
		if (PlatformConstants.RUN.equals(recorderType))
 			code.append("createAtEnd(ScheduleParameters.LAST_PRIORITY));\n");
		else if (PlatformConstants.ITERATION.equals(recorderType))
			code.append("createRepeating(1.0,1.0,ScheduleParameters.LAST_PRIORITY));\n");
		else if (PlatformConstants.ITERATION_INTERVAL.equals(recorderType))
		 	code.append("createRepeating(1.0,(double)" + arg[0] + ",ScheduleParameters.LAST_PRIORITY));\n");
		return code.toString();
	}
	
	//----------------------------------------------------------------------------------------------------
	private String parseRecorderType(String recordType, String[] arg) {
		if (recordType.equals(PlatformConstants.ITERATION) || recordType.equals(PlatformConstants.RUN))
			return recordType;
		if (recordType.startsWith(PlatformConstants.ITERATION_INTERVAL)) {
			String[] parts = recordType.split(":");
			arg[0] = parts[1];
			return parts[0];
		}
		return null;
	}
	
	//----------------------------------------------------------------------------------------------------
	private String getOutputterMapping(RepastSRecordableInfo ri) {
		StringBuilder code = new StringBuilder("fileDescriptor" + ID + ".addColumn(\"");
		code.append(ri.getName() + "\");\n");
		return code.toString();
	}
	
	//--------------------------------------------------------------------------------
	/** Returns the string representation (including casting to double) of the <code>stopData</code>
	 *  when it contains (directly or undirectly) a tick number.
	 */
	private String getFixValue() {
		try {
			long l = Long.parseLong(stopData);
			return String.valueOf((double)l);
		} catch (NumberFormatException e) {
			return "100";
		}
		// throw new IllegalStateException();
	}
	
	//--------------------------------------------------------------------------------
	/** Formats the source code <code>original</code> and returns the formatted string. */
	private String prettyPrint(String original) {
		String[] lines = original.split("\n");
		StringBuilder res = new StringBuilder();
		int tabNo = 0;
		for (int i = 0;i < lines.length;++i) {
			lines[i] = lines[i].trim();
			if (lines[i].endsWith("}"))
				tabNo--;
			for (int j = 0;j < tabNo;++j)
				res.append("\t");
			if (lines[i].startsWith("public") || (lines[i].startsWith("private") && lines[i].endsWith("{")) ||
				lines[i].startsWith("for") || (lines[i].startsWith("if") && lines[i].endsWith("{")) ||
				lines[i].startsWith("try") || lines[i].startsWith("catch")) {
				tabNo++;
				res.append(lines[i] + "\n");
			} else if (lines[i].startsWith("if")) {
				res.append(lines[i] + "\n");
				i++;
				for (int j = 0;j <= tabNo;++j)
					res.append("\t");
				res.append(lines[i] + "\n");
			} else 
				res.append(lines[i] + "\n");
		}
		return res.toString();
	}
}
