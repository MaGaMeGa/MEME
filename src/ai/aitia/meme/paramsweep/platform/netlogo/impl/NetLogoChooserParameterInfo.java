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
package ai.aitia.meme.paramsweep.platform.netlogo.impl;

import java.util.List;

import ai.aitia.meme.paramsweep.batch.param.ParameterInfo;

public class NetLogoChooserParameterInfo<T> extends ParameterInfo<T> {

	//====================================================================================================
	// members
	
	private static final long serialVersionUID = -8302501622874024322L;
	List<T> possibleValues = null;

	//====================================================================================================
	// methods

	//----------------------------------------------------------------------------------------------------
	public NetLogoChooserParameterInfo(String name, String description, T defaultValue, List<T> possibleValues) {
		super(name,description,defaultValue);
		this.possibleValues = possibleValues;
	}
	
	//----------------------------------------------------------------------------------------------------
	public NetLogoChooserParameterInfo(NetLogoChooserParameterInfo<T> p) {
		super(p);
		this.possibleValues = p.possibleValues;
	}
	
	//----------------------------------------------------------------------------------------------------
	public List<T> getPossibleValues() { return possibleValues; }
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public NetLogoChooserParameterInfo<T> clone() {
		NetLogoChooserParameterInfo<T> clone = new NetLogoChooserParameterInfo<T>(this);
		clone.values = cloneList(this.values);
		clone.possibleValues = cloneList(this.possibleValues);
		return clone;
	}
}
