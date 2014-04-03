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

public class Operator_Sort implements IListOperatorPlugin {

	//====================================================================================================
	// implemented interfaces
	
	//----------------------------------------------------------------------------------------------------
	public String getName() { return "sort"; }
	public String getLocalizedName() { return "Sort"; }
	public int getNumberOfParameters() { return 2; } // Collection, inner type
	public OperatorGUIType getGUIType() { return OperatorGUIType.ONEONE_CONSTRUCT; }
	public boolean isRecordable(Object... actParams) { return false; }
	public boolean isSupportedByPlatform(PlatformType platform) { return true; }
	
	//----------------------------------------------------------------------------------------------------
	public String getDescription() {
		String desc = "Sorts the specified list ";
		if (PlatformSettings.getPlatformType() != PlatformType.NETLOGO && PlatformSettings.getPlatformType() != PlatformType.NETLOGO5)
			desc += "(or array) ";
		return desc + "into ascending order, according to the natural ordering of its elements."; 
	}
	
	//----------------------------------------------------------------------------------------------------
	public Class<?> getInnerClass(Object... actParams) {
		Class<?> innerType = (Class) actParams[1];
		return innerType;
	}

	//----------------------------------------------------------------------------------------------------
	public String getCode(Object... actParams) {
		MemberInfo object = (MemberInfo) actParams[0];
		Class innerType = (Class) actParams[1];
		switch (PlatformSettings.getPlatformType()) {
		case REPAST		:
		case MASON		:
		case SIMPHONY2	:
		case CUSTOM 	: return getJavaCode(object,innerType);
		case NETLOGO5	:
		case NETLOGO	: return getNetLogoCode(object,innerType);
		default			: throw new UnsupportedPlatformException(PlatformSettings.getPlatformType().toString());		
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public List<String> checkParameters(Object... actParams) {
		List<String> result = new ArrayList<String>();
		if (!(actParams[0] instanceof MemberInfo))
			result.add("Parameter #0 is not a MemberInfo instance.");
		if (!(actParams[1] instanceof Class)) 
			result.add("Parameter #1 is not a Class instance.");
		else if (PlatformSettings.getPlatformType() != PlatformType.NETLOGO && PlatformSettings.getPlatformType() != PlatformType.NETLOGO5) {
			Class<?> innerType = (Class) actParams[1];
			if (innerType.isPrimitive() && !innerType.equals(Boolean.TYPE))
				return result;
			if (!Comparable.class.isAssignableFrom(innerType) ||
				innerType.equals(Boolean.TYPE))
				result.add("The elements of the list/array are not comparable.");
		}
		return result;
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
	public Class<?> getReturnType(final Object... actParams) {
		final MemberInfo object = (MemberInfo) actParams[0];
		return (PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5) ? list.class : object.getJavaType();
	}
	
	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	private String getJavaCode(MemberInfo object, Class<?> innerType) {
		StringBuilder code = new StringBuilder();
		if (object.getType().endsWith("[]")) { 
			code.append(innerType.getName() + "[] array = new " + innerType.getName() + "[" + object.getName() + ".length];\n");
			code.append("for (int i = 0;i < " + object.getName() + ".length;++i) {\n");
			code.append("array[i] = " + object.getName() + "[i];\n");
			code.append("}\n");
			code.append("java.util.Arrays.sort(array);\n");
			code.append("return array;\n");
		} else { 
			code.append("Object[] source = " + object.getName() + ".toArray();\n");
			code.append("java.util.ArrayList list = new java.util.ArrayList(" + object.getName() + ".size());\n");
			code.append("for (int i = 0;i < source.length;++i) {\n");
			code.append("list.add((" + innerType.getName() + ")source[i]);\n");
			code.append("}\n");
			code.append("java.util.Collections.sort(list);\n");
			code.append("return list;\n");
		}
		return code.toString();
	}
	
	//----------------------------------------------------------------------------------------------------
	private String getNetLogoCode(MemberInfo object, Class<?> innerType) {
		if (Boolean.TYPE.equals(innerType)) {
			StringBuilder code = new StringBuilder();
			code.append("let local0 filter [not ?] ").append(object.getName()).append("\n");
			code.append("report (sentence local0 n-values (length ").append(object.getName()).append(" - length local0) [true])");
			return code.toString();
		} else {
			return "report sort " + object.getName();
		}
	}
}
