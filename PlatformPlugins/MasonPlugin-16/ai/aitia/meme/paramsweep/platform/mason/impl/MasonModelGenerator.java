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
package ai.aitia.meme.paramsweep.platform.mason.impl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import javassist.NotFoundException;
import ai.aitia.meme.paramsweep.batch.output.RecordableInfo;
import ai.aitia.meme.paramsweep.batch.output.RecorderInfo;
import ai.aitia.meme.paramsweep.batch.param.AbstractParameterInfo;
import ai.aitia.meme.paramsweep.platform.mason.MasonPlatform;
import ai.aitia.meme.paramsweep.platform.repast.info.ExtendedOperatorGeneratedRecordableInfo;
import ai.aitia.meme.paramsweep.platform.repast.info.GeneratedRecordableInfo;
import ai.aitia.meme.paramsweep.platform.repast.info.MultiColumnOperatorGeneratedRecordableInfo;
import ai.aitia.meme.paramsweep.platform.repast.info.OperatorGeneratedRecordableInfo;
import ai.aitia.meme.paramsweep.platform.repast.info.ScriptGeneratedRecordableInfo;
import ai.aitia.meme.paramsweep.platform.repast.info.StatisticGeneratedRecordableInfo;
import ai.aitia.meme.paramsweep.utils.AssistantMethod;
import ai.aitia.meme.paramsweep.utils.AssistantMethod.ScheduleTime;
import ai.aitia.meme.paramsweep.utils.PlatformConstants;
import ai.aitia.meme.paramsweep.utils.UserDefinedVariable;
import ai.aitia.meme.paramsweep.utils.Util;
import ai.aitia.meme.utils.Utils.Pair;

public class MasonModelGenerator {

	//===============================================================================
	// members
	

	/** Standard prefix of the names of generated members (except statistics and scripts). */  
	public static final String memberPrefix = "aitiaGenerated";
	
	private static final String STOP_FLAG = memberPrefix + "StopFlag";

	/** Standard suffix of the generated model name (it stands before the timestamp). */
	public static final String modelSuffix = "BatchGenerated";
	
	/** The numeric types. */
	private static final List<Class<?>> numericTypes = new ArrayList<Class<?>>(12);
	static {
		numericTypes.add(Byte.TYPE);
		numericTypes.add(Byte.class);
		numericTypes.add(Short.TYPE);
		numericTypes.add(Short.class);
		numericTypes.add(Integer.TYPE);
		numericTypes.add(Integer.class);
		numericTypes.add(Long.TYPE);
		numericTypes.add(Long.class);
		numericTypes.add(Float.TYPE);
		numericTypes.add(Float.class);
		numericTypes.add(Double.TYPE);
		numericTypes.add(Double.class);
	}
	
	public static final String ITERATION 			= "ITERATION";
	public static final String ITERATION_INTERVAL 	= "ITERATION_INTERVAL";
	public static final String RUN 					= "RUN";
	public static final String CONDITION 			= "CONDITION";
	public static final String RECORD				= "RECORD";

	/** This object controls bytecode modification with Javassist. */
	private ClassPool pool = null;
	
	/** The Javassist representation of the bytecode of the original model class. */
	private CtClass ancestor = null;
	
	private List<RecorderInfo> recorders = null;
	
	/** The string representation of the simulation stopping condition. */
	private String stopData = null;
	
	/** Flag that determines whether the simulation must be stopped in a well-defined
	 *  tick or when a logical condition becomes true.
	 */
	private boolean stopAfterFixInterval = true;
	
	/** The information objects of the new parameters in a list. */
	private List<AbstractParameterInfo> newParameters = null;
	
	/** The base directory of the original (and the generated) model. */
	private String directory = null;
	
	/** The description of the model. */
	private String description = null;

	
	/** The current timestamp in string format. */
	private String timestamp = null;

	/** The object that contains the source code of the generated model. */
	private StringBuilder source = null;
	
	/** The list of the source codes of the "private" (some of them are public
	 *  because of implementation side effect) methods.
	 */ 
	private List<String> privateMethods = null;
	private List<String> newFields = null;

	/** This data structure maps the variable names and original names of the recorders.
	 *  The variable name doesn't contains spaces or accentuated letters.
	 */ 
	private HashMap<String,String> recorderNames = new HashMap<String,String>();
	
	/** The list of the names of the already generated statistics and scripts. */
	private List<String> generatedScripts = new ArrayList<String>();
	
	/** The Javassist representation of the generated recorder fields. */
	private CtField[] recorderFields = null;
	
	private int ID = 0;
	
	/** The Javassist representation of the bytecode of the ai.aitia.meme.paramsweep.generator.IGeneratedModel interface. */
	private final CtClass generatedInterface;
	
	private final CtClass masonGeneratedInterface;

	private CtField listenersField = null;
	private CtField runField = null;
	private CtField constantParameterNamesField = null;
	private CtField mutableParameterNamesField = null;
	
	private List<Pair<String,ScheduleTime>> assistantMethods = new ArrayList<Pair<String,ScheduleTime>>();
	private CtMethod initializeVariablesMethod = null;

	//===============================================================================
	// methods
	
	//-------------------------------------------------------------------------------
	/** Contructor.
	 * @param pool the object that controls bytecode modification with Javassist
	 * @param ancestor the Javassist representation of the bytecode of the original model class
	 * @param recorders list of recorders
	 * @param stopData the string representation of the simulation stopping condition
	 * @param stopAfterFixInterval the information objects of the new parameters in a list
	 * @param newParameters the information objects of the new parameters in a list
	 * @param directory the base directory of the original (and the generated) model
	 * @param description the description of the model
	 */
	public MasonModelGenerator(final ClassPool pool, final CtClass ancestor, final List<RecorderInfo> recorders, final String stopData,
									final boolean stopAfterFixInterval, final List<AbstractParameterInfo> newParameters, final String directory,
									final String description) {
		if (pool == null)
			throw new IllegalArgumentException("'pool' is null.");
		if (ancestor == null)
			throw new IllegalArgumentException("'ancestor' is null.");
		if (recorders == null)
			throw new IllegalArgumentException("'recorders' is null.");
		if (directory == null)
			throw new IllegalArgumentException("'directory' is null.");
		
		this.pool = pool;
		this.ancestor = ancestor;
		this.recorders = recorders;
		this.stopData = stopData;
		this.stopAfterFixInterval = stopAfterFixInterval;
		this.newParameters = newParameters;
		this.directory = directory;
		this.description = description;
		try {
			this.generatedInterface = this.pool.get("ai.aitia.meme.paramsweep.platform.repast.impl.IGeneratedModel");
			this.masonGeneratedInterface = this.pool.get("ai.aitia.meme.paramsweep.platform.mason.impl.IMasonGeneratedModel");
		} catch (NotFoundException e) {
			throw new IllegalStateException(e);
		}
		source = new StringBuilder();
		privateMethods = new ArrayList<String>();
		newFields = new ArrayList<String>();
		timestamp = Util.getTimeStamp();
	}
	
	//--------------------------------------------------------------------------------
	/** Generates the new model.
	 * @return the error message (null if there is no error)
	 */
	public String generateModel() {
		return generateModel(true);
	}

	//--------------------------------------------------------------------------------
	/** Generates the new model.
	 * @return the error message (null if there is no error)
	 */
	public String generateModel(boolean extendAncestor) {
		String error = null;
		CtClass model = null;
		try {
			if (ancestor.getPackageName() != null) 
				source.append("package " + ancestor.getPackageName() + ";\n\n");
			
			importPackages();
			
			if (extendAncestor){
				model = pool.makeClass(ancestor.getName() + modelSuffix + "__" + timestamp,ancestor);
			} else {
				model = pool.makeClass(ancestor.getName() + modelSuffix + "__" + timestamp);
			}
			
			model.addInterface(generatedInterface);
			model.addInterface(masonGeneratedInterface);
			
			if (description != null && description.trim().length() > 0)
				source.append("/* " + description + " */\n");
			
			source.append("public class " + model.getSimpleName());
			
			if (extendAncestor){
				source.append(" extends " + ancestor.getName());
			}
			
			source.append(" implements " + generatedInterface.getSimpleName() + ", " + masonGeneratedInterface.getSimpleName() + " {\n\n");

			//if (! extendAncestor){
				generateModelMember(model);
			//}
			
			generateStopFlagDeclaration(model);
				
			generateAitiaGeneratedVariablesMapDeclaration(model);
				
			recorderFields = createRecorderFields(model);
			for (final CtField field : recorderFields)
				model.addField(field);
			
			// adding constructor with no parameters
			generateNullaryConstructor(model, extendAncestor);
			
			
			// generating getParams() method
//			generateGetOriginalParamsMethod(model);
			//generating getCurrentStep() method
			generateGetCurrentStepMethod(model);
			//generating getCurrentTime() method
			generateGetCurrentTimeMethod(model);
//			//generating own figureTime method
//			generateOwnFigureTime(model);
			
			// implementing IMasonGeneratedModel interface
			generateAddBatchListenerMethod(model);
			generateRemoveBatchListenerMethod(model);
			generateAddRecorderListenerMethod(model);
			generateRemoveRecorderListenerMethod(model);
			generateGetRunMethod(model);
			generateSetRunMethod(model);
			generateGetConstantParameterNamesMethod(model);
			generateSetConstantParameterNamesMethod(model);
			generateGetMutableParameterNamesMethod(model);
			generateSetMutableParameterNamesMethod(model);
			generateModelInitializationMethod(model);
			
			for (String s : newFields)
				source.append(s);
			source.append("\n");
			newFields.clear();
			
			source.append("\n");
			
			
			generateWriteEnd(model);

			// adding getSeed() method
			generateGetSeed(model);
			
			if (newParameters != null) {
//				overrideGetParamsMethod(model);
				generateGetterSetterMethods(model);
			}
			
			if (stopData != null && stopData.trim().length() > 0) {
				// 'null' or empty string indicates that we generated model class because of new parameters only
				
				final List<UserDefinedVariable> userVariables = collectVariables();
				
				if (userVariables.size() > 0) {
					generateUserVariableFields(userVariables,model);
					generateUserVariableInitialization(userVariables,model);
				}
				
				CtMethod stopMethod = createIsStopMethod(model);
				model.addMethod(stopMethod);
			
				// since the stepEndedMethod references the simulationStop() method (being added two lines later), we have to create a
				// simulationStop() method here, and replace its body later
				CtMethod emptySimulationStopMethod = createEmptySimulationStopMethod(model);
				model.addMethod(emptySimulationStopMethod);
				
				CtMethod stepEndedMethod = overrideStepEndedMethod(model);
				model.addMethod(stepEndedMethod);
				
				CtMethod simulationStopMethod = generateOwnSimulationStopMethod(model);
				model.addMethod(simulationStopMethod);

				emptySimulationStopMethod.setBody(simulationStopMethod, null);
				
				CtMethod startMethod = generateOwnSimulationStartMethod(model);
				model.addMethod(startMethod);
			}
			
			model.stopPruning(true);
			model.writeFile(directory);
			
			for (final String s : newFields)
				source.append(s);
			source.append("\n");
			newFields.clear();
			
			for (final String s : privateMethods) 
				source.append(s + "\n");
			privateMethods.clear();
			source.append("}");
			return null;
		} catch (CannotCompileException e) {
			error = "Compile error while generating descendant model.";
			if (e.getReason() != null && e.getReason().trim().length() != 0)
				error += "\nReason: " + e.getReason();
			e.printStackTrace();
		} catch (Exception e) {
			error = "Error while generating descendant model.";
			if (e.getLocalizedMessage() != null && Util.getLocalizedMessage(e).trim().length() > 0)
				error += "\nReason: " + Util.getLocalizedMessage(e);
		} finally {
			if (model != null)
				model.defrost();
		}
		return error;
	}
	
	private void generateModelMember(final CtClass model) throws CannotCompileException {
		String src = ancestor.getName() + " " + Util.GENERATED_MODEL_MODEL_FIELD_NAME + " = null; \n";
		CtField modelField = CtField.make(src,model);
		model.addField(modelField);
		source.append(src);
		
		src = "public void setModel(" + ancestor.getName() + " model) {\nthis." + Util.GENERATED_MODEL_MODEL_FIELD_NAME + " = model;\n}";
		final CtMethod setModelMethod = CtNewMethod.make(src,model);
		model.addMethod(setModelMethod);
		source.append(src + "\n");

		src = "public Object getModel() {\nreturn this." + Util.GENERATED_MODEL_MODEL_FIELD_NAME + ";\n}";
		final CtMethod getModelMethod = CtNewMethod.make(src,model);
		model.addMethod(getModelMethod);
		source.append(src + "\n");
	}

	private void generateStopFlagDeclaration(final CtClass model) throws CannotCompileException {
		String src = "boolean " + STOP_FLAG + " = false; \n";
		CtField modelField = CtField.make(src,model);
		model.addField(modelField);
		source.append(src);
	}

	private CtMethod createEmptySimulationStopMethod(CtClass model) throws CannotCompileException {
		final StringBuilder b = new StringBuilder("public void simulationStop() {\n");
		b.append("}\n");
		final CtMethod simulationStopMethod = CtNewMethod.make(b.toString(),model);
		source.append(b.toString() + "\n");
		return simulationStopMethod;
	}

	private void generateModelInitializationMethod(CtClass model) throws CannotCompileException {
		final StringBuilder sb = new StringBuilder("public void modelInitialization() {\n");
		sb.append(Util.GENERATED_MODEL_MODEL_FIELD_NAME + ".nameThread();\n");
		sb.append(Util.GENERATED_MODEL_MODEL_FIELD_NAME + ".start();\n");
		sb.append("return;\n");
		sb.append("}\n");
		source.append(sb.toString()+"\n");
		CtMethod modelInitMethod = CtNewMethod.make(sb.toString(), model);
		model.addMethod(modelInitMethod);
	}

//	private void generateOwnFigureTime(CtClass model) throws CannotCompileException {
//		StringBuilder sb = new StringBuilder("		static long "+memberPrefix+"FigureTime(long time) {\n" + 
//				"			long n = 1L;\n" + 
//				"			while(true) {\n" + 
//				"				if (n >= time) return n;\n" + 
//				"				if ((n*10L)/4L >= time) return (n*10L)/4L;\n" + 
//				"				if ((n*10L)/2L >= time) return (n*10L)/2L;\n" + 
//				"				n = n*10L;\n" + 
//				"			}\n" + 
//				"		}\n");
//		source.append(sb.toString()+"\n");
//		CtMethod onwFigureTimeMethod = CtNewMethod.make(sb.toString(), model);
//		model.addMethod(onwFigureTimeMethod);
//	}

	private void generateGetCurrentStepMethod(CtClass model) throws CannotCompileException {
		StringBuilder sb = new StringBuilder("public double getCurrentStep() {\n");
		sb.append("return (double) " + Util.GENERATED_MODEL_MODEL_FIELD_NAME + ".schedule.getSteps();\n");
		sb.append("}\n");
		source.append(sb.toString());
		CtMethod getCurrentStepMethod = CtNewMethod.make(sb.toString(), model);
		model.addMethod(getCurrentStepMethod);
	}

	private void generateGetCurrentTimeMethod(CtClass model) throws CannotCompileException {
		StringBuilder sb = new StringBuilder("public double getCurrentTime() {\n");
		sb.append("return " + Util.GENERATED_MODEL_MODEL_FIELD_NAME + ".schedule.getTime();\n");
		sb.append("}\n");
		source.append(sb.toString());
		CtMethod getCurrentTimeMethod = CtNewMethod.make(sb.toString(), model);
		model.addMethod(getCurrentTimeMethod);
	}

	// we don't need a method that returns the parameters, plus the list returned by this generated method seems to be wrong!
//	private void generateGetOriginalParamsMethod(CtClass model) throws CannotCompileException {
//		ArrayList<String> parameterNames = new ArrayList<String>();
//		CtMethod[] allMethods = model.getMethods();
//		for (int i = 0; i < allMethods.length; i++) {
//			try {
//				if (	allMethods[i].getName().startsWith("set") && 
//						allMethods[i].getParameterTypes().length == 1 && 
//						Util.isAcceptableSimpleType(allMethods[i].getParameterTypes()[0].getName()) &&
//						allMethods[i].getReturnType().equals(Void.TYPE)) {
//					//this is a setter method, look for a getter:
//					CtMethod getter = searchGetter(allMethods, allMethods[i].getName().substring(3), allMethods[i].getParameterTypes()[0]);
//					if (getter != null) {
//						//we have getter as well, this is a parameter, add it to the list:
//						parameterNames.add(allMethods[i].getName().substring(3));
//					}
//				}
//			} catch (NotFoundException e) {
//				//not inserting not found parameters...
//			}
//		}
//
//
//		
//		Collections.sort(parameterNames);
//		parameterNames.add("seed");
//		StringBuilder sb = new StringBuilder("public String[] getOriginalParams() {\n");
//		sb.append("return new String[] {\n");
//		for (int i = 0; i < parameterNames.size()-1; i++) {
//			sb.append("\"");
//			sb.append(parameterNames.get(i));
//			sb.append("\",");
//		}
//		if (parameterNames.size() > 0) {
//			sb.append("\"");
//			sb.append(parameterNames.get(parameterNames.size()-1));
//			sb.append("\"");
//		}
//		sb.append("};\n");
//		sb.append("}\n");
//		source.append(sb.toString());
//		CtMethod getParamsMethod = CtNewMethod.make(sb.toString(), model);
//		model.addMethod(getParamsMethod);
//	}

	private CtMethod searchGetter(CtMethod[] methodList, String getterNameEnding, CtClass getterReturnType) throws NotFoundException {
		for (int i = 0; i < methodList.length; i++) {
			if (	(
					methodList[i].getName().equals("get"+getterNameEnding) ||
					methodList[i].getName().equals("is"+getterNameEnding)
					) && 
					methodList[i].getParameterTypes().length == 0 && 
					methodList[i].getReturnType().equals(getterReturnType)) {
				return methodList[i];
			}
		}
		return null;
	}

	private void generateGetSeed(CtClass model) throws CannotCompileException {
		StringBuilder sb = new StringBuilder("public long getSeed() {\n");
		sb.append("return " + Util.GENERATED_MODEL_MODEL_FIELD_NAME + ".seed();\n");
		sb.append("}\n");
		source.append(sb);
		CtMethod getSeedMethod = CtNewMethod.make(sb.toString(), model);
		model.addMethod(getSeedMethod);
	}

	private void generateNullaryConstructor(final CtClass model, final boolean extendsAncestor) throws CannotCompileException {
		StringBuilder sb = new StringBuilder("public "+model.getSimpleName()+"() { \n");
		if (extendsAncestor){
			sb.append("super(1L);\n");
			sb.append("this." + Util.GENERATED_MODEL_MODEL_FIELD_NAME + " = this;\n");
		} // otherwise someone must call the setMethod() method!
		sb.append(generateRecorders(model));
		sb.append("}\n");
		source.append(sb);
		CtConstructor nullaryConstructor = CtNewConstructor.make(sb.toString(), model);
		model.addConstructor(nullaryConstructor);
	}

	//--------------------------------------------------------------------------------
	/** Returns the source code of the generated class. */
	public String getSource() {	return source.toString(); }
	/** Returns the name of the generated class. */
	public String getGeneratedModelName() { return ancestor.getName() + modelSuffix + "__" + timestamp; }
	
	//--------------------------------------------------------------------------------
	/** Writes the source code of the generated class into file.
	 * @return the error message (null if there is no error)
	 */
	public String writeSource() {
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new FileWriter(directory + File.separator + ancestor.getName().replace('.',File.separatorChar) +
													modelSuffix + "__" + timestamp + ".java"));
			writer.print(prettyPrint(source.toString()));
			return null;
		} catch (IOException e) {
			return "Error while writing the model source: "+e.getMessage();
		} finally {
			if (writer != null) {
				writer.flush();
				writer.close();
			}
		}
	}
	
	//================================================================================
	// private methods
	
	//--------------------------------------------------------------------------------
	/** Collects the import declarations from the generated scripts and adds
	 *  to the <code>pool</code> (and the source).
	 */
	private void importPackages() {
		final List<String> imports = new ArrayList<String>();
		imports.add("java.util");
		imports.add("ai.aitia.meme.paramsweep.platform.repast.impl");
		imports.add("ai.aitia.meme.paramsweep.platform.mason");
		imports.add("ai.aitia.meme.paramsweep.platform.mason.impl");
		imports.add("ai.aitia.meme.paramsweep.platform.custom.impl");
		imports.add("ai.aitia.meme.paramsweep.batch");
		imports.add("ai.aitia.meme.paramsweep.generator");
		imports.add("ai.aitia.meme.paramsweep.utils");
		imports.add("sim.engine");
		
		for (int i = 0;i < recorders.size();++i) {
			final List<RecordableInfo> rec = recorders.get(i).getRecordables();
			for (int j = 0;j < rec.size();++j) {
				final RecordableInfo recordableInfo = rec.get(j);
				if (recordableInfo instanceof GeneratedRecordableInfo)
					importPackagesImpl((GeneratedRecordableInfo)recordableInfo,imports);
			}
		}

		for (final String imp : imports) {
			pool.importPackage(imp);
			source.append("import " + imp + ".*;\n");
		}
		source.append("\n");
	}
	
	//--------------------------------------------------------------------------------
	/** Collects the import declarations from the script specified by <code>gmi</code>
	 *  to the output parameter <code>imports</code>.
	 */
	private void importPackagesImpl(GeneratedRecordableInfo gri, List<String> imports) {
		if (gri instanceof StatisticGeneratedRecordableInfo) {
			final StatisticGeneratedRecordableInfo sgri = (StatisticGeneratedRecordableInfo) gri;
			for (final GeneratedRecordableInfo _gri : sgri.getReferences())
				importPackagesImpl(_gri,imports);
		} else if (gri instanceof OperatorGeneratedRecordableInfo) {
			final OperatorGeneratedRecordableInfo ogri = (OperatorGeneratedRecordableInfo) gri;
			for (final GeneratedRecordableInfo _gri : ogri.getReferences())
				importPackagesImpl(_gri,imports);
		} else if (gri instanceof ScriptGeneratedRecordableInfo) {
			final ScriptGeneratedRecordableInfo sgri = (ScriptGeneratedRecordableInfo) gri;
			for (final GeneratedRecordableInfo _gri : sgri.getReferences())
				importPackagesImpl(_gri,imports);
			final List<String> newImports = sgri.getImports();
			for (final String imp : newImports) {
				final int index = imp.lastIndexOf('.');
				if (index == -1) continue;
				final String candidate = imp.substring(0,index);
				if ("java.lang".equals(candidate)) continue;
				else if (!imports.contains(candidate))
					imports.add(candidate);
			}
		}
	}
	
	//--------------------------------------------------------------------------------
	/** Creates the method that stops the simulation. 
	 * @param model the generated model
	 * @return the Javassist representation of the method
	 * @throws CannotCompileException if the syntax of the method is wrong
	 */
	private CtMethod createIsStopMethod(final CtClass model) throws CannotCompileException {
		final StringBuilder sb = new StringBuilder("public boolean " + memberPrefix + "_isStop() {\n");
		if (!stopAfterFixInterval)
			sb.append("return ((boolean) (" + stopData + "))\n");
		else
			sb.append("return getCurrentTime() >= ").append(getFixValue()).append("\n");
		sb.append(" || " + STOP_FLAG + ";");
		sb.append("}\n");
		final CtMethod stopIsMethod = CtNewMethod.make(sb.toString(),model);
		source.append(sb.toString() + "\n");
		return stopIsMethod;
	}
	
	//--------------------------------------------------------------------------------
	/** Creates the recorder fields (DataRecorder objects) from the recorders tree.
	 * @param model the generated model
	 * @return the array of the Javassist representation of the fields
  	 * @throws CannotCompileException if the syntax of the field declarations is wrong
	 */ 
	private CtField[] createRecorderFields(final CtClass model) throws CannotCompileException {
		final CtField[] result = new CtField[recorders.size()];
		for (int i = 0;i < recorders.size();++i) {
			final String origName = recorders.get(i).getName();
			final String variableName = createVariableName(origName);
			recorderNames.put(variableName,origName);
			final String src = "private MasonRecorder " + variableName + " = null; ";
			result[i] = CtField.make(src,model);
			source.append(src + "\n");
		}
		return result;
	}
	
	//--------------------------------------------------------------------------------
	/** Creates and returns a variable name from <code>original</code>. It replaces
	 *  all spaces and accentuated letter. 
	 */
	private String createVariableName(final String original) {
		String vName = new String(original);
		vName = vName.replace(' ','_');
		vName = vName.replace('á','a');
		vName = vName.replace('é','e');
		vName = vName.replace('í','i');
		vName = vName.replace('ó','o');
		vName = vName.replace('ö','o');
		vName = vName.replace('ő','o');
		vName = vName.replace('ú','u');
		vName = vName.replace('ü','u');
		vName = vName.replace('ű','u');
		vName = vName.replace('Á','A');
		vName = vName.replace('É','E');
		vName = vName.replace('Í','I');
		vName = vName.replace('Ó','O');
		vName = vName.replace('Ö','O');
		vName = vName.replace('Ő','O');
		vName = vName.replace('Ú','U');
		vName = vName.replace('Ü','U');
		vName = vName.replace('Ű','U');
		
		for (final char c : vName.toCharArray()) {
			if (!Character.isJavaIdentifierPart(c))
				vName = vName.replace(c,'_');
		}
		
		vName = memberPrefix + "_" + vName;
		int number = 1;
		String result = new String(vName);
		while (recorderNames.get(result) != null) {
			result = vName + "_" + String.valueOf(number++);
		}
		return result;
	}
	
	
	//--------------------------------------------------------------------------------
	// we this is only called from MEME, therefore we don't need to use the model field to access members 
	private CtMethod generateOwnSimulationStartMethod(final CtClass model) throws CannotCompileException {
		final StringBuilder sb = new StringBuilder("public void simulationStart() {\n");
		if (initializeVariablesMethod != null)
			sb.append(initializeVariablesMethod.getName() + "();\n");
		if (needInitializationForTimeSeries())
			sb.append(generateAitiaGeneratedVariablesMapInitializer(model));
		//sb.append(generateRecorders(model));
//		sb.append("	long time = -1L;\n");
//		sb.append("	long steps = 0L;\n");
//		sb.append("	long clock = 0L;\n");
//		sb.append("	long oldClock = System.currentTimeMillis();\n");
//		sb.append("	long firstSteps = " + Util.GENERATED_MODEL_MODEL_FIELD_NAME + ".schedule.getSteps();\n");
		sb.append(" " + Util.GENERATED_MODEL_MODEL_FIELD_NAME + ".schedule.scheduleRepeating(new MasonStepEnded(this), Integer.MAX_VALUE, 1.);\n");
		sb.append("\n");
		sb.append("	// do the loop\n");
		sb.append("	while(! " + memberPrefix + "_isStop()) {\n");
		sb.append("		if (!" + Util.GENERATED_MODEL_MODEL_FIELD_NAME + ".schedule.step(" + Util.GENERATED_MODEL_MODEL_FIELD_NAME + ")) {\n");
		sb.append("			break;\n");
		sb.append("		}\n");
//		sb.append("		steps = " + Util.GENERATED_MODEL_MODEL_FIELD_NAME + ".schedule.getSteps();\n");
//		sb.append("		if (time < 0L) { // don't know how long to make the time yet\n");
//		sb.append("			if (System.currentTimeMillis() - oldClock > 1000L)  // time to set the time\n");
//		sb.append("			{\n");
//		sb.append("				time = "+memberPrefix+"FigureTime(steps - firstSteps);\n");
//		sb.append("			}\n");
//		sb.append("		}\n");
//		sb.append("		if (time > 0L && steps % time == 0L) {\n");
//		sb.append("			clock = System.currentTimeMillis();\n");
//		sb.append("			firstSteps = steps;\n");
//		sb.append("			oldClock = clock;\n");
//		sb.append("		}\n");
//		sb.append("		stepEnded();");
		sb.append("	}\n");

		for (final RecorderInfo rec : recorders)
		generateRecordingCodeAtTheEnd(rec,sb,model);

		sb.append("Thread.currentThread().setName(\"finished simulation: \" + " + Util.GENERATED_MODEL_MODEL_FIELD_NAME + ".getClass().getName());\n");
		sb.append("	" + Util.GENERATED_MODEL_MODEL_FIELD_NAME + ".finish();\n");
		sb.append("return;\n");
		sb.append("}\n");
		final CtMethod simulationStartMethod = CtNewMethod.make(sb.toString(),model);
		source.append(sb.toString() + "\n");
		return simulationStartMethod;
	}

	//--------------------------------------------------------------------------------
	/** Creates the source code belongs to the recorder initialization (creation and
	 *  registration of the recordable elements).
	 * @param model the generated model
	 * @return the source code
	 * @throws CannotCompileException if the syntax of the related generated methods are wrong
	 */
	private String generateRecorders(final CtClass model) throws CannotCompileException {
		final StringBuilder sb = new StringBuilder();
		sb.append("String recorderPathPrefix = System.getProperty(\"" + MasonPlatform.recorderPrefix + "\",\"\");\n");
		for (final RecorderInfo rec : recorders) {
			final String variable = findVariable(rec.getName());
			if (variable == null)
				throw new IllegalStateException();
			sb.append(variable + " = new MasonRecorder(recorderPathPrefix + \"" + rec.getOutputFile().getPath().replaceAll("\\\\","\\\\\\\\") + "\",this,\"" +
					  rec.getDelimiter() + "\");\n");
			final List<RecordableInfo> recordables = rec.getRecordables();
			for (final RecordableInfo ri : recordables) 
				sb.append(generateRecordable(variable,ri,model));
		}
		return sb.toString();
	}
	
	//--------------------------------------------------------------------------------
	/** Returns the variable name belongs to the original name of a recorder.
	 * @param original the original name of the recorder
	 * @return the variable name of the recorder (or null)
	 */
	private String findVariable(final String original) {
		for (final Entry<String,String> e : recorderNames.entrySet()) {
			if (e.getValue().equals(original))
				return e.getKey();
		}
		return null;
	}
	
	private String generateCollectionLengthMethod(final RecordableInfo re, final CtClass model) throws CannotCompileException{
		final String methodName = memberPrefix + "_privateMethod" + String.valueOf(ID++);
		String src = "public int " + methodName + "(){\n";
		String collectionLength = ((MultiColumnOperatorGeneratedRecordableInfo)re).getCollectionLength();
		try {
			Integer.parseInt(collectionLength);
			src += "return " + collectionLength + ";\n";
		} catch (NumberFormatException e){
			String accessor = Util.getMethodAccessorInGeneratedModel(collectionLength);
			src += "return " + accessor + ";\n";
		}
		src += "}\n";
		final CtMethod m = CtNewMethod.make(src,model);
		model.addMethod(m);
		privateMethods.add(src);
		return methodName;
	}
	
	//--------------------------------------------------------------------------------
	/** Creates the source code belongs to the recorder initialization (the
	 *  registration of the recordable elements).
	 * @param variable the variable name of the recorder
	 * @param re the recordable element
	 * @param model the generated model
	 * @return the source code
	 * @throws CannotCompileException if the syntax of the related generated methods are wrong
	 */
	private String generateRecordable(final String variable, final RecordableInfo re, final CtClass model) throws CannotCompileException {
		final StringBuilder sb = new StringBuilder(variable);
		//escaping "-s in re.getName():
		String reGetNameEscaped = re.getName().replace("\"", "\\\"");
		if (re.getName().endsWith(Util.GENERATED_MODEL_MULTICOLUMN_POSTFIX)){
			sb.append(".addMultiColumnSource(\"" + reGetNameEscaped + "\",\"" + generateRecorderMethod(variable, re, model) + "\",\"" + generateCollectionLengthMethod((MultiColumnOperatorGeneratedRecordableInfo)re, model) + "\");\n");
		} else {
			sb.append(".addSource(\"" + reGetNameEscaped + "\",\"" + generateRecorderMethod(variable, re,model) + "\");\n");
		}
		return sb.toString();
	}
	
	//--------------------------------------------------------------------------------
	private String generateRecorderMethod(final String recorderVariableName, final RecordableInfo re, final CtClass model) throws CannotCompileException {
		String accessor = re.getAccessibleName();
		if (re instanceof StatisticGeneratedRecordableInfo) {
			final StatisticGeneratedRecordableInfo sgri = (StatisticGeneratedRecordableInfo) re;
			generateStatistic(recorderVariableName, sgri,model);
		} else if (re instanceof OperatorGeneratedRecordableInfo) {
			final OperatorGeneratedRecordableInfo ogri = (OperatorGeneratedRecordableInfo) re;
			generateOperator(recorderVariableName, ogri,model);
		} else if (re instanceof ScriptGeneratedRecordableInfo) {
			final ScriptGeneratedRecordableInfo sgri = (ScriptGeneratedRecordableInfo) re;
			generateScript(recorderVariableName, sgri,model);
		} else {
			accessor = Util.getMethodAccessorInGeneratedModel(re.getAccessibleName());
		}
		final String methodName = memberPrefix + "_privateMethod" + String.valueOf(ID++);
		String src = "public ";
		src += re.getType().getSimpleName() + " " + methodName + "() {\n";
		src += "return " + accessor + ";\n";
		src += "}\n";
		final CtMethod m = CtNewMethod.make(src,model);
		model.addMethod(m);
		privateMethods.add(src);
		return methodName;
	}
	
	//--------------------------------------------------------------------------------
	/** Creates the method of the statistic instance specified by <code>mi</code>. It also
	 *  creates all referenced generated statistic instance or script, too.
	 * @param mi the information object of the statistic instance
	 * @param model the generated model
	 * @throws CannotCompileException if the syntax of the method (or other related generated method) is wrong
	 */
	private void generateStatistic(final String recorderVariableName, final StatisticGeneratedRecordableInfo re, final CtClass model) throws CannotCompileException {
		if (generatedScripts.contains(re.getAccessibleName())) return;
		final List<GeneratedRecordableInfo> references = re.getReferences();
		for (final GeneratedRecordableInfo gri : references) {
			if (gri instanceof StatisticGeneratedRecordableInfo) 
				generateStatistic(recorderVariableName, (StatisticGeneratedRecordableInfo)gri,model);
			else if (gri instanceof OperatorGeneratedRecordableInfo)
				generateOperator(recorderVariableName, (OperatorGeneratedRecordableInfo)gri,model);
			else 
				generateScript(recorderVariableName, (ScriptGeneratedRecordableInfo)gri,model);
		}
		String src = "private " + re.getType().getCanonicalName() + " " + re.getAccessibleName() + " {\n";
		src += re.getSource() + "}\n";
		final CtMethod m = CtNewMethod.make(src,model);
		model.addMethod(m);
		privateMethods.add(src);
		generatedScripts.add(re.getAccessibleName());
	}
	
	//----------------------------------------------------------------------------------------------------
	private void generateOperator(final String recorderVariableName, final OperatorGeneratedRecordableInfo re, final CtClass model) throws CannotCompileException {
		if (generatedScripts.contains(re.getAccessibleName())) return;
		final List<GeneratedRecordableInfo> references = re.getReferences();
		for (final GeneratedRecordableInfo gri : references) {
			if (gri instanceof OperatorGeneratedRecordableInfo)
				generateOperator(recorderVariableName, (OperatorGeneratedRecordableInfo)gri,model);
			else if (gri instanceof StatisticGeneratedRecordableInfo)
				generateStatistic(recorderVariableName, (StatisticGeneratedRecordableInfo)gri,model);
			else
				generateScript(recorderVariableName, (ScriptGeneratedRecordableInfo)gri,model);
		}
		if (re instanceof ExtendedOperatorGeneratedRecordableInfo) {
			final ExtendedOperatorGeneratedRecordableInfo eogri = (ExtendedOperatorGeneratedRecordableInfo) re;
			for (int i = 0;i < eogri.getAssistantMethods().size();++i)
				generateAssistantMethod(eogri.getAccessibleName(),i,eogri.getAssistantMethods().get(i),model);
		}
		String src = "private " + re.getType().getCanonicalName() + " " + re.getAccessibleName() + " {\n";
		src += "final MasonRecorder " + Util.GENERATED_MODEL_RECORDER_VARIABLE_NAME + " = " + recorderVariableName + ";\n";
		src += re.getSource() + "}\n";
		final CtMethod m = CtNewMethod.make(src,model);
		model.addMethod(m);
		privateMethods.add(src);
		generatedScripts.add(re.getAccessibleName());
	}
	
	//--------------------------------------------------------------------------------
	/** Creates the method of the script specified by <code>mi</code>. It also
	 *  creates all referenced generated statistic instance or script, too.
	 * @param mi the information object of the script
	 * @param model the generated model
	 * @throws CannotCompileException if the syntax of the method (or other related generated method) is wrong
	 */
	private void generateScript(final String recorderVariableName, final ScriptGeneratedRecordableInfo re, final CtClass model) throws CannotCompileException {
		if (generatedScripts.contains(re.getAccessibleName())) return;
		final List<GeneratedRecordableInfo> references = re.getReferences();
		for (final GeneratedRecordableInfo gri : references) {
			if (gri instanceof ScriptGeneratedRecordableInfo) 
				generateScript(recorderVariableName, (ScriptGeneratedRecordableInfo)gri,model);
			else if (gri instanceof StatisticGeneratedRecordableInfo) 
				generateStatistic(recorderVariableName, (StatisticGeneratedRecordableInfo)gri,model);
			else
				generateOperator(recorderVariableName, (OperatorGeneratedRecordableInfo)gri,model);
		}
		String src = "private " + re.getType().getCanonicalName() + " " + re.getAccessibleName() + " {\n";
		src += re.getSource() + "}\n";
		final CtMethod m = CtNewMethod.make(src,model);
		model.addMethod(m);
		privateMethods.add(src);
		generatedScripts.add(re.getAccessibleName());
	}
	
	//----------------------------------------------------------------------------------------------------
	private CtMethod overrideStepEndedMethod(final CtClass model) throws CannotCompileException {
		final StringBuilder b = new StringBuilder("public void stepEnded() {\n");
//		b.append("boolean finish = ").append(memberPrefix).append("_isStop();\n");
		b.append(scheduleAssistantMethods());
		for (final RecorderInfo rec : recorders) 
			generateRecordingCode(rec,b,model);
		final String event = "ai.aitia.meme.paramsweep.batch.BatchEvent"; 
		b.append(event).append(" event = new ").append(event).append("(this,").append(event).append(".EventType.STEP_ENDED,this.getCurrentTime());\n");
		b.append("for (int i = 0;i < ").append(listenersField.getName()).append(".size();++i) {\n");
		b.append("((IBatchListener)").append(listenersField.getName()).append(".get(i)).timeProgressed(event);\n");
		b.append("}\n");
//		b.append("if (finish) {\n");
//		b.append("simulationStop();\n");
//		b.append("}\n}\n");
		b.append("}\n");
		final CtMethod stepMethod = CtNewMethod.make(b.toString(),model);
		source.append(b.toString() + "\n");
		return stepMethod;
	}
	
	//----------------------------------------------------------------------------------------------------
	private void generateRecordingCode(final RecorderInfo rec, final StringBuilder b, final CtClass model) throws CannotCompileException {
		// recording code
		final String variable = findVariable(rec.getName());
		final String[] arg = new String[1];
		final String recordType = parseRecorderType(rec.getRecordType(),arg);
		b.append("boolean recordFor").append(variable).append(" = false;\n");
		if (ITERATION.equals(recordType)) {
			b.append(variable).append(".record();\n");
			b.append("recordFor").append(variable).append(" = true;\n");
		} else if (ITERATION_INTERVAL.equals(recordType)) {
			final String src = "private long " + memberPrefix + "_RecordCounterFor_" + variable + " = 1l;";
			final CtField field = CtField.make(src,model);
			newFields.add(src + "\n");
			model.addField(field);
			
			b.append("if (").append(field.getName()).append(" == ").append(arg[0]).append(") {\n");
			b.append(variable).append(".record();\n");
			b.append("recordFor").append(variable).append(" = true;\n");
			b.append(field.getName()).append(" = 1l;\n");
			b.append("} else {\n");
			b.append(field.getName()).append("++;\n");
			b.append("}\n");
		} else if (RUN.equals(recordType)) {
//			b.append("if (finish) {\n");
//			b.append(variable).append(".record();\n");
//			b.append("record = true;\n");
//			b.append("}\n");
			// generateRecordingCodeAtTheEnd() handles this case
		} else if (CONDITION.equals(recordType)) {
			b.append("if ((boolean) (").append(arg[0]).append(")) {\n");
			b.append(variable).append(".record();\n");
			b.append("recordFor").append(variable).append(" = true;\n");
			b.append("}\n");
		} else 
			throw new CannotCompileException("invalid recordType: " + rec.getRecordType());
		b.append("\n");
		
		// writing to file code
		final long[] writeArg = new long[1];
		final String writeType = parseWriteType(rec.getWriteType(),writeArg);
		if (RUN.equals(writeType)) {
//			b.append("if (finish) {\n");
//			b.append(variable).append(".writeToFile();\n");
//			b.append("}\n");
			// generateRecordingCodeAtTheEnd() handles this case
		} else if (RECORD.equals(writeType)) {
			b.append("if (recordFor").append(variable).append(") {\n");
			b.append(variable).append(".writeToFile();\n");
			b.append("}\n");
		} else if (ITERATION_INTERVAL.equals(writeType)) {
			final String src = "private long " + memberPrefix + "_WriteCounterFor_" + variable + " = 1l;";
			final CtField field = CtField.make(src,model);
			newFields.add(src + "\n");
			model.addField(field);
			
			b.append("if (").append(field.getName()).append(" == ").append(writeArg[0]).append(") {\n");
			b.append(variable).append(".writeToFile();\n");
			b.append(field.getName()).append(" = 1l;\n");
			b.append("} else {\n");
			b.append(field.getName()).append("++;\n");
			b.append("}\n");
		} else
			throw new CannotCompileException("invalid writeType: " + rec.getWriteType());
		b.append("\n");
	}
	
	//----------------------------------------------------------------------------------------------------
	private CtMethod generateOwnSimulationStopMethod(final CtClass model) throws CannotCompileException {
		final StringBuilder b = new StringBuilder("public void memeSimulationStop() {\n");
//		for (final RecorderInfo rec : recorders)
//			generateRecordingCodeAtTheEnd(rec,b,model);
//		b.append("    	Steppable killer = new MasonKiller();\n");
////		//Integer.MAX_VALUE: to schedule kill() as the (hopefully) last action in the next step
//		b.append("		" + Util.GENERATED_MODEL_MODEL_FIELD_NAME + ".schedule.scheduleOnce(killer, Integer.MAX_VALUE);\n");
//		b.append("\n");
		b.append(STOP_FLAG + " = true;\n");
		b.append("}\n");
		final CtMethod simulationStopMethod = CtNewMethod.make(b.toString(),model);
		source.append(b.toString() + "\n");
		return simulationStopMethod;
	}
	
	//----------------------------------------------------------------------------------------------------
	private void generateRecordingCodeAtTheEnd(final RecorderInfo rec, final StringBuilder b, final CtClass model) throws CannotCompileException {
		// recording code
		final String variable = findVariable(rec.getName());
		final String[] arg = new String[1];
		final String recordType = parseRecorderType(rec.getRecordType(),arg);
		b.append("boolean recordFor").append(variable).append(" = false;\n");
		if (ITERATION_INTERVAL.equals(recordType)) {
			try {
				final CtField field = model.getField(memberPrefix + "_RecordCounterFor_" + variable);
				b.append("if (").append(field.getName()).append(" != 1l) {\n");
				b.append(variable).append(".record();\n");
				b.append("recordFor").append(variable).append(" = true;\n");
				b.append("}\n");
			} catch (final NotFoundException e) {
				throw new CannotCompileException(e);
			}
		} else if (RUN.equals(recordType)) {
			b.append(variable).append(".record();\n");
			b.append("recordFor").append(variable).append(" = true;\n");
		} 
		b.append("\n");
		
		// writing to file code
		final long[] writeArg = new long[1];
		final String writeType = parseWriteType(rec.getWriteType(),writeArg);
		if (RUN.equals(writeType)) 
			b.append(variable).append(".writeToFile();\n");
		else if (RECORD.equals(writeType)) {
			b.append("if (recordFor").append(variable).append(") {\n");
			b.append(variable).append(".writeToFile();\n");
			b.append("}\n");
		} else if (ITERATION_INTERVAL.equals(writeType)) {
			try {
				final CtField field = model.getField(memberPrefix + "_WriteCounterFor_" + variable);
				b.append("if (").append(field.getName()).append(" != 1l) {\n");
				b.append(variable).append(".writeToFile();\n");
				b.append("}\n");
			} catch (final NotFoundException e) {
				throw new CannotCompileException(e);
			}
		} 
		b.append("\n");
	}
	
	//--------------------------------------------------------------------------------
	/** Returns the string representation (including casting to double) of the <code>stopData</code>
	 *  when it contains (directly or undirectly) a tick number.
	 */
	private String getFixValue() {
		try {
			final double d = Double.parseDouble(stopData);
			return String.valueOf(d);
		} catch (NumberFormatException e) {
			String name = new String(stopData);
			CtClass type = null;
			try {
				final CtField field = ancestor.getField(stopData);
				type = field.getType();
			} catch (NotFoundException e1) {
				if (name.endsWith("()"))
					name = name.substring(0,name.length()-2);
				CtMethod method = null;
				for (final CtMethod m : ancestor.getMethods()) {
					if (m.getName().equals(name)) {
						method = m;
						break;
					}
				}
				if (method == null)
					throw new IllegalStateException();
				name = name + "()";
				try {
					type = method.getReturnType();
				} catch (NotFoundException e2) {
					throw new IllegalStateException(e2);
				}
			}
			try {
				if (type.equals(CtClass.byteType) || type.equals(CtClass.shortType) || 
					type.equals(CtClass.intType) || type.equals(CtClass.longType) ||
					type.equals(CtClass.floatType))
					return "(double) " + name;
				else if (type.equals(CtClass.doubleType))
					return name;
				else if (type.equals(pool.get("java.lang.Byte")) ||
						 type.equals(pool.get("java.lang.Short")) ||
						 type.equals(pool.get("java.lang.Integer")) ||
						 type.equals(pool.get("java.lang.Long")) ||
						 type.equals(pool.get("java.lang.Float")) ||
						 type.equals(pool.get("java.lang.Double")))
					return name + ".doubleValue()";
			} catch (NotFoundException e1) {}
			throw new IllegalStateException();
		}
	}
	
	//--------------------------------------------------------------------------------
	/** Formats the source code <code>original</code> and returns the formatted string. */
	private String prettyPrint(final String original) {
		final String[] lines = original.split("\n");
		final StringBuilder res = new StringBuilder();
		int tabNo = 0;
		for (int i = 0;i < lines.length;++i) {
			lines[i] = lines[i].trim();
			if (lines[i].endsWith("}") || lines[i].endsWith("} else {") || lines[i].startsWith("} else if"))
				tabNo--;
			for (int j = 0;j < tabNo;++j)
				res.append("\t");
			if (lines[i].startsWith("public") || (lines[i].startsWith("private") && lines[i].endsWith("{")) ||
				lines[i].startsWith("for") || (lines[i].startsWith("if") && lines[i].endsWith("{")) ||
				lines[i].startsWith("try") || lines[i].startsWith("catch") || lines[i].endsWith("} else {") || lines[i].startsWith("} else if")) {
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
	
	//----------------------------------------------------------------------------------------------------
	// this generated method is not used
//	private CtMethod overrideGetParamsMethod(final CtClass model) throws CannotCompileException {
//		final StringBuilder sb = new StringBuilder("public String[] getParams() {\n");
//		sb.append("String[] temp = this.getOriginalParams();\n");
//		sb.append("if (temp == null)\n");
//		sb.append("temp = new String[0];\n");
//		sb.append("String[] res = new String[temp.length + " + String.valueOf(newParameters.size()) + "];\n");
//		sb.append("for (int i = 0;i < temp.length;++i) {\n");
//		sb.append("res[i] = temp[i];\n");
//		sb.append("}\n");
//		for (int i = 0;i < newParameters.size();++i) 
//			sb.append("res[temp.length + " + String.valueOf(i) + "] = \"" + newParameters.get(i).getName() + "\";\n");
//		sb.append("return res;\n");
//		sb.append("}\n");
//		final CtMethod getParamsMethod = CtNewMethod.make(sb.toString(),model);
//		privateMethods.add(sb.toString() + "\n");
//		return getParamsMethod;
//	}

	
	//--------------------------------------------------------------------------------
	/** Creates the getter/setter methods to the new parameters if necessary. 
	 * @param model the generated model
	 * @throws CannotCompileException if the syntax of the methods are wrong
	 */
	private void generateGetterSetterMethods(final CtClass model) throws CannotCompileException {
		for (final AbstractParameterInfo pi : newParameters) {
			boolean uncapitalize = false;
			String type = null;
			try {
				final CtField field = ancestor.getField(pi.getName());
				type = field.getType().getSimpleName();
			} catch (NotFoundException e) {
				// starts with lowercase
				uncapitalize = true;
				try {
					final CtField field = ancestor.getField(Util.uncapitalize(pi.getName()));
					type = field.getType().getSimpleName();
				} catch (NotFoundException ex) {
					throw new IllegalStateException(ex);
				}
			}
			try {
				ancestor.getMethod("get" + pi.getName(),getDescriptor(type,true));
			} catch (NotFoundException e) {
				final CtMethod getMethod = generateGetter(pi,type,uncapitalize,model);
				model.addMethod(getMethod);
			}
			try {
				ancestor.getMethod("set" + pi.getName(),getDescriptor(type,false));
			} catch (NotFoundException e) {
				final CtMethod setMethod = generateSetter(pi,type,uncapitalize,model);
				model.addMethod(setMethod);
			}
		}
	}
	
	//--------------------------------------------------------------------------------
	/** Creates the getter method to the new parameter specified by <code>info</code>.
	 * @param info the information object of the new parameter
	 * @param uncapitalize flag that determines whether the variable name that belongs
	 *                     to the information object starts with lowercase or not
	 * @param model the generated model
	 * @throws CannotCompileException if the syntax of the methods are wrong
	 */
	private CtMethod generateGetter(final AbstractParameterInfo info, final String type, final boolean uncapitalize, final CtClass model)
																													throws CannotCompileException {
		final String uName = uncapitalize ? Util.uncapitalize(info.getName()) : info.getName(); 
		final StringBuilder sb = new StringBuilder("public ");
		sb.append(type);
		sb.append(" get");
		sb.append(info.getName());
		sb.append("() {\n");
		sb.append("return " + uName + ";\n");
		sb.append("}\n");
		final CtMethod getter = CtNewMethod.make(sb.toString(),model);
		privateMethods.add(sb.toString() + "\n");
		return getter;
	}
	
	//--------------------------------------------------------------------------------
	/** Creates the setter method to the new parameter specified by <code>info</code>.
	 * @param info the information object of the new parameter
	 * @param uncapitalize flag that determines whether the variable name that belongs
	 *                     to the information object starts with lowercase or not
	 * @param model the generated model
	 * @throws CannotCompileException if the syntax of the methods are wrong
	 */
	private CtMethod generateSetter(final AbstractParameterInfo info, final String type, final boolean uncapitalize, final CtClass model) 
																													throws CannotCompileException {
		final String uName = uncapitalize ? Util.uncapitalize(info.getName()) : info.getName();
		final StringBuilder sb = new StringBuilder("public void set");
		sb.append(info.getName());
		sb.append("(" + type + " " + uName + ") {\n");
		sb.append("this." + uName + " = " + uName + ";\n");
		sb.append("}\n");
		final CtMethod setter = CtNewMethod.make(sb.toString(),model);
		privateMethods.add(sb.toString() + "\n");
		return setter;
	}
	
	//--------------------------------------------------------------------------------
	/** Returns the descriptor of a getter/setter method.
	 * @see http://java.sun.com/docs/books/jvms/second_edition/html/ClassFile.doc.html
	 * @param type the name of the type belongs to the getter/setter method
	 * @param getter true, if it is need the descriptor of the getter method 
	 */
	private String getDescriptor(final String type, final boolean getter) {
		String res = "(";
		if (getter)
			res += ")";
		if ("byte".equals(type))
			res += "B";
		else if ("short".equals(type))
			res += "S";
		else if ("int".equals(type))
			res += "I";
		else if ("long".equals(type))
			res += "J";
		else if ("float".equals(type))
			res += "F";
		else if ("double".equals(type))
			res += "D";
		else if ("boolean".equals(type))
			res += "Z";
		else
			res += "Ljava/lang/" + type + ";";
		if (!getter)
			res += ")V";
		return res;
	}
	
	//----------------------------------------------------------------------------------------------------
	private String parseRecorderType(final String recordType, final String[] arg) {
		if (recordType.equals(ITERATION) || recordType.equals(RUN))
			return recordType;
		if (recordType.startsWith(ITERATION_INTERVAL) || recordType.startsWith(CONDITION)) {
			final String[] parts = recordType.split(":");
			arg[0] = parts[1];
			return parts[0];
		}
		return null;
	}
	
	//----------------------------------------------------------------------------------------------------
	private String parseWriteType(final String writeType, final long[] arg) {
		if (writeType.equals(RECORD) || writeType.equals(RUN))
			return writeType;
		if (writeType.startsWith(ITERATION_INTERVAL)) {
			final String[] parts = writeType.split(":");
			try {
				arg[0] = Long.parseLong(parts[1]);
			} catch (NumberFormatException e) {
				return null;
			}
			return parts[0];
		}
		return null;

	}
	
	//----------------------------------------------------------------------------------------------------
	private void generateAddBatchListenerMethod(final CtClass model) throws CannotCompileException {
		final String src = "private ArrayList " + memberPrefix + "_listeners = new ArrayList();";
		listenersField  = CtField.make(src,model);
		newFields.add(src + "\n");
		model.addField(listenersField);
		
		final StringBuilder b  = new StringBuilder("public void " + memberPrefix + "_addBatchListener(IBatchListener listener) {\n");
		b.append("this.").append(listenersField.getName()).append(".add(listener);\n");
		b.append("}\n");
		
		final CtMethod method = CtNewMethod.make(b.toString(),model);
		model.addMethod(method);
		privateMethods.add(b.toString() + "\n");
	}
	
	//----------------------------------------------------------------------------------------------------
	private void generateRemoveBatchListenerMethod(final CtClass model) throws CannotCompileException {
		final StringBuilder b = new StringBuilder("public void " + memberPrefix + "_removeBatchListener(IBatchListener listener) {\n");
		b.append("this.").append(listenersField.getName()).append(".remove(listener);\n");
		b.append("}\n");
		
		final CtMethod method = CtNewMethod.make(b.toString(),model);
		model.addMethod(method);
		privateMethods.add(b.toString() + "\n");
	}

	//----------------------------------------------------------------------------------------------------
	private void generateAddRecorderListenerMethod(final CtClass model) throws CannotCompileException {
		final StringBuilder b  = new StringBuilder("public void " + memberPrefix + "_addRecorderListener(MasonRecorderListener listener) {\n");
		
		for (final RecorderInfo rec : recorders) {
			final String variable = findVariable(rec.getName());
			if (variable == null)
				throw new IllegalStateException();
			b.append(variable + ".addRecorderListener(listener);\n");
		}
		b.append("}\n");
		
		final CtMethod method = CtNewMethod.make(b.toString(),model);
		model.addMethod(method);
		privateMethods.add(b.toString() + "\n");
	}
	
	//----------------------------------------------------------------------------------------------------
	private void generateRemoveRecorderListenerMethod(final CtClass model) throws CannotCompileException {
		final StringBuilder b  = new StringBuilder("public void " + memberPrefix + "_removeRecorderListener(MasonRecorderListener listener) {\n");
		
		for (final RecorderInfo rec : recorders) {
			final String variable = findVariable(rec.getName());
			if (variable == null)
				throw new IllegalStateException();
			b.append(variable + ".removeRecorderListener(listener);\n");
		}
		b.append("}\n");

		
		final CtMethod method = CtNewMethod.make(b.toString(),model);
		model.addMethod(method);
		privateMethods.add(b.toString() + "\n");
	}

	//----------------------------------------------------------------------------------------------------
	private void generateGetRunMethod(final CtClass model) throws CannotCompileException {
		final String src = "private long " + memberPrefix + "_run = 0L;";
		runField = CtField.make(src,model);
		newFields.add(src + "\n");
		model.addField(runField);
		
		final StringBuilder b = new StringBuilder("public long " + memberPrefix + "_getRun() {\n");
		b.append("return this.").append(runField.getName()).append(";\n");
		b.append("}\n");
		
		final CtMethod method = CtNewMethod.make(b.toString(),model);
		model.addMethod(method);
		privateMethods.add(b.toString() + "\n");
	}
	
	//----------------------------------------------------------------------------------------------------
	private void generateSetRunMethod(final CtClass model) throws CannotCompileException {
		final StringBuilder b = new StringBuilder("public void " + memberPrefix + "_setRun(long run) {\n");
		b.append("this.").append(runField.getName()).append(" = run;\n");
		b.append("}\n");
		
		final CtMethod method = CtNewMethod.make(b.toString(),model);
		model.addMethod(method);
		privateMethods.add(b.toString() + "\n");
	}
	
	//----------------------------------------------------------------------------------------------------
	private void generateGetConstantParameterNamesMethod(final CtClass model) throws CannotCompileException {
		final String src = "private HashMap " + memberPrefix + "_constantParameterNames = new HashMap();";
		constantParameterNamesField = CtField.make(src,model);
		newFields.add(src + "\n");
		model.addField(constantParameterNamesField);
		
		final StringBuilder b = new StringBuilder("public HashMap " + memberPrefix + "_getConstantParameterNames() {\n");
		b.append("return this.").append(constantParameterNamesField.getName()).append(";\n");
		b.append("}\n");
		
		final CtMethod method = CtNewMethod.make(b.toString(),model);
		model.addMethod(method);
		privateMethods.add(b.toString() + "\n");
	}
	
	//----------------------------------------------------------------------------------------------------
	private void generateSetConstantParameterNamesMethod(final CtClass model) throws CannotCompileException {
		final StringBuilder b = new StringBuilder("public void " + memberPrefix + "_setConstantParameterNames(HashMap constants) {\n");
		b.append("this.").append(constantParameterNamesField.getName()).append(" = constants;\n");
		b.append("}\n");
		
		final CtMethod method = CtNewMethod.make(b.toString(),model);
		model.addMethod(method);
		privateMethods.add(b.toString() + "\n");
	}
	
	//----------------------------------------------------------------------------------------------------
	private void generateGetMutableParameterNamesMethod(final CtClass model) throws CannotCompileException {
		final String src = "private HashMap " + memberPrefix + "_mutableParameterNames = new HashMap();";
		mutableParameterNamesField  = CtField.make(src,model);
		newFields.add(src + "\n");
		model.addField(mutableParameterNamesField);
		
		final StringBuilder b = new StringBuilder("public HashMap " + memberPrefix + "_getMutableParameterNames() {\n");
		b.append("return this.").append(mutableParameterNamesField.getName()).append(";\n");
		b.append("}\n");
		
		final CtMethod method = CtNewMethod.make(b.toString(),model);
		model.addMethod(method);
		privateMethods.add(b.toString() + "\n");
	}
	
	//----------------------------------------------------------------------------------------------------
	private void generateSetMutableParameterNamesMethod(final CtClass model) throws CannotCompileException {
		final StringBuilder b = new StringBuilder("public void " + memberPrefix + "_setMutableParameterNames(HashMap mutables) {\n");
		b.append("this.").append(mutableParameterNamesField.getName()).append(" = mutables;\n");
		b.append("}\n");
		
		final CtMethod method = CtNewMethod.make(b.toString(),model);
		model.addMethod(method);
		privateMethods.add(b.toString() + "\n");
	}
	
	//----------------------------------------------------------------------------------------------------
	private void generateWriteEnd(final CtClass model) throws CannotCompileException {
		final StringBuilder b = new StringBuilder("public void " + memberPrefix + "_writeEnd() {\n");
		for (final CtField field : recorderFields) {
			b.append("this.").append(field.getName()).append(".writeToFile();\n");
			b.append("this.").append(field.getName()).append(".writeEnd();\n");
		}
		b.append("}\n");
		
		final CtMethod method = CtNewMethod.make(b.toString(),model);
		model.addMethod(method);
		privateMethods.add(b.toString() + "\n");
	}
	
	//----------------------------------------------------------------------------------------------------
	private boolean needInitializationForTimeSeries() {
		for (final RecorderInfo rec : recorders) {
			final List<RecordableInfo> recordables = rec.getRecordables();
			for (final RecordableInfo re : recordables) {
				if (needInitializationForTimeSeries(re)) return true;
			}
		}
		return false;
	}
	
	//----------------------------------------------------------------------------------------------------
	private boolean needInitializationForTimeSeries(final RecordableInfo info) {
		if (info instanceof GeneratedRecordableInfo) {
			final List<GeneratedRecordableInfo> references = ((GeneratedRecordableInfo)info).getReferences();
			for (final GeneratedRecordableInfo gri : references)
				if (needInitializationForTimeSeries(gri)) return true; 
			if ((info instanceof ExtendedOperatorGeneratedRecordableInfo) && 
				"ai.aitia.meme.paramsweep.operatorPlugin.Operator_TimeSeries".equalsIgnoreCase(((OperatorGeneratedRecordableInfo)info).getGeneratorName())) return true;
		}
		return false;
	}

	private void generateAitiaGeneratedVariablesMapDeclaration(final CtClass model) throws CannotCompileException{
		final String fieldStr = "private java.util.HashMap " + PlatformConstants.AITIA_GENERATED_VARIABLES + " = null;\n";
		final CtField field = CtField.make(fieldStr,model);
		model.addField(field);
		source.append(fieldStr);
	}
	
	//----------------------------------------------------------------------------------------------------
	private String generateAitiaGeneratedVariablesMapInitializer(final CtClass model) throws CannotCompileException {
		final StringBuilder sb = new StringBuilder();
		sb.append(PlatformConstants.AITIA_GENERATED_VARIABLES).append(" = new java.util.HashMap();\n");
		return sb.toString();
	}
	
	//----------------------------------------------------------------------------------------------------
	private void generateAssistantMethod(final String name, final int idx, final AssistantMethod method, final CtClass model) 
																													throws CannotCompileException {
		String _name = name;
		if (_name.endsWith("()")) {
			_name = _name.substring(0,_name.length() - 2);
		}
		final String fullName = _name + PlatformConstants.AITIA_GENERATED_INFIX + idx + "()";
		final StringBuilder src = new StringBuilder();
		src.append("private ").append(method.returnValue.getName()).append(" ").append(fullName).append(" {\n");
		src.append(method.body);
		src.append("}\n");
		final CtMethod m = CtNewMethod.make(src.toString(),model);
		model.addMethod(m);
		privateMethods.add(src.toString());
		assistantMethods.add(new Pair<String,ScheduleTime>(fullName,method.scheduleTime));
	}
	
	//----------------------------------------------------------------------------------------------------
	private String scheduleAssistantMethods()  {
		final StringBuilder sb = new StringBuilder();
		for (final Pair<String,ScheduleTime> p : assistantMethods) {
			switch (p.getSecond()) {
			case NEVER : continue;
			case TICK  : sb.append(scheduleMethodTick(p.getFirst()));
						 break;
			case RUN   : sb.append(scheduleMethodRun(p.getFirst()));
						 break;
			default	   : throw new IllegalStateException();
			}
		}
		return sb.toString();
	}
	
	//----------------------------------------------------------------------------------------------------
	private String scheduleMethodTick(final String methodName) { return methodName + ";\n"; }
	
	//----------------------------------------------------------------------------------------------------
	private String scheduleMethodRun(final String methodName) {
		StringBuilder sb = new StringBuilder();
		sb.append("if (finish) {\n");
		sb.append(methodName).append(";\n");
		sb.append("}\n");
		return sb.toString();
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
	private void generateUserVariableFields(final List<UserDefinedVariable> variables, final CtClass model) throws CannotCompileException {
		for (final UserDefinedVariable variable : variables) {
			String declStr = "private " + variable.getType().getCanonicalName() + " " + variable.getName() + ";\n";
			final CtField field = CtField.make(declStr,model);
			model.addField(field);
			source.append(declStr);
		}
		source.append("\n");
	}
	
	//----------------------------------------------------------------------------------------------------
	private void generateUserVariableInitialization(final List<UserDefinedVariable> variables, final CtClass model) throws CannotCompileException {
		final String methodName = memberPrefix + "_privateMethod" + String.valueOf(ID++) + "()";
		final StringBuilder code = new StringBuilder();
		code.append("private void ").append(methodName).append(" {\n");
		for (final UserDefinedVariable variable : variables) 
			code.append(variable.getInitializationCode()).append("\n");
		code.append("}\n");
		initializeVariablesMethod = CtNewMethod.make(code.toString(),model);
		model.addMethod(initializeVariablesMethod);
		privateMethods.add(code.toString() + "\n");
	}
}
