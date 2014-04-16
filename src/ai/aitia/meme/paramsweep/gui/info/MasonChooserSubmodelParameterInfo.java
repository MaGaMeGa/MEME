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

public class MasonChooserSubmodelParameterInfo extends MasonChooserParameterInfo implements ISubmodelGUIInfo {

	//====================================================================================================
	// members
	
	private static final long serialVersionUID = 6494379199798639435L;
	
	protected SubmodelInfo parent;
	protected Class<?> parentValue;
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public MasonChooserSubmodelParameterInfo(final String name, final String type, final Class<?> javaType, final List<Integer> validValues,
											 final List<String> validStringValues, final SubmodelInfo parent) {
		this(name,null,type,javaType,validValues,validStringValues,parent);
	}

	//----------------------------------------------------------------------------------------------------
	public MasonChooserSubmodelParameterInfo(final String name, final String description, final String type, final Class<?> javaType,
											 final List<Integer> validValues, final List<String> validStringValues, final SubmodelInfo parent) {
		super(name,description,type,javaType,validValues,validStringValues);
		this.parent = parent;
	}
	
	//----------------------------------------------------------------------------------------------------
	public SubmodelInfo getParent() { return parent; }
	
	//----------------------------------------------------------------------------------------------------
	public Class<?> getParentValue() { return parentValue; }
	public void setParentValue(final Class<?> parentValue) { this.parentValue = parentValue; }
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public MasonChooserSubmodelParameterInfo clone() {
		final List<Integer> validValuesClone = new ArrayList<Integer>(this.validValues);
		final List<String> validNamedValuesClone = new ArrayList<String>(this.validStringValues);
		final MasonChooserSubmodelParameterInfo clone = new MasonChooserSubmodelParameterInfo(this.name,this.description,this.javaType,validValuesClone,
																							  validNamedValuesClone,this.parent);
		
		return clone; 
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override public boolean isSubmodelParameter() { return true; }
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public boolean equals(final Object o) {
		if (o instanceof MasonChooserSubmodelParameterInfo) {
			final MasonChooserSubmodelParameterInfo that = (MasonChooserSubmodelParameterInfo) o;
			
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
}