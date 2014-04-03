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
 * @param <T> is the value type of the class.
 */
public class GeneInfo implements Serializable {
	//=========================================================================
	//members
	
	private static final long serialVersionUID = 2001753160099291123L;
	/** Types of assignable values. */
	public static String LIST = "list";
	public static String INTERVAL = "interval";
	
	/** The actual gene value. */
	//protected ai.aitia.meme.paramsweep.gui.info.ParameterInfo info;
	protected String name;
	/** The minimum value of the gene. */
	protected Number minValue;
	/** The maximum value of the gene. */
	protected Number maxValue;
	/** The value range of the gene. */
	protected List<Object> valueRange;
	/** The gene value. */
	//protected Object value;
	/** The actual type: list or interval. */
	protected String valueType = null;
	/** The java type's name. */
	protected String type = null;
	/** The java type. */
	protected Class<?> javaType = null;
	/** Indicates that the gene takes integer values only. It's checked
	 * when the type is double or float. */
	protected boolean integerVals = false;

	/** Specifies whether the gene will be the part of the chromosome. */
	//protected boolean inChromosome = false;

	//=========================================================================
	//constructors
	public GeneInfo( /*ai.aitia.meme.paramsweep.gui.info.ParameterInfo info*/ String name, 
				 Number min, Number max, String type, Class<?> javaType ){
		//this.info = info;
		this.name = name;
		minValue = min;
		maxValue = max;
		valueType = INTERVAL;
		this.type = type;
		this.javaType = javaType;
	}

	public GeneInfo( /*ai.aitia.meme.paramsweep.gui.info.ParameterInfo info*/ String name, 
				 List<Object> values, String type, Class<?> javaType){
		//this.info = info;
		this.name = name;
		valueRange = values;
		valueType = LIST;
		this.type = type;
		this.javaType = javaType;
	}
	
	//=========================================================================
	//public functions
	/* public ai.aitia.meme.paramsweep.gui.info.ParameterInfo getInfo() {
		return info;
	}

	public void setInfo(ai.aitia.meme.paramsweep.gui.info.ParameterInfo info) {
		this.info = info;
	}*/
	
	public String getName(){
		return name;
	}
	
	public void setName( String name ){
		this.name = name;
	}

	public Number getMinValue() {
		return minValue;
	}

	public void setMinValue(Number minValue) {
		this.minValue = minValue;
	}

	public Number getMaxValue() {
		return maxValue;
	}

	public void setMaxValue(Number maxValue) {
		this.maxValue = maxValue;
	}

	public List<Object> getValueRange() {
		return valueRange;
	}

	public void setValueRange(List<Object> valueRange) {
		this.valueRange = valueRange;
	}
	
	public String getValueType() {
		return valueType;
	}

	public void setValueType(String type) {
		this.valueType = type;
	}
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	public Class<?> getJavaType() {
		return javaType;
	}

	public void setJavaType(Class<?> type) {
		this.javaType = type;
	}
	
	public boolean isIntegerVals() {
		return integerVals;
	}

	public void setIntegerVals(boolean integerVals) {
		this.integerVals = integerVals;
	}
	
	/*public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}*/

	
	public Object getRandomValue(){
		//TODO: implement
		if( valueType == LIST ){
			
		}else{
			
		}
		
		return null;
	}
	
	@Override
	public String toString(){
		//String str = info.getName() + " - " + info.getType();
		String str = name + " - " + type;
		
		if( minValue != null || maxValue != null || valueRange != null ){
			str += " - [";
		
			if( valueType == INTERVAL ){
				str += "type: ";
				if( ("double".equalsIgnoreCase( type ) || "float".equalsIgnoreCase( type ) ) &&
						integerVals )
					str += "integer ";
				str += "interval [" + (minValue == null ? "?" : minValue) + ";";
				str += (maxValue == null ? "?" : maxValue) + "]";
			}else if ( valueType == LIST ){
				str += "type: list";
			}
			
			str += "]";
		}
		
		return str;
	}
	
	public GeneInfo cloneGeneInfo(){
		GeneInfo clone = valueType.equals( GeneInfo.INTERVAL ) ?
							new GeneInfo( new String( name ), new Double( minValue.doubleValue() ),
									      new Double( maxValue.doubleValue() ), new String( type ), javaType ) :
							new GeneInfo( new String( name ), null, new String( type ), javaType );
		if( clone.getValueType().equals( GeneInfo.LIST ) ){
			clone.setValueRange( new ArrayList<Object>( valueRange ) );
		}
		clone.setIntegerVals( integerVals );
		return clone;
	}
}
