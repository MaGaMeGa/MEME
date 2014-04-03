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

import ai.aitia.meme.paramsweep.colt.SortedDoubleArrayList;
import ai.aitia.meme.paramsweep.plugin.IStatisticsPlugin;

/** Statistic method decriptor plugin. This method returns the phi-quantile; that is, an
 *  element elem for which holds that phi percent of data elements are less than elem.
 *  The quantile need not necessarily be contained in the data sequence, it can be a
 *  linear interpolation.
 *  <p>
 *  <b>Parameters:</b>
 *  <dd>data - the data sequence; must be sorted ascending (because of this the
 *  descriptor uses SortedDoubleArrayList as argument type instead of DoubleArrayList).</dd>
 *  <dd>phi - the percentage; must satisfy 0 &lt;= phi &lt;= 1.</dd>
 */
public class Colt_Quantile implements IStatisticsPlugin {

	//=======================================================================================
	// methods
	
	//---------------------------------------------------------------------------------------
	public String getName() { return "quantile"; }
	public String getLocalizedName() { return "Quantile"; }
	public String getFullyQualifiedName() { return "cern.jet.stat.Descriptive.quantile"; }
	public int getNumberOfParameters() { return 2; }
	public List<String> getParameterNames() { return Arrays.asList(new String[] { "data", "phi" }); }
	public List<Class> getParameterTypes() { return Arrays.asList(new Class[] { SortedDoubleArrayList.class, Double.TYPE }); }
	public Class getReturnType() { return Double.TYPE; }
	public String getDescription() {
		return "Computes the phi-quantile; that is, an element elem for which holds that phi percent " +
			   "of data elements are less than elem. The quantile need not necessarily be contained" +
			   " in the data sequence, it can be a linear interpolation. Phi must satisfy: 0 &lt;= phi &lt;= 1.";
	}
	public List<String> getParameterDescriptions() {
		return Arrays.asList(new String[] { "Data sequence", "The percentage; must satisfy: 0 <= phi <= 1"});
	}
}
