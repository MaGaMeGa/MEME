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


import ai.aitia.meme.paramsweep.batch.param.ParameterInfo;

public class MasonIntervalParameterInfo<T> extends ParameterInfo<T> {
	
	private static final long serialVersionUID = 3391938726700707975L;
	Number min;
	Number max;
	boolean isDouble;

	public MasonIntervalParameterInfo(String name, String description, T defaultValue, Number min, Number max, boolean isDouble) {
		super(name, description, defaultValue);
		this.min = min;
		this.max = max;
		this.isDouble = isDouble;
	}

	public MasonIntervalParameterInfo(MasonIntervalParameterInfo<T> p) {
		super(p);
		
		this.min = p.min;
		this.max = p.max;
		this.isDouble = p.isDouble;
	}
	
	
	public MasonIntervalParameterInfo<T> clone() {
		MasonIntervalParameterInfo<T> clone = new MasonIntervalParameterInfo<T>(this);
		clone.values = cloneList(this.values);
		clone.min = this.min;
		clone.max = this.max;
		clone.isDouble = this.isDouble;
		return clone;
	}
	
	public Number getIntervalMin() {return min;}
	public Number getIntervalMax() {return max;}
	public boolean isIntervalDouble() {return isDouble;}
}
