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
import ai.aitia.meme.paramsweep.gui.info.MemberInfo;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings.UnsupportedPlatformException;
import ai.aitia.meme.paramsweep.platform.PlatformManager.PlatformType;
import ai.aitia.meme.paramsweep.plugin.IListOperatorPlugin;
import ai.aitia.meme.paramsweep.utils.Utilities;

public class Operator_Permutation implements IListOperatorPlugin {
	
	//====================================================================================================
	// implemented interfaces
	
	//----------------------------------------------------------------------------------------------------
	public String getName() { return "permutation"; }
	public String getLocalizedName() { return "Random permutation"; }
	public int getNumberOfParameters() { return 1; }
	public OperatorGUIType getGUIType() { return OperatorGUIType.ANY_TYPE_CONSTRUCT; }
	public boolean isRecordable(Object... actParams) { return false; }
	public boolean isSupportedByPlatform(PlatformType platform) { return true; }

	//----------------------------------------------------------------------------------------------------
	public Class<?> getReturnType(Object... actParams) { 
		return (PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5) ? ((MemberInfo)actParams[0]).getJavaType() : ArrayList.class;
	}

	//----------------------------------------------------------------------------------------------------
	public String getDescription() {
		String desc = "Returns the permutation of the selected list/";
		if (PlatformSettings.getPlatformType() != PlatformType.NETLOGO && PlatformSettings.getPlatformType() != PlatformType.NETLOGO5)
			desc += "array in a list";
		else
			desc += "agentset";
		return desc + ".";
	}

	//----------------------------------------------------------------------------------------------------
	public Class<?> getInnerClass(Object... actParams) {
		MemberInfo info = (MemberInfo) actParams[0];
		return (PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5) ? info.getInnerType() : boxingType(info.getInnerType());
	}

	//----------------------------------------------------------------------------------------------------
	public String getInstanceDisplayName(String name, Object... actParams) {
		String _name = name;
		if (_name.endsWith("()"))
			_name = _name.substring(0,_name.length() - 2);
		MemberInfo info = (MemberInfo) actParams[0];
		return _name + "(" + Utilities.name(info) + ")";

	}
	
	//----------------------------------------------------------------------------------------------------
	public List<String> checkParameters(Object... actParams) {
		List<String> result = new ArrayList<String>();
		if (!(actParams[0] instanceof MemberInfo))
			result.add("Parameter #0 is not a MemberInfo instance.");
		return result;
	}

	//----------------------------------------------------------------------------------------------------
	public String getCode(Object... actParams) {
		MemberInfo info = (MemberInfo) actParams[0];
		switch (PlatformSettings.getPlatformType()) {
		case REPAST		:
		case MASON		:
		case SIMPHONY2	:
		case CUSTOM 	: return getJavaCode(info);
		case NETLOGO5	:
		case NETLOGO	: return getNetLogoCode(info);
		default			: throw new UnsupportedPlatformException(PlatformSettings.getPlatformType().toString());		
		}
	}
	
	//====================================================================================================
	// private methods
	
	//----------------------------------------------------------------------------------------------------
	private String getJavaCode(MemberInfo info) {
		StringBuilder code = new StringBuilder();
		code.append("java.util.ArrayList result = new java.util.ArrayList();\n");
		if (info.getType().endsWith("[]")) {
			if (info.getInnerType().isPrimitive()) {
				code.append("for (int i = 0;i < " + info.getName() + ".length;++i) {\n");
					code.append("result.add(new " + boxingType(info.getInnerType()).getName() + "(" + info.getName() + "[i]));\n");
				code.append("}\n");
			} else
				code.append("result.addAll(java.util.Arrays.asList(" + info.getName() + "));\n");
		} else 
				code.append("result.addAll(" + info.getName() + ");\n");
		code.append("java.util.Collections.shuffle(result);\n");
		code.append("return result;\n");
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
	
	//----------------------------------------------------------------------------------------------------
	private String getNetLogoCode(final MemberInfo info) {
		if (agentset.class.equals(info.getJavaType()))
			return "report " + info.getName() + "\n";
		else { 
			final StringBuilder code = new StringBuilder();
			code.append("ifelse is-list? ").append(info.getName()).append("\n");
			code.append("[report shuffle ").append(info.getName()).append("]\n");
			code.append("[report ").append(info.getName()).append("]");
			return code.toString();
		}
	}
}
