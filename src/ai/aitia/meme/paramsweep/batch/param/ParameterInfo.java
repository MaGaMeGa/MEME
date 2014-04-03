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
package ai.aitia.meme.paramsweep.batch.param;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** This class comprises two types of parameters regarding their values they
 *  represent: constant and list.
 */
public class ParameterInfo<T> extends AbstractParameterInfo<T> {

	//====================================================================================================
	// members
	
	private static final long serialVersionUID = -5707926781004922902L;
	protected List<T> values;

	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	/** Creates an instance of type constant. */
	public ParameterInfo(String name, String description, T defaultValue) {
		super(name,description,defaultValue);
		setValue(defaultValue);
	}
	
	//----------------------------------------------------------------------------------------------------
	ParameterInfo(String name, String description, T defaultValue, boolean originalConstant) {
		this(name,description,defaultValue);
		this.originalConstant = originalConstant;
	}
	
	//----------------------------------------------------------------------------------------------------
	public ParameterInfo(ParameterInfo<T> p) {
		super(p);
		values = p.values;
	}
	
	//----------------------------------------------------------------------------------------------------
	/** For CONSTANT type use this method to set parameter value. */
	public void setValue(T o) {
		if (o == null) 
			throw new IllegalArgumentException("Value is null. (" + name + ")");
		valueType = ValueType.CONSTANT;
		values = new ArrayList<T>();
		values.add(o);
		originalConstant = true;
	}
	
	//----------------------------------------------------------------------------------------------------
	/** For LIST type use this method to set parameter values. */
	public void setValues(List<T> values) {
		if (values == null) 
			throw new IllegalArgumentException("Value list is null.");
		if (values.size() == 0) 
			throw new IllegalArgumentException("Empty value list.");
		valueType = ValueType.LIST;
		this.values = values;
		originalConstant = false;
	}
	
	//----------------------------------------------------------------------------------------------------
	public void setValues(T... values) {
		if (values == null) 
			throw new IllegalArgumentException("Values are null.");
		if (values.length == 0) 
			throw new IllegalArgumentException("Empty values.");
		valueType = ValueType.LIST;
		List<T> l = new ArrayList<T>();
		for (T v : values) 
			l.add(v);
		this.values = l;
		originalConstant = false;
	}
	
	//----------------------------------------------------------------------------------------------------
	/** Returns a copy of the internal list: changing the list has no 
	 *  effect to the original. For CONSTANT the list contains that single 
	 *  value. The returned list may differ from the result of a full 
	 *  iteration created by an iterator in case of <code>runs</code> 
	 *  {@literal >} 1.
	 *  The returned list doesn't contain the multiplications defined by
	 *  the <code>runs</code> member.
	 */
	public List<T> getValues() {
		List<T> l = new ArrayList<T>();
		l.addAll(values);
		return l;
	}
	

	//----------------------------------------------------------------------------------------------------
	/** A utility method that returns a single valued parameter created
	 *  from the value at index in the list. If no such index in the list
	 *  it uses the last one.
	 */
	public ParameterInfo<T> getSingleValuedParameterAt(int index) {
		ParameterInfo<T> p = new ParameterInfo<T>(this);
		if (values.size() < index + 1) 
			index = values.size() - 1;	// last
		
		p.setValue(values.get(index));
		return p;
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public long getMultiplicity() { return values.size(); }
	
    //----------------------------------------------------------------------------------------------------
	@Override
	public Iterator<T> iterator() {
    	if (valueType == ValueType.CONSTANT) 
    		return new ConstantIterator();
   		return new MyListIterator();
    }
    
    //----------------------------------------------------------------------------------------------------
	@Override
	public Iterator<ParameterInfo<T>> parameterIterator() {
    	if (valueType == ValueType.CONSTANT) 
    		return new PiConstantIterator();
   		return new PiMyListIterator();
    }
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public ParameterInfo<T> clone() {
		ParameterInfo<T> clone = new ParameterInfo<T>(this);
		clone.values = cloneList(this.values);
		return clone;
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override public String toString() {
		return "name=" + name +
			   ";description=" + description + 
			   ";valueType=" + valueType +
			   ";value" + (values.size() == 1 ? "" : "s") + "=" + values;
	}
	
	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	protected List<T> cloneList(List<T> original) {
		List<T> result = new ArrayList<T>(original.size());
		for (T t : original) 
			result.add(t);
		return result;
	}
	
	
	//====================================================================================================
	// nested classes
	
	//----------------------------------------------------------------------------------------------------
	class ConstantIterator implements Iterator<T> {
		
		//====================================================================================================
		// members
		
		//----------------------------------------------------------------------------------------------------
		private boolean finished = false; 
		
		//====================================================================================================
		// implemented interfaces
		
		//----------------------------------------------------------------------------------------------------
		public boolean hasNext() { return !finished; }

		//----------------------------------------------------------------------------------------------------
		public T next() {
			finished = true;
			return values.get(0);
		}

		//----------------------------------------------------------------------------------------------------
		public void remove() { throw new UnsupportedOperationException(); }
	}
	
	//----------------------------------------------------------------------------------------------------
	final private class MyListIterator implements Iterator<T> {
		
		//====================================================================================================
		// members
		
		private Iterator<T> valueIt;
		private long actRun = run;
		private T actValue;
		
		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		public MyListIterator() { valueIt = ParameterInfo.this.values.iterator(); }
		
		//====================================================================================================
		// implemented interfaces
		
		//----------------------------------------------------------------------------------------------------
		public boolean hasNext() { return valueIt.hasNext() || actRun < run; }

		//----------------------------------------------------------------------------------------------------
		public T next() {
			if (actRun == run) {
				actRun = 0;
				actValue = valueIt.next();
			}
			++actRun;
			return actValue;
		}

		//----------------------------------------------------------------------------------------------------
		public void remove() { throw new UnsupportedOperationException(); }
	}
	
	//----------------------------------------------------------------------------------------------------
	final private class PiConstantIterator implements Iterator<ParameterInfo<T>> {
		
		//====================================================================================================
		// members
		
		private boolean finished = false; 
		
		//====================================================================================================
		// implemented interfaces
		
		//----------------------------------------------------------------------------------------------------
		public boolean hasNext() { return !finished; }

		//----------------------------------------------------------------------------------------------------
		public ParameterInfo<T> next() {
			finished = true;
			return ParameterInfo.this;
		}

		//----------------------------------------------------------------------------------------------------
		public void remove() { throw new UnsupportedOperationException(); }
	}
	
	//----------------------------------------------------------------------------------------------------
	class PiMyListIterator implements Iterator<ParameterInfo<T>> {
		
		//====================================================================================================
		// members
		
		private Iterator<T> valueIt;
		
		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		public PiMyListIterator() { valueIt = ParameterInfo.this.iterator(); }
		
		//====================================================================================================
		// implemented interfaces
		
		//----------------------------------------------------------------------------------------------------
		public boolean hasNext() { return valueIt.hasNext(); }

		//----------------------------------------------------------------------------------------------------
		public ParameterInfo<T> next() {
			return new ParameterInfo<T>(ParameterInfo.this.name,ParameterInfo.this.description,valueIt.next(),false);
		}

		//----------------------------------------------------------------------------------------------------
		public void remove() { throw new UnsupportedOperationException(); }
	}
}
