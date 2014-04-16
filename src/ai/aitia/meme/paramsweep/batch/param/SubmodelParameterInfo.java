/*******************************************************************************
 * Copyright (C) 2006-2014 AITIA International, Inc.
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
package ai.aitia.meme.paramsweep.batch.param;


public class SubmodelParameterInfo<T> extends ParameterInfo<T> implements ISubmodelParameterInfo {
	
	//====================================================================================================
	// members
	
	private static final long serialVersionUID = -6469952187619533059L;
	
	private final SubmodelInfo<?> parent;
	
	//====================================================================================================
	// methods

	//----------------------------------------------------------------------------------------------------
	public SubmodelParameterInfo(final String name, final String description, final T defaultValue, final boolean originalConstant, final SubmodelInfo<?> parent) {
		super(name,description,defaultValue,originalConstant);
		this.parent = parent;
	}

	//----------------------------------------------------------------------------------------------------
	public SubmodelParameterInfo(final String name, final String description, final T defaultValue, final SubmodelInfo<?> parent) {
		super(name,description,defaultValue);
		this.parent = parent;
	}
	
	//----------------------------------------------------------------------------------------------------
	public SubmodelParameterInfo(final ParameterInfo<T> p, final SubmodelInfo<?> parent) {
		super(p);
		this.parent = parent;
	}

	//----------------------------------------------------------------------------------------------------
	public SubmodelInfo<?> getParentInfo() { return parent; }
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public SubmodelParameterInfo<T> clone() {
		final SubmodelParameterInfo<T> clone = new SubmodelParameterInfo<T>(this.name,this.description,this.defaultValue,this.originalConstant,this.parent);
		
		return clone;
	}
}