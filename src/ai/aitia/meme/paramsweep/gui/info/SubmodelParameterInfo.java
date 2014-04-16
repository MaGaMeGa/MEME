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

public class SubmodelParameterInfo extends ParameterInfo implements ISubmodelGUIInfo {

	//====================================================================================================
	// members
	
	private static final long serialVersionUID = 6991443589897247037L;
	
	protected SubmodelInfo parent;
	protected Class<?> parentValue;

	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public SubmodelParameterInfo(final String name, final String type, final Class<?> javaType, final SubmodelInfo parent) {
		this(name,null,type,javaType,parent);
	}
	
	//----------------------------------------------------------------------------------------------------
	public SubmodelParameterInfo(final String name, final String description, final String type, final Class<?> javaType, final SubmodelInfo parent) {
		super(name,description,type,javaType);
		this.parent = parent;
	}

	//----------------------------------------------------------------------------------------------------
	public SubmodelInfo getParent() { return parent; }
	public void setParent(final SubmodelInfo parent) { this.parent = parent; }
	
	//----------------------------------------------------------------------------------------------------
	public Class<?> getParentValue() { return parentValue; }
	public void setParentValue(final Class<?> parentValue) { this.parentValue = parentValue; }

	//----------------------------------------------------------------------------------------------------
	@Override
	public SubmodelParameterInfo clone() {
		final SubmodelParameterInfo clone = new SubmodelParameterInfo(this.name,this.description,this.type,this.javaType,this.parent);
		
		return clone;
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override public boolean isSubmodelParameter() { return true; }
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public boolean equals(final Object o) {
		if (o instanceof SubmodelParameterInfo) {
			final SubmodelParameterInfo that = (SubmodelParameterInfo) o;
			
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
		if (parent != null)
			result = 31 * result + name.hashCode();
		
		result = 31 * result + parent.hashCode();
		
		return result;
		
	}
}