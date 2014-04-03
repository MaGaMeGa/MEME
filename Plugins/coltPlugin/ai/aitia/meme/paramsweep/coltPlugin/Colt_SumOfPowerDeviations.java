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

import java.util.Arrays;
import java.util.List;

import ai.aitia.meme.paramsweep.colt.DoubleArrayList;
import ai.aitia.meme.paramsweep.plugin.IStatisticsPlugin;

/** Statistic method decriptor plugin. This method returns Sum( (data[i]-c)<sup>k</sup> );
 *  optimized for common parameters like c == 0.0 and/or k == -2 .. 4.
 */
public class Colt_SumOfPowerDeviations implements IStatisticsPlugin {

	//=======================================================================================
	// methods
	
	//---------------------------------------------------------------------------------------
	public String getName() { return "sumOfPowerDeviations"; }
	public String getLocalizedName() { return "Sum of power deviations"; }
	public String getFullyQualifiedName() { return "cern.jet.stat.Descriptive.sumOfPowerDeviations"; }
	public int getNumberOfParameters() { return 3; }
	public List<String> getParameterNames() { return Arrays.asList(new String[] { "data", "k", "c" }); }
	public List<Class> getParameterTypes() { return Arrays.asList(new Class[] { DoubleArrayList.class, Integer.TYPE, Double.TYPE }); }
	public Class getReturnType() { return Double.TYPE; }
	public String getDescription() { return "Computes the sum of power deviations of a data sequence, which is Sum( (data[i]-c)<sup>k</sup> )."; }
	public List<String> getParameterDescriptions() { return Arrays.asList(new String[] { "Data sequence", "Exponent", "Constant" }); }
}
