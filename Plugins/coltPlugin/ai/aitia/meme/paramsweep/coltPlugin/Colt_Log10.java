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
package ai.aitia.meme.paramsweep.coltPlugin;

/** Statistic method decriptor plugin. This method returns log<sub>10</sub>value. */
public class Colt_Log10 extends Colt_Log2 {

	//=======================================================================================
	// methods
	
	//---------------------------------------------------------------------------------------
	@Override public String getName() { return "log10_"; }
	@Override public String getLocalizedName() { return "Logarithm (10-based)"; }
	@Override public String getFullyQualifiedName() { return "cern.jet.math.Arithmetic.log10"; }
	@Override public String getDescription() { return "Returns the 10-based logarithm of the value."; }
}
