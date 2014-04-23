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
package ai.aitia.meme.paramsweep.platform.repast.impl;

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
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;
import ai.aitia.meme.paramsweep.batch.output.RecordableInfo;
import ai.aitia.meme.paramsweep.batch.output.RecorderInfo;
import ai.aitia.meme.paramsweep.batch.param.AbstractParameterInfo;
import ai.aitia.meme.paramsweep.platform.repast.RepastPlatform;
import ai.aitia.meme.paramsweep.platform.repast.info.ExtendedOperatorGeneratedRecordableInfo;
import ai.aitia.meme.paramsweep.platform.repast.info.GeneratedRecordableInfo;
import ai.aitia.meme.paramsweep.platform.repast.info.OperatorGeneratedRecordableInfo;
import ai.aitia.meme.paramsweep.platform.repast.info.ScriptGeneratedRecordableInfo;
import ai.aitia.meme.paramsweep.platform.repast.info.StatisticGeneratedRecordableInfo;
import ai.aitia.meme.paramsweep.utils.AssistantMethod;
import ai.aitia.meme.paramsweep.utils.PlatformConstants;
import ai.aitia.meme.paramsweep.utils.UserDefinedVariable;
import ai.aitia.meme.paramsweep.utils.Util;
import ai.aitia.meme.paramsweep.utils.AssistantMethod.ScheduleTime;
import ai.aitia.meme.utils.Utils.Pair;

/** This class generates the batch (or extended) versions of Repast model classes (class
 *  files. 
 */ 
public class ModelGenerator {

	//===============================================================================
	// members
	
	/** Standard prefix of the names of generated members (except statistics and scripts). */  
	public static final String memberPrefix = "aitiaGenerated";
	
	/** Standard suffix of the generated model name (it stands before the timestamp). */
	public static final String modelSuffix = "BatchGenerated";
	
	/** The accessible name of the Schedule object of the model in string format. */ 
	private static final String schedule = "getSchedule()";
	
	/** The numeric types in string format. */
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
	public ModelGenerator(ClassPool pool,
						  CtClass ancestor,
						  List<RecorderInfo> recorders,
						  String stopData,
						  boolean stopAfterFixInterval,
						  List<AbstractParameterInfo> newParameters,
						  String directory,
						  String description) {
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
		} catch (NotFoundException e) {
			throw new IllegalStateException(e);
		}
		source = new StringBuilder();
		privateMethods = new ArrayList<String>();
		timestamp = Util.getTimeStamp();
	}
	
	//--------------------------------------------------------------------------------
	/** Generates the new model.
	 * @return the error message (null if there is no error)
	 */
	public String generateModel() {
		String error = null;
		CtClass model = null;
		try {
			if (ancestor.getPackageName() != null) 
				source.append("package " + ancestor.getPackageName() + ";\n\n");
			importPackages();
			model = pool.makeClass(ancestor.getName() + modelSuffix + "__" + timestamp,ancestor);
			model.addInterface(generatedInterface);
			
			if (description != null && description.trim().length() > 0)
				source.append("/* " + description + " */\n");
			source.append("public class " + model.getSimpleName() + " extends " + ancestor.getName() + " implements " +
					      generatedInterface.getSimpleName() + " {\n\n");
			
			recorderFields = createRecorderFields(model);
			for (CtField field : recorderFields)
				model.addField(field);
			
			source.append("\n");

			if (newParameters != null) {
				CtMethod getInitParamMethod = overrideGetInitParamMethod(model);
				model.addMethod(getInitParamMethod);
				generateGetterSetterMethods(model);
			}
			
			if (stopData != null && stopData.trim().length() > 0) {
				// 'null' or empty string indicates that we generated model class because of new parameters only
				
				final List<UserDefinedVariable> userVariables = collectVariables();
				
				if (userVariables.size() > 0) {
					generateUserVariableFields(userVariables,model);
					generateUserVariableInitialization(userVariables,model);
				}
				
				CtMethod stopMethod = createStopMethod(model);
				model.addMethod(stopMethod);
			
				CtMethod beginMethod = overrideBeginMethod(model);
				model.addMethod(beginMethod);
			}
			
			model.stopPruning(true);
			model.writeFile(directory);
			
			for (String s : privateMethods) 
				source.append(s + "\n");
			privateMethods.clear();
			source.append("}");
			return null;
		} catch (CannotCompileException e) {
			error = "Compile error while generating descendant model.";
			if (e.getReason() != null && e.getReason().trim().length() != 0)
				error += "\nReason: " + e.getReason();
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
		try {
			PrintWriter writer = new PrintWriter(new FileWriter(directory + File.separator + ancestor.getName().replace('.',File.separatorChar) +
															 	modelSuffix + "__" + timestamp + ".java"));
			writer.print(prettyPrint(source.toString()));
			writer.flush();
			writer.close();
			return null;
		} catch (IOException e) {
			return "Error while writing the model source.";
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
		imports.add("ai.aitia.meme.paramsweep.platform.repast.impl");
		imports.add("uchicago.src.sim.engine");
		imports.add("uchicago.src.sim.analysis");
		
		for (int i = 0;i < recorders.size();++i) {
			List<RecordableInfo> rec = recorders.get(i).getRecordables();
			for (int j = 0;j < rec.size();++j) {
				RecordableInfo recordableInfo = rec.get(j);
				if (recordableInfo instanceof GeneratedRecordableInfo)
					importPackagesImpl((GeneratedRecordableInfo)recordableInfo,imports);
			}
		}

		for (String imp : imports) {
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
			StatisticGeneratedRecordableInfo sgri = (StatisticGeneratedRecordableInfo) gri;
			for (GeneratedRecordableInfo _gri : sgri.getReferences())
				importPackagesImpl(_gri,imports);
		} else if (gri instanceof OperatorGeneratedRecordableInfo) {
			OperatorGeneratedRecordableInfo ogri = (OperatorGeneratedRecordableInfo) gri;
			for (GeneratedRecordableInfo _gri : ogri.getReferences())
				importPackagesImpl(_gri,imports);
		} else if (gri instanceof ScriptGeneratedRecordableInfo) {
			ScriptGeneratedRecordableInfo sgri = (ScriptGeneratedRecordableInfo) gri;
			for (GeneratedRecordableInfo _gri : sgri.getReferences())
				importPackagesImpl(_gri,imports);
			List<String> newImports = sgri.getImports();
			for (String imp : newImports) {
				int index = imp.lastIndexOf('.');
				if (index == -1) continue;
				String candidate = imp.substring(0,index);
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
	private CtMethod createStopMethod(CtClass model) throws CannotCompileException {
		StringBuilder sb = new StringBuilder("public void " + memberPrefix + "_stop() {\n");
		if (!stopAfterFixInterval)
			sb.append("if ((boolean)(" + stopData + "))\n");
		sb.append("fireSimEvent(new SimEvent(this,SimEvent.STOP_EVENT));\n");
		sb.append("}\n");
		CtMethod stopMethod = CtNewMethod.make(sb.toString(),model);
		source.append(sb.toString() + "\n");
		return stopMethod;
	}
	
	//--------------------------------------------------------------------------------
	/** Creates the recorder fields (DataRecorder objects) from the recorders tree.
	 * @param model the generated model
	 * @return the array of the Javassist representation of the fields
  	 * @throws CannotCompileException if the syntax of the field declarations are wrong
	 */ 
	private CtField[] createRecorderFields(CtClass model) throws CannotCompileException {
		CtField[] result = new CtField[recorders.size()];
		for (int i = 0;i < recorders.size();++i) {
			String origName = recorders.get(i).getName();
			String variableName = createVariableName(origName);
			recorderNames.put(variableName, origName);
			String src = "private DataRecorder " + variableName + " = null; ";
			result[i] = CtField.make(src,model);
			source.append(src + "\n");
		}
		return result;
	}
	
	//--------------------------------------------------------------------------------
	/** Creates and returns a variable name from <code>original</code>. It replaces
	 *  all spaces and accentuated letter. 
	 */
	private String createVariableName(String original) {
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
	/** Creates the begin() method of the generated model. 
	 * @param model the generated model
	 * @return the Javassist representation of the method
	 * @throws CannotCompileException if the syntax of the method is wrong
	 */
	private CtMethod overrideBeginMethod(CtClass model) throws CannotCompileException {
		StringBuilder sb = new StringBuilder("public void begin() {\n");
		sb.append("super.begin();\n");
		sb.append("if (" + schedule + " == null)\n");
		sb.append("throw new IllegalStateException(\"" + schedule + " cannot be 'null'.\");\n");
		if (initializeVariablesMethod != null)
			sb.append(initializeVariablesMethod.getName() + "();\n");
		if (needInitializationForTimeSeries())
			sb.append(generateInitializing(model));
		sb.append(generateRecorders(model));
		sb.append(scheduleAssistantMethods());
		sb.append(scheduleRecordAndWrite(model));
		sb.append(scheduleStop());
		sb.append("}\n");
		CtMethod beginMethod = CtNewMethod.make(sb.toString(),model);
		source.append(sb.toString() + "\n");
		return beginMethod;
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
	
	//----------------------------------------------------------------------------------------------------
	private String generateInitializing(final CtClass model) throws CannotCompileException {
		final String fieldStr = "private java.util.HashMap " + PlatformConstants.AITIA_GENERATED_VARIABLES + " = null;\n";
		final CtField field = CtField.make(fieldStr,model);
		model.addField(field);
		final StringBuilder sb = new StringBuilder();
		sb.append(PlatformConstants.AITIA_GENERATED_VARIABLES).append(" = new java.util.HashMap();\n");
		return sb.toString();
	}
	
	//--------------------------------------------------------------------------------
	/** Creates the source code belongs to the recorder initialization (creation and
	 *  registration of the recordable elements).
	 * @param model the generated model
	 * @return the source code
	 * @throws CannotCompileException if the syntax of the related generated methods are wrong
	 */
	private String generateRecorders(CtClass model) throws CannotCompileException {
		StringBuilder sb = new StringBuilder();
		sb.append("String recorderPathPrefix = System.getProperty(\"" + RepastPlatform.recorderPrefix + "\",\"\");\n");
		for (int i = 0;i < recorders.size();++i) {
			RecorderInfo rec = recorders.get(i);
			String variable = findVariable(rec.getName());
			if (variable == null)
				throw new IllegalStateException();
			sb.append(variable + " = new DataRecorder(recorderPathPrefix + \"" + rec.getOutputFile().getPath().replaceAll("\\\\","\\\\\\\\")
					  + "\",this);\n");
			sb.append(variable + ".setDelimeter(\"" + rec.getDelimiter() + "\");\n");
			List<RecordableInfo> recordables = rec.getRecordables();
			for (int j = 0;j < recordables.size();++j) {
				sb.append(generateRecordable(variable,recordables.get(j),model));
			}
		}
		return sb.toString();
	}
	
	//--------------------------------------------------------------------------------
	/** Returns the variable name belongs to the original name of a recorder.
	 * @param original the original name of the recorder
	 * @return the variable name of the recorder (or null)
	 */
	private String findVariable(String original) {
		for (Entry<String,String> e : recorderNames.entrySet()) {
			if (e.getValue().equals(original))
				return e.getKey();
		}
		return null;
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
	private String generateRecordable(String variable, RecordableInfo re, CtClass model) throws CannotCompileException {
		StringBuilder sb = new StringBuilder(variable);
		//escaping "-s in re.getName():
		String reGetNameEscaped = re.getName().replace("\"", "\\\"");
		if (!isNumeric(re)) { // String, boolean or Boolean
			sb.append(".createObjectDataSource(\"" + reGetNameEscaped + "\",this,\"" + generateRecorderMethod(re,model) + "\");\n");
		} else { // numeric
			sb.append(".createNumericDataSource(\"" + reGetNameEscaped + "\",this,\"" + generateRecorderMethod(re,model) + "\");\n");
		}
		return sb.toString();
	}
		
	//--------------------------------------------------------------------------------
	/** Creates a "private" method that returns the value of a recordable element as
	 *  a double or (in the case of non-number element) an object. If the recordable 
	 *  element is a statistic instance or a script this method creates its method, too.
	 * @param mi the information object of the recordable element  
	 * @param model the generated model
	 * @return the name of the created method
	 * @throws CannotCompileException if the syntax of the method (or other related generated methods) is wrong
	 */
	private String generateRecorderMethod(RecordableInfo re, CtClass model) throws CannotCompileException {
		if (re instanceof StatisticGeneratedRecordableInfo) {
			StatisticGeneratedRecordableInfo sgri = (StatisticGeneratedRecordableInfo) re;
			generateStatistic(sgri,model);
		} else if (re instanceof OperatorGeneratedRecordableInfo) {
			OperatorGeneratedRecordableInfo ogri = (OperatorGeneratedRecordableInfo) re;
			generateOperator(ogri,model);
		} else if (re instanceof ScriptGeneratedRecordableInfo) {
			ScriptGeneratedRecordableInfo sgri = (ScriptGeneratedRecordableInfo) re;
			generateScript(sgri,model);
		}
		String methodName = memberPrefix + "_privateMethod" + String.valueOf(ID++);
		String src = "public ";
		if (isNumeric(re)) {
			src += "double " + methodName + "() {\n";
			src += "return ";
			if (!isPrimitive(re))
				src += re.getAccessibleName() + ".doubleValue();\n";
			else if (re.getType().equals(Double.TYPE))
				src += re.getAccessibleName() + ";\n";
			else
				src += "(double) " + re.getAccessibleName() + ";\n";
		} else {
			src += "Object " + methodName + "() {\n";
			src += "return ";
			if (re.getType().equals(Boolean.TYPE))
				src += "new Boolean(" + re.getAccessibleName() + ");\n";
			else
				src += re.getAccessibleName() + ";\n";
		}
		src += "}\n";
		CtMethod m = CtNewMethod.make(src,model);
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
	private void generateStatistic(StatisticGeneratedRecordableInfo re, CtClass model) throws CannotCompileException {
		if (generatedScripts.contains(re.getAccessibleName())) return;
		List<GeneratedRecordableInfo> references = re.getReferences();
		for (GeneratedRecordableInfo gri : references) {
			if (gri instanceof StatisticGeneratedRecordableInfo) 
				generateStatistic((StatisticGeneratedRecordableInfo)gri,model);
			else if (gri instanceof OperatorGeneratedRecordableInfo)
				generateOperator((OperatorGeneratedRecordableInfo)gri,model);
			else 
				generateScript((ScriptGeneratedRecordableInfo)gri,model);
		}
		String src = "private " + re.getType().getCanonicalName() + " " + re.getAccessibleName() + " {\n";
		src += re.getSource() + "}\n";
		CtMethod m = CtNewMethod.make(src,model);
		model.addMethod(m);
		privateMethods.add(src);
		generatedScripts.add(re.getAccessibleName());
	}
	
	//----------------------------------------------------------------------------------------------------
	private void generateOperator(OperatorGeneratedRecordableInfo re, CtClass model) throws CannotCompileException {
		if (generatedScripts.contains(re.getAccessibleName())) return;
		List<GeneratedRecordableInfo> references = re.getReferences();
		for (GeneratedRecordableInfo gri : references) {
			if (gri instanceof OperatorGeneratedRecordableInfo)
				generateOperator((OperatorGeneratedRecordableInfo)gri,model);
			else if (gri instanceof StatisticGeneratedRecordableInfo)
				generateStatistic((StatisticGeneratedRecordableInfo)gri,model);
			else
				generateScript((ScriptGeneratedRecordableInfo)gri,model);
		}
		if (re instanceof ExtendedOperatorGeneratedRecordableInfo) {
			final ExtendedOperatorGeneratedRecordableInfo eogri = (ExtendedOperatorGeneratedRecordableInfo) re;
			for (int i = 0;i < eogri.getAssistantMethods().size();++i)
				generateAssistantMethod(eogri.getAccessibleName(),i,eogri.getAssistantMethods().get(i),model);
		}
		String src = "private " + re.getType().getCanonicalName() + " " + re.getAccessibleName() + " {\n";
		src += re.getSource() + "}\n";
		CtMethod m = CtNewMethod.make(src,model);
		model.addMethod(m);
		privateMethods.add(src);
		generatedScripts.add(re.getAccessibleName());
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
		src.append("public ").append(method.returnValue.getName()).append(" ").append(fullName).append(" {\n");
		src.append(method.body);
		src.append("}\n");
		final CtMethod m = CtNewMethod.make(src.toString(),model);
		model.addMethod(m);
		privateMethods.add(src.toString());
		assistantMethods.add(new Pair<String,ScheduleTime>(fullName,method.scheduleTime));
	}
	
	//--------------------------------------------------------------------------------
	/** Creates the method of the script specified by <code>mi</code>. It also
	 *  creates all referenced generated statistic instance or script, too.
	 * @param mi the information object of the script
	 * @param model the generated model
	 * @throws CannotCompileException if the syntax of the method (or other related generated method) is wrong
	 */
	private void generateScript(ScriptGeneratedRecordableInfo re, CtClass model) throws CannotCompileException {
		if (generatedScripts.contains(re.getAccessibleName())) return;
		List<GeneratedRecordableInfo> references = re.getReferences();
		for (GeneratedRecordableInfo gri : references) {
			if (gri instanceof ScriptGeneratedRecordableInfo) 
				generateScript((ScriptGeneratedRecordableInfo)gri,model);
			else if (gri instanceof StatisticGeneratedRecordableInfo) 
				generateStatistic((StatisticGeneratedRecordableInfo)gri,model);
			else
				generateOperator((OperatorGeneratedRecordableInfo)gri,model);
		}
		String src = "private " + re.getType().getCanonicalName() + " " + re.getAccessibleName() + " {\n";
		src += re.getSource() + "}\n";
		CtMethod m = CtNewMethod.make(src,model);
		model.addMethod(m);
		privateMethods.add(src);
		generatedScripts.add(re.getAccessibleName());
	}
	
	//--------------------------------------------------------------------------------
	/** Creates a "private" method that records the values of the registered elements of
	 *  a recorder. This recording may be conditional. If is not then it also writes
	 *  the values to the output file immediatly after the recording.
	 * @param recorder the variable name of the recorder
	 * @param arg the string representation of the recording condition (null if there is not condition)  
	 * @param model the generated model
	 * @return the name of the created method
	 * @throws CannotCompileException if the syntax of the method is wrong
	 */
	private String generateExecuteRecordMethod(String recorder, String arg, CtClass model) throws CannotCompileException {
		String methodName = memberPrefix + "_privateMethod" + String.valueOf(ID++);
		String src = "public void " + methodName + "() {\n";
		if (arg == null) {
			src += recorder + ".record();\n";
			src += recorder + ".writeToFile();\n";
		} else {
			src += "if ((boolean) (" + arg + "))\n";
			src += recorder + ".record();\n";
		}
		src += "}\n";
		CtMethod m = CtNewMethod.make(src,model);
		model.addMethod(m);
		privateMethods.add(src);
		return methodName;
	}
	
	//--------------------------------------------------------------------------------
	/** Creates a "private" method that records and writes to file the values of the
	 *  registered elements of a recorder.
	 * @param recorder the variable name of the recorder
	 * @param arg the string representation of the recording condition (null is not permitted)  
	 * @param model the generated model
	 * @return the name of the created method
	 * @throws CannotCompileException if the syntax of the method is wrong
	 */
	private String generateConditionRecordAndWrite(String recorder, String arg, CtClass model) throws CannotCompileException {
		String methodName = memberPrefix + "_privateMethod" + String.valueOf(ID++);
		String src = "public void " + methodName + "() {\n";
		src += "if ((boolean) (" + arg + ")) {\n";
		src += recorder + ".record();\n";
		src += recorder + ".writeToFile();\n";
		src += "}\n";
		src += "}\n";
		CtMethod m = CtNewMethod.make(src,model);
		model.addMethod(m);
		privateMethods.add(src);
		return methodName;
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
	private String scheduleMethodTick(final String methodName) {
		String _methodName = methodName;
		if (_methodName.endsWith("()"))
			_methodName = _methodName.substring(0,_methodName.length() - 2);
		return schedule + ".scheduleActionAtInterval(1.0,this,\"" + _methodName + "\",Schedule.LAST);\n";
	}
	
	//----------------------------------------------------------------------------------------------------
	private String scheduleMethodRun(final String methodName) {
		String _methodName = methodName;
		if (_methodName.endsWith("()"))
			_methodName = _methodName.substring(0,_methodName.length() - 2);
		return schedule + ".scheduleActionAtEnd(this,\"" + _methodName + "\");\n";
	}
		
	//--------------------------------------------------------------------------------
	/** Creates the source code belongs to the recorder scheduling (recording and writing to file).
	 * @param model the generated model
	 * @return the source code
	 * @throws CannotCompileException if the syntax of the related generated methods are wrong
	 */
	private String scheduleRecordAndWrite(CtClass model) throws CannotCompileException  {
		StringBuilder sb = new StringBuilder();
		
		for (int i = 0;i < recorders.size();++i) {
			RecorderInfo rec = recorders.get(i);
			String variable = findVariable(rec.getName());
			if (variable == null)
				throw new IllegalStateException();
			String[] arg = new String[1];
			String recorderType = parseRecorderType(rec.getRecordType(),arg);
			long[] writeArg = new long[1];
			String writeType = parseWriteType(rec.getWriteType(),writeArg);
			if (recorderType == null || writeType == null)
				throw new IllegalStateException();
			if (ITERATION.equals(recorderType)) {
				if (RUN.equals(writeType) ||
				   (ITERATION_INTERVAL.equals(writeType) && writeArg[0] > 1)) {
					sb.append(schedule + ".scheduleActionAtInterval(1.0," + variable + ",\"record\",Schedule.LAST);\n");
					if (ITERATION_INTERVAL.equals(writeType))
						sb.append(schedule + ".scheduleActionAtInterval((double)" + String.valueOf(writeArg[0]) + "," +
								  variable + ",\"writeToFile\",Schedule.LAST);\n");
					sb.append(schedule + ".scheduleActionAtEnd(" + variable + ",\"writeToFile\");\n");
				} else
					sb.append(schedule + ".scheduleActionAtInterval(1.0,this,\"" + generateExecuteRecordMethod(variable,null,model) +
							  "\",Schedule.LAST);\n");
			} else if (ITERATION_INTERVAL.equals(recorderType)) {
				if (RUN.equals(writeType) ||
				   (ITERATION_INTERVAL.equals(writeType) && writeArg[0] > Long.parseLong(arg[0]))) {
					sb.append(schedule + ".scheduleActionAtInterval((double)" + arg[0] + "," + variable + ",\"record\",Schedule.LAST);\n");
					if (ITERATION_INTERVAL.equals(writeType))
						sb.append(schedule + ".scheduleActionAtInterval((double)" + String.valueOf(writeArg[0]) + "," + variable + 
								  ",\"writeToFile\",Schedule.LAST);\n");
					sb.append(schedule + ".scheduleActionAtEnd(" + variable + ",\"writeToFile\");\n");
				} else
					sb.append(schedule + ".scheduleActionAtInterval((double)" + arg[0] + ",this,\"" + 
							  generateExecuteRecordMethod(variable,null,model) + "\",Schedule.LAST);\n");
			} else if (RUN.equals(recorderType)) 
				sb.append(schedule + ".scheduleActionAtEnd(this,\"" + generateExecuteRecordMethod(variable,null,model) + "\");\n");
			else if (CONDITION.equals(recorderType)) {
				if (RUN.equals(writeType))
					sb.append(schedule + ".scheduleActionAtInterval(1.0,this,\"" + generateExecuteRecordMethod(variable,arg[0],model) + 
							  "\",Schedule.LAST);\n");
				else
					sb.append(schedule + ".scheduleActionAtInterval(1.0,this,\"" + generateConditionRecordAndWrite(variable,arg[0],model) +
							  "\",Schedule.LAST);\n");
				sb.append(schedule + ".scheduleActionAtEnd(" + variable + ",\"writeToFile\");\n");
			} else
				throw new IllegalStateException();
		}
		return sb.toString();
	}
	
	//--------------------------------------------------------------------------------
	/** Creates the source code belongs to the simulation stop scheduling.
	 * @return the source code
	 */
	private String scheduleStop() {
		StringBuilder sb = new StringBuilder(schedule + ".");
		if (stopAfterFixInterval) 
			sb.append("scheduleActionAt(" + getFixValue() + ",this,\"" + memberPrefix + "_stop\",Schedule.LAST);\n");
		else
			sb.append("scheduleActionAtInterval(1.0,this,\"" + memberPrefix + "_stop\",Schedule.LAST);\n");
		return sb.toString();
	}
	
	//--------------------------------------------------------------------------------
	/** Returns the string representation (including casting to double) of the <code>stopData</code>
	 *  when it contains (directly or undirectly) a tick number.
	 */
	private String getFixValue() {
		try {
			double d = Double.parseDouble(stopData);
			return String.valueOf(d);
		} catch (NumberFormatException e) {
			String name = new String(stopData);
			CtClass type = null;
			try {
				CtField field = ancestor.getField(stopData);
				type = field.getType();
			} catch (NotFoundException e1) {
				if (name.endsWith("()"))
					name = name.substring(0,name.length()-2);
				CtMethod method = null;
				for (CtMethod m : ancestor.getMethods()) {
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
	
	//--------------------------------------------------------------------------------
	/** Creates the getInitParam() method of the generated model. 
	 * @param model the generated model
	 * @return the Javassist representation of the method
	 * @throws CannotCompileException if the syntax of the method is wrong
	 */
	private CtMethod overrideGetInitParamMethod(CtClass model) throws CannotCompileException {
		StringBuilder sb = new StringBuilder("public String[] getInitParam() {\n");
		sb.append("String[] temp = super.getInitParam();\n");
		sb.append("if (temp == null)\n");
		sb.append("temp = new String[0];\n");
		sb.append("String[] res = new String[temp.length + " + String.valueOf(newParameters.size()) + "];\n");
		sb.append("for (int i = 0;i < temp.length;++i) {\n");
		sb.append("res[i] = temp[i];\n");
		sb.append("}\n");
		for (int i = 0;i < newParameters.size();++i) 
			sb.append("res[temp.length + " + String.valueOf(i) + "] = \"" + newParameters.get(i).getName() + "\";\n");
		sb.append("return res;\n");
		sb.append("}\n");
		CtMethod getInitParamMethod = CtNewMethod.make(sb.toString(),model);
		privateMethods.add(sb.toString() + "\n");
		return getInitParamMethod;
	}
	
	//--------------------------------------------------------------------------------
	/** Creates the getter/setter methods to the new parameters if necessary. 
	 * @param model the generated model
	 * @throws CannotCompileException if the syntax of the methods are wrong
	 */
	private void generateGetterSetterMethods(CtClass model) throws CannotCompileException {
		for (AbstractParameterInfo pi : newParameters) {
			boolean uncapitalize = false;
			String type = null;
			try {
				CtField field = ancestor.getField(pi.getName());
				type = field.getType().getSimpleName();
			} catch (NotFoundException e) {
				// starts with lowercase
				uncapitalize = true;
				try {
					CtField field = ancestor.getField(Util.uncapitalize(pi.getName()));
					type = field.getType().getSimpleName();
				} catch (NotFoundException ex) {
					throw new IllegalStateException(ex);
				}
			}
			try {
				ancestor.getMethod("get" + pi.getName(),getDescriptor(type,true));
			} catch (NotFoundException e) {
				CtMethod getMethod = generateGetter(pi,type,uncapitalize,model);
				model.addMethod(getMethod);
			}
			try {
				ancestor.getMethod("set" + pi.getName(),getDescriptor(type,false));
			} catch (NotFoundException e) {
				CtMethod setMethod = generateSetter(pi,type,uncapitalize,model);
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
	private CtMethod generateGetter(AbstractParameterInfo info, String type, boolean uncapitalize, CtClass model) throws CannotCompileException {
		String uName = uncapitalize ? Util.uncapitalize(info.getName()) : info.getName(); 
		StringBuilder sb = new StringBuilder("public ");
		sb.append(type);
		sb.append(" get");
		sb.append(info.getName());
		sb.append("() {\n");
		sb.append("return " + uName + ";\n");
		sb.append("}\n");
		CtMethod getter = CtNewMethod.make(sb.toString(),model);
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
	private CtMethod generateSetter(AbstractParameterInfo info, String type, boolean uncapitalize, CtClass model) throws CannotCompileException {
		String uName = uncapitalize ? Util.uncapitalize(info.getName()) : info.getName();
		StringBuilder sb = new StringBuilder("public void set");
		sb.append(info.getName());
		sb.append("(" + type + " " + uName + ") {\n");
		sb.append("this." + uName + " = " + uName + ";\n");
		sb.append("}\n");
		CtMethod setter = CtNewMethod.make(sb.toString(),model);
		privateMethods.add(sb.toString() + "\n");
		return setter;
	}
	
	//--------------------------------------------------------------------------------
	/** Returns the descriptor of a getter/setter method.
	 * @see http://java.sun.com/docs/books/jvms/second_edition/html/ClassFile.doc.html
	 * @param type the name of the type belongs to the getter/setter method
	 * @param getter true, if it is need the descriptor of the getter method 
	 */
	private String getDescriptor(String type, boolean getter) {
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
	private boolean isNumeric(RecordableInfo info) { return numericTypes.contains(info.getType()); }
	
	//----------------------------------------------------------------------------------------------------
	private boolean isPrimitive(RecordableInfo info) {
		if (Boolean.TYPE.equals(info.getType())) return true;
		return numericTypes.contains(info.getType()) && Character.isLowerCase(info.getType().getSimpleName().charAt(0));
	}
	
	//----------------------------------------------------------------------------------------------------
	private String parseRecorderType(String recordType, String[] arg) {
		if (recordType.equals(ITERATION) || recordType.equals(RUN))
			return recordType;
		if (recordType.startsWith(ITERATION_INTERVAL) || recordType.startsWith(CONDITION)) {
			String[] parts = recordType.split(":");
			arg[0] = parts[1];
			return parts[0];
		}
		return null;
	}
	
	//----------------------------------------------------------------------------------------------------
	private String parseWriteType(String writeType, long[] arg) {
		if (writeType.equals(RECORD) || writeType.equals(RUN))
			return writeType;
		if (writeType.startsWith(ITERATION_INTERVAL)) {
			String[] parts = writeType.split(":");
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
