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

import java.util.Iterator;
import java.util.NoSuchElementException;

import ai.aitia.meme.paramsweep.util.Primitive;

/** This class represents a parameter type that defines its values by using
 *  start, end and increment values. <code>T</code> is the type of the parameter.
 *  Valid types are the following:<br>
 *  - Byte<br>
 *  - Short<br>
 *  - Integer<br>
 *  - Long<br>
 *  - Float<br>
 *  - Double<br>
 */
final public class IncrementalParameterInfo<T extends Number> extends AbstractParameterInfo<T> {
	
	//====================================================================================================
	// members
	
	private static final long serialVersionUID = 3348443260272133255L;
	private T start, end, increment;
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public IncrementalParameterInfo(String name, String description, T defaultValue) {
		super(name,description,defaultValue);
		valueType = ValueType.INCREMENT;
		originalConstant = false;
	}
	
	//----------------------------------------------------------------------------------------------------
	public IncrementalParameterInfo(IncrementalParameterInfo<T> p) {
		super(p);
		this.valueType = ValueType.INCREMENT;
		this.start = p.start;
		this.end = p.end;
		this.increment = p.increment;
	}
	
	//----------------------------------------------------------------------------------------------------
	/** A method to set parameter values. */
	public void setValues(T start, T end, T increment) {
		if (start == null) 
			throw new IllegalArgumentException("Null start value.");
		if (end == null) 
			throw new IllegalArgumentException("Null end value.");
		if (increment == null) 
			throw new IllegalArgumentException("Null increment value.");
		
		double s = start.doubleValue();
		double e = end.doubleValue();
		double st = increment.doubleValue();
		if (Math.signum(e - s) != Math.signum(st)) 
			throw new IllegalArgumentException("Start and end value does not match the sign of increment value.");

		this.start = start;
		this.end = end;
		this.increment = increment;
	}
	
	//----------------------------------------------------------------------------------------------------
	public T getStart() { return start; }
	public T getEnd() { return end; }
	public T getIncrement() { return increment; }
	@Override public long getMultiplicity() { return calcStepCount(start,end,increment); }
	@Override public Iterator<T> iterator() { return new IncrementIterator(start,end,increment); }
    @Override public Iterator<ParameterInfo<T>> parameterIterator() { return new PiIncrementIterator(start,end,increment); }
	@Override public IncrementalParameterInfo<T> clone() { return new IncrementalParameterInfo<T>(this); }
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "name=" + name +
			   ";description=" + description + 
			   ";valueType=" + valueType +
			   ";start=" + start +
			   ";end=" + end +
			   ";inc=" + increment;
	}
    
    //====================================================================================================
	// assistant methods
    
    //----------------------------------------------------------------------------------------------------
	private long calcStepCount(T s, T e, T st) {
		long cnt;
		if (Math.abs(e.longValue()) < Math.abs(s.longValue())) 
			cnt = _calcStepCount(e,s,st);
		else 
			cnt = _calcStepCount(s,e,st);
		return cnt;
	}
    
	//----------------------------------------------------------------------------------------------------
	private long _calcStepCount(T start, T end, T step) {
		double s = start.doubleValue();
		double e = end.doubleValue();
		double st = step.doubleValue();
//		return (long) Math.ceil((Math.abs(e - s)) / Math.abs(st)) + 1;
		return (long) Math.floor((Math.abs(e - s)) / Math.abs(st)) + 1;
	}
	
	//====================================================================================================
	// nested classes
    
	//----------------------------------------------------------------------------------------------------
	final private class IncrementIterator implements Iterator<T> {
		
		//====================================================================================================
		// members
		
		private T start, end, increment;
		private long stepCount;
		private long currentLong = 0;
		private double currentDouble = 0;
		private long currentStepCount = 0;
		private boolean intType = false;
		private Primitive<T> primitive;
		private long actRun = run;
		
		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		public IncrementIterator(T start, T end, T increment) {
			this.start = start;
			this.end = end;
			this.increment = increment;
			stepCount = calcStepCount(this.start, this.end, this.increment);
//			intType = (this.start.doubleValue() == this.start.longValue());
			intType = !start.getClass().equals(Double.class) && !start.getClass().equals(Float.class);
			if (intType) 
				currentLong = this.start.longValue();
			else 
				currentDouble = this.start.doubleValue();
			primitive = new Primitive<T>(start);
		}
		
		//====================================================================================================
		// implemented interfaces
		
		//----------------------------------------------------------------------------------------------------
		public boolean hasNext() { return currentStepCount < stepCount || actRun < run;	}
		
		//----------------------------------------------------------------------------------------------------
		/** Uses some trick to provide values as T generic type. */
		public T next() {
			if (!hasNext()) 
				throw new NoSuchElementException();
			
			if (intType) {
				if (actRun == run) {
					currentLong = start.longValue() + currentStepCount++ * increment.longValue();
					actRun = 0;
				}
				++actRun;
				return primitive.getObjectWrapper(currentLong);
			} else {
				if (actRun == run) {
					currentDouble = start.doubleValue() + currentStepCount++ * increment.doubleValue();
					actRun = 0;
				}
				++actRun;
				return primitive.getObjectWrapper(currentDouble);
			}
		}

		//----------------------------------------------------------------------------------------------------
		public void remove() { throw new UnsupportedOperationException(); }
	}
	
	//----------------------------------------------------------------------------------------------------
	final private class PiIncrementIterator implements Iterator<ParameterInfo<T>> {
		
		//====================================================================================================
		// members
		
		private IncrementIterator it;
		
		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		public PiIncrementIterator(T start, T end, T increment) { it = new IncrementIterator(start,end,increment); }
		
		//====================================================================================================
		// implemented interfaces
		
		//----------------------------------------------------------------------------------------------------
		public boolean hasNext() { return it.hasNext(); }

		//----------------------------------------------------------------------------------------------------
		public ParameterInfo<T> next() {
			return new ParameterInfo<T>(IncrementalParameterInfo.this.name,IncrementalParameterInfo.this.description,it.next(),false);
		}

		//----------------------------------------------------------------------------------------------------
		public void remove() { throw new UnsupportedOperationException(); }
		
	}
}
