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
package ai.aitia.meme.paramsweep.platform.mason.info;

import java.util.ArrayList;
import java.util.List;

import ai.aitia.meme.paramsweep.batch.param.ParameterInfo;

public class MasonChooserParameterInfo<T> extends ParameterInfo<T> {
	
	private static final long serialVersionUID = 6996498449738216729L;
	List<T> possibleValues;
	List<String> possibleNamedValues;

	public MasonChooserParameterInfo(MasonChooserParameterInfo<T> p) {
		super(p);
		this.possibleValues = p.possibleValues;
		this.possibleNamedValues = p.possibleNamedValues;
	}
	
	public MasonChooserParameterInfo(String name, String description, T defaultValue, List<T> possibleValues, List<String> possibleNamedValues) {
		super(name, description, defaultValue);
		this.possibleValues = possibleValues;
		this.possibleNamedValues = possibleNamedValues;
	}
	
	public MasonChooserParameterInfo<T> clone() {
		MasonChooserParameterInfo<T> clone = new MasonChooserParameterInfo<T>(this);
		clone.values = cloneList(values);
		clone.possibleValues = cloneList(this.possibleValues);
		clone.possibleNamedValues = new ArrayList<String>(possibleNamedValues);
		return clone;
	}

	public List<T> getPossibleValues() {
		return possibleValues;
	}

	public List<String> getPossibleNamedValues() {
		return possibleNamedValues;
	}

}
