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
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings.UnsupportedPlatformException;
import ai.aitia.meme.paramsweep.platform.PlatformManager.PlatformType;
import ai.aitia.meme.paramsweep.plugin.IListOperatorPlugin;
import ai.aitia.meme.paramsweep.utils.Utilities;

public class Operator_Subtract implements IListOperatorPlugin {
	
	//====================================================================================================
	// implemented interfaces

	//----------------------------------------------------------------------------------------------------
	public String getName() { return "subtract"; }
	public String getLocalizedName() { return "Subtract"; }
	public int getNumberOfParameters() { return 2; } // two collection
	public OperatorGUIType getGUIType() { return OperatorGUIType.BINARY_LIST_CONSTRUCT; }
	public boolean isRecordable(Object... actParams) { return false; }
	public boolean isSupportedByPlatform(PlatformType platform) { return true; }

	//----------------------------------------------------------------------------------------------------
	public Class<?> getReturnType(Object... actParams) { 
		return (PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5) ? list.class : ArrayList.class;
	}
	
	//----------------------------------------------------------------------------------------------------
	public String getDescription() {
		String desc = "Returns the elements contained in the first list/";
		if (PlatformSettings.getPlatformType() != PlatformType.NETLOGO && PlatformSettings.getPlatformType() != PlatformType.NETLOGO5)
			desc += "array";
		else
			desc += "agentset";
		return desc + "but not in the second.";
	}
	
	//----------------------------------------------------------------------------------------------------
	public String getInstanceDisplayName(String name, Object... actParams) {
		String _name = name;
		if (_name.endsWith("()"))
			_name = _name.substring(0,_name.length() - 2);
		MemberInfo a = (MemberInfo) actParams[0];
		MemberInfo b = (MemberInfo) actParams[1];
		return _name + "(" + Utilities.name(a) + "," + Utilities.name(b) + ")";
	}

	//----------------------------------------------------------------------------------------------------
	public List<String> checkParameters(Object... actParams) {
		List<String> result = new ArrayList<String>();
		for (int i = 0;i < actParams.length;++i) {
			if (!(actParams[i] instanceof MemberInfo))
				result.add("Parameter #" + i + " is not a MemberInfo instance.");
		}
		return result;
	}

	//----------------------------------------------------------------------------------------------------
	public Class<?> getInnerClass(Object... actParams) {
		MemberInfo first = (MemberInfo) actParams[0];
		Class<?> result = first.getInnerType();
		if (PlatformSettings.getPlatformType() != PlatformType.NETLOGO && PlatformSettings.getPlatformType() != PlatformType.NETLOGO5) {
			if (result == null) 
				return Object.class; 
			result = boxingType(result);
			MemberInfo mi = (MemberInfo) actParams[1];
			if (mi.getInnerType() == null || !result.equals(boxingType(mi.getInnerType())))
				return Object.class;
		}
		return result;
	}

	//----------------------------------------------------------------------------------------------------
	public String getCode(Object... actParams) {
		MemberInfo a = (MemberInfo) actParams[0];
		MemberInfo b = (MemberInfo) actParams[1];
		switch (PlatformSettings.getPlatformType()) {
		case REPAST		:
		case MASON		:
		case SIMPHONY2	:
		case CUSTOM 	: return getJavaCode(a,b);
		case NETLOGO5	:
		case NETLOGO	: return getNetLogoCode(a,b);
		default			: throw new UnsupportedPlatformException(PlatformSettings.getPlatformType().toString());
		}
	}
	
	//====================================================================================================
	// private methods
	
	//----------------------------------------------------------------------------------------------------
	private String getJavaCode(MemberInfo a, MemberInfo b) {
		StringBuilder code = new StringBuilder();
		code.append("java.util.ArrayList aList = new java.util.ArrayList();\n");
		if (a.getType().endsWith("[]")) {
			if (a.getInnerType().isPrimitive()) {
				code.append("for (int i = 0;i < " + a.getName() + ".length;++i) {\n");
					code.append("aList.add(new " + boxingType(a.getInnerType()).getName() + "(" + a.getName() + "[i]));\n");
				code.append("}\n");
			} else
				code.append("aList.addAll(java.util.Arrays.asList(" + a.getName() + "));\n");
		} else 
				code.append("aList.addAll(" + a.getName() + ");\n");
		code.append("java.util.ArrayList bList = new java.util.ArrayList();\n");
		if (b.getType().endsWith("[]")) {
			if (b.getInnerType().isPrimitive()) {
				code.append("for (int i = 0;i < " + b.getName() + ".length;++i) {\n");
					code.append("bList.add(new " + boxingType(b.getInnerType()).getName() + "(" + b.getName() + "[i]));\n");
				code.append("}\n");
			} else
				code.append("bList.addAll(java.util.Arrays.asList(" + b.getName() + "));\n");
		} else 
				code.append("bList.addAll(" + b.getName() + ");\n");
		code.append("\n");
		code.append("java.util.ArrayList result = new java.util.ArrayList();\n");
		code.append("for (int i = 0;i < aList.size();++i) {\n");
			code.append("Object candidate = aList.get(i);\n");
			code.append("if (!bList.contains(candidate)) {\n");
			code.append("result.add(candidate);\n");
			code.append("}\n");
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
	private String getNetLogoCode(MemberInfo a, MemberInfo b) {
		StringBuilder code = new StringBuilder();
		code.append("ifelse is-list? ").append(a.getName()).append("\n");
		code.append("[ifelse is-list? ").append(b.getName()).append("\n");
		code.append("[report filter [not member? ? ").append(b.getName()).append("] sort ").append(a.getName()).append("]\n");
		code.append("[report ").append(a.getName()).append("]\n");
		code.append("]\n");
		code.append("[ifelse is-agentset? ").append(b.getName()).append("\n");
		code.append("[report filter [not member? ? ").append(b.getName()).append("] sort ").append(a.getName()).append("]\n");
		code.append("[report sort ").append(a.getName()).append("]\n");
		code.append("]");
		return code.toString();
	}
}
