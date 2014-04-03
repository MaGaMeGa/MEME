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

/** Statistic method decriptor plugin. This method returns the sample weighted variance
 *  of a data sequence. That is (sumOfSquaredProducts - sumOfProducts * sumOfProducts /
 *  sumOfWeights) / (sumOfWeights - 1).
 *  <b>Parameters:</b>
 *  <dd>sumOfWeights - == Sum( weights[i] ).</dd>
 *  <dd>sumOfProducts - == Sum( data[i] * weights[i] ).</dd>
 *  <dd>sumOfSquaredProducts - == Sum( data[i] * data[i] * weights[i] ).</dd>
 */
import ai.aitia.meme.paramsweep.plugin.IStatisticsPlugin;

@Deprecated
public class Colt_SampleWeightedVariance implements IStatisticsPlugin {

	//=======================================================================================
	// methods
	
	//---------------------------------------------------------------------------------------
	public String getName() { return "sampleWeightedVariance"; }
	public String getLocalizedName() { return "Sample weighted variance"; }
	public String getFullyQualifiedName() { return "cern.jet.stat.Descriptive.sampleWeightedVariance"; }
	public int getNumberOfParameters() { return 3; }
	public List<String> getParameterNames() { return Arrays.asList(new String[] { "sumOfWeights", "sumOfProducts", "sumOfSquaredProducts" }); }
	public List<Class> getParameterTypes() { return Arrays.asList(new Class[] { Double.TYPE, Double.TYPE, Double.TYPE }); }
	public Class getReturnType() { return Double.TYPE; }
	public String getDescription() {
		return "Computes the sample weighted variance of a data sequence. That is " +
			   "(sumOfSquaredProducts - sumOfProducts * sumOfProducts / sumOfWeights) / (sumOfWeights - 1)" +
			   " where sumOfWeights is Sum( weights[i] ), sumOfProducts is Sum( data[i] * weights[i] )" +
			   " and sumOfSquaredProducts is Sum( data[i] * data[i] * weights[i] ).";
	}
	public List<String> getParameterDescriptions() {
		return Arrays.asList(new String[] { "Sum( weights[i] )", "Sum( data[i] * weights[i] )",
											"Sum( data[i] * data[i] * weights[i] )" });
	}
}
