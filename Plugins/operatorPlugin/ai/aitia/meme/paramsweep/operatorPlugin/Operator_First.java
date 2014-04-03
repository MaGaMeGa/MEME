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

import java.util.List;

import _.list;
import ai.aitia.meme.paramsweep.gui.info.MemberInfo;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings.UnsupportedPlatformException;
import ai.aitia.meme.paramsweep.platform.PlatformManager.PlatformType;
import ai.aitia.meme.paramsweep.plugin.IOperatorPlugin;
import ai.aitia.meme.paramsweep.utils.Utilities;

public class Operator_First extends Operator_ElementSelection implements IOperatorPlugin {

	//====================================================================================================
	// implemented interfaces
	
	//----------------------------------------------------------------------------------------------------
	@Override public String getName() { return "first"; }
	@Override public String getLocalizedName() { return "First element"; }
	@Override public int getNumberOfParameters() { return 2; } // collection, inner type
	@Override public OperatorGUIType getGUIType() { return OperatorGUIType.SPEC_ELEMENT_SELECTION; }
	@Override public boolean isRecordable(Object... actParams) { return super.isRecordable(actParams[0],0,actParams[1]); }
	@Override public List<String> checkParameters(Object... actParams) { return super.checkParameters(actParams[0],0,actParams[1]); }

	//----------------------------------------------------------------------------------------------------
	@Override
	public String getDescription() {
		String desc = "Selects the first element of a collection";
		if (PlatformSettings.getPlatformType() != PlatformType.NETLOGO && PlatformSettings.getPlatformType() != PlatformType.NETLOGO5)
			desc += "(or an array)";
		return desc + ".";
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override public Class<?> getReturnType(Object... actParams) { return (Class) actParams[1]; }
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public String getInstanceDisplayName(String name, Object... actParams) {
		String _name = name;
		if (_name.endsWith("()"))
			_name = _name.substring(0,_name.length() - 2);
		MemberInfo object = (MemberInfo) actParams[0];
		return _name + "(" + Utilities.name(object) + ")";
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public String getCode(Object... actParams) {
		switch (PlatformSettings.getPlatformType()) {
		case REPAST		:
		case MASON		:
		case SIMPHONY2	:
		case CUSTOM 	: return super.getCode(actParams[0],0,actParams[1]);
		case NETLOGO5	:
		case NETLOGO	: return getNetLogoCode((MemberInfo)actParams[0]);
		default			: throw new UnsupportedPlatformException(PlatformSettings.getPlatformType().toString());		
		}
	}
	
	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	private String getNetLogoCode(MemberInfo info) { 
		String prefix = "report first ";
		if (info.getJavaType().equals(list.class))
			return prefix + info.getName();
		else
			return prefix + "(sort " + info.getName() + ")";
	}
}
