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
package ai.aitia.meme.paramsweep.platform.mason.info;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ai.aitia.meme.paramsweep.batch.param.ISubmodelParameterInfo;
import ai.aitia.meme.paramsweep.batch.param.ParameterInfo;
import ai.aitia.meme.paramsweep.batch.param.SubmodelInfo;

public class MasonChooserSubmodelParameterInfo<T> extends MasonChooserParameterInfo<T> implements ISubmodelParameterInfo {
	
	//====================================================================================================
	// members

	private static final long serialVersionUID = -2727591278171724847L;
	
	protected SubmodelInfo<?> parent;
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public MasonChooserSubmodelParameterInfo(final String name, final String description, final T defaultValue, final List<T> possibleValues,
											 final List<String> possibleNamedValues, final SubmodelInfo<?> parent) {
		super(name,description,defaultValue,possibleValues,possibleNamedValues);
		this.parent = parent;
	}
	
	//----------------------------------------------------------------------------------------------------
	public SubmodelInfo<?> getParentInfo() { return parent; }
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public MasonChooserSubmodelParameterInfo<T> clone() {
		final List<T> possibleValuesClone = new ArrayList<T>(this.possibleValues);
		final List<String> possibleNamedValuesClone = new ArrayList<String>(this.possibleNamedValues);
		final MasonChooserSubmodelParameterInfo<T> clone = new MasonChooserSubmodelParameterInfo<T>(this.getName(),this.getDescription(),this.getDefaultValue(),
																									possibleValuesClone,possibleNamedValuesClone,this.parent);
		
		return clone; 
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public Iterator<ParameterInfo<T>> parameterIterator() {
    	if (getValueType() == ValueType.CONSTANT) 
    		return new PiConstantIterator();
   		return new McSmPiListIterator();
    }
	
	//====================================================================================================
	// nested classes
	
	//----------------------------------------------------------------------------------------------------
	class McSmPiListIterator extends PiMyListIterator {
		
		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		public McSmPiListIterator() { valueIt = MasonChooserSubmodelParameterInfo.this.iterator(); }
		
		//----------------------------------------------------------------------------------------------------
		@Override
		public ParameterInfo<T> next() {
			return new MasonChooserSubmodelParameterInfo<T>(getName(),getDescription(),valueIt.next(),possibleValues,possibleNamedValues,parent);
		}
	}
}