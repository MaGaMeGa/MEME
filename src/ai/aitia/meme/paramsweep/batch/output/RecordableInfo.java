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
package ai.aitia.meme.paramsweep.batch.output;

import java.io.Serializable;

/** Class representing recordable entities of the model. */
public class RecordableInfo implements Serializable {
	
	//====================================================================================================
	// members
	
	private static final long serialVersionUID = -3649458533117479001L;
	
	/** Display name (on the GUI and in the result file too). */
	protected String name;
	
	protected Class type;
	protected String description;
	
	/** Name that can be use in source code. */
	protected String accessibleName;
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public RecordableInfo(String name, Class type, String description, String accessibleName) {
		if (name == null || name.trim().equals("")) 
			throw new IllegalArgumentException("Name is empty or null.");
		if (type == null) 
			throw new IllegalArgumentException("Class type is null.");
		if (accessibleName == null || accessibleName.trim().equals("")) 
			throw new IllegalArgumentException("Accessible name is empty or null.");
		this.name = name;
		this.type = type;
		this.description = description;
		this.accessibleName = accessibleName;
	}
	
	//----------------------------------------------------------------------------------------------------
	public RecordableInfo(String name, Class type, String accessibleName) { this(name,type,name,accessibleName); }
	
	//----------------------------------------------------------------------------------------------------
	public String getName() { return name; }
	
	//----------------------------------------------------------------------------------------------------
	public void setName(String name) {
		if (name == null || name.trim().equals("")) 
			throw new IllegalArgumentException("Name is empty or null.");
		this.name = name;
	}
	
	//----------------------------------------------------------------------------------------------------
	public Class getType() { return type; }
	
	//----------------------------------------------------------------------------------------------------
	public void setType(Class type) {
		if (type == null) 
			throw new IllegalArgumentException("Class type is null.");
		this.type = type;
	}
	
	//----------------------------------------------------------------------------------------------------
	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }
	public String getAccessibleName() { return accessibleName; }
	
	//----------------------------------------------------------------------------------------------------
	public void setAccessibleName(String accessibleName) {
		if (accessibleName == null || accessibleName.trim().equals("")) 
			throw new IllegalArgumentException("Accessible name is empty or null.");
		this.accessibleName = accessibleName;
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public boolean equals(Object o) {
		if (o instanceof RecordableInfo) {
			RecordableInfo that = (RecordableInfo) o;
			return this.accessibleName.equals(that.accessibleName);
		}
		return false;
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override public String toString() { return this.name; }
}
