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

/** This class encapsulates the string representations of a possible return type and
 *  its default value.
 */
public class ReturnTypeElement {
	
	//====================================================================================================
	// members
	
	/** The type in string format. */
	private String name = null;
	/** The default value in string format. */
	private String defaultValue = null;
	private Class<?> javaType = null;
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	/** Constructor.
	 * @param name the type in string format
	 * @param defaultValue the default value in string format
	 */
	public ReturnTypeElement(String name, String defaultValue, Class<?> javaType) {
		this.name = name;
		this.defaultValue = defaultValue;
		this.javaType = javaType;
	}
	
	//----------------------------------------------------------------------------------------------------
	/** Constructor.
	 * @param name the type in string format
	 */
	public ReturnTypeElement(String name, Class<?> javaType) {
		this.name = name;
		this.defaultValue = name.equals("char") ? "''" : "null";
		this.javaType = javaType;
	}
	
	//----------------------------------------------------------------------------------------------------
	public String getDefaultValue() { return defaultValue; }
	public Class<?> getJavaType() { return javaType; }
	@Override public String toString() { return name; }
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ReturnTypeElement) {
			ReturnTypeElement that = (ReturnTypeElement) obj;
			return this.name.equals(that.name);
		}
		return false;
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override public int hashCode() { return name.hashCode(); }
}
