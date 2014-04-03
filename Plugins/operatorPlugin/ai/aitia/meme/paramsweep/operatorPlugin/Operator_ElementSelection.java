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
import ai.aitia.meme.paramsweep.plugin.IOperatorPlugin;
import ai.aitia.meme.paramsweep.utils.Utilities;

public class Operator_ElementSelection implements IOperatorPlugin {

	//====================================================================================================
	// implemented interfaces
	
	//----------------------------------------------------------------------------------------------------
	public String getName() { return "elementSelection"; }
	public String getLocalizedName() { return "Element selection"; }
	public int getNumberOfParameters() { return 3; } // collection, index, inner type
	public OperatorGUIType getGUIType() { return OperatorGUIType.ELEMENT_SELECTION; }
	public boolean isSupportedByPlatform(PlatformType platform) { return true; }
	
	//----------------------------------------------------------------------------------------------------
	public String getDescription() {
		String desc = "Selects an element of a collection";
		if (PlatformSettings.getPlatformType() != PlatformType.NETLOGO && PlatformSettings.getPlatformType() != PlatformType.NETLOGO5)
			desc += "(or an array)";
		return desc + ".";
	}
	
	//----------------------------------------------------------------------------------------------------
	public String getCode(Object... actParams) {
		MemberInfo object = (MemberInfo) actParams[0];
		int index = ((Integer)actParams[1]).intValue();
		Class innerType = (Class) actParams[2];
		switch (PlatformSettings.getPlatformType()) {
		case REPAST		:
		case MASON		:
		case SIMPHONY2	:
		case CUSTOM 	: return getJavaCode(object,index,innerType);
		case NETLOGO5	:
		case NETLOGO	: return getNetLogoCode(object,index,innerType);
		default			: throw new UnsupportedPlatformException(PlatformSettings.getPlatformType().toString());		
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public Class<?> getReturnType(Object... actParams) { return (Class) actParams[2]; }
	
	//----------------------------------------------------------------------------------------------------
	public String getInstanceDisplayName(String name, Object... actParams) {
		String _name = name;
		if (_name.endsWith("()"))
			_name = _name.substring(0,_name.length() - 2);
		MemberInfo object = (MemberInfo) actParams[0];
		int index = ((Integer)actParams[1]).intValue();
		return _name + "(" + Utilities.name(object) + "," + String.valueOf(index) + ")";
	}

	//----------------------------------------------------------------------------------------------------
	public boolean isRecordable(Object... actParams) {
		Class innerType = (Class) actParams[2];
		return (innerType.isPrimitive() && !innerType.equals(Character.TYPE)) ||
				Number.class.isAssignableFrom(innerType) ||
				innerType.equals(String.class);
	}
	
	//----------------------------------------------------------------------------------------------------
	public List<String> checkParameters(Object... actParams) {
		List<String> result = new ArrayList<String>();
		if (!(actParams[0] instanceof MemberInfo))
			result.add("Parameter #0 is not a MemberInfo instance.");
		if (!(actParams[1] instanceof Integer))
			result.add("Parameter #1 is not an Integer instance.");
		if (!(actParams[2] instanceof Class))
			result.add("Parameter #2 is not a Class instance.");
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	private String getJavaCode(MemberInfo object, int index, Class innerType) {
		if (object.getType().endsWith("[]")) // array type
			return "return " + object.getName() + "[" + String.valueOf(index) + "];\n";
		else 
			return "return (" + innerType.getName() + ") " + object.getName() + ".toArray()[" + String.valueOf(index) + "];\n";
	}
	
	//----------------------------------------------------------------------------------------------------
	private String getNetLogoCode(MemberInfo object, int index, Class innerType) {
		String prefix = "report item " + index + " ";
		if (object.getJavaType().equals(list.class))
			return prefix + object.getName();
		else
			return prefix + "(sort " + object.getName() + ")";
	}
}
