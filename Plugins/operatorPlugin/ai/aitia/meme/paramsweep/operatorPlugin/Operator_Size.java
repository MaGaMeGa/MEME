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
import java.util.Collection;
import java.util.List;
import java.util.Map;

import _.agentset;
import ai.aitia.meme.paramsweep.gui.info.MemberInfo;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings.UnsupportedPlatformException;
import ai.aitia.meme.paramsweep.platform.PlatformManager.PlatformType;
import ai.aitia.meme.paramsweep.plugin.IOperatorPlugin;
import ai.aitia.meme.paramsweep.utils.Utilities;

public class Operator_Size implements IOperatorPlugin {

	//====================================================================================================
	// implemented interfaces
	
	//----------------------------------------------------------------------------------------------------
	public String getName() { return "size"; }
	public String getLocalizedName() { return "Size"; }
	public int getNumberOfParameters() { return 1; }
	public OperatorGUIType getGUIType() { return OperatorGUIType.SIZE; }
	public boolean isRecordable(Object... actParams) { return true; }
	public boolean isSupportedByPlatform(PlatformType platform) { return true; }
	
	//----------------------------------------------------------------------------------------------------
	public Class<?> getReturnType(Object... actParams) { 
		return (PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5) ? Double.TYPE : Integer.TYPE;
	}
	
	//----------------------------------------------------------------------------------------------------
	public String getDescription() {
		String desc = "Returns the size of an arbitrary collection";
		if (PlatformSettings.getPlatformType() != PlatformType.NETLOGO && PlatformSettings.getPlatformType() != PlatformType.NETLOGO5)
			desc += "/array";
		return desc + "/object.";
	}
	
	//----------------------------------------------------------------------------------------------------
	public String getCode(Object... actParams) {
		MemberInfo member = (MemberInfo) actParams[0];
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
	public String getInstanceDisplayName(String name, Object... actParams) {
		String _name = name;
		if (_name.endsWith("()"))
			_name = _name.substring(0,_name.length() - 2);
		MemberInfo member = (MemberInfo) actParams[0];
		return _name + "(" + Utilities.name(member) + ")";
	}
	
	//----------------------------------------------------------------------------------------------------
	public List<String> checkParameters(Object... actParams) {
		List<String> result = new ArrayList<String>();
		if (!(actParams[0] instanceof MemberInfo))
			result.add("Parameter #0 is not a MemberInfo instance.");
		return result;
	}
	
	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	private String getJavaCode(MemberInfo member) {
		if (member.getJavaType().isArray())
			return "return " + member.getName() + ".length;\n";
		else if (Collection.class.isAssignableFrom(member.getJavaType()) ||
				 Map.class.isAssignableFrom(member.getJavaType()))
			return "return " + member.getName() + ".size();\n";
		else 
			return "return 1;\n";
	}

	//----------------------------------------------------------------------------------------------------
	private String getNetLogoCode(MemberInfo member) {
		if (agentset.class.equals(member.getJavaType()))
			return "report count " + member.getName();
		else
			return "ifelse is-agentset? " + member.getName() + "\n[report count " + member.getName() + "]\n[ifelse is-list? " +
		       	   member.getName() + "\n[report length " + member.getName() + "]\n[report 1]\n]";
	}
}
