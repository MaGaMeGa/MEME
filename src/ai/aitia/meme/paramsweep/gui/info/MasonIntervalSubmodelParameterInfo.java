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

public class MasonIntervalSubmodelParameterInfo extends MasonIntervalParameterInfo implements ISubmodelGUIInfo {

	//====================================================================================================
	// members
	
	private static final long serialVersionUID = 6870852296005661875L;
	
	protected SubmodelInfo parent;
	protected Class<?> parentValue;
	
	//====================================================================================================
	// methods

	//----------------------------------------------------------------------------------------------------
	public MasonIntervalSubmodelParameterInfo(final String name, final String type, final Class<?> javaType, final Number min, final Number max, 
											  final boolean isDoubleInterval, final SubmodelInfo parent) {
		this(name,null,type,javaType,min,max,isDoubleInterval,parent);
	}

	//----------------------------------------------------------------------------------------------------
	public MasonIntervalSubmodelParameterInfo(final String name, final String description, final String type, final Class<?> javaType, final Number min,
											  final Number max, final boolean isDoubleInterval, final SubmodelInfo parent) {
		super(name,description,type,javaType,min,max,isDoubleInterval);
		this.parent = parent;
	}

	//----------------------------------------------------------------------------------------------------
	public SubmodelInfo getParent() { return parent; }
	
	//----------------------------------------------------------------------------------------------------
	public Class<?> getParentValue() { return parentValue; }
	public void setParentValue(final Class<?> parentValue) { this.parentValue = parentValue; }
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public MasonIntervalSubmodelParameterInfo clone() {
		return new MasonIntervalSubmodelParameterInfo(this.name,this.description,this.type,this.javaType,this.intervalMin,this.intervalMax,this.isDoubleInterval,
													  this.parent); 
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override public boolean isSubmodelParameter() { return true; }
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public boolean equals(final Object o) {
		if (o instanceof MasonIntervalSubmodelParameterInfo) {
			final MasonIntervalSubmodelParameterInfo that = (MasonIntervalSubmodelParameterInfo) o;
			
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