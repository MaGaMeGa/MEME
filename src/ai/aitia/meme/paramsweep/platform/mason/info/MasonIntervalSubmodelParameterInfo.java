/*******************************************************************************
 * Copyright (C) 2006-2014 AITIA International, Inc.
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

import ai.aitia.meme.paramsweep.batch.param.ISubmodelParameterInfo;
import ai.aitia.meme.paramsweep.batch.param.SubmodelInfo;

public class MasonIntervalSubmodelParameterInfo<T> extends MasonIntervalParameterInfo<T> implements ISubmodelParameterInfo {
	
	//====================================================================================================
	// members
	
	private static final long serialVersionUID = -5765896717297439205L;
	
	protected SubmodelInfo<?> parent;
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public MasonIntervalSubmodelParameterInfo(final String name, final String description, final T defaultValue, final Number min, final Number max,
											  final boolean isDouble, final SubmodelInfo<?> parent) {
		super(name,description,defaultValue,min,max,isDouble);
		this.parent = parent;
	}

	//----------------------------------------------------------------------------------------------------
	public SubmodelInfo<?> getParentInfo() { return parent; }
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public MasonIntervalSubmodelParameterInfo<T> clone() {
		return new MasonIntervalSubmodelParameterInfo<T>(this.getName(),this.getDescription(),this.getDefaultValue(),this.min,this.max,this.isDouble,this.parent);
	}
}