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
package ai.aitia.meme.paramsweep.parser;

import java.util.ArrayList;
import java.util.List;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;
import ai.aitia.meme.paramsweep.gui.info.GeneratedMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.OperatorGeneratedMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.ParameterInfo;
import ai.aitia.meme.paramsweep.gui.info.ScriptGeneratedMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.SimpleGeneratedMemberInfo;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings;
import ai.aitia.meme.paramsweep.platform.PlatformManager.PlatformType;
import ai.aitia.meme.paramsweep.platform.repast.impl.ModelGenerator;
import ai.aitia.meme.paramsweep.utils.UserDefinedVariable;
import ai.aitia.meme.paramsweep.utils.Util;

/** This class checks the syntax of a script built from the members of a
 *  model. It is uses {@link ai.aitia.meme.paramsweep.platform.repast.impl.ModelGenerator ModelGenerator}
 *  class and Javassist to perform the checking operation.
 */
public class ScriptParser {
	
	//=====================================================================================
	// members
	
	/** Standard prefix of the names of generated members (except statistics and scripts). */  
	public static final String memberPrefix = "aitiaGenerated";
	
	public static final String EXCEPTION_START 	= "[SCRIPT EXCEPTION]";
	public static final String EXCEPTION_END	= "[END OF SCRIPT EXCEPTION]";
	/** This object controls bytecode modification with Javassist. */
	private ClassPool pool = null;
	/** The Javassist representation of the model class. */
	private CtClass ancestor = null;
	/** The class loader object that is used by the application to load the model-related
	 *  classes.
	 */
	private ClassLoader loader = null;
	/** The list of information objects of all members of the model that can be
	 *  used in a script. 
	 */
	private List<GeneratedMemberInfo> generatedMembers = null;
	/** The list of information objects of the new (defined by this wizard) parameters. */
	private List<ParameterInfo> newParameters = null;
	private List<String> generatedScripts = new ArrayList<String>();

	
	//=====================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------
	/** Constructor.
	 * @param pool this object controls bytecode modification with Javassist
	 * @param ancestor the Javassist representation of the model class
	 * @param loader the class loader object that is used by the application to load the model-related
	 *  			 classes
	 * @param generatedMembers the list of information objects of all members of the model that can be
	 *  					   used in a script. 
	 * @param newParameters the list of information objects of the new (defined by this wizard) parameters
	 */
	public ScriptParser(ClassPool pool, CtClass ancestor, ClassLoader loader, List<GeneratedMemberInfo> generatedMembers,
						List<ParameterInfo> newParameters) {
		if (pool == null)
			throw new IllegalArgumentException("'pool' is null");
		if (ancestor == null)
			throw new IllegalArgumentException("'ancestor' is null");
		if (loader == null)
			throw new IllegalArgumentException("'loader' is null");
		if (generatedMembers == null)
			throw new IllegalArgumentException("'generatedMembers' is null");
		this.pool = pool;
		this.ancestor = ancestor;
		this.loader = loader;
		this.generatedMembers = generatedMembers;
		this.newParameters = newParameters;
	}
	
	//-------------------------------------------------------------------------------------
	/** Checks the syntax of the script specified by <code>sgmi</code>. It uses
	 *  {@link ai.aitia.meme.paramsweep.platform.repast.impl.ModelGenerator#checkScript(ScriptGeneratedMemberInfo,String[]) 
	 *  ModelGenerator.checkScript()} method to perform the checking operations.<br>
	 *  If the source of the script is correct, then it completes this source with runtime
	 *  error handling. 
	 * @param sgmi the information object of a script
	 * @param errors output parameter for errors
	 * @return the final version of the info object.
	 */
	public ScriptGeneratedMemberInfo checkScript(ScriptGeneratedMemberInfo sgmi, List<String> errors) {
		for (GeneratedMemberInfo mi : generatedMembers) {
			if (sgmi.getSource().contains(mi.getName())) 
				sgmi.addReference(mi);
		}
		String[] modelName = new String[1];
		try {
			checkScript(sgmi,modelName);
		} catch (CannotCompileException e) {
			String err = e.getReason() == null || e.getReason().length() == 0 ? Util.getLocalizedMessage(e) : e.getReason();
			if (err == null || err.length() == 0) 
				err = e.toString();
			err = err.replaceAll(modelName[0]," the model");
			errors.add(err);
			return null;
		} catch (Exception e) {
			String err = e.getLocalizedMessage() == null || e.getLocalizedMessage().length() == 0 ? e.toString() : Util.getLocalizedMessage(e);
			err = err.replaceAll(modelName[0]," the model");
			errors.add(err);
			return null;
		}
		
		// try the new model
		try {
			Class.forName(modelName[0],true,loader);
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException(e);
		} catch (VerifyError e) {
			//TODO: j� �s �rthet� hiba�zenetek
			String err = Util.getLocalizedMessage(e);
			String mN = modelName[0].replace('.','/');
			String normalName = mN.substring(0,mN.lastIndexOf("__")) + ModelGenerator.modelSuffix;
			err = err.replaceAll(mN,normalName);
			errors.add(err);
			return null;
		}
		sgmi.setSource(completeSource(sgmi.getName(),sgmi.getSource()));
		return sgmi;
	}
	
	//----------------------------------------------------------------------------------------------------
	public String checkVariableInitCode(final UserDefinedVariable variable, final List<String> imports) {
		String[] modelName = new String[1];
		try {
			checkVariableInitCode(variable,imports,modelName);
		} catch (CannotCompileException e) {
			String err = e.getReason() == null || e.getReason().length() == 0 ? Util.getLocalizedMessage(e) : e.getReason();
			if (err == null || err.length() == 0) 
				err = e.toString();
			err = err.replaceAll(modelName[0]," the model");
			return err; 
		} catch (Exception e) {
			String err = e.getLocalizedMessage() == null || e.getLocalizedMessage().length() == 0 ? e.toString() : Util.getLocalizedMessage(e);
			err = err.replaceAll(modelName[0]," the model");
			return err;
		}
		
		// try the new model
		try {
			Class.forName(modelName[0],true,loader);
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException(e);
		} catch (VerifyError e) {
			//TODO: j� �s �rthet� hiba�zenetek
			String err = Util.getLocalizedMessage(e);
			String mN = modelName[0].replace('.','/');
			String normalName = mN.substring(0,mN.lastIndexOf("__")) + ModelGenerator.modelSuffix;
			err = err.replaceAll(mN,normalName);
			return err;
		}
		return null;
	}

	//--------------------------------------------------------------------------------
	/** Checks the syntax of the script specified by <code>sgmi</code>.
	 * @param sgmi the information object of the script
	 * @param modelName output parameter <code>modelName[0]</code> contains the name of 
	 *                  the generated (dummy) class
	 * @throws CannotCompileException if the syntax of the script is wrong
	 */
	private void checkScript(ScriptGeneratedMemberInfo sgmi, String[] modelName) throws CannotCompileException, Exception {
		String timestamp = Util.getTimeStamp();
		String dummyName = ancestor.getName() + "__Dummy_" + timestamp;
		modelName[0] = dummyName;
		importPackages(sgmi);
		CtClass model = pool.makeClass(dummyName,ancestor);

		if (newParameters != null) {
			if (PlatformSettings.getPlatformType() == PlatformType.REPAST) {
				CtMethod getInitParamMethod = overrideGetInitParamMethod(model);
				model.addMethod(getInitParamMethod);
			}
			
			generateGetterSetterMethods(model);
		}
		generateUserVariableFields(sgmi,model);
		generateScript(sgmi,model);
		generateTestMethod(sgmi,model);
		model.stopPruning(true);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void checkVariableInitCode(final UserDefinedVariable variable, final List<String> imports, final String[] modelName) 
																										throws CannotCompileException, Exception {
		String timestamp = Util.getTimeStamp();
		String dummyName = ancestor.getName() + "__Dummy_" + timestamp;
		modelName[0] = dummyName;
		importPackages(imports);
		CtClass model = pool.makeClass(dummyName,ancestor);
		generateInitCodeTestMethod(variable,model);
		model.stopPruning(true);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void importPackages(final List<String> imports) {
		pool.importPackage("ai.aitia.meme.paramsweep.platform.repast.impl");
		pool.importPackage("ai.aitia.meme.paramsweep.platform.custom.impl");
		pool.importPackage("ai.aitia.meme.paramsweep.platform.simphony2.impl");
		if (PlatformSettings.getPlatformType() == PlatformType.REPAST) {
			pool.importPackage("uchicago.src.sim.engine");
			pool.importPackage("uchicago.src.sim.analysis");
		}
		for (final String imp : imports) {
			final int index = imp.lastIndexOf('.');
			if (index == -1) continue;
			final String candidate = imp.substring(0,index);
			if ("java.lang".equals(candidate)) continue;
			else 
				pool.importPackage(candidate);
		}
	}

	
	//--------------------------------------------------------------------------------
	/** Collects the import declarations from the script specified by <code>sgmi</code>
	 *  and adds to the <code>pool</code> (and the source).
	 */
	private void importPackages(ScriptGeneratedMemberInfo sgmi) {
		List<String> imports = new ArrayList<String>();
		imports.add("ai.aitia.meme.paramsweep.platform.repast.impl");
		imports.add("ai.aitia.meme.paramsweep.platform.custom.impl");
		imports.add("ai.aitia.meme.paramsweep.platform.simphony2.impl");
		if (PlatformSettings.getPlatformType() == PlatformType.REPAST) {
			imports.add("uchicago.src.sim.engine");
			imports.add("uchicago.src.sim.analysis");
		}
		importPackagesImpl(sgmi,imports);
		for (String imp : imports) 
			pool.importPackage(imp);
	}
	
	//--------------------------------------------------------------------------------
	/** Collects the import declarations from the script specified by <code>gmi</code>
	 *  to the output parameter <code>imports</code>.
	 */
	private void importPackagesImpl(GeneratedMemberInfo gmi, List<String> imports) {
		if (gmi instanceof SimpleGeneratedMemberInfo) {
			SimpleGeneratedMemberInfo sgmi = (SimpleGeneratedMemberInfo) gmi;
			for (GeneratedMemberInfo _gmi : sgmi.getReferences())
				importPackagesImpl(_gmi,imports);
		} else if (gmi instanceof OperatorGeneratedMemberInfo) {
			OperatorGeneratedMemberInfo ogmi = (OperatorGeneratedMemberInfo) gmi;
			for (GeneratedMemberInfo _gmi : ogmi.getReferences())
				importPackagesImpl(_gmi,imports);
		} else if (gmi instanceof ScriptGeneratedMemberInfo) {
			ScriptGeneratedMemberInfo sgmi = (ScriptGeneratedMemberInfo) gmi;
			for (GeneratedMemberInfo _gmi : sgmi.getReferences())
				importPackagesImpl(_gmi,imports);
			List<String> newImports = sgmi.getImports();
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
		return getInitParamMethod;
	}
	
	//--------------------------------------------------------------------------------
	/** Creates the getter/setter methods to the new parameters if necessary. 
	 * @param model the generated model
	 * @throws CannotCompileException if the syntax of the methods are wrong
	 */
	private void generateGetterSetterMethods(CtClass model) throws CannotCompileException {
		for (ParameterInfo pi : newParameters) {
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
	private CtMethod generateGetter(ParameterInfo info, String type, boolean uncapitalize, CtClass model) throws CannotCompileException {
		String uName = uncapitalize ? Util.uncapitalize(info.getName()) : info.getName(); 
		StringBuilder sb = new StringBuilder("public ");
		sb.append(type);
		sb.append(" get");
		sb.append(info.getName());
		sb.append("() {\n");
		sb.append("return " + uName + ";\n");
		sb.append("}\n");
		CtMethod getter = CtNewMethod.make(sb.toString(),model);
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
	private CtMethod generateSetter(ParameterInfo info, String type, boolean uncapitalize, CtClass model) throws CannotCompileException {
		String uName = uncapitalize ? Util.uncapitalize(info.getName()) : info.getName();
		StringBuilder sb = new StringBuilder("public void set");
		sb.append(info.getName());
		sb.append("(" + type + " " + uName + ") {\n");
		sb.append("this." + uName + " = " + uName + ";\n");
		sb.append("}\n");
		CtMethod setter = CtNewMethod.make(sb.toString(),model);
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
	
	//--------------------------------------------------------------------------------
	/** Creates the method of the statistic instance specified by <code>mi</code>. It also
	 *  creates all referenced generated statistic instance or script, too.
	 * @param mi the information object of the statistic instance
	 * @param model the generated model
	 * @throws CannotCompileException if the syntax of the method (or other related generated method) is wrong
	 */
	private void generateStatistic(SimpleGeneratedMemberInfo mi, CtClass model) throws CannotCompileException {
		if (generatedScripts.contains(mi.getName())) return;
		List<GeneratedMemberInfo> references = mi.getReferences();
		for (GeneratedMemberInfo gmi : references) {
			if (gmi instanceof SimpleGeneratedMemberInfo) 
				generateStatistic((SimpleGeneratedMemberInfo)gmi,model);
			else if (gmi instanceof OperatorGeneratedMemberInfo)
				generateOperator((OperatorGeneratedMemberInfo)gmi,model);
			else 
				generateScript((ScriptGeneratedMemberInfo)gmi,model);
		}
		String src = "private " + mi.getJavaType().getCanonicalName() + " " + mi.getName() + " {\n";
		src += mi.getSource() + "}\n";
		CtMethod m = CtNewMethod.make(src,model);
		model.addMethod(m);
		generatedScripts.add(mi.getName());
	}
	
	//----------------------------------------------------------------------------------------------------
	private void generateOperator(OperatorGeneratedMemberInfo mi, CtClass model) throws CannotCompileException {
		if (generatedScripts.contains(mi.getName())) return;
		List<GeneratedMemberInfo> references = mi.getReferences();
		for (GeneratedMemberInfo gmi : references) {
			if (gmi instanceof OperatorGeneratedMemberInfo)
				generateOperator((OperatorGeneratedMemberInfo)gmi,model);
			else if (gmi instanceof SimpleGeneratedMemberInfo)
				generateStatistic((SimpleGeneratedMemberInfo)gmi,model);
			else
				generateScript((ScriptGeneratedMemberInfo)gmi,model);
		}
		String src = "private " + mi.getJavaType().getCanonicalName() + " " + mi.getName() + " {\n";
		src += mi.getSource() + "}\n";
		CtMethod m = CtNewMethod.make(src,model);
		model.addMethod(m);
		generatedScripts.add(mi.getName());
	}
	
	//--------------------------------------------------------------------------------
	/** Creates the method of the script specified by <code>mi</code>. It also
	 *  creates all referenced generated statistic instance or script, too.
	 * @param mi the information object of the script
	 * @param model the generated model
	 * @throws CannotCompileException if the syntax of the method (or other related generated method) is wrong
	 */
	private void generateScript(ScriptGeneratedMemberInfo mi, CtClass model) throws CannotCompileException {
		if (generatedScripts.contains(mi.getName())) return;
		List<GeneratedMemberInfo> references = mi.getReferences();
		for (GeneratedMemberInfo gmi : references) {
			if (gmi instanceof ScriptGeneratedMemberInfo) 
				generateScript((ScriptGeneratedMemberInfo)gmi,model);
			else if (gmi instanceof SimpleGeneratedMemberInfo) 
				generateStatistic((SimpleGeneratedMemberInfo)gmi,model);
			else
				generateOperator((OperatorGeneratedMemberInfo)gmi,model);
		}
		String src = "private " + mi.getJavaType().getCanonicalName() + " " + mi.getName() + "{\n";
		src += mi.getSource() + "}\n";
		CtMethod m = CtNewMethod.make(src,model);
		model.addMethod(m);
		generatedScripts.add(mi.getName());
	}
	
	//--------------------------------------------------------------------------------
	/** Creates a test method that calls the script specified by <code>sgmi</code>.
	 * @param sgmi the information object of the examined script
	 * @param model the generated model
	 * @throws CannotCompileException if the syntax of the methods are wrong
	 */
	private void generateTestMethod(ScriptGeneratedMemberInfo sgmi, CtClass model) throws CannotCompileException {
		String src = "public void " + memberPrefix + "_test() {";
		src += generateUserVariableInitialization(sgmi);
		src += sgmi.getName() + ";}";
		CtMethod m = CtNewMethod.make(src,model);
		model.addMethod(m);
	}
	
	//--------------------------------------------------------------------------------
	private void generateInitCodeTestMethod(final UserDefinedVariable variable, final CtClass model) throws CannotCompileException {
		final CtField field = CtField.make("private " + variable.getType().getCanonicalName() + " " + variable.getName() + ";",model);
		model.addField(field);
		String src = "public void " + memberPrefix + "_testInit() {";
		src += variable.getInitializationCode() + "}";
		CtMethod m = CtNewMethod.make(src,model);
		model.addMethod(m);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void generateUserVariableFields(final ScriptGeneratedMemberInfo sgmi, final CtClass model) throws CannotCompileException {
		final List<UserDefinedVariable> variables = collectVariables(sgmi);
		for (final UserDefinedVariable variable : variables) {
			final CtField field = CtField.make("private " + variable.getType().getCanonicalName() + " " + variable.getName() + ";",model);
			model.addField(field);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private String generateUserVariableInitialization(final ScriptGeneratedMemberInfo sgmi) {
		final List<UserDefinedVariable> variables = collectVariables(sgmi);
		final StringBuilder code = new StringBuilder();
		for (final UserDefinedVariable variable : variables) 
			code.append(variable.getInitializationCode()).append("\n");
		return code.toString();
	}
	
	//----------------------------------------------------------------------------------------------------
	private List<UserDefinedVariable> collectVariables(final GeneratedMemberInfo gmi) {
		final List<UserDefinedVariable> result = new ArrayList<UserDefinedVariable>();
		final List<GeneratedMemberInfo> references = gmi.getReferences();
		for (final GeneratedMemberInfo info : references) 
			Util.addAllDistinct(result,collectVariables(info));
		if (gmi instanceof ScriptGeneratedMemberInfo) {
			ScriptGeneratedMemberInfo sgmi = (ScriptGeneratedMemberInfo) gmi;
			Util.addAllDistinct(result,sgmi.getUserVariables());
		}
		return result;
	}
	
	//-------------------------------------------------------------------------------------
	/** Completes the source code <code>original</code> with runtime error handling.
	 * @param name the name of a script
	 * @param original the source code of a script
	 * @return the modified source code
	 */
	private String completeSource(String name, String original) {
		StringBuilder sb = new StringBuilder();
		sb.append("try {\n");
		sb.append(original);
		sb.append("\n}\n");
		sb.append("catch (Throwable t) {\n");
		sb.append("System.err.println(\"" + EXCEPTION_START + "\");\n");
        sb.append("System.err.println(\"Name: " + name + "\");\n");
        sb.append("System.err.println(t.getLocalizedMessage());\n");
		sb.append("t.printStackTrace();\n");
		sb.append("System.err.println(\"" + EXCEPTION_END + "\");\n");
		sb.append("throw new RuntimeException(t);\n");
		sb.append("}\n");
		return sb.toString();
	}
}
