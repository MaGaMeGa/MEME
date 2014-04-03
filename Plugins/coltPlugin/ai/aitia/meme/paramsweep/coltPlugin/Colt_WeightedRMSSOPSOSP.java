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

import ai.aitia.meme.paramsweep.plugin.IStatisticsPlugin;

public class Colt_WeightedRMSSOPSOSP implements IStatisticsPlugin {

	//=======================================================================================
	// methods
	
	//---------------------------------------------------------------------------------------
	public String getName() { return "weightedRMS"; }
	public String getLocalizedName() { return "Weighted RMS from sum of products and sum of squared products"; }
	public String getFullyQualifiedName() { return "cern.jet.stat.Descriptive.weightedRMS"; }
	public int getNumberOfParameters() { return 2; }
	public List<String> getParameterNames() { return Arrays.asList(new String[] { "sumOfProducts", "sumOfSquaredProducts" }); }
	public List<Class> getParameterTypes() { return Arrays.asList(new Class[] { Double.TYPE, Double.TYPE }); }
	public Class getReturnType() { return Double.TYPE; }
	public String getDescription() {
		return "Returns the weighted RMS (Root-Mean-Square) of a data sequence. That is" +
			   " Sum( data[i] * data[i] * weights[i]) / Sum( data[i] * weights[i] ), or in other" +
			   " words sumOfProducts / sumOfSquaredProducts. The data sequence is defined by " +
			   "the sum of products of its elements and the sum of squared products of its elements.";
	}
	public List<String> getParameterDescriptions() {
		return Arrays.asList(new String[] { "Sum( data[i] * weights[i] )", 
											"Sum( data[i] * data[i] * weights[i] )" });
	}
}
