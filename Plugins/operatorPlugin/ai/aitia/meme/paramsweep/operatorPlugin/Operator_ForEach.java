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
package ai.aitia.meme.paramsweep.operatorPlugin;

import java.util.ArrayList;
import java.util.List;

import _.list;
import ai.aitia.meme.paramsweep.gui.info.MemberInfo;
import ai.aitia.meme.paramsweep.gui.info.OneArgFunctionMemberInfo;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings.UnsupportedPlatformException;
import ai.aitia.meme.paramsweep.platform.PlatformManager.PlatformType;
import ai.aitia.meme.paramsweep.plugin.IListOperatorPlugin;
import ai.aitia.meme.paramsweep.utils.Utilities;

public class Operator_ForEach implements IListOperatorPlugin {
	
	//====================================================================================================
	// implemented interfaces

	//----------------------------------------------------------------------------------------------------
	public String getName() { return "foreach"; }
	public String getLocalizedName() { return "For each"; }
	public OperatorGUIType getGUIType() { return OperatorGUIType.FOREACH; }
	public int getNumberOfParameters() { return 3; } // collection, inner type, function
	public boolean isRecordable(Object... actParams) { return false; }
	public boolean isSupportedByPlatform(PlatformType platform) { return true; }
	
	//----------------------------------------------------------------------------------------------------
	public Class<?> getReturnType(Object... actParams) { 
		return (PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5) ? list.class : ArrayList.class;
	}
	
	//----------------------------------------------------------------------------------------------------
	public String getDescription() {
		String desc = "Executes the selected function with each element of the specified list";
		if (PlatformSettings.getPlatformType() != PlatformType.NETLOGO && PlatformSettings.getPlatformType() != PlatformType.NETLOGO5)
			desc += "/array";
		return desc + " and creates a list from the results.";
	}
	
	//----------------------------------------------------------------------------------------------------
	public String getInstanceDisplayName(String name, Object... actParams) {
		String _name = name;
		if (_name.endsWith("()"))
			_name = _name.substring(0,_name.length() - 2);
		MemberInfo info = (MemberInfo) actParams[0];
		OneArgFunctionMemberInfo function = (OneArgFunctionMemberInfo) actParams[2];
		return _name + "(" + Utilities.name(info) + "," + function.getName() + ")";
	}
	
	//----------------------------------------------------------------------------------------------------
	public Class<?> getInnerClass(Object... actParams) {
		OneArgFunctionMemberInfo function = (OneArgFunctionMemberInfo) actParams[2];
		return (PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5) ? function.getJavaType() : boxingType(function.getJavaType());
	}

	//----------------------------------------------------------------------------------------------------
	public List<String> checkParameters(Object... actParams) {
		List<String> result = new ArrayList<String>();
		if (!(actParams[0] instanceof MemberInfo))
			result.add("Parameter #0 is not a MemberInfo instance.");
		if (!(actParams[1] instanceof Class))
			result.add("Parameter #1 is not a Class instance.");
		if (!(actParams[2] instanceof OneArgFunctionMemberInfo))
			result.add("Parameter #2 is not a OneArgFunctionMemberInfo instance.");
		return result;
	}

	//----------------------------------------------------------------------------------------------------
	public String getCode(Object... actParams) {
		MemberInfo info = (MemberInfo) actParams[0];
		Class<?> innerType = (Class) actParams[1];
		OneArgFunctionMemberInfo function = (OneArgFunctionMemberInfo) actParams[2];
		switch (PlatformSettings.getPlatformType()) {
		case REPAST		:
		case MASON		:
		case SIMPHONY2	:
		case CUSTOM 	: return getJavaCode(info,innerType,function);
		case NETLOGO5	:
		case NETLOGO	: return getNetLogoCode(info,function);
		default			: throw new UnsupportedPlatformException(PlatformSettings.getPlatformType().toString());
		}
	}
	
	//====================================================================================================
	// private methods
	
	//----------------------------------------------------------------------------------------------------
	private String getJavaCode(MemberInfo info, Class<?> innerType, OneArgFunctionMemberInfo function) {
		String functionName = function.getName().substring(0,function.getName().length() - 1); // contains the (
		StringBuilder code = new StringBuilder();
		code.append("java.util.ArrayList result = new java.util.ArrayList();\n");
		String functionCall = null;
		if (info.getType().endsWith("[]")) {
			code.append("for (int i = 0;i < " + info.getName() + ".length;++i) {\n");
			functionCall = functionName + cast(info,innerType,function,false) + ")";
		} else {
			code.append("for (int i = 0;i < " + info.getName() + ".size();++i) {\n");
			functionCall = functionName + cast(info,innerType,function,true) + ")";
		}
		if (function.getJavaType().isPrimitive()) 
			code.append("result.add(new " + boxingType(function.getJavaType()).getName() + "(" + functionCall + "));\n");
		else
			code.append("result.add(" + functionCall + ");\n");
		code.append("}\n");
		code.append("return result;\n");
		return code.toString();
	}
	
	//----------------------------------------------------------------------------------------------------
	private Class<?> boxingType(Class<?> memberType) {
		if (Byte.TYPE.equals(memberType)) return Byte.class;
		if (Short.TYPE.equals(memberType)) return Short.class;
		if (Integer.TYPE.equals(memberType)) return Integer.class;
		if (Long.TYPE.equals(memberType)) return Long.class;
		if (Float.TYPE.equals(memberType)) return Float.class;
		if (Double.TYPE.equals(memberType)) return Double.class;
		if (Boolean.TYPE.equals(memberType)) return Boolean.class;
		if (Character.TYPE.equals(memberType)) return Character.class;
		return memberType;
	}
	
	//----------------------------------------------------------------------------------------------------
	private String cast(MemberInfo info, Class<?> innerType, OneArgFunctionMemberInfo function, boolean list) {
		String selector = info.getName() + (list ? ".get(i)" : "[i]");
		if (!innerType.isPrimitive())
			selector = "((" + innerType.getName() + ")" + info.getName() + (list ? ".get(i))" : "[i])");
		if (innerType.isPrimitive() && function.getParameterType().isPrimitive()) 
			return selector;
		else if (!innerType.isPrimitive() && function.getParameterType().isPrimitive())
			return selector + numberValueCasting(innerType);
		else if (innerType.isPrimitive() && !function.getParameterType().isPrimitive())
			return "new " + boxingType(innerType).getName() + "(" + selector + ")";
		else
			return selector;
	}
	
	//----------------------------------------------------------------------------------------------------
	private String numberValueCasting(Class<?> type) {
		if (Byte.class.equals(type))
			return ".byteValue()";
		if (Short.class.equals(type))
			return ".shortValue()";	
		if (Integer.class.equals(type))
			return ".intValue()";
		if (Long.class.equals(type))
			return ".longValue()";
		if (Float.class.equals(type))
			return ".floatValue()";
		if (Double.class.equals(type))
			return ".doubleValue()";
		return "";
	}
	
	//----------------------------------------------------------------------------------------------------
	private String getNetLogoCode(MemberInfo info, OneArgFunctionMemberInfo function) {
		StringBuilder code = new StringBuilder();
		code.append("report map [").append(function.getName()).append(" ?] ").append(info.getName());
		return code.toString();
	}
}
