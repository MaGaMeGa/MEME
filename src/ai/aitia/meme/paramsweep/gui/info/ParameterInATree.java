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
package ai.aitia.meme.paramsweep.gui.info;

//----------------------------------------------------------------------------------------------------
public class ParameterInATree extends AvailableParameter {
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public ParameterInATree(final ParameterInfo info) {
		super(info);
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		String result = info.toString();
//		final int idx = result.indexOf(":");
//		if (idx > 0) {
//			final String humanName = result.substring(0,idx).replaceAll("([A-Z])", " $1");
//			result = humanName + result.substring(idx);
//		}
		
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof ParameterInATree) {
			super.equals(obj);
		}
		
		return false;
	}
}