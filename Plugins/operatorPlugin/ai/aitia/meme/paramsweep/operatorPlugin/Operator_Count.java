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
import ai.aitia.meme.paramsweep.gui.info.ConstantKeyInfo;
import ai.aitia.meme.paramsweep.gui.info.MemberInfo;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings.UnsupportedPlatformException;
import ai.aitia.meme.paramsweep.platform.PlatformManager.PlatformType;
import ai.aitia.meme.paramsweep.plugin.IListOperatorPlugin;
import ai.aitia.meme.paramsweep.utils.Utilities;

public class Operator_Count implements IListOperatorPlugin {
	
	//====================================================================================================
	// implemented interfaces

	public String getName() { return "count"; }
	public String getLocalizedName() { return "Count element"; }
	public int getNumberOfParameters() { return 3; } // collection, inner type (or type in case of NetLogo), element
	public OperatorGUIType getGUIType() { return OperatorGUIType.REMOVE; }
	public boolean isRecordable(Object... actParams) { return true; }
	public boolean isSupportedByPlatform(PlatformType platform) { return true; }
	public Class<?> getReturnType(Object... actParams) { return Integer.TYPE; }
	public String getDescription() { return "Returns the number of elements equals to the specified one in the given collection."; }
	
	//----------------------------------------------------------------------------------------------------
	public Class<?> getInnerClass(Object... actParams) {
		Class<?> innerType = (Class) actParams[1];
		return innerType;
	}
	
	//----------------------------------------------------------------------------------------------------
	public String getInstanceDisplayName(String name, Object... actParams) {
		String _name = name;
		if (_name.endsWith("()"))
			_name = _name.substring(0,_name.length() - 2);
		MemberInfo info = (MemberInfo) actParams[0];
		MemberInfo element = (MemberInfo) actParams[2];
		return _name + "(" + Utilities.name(info) + "," + Utilities.name(element) + ")";
	}

	//----------------------------------------------------------------------------------------------------
	public List<String> checkParameters(Object... actParams) {
		List<String> result = new ArrayList<String>();
		if (!(actParams[0] instanceof MemberInfo))
			result.add("Parameter #0 is not a MemberInfo instance.");
		if (!(actParams[1] instanceof Class))
			result.add("Parameter #1 is not a Class instance.");
		if (!(actParams[2] instanceof MemberInfo))
			result.add("Parameter #2 is not a MemberInfo instance.");
		return result;
	}

	//----------------------------------------------------------------------------------------------------
	public String getCode(Object... actParams) {
		MemberInfo info = (MemberInfo) actParams[0];
		Class<?> innerType = (Class) actParams[1];
		MemberInfo object = (MemberInfo) actParams[2];
		switch (PlatformSettings.getPlatformType()) {
		case REPAST		:
		case MASON		:
		case SIMPHONY2	:
		case CUSTOM 	: return getJavaCode(info,innerType,object);
		case NETLOGO5	:
		case NETLOGO	: return getNetLogoCode(info,object,innerType);
		default			: throw new UnsupportedPlatformException(PlatformSettings.getPlatformType().toString());
		}
	}
	
	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	private String getJavaCode(MemberInfo info, Class<?> innerType, MemberInfo object) {
		StringBuilder code = new StringBuilder();
		code.append("int result = 0;\n");
		if (info.getType().endsWith("[]")) {
			code.append("for (int i = 0;i < " + info.getName() + ".length;++i) {\n");
			if (info.getInnerType().isPrimitive()) {
				code.append("if (").append(info.getName()).append("[i] == ").append(element(object,innerType)).append(") {\n");
					code.append("result++;\n");
				code.append("}\n");
			} else {
				code.append("if (").append(info.getName()).append("[i].equals(").append(element(object,innerType)).append(")) {\n");
					code.append("result++;\n");
				code.append("}\n");
			}
		} else  {
			code.append("for (int i = 0;i < " + info.getName() + ".size();++i) {\n");
			code.append("if (").append(info.getName()).append(".get(i).equals(").append(element2(object,innerType)).append(")) {\n");
				code.append("result++;\n");
			code.append("}\n");
		}
		code.append("}\n");
		code.append("return result;\n");
		return code.toString();
	}

	
	//----------------------------------------------------------------------------------------------------
	private String element(MemberInfo object, Class<?> innerType) {
		if (object instanceof ConstantKeyInfo) {
			ConstantKeyInfo info = (ConstantKeyInfo) object;
			if (info.isStringKey())
				return "\"" + (String) info.getValue() + "\"";
			else 
				return numberValueCasting(info,innerType);
		} 
		return object.getName();
	}
	
	//----------------------------------------------------------------------------------------------------
	private String element2(MemberInfo object, Class<?> innerType) {
		if (object instanceof ConstantKeyInfo) {
			ConstantKeyInfo info = (ConstantKeyInfo) object;
			if (info.isStringKey())
				return "\"" + (String) info.getValue() + "\"";
			else if (!Number.class.equals(innerType) && Number.class.isAssignableFrom(innerType))
				return "new " + innerType.getName() + "(" + numberValueCasting(info,innerType) + ")";
			return "new " + boxingType(object.getJavaType()).getName() + "(" + numberValueCasting(info,object.getJavaType()) + ")";
		} 
		if (object.getJavaType().isPrimitive())
			return "new " + boxingType(object.getJavaType()).getName() + "(" + object.getName() + ")";
		else
			return object.getName();
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
		if (Number.class.equals(memberType)) return Double.class;
		return memberType;
	}
	
	//----------------------------------------------------------------------------------------------------
	private String numberValueCasting(ConstantKeyInfo key, Class<?> type) {
		if (Byte.class.equals(type) || Byte.TYPE.equals(type))
			return String.valueOf(key.getNumberValue().byteValue());
		if (Short.class.equals(type) || Short.TYPE.equals(type))
			return String.valueOf(key.getNumberValue().shortValue());
		if (Integer.class.equals(type) || Integer.TYPE.equals(type))
			return String.valueOf(key.getNumberValue().intValue());
		if (Long.class.equals(type) || Long.TYPE.equals(type))
			return String.valueOf(key.getNumberValue().longValue());
		if (Float.class.equals(type) || Float.TYPE.equals(type))
			return String.valueOf(key.getNumberValue().floatValue());
		if (Double.class.equals(type) || Double.TYPE.equals(type) || Number.class.equals(type))
			return String.valueOf(key.getNumberValue().doubleValue());
		return "";
	}
	
	//----------------------------------------------------------------------------------------------------
	private String getNetLogoCode(MemberInfo info, MemberInfo object, Class<?> innerType) {
		if (info.getJavaType().equals(list.class))  
			return "report length filter [ ? = " + element(object,innerType) + "] " + info.getName() + "\n";
		else 
			return "report count " + info.getName() + " with [ self = " + element(object,innerType) + " ]\n"; 
	}
}
