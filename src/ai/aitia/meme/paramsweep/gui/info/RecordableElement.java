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

import java.io.Serializable;

import ai.aitia.meme.paramsweep.plugin.gui.MultiColumnRecordableOperatorGUI;

public class RecordableElement implements Serializable {
	
	//====================================================================================================
	// members
	
	private static final long serialVersionUID = -4020818362103779474L;
	
	final protected MemberInfo info;
	protected String alias = null;
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public RecordableElement(MemberInfo info, String alias) {
		this.info = info;
		this.alias = alias;
	}
	
	//----------------------------------------------------------------------------------------------------
	public RecordableElement(MemberInfo info) { this(info,null); }
	
	//----------------------------------------------------------------------------------------------------
	public MemberInfo getInfo() { return info; }
	public String getAlias() { return alias; }
	public void setAlias(String alias) { this.alias = alias; }
	
	//----------------------------------------------------------------------------------------------------
	public String getNameForRecorders() {
		if (alias != null)
			return alias;
		return info.getName();
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		if (MultiColumnOperatorGeneratedMemberInfo.class.isAssignableFrom(info.getClass())) {
			return "Multi-column: " + info.toString();
		} else if (alias == null) {
			return info.toString();
		} else {
			return alias + " (Source: " + info.toString() + ")";
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public boolean equals(Object o) {
		if (o instanceof RecordableElement)
			return info.equals(((RecordableElement)o).info);
		return false;
		//return info.equals(o);
	}
}
