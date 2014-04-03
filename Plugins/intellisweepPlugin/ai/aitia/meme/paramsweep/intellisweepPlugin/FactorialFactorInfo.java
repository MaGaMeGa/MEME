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
 *
 */
public class FactorialFactorInfo extends FactorialParameterInfo {
	private static final long serialVersionUID = 6517848951899841134L;

	public FactorialFactorInfo(ParameterInfo pi) {
	    super(pi);
    }

	//-------------------------------------------------------------------------------
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(this.name);
		sb.append(" : ");
		sb.append(this.type);
		sb.append(" [low=");
		sb.append(lowValue);
		sb.append(", high=");
		sb.append(highValue);
		sb.append("]");
		return sb.toString();
	}

}
