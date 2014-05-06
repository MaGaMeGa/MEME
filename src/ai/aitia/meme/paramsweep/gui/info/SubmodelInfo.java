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
package ai.aitia.meme.paramsweep.gui.info;

import java.util.ArrayList;
import java.util.List;

public class SubmodelInfo extends ParameterInfo implements ISubmodelGUIInfo {
	
	//====================================================================================================
	// members
	
	private static final long serialVersionUID = 6758605100283402861L;
	
	private List<Class<?>> possibleTypes;
	private Class<?> actualType;
	private SubmodelInfo parent;
	private Class<?> parentValue;
	
	//====================================================================================================
	// methods

	//----------------------------------------------------------------------------------------------------
	public SubmodelInfo(final String name, final String type, final Class<?> javaType, final List<Class<?>> possibleTypes, final SubmodelInfo parent) {
		this(name,null,type,javaType,possibleTypes,parent);
	}
	
	//----------------------------------------------------------------------------------------------------
	public SubmodelInfo(final String name, final String description, final String type, final Class<?> javaType, final List<Class<?>> possibleTypes,
						final SubmodelInfo parent) {
		super(name,description,type,javaType);
		this.possibleTypes = possibleTypes;
		this.parent = parent;
	}
	
	//----------------------------------------------------------------------------------------------------
	public List<Class<?>> getPossibleTypes() { return possibleTypes; }
	public Class<?> getActualType() { return actualType; }
	public SubmodelInfo getParent() { return parent; }
	public Class<?> getParentValue() { return parentValue; }

	//----------------------------------------------------------------------------------------------------
	public void setPossibleTypes(final List<Class<?>> possibleTypes) { this.possibleTypes = possibleTypes; }
	public void setActualType(final Class<?> actualType) { this.actualType = actualType; }
	public void setParent(final SubmodelInfo parent) { this.parent = parent; }
	public void setParentValue(final Class<?> parentValue) { this.parentValue = parentValue; }
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public void setInitValue() {
		values.clear();
		super.setInitValue();
		if (values.isEmpty())
			values.add(null);
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public SubmodelInfo clone() {
		final List<Class<?>> possibleTypesClone = new ArrayList<Class<?>>(this.possibleTypes);
		final SubmodelInfo clone = new SubmodelInfo(this.name,this.description,this.type,this.actualType,possibleTypesClone,this.parent);
		clone.actualType = this.actualType;
		
		return clone;
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override public boolean isSubmodelParameter() { return parent != null; }
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public boolean equals(final	Object o) {
		if (o instanceof SubmodelInfo) {
			final SubmodelInfo that = (SubmodelInfo) o;
			
			if (this.parent == null)
				return that.parent == null && this.name.equals(that.name);
			
			return this.name.equals(that.name) && this.parent.equals(that.parent);
		}
		
		return false;
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public int hashCode() {
		int result = 17;
		result = 31 * result + name.hashCode();
		if (parent != null)
			result = 31 * result + parent.hashCode();
		
		return result;
		
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		String result = name + " : " + javaType.getSimpleName();
		if (actualType != null) 
			result += "[actual type=" + actualType.getSimpleName() + "]";
		
		return result;
	}
}