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

import _.unknown;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings;
import ai.aitia.meme.paramsweep.internal.platform.IGUIController.RunOption;

public class MasonChooserParameterInfo extends ParameterInfo {
	
	private static final long serialVersionUID = 2402682107091329763L;
	protected List<Integer> validValues = null;
	protected List<String> validStringValues = null;

	//----------------------------------------------------------------------------------------------------
	protected MasonChooserParameterInfo(MasonChooserParameterInfo p) {
		super(p);
		this.validValues = new ArrayList<Integer>(p.validValues);
		this.validStringValues = new ArrayList<String>(p.validStringValues);
	}

	//----------------------------------------------------------------------------------------------------
	public MasonChooserParameterInfo(String name, String description, String type, Class<?> javaType, List<Integer> validValues, List<String> validStringValues) {
		super(name, description, type, javaType);
		this.validValues = validValues;
		this.validStringValues = validStringValues;
	}

	//----------------------------------------------------------------------------------------------------
	public MasonChooserParameterInfo(String name, String type, Class<?> javaType, List<Integer> validValues, List<String> validStringValues) {
		super(name, type, javaType);
		this.validValues = validValues;
		this.validStringValues = validStringValues;
	}

	//----------------------------------------------------------------------------------------------------
	public boolean isValidValue(String value) {
		Object v = getValue(value,type);
		return validValues.contains(v);
	}
	
	//----------------------------------------------------------------------------------------------------
	public boolean isValidValues(String[] values) {
		for (int i = 0; i < values.length; i++) {
			Object v = getValue(values[i],type);
			if (!validValues.contains(v)) return false;
		}
		return true;
	}
	
	//----------------------------------------------------------------------------------------------------
	public boolean isValidValues(String start, String end, String step) {
		int s = Integer.parseInt(start);
		int e = Integer.parseInt(end);
		int st = Integer.parseInt(step);
		
		for (double d = s;d <= e;d += st) {
			if (!validValues.contains(d)) return false;
		}
		return true;
	}
	
	@Override
	public MasonChooserParameterInfo clone() {
		return new MasonChooserParameterInfo(this);
	}

	public String getValidValuesString() {
		StringBuilder ret = new StringBuilder(validValues.toString());
		ret.append(" (");
		for (int i = 0; i < validValues.size(); i++) {
			ret.append(validValues.get(i));
			ret.append("=");
			ret.append(validStringValues.get(i));
			if (i < validValues.size()-1) ret.append(", ");
		}
		ret.append(")");
		return ret.toString();
	}
	
	public String[] getValidStrings(){
		return validStringValues.toArray(new String[validStringValues.size()]);
	}
	
	//-------------------------------------------------------------------------------
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(name);
		sb.append(" : ").append(type.equals("List") || javaType.equals(unknown.class) ? javaType.getSimpleName() : type);
		sb.append(" - [");
		if (PlatformSettings.getGUIControllerForPlatform().getRunOption() == RunOption.LOCAL && runs > 1) {
			sb.append("runs=");
			sb.append(runs);
			sb.append(",");
		}
		switch (defType) {
		case CONST_DEF : sb.append("value=");
						 sb.append(validStringValues.get((Integer)getValue()));
						 break;
		case LIST_DEF  : 
		case INCR_DEF  : 
			throw new UnsupportedOperationException();
		}
		sb.append("]");
		return sb.toString();
	}
}