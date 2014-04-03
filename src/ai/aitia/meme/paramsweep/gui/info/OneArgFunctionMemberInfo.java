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
package ai.aitia.meme.paramsweep.gui.info;

import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings;
import ai.aitia.meme.paramsweep.platform.PlatformManager.PlatformType;

public class OneArgFunctionMemberInfo extends ArgsFunctionMemberInfo {

	//====================================================================================================
	// members
	
	private static final long serialVersionUID = -2957340213075149396L;
	protected Class<?> parameterType = null;
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public OneArgFunctionMemberInfo(String name, String returnType, Class<?> returnJavaType, Class<?> parameterType) {
		super(name,returnType,returnJavaType);
		this.parameterType = parameterType;
	}
	
	//----------------------------------------------------------------------------------------------------
	public Class<?> getParameterType() { return parameterType; }
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		if (PlatformSettings.getPlatformType() != PlatformType.NETLOGO && PlatformSettings.getPlatformType() != PlatformType.NETLOGO5) {
			String str = name.substring(0,name.length() - 1) + parameterType.getSimpleName() + ")";
			return str + " : " + (type.equals("List") ? javaType.getSimpleName() : type);
		} else 
			return name + "[" + parameterType.getSimpleName() + "] : " + type;
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public int compareTo(NodeInfo o) {
		if (o instanceof OneArgFunctionMemberInfo) {
			OneArgFunctionMemberInfo other = (OneArgFunctionMemberInfo) o;
			String str = name.substring(0,name.length() - 1) + parameterType.getSimpleName() + ")";
			String other_str = other.name.substring(0,other.name.length() - 1) + other.parameterType.getSimpleName() + ")";
			return (PlatformSettings.getPlatformType() != PlatformType.NETLOGO  && PlatformSettings.getPlatformType() != PlatformType.NETLOGO5 ) ? str.compareTo(other_str) : name.compareTo(other.name);
		}
		return super.compareTo(o);
	}

	//----------------------------------------------------------------------------------------------------
	@Override public boolean equals(Object o) {
		if (o instanceof OneArgFunctionMemberInfo) {
			OneArgFunctionMemberInfo that = (OneArgFunctionMemberInfo) o;
			return compareTo(that) == 0;
		}
		return false;
	}
}
