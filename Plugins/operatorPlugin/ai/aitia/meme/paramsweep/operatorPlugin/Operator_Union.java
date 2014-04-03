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

public class Operator_Union implements IListOperatorPlugin {
	
	//====================================================================================================
	// implemented interfaces

	//----------------------------------------------------------------------------------------------------
	public String getName() { return "union"; }
	public String getLocalizedName() { return "Union"; }
	public int getNumberOfParameters() { return -1; } // unknown info
	public OperatorGUIType getGUIType() { return OperatorGUIType.LIST_UNION_INTERSECTION; }
	public boolean isRecordable(Object... actParams) { return false; }
	public boolean isSupportedByPlatform(PlatformType platform) { return true; }
	
	//----------------------------------------------------------------------------------------------------
	public Class<?> getReturnType(Object... actParams) { 
		return (PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5) ? ((MemberInfo)actParams[0]).getJavaType() : ArrayList.class;
	}

	//----------------------------------------------------------------------------------------------------
	public String getDescription() {
		String desc = "Returns the union list";
		if (PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5)
			desc += "/agentset";
		desc += " of the parameters (lists ";
		if (PlatformSettings.getPlatformType() != PlatformType.NETLOGO && PlatformSettings.getPlatformType() != PlatformType.NETLOGO5)
			desc += "and/";
		desc += "or ";
		if (PlatformSettings.getPlatformType() == PlatformType.NETLOGO && PlatformSettings.getPlatformType() != PlatformType.NETLOGO5)
			desc += "agentsets";
		else
			desc += "arrays";
		return desc + ").";
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
		if (result == null) 
			return Object.class;
		if (PlatformSettings.getPlatformType() != PlatformType.NETLOGO && PlatformSettings.getPlatformType() != PlatformType.NETLOGO5) {
			result = boxingType(result);
			for (int i = 1;i < actParams.length;++i) {
				MemberInfo mi = (MemberInfo) actParams[i];
				if (mi.getInnerType() == null || !result.equals(boxingType(mi.getInnerType())))
					return Object.class;
			}
		}
		return result;
	}

	//----------------------------------------------------------------------------------------------------
	public String getInstanceDisplayName(String name, Object... actParams) {
		String _name = name;
		if (_name.endsWith("()"))
			_name = _name.substring(0,_name.length() - 2);
		StringBuilder sb = new StringBuilder();
		sb.append(_name + "(");
		for (Object o : actParams) {
			MemberInfo mi = (MemberInfo) o;
			sb.append(Utilities.name(mi) + ",");
		}
		String result = sb.toString();
		result = result.substring(0,result.length() - 1) + ")";
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	public String getCode(Object... actParams) {
		switch (PlatformSettings.getPlatformType()) {
		case REPAST		:
		case MASON		:
		case SIMPHONY2	:
		case CUSTOM 	: return getJavaCode(actParams);
		case NETLOGO5	:
		case NETLOGO	: return getNetLogoCode(actParams);
		default			: throw new UnsupportedPlatformException(PlatformSettings.getPlatformType().toString());
		}
	}
	
	//====================================================================================================
	// private methods
	
	//----------------------------------------------------------------------------------------------------
	private String getJavaCode(Object... actParams) {
		StringBuilder code = new StringBuilder();
		code.append("java.util.ArrayList result = new java.util.ArrayList();\n");
		for (int i = 0;i < actParams.length;++i) {
			MemberInfo mi = (MemberInfo) actParams[i];
			if (mi.getType().endsWith("[]")) {
				if (mi.getInnerType().isPrimitive()) {
					code.append("for (int i = 0;i < " + mi.getName() + ".length;++i) {\n");
						code.append("result.add(new " + boxingType(mi.getInnerType()).getName() + "(" + mi.getName() + "[i]));\n");
					code.append("}\n");
				} else
					code.append("result.addAll(java.util.Arrays.asList(" + mi.getName() + "));\n");
			} else  
				code.append("result.addAll(" + mi.getName() + ");\n");
		}
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
	private String getNetLogoCode(Object... actParams) {
		StringBuilder code = new StringBuilder();
		MemberInfo first = (MemberInfo) actParams[0];
		if (list.class.equals(first.getJavaType())) { 
			code.append("let result (sentence ");
			for (int i = 0;i < actParams.length;++i) {
				MemberInfo mi = (MemberInfo) actParams[i];
				code.append(mi.getName()).append(" ");
			}
			code.append(")\n");
			code.append("set result remove-duplicates result");
		} else {
			code.append("let result (turtle-set ");
			for (int i = 0;i < actParams.length;++i) {
				MemberInfo mi = (MemberInfo) actParams[i];
				code.append(mi.getName()).append(" ");
			}
			code.append(")");
		}
		code.append("\nreport result");
		return code.toString();
	}
}
