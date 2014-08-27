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
import ai.aitia.meme.paramsweep.platform.PlatformManager;
import ai.aitia.meme.paramsweep.platform.PlatformManager.PlatformType;
import ai.aitia.meme.paramsweep.plugin.IExtendedOperatorPlugin;
import ai.aitia.meme.paramsweep.plugin.IListOperatorPlugin;
import ai.aitia.meme.paramsweep.utils.AssistantMethod.ScheduleTime;
import ai.aitia.meme.paramsweep.utils.PlatformConstants;
import ai.aitia.meme.paramsweep.utils.Utilities;

public class Operator_TimeSeries implements IExtendedOperatorPlugin, IListOperatorPlugin {

	//====================================================================================================
	// implemented interfaces
	
	//----------------------------------------------------------------------------------------------------
	public String getName() { return "timeseries"; }
	public String getLocalizedName() { return "Timeseries"; }
	public int getNumberOfParameters() { return 1; }
	public OperatorGUIType getGUIType() { return OperatorGUIType.TIME_SERIES; }
	public boolean isRecordable(final Object... actParams) { return false; }
	public boolean isSupportedByPlatform(final PlatformType platform) { return true; }
	public String getDescription() { return "Appends the current value of the parameter to 'history' list and returns the list."; }
	
	//----------------------------------------------------------------------------------------------------
	public Class<?> getReturnType(final Object... actParams) { 
		return (PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5) ? list.class : List.class;
	}
	
	//----------------------------------------------------------------------------------------------------
	public Class<?> getInnerClass(Object... actParams) {
		MemberInfo member = (MemberInfo) actParams[0];
		return boxingType(member.getJavaType());
	}
	
	//----------------------------------------------------------------------------------------------------
	public String getCode(final Object... actParams) {
		final MemberInfo member = (MemberInfo) actParams[0];
		switch (PlatformSettings.getPlatformType()) {
		case REPAST		:
		case MASON		:
		case SIMPHONY2	:
		case CUSTOM 	: return getJavaCode(member);
		case NETLOGO5	:
		case NETLOGO	: return getNetLogoCode(member);
		default			: throw new UnsupportedPlatformException(PlatformSettings.getPlatformType().toString());		
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public String getInstanceDisplayName(final String name, final Object... actParams) {
		String _name = name;
		if (_name.endsWith("()"))
			_name = _name.substring(0,_name.length() - 2);
		final MemberInfo member = (MemberInfo) actParams[0];
		return _name + "(" + Utilities.name(member) + ")";
	}
	
	//----------------------------------------------------------------------------------------------------
	public List<String> checkParameters(final Object... actParams) {
		final List<String> result = new ArrayList<String>();
		if (!(actParams[0] instanceof MemberInfo))
			result.add("Parameter #0 is not a MemberInfo instance.");
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	public int getAssistantMethodCount() { return 1; }
	public ScheduleTime getScheduleTime(int idx) { return ScheduleTime.TICK; }
	public Class<?> getReturnType(int idx, Object... actParams) { return Void.TYPE; }
	
	//----------------------------------------------------------------------------------------------------
	public String getCode(final int idx, final Object... actParams) {
		if (idx > 0)
			throw new IllegalArgumentException("Only 1 assistant method exists.");
		final MemberInfo member = (MemberInfo) actParams[0];
		switch (PlatformSettings.getPlatformType()) {
		case REPAST		:
		case MASON		:
		case SIMPHONY2	:
		case CUSTOM 	: return getAssistantJavaCode(member);
		case NETLOGO5	:
		case NETLOGO	: return getAssistantNetLogoCode(member);
		default			: throw new UnsupportedPlatformException(PlatformSettings.getPlatformType().toString());		
		}
		
	}
	
	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	private String getJavaCode(final MemberInfo member) {
		final StringBuilder code = new StringBuilder();
		code.append("java.util.ArrayList list = (java.util.ArrayList) ").append(PlatformConstants.AITIA_GENERATED_VARIABLES).
			 append(".get(\"").append(member.getName()).append("\");\n");
		code.append("if (list == null) {\n");
			code.append("list = new java.util.ArrayList();\n");
		code.append("}\n");
		code.append("return list;\n");
		return code.toString();
	}
	
	//----------------------------------------------------------------------------------------------------
	private String getAssistantJavaCode(final MemberInfo member) {
		final StringBuilder code = new StringBuilder();
		code.append("java.util.ArrayList list = (java.util.ArrayList) ").append(PlatformConstants.AITIA_GENERATED_VARIABLES).append(".get(\"").
			 append(member.getName()).append("\");\n");
		code.append("if (list == null) {\n");
			code.append("list = new java.util.ArrayList();\n");
			code.append(PlatformConstants.AITIA_GENERATED_VARIABLES).append(".put(\"").append(member.getName()).append("\",list);\n");
		code.append("}\n");
		if (member.getJavaType().isPrimitive())
			code.append("list.add(new ").append(boxingType(member.getJavaType()).getName()).append("(").append(member.getName()).append("));\n");
		else
			code.append("list.add(").append(member.getName()).append(");\n");
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
	private String getNetLogoCode(final MemberInfo member) {
		final StringBuilder code = new StringBuilder();
		code.append("let _aitia_generated_list []\n");
		code.append("if table").append(version()).append(":has-key? ").append(PlatformConstants.AITIA_GENERATED_VARIABLES).append(" \"").append(member.getName()).
			 append("\"\n");
			code.append("[ set _aitia_generated_list table").append(version()).append(":get ").append(PlatformConstants.AITIA_GENERATED_VARIABLES).append(" \"").
				 append(member.getName()).append("\" ]\n");
		code.append("report _aitia_generated_list\n");
		return code.toString();
	}
	
	//----------------------------------------------------------------------------------------------------
	private String getAssistantNetLogoCode(final MemberInfo member) {
		final StringBuilder code = new StringBuilder();
		code.append("let _aitia_generated_list []\n");
		code.append("if table").append(version()).append(":has-key? ").append(PlatformConstants.AITIA_GENERATED_VARIABLES).append(" \"").append(member.getName()).
			 append("\"\n");
			code.append("[ set _aitia_generated_list table").append(version()).append(":get ").append(PlatformConstants.AITIA_GENERATED_VARIABLES).append(" \"").
				 append(member.getName()).append("\" ]\n");
		code.append("let aitia_generated_new_list sentence _aitia_generated_list ").append(member.getName()).append("\n");
		code.append("table").append(version()).append(":put ").append(PlatformConstants.AITIA_GENERATED_VARIABLES).append(" \"").append(member.getName()).
			 append("\" aitia_generated_new_list\n");
		return code.toString();
	}
	
	//----------------------------------------------------------------------------------------------------
	private String version() {
		final String fullVersion = PlatformManager.getPlatform(PlatformSettings.getPlatformType()).getVersion();
		return '5' == fullVersion.charAt(0) ? "5" : ""; 
	}
}
