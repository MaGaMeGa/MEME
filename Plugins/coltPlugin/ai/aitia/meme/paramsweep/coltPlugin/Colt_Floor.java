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

/** Statistic method decriptor plugin. This method returns the largest long &lt;= value.<p>  
 *  Examples: 1.0 -&gt; 1, 1.2 -&gt; 1, 1.9 -&gt; 1, 2.0 -&gt; 2, 2.2 -&gt; 2, 2.9 -&gt; 2<p>
 *  This method is safer than using (long) Math.floor(value), because of possible
 *  rounding error.
 */
public class Colt_Floor implements IStatisticsPlugin {

	//=======================================================================================
	// methods
	
	//---------------------------------------------------------------------------------------
	public String getName() { return "floor"; }
	public String getLocalizedName() { return "Floor"; }
	public String getFullyQualifiedName() { return "cern.jet.math.Arithmetic.floor"; }
	public int getNumberOfParameters() { return 1; }
	public List<String> getParameterNames() { return Arrays.asList(new String[] { "value" }); }
	public List<Class> getParameterTypes() { return Arrays.asList(new Class[] { Double.TYPE }); }
	public Class getReturnType() { return Long.TYPE; }
	public String getDescription() { return "Returns the largest long &lt;= value."; }
	public List<String> getParameterDescriptions() { 
		return Arrays.asList(new String[] { "Value" });
	}
}
