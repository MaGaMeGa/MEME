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
package ai.aitia.meme.paramsweep.gui.info;

import java.util.ArrayList;
import java.util.List;

public class ChooserParameterInfo extends ParameterInfo {

	//====================================================================================================
	// members
	
	private static final long serialVersionUID = -7186565331682912505L;
	protected List<Object> validValues = null;  

	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public ChooserParameterInfo(String name, String type, Class<?> javaType, List<Object> validValues) {
		super(name,type,javaType);
		this.validValues = validValues;
	}
	
	//----------------------------------------------------------------------------------------------------
	public List<Object> getValidValues() { return validValues; }
	@Override public ChooserParameterInfo clone() { return new ChooserParameterInfo(this); }
	
	//----------------------------------------------------------------------------------------------------
	public boolean isValidValue(String value) {
		Object v = getValue(value,type);
		return validValues.contains(v);
	}
	
	//----------------------------------------------------------------------------------------------------
	public boolean isValidValues(String values) {
		String[] vals = values.trim().split(" ");
		for (String value : vals) {
			Object v = getValue(value,type);
			if (!validValues.contains(v)) return false;
		}
		return true;
	}
	
	//----------------------------------------------------------------------------------------------------
	public boolean isValidValues(String start, String end, String step) {
		double s = Double.parseDouble(start);
		double e = Double.parseDouble(end);
		double st = Double.parseDouble(step);
		
		for (double d = s;d <= e;d += st) {
			if (!validValues.contains(d)) return false;
		}
		return true;
	}
	
	//----------------------------------------------------------------------------------------------------
	public String validValuesToString() {
		StringBuilder result = new StringBuilder();
		for (Object o : validValues)
			   result.append(" ").append(toStringWithoutFuckingScientificNotation(o,type));
		return result.toString().substring(1);
	}

	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	protected ChooserParameterInfo(ChooserParameterInfo p) {
		super(p);
		validValues = new ArrayList<Object>(p.validValues);
	}
}
