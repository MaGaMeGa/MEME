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

import java.util.Iterator;

public class SubmodelIncrementalParameterInfo<T extends Number> extends IncrementalParameterInfo<T> implements ISubmodelParameterInfo {
	
	//====================================================================================================
	// members
	
	private static final long serialVersionUID = -264054169911149696L;
	
	protected SubmodelInfo<?> parent;
	
	//====================================================================================================
	// methods

	//----------------------------------------------------------------------------------------------------
	public SubmodelIncrementalParameterInfo(String name, String description, T defaultValue, final SubmodelInfo<?> parent) {
		super(name,description,defaultValue);
		this.parent = parent;
	}
	
	//----------------------------------------------------------------------------------------------------
	public SubmodelIncrementalParameterInfo(IncrementalParameterInfo<T> p, final SubmodelInfo<?> parent) {
		super(p);
		this.parent = parent;
	}

	//----------------------------------------------------------------------------------------------------
	public SubmodelInfo<?> getParentInfo() { return parent; }
	
    //----------------------------------------------------------------------------------------------------
	@Override public Iterator<ParameterInfo<T>> parameterIterator() { return new SmPiIncrementIterator(getStart(),getEnd(),getIncrement()); }
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public SubmodelIncrementalParameterInfo<T> clone() {
		final SubmodelIncrementalParameterInfo<T> clone = new SubmodelIncrementalParameterInfo<T>(this.name,this.description,this.defaultValue,this.parent);
		
		return clone;
	}
	
	//====================================================================================================
	// nested classes
	
	//----------------------------------------------------------------------------------------------------
	class SmPiIncrementIterator extends PiIncrementIterator {
		
		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		public SmPiIncrementIterator(T start, T end, T increment) { super(start,end,increment); }
		
		//----------------------------------------------------------------------------------------------------
		@Override
		public ParameterInfo<T> next() {
			return new SubmodelParameterInfo<T>(name,description,it.next(),false,parent);
		}
	}
}