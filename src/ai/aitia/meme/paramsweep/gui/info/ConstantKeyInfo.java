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


public class ConstantKeyInfo extends MemberInfo {
	
	//====================================================================================================
	// members
	
	private static final long serialVersionUID = 1984307850020672226L;
	
	protected String strValue = null;
	protected Double numberValue = null;
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public ConstantKeyInfo(String strValue) {
		super(strValue,"String",String.class);
		this.strValue = strValue;
	}
	
	//----------------------------------------------------------------------------------------------------
	public ConstantKeyInfo(Double numberValue) {
		super(numberValue.toString(),"Number",Number.class);
		this.numberValue = numberValue;
	}
	
	//----------------------------------------------------------------------------------------------------
	public Object getValue() { return strValue == null ? numberValue : strValue; }
	public boolean isStringKey() { return strValue != null; }
	public Number getNumberValue() { return numberValue; }

	//----------------------------------------------------------------------------------------------------
	@Override public String toString() { return strValue != null ? strValue : numberValue.toString(); }

	//----------------------------------------------------------------------------------------------------
	@Override
	public boolean equals(Object o) {
		if (o instanceof ConstantKeyInfo) {
			ConstantKeyInfo that = (ConstantKeyInfo) o;
			if (this.isStringKey() && that.isStringKey())
				return this.strValue.equals(that.strValue);
			if (this.numberValue == null || that.numberValue == null) // means the other is string key
				return false;
			return this.numberValue == that.numberValue;
		}
		return false;
	}
}
