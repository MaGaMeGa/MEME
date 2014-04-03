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
package ai.aitia.meme.paramsweep.intellisweepPlugin;

import ai.aitia.meme.paramsweep.gui.info.ParameterInfo;

/**
 * @author Ferschl
 */
public class FactorialParameterInfo extends ParameterInfo {

    private static final long serialVersionUID = 1L;
	/** The low value of the parameter in the two-level design. */
	protected Object lowValue = null;
	/** The high value of the parameter in the two-level design. */
	protected Object highValue = null;
	
	//------------------------------------------------------------------------------
	public FactorialParameterInfo(ParameterInfo pi) {
	    super(pi.getName(), pi.getType(), pi.getJavaType());
	    
    }

    public Object getHighValue() { return highValue; }
    public void setHighValue(Object highValue) { this.highValue = highValue; }
    public Object getLowValue() { return lowValue; }
    public void setLowValue(Object lowValue) { this.lowValue = lowValue; }

	//-------------------------------------------------------------------------------
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(this.name);
		sb.append(" : ");
		sb.append(this.type);
		sb.append(" [value=");
		sb.append(this.getValue());
		sb.append("]");
		return sb.toString();
	}

}

