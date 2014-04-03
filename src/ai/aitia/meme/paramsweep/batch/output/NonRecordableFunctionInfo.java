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
package ai.aitia.meme.paramsweep.batch.output;

import java.util.List;

/** Class representing non-recordable methods. */
public class NonRecordableFunctionInfo extends NonRecordableInfo {

	//====================================================================================================
	// members
	
	private static final long serialVersionUID = 1287790128353102253L;
	protected List<Class<?>> parameterTypes = null;

	//====================================================================================================
	// methods

	//----------------------------------------------------------------------------------------------------
	public NonRecordableFunctionInfo(String name, Class type, String accessibleName, List<Class<?>> parameterTypes) {
		super(name,type,accessibleName);
		this.parameterTypes = parameterTypes;
	}
	
	//----------------------------------------------------------------------------------------------------
	public NonRecordableFunctionInfo(String name, Class type, String description, String accessibleName, List<Class<?>> parameterTypes) {
		super(name,type,description,accessibleName);
		this.parameterTypes = parameterTypes;
	}
	
	//----------------------------------------------------------------------------------------------------
	public List<Class<?>> getParameterTypes() { return parameterTypes; }
}
