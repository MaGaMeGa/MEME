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

public class Operator_Tail implements IListOperatorPlugin {

	//====================================================================================================
	// implemented interfaces
	
	//----------------------------------------------------------------------------------------------------
	public String getName() { return "tail"; }
	public String getLocalizedName() { return "Tail"; }
	public int getNumberOfParameters() { return 2; } // collection, inner type
	public OperatorGUIType getGUIType() { return OperatorGUIType.LIST_SELECTION; }
	public boolean isRecordable(Object... actParams) { return false; }
	public boolean isSupportedByPlatform(PlatformType platform) { return true; }
	
	//----------------------------------------------------------------------------------------------------
	public String getDescription() {
		return "Returns a collection (or array) without its first element."; 
	}
	
	//----------------------------------------------------------------------------------------------------
	public Class<?> getInnerClass(Object... actParams) {
		Class innerType = (Class) actParams[1];
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
		case NETLOGO	: return getNetLogoCode(object);
		default			: throw new UnsupportedPlatformException(PlatformSettings.getPlatformType().toString());		
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public Class<?> getReturnType(Object... actParams) {
		MemberInfo object = (MemberInfo) actParams[0];
		if (object.getType().endsWith("[]"))  
			return object.getJavaType();
		else if (PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5)
			return list.class;
		return ArrayList.class;
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
		return result;
	}
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	private String getJavaCode(MemberInfo object, Class<?> innerType) {
		StringBuilder code = new StringBuilder();
		if (object.getType().endsWith("[]")) {
			code.append(innerType.getName() + "[] tail = new " + innerType.getName() + "[" + object.getName() + ".length - 1];\n");
			code.append("for (int i = 0;i < " + object.getName() + ".length - 1;++i) {\n");
			code.append("tail[i] = " + object.getName() + "[i + 1];\n");
			code.append("}\n");
			code.append("return tail;\n");
		} else {
			code.append("Object[] source = " + object.getName() + ".toArray();\n");
			code.append("java.util.ArrayList tail = new java.util.ArrayList(" + object.getName() + ".size() -1);\n");
			code.append("for (int i = 0;i < source.length - 1;++i) {\n");
			code.append("tail.add((" + innerType.getName() + ")source[i + 1]);\n");
			code.append("}\n");
			code.append("return tail;\n");
		}
		return code.toString();
	}

	
	//----------------------------------------------------------------------------------------------------
	private String getNetLogoCode(MemberInfo info) { 
		String prefix = "report but-first ";
		if (info.getJavaType().equals(list.class))
			return prefix + info.getName();
		else
			return prefix + "(sort " + info.getName() + ")";
	}

	
}
