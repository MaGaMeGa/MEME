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
package ai.aitia.meme.paramsweep.utils;

import java.io.Serializable;

public class UserDefinedVariable implements Serializable {

	//====================================================================================================
	// members
	
	private static final long serialVersionUID = -4066855282497025151L;
	protected String name;
	protected Class<?> type;
	protected String initializationCode;
	protected boolean defaultInitialized = true;

	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public UserDefinedVariable(final String name, final Class<?> type, final String initializationCode) {
		if (name == null)
			throw new IllegalArgumentException("'name' is null");
		if (type == null)
			throw new IllegalArgumentException("'type' is null");
		this.name = name;
		this.type = type;
		this.initializationCode = initializationCode;
	}
	
	//----------------------------------------------------------------------------------------------------
	public String getName() { return name; }
	public void setName(final String name) { this.name = name; }
	public Class<?> getType() { return type; }
	public void setType(final Class<?> type) { this.type = type; }
	public String getInitializationCode() { return initializationCode; }
	public void setInitializationCode(final String initializationCode) { this.initializationCode = initializationCode; }
	public boolean isDefaultInitialized() { return defaultInitialized; }
	public void setDefaultInitialized(final boolean defaultInitialized) { this.defaultInitialized = defaultInitialized; }
	
	//----------------------------------------------------------------------------------------------------
	@Override public String toString() { return name + " : " + type.getCanonicalName(); }
	
	//----------------------------------------------------------------------------------------------------
	@Override public boolean equals(Object o) {
		if (o instanceof UserDefinedVariable) {
			final UserDefinedVariable that = (UserDefinedVariable) o;
			return this.name.equals(that.name);
		}
		
		return false;
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override public int hashCode() { return name.hashCode(); }
}
