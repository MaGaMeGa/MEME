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
public class AvailableParameter {
	
	//====================================================================================================
	// members
	
	public final ParameterInfo info;
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public AvailableParameter(final ParameterInfo info) {
		if (info == null) 
			throw new IllegalArgumentException("AvailableParameter(): null parameter.");
		
		this.info = info;
		
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
//		final String humanName = info.getName().replaceAll("([A-Z])", " $1");
		final String humanName = info.getName();
		String result = humanName;
		if (info.getValue() != null) {
			if (info instanceof MasonChooserParameterInfo) {
				final MasonChooserParameterInfo mpInfo = (MasonChooserParameterInfo) info;
				result += ": " + mpInfo.getValidStrings()[((Integer)info.getValue())];
			} else
 				result += ": " + info.getValue().toString();
		}
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof AvailableParameter) {
			final AvailableParameter that = (AvailableParameter) obj;
			return this.info.equals(that.info);
		}
		
		return false;
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public int hashCode() {
		return info.hashCode();
	}
}