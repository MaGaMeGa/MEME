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

import java.io.Serializable;
import java.util.Iterator;

/** This class defines basic properties and operations of a parameter. 
 *  <code>T</code> is the type of the parameter. Valid types are the following:<br>
 *  - Byte<br>
 *  - Short<br>
 *  - Integer<br>
 *  - Long<br>
 *  - Float<br>
 *  - Double<br>
 *  - Boolean<br>
 *  - String<br>
 */
public abstract class AbstractParameterInfo<T> implements Serializable,
														  Iterable<T>,
														  Comparable<AbstractParameterInfo<T>>,
														  Cloneable {
	
	//====================================================================================================
	// members
	
	private static final long serialVersionUID = -3057966448289276565L;
	final String name;
	final String description;
	final T defaultValue;
	
	/** The values of a parameter can be specified in three ways: as a constant,
	 *  as a list of values or as an iteration from a start value till an end value with
	 *  a specific step value. This enum represents the three ways. 
	 */ 
	public enum ValueType {
		CONSTANT,
		LIST,
		INCREMENT
	};
		
	ValueType valueType;
	
	/** When a batch procedure is to be partitioned into single runs, all the parameters
	 *  will finally look like constants, despite their original type. This method helps 
	 *  telling the difference. Be aware that only parameterIterator() method can
	 *  provide parameters that preserve this property.
	 */
	boolean originalConstant;
	
	/** Number of runs that uses the same value of the parameter. */
	long run = 1;
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	protected AbstractParameterInfo(String name, String description, T defaultValue) {
		if (name == null || name.trim().equals("")) 
			throw new IllegalArgumentException("Name is null or empty.");
		this.name = name;
		this.description = description;
		this.defaultValue = defaultValue;
		this.originalConstant = false;
	}
	
	//----------------------------------------------------------------------------------------------------
	protected AbstractParameterInfo(AbstractParameterInfo<T> p) {
		this.name = p.name;
		this.description = p.description;
		this.defaultValue = p.defaultValue;
		this.valueType = p.valueType;
		this.originalConstant = p.originalConstant;
		this.run = p.run;
	}
	
	//----------------------------------------------------------------------------------------------------
	public String getName() { return name; }
	public String getDescription() { return description; }
	public T getDefaultValue() { return defaultValue; }
	public ValueType getValueType() { return valueType; }
	
	//----------------------------------------------------------------------------------------------------
	/** When a batch procedure is to be partitioned into single runs, all the parameters
	 *  will finally look like constants, despite their original type. This method helps 
	 *  telling the difference. Be aware that only parameterIterator() method can
	 *  provide parameters that preserve this property.
	 */
	public boolean isOriginalConstant() { return originalConstant; }
	
	//----------------------------------------------------------------------------------------------------
	public boolean isSingleValued() { return valueType == ValueType.CONSTANT; }
	public long getRunNumber() { return run; }
	public void setRunNumber(long run) { this.run = run; }
	
	//----------------------------------------------------------------------------------------------------
	/** Returns the number of values. */
	public abstract long getMultiplicity();
	
	//----------------------------------------------------------------------------------------------------
	/** A convenience iterator that returns values of this parameter 
     *  converted to constant parameter instances. Be aware that only this
     *  method can provide parameters that preserve the original type 
     *  property.
     */
	public abstract Iterator<ParameterInfo<T>> parameterIterator();
	
	//----------------------------------------------------------------------------------------------------
	/** Based on name equality. */
	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof AbstractParameterInfo)) 
			return false;
		return name.equals(((AbstractParameterInfo)o).name);
	}
	
	//----------------------------------------------------------------------------------------------------
	/** Based on name hash code. */
	@Override
	public int hashCode() { return name.hashCode(); }
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return 
			"name=" + name +
			";description=" + description + 
			";defaultValue=" + defaultValue +
			";valueType=" + valueType;
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override public abstract Object clone();
	
	//====================================================================================================
	// implemented interfaces
	
	//----------------------------------------------------------------------------------------------------
	public int compareTo(AbstractParameterInfo<T> other) { return this.name.compareTo(other.name); }
	
	//----------------------------------------------------------------------------------------------------
	/** The values of parameter. */
	public abstract Iterator<T> iterator();
}
