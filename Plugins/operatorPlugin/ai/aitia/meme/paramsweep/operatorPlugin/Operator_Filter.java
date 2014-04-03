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

import _.agentset;
import _.list;
import ai.aitia.meme.paramsweep.gui.info.MemberInfo;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings.UnsupportedPlatformException;
import ai.aitia.meme.paramsweep.platform.PlatformManager.PlatformType;
import ai.aitia.meme.paramsweep.plugin.IListOperatorPlugin;
import ai.aitia.meme.paramsweep.utils.Utilities;

public class Operator_Filter implements IListOperatorPlugin {


	//====================================================================================================
	// members

	//====================================================================================================
	// methods

	//====================================================================================================
	// implemented interfaces

	//----------------------------------------------------------------------------------------------------
	public String getName() { return "filter"; }
	public String getLocalizedName() { return "Filter"; }
	public boolean isRecordable(Object... actParams) { return false; }
	public boolean isSupportedByPlatform(final PlatformType platform) { return true; }
	public OperatorGUIType getGUIType() { return OperatorGUIType.FILTER; }
	public int getNumberOfParameters() { return 3; }	
	
	//----------------------------------------------------------------------------------------------------
	public String getDescription() {
		String desc = "Returns a list containing only those items of a list ";
		if (PlatformSettings.getPlatformType() != PlatformType.NETLOGO && PlatformSettings.getPlatformType() != PlatformType.NETLOGO5)
			desc += "(or array) ";
		desc += "for which filter condition is true";
		return desc;
	}
	
	//----------------------------------------------------------------------------------------------------
	public Class<?> getInnerClass(final Object... actParams) {
		final Class<?> innerType = (Class<?>) actParams[1];
		return innerType;
	}
	
	//----------------------------------------------------------------------------------------------------
	public List<String> checkParameters(final Object... actParams) {
		final List<String> result = new ArrayList<String>();
		if (!(actParams[0] instanceof MemberInfo))
			result.add("Parameter #0 is not a MemberInfo instance.");
		if (!(actParams[1] instanceof Class))
			result.add("Parameter #1 is not a Class instance.");
		if (!(actParams[2] instanceof String))
			result.add("Parameter #2 is not a String instance.");
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	public Class<?> getReturnType(Object... actParams) {
		return (PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5) ? (((MemberInfo)actParams[0]).getJavaType().equals(list.class) ? list.class 
																																		 : agentset.class)
																          : List.class;
	}
	
	//----------------------------------------------------------------------------------------------------
	public String getInstanceDisplayName(final String name, final Object... actParams) {
		String _name = name;
		if (_name.endsWith("()"))
			_name = _name.substring(0,_name.length() - 2);
		final MemberInfo object = (MemberInfo) actParams[0];
		final String filter  = (String) actParams[2];
		return _name + "(" + Utilities.name(object) + "," + filter + ")";
	}
	
	//----------------------------------------------------------------------------------------------------
	public String getCode(final Object... actParams) {
		final MemberInfo member = (MemberInfo) actParams[0];
		final Class<?> innerType = (Class<?>) actParams[1];
		final String filter = (String) actParams[2];
		switch (PlatformSettings.getPlatformType()) {
		case REPAST		:
		case MASON		:
		case SIMPHONY2	:
		case CUSTOM 	: return getJavaCode(member,innerType,filter);
		case NETLOGO5	:
		case NETLOGO	: return getNetLogoCode(member,filter);
		default			: throw new UnsupportedPlatformException(PlatformSettings.getPlatformType().toString());		
		}
	}
	
	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	private String getJavaCode(final MemberInfo member, final Class<?> innerType, final String filter) {
		final String actFilter = filter.replaceAll(Utilities.FILTER_EXP,"act");
		final StringBuilder code = new StringBuilder();
		code.append("java.util.ArrayList list = new java.util.ArrayList();\n");
		if (member.getType().endsWith("[]")) {
			code.append("for (int i = 0; i < ").append(member.getName()).append(".length; ++i) {\n");
			code.append(innerType.getCanonicalName()).append(" act = ").append(member.getName()).append("[i];\n");
		} else {
			code.append("for (int i = 0; i < ").append(member.getName()).append(".size(); ++i) {\n");
			code.append(innerType.getCanonicalName()).append(" act = (").append(innerType.getCanonicalName()).append(") ").append(member.getName()).
				 append(".get(i);\n");
		}
		code.append("if (").append(actFilter).append(") {\n");
		if (innerType.isPrimitive())
			code.append("list.add(new ").append(boxingType(member.getJavaType()).getName()).append("(").append(member.getName()).append("));\n");
		else
			code.append("list.add(").append(member.getName()).append(");\n");
		code.append("}\n");
		code.append("}\n");
		code.append("return list;\n");
		return code.toString();
	}
	
	//----------------------------------------------------------------------------------------------------
	private Class<?> boxingType(final Class<?> memberType) {
		if (Byte.TYPE.equals(memberType))
			return Byte.class;
		if (Short.TYPE.equals(memberType))
			return Short.class;
		if (Integer.TYPE.equals(memberType))
			return Integer.class;
		if (Long.TYPE.equals(memberType))
			return Long.class;
		if (Float.TYPE.equals(memberType))
			return Float.class;
		if (Double.TYPE.equals(memberType))
			return Double.class;
		if (Boolean.TYPE.equals(memberType))
			return Boolean.class;
		if (Character.TYPE.equals(memberType))
			return Character.class;
		return memberType;
	}

	//----------------------------------------------------------------------------------------------------
	private String getNetLogoCode(final MemberInfo member, final String filter) {
		if (member.getJavaType().equals(list.class)) { 
			final String actFilter = filter.replaceAll(Utilities.FILTER_EXP,"?");
			return "report filter [" + actFilter + "] " + member.getName() + "\n";
		} else {
			final String actFilter = filter.replaceAll(Utilities.FILTER_EXP + "\\.","");
			return "report " + member.getName() + " with [" + actFilter + "]\n"; 
		} 
	}
}
