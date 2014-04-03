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
import ai.aitia.meme.paramsweep.plugin.IOperatorPlugin;
import ai.aitia.meme.paramsweep.utils.Utilities;

public class Operator_Selection implements IOperatorPlugin {

	//====================================================================================================
	// implemented interfaces
	
	//----------------------------------------------------------------------------------------------------
	public String getName() { return "selection"; }
	public String getLocalizedName() { return "Member selection"; }
	public int getNumberOfParameters() { return 2; }
	public OperatorGUIType getGUIType() { return OperatorGUIType.MEMBER_SELECTION; }
	public boolean isSupportedByPlatform(PlatformType platform) { return platform != PlatformType.NETLOGO && platform != PlatformType.NETLOGO5; }
	
	//----------------------------------------------------------------------------------------------------
	public String getDescription() {
		return "Selects a member of an object.";
	}

	//----------------------------------------------------------------------------------------------------
	public String getCode(Object... actParams) {
		MemberInfo object = (MemberInfo) actParams[0];
		MemberInfo member = (MemberInfo) actParams[1];
		return "return " + object.getName() + "." + member.getName() + ";\n";
	}
	
	//----------------------------------------------------------------------------------------------------
	public Class<?> getReturnType(Object... actParams) { 
		MemberInfo member = (MemberInfo) actParams[1];
		return member.getJavaType();
	}
	
	//----------------------------------------------------------------------------------------------------
	public String getInstanceDisplayName(String name, Object... actParams) {
		String _name = name;
		if (_name.endsWith("()"))
			_name = _name.substring(0,_name.length() - 2);
		MemberInfo object = (MemberInfo) actParams[0];
		MemberInfo member = (MemberInfo) actParams[1];
//		return Utilities.name(object) + "." + Utilities.name(member);
		return _name + "(" + Utilities.name(object) + "," + Utilities.name(member) + ")";
	}

	//----------------------------------------------------------------------------------------------------
	public boolean isRecordable(Object... actParams) {
		MemberInfo member = (MemberInfo) actParams[1];
		return member.isNumeric() || member.isBoolean() || member.getType().equals("String");
	}
	
	//----------------------------------------------------------------------------------------------------
	public List<String> checkParameters(Object... actParams) {
		List<String> result = new ArrayList<String>();
		for (int i = 0;i < getNumberOfParameters();++i) {
			if (!(actParams[i] instanceof MemberInfo))
				result.add("Parameter #" + i + "is not a MemberInfo instance.");
		}
		return result;
	}
}
