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

public class Operator_MinPlaces implements IListOperatorPlugin {
	
	//====================================================================================================
	// implemented interfaces
	
	//----------------------------------------------------------------------------------------------------
	public String getName() { return "minPlaces"; }
	public String getLocalizedName() { return "Minimum places"; }
	public int getNumberOfParameters() { return 2; } // Collection, inner type
	public OperatorGUIType getGUIType() { return OperatorGUIType.ONEONE_CONSTRUCT; }
	public boolean isRecordable(Object... actParams) { return false; }
	public boolean isSupportedByPlatform(PlatformType platform) { return true; }
	
	//----------------------------------------------------------------------------------------------------
	public Class<?> getReturnType(Object... actParams) { 
		return (PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5) ? list.class : ArrayList.class;
	}
	
	//----------------------------------------------------------------------------------------------------
	public Class<?> getInnerClass(Object... actParams) { 
		return (PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5) ? Double.TYPE : Integer.class;
	}

	//----------------------------------------------------------------------------------------------------
	public String getDescription() {
		String desc = "Returns the list of the indices of the minimal values in the specified list";
		if (PlatformSettings.getPlatformType() != PlatformType.NETLOGO && PlatformSettings.getPlatformType() != PlatformType.NETLOGO5)
			desc += "/array";
		return desc + "."; 
	}
	
	//----------------------------------------------------------------------------------------------------
	public String getInstanceDisplayName(String name, Object... actParams) {
		String _name = name;
		if (_name.endsWith("()"))
			_name = _name.substring(0,_name.length() - 2);
		MemberInfo object = (MemberInfo) actParams[0];
		return _name + "(" + Utilities.name(object) + ")";
	}

	//----------------------------------------------------------------------------------------------------
	public List<String> checkParameters(Object... actParams) {
		List<String> result = new ArrayList<String>();
		if (!(actParams[0] instanceof MemberInfo))
			result.add("Parameter #0 is not a MemberInfo instance.");
		if (!(actParams[1] instanceof Class)) 
			result.add("Parameter #1 is not a Class instance.");
		else {
			Class<?> innerType = (Class) actParams[1];
			if (PlatformSettings.getPlatformType() != PlatformType.NETLOGO && PlatformSettings.getPlatformType() != PlatformType.NETLOGO5) {
				if (innerType.isPrimitive() && !innerType.equals(Boolean.TYPE))
					return result;
				if (!Comparable.class.isAssignableFrom(innerType) ||
					innerType.equals(Boolean.TYPE))
					result.add("The elements of the list/array are not comparable.");
			} else if (!innerType.equals(Double.TYPE))
				result.add("The elements of the list must be numbers.");
		}
		return result;
	}

	//----------------------------------------------------------------------------------------------------
	public String getCode(Object... actParams) {
		MemberInfo mi = (MemberInfo) actParams[0];
		Class<?> innerType = (Class) actParams[1];
		switch (PlatformSettings.getPlatformType()) {
		case REPAST		:
		case MASON		:
		case SIMPHONY2	:
		case CUSTOM 	: return getJavaCode(mi,innerType);
		case NETLOGO5	:
		case NETLOGO	: return getNetLogoCode(mi);
		default			: throw new UnsupportedPlatformException(PlatformSettings.getPlatformType().toString());		
		}	}
	
	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	private String getJavaCode(MemberInfo mi, Class<?> innerType) {
		StringBuilder code = new StringBuilder();
		code.append("java.util.ArrayList result = new java.util.ArrayList();\n");
		if (mi.getType().endsWith("[]")) {
			code.append("if (" + mi.getName() + ".length == 0) {\n");
				code.append("return result;\n");
			code.append("}\n");
			code.append(innerType.getName() + " min = " + mi.getName() + "[0];\n");
			code.append("result.add(new Integer(0));\n");
			code.append("for (int i = 1; i < " + mi.getName() + ".length;++i) {\n");
			if (innerType.isPrimitive()) {
				code.append("if (" + mi.getName() + "[i] < min) {\n");
					code.append("result.clear();\n");
					code.append("min = " + mi.getName() + "[i];\n");
					code.append("result.add(new Integer(i));\n");
				code.append("} else if (" + mi.getName() + "[i] == min) {\n");
					code.append("result.add(new Integer(i));\n");
				code.append("}\n");
			} else {
				code.append("if (((java.lang.Comparable)" + mi.getName() + "[i]).compareTo(min) < 0) {\n");
					code.append("result.clear();\n");
					code.append("min = " + mi.getName() + "[i];\n");
					code.append("result.add(new Integer(i));\n");
				code.append("} else if (((java.lang.Comparable)" + mi.getName() + "[i]).compareTo(min) == 0) {\n");
					code.append("result.add(new Integer(i));\n");
				code.append("}\n");
			}
			code.append("}\n");
		} else {
			code.append("if (" + mi.getName() + ".size() == 0) {\n");
				code.append("return result;\n");
			code.append("}\n");
			code.append(innerType.getName() + " min = " + mi.getName() + ".get(0);\n");
			code.append("result.add(new Integer(0));\n");
			code.append("for (int i = 1; i < " + mi.getName() + ".size();++i) {\n");
				code.append("if (((java.lang.Comparable)" + mi.getName() + ".get(i)).compareTo(min) < 0) {\n");
					code.append("result.clear();\n");
					code.append("min = " + mi.getName() + ".get(i);\n");
					code.append("result.add(new Integer(i));\n");
				code.append("} else if (((java.lang.Comparable)" + mi.getName() + ".get(i)).compareTo(min) == 0) {\n");
					code.append("result.add(new Integer(i));\n");
				code.append("}\n");
			code.append("}\n");
		}
		code.append("return result;\n");
		return code.toString();
	}
	
	//----------------------------------------------------------------------------------------------------
	private String getNetLogoCode(MemberInfo mi) {
		StringBuilder code = new StringBuilder();
		code.append("let _min min ").append(mi.getName()).append("\n");
		code.append("let result []\n");
		code.append("foreach n-values length ").append(mi.getName()).append(" [?] [\n");
		code.append("if item ? ").append(mi.getName()).append(" = _min [\n");
		code.append("set result lput ? result\n");
		code.append("]\n");
		code.append("]\n");
		code.append("report result");
		return code.toString();
	}
}
