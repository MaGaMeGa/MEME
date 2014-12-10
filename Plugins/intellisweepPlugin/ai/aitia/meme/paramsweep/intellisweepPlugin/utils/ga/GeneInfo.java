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
package ai.aitia.meme.paramsweep.intellisweepPlugin.utils.ga;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a gene of a solution. (A quasi-union type: a Gene either has an
 * interval or a list of assignable values.)
 * 
 * @author Attila Szabo
 * 
 * @param <T>
 *            is the value type of the class.
 */
public class GeneInfo implements Serializable {
	
	//=========================================================================
	// members

	private static final long serialVersionUID = 2001753160099291123L;
	/** Types of assignable values. */
	public static final String LIST = "list";
	public static final String INTERVAL = "interval";
	public static final String BOOLEAN = "boolean";

	protected String name;
	/** The minimum value of the gene. */
	protected Number minValue;
	/** The maximum value of the gene. */
	protected Number maxValue;
	/** The value range of the gene. */
	protected List<Object> valueRange;
	/** The gene value. */
	// protected Object value;
	/** The actual type: list or interval. */
	protected String valueType = null;
	/** The java type's name. */
	protected String type = null;
	/** The java type. */
	protected Class<?> javaType = null;
	/**
	 * Indicates that the gene takes integer values only. It's checked when the
	 * type is double or float.
	 */
	protected boolean integerVals = false;

	//=========================================================================
	// constructors

	//----------------------------------------------------------------------------------------------------
	public GeneInfo(final String name, final String type, final Class<?> javaType) {
		this.name = name;
		this.type = type;
		this.javaType = javaType;
		valueType = BOOLEAN;
	}

	//----------------------------------------------------------------------------------------------------
	public GeneInfo(final String name, final Number min, final Number max, final String type, final Class<?> javaType) {
		this.name = name;
		minValue = min;
		maxValue = max;
		valueType = INTERVAL;
		this.type = type;
		this.javaType = javaType;
	}

	//----------------------------------------------------------------------------------------------------
	public GeneInfo(final String name, final List<Object> values, final String type, final Class<?> javaType) {
		this.name = name;
		valueRange = values;
		valueType = LIST;
		this.type = type;
		this.javaType = javaType;
	}

	//=========================================================================
	// public functions

	//----------------------------------------------------------------------------------------------------
	public String getName() { return name; }
	public Number getMinValue() { return minValue; }
	public Number getMaxValue() { return maxValue; }
	public List<Object> getValueRange() { return valueRange; }
	public String getValueType() { return valueType; }
	public String getType() { return type; }
	public Class<?> getJavaType() { return javaType; }
	public boolean isIntegerVals() { return integerVals; }

	//----------------------------------------------------------------------------------------------------
	public void setName(final String name) { this.name = name; }
	public void setMinValue(final Number minValue) { this.minValue = minValue; }
	public void setMaxValue(final Number maxValue) { this.maxValue = maxValue; }
	public void setValueRange(final List<Object> valueRange) { this.valueRange = valueRange; }
	public void setValueType(final String type) { this.valueType = type; }
	public void setType(final String type) { this.type = type; }
	public void setJavaType(final Class<?> type) { this.javaType = type; }
	public void setIntegerVals(final boolean integerVals) { this.integerVals = integerVals; }

	//----------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		String str = name + " - " + type;

		if (minValue != null || maxValue != null || valueRange != null) {
			str += " - [";

			if (valueType == INTERVAL) {
				str += "type: ";
				if (("double".equalsIgnoreCase(type) || "float".equalsIgnoreCase(type))
						&& integerVals)
					str += "integer ";
				str += "interval [" + (minValue == null ? "?" : minValue) + ";";
				str += (maxValue == null ? "?" : maxValue) + "]";
			} else if (valueType == LIST) {
				str += "type: list";
			}

			str += "]";
		}

		return str;
	}

	//----------------------------------------------------------------------------------------------------
	public GeneInfo cloneGeneInfo() {
		GeneInfo clone = null;
		switch (valueType) {
		case BOOLEAN : clone = new GeneInfo(name, type, javaType);
					   break;
		case INTERVAL : clone = (minValue instanceof Double) ? new GeneInfo(name, new Double(minValue.doubleValue()), new Double(maxValue.doubleValue()), type, javaType) :
															   new GeneInfo(name, new Long(minValue.longValue()), new Long(maxValue.longValue()), type, javaType);
						break;
		case LIST : clone = new GeneInfo(name, null, type, javaType);
					clone.setValueRange(new ArrayList<Object>(valueRange));
		}
		if (clone != null) {
			clone.setIntegerVals(integerVals);
		}
		
		return clone;
	}
}
