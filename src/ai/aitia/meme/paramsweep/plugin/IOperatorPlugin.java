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
package ai.aitia.meme.paramsweep.plugin;

import java.util.List;

import ai.aitia.meme.paramsweep.platform.PlatformManager.PlatformType;

public interface IOperatorPlugin extends IPSScriptPlugin {
	
	//====================================================================================================
	// nested types
	
	public static enum OperatorGUIType {
		MEMBER_SELECTION, ELEMENT_SELECTION, SPEC_ELEMENT_SELECTION, MAP_SELECTION, LIST, LIST_SELECTION, ANY_TYPE_CONSTRUCT, ONEONE_CONSTRUCT,
		LIST_UNION_INTERSECTION, BINARY_LIST_CONSTRUCT, SIZE, REMOVE, FOREACH, TIME_SERIES, FILTER, MULTIPLE_COLUMN
	}
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	/**
	 * Creates the code for the operator. The code should produce the result of the operation on the actParams parameters.
	 * @param actParams	The parameters of the operator.
	 * @return	A code segment that produces the result.
	 */
	public String getCode(Object... actParams);
	public boolean isRecordable(Object... actParams);
	public OperatorGUIType getGUIType();
	public List<String> checkParameters(Object... actParams);
	public Class<?> getReturnType(Object... actParams);
	public String getInstanceDisplayName(String name, Object... actParams);
	public boolean isSupportedByPlatform(PlatformType platform);
}
