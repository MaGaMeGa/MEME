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
package ai.aitia.meme.paramsweep.plugin.gui;

public class NetLogoType {
	
	//====================================================================================================
	// members
	
	private String label = null;
	private Class<?> type = null;
	private Class<?> innerType = null;
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public NetLogoType(String label, Class<?> type, Class<?> innerType) {
		this.label = label;
		this.type = type;
		this.innerType = innerType;
	}
	
	//----------------------------------------------------------------------------------------------------
	public NetLogoType(String label, Class<?> type) { this(label,type,Void.class); }
	
	//----------------------------------------------------------------------------------------------------
	public Class<?> getType() { return type; }
	public Class<?> getInnerType() { return innerType; }
	@Override public String toString() { return label; }
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public boolean equals(Object other) {
		if (other == null) return false;
		if (other instanceof NetLogoType) {
			NetLogoType that = (NetLogoType) other;
			return this.type.equals(that.type) && this.innerType.equals(that.innerType);
		}
		return false;
	}
}
