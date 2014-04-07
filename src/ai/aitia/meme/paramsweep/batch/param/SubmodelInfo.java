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

import java.util.ArrayList;
import java.util.List;

public class SubmodelInfo<T> extends ParameterInfo<T> implements ISubmodelParameterInfo {
	
	//====================================================================================================
	// members
	
	private static final long serialVersionUID = -6014247442037116214L;
	
	protected List<Class<?>> possibleTypes;
	protected Class<?> referenceType;
	protected Class<?> actualType;
	protected SubmodelInfo<?> parent;

	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	/** Creates an instance of type constant. */
	public SubmodelInfo(final String name, final String description, final T defaultValue, final List<Class<?>> possibleTypes, final Class<?> referenceType) {
		super(name,description,defaultValue);
		setValue(defaultValue);
		this.possibleTypes = possibleTypes;
		this.referenceType = referenceType;
	}
	
	//----------------------------------------------------------------------------------------------------
	public SubmodelInfo(final String name, final String description, final T defaultValue, final boolean originalConstant,
						final List<Class<?>> possibleTypes, final Class<?> referenceType) {
		super(name,description,defaultValue,originalConstant);
		this.possibleTypes = possibleTypes;
		this.referenceType = referenceType;
	}
	
	//----------------------------------------------------------------------------------------------------
	public SubmodelInfo(final SubmodelInfo<T> p) {
		super(p);
		this.possibleTypes = p.possibleTypes;
		this.referenceType = p.referenceType;
	}

	//----------------------------------------------------------------------------------------------------
	public List<Class<?>> getPossibleTypes() { return possibleTypes; }
	public Class<?> getReferenceType() { return referenceType; }
	public Class<?> getActualType() { return actualType; }
	public SubmodelInfo<?> getParentInfo() { return parent; }

	//----------------------------------------------------------------------------------------------------
	public void setPossibleTypes(final List<Class<?>> possibleTypes) { this.possibleTypes = possibleTypes; }
	public void setReferenceType(final Class<?> referenceType) { this.referenceType = referenceType; }
	public void setActualType(final Class<?> actualType) { this.actualType = actualType; }
	public void setParent(final SubmodelInfo<?> parent) { this.parent = parent; }
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public void setValue(T o) {
		valueType = ValueType.CONSTANT;
		values = new ArrayList<T>();
		values.add(o);
		originalConstant = true;
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public SubmodelInfo<T> clone() {
		final List<Class<?>> possibleTypesClone = new ArrayList<Class<?>>(this.possibleTypes);
		final SubmodelInfo<T> clone = new SubmodelInfo<T>(this.name,this.description,this.defaultValue,this.originalConstant,possibleTypesClone,this.referenceType);
		clone.actualType = this.actualType;
		clone.parent = this.parent;
		
		return clone;
	}
}