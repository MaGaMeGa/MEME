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
import java.util.Arrays;
import java.util.List;

import ai.aitia.meme.paramsweep.colt.DoubleArrayList;
import ai.aitia.meme.paramsweep.colt.SortedDoubleArrayList;
import ai.aitia.meme.paramsweep.gui.info.GeneratedMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.MemberInfo;
import ai.aitia.meme.paramsweep.gui.info.NLSimpleGeneratedMemberInfo;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings;
import ai.aitia.meme.paramsweep.platform.PlatformManager;
import ai.aitia.meme.paramsweep.plugin.IStatisticsPlugin;
import ai.aitia.meme.paramsweep.utils.PlatformConstants;
import ai.aitia.meme.paramsweep.utils.Utilities;
import ai.aitia.meme.utils.Utils;

public class NLStatisticsInfoGenerator extends StatisticsInfoGenerator {

	//=====================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public NLStatisticsInfoGenerator(IStatisticsPlugin stat) {
		super(stat);
		this.localID = 0;
		this.error = new ArrayList<String>();
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public NLSimpleGeneratedMemberInfo generateInfoObject(String origName, Object... actParameters) {
		if (actParameters.length != stat.getNumberOfParameters()) {
			error.add("Invalid number of parameters!");
			return null;
		}
		
		List<String> typeErrors = checkParameterTypes(actParameters);
		if (typeErrors != null) {
			error.addAll(typeErrors);
			return null;
		}

		NLSimpleGeneratedMemberInfo result = new NLSimpleGeneratedMemberInfo(origName,stat.getReturnType().getSimpleName(),stat.getReturnType());
		result.setSyntaxBody(generateSyntaxBody());
		result.setReportBody(generateReportBody(origName));
		
		StringBuilder sb = new StringBuilder("to-report ");
		sb.append(origName).append("\n");
		StringBuilder call = new StringBuilder(origName + "(");
		List<String> locals = new ArrayList<String>();
		for (int i = 0;i < actParameters.length;++i) {
			if (actParameters[i] instanceof List) 
				locals.add(generateLogoList((List)actParameters[i],sb,call,result));
			else if (actParameters[i] instanceof MemberInfo) {
				MemberInfo mi = (MemberInfo) actParameters[i];
				call.append(Utilities.name(mi) + ",");
				if (mi instanceof GeneratedMemberInfo)
					result.addReference((GeneratedMemberInfo)mi);
				locals.add(generateVariable(mi,sb));
			}
		}
		sb.append("report (").append(PlatformConstants.AITIA_GENERATED_SCRIPTS).append(":").append(origName).append(" ");
		for (String localName : locals) 
			sb.append(localName).append(" ");
		sb.append(")\n");
		sb.append("end\n\n");
		if (call.toString().endsWith(","))
			call.deleteCharAt(call.length() - 1);
		call.append(")");
		result.setCall(call.toString());
		result.setSource(sb.toString());
		return result;
	}
	
	//=====================================================================================
	// private methods
	
	//----------------------------------------------------------------------------------------------------
	private String generateSyntaxBody() {
		StringBuilder sb = new StringBuilder("return Syntax.reporterSyntax(new int[] {");
		for (Class<?> c : stat.getParameterTypes())
			sb.append("Syntax.").append(getNetLogoType(c)).append(",");
		int idx = sb.lastIndexOf(",");
		sb.replace(idx,idx + 1,"");
		sb.append("},");
		sb.append("Syntax.").append(getNetLogoType(stat.getReturnType()));
		sb.append(");\n");
		return sb.toString();
	}
	
	//----------------------------------------------------------------------------------------------------
	private String getNetLogoType(Class<?> clazz) {
		final int majorVersion = majorVersion();
		
		if (Byte.TYPE.equals(clazz) || Byte.class.equals(clazz) || Short.TYPE.equals(clazz) || Short.class.equals(clazz) ||
			Integer.TYPE.equals(clazz) || Integer.class.equals(clazz) || Long.TYPE.equals(clazz) || Long.class.equals(clazz) ||
			Float.TYPE.equals(clazz) || Float.class.equals(clazz) || Double.TYPE.equals(clazz) || Double.class.equals(clazz))
			return 5 == majorVersion ? "NumberType()" : "TYPE_NUMBER";
		else if (Boolean.TYPE.equals(clazz) || Boolean.class.equals(clazz))
			return 5 == majorVersion ? "BooleanType()" : "TYPE_BOOLEAN";
		else if (String.class.equals(clazz))
			return 5 == majorVersion ? "StringType()" : "TYPE_STRING";
		else if (clazz.isArray() && getNetLogoType(clazz.getComponentType()).equals(5 == majorVersion ? "NumberType()" : "TYPE_NUMBER"))
			return 5 == majorVersion ? "ListType()" : "TYPE_LIST";
		else if (cern.colt.list.DoubleArrayList.class.isAssignableFrom(clazz))
			return 5 == majorVersion ? "ListType()" : "TYPE_LIST";
		
		return null;
	}
	
	//----------------------------------------------------------------------------------------------------
	private String generateReportBody(String name) {
		StringBuilder sb = new StringBuilder("try {\n");
		@SuppressWarnings("rawtypes") List<Class> types = stat.getParameterTypes();
		List<String> locals = new ArrayList<String>();
		for (int i = 0;i < types.size();++i) {
			if (types.get(i).equals(SortedDoubleArrayList.class))
				locals.add(eGenerateDoubleArrayList(i,sb,true));
			else if (types.get(i).equals(DoubleArrayList.class) || types.get(i).equals(cern.colt.list.DoubleArrayList.class))
				locals.add(eGenerateDoubleArrayList(i,sb,false));
			else if (types.get(i).equals(byte[].class))
				locals.add(eGenerateArray("byte",i,sb));
			else if (types.get(i).equals(short[].class))
				locals.add(eGenerateArray("short",i,sb));
			else if (types.get(i).equals(int[].class))
				locals.add(eGenerateArray("int",i,sb));
			else if (types.get(i).equals(long[].class))
				locals.add(eGenerateArray("long",i,sb));
			else if (types.get(i).equals(float[].class))
				locals.add(eGenerateArray("float",i,sb));
			else if (types.get(i).equals(double[].class))
				locals.add(eGenerateArray("double",i,sb));
			else if (types.get(i).equals(Byte[].class))
				locals.add(eGenerateArray("Byte",i,sb));
			else if (types.get(i).equals(Short[].class))
				locals.add(eGenerateArray("Short",i,sb));
			else if (types.get(i).equals(Integer[].class))
				locals.add(eGenerateArray("Integer",i,sb));
			else if (types.get(i).equals(Long[].class))
				locals.add(eGenerateArray("Long",i,sb));
			else if (types.get(i).equals(Float[].class))
				locals.add(eGenerateArray("Float",i,sb));
			else if (types.get(i).equals(Double[].class))
				locals.add(eGenerateArray("Double",i,sb));
			else if (types.get(i).equals(Byte.TYPE))
				locals.add(eGenerateInteger("byte",i,sb));
			else if (types.get(i).equals(Short.TYPE))
				locals.add(eGenerateInteger("short",i,sb));
			else if (types.get(i).equals(Integer.TYPE))
				locals.add(eGenerateInteger("int",i,sb));
			else if (types.get(i).equals(Long.TYPE))
				locals.add(eGenerateInteger("long",i,sb));
			else if (types.get(i).equals(Float.TYPE))
				locals.add(eGenerateReal("float",i,sb));
			else if (types.get(i).equals(Double.TYPE))
				locals.add(eGenerateReal("double",i,sb));
			else if (types.get(i).equals(Byte.class))
				locals.add(eGenerateIntegerWrapper("Byte",i,sb));
			else if (types.get(i).equals(Short.class))
				locals.add(eGenerateIntegerWrapper("Short",i,sb));
			else if (types.get(i).equals(Integer.class))
				locals.add(eGenerateIntegerWrapper("Integer",i,sb));
			else if (types.get(i).equals(Long.class))
				locals.add(eGenerateIntegerWrapper("Long",i,sb));
			else if (types.get(i).equals(Float.class))
				locals.add(eGenerateRealWrapper("Float",i,sb));
			else if (types.get(i).equals(Double.class))
				locals.add(eGenerateRealWrapper("Double",i,sb));
		}
		sb.append(returnWithBoxingIfNeed(locals));
		sb.append("}\n");
		sb.append("catch (Exception e) {\n");
		sb.append("System.err.println(\"" + EXCEPTION_START + "\");\n");
		sb.append("System.err.println(\"Name: " + name + "\");\n");
		sb.append("System.err.println(e.getLocalizedMessage());\n");
		sb.append("e.printStackTrace();\n");
		sb.append("System.err.println(\"" + EXCEPTION_END + "\");\n");
		sb.append("throw new ExtensionException(e);\n");
		sb.append("} catch (Throwable t) {\n");
		sb.append("System.err.println(\"" + EXCEPTION_START + "\");\n");
		sb.append("System.err.println(\"Name: " + name + "\");\n");
		sb.append("System.err.println(t.getLocalizedMessage());\n");
		sb.append("t.printStackTrace();\n");
		sb.append("System.err.println(\"" + EXCEPTION_END + "\");\n");
		sb.append("throw new RuntimeException(t);\n");
		sb.append("}\n");
		return sb.toString();
	}
	
	//----------------------------------------------------------------------------------------------------
	private String returnWithBoxingIfNeed(final List<String> locals) {
		final StringBuilder sb = new StringBuilder("return ");
		final boolean primitive = stat.getReturnType().isPrimitive();
		
		final List<Class<?>> problematicWrapperTypes = Arrays.asList(new Class<?>[] { Byte.class, Short.class, Float.class, Long.class });
		final boolean problematicWrapperType = problematicWrapperTypes.contains(stat.getReturnType());
		
		if (primitive || problematicWrapperType) {
			final String boxingStrPrefix = boxingPrefix(stat.getReturnType());
			sb.append("new ").append(boxingStrPrefix);
		}
		sb.append(stat.getFullyQualifiedName() + "(" + Utils.join(locals,",") + ")");
		if (primitive)
			sb.append(")");
		else if (problematicWrapperType) {
			sb.append(unboxingSuffix(stat.getReturnType())).append(")");
		}
		sb.append(";\n");
		return sb.toString();
	}
	
	//----------------------------------------------------------------------------------------------------
	private String boxingPrefix(Class<?> memberType) {
		if (Byte.TYPE.equals(memberType) || Short.TYPE.equals(memberType)) 
			return "Integer((int)";
		if (Integer.TYPE.equals(memberType) || Byte.class.equals(memberType) || Short.class.equals(memberType)) 
			return "Integer(";
		if (Long.TYPE.equals(memberType) || Float.TYPE.equals(memberType))
			return "Double((double)";
		if (Double.TYPE.equals(memberType) || Float.class.equals(memberType) || Long.class.equals(memberType)) 
			return "Double(";
		if (Boolean.TYPE.equals(memberType)) 
			return "Boolean(";
		if (Character.TYPE.equals(memberType))
			return "Character(";
		return memberType.getName() + "(";
	}
	
	//----------------------------------------------------------------------------------------------------
	private String unboxingSuffix(final Class<?> memberType) {
		if (Byte.class.equals(memberType) || Short.class.equals(memberType))
			return ".intValue()";
		else if (Float.class.equals(memberType) || Long.class.equals(memberType))
			return ".doubleValue()";
		else
			return "";
	}
	
	//----------------------------------------------------------------------------------------------------
	private String eGenerateDoubleArrayList(int idx, StringBuilder source, boolean sorted) {
		String temp = LOCAL_PREFIX + String.valueOf(localID++);
		source.append("LogoList ").append(temp).append(" = ").append(PlatformConstants.ARGUMENT).append("[").append(idx).append("].getList();\n");

		String local = LOCAL_PREFIX + String.valueOf(localID++);
		source.append("ai.aitia.meme.paramsweep.colt.DoubleArrayList ").append(local);
		source.append(" = new ai.aitia.meme.paramsweep.colt.DoubleArrayList();\n");
		source.append("java.util.Iterator it = ").append(temp).append(".iterator();\n");
		source.append("while (it.hasNext()) {\n");
		source.append("Object item = it.next();\n");
		source.append(local).append(".add((Number)item);\n");
		source.append("}\n");
		if (sorted) {
			source.append(local + ".sort();\n");
		}
		return local;
	}
	
	//----------------------------------------------------------------------------------------------------
	private String eGenerateArray(String type, int idx, StringBuilder source) {
		String temp = LOCAL_PREFIX + String.valueOf(localID++);
		source.append("LogoList ").append(temp).append(" = ").append(PlatformConstants.ARGUMENT).append("[").append(idx).append("].getList();\n");

		String local0 = LOCAL_PREFIX + String.valueOf(localID++);
		source.append("ai.aitia.meme.paramsweep.colt.DoubleArrayList ");
		source.append(local0 + " = new ai.aitia.meme.paramsweep.colt.DoubleArrayList();\n");
		source.append("java.util.Iterator it = ").append(temp).append(".iterator();\n");
		source.append("while (it.hasNext()) {\n");
		source.append("Object item = it.next();\n");
		source.append(local0).append(".add((Number)item);\n");

		String local1 = LOCAL_PREFIX + String.valueOf(localID++);
		source.append(type).append("[] ").append(local1).append(" = ").append(local0).append(".to_").append(type).append("Array();\n");
		return local1;
	}
	
	//----------------------------------------------------------------------------------------------------
	private String eGenerateInteger(String type, int idx, StringBuilder source) {
		String local = LOCAL_PREFIX + String.valueOf(localID++);
		source.append(type).append(" ").append(local).append(" = (").append(type).append(") ").append(PlatformConstants.ARGUMENT);
		source.append("[").append(idx).append("].getIntValue();\n");
		return local;
	}
	
	//----------------------------------------------------------------------------------------------------
	private String eGenerateReal(String type, int idx, StringBuilder source) {
		String local = LOCAL_PREFIX + String.valueOf(localID++);
		source.append(type).append(" ").append(local).append(" = (").append(type).append(") ").append(PlatformConstants.ARGUMENT);
		source.append("[").append(idx).append("].getDoubleValue();\n");
		return local;
	}
	
	//----------------------------------------------------------------------------------------------------
	private String eGenerateIntegerWrapper(String type, int idx, StringBuilder source) {
		String local = LOCAL_PREFIX + String.valueOf(localID++);
		source.append(type).append(" ").append(local).append(" = new ").append(type).append("((").append(primitiveType(type)).append(")");
		source.append(PlatformConstants.ARGUMENT).append("[").append(idx).append("].getIntValue());\n");
		return local;
	}
	
	//----------------------------------------------------------------------------------------------------
	private String eGenerateRealWrapper(String type, int idx, StringBuilder source) {
		String local = LOCAL_PREFIX + String.valueOf(localID++);
		source.append(type).append(" ").append(local).append(" = new ").append(type).append("((").append(primitiveType(type)).append(")");
		source.append(PlatformConstants.ARGUMENT).append("[").append(idx).append("].getDoubleValue());\n");
		return local;
	}
	
	//----------------------------------------------------------------------------------------------------
	private String generateLogoList(List<MemberInfo> list, StringBuilder source, StringBuilder call, NLSimpleGeneratedMemberInfo result) {
		String local = LOCAL_PREFIX + String.valueOf(localID++);
		source.append("let ").append(local).append(" (sentence ");
		call.append("[");
		for (MemberInfo mi : list) {
			call.append(Utilities.name(mi) + ",");
			source.append(mi.getName()).append(" ");
			if (mi instanceof GeneratedMemberInfo)
				result.addReference((GeneratedMemberInfo)mi);
		}
		source.append(")\n");
		call.deleteCharAt(call.length() - 1);
		call.append("],");
		return local;
	}
	
	//----------------------------------------------------------------------------------------------------
	private String generateVariable(MemberInfo mi, StringBuilder source) {
		String local = LOCAL_PREFIX + String.valueOf(localID++);
		source.append("let ").append(local).append(" ").append(mi.getName()).append("\n");
		return local;
	}
	
	//----------------------------------------------------------------------------------------------------
	private int majorVersion() {
		final String fullVersion = PlatformManager.getPlatform(PlatformSettings.getPlatformType()).getVersion();
		return '5' == fullVersion.charAt(0) ? 5 : 4; 
	}
}
