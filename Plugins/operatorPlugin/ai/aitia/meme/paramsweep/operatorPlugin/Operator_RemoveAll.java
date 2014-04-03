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

public class Operator_RemoveAll implements IListOperatorPlugin {
		
	//====================================================================================================
	// implemented interfaces
	
	//----------------------------------------------------------------------------------------------------
	public String getName() { return "removeAll"; }
	public String getLocalizedName() { return "Remove element (all instances)"; }
	public int getNumberOfParameters() { return 3; } // collection, inner type, element
	public OperatorGUIType getGUIType() { return OperatorGUIType.REMOVE; }
	public boolean isRecordable(Object... actParams) { return false; }
	public boolean isSupportedByPlatform(PlatformType platform) { return true; }
	
	//----------------------------------------------------------------------------------------------------
	public Class<?> getReturnType(Object... actParams) { 
		return (PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5) ? ((MemberInfo)actParams[0]).getJavaType() : ArrayList.class;
	}
	
	//----------------------------------------------------------------------------------------------------
	public String getDescription() {
		String desc = "Returns a list";
		if (PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5)
			desc += "/agentset";
		desc += " that contains the same elements than the selected list/";
		if (PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5)
			desc += "agentset";
		else
			desc += "array"; 
		return desc + "except all instances of the given element.";
	}

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
		case NETLOGO	: return getNetLogoCode(info,object);
		default			: throw new UnsupportedPlatformException(PlatformSettings.getPlatformType().toString());
		}
	}
	
	//====================================================================================================
	// private methods
	
	//----------------------------------------------------------------------------------------------------
	private String getJavaCode(MemberInfo info, Class<?> innerType, MemberInfo object) {
		StringBuilder code = new StringBuilder();
		code.append("java.util.ArrayList result = new java.util.ArrayList();\n");
		if (info.getType().endsWith("[]")) {
			if (info.getInnerType().isPrimitive()) {
				code.append("for (int i = 0;i < " + info.getName() + ".length;++i) {\n");
					code.append("result.add(new " + boxingType(info.getInnerType()).getName() + "(" + info.getName() + "[i]));\n");
				code.append("}\n");
			} else
				code.append("result.addAll(java.util.Arrays.asList(" + info.getName() + "));\n");
		} else 
				code.append("result.addAll(" + info.getName() + ");\n");
		code.append("java.util.ArrayList element = new java.util.ArrayList();\n");
		code.append("element.add(" + element(object,boxingType(innerType)) + ");\n");
		code.append("result.removeAll(element);\n");
		code.append("return result;\n");
		return code.toString();
	}
	
	//----------------------------------------------------------------------------------------------------
	private String element(MemberInfo object, Class<?> innerType) {
		if (object instanceof ConstantKeyInfo) {
			ConstantKeyInfo info = (ConstantKeyInfo) object;
			if (info.isStringKey())
				return "\"" + (String) info.getValue() + "\"";
			else if (!Number.class.equals(innerType) && Number.class.isAssignableFrom(innerType))
				return "new " + innerType.getName() + "(" + numberValueCasting(info,innerType) + ")";
			return "new " + boxingType(object.getJavaType()).getName() + "(" + numberValueCasting(info,object.getJavaType()) + ")";
		} 
		if (object.getJavaType().isPrimitive())
			return "new " + innerType.getName() + "(" + object.getName() + ")";
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
		if (Byte.class.equals(type))
			return String.valueOf(key.getNumberValue().byteValue());
		if (Short.class.equals(type))
			return String.valueOf(key.getNumberValue().shortValue());
		if (Integer.class.equals(type))
			return String.valueOf(key.getNumberValue().intValue());
		if (Long.class.equals(type))
			return String.valueOf(key.getNumberValue().longValue());
		if (Float.class.equals(type))
			return String.valueOf(key.getNumberValue().floatValue());
		if (Double.class.equals(type) || Number.class.equals(type))
			return String.valueOf(key.getNumberValue().doubleValue());
		return "";
	}
	
	//----------------------------------------------------------------------------------------------------
	private String getNetLogoCode(MemberInfo info, MemberInfo object) {
		StringBuilder code = new StringBuilder();
		if (info.getJavaType().equals(list.class)) {
			code.append("report remove ").append(object.getName()).append(" ").append(info.getName());
		} else {
			code.append("let local0 [who] of ").append(object.getName()).append("\n");
			code.append("report ").append(info.getName()).append(" with [who != local0]");
		}
		return code.toString();
	}

}
