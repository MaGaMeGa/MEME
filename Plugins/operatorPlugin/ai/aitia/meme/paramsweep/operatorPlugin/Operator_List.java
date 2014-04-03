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

import ai.aitia.meme.paramsweep.gui.info.MemberInfo;
import ai.aitia.meme.paramsweep.platform.PlatformManager.PlatformType;
import ai.aitia.meme.paramsweep.plugin.IListOperatorPlugin;
import ai.aitia.meme.paramsweep.utils.Utilities;

public class Operator_List implements IListOperatorPlugin {

	//====================================================================================================
	// implemented interfaces
	
	//----------------------------------------------------------------------------------------------------
	public String getName() { return "list"; }
	public String getLocalizedName() { return "List"; }
	public boolean isRecordable(Object... actParams) { return false; }
	public OperatorGUIType getGUIType() { return OperatorGUIType.LIST; }
	public int getNumberOfParameters() { return 2; } // collection, member of innerType 
	public Class<?> getReturnType(Object... actrParams) { return ArrayList.class; }
	public boolean isSupportedByPlatform(PlatformType platform) { return platform != PlatformType.NETLOGO && platform != PlatformType.NETLOGO5; }
	
	//----------------------------------------------------------------------------------------------------
	public String getDescription() {
		return "Creates a list from a collection. Gets the selected member from each" +
				"element of the collection and builds a list using those.";
	}
	
	//----------------------------------------------------------------------------------------------------
	public Class<?> getInnerClass(Object... actParams) {
		MemberInfo member = (MemberInfo) actParams[1];
		return boxingType(member.getJavaType());
	}
	
	//----------------------------------------------------------------------------------------------------
	public String getCode(Object... actParams) {
		MemberInfo collection = (MemberInfo) actParams[0];
		Class<?> innerType = collection.getInnerType();
		MemberInfo member = (MemberInfo) actParams[1];
		StringBuilder code = new StringBuilder();
		code.append("java.util.ArrayList result = new java.util.ArrayList(" + collection.getName());
		if (collection.getType().endsWith("[]")) {
			code.append(".length);\n");
			code.append("for (int i = 0;i < " + collection.getName() + ".length;++i) {\n");
			code.append(boxingIfNeed(collection.getName() + "[i]." + member.getName(),member.getJavaType()));
			code.append("}\n");
		} else {
			code.append(".size());\n");
			code.append("Object[] source = " + collection.getName() + ".toArray();\n");
			code.append("for (int i = 0;i < source.length;++i) {\n");
			code.append(boxingIfNeed("((" + innerType.getName() + ")source[i])." + member.getName(),member.getJavaType()));
			code.append("}\n");
		}
		code.append("return result;\n");
		return code.toString();
	}
	
	//----------------------------------------------------------------------------------------------------
	public List<String> checkParameters(Object... actParams) {
		List<String> result = new ArrayList<String>();
		if (!(actParams[0] instanceof MemberInfo))
			result.add("Parameter #0 is not a MemberInfo instance.");
		if (!(actParams[1] instanceof MemberInfo))
			result.add("Parameter #1 is not a MemberInfo instance.");
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	public String getInstanceDisplayName(String name, Object... actParams) {
		String _name = name;
		if (_name.endsWith("()"))
			_name = _name.substring(0,_name.length() - 2);
		MemberInfo collection = (MemberInfo) actParams[0];
		MemberInfo member = (MemberInfo) actParams[1];
//		return Utilities.name(collection) + "." + Utilities.name(member) + "[]";
		return _name + "(" + Utilities.name(collection) + "," + Utilities.name(member) + ")";
	}
	
	//====================================================================================================
	// private methods
	
	//----------------------------------------------------------------------------------------------------
	private String boxingIfNeed(String core, Class<?> memberType) {
		StringBuilder code = new StringBuilder();
		code.append("result.add(");
		if (memberType.isPrimitive()) 
			code.append("new " + boxingType(memberType).getName() + "(");
		code.append(core);
		if (memberType.isPrimitive())
			code.append(")");
		code.append(");\n");
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
}
