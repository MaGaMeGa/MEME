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
package ai.aitia.meme.paramsweep.generator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ai.aitia.meme.paramsweep.colt.DoubleArrayList;
import ai.aitia.meme.paramsweep.colt.SortedDoubleArrayList;
import ai.aitia.meme.paramsweep.gui.info.GeneratedMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.MemberInfo;
import ai.aitia.meme.paramsweep.gui.info.SimpleGeneratedMemberInfo;
import ai.aitia.meme.paramsweep.plugin.IStatisticsPlugin;
import ai.aitia.meme.paramsweep.utils.Util;
import ai.aitia.meme.paramsweep.utils.Utilities;
import ai.aitia.meme.utils.Utils;

/** This class generates the information object of a statistic instance defined with
 *  the wizard.
 */
public class StatisticsInfoGenerator implements IStatisticInfoGenerator {

	//=====================================================================================
	// members
	
	/** The general prefix of local variable names. */
	protected static final String LOCAL_PREFIX = "localVariable";
	public static final String EXCEPTION_START = "[STAT EXCEPTION]";
	public static final String EXCEPTION_END	= "[END OF STAT EXCEPTION]";

	/** This data structure maps the possible parameter types of the statistics to 
	 *  the type of the informations objects. The latter can be MemberInfo or list of
	 *  MemberInfo.
	 */
	protected static HashMap<Class,Class> parameterTypesMap = new HashMap<Class,Class>();
	
	static {
		parameterTypesMap.put(DoubleArrayList.class,List.class); // List of MemberInfo
		parameterTypesMap.put(SortedDoubleArrayList.class,List.class);
		parameterTypesMap.put(cern.colt.list.DoubleArrayList.class,List.class);
//		parameterTypesMap.put(ObjectList.class,List.class);
		parameterTypesMap.put(byte[].class,List.class);
		parameterTypesMap.put(short[].class,List.class);
		parameterTypesMap.put(int[].class,List.class);
		parameterTypesMap.put(long[].class,List.class);
		parameterTypesMap.put(float[].class,List.class);
		parameterTypesMap.put(double[].class,List.class);
		parameterTypesMap.put(Byte[].class,List.class);
		parameterTypesMap.put(Short[].class,List.class);
		parameterTypesMap.put(Integer[].class,List.class);
		parameterTypesMap.put(Long[].class,List.class);
		parameterTypesMap.put(Float[].class,List.class);
		parameterTypesMap.put(Double[].class,List.class);
		parameterTypesMap.put(Byte.TYPE,MemberInfo.class);
		parameterTypesMap.put(Short.TYPE,MemberInfo.class);
		parameterTypesMap.put(Integer.TYPE,MemberInfo.class);
		parameterTypesMap.put(Long.TYPE,MemberInfo.class);
		parameterTypesMap.put(Float.TYPE,MemberInfo.class);
		parameterTypesMap.put(Double.TYPE,MemberInfo.class);
		parameterTypesMap.put(Byte.class,MemberInfo.class);
		parameterTypesMap.put(Short.class,MemberInfo.class);
		parameterTypesMap.put(Integer.class,MemberInfo.class);
		parameterTypesMap.put(Long.class,MemberInfo.class);
		parameterTypesMap.put(Float.class,MemberInfo.class);
		parameterTypesMap.put(Double.class,MemberInfo.class);
	}
	
	/** The statistic plugin. */
	protected IStatisticsPlugin stat = null;
	protected long localID = 0;
	/** Error storage. */
	protected List<String> error = null;
	
	//=====================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------
	/** Constructor
	 * @param stat the statistic plugin
	 * @param miList the list of information objects of all possible actual parameters
	 */
	public StatisticsInfoGenerator(IStatisticsPlugin stat) {
		this.stat = stat;
		this.localID = 0;
		this.error = new ArrayList<String>();
	}
	
	
	//-------------------------------------------------------------------------------------
	public List<String> getError() { return error; }
 	
	//-------------------------------------------------------------------------------------
	/** Checks if the statistic plugin <code>stat</code> is valid. */
	public static boolean checkPlugin(IStatisticsPlugin stat) {
		int nr = stat.getNumberOfParameters();
		return nr == stat.getParameterNames().size() && nr == stat.getParameterTypes().size();
	}
	
	//-------------------------------------------------------------------------------------
	/** Generates the information object from the <code>stat</code> and its 
	 *  actual parameters
	 * @param actParameters the actual parameters (MemberInfo or List&lt;MemberInfo&gt;)
	 * @return the created information object (or null if any problem occures)
	 */
	@SuppressWarnings("unchecked")
	public SimpleGeneratedMemberInfo generateInfoObject(String origName, Object... actParameters) {
		if (actParameters.length != stat.getNumberOfParameters()) {
			error.add("Invalid number of parameters!");
			return null;
		}
		List<String> typeErrors = checkParameterTypes(actParameters);
		if (typeErrors != null) {
			error.addAll(typeErrors);
			return null;
		}
		String name = origName;
		if (!name.endsWith("()"))
				name += "()";
		SimpleGeneratedMemberInfo result = new SimpleGeneratedMemberInfo(name,stat.getReturnType().getSimpleName(),stat.getReturnType());
		StringBuilder sb = new StringBuilder();
		sb.append("try {\n");
		StringBuilder call = new StringBuilder(name.substring(0,name.length() - 2) + "(");
		List<Class> types = stat.getParameterTypes();
		List<String> locals = new ArrayList<String>();
		for (int i = 0;i < actParameters.length;++i) {
			if (actParameters[i] instanceof List) {
				if (types.get(i).equals(SortedDoubleArrayList.class))
					locals.add(generateDoubleArrayList((List)actParameters[i],sb,call,result,true));
				else if (types.get(i).equals(DoubleArrayList.class) || types.get(i).equals(cern.colt.list.DoubleArrayList.class))
					locals.add(generateDoubleArrayList((List)actParameters[i],sb,call,result,false));
//				else if (types.get(i).equals(ObjectList.class))
//					locals.add(generateObjectList((List)actParameters[i],sb,call,result));
				else if (types.get(i).equals(byte[].class))
					locals.add(generateArray("byte",(List)actParameters[i],sb,call,result));
				else if (types.get(i).equals(short[].class))
					locals.add(generateArray("short",(List)actParameters[i],sb,call,result));
				else if (types.get(i).equals(int[].class))
					locals.add(generateArray("int",(List)actParameters[i],sb,call,result));
				else if (types.get(i).equals(long[].class))
					locals.add(generateArray("long",(List)actParameters[i],sb,call,result));
				else if (types.get(i).equals(float[].class))
					locals.add(generateArray("float",(List)actParameters[i],sb,call,result));
				else if (types.get(i).equals(double[].class))
					locals.add(generateArray("double",(List)actParameters[i],sb,call,result));
				else if (types.get(i).equals(Byte[].class))
					locals.add(generateArray("Byte",(List)actParameters[i],sb,call,result));
				else if (types.get(i).equals(Short[].class))
					locals.add(generateArray("Short",(List)actParameters[i],sb,call,result));
				else if (types.get(i).equals(Integer[].class))
					locals.add(generateArray("Integer",(List)actParameters[i],sb,call,result));
				else if (types.get(i).equals(Long[].class))
					locals.add(generateArray("Long",(List)actParameters[i],sb,call,result));
				else if (types.get(i).equals(Float[].class))
					locals.add(generateArray("Float",(List)actParameters[i],sb,call,result));
				else if (types.get(i).equals(Double[].class))
					locals.add(generateArray("Double",(List)actParameters[i],sb,call,result));
			} else if (actParameters[i] instanceof MemberInfo) {
				MemberInfo mi = (MemberInfo) actParameters[i];
				call.append(Utilities.name(mi) + ",");
				if (mi instanceof GeneratedMemberInfo)
					result.addReference((GeneratedMemberInfo)mi);
				if (types.get(i).equals(Byte.TYPE))
					locals.add(generateByte(mi,sb));
				else if (types.get(i).equals(Short.TYPE))
					locals.add(generateShort(mi,sb));
				else if (types.get(i).equals(Integer.TYPE))
					locals.add(generateInt(mi,sb));
				else if (types.get(i).equals(Long.TYPE))
					locals.add(generateLong(mi,sb));
				else if (types.get(i).equals(Float.TYPE))
					locals.add(generateFloat(mi,sb));
				else if (types.get(i).equals(Double.TYPE))
					locals.add(generateDouble(mi,sb));
				else if (types.get(i).equals(Byte.class))
					locals.add(generateByteWrapper(mi,sb));
				else if (types.get(i).equals(Short.class))
					locals.add(generateShortWrapper(mi,sb));
				else if (types.get(i).equals(Integer.class))
					locals.add(generateIntegerWrapper(mi,sb));
				else if (types.get(i).equals(Long.class))
					locals.add(generateLongWrapper(mi,sb));
				else if (types.get(i).equals(Float.class))
					locals.add(generateFloatWrapper(mi,sb));
				else if (types.get(i).equals(Double.class))
					locals.add(generateDoubleWrapper(mi,sb));
			}
		}
		sb.append("return " + stat.getFullyQualifiedName() + "(" + Utils.join(locals,",") + ");\n");
		sb.append("}\n");
		sb.append("catch (Throwable t) {\n");
		sb.append("System.err.println(\"" + EXCEPTION_START + "\");\n");
        sb.append("System.err.println(\"Name: " + name + "\");\n");
        sb.append("System.err.println(t.getLocalizedMessage());\n");
		sb.append("t.printStackTrace();\n");
		sb.append("System.err.println(\"" + EXCEPTION_END + "\");\n");
		sb.append("throw new RuntimeException(t);\n");
		sb.append("}\n");
		if (call.toString().endsWith(",")) call.deleteCharAt(call.length() - 1);
		call.append(")");
		result.setCall(call.toString());
		result.setSource(sb.toString());
		return result;
	}
	
	//=====================================================================================
	// private methods
	
	//-------------------------------------------------------------------------------------
	/** Checks if the types of formal and actual parameters are equivalent.<br>
	 * Pre-condition: actParameters.length == stat.getNumberOfParameters()
	 * @param actParameters the actual parameters (MemberInfo or List&lt;MemberInfo&gt;)
	 */
	protected List<String> checkParameterTypes(Object... actParameters) {
		List<String> errors = new ArrayList<String>();
		List<Class> types = stat.getParameterTypes();
		for (int i = 0;i < types.size();++i) {
			Class expectedType = MemberInfo.class;
			expectedType = parameterTypesMap.get(types.get(i));
			if (expectedType.isInstance(actParameters[i])) {
				if (expectedType.equals(List.class)) {
					List list = (List) actParameters[i];
					for (Object o : list) {
						if (!(o instanceof MemberInfo)) {
							error.add(String.format("Invalid element in list (parameter %d): a(n) %s instead of MemberInfo",i,actParameters[i].getClass().getName()));
							break;
						}
					}
				}
			} else 
				errors.add(String.format("Unexpected type: %s (instead of %s)",actParameters[i].getClass(),expectedType));
		}
		return errors.size() == 0 ? null : errors;
	}
	
	//------------------------------------------------------------------------------------
	/** Creates the source code of a DoubleArrayList declaration and initialization.
	 * @param list the information objects of the elements of the DoubleArrayList 
	 * @param source the object that stores the source code
	 * @param call the object that stores the displayable name of the statistic instance (name with actual parameters)
	 * @param result the information object generated by this class
	 * @param sorted flag that determines whether the DoubleArrayList must be sorted or not
	 * @return the name of the created local variable
	 */
	private String generateDoubleArrayList(List<MemberInfo> list, StringBuilder source, StringBuilder call, SimpleGeneratedMemberInfo result, boolean sorted) {
		String local = LOCAL_PREFIX + String.valueOf(localID++);
		source.append("ai.aitia.meme.paramsweep.colt.DoubleArrayList ");
		source.append(local + " = new ai.aitia.meme.paramsweep.colt.DoubleArrayList();\n");
		call.append("[");
		for (MemberInfo mi : list) {
			call.append(Utilities.name(mi) + ",");
			source.append(local + ".add("); 
			if (!(mi instanceof GeneratedMemberInfo)){
				source.append(Util.GENERATED_MODEL_MODEL_FIELD_NAME).append(".");
			}
			source.append(mi.getName()).append(");\n");
			if (mi instanceof GeneratedMemberInfo)
				result.addReference((GeneratedMemberInfo)mi);
		}
		if (sorted) {
			source.append(local + ".sort();\n");
		}
		call.deleteCharAt(call.length() - 1);
		call.append("],");
		return local;
	}
	
	//----------------------------------------------------------------------------------------------------
//	private String generateObjectList(List<MemberInfo> list, StringBuilder source, StringBuilder call, SimpleGeneratedMemberInfo result) {
//		String local = LOCAL_PREFIX + String.valueOf(localID++);
//		source.append("ai.aitia.meme.paramsweep.colt.ObjectList ");
//		source.append(local + " = new ai.aitia.meme.paramsweep.colt.ObjectList();\n");
//		call.append("[");
//		for (MemberInfo mi : list) {
//			call.append(Utilities.name(mi) + ",");
//			source.append(local + ".add("); source.append(mi.getName()); source.append(");\n");
//			if (mi instanceof GeneratedMemberInfo)
//				result.addReference((GeneratedMemberInfo)mi);
//		}
//		call.deleteCharAt(call.length() - 1);
//		call.append("],");
//		return local;
//	}
	
	//------------------------------------------------------------------------------------
	/** Creates the source code of an array declaration and initialization.
	 * @param type the type of the array
	 * @param list the information objects of the elements of the array 
	 * @param source the object that stores the source code
	 * @param call the object that stores the displayable name of the statistic instance (name with actual parameters)
	 * @param result the information object generated by this class
	 * @return the name of the created local variable
	 */
	private String generateArray(String type, List<MemberInfo> list, StringBuilder source, StringBuilder call, SimpleGeneratedMemberInfo result) {
		String local0 = LOCAL_PREFIX + String.valueOf(localID++);
		source.append("ai.aitia.meme.paramsweep.colt.DoubleArrayList ");
		source.append(local0 + " = new ai.aitia.meme.paramsweep.colt.DoubleArrayList();\n");
		call.append("[");
		for (MemberInfo mi : list) {
			call.append(Utilities.name(mi) + ",");
			source.append(local0 + ".add("); source.append(mi.getName()); source.append(");\n");
			if (mi instanceof GeneratedMemberInfo)
				result.addReference((GeneratedMemberInfo)mi);
		}
		String local1 = LOCAL_PREFIX + String.valueOf(localID++);
		source.append(type + "[] " + local1 + " = " + local0 + ".to_" + type + "Array();\n");
		call.deleteCharAt(call.length() - 1);
		call.append("],");
		return local1;

	}
	
	//------------------------------------------------------------------------------------
	/** Creates the source code of an primitive number variable declaration and initialization.
	 * @param type the type of the variable
	 * @param mi the information object of the variable 
	 * @param source the object that stores the source code
	 * @return the name of the created local variable
	 */
	private String generatePrimitiveNumber(String type, MemberInfo mi, StringBuilder source) {
		String local = LOCAL_PREFIX + String.valueOf(localID++);
		source.append(type + " " + local + " = ");
		if (mi.isPrimitive()) {
			if (!mi.getType().equals(type))
				source.append("(" + type + ") ");
			source.append(mi.getName() + ";\n");
		} else
			source.append(mi.getName() + "." + type + "Value();\n");
		return local;
	}
	
	//------------------------------------------------------------------------------------
	/** Creates the source code of a <code>byte</code> variable declaration and initialization.
	 * @param mi the information object of the variable 
	 * @param source the object that stores the source code
	 * @return the name of the created local variable
	 */
	private String generateByte(MemberInfo mi, StringBuilder source) {
		return generatePrimitiveNumber("byte",mi,source);
	}
	
	//------------------------------------------------------------------------------------
	/** Creates the source code of a <code>short</code> variable declaration and initialization.
	 * @param mi the information object of the variable 
	 * @param source the object that stores the source code
	 * @return the name of the created local variable
	 */
	private String generateShort(MemberInfo mi, StringBuilder source) {
		return generatePrimitiveNumber("short",mi,source);
	}
	
	//------------------------------------------------------------------------------------
	/** Creates the source code of an <code>int</code> variable declaration and initialization.
	 * @param mi the information object of the variable 
	 * @param source the object that stores the source code
	 * @return the name of the created local variable
	 */
	private String generateInt(MemberInfo mi, StringBuilder source) {
		return generatePrimitiveNumber("int",mi,source);
	}
	
	//------------------------------------------------------------------------------------
	/** Creates the source code of a <code>long</code> variable declaration and initialization.
	 * @param mi the information object of the variable 
	 * @param source the object that stores the source code
	 * @return the name of the created local variable
	 */
	private String generateLong(MemberInfo mi, StringBuilder source) {
		return generatePrimitiveNumber("long",mi,source);
	}
	
	//------------------------------------------------------------------------------------
	/** Creates the source code of a <code>float</code> variable declaration and initialization.
	 * @param mi the information object of the variable 
	 * @param source the object that stores the source code
	 * @return the name of the created local variable
	 */
	private String generateFloat(MemberInfo mi, StringBuilder source) {
		return generatePrimitiveNumber("float",mi,source);
	}
	
	//------------------------------------------------------------------------------------
	/** Creates the source code of a <code>double</code> variable declaration and initialization.
	 * @param mi the information object of the variable 
	 * @param source the object that stores the source code
	 * @return the name of the created local variable
	 */
	private String generateDouble(MemberInfo mi, StringBuilder source) {
		return generatePrimitiveNumber("double",mi,source);
	}
	
	//------------------------------------------------------------------------------------
	/** Creates the source code of an non-primitive number variable declaration and initialization.
	 * @param type the type of the variable
	 * @param mi the information object of the variable 
	 * @param source the object that stores the source code
	 * @return the name of the created local variable
	 */
	private String generateNumberWrapper(String type, MemberInfo mi, StringBuilder source) {
		String local = LOCAL_PREFIX + String.valueOf(localID++);
		if (type.equals(mi.getType())) {
			source.append(type + " " + local + " = " + mi.getName() + ";\n");
			
		} else {
			source.append(type + " " + local + " = new " + type + "(");
			String primitiveType = primitiveType(type);
			if (mi.isPrimitive()) {
				if (!mi.getType().equals(primitiveType))
					source.append("(" + primitiveType + ") ");
				source.append(mi.getName() + ");\n");
			} else
				source.append(mi.getName() + "." + primitiveType + "Value());\n");
		}
		return local;
	}
	
	//------------------------------------------------------------------------------------
	/** Creates the source code of a <code>Byte</code> variable declaration and initialization.
	 * @param mi the information object of the variable 
	 * @param source the object that stores the source code
	 * @return the name of the created local variable
	 */
	private String generateByteWrapper(MemberInfo mi, StringBuilder source) {
		return generateNumberWrapper("Byte",mi,source);
	}
	
	//------------------------------------------------------------------------------------
	/** Creates the source code of a <code>Short</code> variable declaration and initialization.
	 * @param mi the information object of the variable 
	 * @param source the object that stores the source code
	 * @return the name of the created local variable
	 */
	private String generateShortWrapper(MemberInfo mi, StringBuilder source) {
		return generateNumberWrapper("Short",mi,source);
	}
	
	//------------------------------------------------------------------------------------
	/** Creates the source code of an <code>Integer</code> variable declaration and initialization.
	 * @param mi the information object of the variable 
	 * @param source the object that stores the source code
	 * @return the name of the created local variable
	 */
	private String generateIntegerWrapper(MemberInfo mi, StringBuilder source) {
		return generateNumberWrapper("Integer",mi,source);
	}
	
	//------------------------------------------------------------------------------------
	/** Creates the source code of a <code>Long</code> variable declaration and initialization.
	 * @param mi the information object of the variable 
	 * @param source the object that stores the source code
	 * @return the name of the created local variable
	 */
	private String generateLongWrapper(MemberInfo mi, StringBuilder source) {
		return generateNumberWrapper("Long",mi,source);
	}
	
	//------------------------------------------------------------------------------------
	/** Creates the source code of a <code>Float</code> variable declaration and initialization.
	 * @param mi the information object of the variable 
	 * @param source the object that stores the source code
	 * @return the name of the created local variable
	 */
	private String generateFloatWrapper(MemberInfo mi, StringBuilder source) {
		return generateNumberWrapper("Float",mi,source);
	}
	
	//------------------------------------------------------------------------------------
	/** Creates the source code of a <code>Double</code> variable declaration and initialization.
	 * @param mi the information object of the variable 
	 * @param source the object that stores the source code
	 * @return the name of the created local variable
	 */
	private String generateDoubleWrapper(MemberInfo mi, StringBuilder source) {
		return generateNumberWrapper("Double",mi,source);
	}

	//------------------------------------------------------------------------------------
	/** Returns the primitive type in string format belongs to <code>wrapperType</code>. */
	protected String primitiveType(String wrapperType) {
		if ("Byte".equals(wrapperType)) return "byte";
		if ("Short".equals(wrapperType)) return "short";
		if ("Integer".equals(wrapperType)) return "int";
		if ("Long".equals(wrapperType)) return "long";
		if ("Float".equals(wrapperType)) return "float";
		if ("Double".equals(wrapperType)) return "double";
		return null;
	}
}
