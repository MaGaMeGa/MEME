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

/** Statistic method decriptor plugin. This method efficiently returns the binomial
 *  coefficient, often also referred to as "n over k" or "n choose k". The binomial
 *  coefficient is defined as (n * n-1 * ... * n-k+1 ) / ( 1 * 2 * ... * k ).<br>
 *  <ul>
 *  <li>k&lt;0: 0.</li>
 *  <li>k==0: 1.</li>
 *  <li>k==1: n.</li>
 *  <li>else: (n * n-1 * ... * n-k+1 ) / ( 1 * 2 * ... * k ).</li>
 *  <ul> 
 *  <p>
 *  <b>Returns:</b>
 *  <dd>the binomial coefficient.</li>
 */
public class Colt_Binomial implements IStatisticsPlugin {

	//=======================================================================================
	// methods
	
	//---------------------------------------------------------------------------------------
	public String getName() { return "binomial"; }
	public String getLocalizedName() { return "Binomial"; }
	public String getFullyQualifiedName() { return "cern.jet.math.Arithmetic.binomial"; }
	public int getNumberOfParameters() { return 2; }
	public List<String> getParameterNames() { return Arrays.asList(new String[] { "n", "k" }); }
	public List<Class> getParameterTypes() { return Arrays.asList(new Class[] { Double.TYPE, Long.TYPE }); }
	public Class getReturnType() { return Double.TYPE; }
	public String getDescription() { return "Efficiently returns the binomial coefficient, often also referred to as \"n over k\" or \"n choose k\"."; }
	public List<String> getParameterDescriptions() { 
		return Arrays.asList(new String[] { "n", "k" });
	}
}
