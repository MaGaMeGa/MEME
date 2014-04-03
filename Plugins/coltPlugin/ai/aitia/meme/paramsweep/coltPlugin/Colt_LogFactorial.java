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

/** Statistic method decriptor plugin. This method returns log(k!). Tries to avoid
 *  overflows. For k&lt;30 simply looks up a table in O(1). For k&gt;=30 uses stirlings
 *  approximation.
 *  <p>
 *  <b>Parameters:</b>
 *  <dd>k - must hold k &gt;= 0.</dd>
 */
public class Colt_LogFactorial implements IStatisticsPlugin {

	//=======================================================================================
	// methods
	
	//---------------------------------------------------------------------------------------
	public String getName() { return "logFactorial"; }
	public String getLocalizedName() { return "Logarithm of factorial"; }
	public String getFullyQualifiedName() { return "cern.jet.math.Arithmetic.logFactorial"; }
	public int getNumberOfParameters() { return 1; }
	public List<String> getParameterNames() { return Arrays.asList(new String[] { "k" }); }
	public List<Class> getParameterTypes() { return Arrays.asList(new Class[] { Integer.TYPE }); }
	public Class getReturnType() { return Double.TYPE; }
	public String getDescription() { return "Returns log(k!). Tries to avoid overflows."; }
	public List<String> getParameterDescriptions() { 
		return Arrays.asList(new String[] { "K: must >= 0" });
	}
}
