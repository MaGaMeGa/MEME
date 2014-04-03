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

import ai.aitia.meme.paramsweep.gui.info.ConstantKeyInfo;
import ai.aitia.meme.paramsweep.gui.info.MemberInfo;
import ai.aitia.meme.paramsweep.platform.PlatformManager.PlatformType;
import ai.aitia.meme.paramsweep.plugin.IOperatorPlugin;
import ai.aitia.meme.paramsweep.utils.Utilities;

public class Operator_MapSelection implements IOperatorPlugin {
	
	//====================================================================================================
	// implemented interfaces
	
	//----------------------------------------------------------------------------------------------------
	public String getName() { return "mapSelection"; }
	public String getLocalizedName() { return "Map selection"; }
	public int getNumberOfParameters() { return 3; } // map, value type, key
	public OperatorGUIType getGUIType() { return OperatorGUIType.MAP_SELECTION; }
	public boolean isSupportedByPlatform(PlatformType platform) { return platform != PlatformType.NETLOGO && platform != PlatformType.NETLOGO5; }
	
	//----------------------------------------------------------------------------------------------------
	public String getDescription() {
		return "Selects an element of a map.";
	}
	
	//----------------------------------------------------------------------------------------------------
	public String getCode(Object... actParams) {
		MemberInfo map = (MemberInfo) actParams[0];
		Class innerType = (Class) actParams[1];
		MemberInfo key = (MemberInfo) actParams[2];
		if (key instanceof ConstantKeyInfo) {
			ConstantKeyInfo _key = (ConstantKeyInfo) key;
			if (_key.isStringKey())
				return "return (" + innerType.getCanonicalName() + ") " + map.getName() + ".get(\"" + key.getName() + "\");\n";
			else 
				return generateNumberTypeCode(map,_key,innerType);
		} 
		return "return (" + innerType.getCanonicalName() + ") " + map.getName() + ".get(" + key.getName() + ");\n";
	}
	
	//----------------------------------------------------------------------------------------------------
	public Class<?> getReturnType(Object... actParams) { return (Class) actParams[1]; }
	
	//----------------------------------------------------------------------------------------------------
	public String getInstanceDisplayName(String name, Object... actParams) {
		String _name = name;
		if (_name.endsWith("()"))
			_name = _name.substring(0,_name.length() - 2);
		MemberInfo object = (MemberInfo) actParams[0];
		MemberInfo key = (MemberInfo) actParams[2];
//		return Utilities.name(object) + "[" + Utilities.name(key) + "]";
		return _name + "(" + Utilities.name(object) + "," + Utilities.name(key) + ")";
	}

	//----------------------------------------------------------------------------------------------------
	public boolean isRecordable(Object... actParams) {
		Class innerType = (Class) actParams[1];
		return (innerType.isPrimitive() && !innerType.equals(Character.TYPE)) ||
				Number.class.isAssignableFrom(innerType) ||
				innerType.equals(String.class);
	}
	
	//----------------------------------------------------------------------------------------------------
	public List<String> checkParameters(Object... actParams) {
		List<String> result = new ArrayList<String>();
		if (!(actParams[0] instanceof MemberInfo))
			result.add("Parameter #0 is not a MemberInfo instance.");
		if (!(actParams[1] instanceof Class))
			result.add("Parameter #1 is not a Class instance.");
		if (!(actParams[2] instanceof MemberInfo))
			result.add("Parameter #2 is not a MemberInfo instance.");
		return result;
	}
	
	//====================================================================================================
	// private methods
	
	//----------------------------------------------------------------------------------------------------
	private String generateNumberTypeCode(MemberInfo map, ConstantKeyInfo key, Class innerType) {
		String prefix = "return (" + innerType.getCanonicalName() + ") " + map.getName() + ".get(new ";
		String result = "";
		result += generateIf(map,key,"Byte",prefix);
		result += generateIf(map,key,"Short",prefix);
		result += generateIf(map,key,"Integer",prefix);
		result += generateIf(map,key,"Long",prefix);
		result += generateIf(map,key,"Float",prefix);
		result += generateIf(map,key,"Double",prefix);
		result += "return null;\n";
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	private String generateIf(MemberInfo map, ConstantKeyInfo key, String type, String prefix) {
		String cast = numberValueCasting(key,type);
		String result = "if (" + map.getName() + ".containsKey(new " + type + "(\"" + cast + "\"))) {\n";
		result += prefix + type + "(\"" + cast + "\"));\n";
		result += "}\n";
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	private String numberValueCasting(ConstantKeyInfo key, String type) {
		if ("Byte".equals(type))
			return String.valueOf(key.getNumberValue().byteValue());
		if ("Short".equals(type))
			return String.valueOf(key.getNumberValue().shortValue());
		if ("Integer".equals(type))
			return String.valueOf(key.getNumberValue().intValue());
		if ("Long".equals(type))
			return String.valueOf(key.getNumberValue().longValue());
		if ("Float".equals(type))
			return String.valueOf(key.getNumberValue().floatValue());
		if ("Double".equals(type))
			return String.valueOf(key.getNumberValue().doubleValue());
		return "";
	}
	
	
}
