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
package ai.aitia.meme.paramsweep.batch;

import ai.aitia.meme.paramsweep.batch.output.RecordableInfo;

/** Information class that represent a result value. It extends
 *  <code>RecordableInfo<code> with a recorded value members.
 *  See <code>RecordableInfo</code> for details. */
@SuppressWarnings("serial")
public class ResultValueInfo extends RecordableInfo {
	
	//====================================================================================================
	// members
	
	protected Object value;
	
	/** Custom information. Unique within a run (time stamp, tick, etc.). */
	protected Object label;
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public ResultValueInfo(String name, Class type, String accessibleName, Object value, Object label) {
		super(name,type,name,accessibleName);
		this.value = value;
		this.label = label;
	}
	
	//----------------------------------------------------------------------------------------------------
	public ResultValueInfo(RecordableInfo info, Object value, Object label) {
		super(info.getName(),info.getType(),info.getAccessibleName());
		this.value = value;
		this.label = label;
	}
	
	//----------------------------------------------------------------------------------------------------
	public Object getValue() { return value; }
	public Object getLabel() { return label; }
}
