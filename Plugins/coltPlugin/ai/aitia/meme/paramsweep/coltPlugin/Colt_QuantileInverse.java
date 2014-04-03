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

/** Statistic method decriptor plugin. This method returns how many percent of the
 *  elements contained in the receiver are &lt;= element. Does linear interpolation if
 *  the element is not contained but lies in between two contained elements.
 *  <p>
 *  <b>Parameters:</b>
 *  <dd>data - the sequence to be searched (must be sorted ascending; because of this the
 *  descriptor uses SortedDoubleArrayList as argument type instead of DoubleArrayList).</dd>
 *  <dd>element - the element to search for.</dd> 
 *  <p>
 *  <b>Returns:</b>
 *  <dd>the percentage phi of elements &lt;= element (0.0 &lt;= phi &lt;= 1.0).</dd>
 */
public class Colt_QuantileInverse implements IStatisticsPlugin {

	//=======================================================================================
	// methods
	
	//---------------------------------------------------------------------------------------
	public String getName() { return "quantileInverse"; }
	public String getLocalizedName() { return "Inverse quantile"; }
	public String getFullyQualifiedName() { return "cern.jet.stat.Descriptive.quantileInverse"; }
	public int getNumberOfParameters() { return 2; }
	public List<String> getParameterNames() { return Arrays.asList(new String[] { "data", "element" }); }
	public List<Class> getParameterTypes() { return Arrays.asList(new Class[] { SortedDoubleArrayList.class, Double.TYPE }); }
	public Class getReturnType() { return Double.TYPE; }
	public String getDescription() {
		return "Computes how many percent of the elements contained in the data sequence are less or " +
			   "equal to element. Does linear interpolation if the element is not contained " +
			   "but lies in between two contained elements.";
	}
	public List<String> getParameterDescriptions() {
		return Arrays.asList(new String[] { "Data sequence", "The element to search for"});
	}
}
